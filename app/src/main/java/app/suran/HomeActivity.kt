package app.suran

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class HomeActivity : ComponentActivity() {
    private val vm: SuranViewModel by viewModels()

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { vm.onResume() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = barStyle(Ink.dark), navigationBarStyle = barStyle(Ink.dark))
        setContent {
            val s by vm.state.collectAsStateWithLifecycle()
            val dark = Ink.dark
            // System bar icons follow SURAN's theme, not the system's.
            LaunchedEffect(dark) { enableEdgeToEdge(statusBarStyle = barStyle(dark), navigationBarStyle = barStyle(dark)) }
            // Page colour behind the crossfade, so the (light) window background never shows through in dark.
            Box(Modifier.fillMaxSize().background(Ink.bg)) {
            Crossfade(targetState = s.screen, animationSpec = tween(200), label = "screen") { screen ->
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        s = s,
                        onRow = vm::launch,
                        onChange = vm::openEdit,
                        onMakeHome = vm::showHomeSheet,
                        onChooseHome = ::requestHome,
                        onLater = vm::hideHomeSheet,
                        onConnectivity = ::openConnectivity,
                        onClearError = vm::clearLaunchError,
                        onClearReady = vm::clearReadyLine,
                        onTheme = vm::toggleTheme,
                    )
                    Screen.CHOOSE -> ChooseScreen(
                        s = s,
                        canSave = normalize(s.draft).isNotEmpty() &&
                            (s.chooseMode == ChooseMode.FIRST_RUN || vm.hasChanges(s)),
                        onToggleCatalog = vm::toggleCatalog,
                        onPick = vm::pickCandidate,
                        onDismissAmbiguity = vm::dismissAmbiguity,
                        onToggleApp = vm::toggleApp,
                        onToggleEntry = vm::toggleEntry,
                        onExpand = vm::toggleLongTail,
                        onCancel = vm::cancelEdit,
                        onCta = { vm.save() },
                    )
                }
            }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Session.reset()   // "Open once" ends and the Outside guard resets when Home is shown.
        vm.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Home button pressed while SURAN is already in front.
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.onHomePressed()
    }

    /**
     * Opens the native Wi-Fi or Bluetooth settings. No permission needed, nothing is toggled by SURAN.
     * Tries the specific screen first, then wireless settings, then the main Settings screen.
     * Started without NEW_TASK so Back from Settings returns to SURAN.
     */
    private fun openConnectivity(target: String) {
        val actions = if (target == "wifi") {
            listOf(Settings.ACTION_WIFI_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
        } else {
            listOf(Settings.ACTION_BLUETOOTH_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS)
        }
        for (action in actions) {
            try {
                startActivity(Intent(action))
                vm.logConnectivity(target, action)
                return
            } catch (e: Exception) {
                // Not available on this device: try the next, broader screen.
            }
        }
        vm.reportError("Couldn't open settings.")
    }

    private fun requestHome() {
        if (isDefaultHome(this)) return
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        // API 26-28 (e.g. Honor 10 Lite on Android 9): no role API. Send the user to system settings.
        // The explanation is shown by SURAN's own sheet before this point.
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            // Fallback: pressing "Home" with no default set shows the system chooser.
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
