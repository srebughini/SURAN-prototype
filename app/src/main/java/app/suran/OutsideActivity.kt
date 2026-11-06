package app.suran

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom sheet shown when something outside the configuration is reached.
 * Entry points: debug intent `app.suran.OUTSIDE --es package <pkg>` (debug builds) and the M2 service.
 */
class OutsideActivity : ComponentActivity() {
    private var pkg by mutableStateOf("")
    private val store by lazy { ConfigStore(filesDir) }
    private val log by lazy { EventLog(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = barStyle(Ink.dark), navigationBarStyle = barStyle(Ink.dark))
        if (!accept(intent)) { finish(); return }

        setContent {
            var shown by remember { mutableStateOf(false) }
            var error by remember(pkg) { mutableStateOf<String?>(null) }
            LaunchedEffect(Unit) { shown = true }
            val n = remember(pkg) { store.load()?.entries?.size ?: 0 }
            val label = remember(pkg) { appLabelOf(this@OutsideActivity, pkg) }
            BackHandler { onBack() }

            Box(
                Modifier.fillMaxSize().background(Ink.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
            ) {
                AnimatedVisibility(
                    visible = shown,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(tween(200)) { it },
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .sheetSurface()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                            .navigationBarsPadding().padding(24.dp),
                    ) {
                        T("Not part of this phone", 24.sp, weight = FontWeight.Medium, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(8.dp))
                        T(
                            if (n >= 2) "$label isn't one of the $n things this phone is for."
                            else "$label isn't what this phone is for.",
                            17.sp, lineHeight = 25.sp,
                        )
                        if (error != null) {
                            Spacer(Modifier.height(8.dp))
                            T(error!!, 15.sp, color = Ink.error)
                        }
                        Spacer(Modifier.height(24.dp))
                        PrimaryButton("Back to your phone") { onBack() }
                        Spacer(Modifier.height(12.dp))
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, Ink.ink, RoundedCornerShape(10.dp))
                                .pressable({ onOnce { error = "Couldn't open it." } }, label = "Open $label once"),
                            contentAlignment = Alignment.Center,
                        ) { T("Open once", 18.sp, weight = FontWeight.SemiBold) }
                        Box(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .pressable({ onAdd { error = "Couldn't open it." } }, label = "Add $label to this phone"),
                            contentAlignment = Alignment.Center,
                        ) { T("Add to this phone", 17.sp, weight = FontWeight.Medium) }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!accept(intent)) finish()
    }

    private fun accept(i: Intent): Boolean {
        val p = i.getStringExtra("package")
        if (p.isNullOrBlank()) return false
        pkg = p
        log.log("outside_shown", "pkg" to p)
        return true
    }

    private fun goHome() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }

    private fun onBack() {
        log.log("outside_action", "action" to "back")
        goHome()
    }

    private fun onOnce(onFail: () -> Unit) {
        log.log("outside_action", "action" to "once", "pkg" to pkg)
        Session.onceAllowed.add(pkg)
        if (launchApp(this, pkg)) finish() else onFail()
    }

    private fun onAdd(onFail: () -> Unit) {
        log.log("outside_action", "action" to "add", "pkg" to pkg)
        val cfg = store.load() ?: Config(false, emptyList())
        if (cfg.entries.none { it.pkg == pkg }) {
            val next = cfg.copy(entries = normalize(cfg.entries + Entry(pkg, null, appLabelOf(this, pkg))))
            if (store.save(next)) log.log("config_saved", "added" to listOf(pkg), "removed" to emptyList<String>())
        }
        if (launchApp(this, pkg)) finish() else onFail()
    }
}
