package app.suran

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChooseScreen(
    s: UiState,
    canSave: Boolean,
    onToggleCatalog: (CatalogItem) -> Unit,
    onPick: (CatalogItem, InstalledApp) -> Unit,
    onDismissAmbiguity: () -> Unit,
    onToggleApp: (InstalledApp) -> Unit,
    onToggleEntry: (Entry) -> Unit,
    onExpand: () -> Unit,
    onCancel: () -> Unit,
    onCta: () -> Unit,
) {
    val edit = s.chooseMode == ChooseMode.EDIT
    // Back: closes the sheet first; cancels in edit mode; does nothing during first-run.
    BackHandler(enabled = true) {
        if (s.ambiguity != null) onDismissAmbiguity() else if (edit) onCancel()
    }

    val installedPkgs = s.installed.map { it.pkg }.toSet()
    val candidatePkgs = s.catalog.flatMap { it.second }.map { it.pkg }.toSet()
    val longTail = s.installed.filter { it.pkg !in candidatePkgs }
    val missing = s.draft.filter { it.pkg !in installedPkgs }
    val count = normalize(s.draft).size

    Box(Modifier.fillMaxSize().background(Ink.bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (edit) {
                    Box(
                        Modifier.heightIn(min = 48.dp).pressable(onCancel, label = "Cancel changes")
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) { T("Cancel", 17.sp, color = Ink.muted) }
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                T("What is this phone for?", 32.sp, weight = FontWeight.Medium, spacing = (-0.64).sp,
                    modifier = Modifier.padding(top = 16.dp).semantics { heading() })

                if (missing.isNotEmpty()) {
                    GroupLabel("Not installed")
                    missing.forEach { e ->
                        SelectRow(catalogItem(e.intentId)?.label ?: e.appLabel, e.appLabel.takeIf { catalogItem(e.intentId) != null },
                            selected = true) { onToggleEntry(e) }
                    }
                }

                Group.values().forEach { g ->
                    val items = s.catalog.filter { it.first.group == g }
                    if (items.isNotEmpty()) {
                        GroupLabel(g.title)
                        items.forEach { (item, cands) ->
                            val chosen = s.draft.firstOrNull { it.intentId == item.id }
                            val sub = when {
                                chosen != null -> chosen.appLabel
                                cands.size == 1 -> cands[0].label
                                else -> null
                            }?.takeIf { !it.equals(item.label, ignoreCase = true) }
                            SelectRow(item.label, sub, selected = chosen != null) { onToggleCatalog(item) }
                        }
                    }
                }

                // Secondary on purpose: quiet, collapsed, smaller type.
                if (longTail.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Ink.line))
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).pressable(onExpand, label = if (s.longTailExpanded) "Hide other apps" else "Show other apps"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T("Everything else (${longTail.size})", 17.sp, color = Ink.muted, modifier = Modifier.weight(1f))
                        T(if (s.longTailExpanded) "Hide" else "Show", 15.sp, color = Ink.muted)
                    }
                    if (s.longTailExpanded) {
                        longTail.forEach { a ->
                            SelectRow(a.label, null, selected = s.draft.any { it.pkg == a.pkg }, big = false) { onToggleApp(a) }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp, top = 8.dp)) {
                if (s.saveError) {
                    T("Couldn't save. Try again.", 15.sp, color = Ink.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                val label = when {
                    edit -> "Save changes"
                    count == 0 -> "Choose what this phone is for"
                    count == 1 -> "Start with this one"
                    else -> "Start with these $count"
                }
                PrimaryButton(label, enabled = canSave, onClick = onCta)
            }
        }

        val amb = s.ambiguity
        if (amb != null) {
            val cands = s.catalog.firstOrNull { it.first.id == amb.id }?.second.orEmpty()
            AmbiguitySheet(amb, cands, onPick, onDismissAmbiguity)
        }
    }
}

@Composable
private fun GroupLabel(text: String) =
    T(text, 13.sp, color = Ink.muted, spacing = 0.65.sp, modifier = Modifier.padding(top = 32.dp, bottom = 4.dp))

@Composable
fun PrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val bg = when {
        !enabled -> Ink.line
        pressed -> Ink.accentPressed
        else -> Ink.accent
    }
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp)).background(bg)
            .clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(label, 19.sp, color = if (enabled) Ink.bg else Ink.muted, weight = FontWeight.SemiBold) }
}

@Composable
private fun SelectRow(label: String, sub: String?, selected: Boolean, big: Boolean = true, onToggle: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .background(if (pressed) Ink.surfaceAlt else Color.Transparent)
            .toggleable(value = selected, interactionSource = src, indication = null, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(selected)
        Spacer(Modifier.width(16.dp))
        Column {
            T(
                label, if (big) 22.sp else 18.sp,
                color = if (big || selected) Ink.ink else Ink.muted,
                weight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (sub != null) T(sub, 15.sp, color = Ink.muted)
        }
    }
}

/** Selection is never color alone: filled dot + ring + heavier label. */
@Composable
private fun Dot(selected: Boolean) {
    Canvas(Modifier.size(24.dp)) {
        val stroke = 1.5.dp.toPx()
        val r = size.minDimension / 2f - stroke / 2f
        if (selected) {
            drawCircle(Ink.accent, radius = r + stroke / 2f)
        } else {
            drawCircle(Ink.muted, radius = r, style = Stroke(stroke))
        }
    }
}

@Composable
private fun AmbiguitySheet(
    item: CatalogItem,
    cands: List<InstalledApp>,
    onPick: (CatalogItem, InstalledApp) -> Unit,
    onDismiss: () -> Unit,
) {
    val none = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Ink.scrim)
            .clickable(interactionSource = none, indication = null, onClick = onDismiss),
    ) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .sheetSurface()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                .navigationBarsPadding().padding(24.dp),
        ) {
            T("Which one for ${item.label}?", 24.sp, weight = FontWeight.Medium, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            cands.forEach { a ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable({ onPick(item, a) }, label = "Use ${a.label}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) { T(a.label, 20.sp) }
            }
        }
    }
}
