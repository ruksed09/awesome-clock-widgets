package com.versity.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.Ornament
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.matches
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.HomePreview
import com.versity.launcher.ui.components.LockPreview
import com.versity.launcher.ui.studio.BackGlyph
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioDivider
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioIconButton
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioMotion
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioSegmented
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.ThemeToggleIcon

internal val Gutter = 16.dp

/** Settings pages stay at a comfortable reading width on tablets and wide windows, centred. */
internal fun Modifier.readableWidth(max: Dp = 720.dp): Modifier =
    this.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = max)

/** Scrolling page body: readable width, gutters and even spacing between sections. */
@Composable
internal fun PageBody(modifier: Modifier = Modifier, max: Dp = 720.dp, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).readableWidth(max)
            .padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) { content() }
}

internal fun layoutLabel(l: HomeLayout) = when (l) {
    HomeLayout.MINIMAL -> "Minimal"
    HomeLayout.ORBIT -> "Centered"
}

internal fun clockStyleLabel(s: ClockStyle) = when (s) {
    ClockStyle.ANALOGUE -> "Analogue"
    ClockStyle.ORNAMENT -> "Ornament"
    ClockStyle.GIANT -> "Giant"
    ClockStyle.OUTLINE -> "Outline"
    ClockStyle.NEON -> "Neon"
    ClockStyle.EXTRUDED -> "3D"
    ClockStyle.SEGMENT -> "LED"
    ClockStyle.FLIP -> "Flip"
    ClockStyle.STAGE -> "Stage"
    ClockStyle.GLASS -> "Glass"
}

internal fun ornamentLabel(o: Ornament) = when (o) {
    Ornament.CELESTIAL -> "Celestial line"
    Ornament.UNDERLINE -> "Underline"
    Ornament.MOON -> "Moon divider"
    Ornament.CORNERS -> "Frame corners"
}

internal fun uiModeLabel(m: UiMode) = when (m) {
    UiMode.SYSTEM -> "System"
    UiMode.LIGHT -> "Light"
    UiMode.DARK -> "Dark"
}

/** The theme the settings currently match, or null for a custom mix. */
internal fun currentHomeTheme(s: LauncherSettings): ThemeBundle? = ThemePresets.themes.firstOrNull { it.matches(s) }

internal fun lockThemeLabel(s: LauncherSettings): String =
    s.lockThemeId?.let(ThemePresets::theme)?.name ?: "Same as home screen"

// ---------------------------------------------------------------- Headers

@Composable
internal fun HubHeader(dark: Boolean, onToggleTheme: () -> Unit) {
    // Same width as the hub's cards, so the title lines up with them on tablets.
    Row(
        Modifier.readableWidth(1040.dp).padding(start = Gutter + 4.dp, end = 4.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StudioText("Versity", StudioType.display, modifier = Modifier.semantics { heading() })
            StudioText("Themes, wallpapers and widgets", StudioType.small, Studio.colors.muted, maxLines = 1)
        }
        ThemeToggle(dark, onToggleTheme)
    }
}

@Composable
internal fun PageHeader(title: String, dark: Boolean, onBack: () -> Unit, action: (@Composable () -> Unit)? = null, onToggleTheme: () -> Unit) {
    Column {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioIconButton("Back", onClick = onBack) { BackGlyph() }
            StudioText(title, StudioType.title, modifier = Modifier.weight(1f).padding(start = 2.dp).semantics { heading() }, maxLines = 1)
            action?.invoke()
            ThemeToggle(dark, onToggleTheme)
        }
    }
}

@Composable
internal fun ThemeToggle(dark: Boolean, onToggle: () -> Unit) =
    StudioIconButton(
        if (dark) "Switch to light mode" else "Switch to dark mode",
        Modifier.testTag(TestTags.THEME_TOGGLE),
        onClick = onToggle,
    ) { ThemeToggleIcon(dark) }

// ---------------------------------------------------------------- Previews

/** Recessed well that previews sit in. */
@Composable
internal fun PreviewStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Studio.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(c.panel, StudioCardCorner)
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
internal fun LabelledPreview(label: String, changed: Boolean, content: @Composable () -> Unit) {
    val c = Studio.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            StudioText(label, StudioType.caption.copy(fontWeight = FontWeight.Medium), if (changed) c.ink else c.muted, maxLines = 1)
            if (!changed) StudioText("· no change", StudioType.caption, c.muted, maxLines = 1)
        }
    }
}

/** Home and lock side by side, as wide as fits up to 124dp each. */
@Composable
internal fun SheetPreviews(home: LauncherSettings, lock: LauncherSettings, homeChanges: Boolean, lockChanges: Boolean) {
    PreviewStage {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val each: Dp = min((maxWidth - 16.dp) / 2, 124.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                LabelledPreview("Home screen", homeChanges) { HomePreview(home, each, border = Color.Black.copy(alpha = 0.12f)) }
                LabelledPreview("Lock screen", lockChanges) { LockPreview(lock, each, border = Color.Black.copy(alpha = 0.12f)) }
            }
        }
    }
}

/** Small "Home" / "Lock" markers on a preview's corner, showing where it is applied. */
@Composable
internal fun AppliedTags(onHome: Boolean, onLock: Boolean, modifier: Modifier = Modifier) {
    if (!onHome && !onLock) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOfNotNull("Home".takeIf { onHome }, "Lock".takeIf { onLock }).forEach {
            Text(
                it, color = Color(0xFF17171A), fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = Studio.font,
                modifier = Modifier.background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
internal fun TargetPicker(target: ApplyTarget, onChange: (ApplyTarget) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StudioLabel("Apply to")
        StudioSegmented(ApplyTarget.entries, target, { it.label }, onChange, tag = { TestTags.applyTarget(it.name) })
    }
}

// ---------------------------------------------------------------- Sheets and dialogs

/** Scrim plus bottom sheet; the sheet rises on a decelerating curve and leaves on an accelerating one. */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun <T : Any> SheetHost(visible: Boolean, onDismiss: () -> Unit, key: T?, content: @Composable (T) -> Unit) {
    var shown by remember { mutableStateOf(key) }
    if (key != null) shown = key
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(1)), exit = fadeOut(tween(StudioMotion.QUICK + 50))) {
        val c = Studio.colors
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .animateEnterExit(enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(200)))
                    .background(c.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .animateEnterExit(
                        enter = slideInVertically(tween(StudioMotion.SLOW, easing = StudioMotion.Enter)) { it / 3 } + fadeIn(tween(StudioMotion.STANDARD)),
                        exit = slideOutVertically(tween(200, easing = StudioMotion.Exit)) { it / 4 } + fadeOut(tween(200)),
                    ),
            ) { shown?.let { content(it) } }
        }
    }
}

/** Shared sheet layout: handle, title block, scrolling body and a pinned action bar. */
@Composable
internal fun SheetScaffold(
    label: String,
    title: String,
    subtitle: String,
    tag: String,
    onDismiss: () -> Unit,
    confirm: String,
    onConfirm: () -> Unit,
    body: @Composable () -> Unit,
) {
    val c = Studio.colors
    BackHandler(onBack = onDismiss)
    val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    Column(
        Modifier
            .testTag(tag)
            .semantics { paneTitle = title }
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 32.dp, height = 4.dp).background(c.lineStrong, RoundedCornerShape(2.dp)))
        }
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    StudioLabel(label)
                    StudioText(title, StudioType.heading)
                    StudioText(subtitle, StudioType.small, c.muted)
                }
                StudioIconButton("Close", onClick = onDismiss) { StudioGlyph(Glyph.CLOSE, 16.dp, c.ink) }
            }
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { body() }
        }
        StudioDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioTextButton("Cancel", Modifier.testTag(TestTags.APPLY_CANCEL), onClick = onDismiss)
            StudioPrimaryButton(confirm, Modifier.testTag(TestTags.APPLY_CONFIRM), onClick = onConfirm)
        }
    }
}

/** Confirmation: a question, the consequence, and buttons that name the action. */
@Composable
internal fun ConfirmDialog(
    title: String,
    body: String,
    cancel: String,
    confirm: String,
    cancelTag: String,
    confirmTag: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        val c = Studio.colors
        Column(
            Modifier.fillMaxWidth()
                .background(c.surface, StudioCardCorner)
                .border(1.dp, c.line, StudioCardCorner)
                .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StudioText(title, StudioType.title, modifier = Modifier.padding(end = 8.dp))
            StudioText(body, StudioType.body, c.muted, Modifier.padding(end = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                StudioTextButton(cancel, Modifier.testTag(cancelTag), onClick = onCancel)
                StudioPrimaryButton(confirm, Modifier.testTag(confirmTag), onClick = onConfirm)
            }
        }
    }
}
