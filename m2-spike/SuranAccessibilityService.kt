package app.suran

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager

/**
 * M2 SPIKE. Detects which package came to the foreground (TYPE_WINDOW_STATE_CHANGED only),
 * and shows OutsideActivity when it is not part of the configuration.
 * Reads no window content, performs no actions, modifies nothing.
 */
class SuranAccessibilityService : AccessibilityService() {
    private var imePkgs: Set<String> = emptySet()
    private var homePkgs: Set<String> = emptySet()
    private var refreshedAt = 0L
    private var lastPkg: String? = null
    private var lastAt = 0L

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = Session.reset()
    }

    override fun onServiceConnected() {
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        EventLog(this).log("m2_service", "state" to "connected")
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenOff) } catch (_: Exception) {}
        EventLog(this).log("m2_service", "state" to "destroyed")
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (e == null || e.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = e.packageName?.toString() ?: return
        val now = SystemClock.elapsedRealtime()
        if (pkg == lastPkg && now - lastAt < 1500) return   // debounce
        lastPkg = pkg; lastAt = now

        if (pkg == packageName) return
        refreshSystemSets(now)
        val cfg = ConfigStore(filesDir).load()
        if (cfg == null || !cfg.setupDone) return
        if (pkg in Allowlist.packages || pkg in imePkgs || pkg in homePkgs) return
        if (cfg.entries.any { it.pkg == pkg } || pkg in Session.onceAllowed) { Session.lastOutside = null; return }
        if (Session.lastOutside == pkg) return               // already shown; wait until reopened
        Session.lastOutside = pkg

        startActivity(
            Intent(this, OutsideActivity::class.java)
                .putExtra("package", pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    }

    private fun refreshSystemSets(now: Long) {
        if (now - refreshedAt < 60_000 && refreshedAt != 0L) return
        refreshedAt = now
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imePkgs = imm.enabledInputMethodList.map { it.packageName }.toSet()
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        homePkgs = packageManager.queryIntentActivities(home, 0).map { it.activityInfo.packageName }.toSet()
    }
}
