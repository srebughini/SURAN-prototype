package app.suran

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun rememberNow(): State<Long> {
    val ctx = LocalContext.current
    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { now.value = System.currentTimeMillis() }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ctx.registerReceiver(r, f)
        now.value = System.currentTimeMillis()
        onDispose { ctx.unregisterReceiver(r) }
    }
    return now
}

@Composable
fun HomeScreen(
    s: UiState,
    onRow: (Entry) -> Unit,
    onChange: () -> Unit,
    onMakeHome: () -> Unit,
    onChooseHome: () -> Unit,
    onLater: () -> Unit,
    onConnectivity: (String) -> Unit,
    onClearError: () -> Unit,
    onClearReady: () -> Unit,
) {
    BackHandler(enabled = true) { if (s.homeSheet) onLater() }   // Back on Home otherwise does nothing
    val cfg = s.config ?: return
    val ctx = LocalContext.current
    val now by rememberNow()
    val installed = s.installed.associateBy { it.pkg }

    LaunchedEffect(s.launchError) { if (s.launchError != null) { delay(3000); onClearError() } }
    LaunchedEffect(s.readyLine) { if (s.readyLine) { delay(6000); onClearReady() } }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Ink.bg).systemBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 32.dp)) {
                T(DateFormat.getTimeFormat(ctx).format(Date(now)), 44.sp, weight = FontWeight.Normal)
                T(
                    DateUtils.formatDateTime(ctx, now, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_DATE),
                    15.sp, color = Ink.muted,
                )
            }
            // Quiet, text-only entry to the native system screens. Not a quick-settings panel.
            Row(Modifier.padding(top = 28.dp)) {
                QuietLink("Wi-Fi", "Wi-Fi settings") { onConnectivity("wifi") }
                QuietLink("Bluetooth", "Bluetooth settings") { onConnectivity("bluetooth") }
            }
        }

        // List: anchored to the bottom when it fits, scrolls when it does not.
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val h = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Centred composition: the list sits in the middle of the free space between the clock and the footer
                // (one item = one quiet word; a long list fills the space and scrolls).
                Column(
                    Modifier.fillMaxWidth().heightIn(min = h).padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    cfg.entries.forEach { e ->
                        val inst = installed[e.pkg]
                        val primary = catalogItem(e.intentId)?.label ?: e.appLabel
                        // One label per app. Only the "Not installed" state shows a second line.
                        val secondary = if (inst == null) "Not installed" else null
                        HomeRow(primary, secondary, dim = inst == null) { onRow(e) }
                    }
                }
            }
        }

        // Notice line (one at a time), then footer.
        val notice = when {
            s.readyLine -> "Your phone is ready. It's for ${cfg.entries.size} ${if (cfg.entries.size == 1) "thing" else "things"}."
            !s.isDefaultHome -> "Make SURAN your Home"
            else -> null
        }
        if (notice != null) {
            NoticeStrip(notice, tappable = !s.readyLine, onClick = onMakeHome)
            Spacer(Modifier.height(8.dp))
        }
        // Hairline above the footer, as on the website.
        Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
        val n = cfg.entries.size
        if (s.launchError != null) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                T(s.launchError, 15.sp, color = Ink.error)
            }
        } else {
            val footer = buildAnnotatedString {
                append(if (n == 1) "This phone is for 1 thing · " else "This phone is for $n things · ")
                withStyle(SpanStyle(color = Ink.ink, fontWeight = FontWeight.Medium)) { append("Change") }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .pressable(onChange, label = "Change what this phone is for")
                    .semantics { contentDescription = "This phone is for $n ${if (n == 1) "thing" else "things"}. Change what this phone is for" },
                verticalAlignment = Alignment.CenterVertically,
            ) { TA(footer, 15.sp) }
        }
    }
    if (s.homeSheet) HomeSetupSheet(onChooseHome, onLater)
    }
}

@Composable
private fun HomeRow(primary: String, secondary: String?, dim: Boolean, onClick: () -> Unit) {
    val desc = if (secondary == null) primary else "$primary, $secondary"
    // Centred label; the app name (when it differs) sits quietly underneath so labels share one axis.
    Column(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .clip(RoundedCornerShape(10.dp))
            .pressable(onClick, label = "Open $primary")
            .semantics { contentDescription = desc }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        T(
            primary, 26.sp, weight = FontWeight.Normal, spacing = (-0.5).sp,
            color = if (dim) Ink.muted else Ink.ink, maxLines = 1,
        )
        if (secondary != null) T(secondary, 16.sp, color = Ink.muted, maxLines = 1)
    }
}

/**
 * Explains the default Home step in plain words, then hands over to Android's own flow.
 * Shown once after first-run Start, and again whenever the user taps "Make SURAN your Home".
 * "Not now" is always available; nothing here bypasses the system confirmation.
 */
@Composable
private fun HomeSetupSheet(onChoose: () -> Unit, onLater: () -> Unit) {
    val action = if (Build.VERSION.SDK_INT >= 29) "Android will ask you to confirm. Choose SURAN."
    else "Android will open its settings. Look for Home or Launcher, and choose SURAN."
    Box(
        Modifier.fillMaxSize().background(Ink.ink.copy(alpha = 0.4f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLater),
    ) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .sheetSurface()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            T("Make SURAN your Home", 24.sp, weight = FontWeight.Medium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(12.dp))
            T("Home is the screen you see when you press the Home button. For this phone to show only what you chose, SURAN needs to be that screen.", 17.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(12.dp))
            T(action, 17.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(12.dp))
            T("Not ready? Skip it. Until then the Home button keeps opening your current home screen, and you can finish from the line at the bottom of SURAN.", 15.sp, color = Ink.muted, lineHeight = 22.sp)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Choose SURAN as Home") { onChoose() }
            Box(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable(onLater, label = "Not now"),
                contentAlignment = Alignment.Center,
            ) { T("Not now", 17.sp, weight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun QuietLink(label: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp)
            .pressable(onClick, label = "Open $description")
            .semantics { contentDescription = description }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) { T(label, 15.sp, color = Ink.muted, maxLines = 1, underline = true) }
}

/** The one soft-green surface on Home: a quiet notice, never a card. */
@Composable
private fun NoticeStrip(text: String, tappable: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val base = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
        .background(if (pressed && tappable) Ink.line else Ink.surfaceAlt)
    val m = if (tappable) base.clickable(
        interactionSource = src, indication = null, role = Role.Button,
        onClickLabel = "Make SURAN your Home", onClick = onClick,
    ) else base
    Row(m.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        T(text, 15.sp, weight = if (tappable) FontWeight.Medium else FontWeight.Normal)
    }
}
