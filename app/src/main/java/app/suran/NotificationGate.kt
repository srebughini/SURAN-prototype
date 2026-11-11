package app.suran

import android.app.Notification
import android.content.ComponentName
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.TelecomManager
import java.io.File

/**
 * M1 experiment, phase 1 (LOG-ONLY): "only notifications from the chosen apps make the phone less distracting".
 *
 * This service NEVER cancels, snoozes, hides or changes a notification. For each posted notification it works out
 * the decision SURAN would take in a later phase and writes one deduplicated line to events.jsonl:
 * package, category, decision, reason.
 *
 * Privacy: reads only packageName, notification.category and notification.flags. It never touches title, text,
 * extras, actions or any other content, and never looks for one-time codes. Nothing is transmitted (no INTERNET).
 *
 * Switch-on: the user grants Notification access in Android Settings. Switch-off: revoke it there, or remove the
 * <service> element from the manifest. Nothing else is stored; config.json is only read.
 *
 * Callbacks arrive on the main thread, so the plain collections below need no locking.
 */
class NotificationGate : NotificationListenerService() {

    private class Decision(val decision: String, val reason: String)

    private val log by lazy { EventLog(applicationContext) }
    private val seen = HashSet<String>()                 // pkg|category|reason already logged in this process
    private val launcherCache = HashMap<String, Boolean>()
    private var cfg: Config? = null
    private var cfgStamp = -1L

    override fun onListenerConnected() {
        super.onListenerConnected()
        launcherCache.clear()
        log.log("notification_listener_connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        log.log("notification_listener_disconnected")
        // Ask Android to bind again. Whether OxygenOS 11 honours this is NOT verified: check on the OnePlus 6.
        try {
            NotificationListenerService.requestRebind(ComponentName(this, NotificationGate::class.java))
        } catch (e: Exception) {
            // Best effort only.
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            val s = sbn ?: return
            val pkg = s.packageName ?: return
            val n = s.notification ?: return
            val d = decide(pkg, n)
            if (!seen.add("$pkg|${n.category}|${d.reason}")) return
            log.log("notification_decision", "pkg" to pkg, "category" to n.category, "decision" to d.decision, "reason" to d.reason)
        } catch (e: Exception) {
            // Logging must never break the phone.
        }
    }

    /** First rule that matches wins. Cheap checks first. Anything uncertain is "allow". */
    private fun decide(pkg: String, n: Notification): Decision {
        val c = config()
        if (c == null || !c.setupDone || c.entries.isEmpty()) return allow("no_config")
        if (pkg == packageName) return allow("self")
        if (c.entries.any { it.pkg == pkg }) return allow("selected")
        if (pkg in ALLOW_VERIFIED) return allow("whitelist_verified")
        if (pkg in ALLOW_TO_VERIFY) return allow("whitelist_to_verify")
        if (n.category in ALLOW_CATEGORIES) return allow("category")
        if ((n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE)) != 0) return allow("ongoing")
        if (pkg == defaultSmsPackage()) return allow("default_sms")
        if (pkg == defaultDialerPackage()) return allow("default_dialer")
        if (!hasLauncher(pkg)) return allow("no_launcher")   // system and service packages have no launcher icon
        return Decision("would_suppress", "unselected")
    }

    private fun allow(reason: String) = Decision("allow", reason)

    /** config.json is only read; it is reloaded when the file changes. "config.json" mirrors ConfigStore's file name. */
    private fun config(): Config? {
        val stamp = File(filesDir, "config.json").lastModified()
        if (stamp != cfgStamp) {
            cfg = ConfigStore(filesDir).load()
            cfgStamp = stamp
        }
        return cfg
    }

    private fun defaultSmsPackage(): String? = try {
        Telephony.Sms.getDefaultSmsPackage(this)
    } catch (e: Exception) {
        null
    }

    private fun defaultDialerPackage(): String? = try {
        getSystemService(TelecomManager::class.java)?.defaultDialerPackage
    } catch (e: Exception) {
        null
    }

    /** Uses the existing <queries> entry for MAIN/LAUNCHER. A package that is not visible reads as "no launcher" (allow). */
    private fun hasLauncher(pkg: String): Boolean =
        launcherCache.getOrPut(pkg) { packageManager.getLaunchIntentForPackage(pkg) != null }

    companion object {
        /**
         * Packages that must never be silenced because their notifications can carry sign-in codes or approvals.
         * NOT a complete list: banks, e-mail, chat apps and push-approval apps are not covered (see TS-25).
         *
         * ALLOW_VERIFIED: confirmed on the OnePlus 6 (installed under this exact package name AND seen in events.jsonl).
         * Empty until that check is done: move a package here only with that evidence.
         */
        val ALLOW_VERIFIED: Set<String> = emptySet()

        /**
         * ALLOW_TO_VERIFY: names taken from the project catalog and from general knowledge, NOT checked on the device.
         * A wrong or missing name fails open in phase 1 (nothing is suppressed), but must be fixed before any phase 2.
         */
        val ALLOW_TO_VERIFY: Set<String> =
            (catalogItem("authenticator")?.packages.orEmpty() + listOf(
                "com.authy.authy",                // Authy
                "org.fedorahosted.freeotp",       // FreeOTP
                "com.beemdevelopment.aegis",      // Aegis
                "com.duosecurity.duomobile",      // Duo Mobile
                "com.okta.android.auth",          // Okta Verify
                "com.twofasapp",                  // 2FAS
                "com.yubico.yubioath",            // Yubico Authenticator
                "com.google.android.gms",         // Google Play services: "Is it you signing in?" prompts
            )).toSet()

        /** Calls and alarms must always get through, plus system and error notices. */
        val ALLOW_CATEGORIES: Set<String> = setOf(
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_SYSTEM,
            Notification.CATEGORY_ERROR,
        )
    }
}
