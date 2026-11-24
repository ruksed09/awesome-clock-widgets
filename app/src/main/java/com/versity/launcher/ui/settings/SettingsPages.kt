package com.versity.launcher.ui.settings

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.DrawerStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.IconStyle
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.Ornament
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.system.UpdateState
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.HomePreview
import com.versity.launcher.ui.components.LockPreview
import com.versity.launcher.ui.home.components.ClockFace
import com.versity.launcher.ui.previewPalette
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCard
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioChipCorner
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioMotion
import com.versity.launcher.ui.studio.StudioRow
import com.versity.launcher.ui.studio.StudioSection
import com.versity.launcher.ui.studio.StudioSegmented
import com.versity.launcher.ui.studio.StudioSlider
import com.versity.launcher.ui.studio.StudioSwitchRow
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.fontFamilyFor
import java.time.LocalDateTime
import kotlin.math.roundToInt

private val PreviewNow: LocalDateTime = LocalDateTime.of(2026, 11, 11, 11, 11)
private val AccentSwatches = listOf(
    0xFF8FA3FFL, 0xFF4FC3E8L, 0xFF5ED6A0L, 0xFFFFD166L,
    0xFFFF8A5CL, 0xFFFF6B8BL, 0xFFC792EAL, 0xFFE8EAF2L,
)

// ---------------------------------------------------------------- Settings

@Composable
internal fun SettingsPage(
    settings: LauncherSettings,
    actions: CustomiseActions,
    isDefault: Boolean,
    go: (CustomisePage) -> Unit,
    onReset: () -> Unit,
    update: UpdateState = UpdateState.Idle,
    updateActions: UpdateActions = UpdateActions({}, {}, {}),
    versionName: String = "",
    /** The page shown beside this list on tablets, highlighted here. */
    selected: CustomisePage? = null,
) {
    val c = Studio.colors
    PageBody {
        StudioSection("Home screen") {
            StudioRow(
                "Clock", glyph = Glyph.CLOCK, value = clockStyleLabel(settings.clockStyle),
                modifier = Modifier.testTag(TestTags.nav(CustomisePage.CLOCK.name)), selected = selected == CustomisePage.CLOCK,
            ) { go(CustomisePage.CLOCK) }
            StudioRow(
                "Colours and font", glyph = Glyph.PALETTE, divider = true,
                value = "${ThemePresets.palette(settings.paletteId).name} · ${ThemePresets.font(settings.fontId).name}",
                modifier = Modifier.testTag(TestTags.nav(CustomisePage.COLOURS.name)), selected = selected == CustomisePage.COLOURS,
            ) { go(CustomisePage.COLOURS) }
            StudioRow(
                "Layout and position", glyph = Glyph.LAYOUT, divider = true,
                value = layoutLabel(settings.layout) + if (settings.clockPlacement != null) " · moved" else "",
                modifier = Modifier.testTag(TestTags.nav(CustomisePage.LAYOUT.name)), selected = selected == CustomisePage.LAYOUT,
            ) { go(CustomisePage.LAYOUT) }
        }
        StudioSection("Lock screen") {
            StudioRow(
                "Lock screen", glyph = Glyph.LOCK,
                value = if (settings.lockStyleEnabled) lockThemeLabel(settings) else "Off",
                modifier = Modifier.testTag(TestTags.nav(CustomisePage.LOCK.name)), selected = selected == CustomisePage.LOCK,
            ) { go(CustomisePage.LOCK) }
        }
        StudioSection("Apps") {
            StudioRow(
                "App drawer and icons", glyph = Glyph.APPS,
                value = (if (settings.drawerStyle == DrawerStyle.PLAIN) "Plain" else "Themed") + " · " +
                    (if (settings.iconStyle == IconStyle.ORIGINAL) "Original icons" else "Tinted icons"),
                modifier = Modifier.testTag(TestTags.nav(CustomisePage.DRAWER.name)), selected = selected == CustomisePage.DRAWER,
            ) { go(CustomisePage.DRAWER) }
        }
        StudioSection("Appearance") {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StudioSegmented(UiMode.entries, settings.uiMode, ::uiModeLabel, { actions.onUiMode(it) }, tag = { TestTags.uiMode(it.name) })
                StudioHint("Light or dark for Versity's own screens. System follows your phone. The plain app drawer always follows your phone.", Modifier.padding(bottom = 6.dp))
            }
        }
        UpdatesSection(update, versionName, settings.autoUpdateCheck, actions::onAutoUpdateCheck, updateActions)
        StudioSection("Versity") {
            StudioRow(
                "Home app", glyph = Glyph.HOME,
                detail = if (isDefault) "Versity is your home app" else "Set Versity as your home app to see your clock and widgets there",
                chevron = !isDefault,
                onClick = if (isDefault) null else actions::onSetDefaultLauncher,
            )
            StudioRow(
                "About", glyph = Glyph.INFO, divider = true,
                modifier = Modifier.testTag(TestTags.nav(CustomisePage.ABOUT.name)), selected = selected == CustomisePage.ABOUT,
            ) { go(CustomisePage.ABOUT) }
            StudioRow(
                "Reset everything", glyph = Glyph.RESET, divider = true, chevron = false, titleColor = c.danger,
                modifier = Modifier.testTag(TestTags.RESET), onClick = onReset,
            )
        }
    }
}

// ---------------------------------------------------------------- Clock

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ClockPage(settings: LauncherSettings, actions: CustomiseActions) {
    Box(Modifier.size(1.dp).testTag(TestTags.PREVIEW).semantics { previewPalette = settings.paletteId })
    PageBody {
        StudioCard(title = "Face") {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 520.dp) 3 else 2
                val tileW = (maxWidth - 8.dp * (columns - 1)) / columns
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemePresets.clockStyles.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { style -> ClockTile(settings, style, tileW) { actions.onClockStyle(style) } }
                        }
                    }
                }
            }
        }
        if (settings.clockStyle == ClockStyle.ORNAMENT) {
            StudioCard(title = "Ornament") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Ornament.entries.forEach { o ->
                        StudioChip(ornamentLabel(o), settings.ornament == o, TestTags.ornament(o.name)) { actions.onOrnament(o) }
                    }
                }
                StudioSegmented(
                    listOf(false, true), settings.ornamentDate, { if (it) "Time and date" else "Time only" }, { actions.onOrnamentDate(it) },
                    tag = { TestTags.ORNAMENT_DATE + if (it) "_on" else "_off" },
                )
            }
        }
        StudioHint("Move or resize the clock in Layout and position. Themes set a clock too.", Modifier.padding(horizontal = 4.dp))
    }
}

/** One clock face at 11:11 on the current colours, as a choice. */
@Composable
private fun ClockTile(settings: LauncherSettings, style: ClockStyle, width: Dp, onClick: () -> Unit) {
    val c = Studio.colors
    val selected = settings.clockStyle == style
    val look = remember(settings.paletteId, settings.fontId, settings.accentArgb, settings.ornament, style) {
        settings.copy(clockStyle = style, ornamentDate = false, clockPlacement = null)
    }
    Column(
        Modifier
            .testTag(TestTags.clockStyle(style.name))
            .width(width)
            .clip(StudioCardCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = clockStyleLabel(style) },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppTheme(look) {
            val colors = LauncherTheme.colors
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
                    .border(if (selected) 2.dp else 1.dp, if (selected) c.brand else c.line, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                // Lay the face out at phone width, then scale it into the tile.
                val full = 300.dp
                val s = minOf(maxWidth * 0.86f / full, 1f)
                Box(Modifier.requiredSize(full, 96.dp / s).graphicsLayer { scaleX = s; scaleY = s }, contentAlignment = Alignment.Center) {
                    ClockFace(look, PreviewNow, is24Hour = true, modifier = Modifier.width(full))
                }
            }
        }
        StudioText(clockStyleLabel(style), StudioType.small, if (selected) c.brandText else c.ink, Modifier.padding(start = 2.dp), maxLines = 1)
    }
}

// ---------------------------------------------------------------- Colours and font

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColoursPage(settings: LauncherSettings, actions: CustomiseActions) {
    val c = Studio.colors
    // Sliders hold the finger position locally and save on release, so dragging never waits on disk.
    var dragDim by remember { mutableStateOf<Int?>(null) }
    var dragAccent by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(settings.dimPercent) { dragDim = null }
    LaunchedEffect(settings.accentArgb) { dragAccent = null }
    val live = settings.copy(dimPercent = dragDim ?: settings.dimPercent, accentArgb = dragAccent ?: settings.accentArgb)
    Box(Modifier.size(1.dp).testTag(TestTags.PREVIEW).semantics { previewPalette = live.paletteId })

    PageBody {
        PreviewStage {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val each = min((maxWidth - 16.dp) / 2, 112.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                    LabelledPreview("Home screen", true) { HomePreview(live, each, border = Color.Black.copy(alpha = 0.12f)) }
                    LabelledPreview("Lock screen", true) { LockPreview(live, each, border = Color.Black.copy(alpha = 0.12f)) }
                }
            }
        }
        StudioCard(title = "Palette") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemePresets.palettes.forEach { p ->
                    PaletteTile(p.name, p.backgroundTop, p.backgroundBottom, p.accent, settings.paletteId == p.id, TestTags.palette(p.id)) { actions.onPalette(p.id) }
                }
            }
        }
        StudioCard(title = "Accent") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioChip("From palette", settings.accentArgb == null, TestTags.accent(null)) { actions.onAccent(null) }
                AccentSwatches.forEach { argb -> Swatch(argb, settings.accentArgb == argb, TestTags.accent(argb)) { actions.onAccent(argb) } }
            }
            val current = live.accentArgb ?: ThemePresets.palette(live.paletteId).accent
            val hue = FloatArray(3).also { AndroidColor.colorToHSV(current.toInt(), it) }[0]
            val spectrum = remember { Brush.horizontalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 0.6f, 1f))) }) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioText("Custom", StudioType.body, modifier = Modifier.width(64.dp))
                StudioSlider(
                    value = hue,
                    onChange = { h -> dragAccent = AndroidColor.HSVToColor(floatArrayOf(h, 0.6f, 1f)).toLong() and 0xFFFFFFFFL },
                    onFinished = { actions.onAccent(dragAccent ?: settings.accentArgb) },
                    range = 0f..359f,
                    trackBrush = spectrum,
                    modifier = Modifier.weight(1f),
                    description = "Custom accent hue",
                )
            }
        }
        StudioCard(title = "Font") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemePresets.fonts.forEach { font ->
                    val family = remember(font.id) { fontFamilyFor(font) }
                    StudioChip(font.name, settings.fontId == font.id, TestTags.font(font.id), family = family) { actions.onFont(font.id) }
                }
            }
        }
        StudioCard(title = "Wallpaper dim") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioSlider(
                    value = live.dimPercent.toFloat(),
                    onChange = { dragDim = it.roundToInt() },
                    onFinished = { actions.onDim(dragDim ?: settings.dimPercent) },
                    range = LauncherSettings.MIN_DIM.toFloat()..LauncherSettings.MAX_DIM.toFloat(),
                    modifier = Modifier.weight(1f).testTag(TestTags.DIM_SLIDER),
                    description = "Dim, ${live.dimPercent} percent",
                )
                StudioText("${live.dimPercent}%", StudioType.small, c.muted, Modifier.width(44.dp).padding(start = 8.dp))
            }
            StudioHint("Darkens the wallpaper so the clock and labels stay easy to read.")
        }
    }
}

@Composable
private fun PaletteTile(name: String, top: Long, bottom: Long, accent: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val c = Studio.colors
    Column(
        Modifier
            .testTag(tag)
            .width(76.dp)
            .clip(StudioChipCorner)
            .background(if (selected) c.brandSoft else c.surface, StudioChipCorner)
            .border(1.dp, if (selected) c.brand else c.line, StudioChipCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(5.dp)
            .semantics { contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(5.dp)).background(Brush.verticalGradient(listOf(Color(top), Color(bottom))))) {
            Box(Modifier.align(Alignment.BottomStart).padding(5.dp).size(width = 14.dp, height = 3.dp).background(Color(accent), RoundedCornerShape(2.dp)))
        }
        StudioText(name, StudioType.caption.copy(fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal), if (selected) c.brandText else c.ink, maxLines = 1)
    }
}

@Composable
private fun Swatch(argb: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val c = Studio.colors
    Box(
        Modifier
            .testTag(tag)
            .size(36.dp)
            .clip(StudioChipCorner)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.brand else c.lineStrong, StudioChipCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(if (selected) 5.dp else 4.dp)
            .background(Color(argb), RoundedCornerShape(5.dp))
            .semantics { contentDescription = "Accent colour" },
    )
}

// ---------------------------------------------------------------- Layout and position

@Composable
internal fun LayoutPage(settings: LauncherSettings, actions: CustomiseActions, onArrange: () -> Unit) {
    val c = Studio.colors
    PageBody {
        StudioCard(title = "Layout") {
            StudioSegmented(HomeLayout.entries, settings.layout, ::layoutLabel, { actions.onLayout(it) }, tag = { "layout_${it.name}" })
            StudioHint(
                if (settings.layout == HomeLayout.MINIMAL) "The clock sits near the top, apps at the bottom."
                else "The date and clock sit in the middle of the screen.",
            )
        }
        StudioCard(title = "Clock and widget positions") {
            fun describe(p: com.versity.launcher.data.ClockPlacement?) = p?.let { "moved, ${(it.scale * 100).roundToInt()}% size" } ?: "follows the layout"
            StudioText("Home clock: ${describe(settings.clockPlacement)}", StudioType.body)
            StudioText("Lock clock: ${describe(settings.lockClockPlacement)}", StudioType.body)
            if (settings.homeWidgets.isNotEmpty()) {
                StudioText("${settings.homeWidgets.size} widget${if (settings.homeWidgets.size == 1) "" else "s"} on the home screen", StudioType.body)
            }
            StudioHint("Drag the clock and widgets anywhere and pinch to resize, with guides that snap to the centre and thirds.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudioButton("Arrange", Modifier.testTag(TestTags.EDIT_CLOCK), glyph = Glyph.MOVE) { onArrange() }
                if (settings.clockPlacement != null || settings.lockClockPlacement != null) {
                    StudioTextButton("Clocks back to layout", Modifier.testTag(TestTags.EDITOR_RESET), color = c.brandText) {
                        actions.onArrange(null, null, settings.homeWidgets)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Lock screen

@Composable
internal fun LockPage(
    settings: LauncherSettings,
    actions: CustomiseActions,
    lockWallpaperStatus: String?,
    onBrowseThemes: () -> Unit,
    onRequestLockWallpaper: () -> Unit,
) {
    val c = Studio.colors
    PageBody {
        PreviewStage {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LabelledPreview("Lock screen", true) { LockPreview(settings, min(maxWidth * 0.4f, 132.dp), border = Color.Black.copy(alpha = 0.12f)) }
            }
        }
        StudioSection("Look") {
            StudioRow("Theme", value = lockThemeLabel(settings), chevron = false)
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StudioButton("Browse themes", Modifier.testTag(TestTags.BROWSE_THEMES)) { onBrowseThemes() }
                if (settings.lockThemeId != null || settings.lockWallpaperUri != null) {
                    StudioTextButton("Match home screen", Modifier.testTag(TestTags.LOCK_FOLLOW_HOME), color = c.brandText) { actions.onLockFollowsHome() }
                }
            }
        }
        StudioHint("Open a theme or wallpaper and choose Lock to give the lock screen its own look.", Modifier.padding(horizontal = 4.dp))
        StudioSection("Themed lock screen") {
            StudioSwitchRow(
                "Show while locked", settings.lockStyleEnabled, TestTags.LOCK_TOGGLE,
                detail = "Appears over your lock screen and closes when you unlock with your PIN, fingerprint or face. Your phone's security doesn't change.",
            ) { actions.onLockStyle(it) }
            StudioRow("Preview", divider = true, modifier = Modifier.testTag(TestTags.PREVIEW_LOCK)) { actions.onPreviewLock() }
        }
        StudioSection("Phone lock screen") {
            StudioRow(
                "Set as lock-screen wallpaper",
                detail = lockWallpaperStatus ?: "Puts this background on your phone's own lock screen. Its clock stays on top.",
                modifier = Modifier.testTag(TestTags.SET_LOCK_WALLPAPER),
            ) { onRequestLockWallpaper() }
        }
    }
}

// ---------------------------------------------------------------- Drawer and icons

@Composable
internal fun DrawerPage(settings: LauncherSettings, actions: CustomiseActions) {
    PageBody {
        StudioCard(title = "App drawer") {
            StudioSegmented(
                DrawerStyle.entries, settings.drawerStyle,
                { if (it == DrawerStyle.PLAIN) "Plain" else "Match theme" }, { actions.onDrawerStyle(it) },
                tag = { TestTags.drawerStyle(it.name) },
            )
            StudioHint("Plain follows your phone: its light or dark mode and, on Android 12 and newer, its wallpaper colours. Themes never restyle it unless you choose Match theme.")
        }
        StudioCard(title = "App icons") {
            StudioSegmented(
                IconStyle.entries, settings.iconStyle,
                { if (it == IconStyle.ORIGINAL) "Original" else "Tint to theme" }, { actions.onIconStyle(it) },
                tag = { TestTags.iconStyle(it.name) },
            )
            StudioHint("Original keeps every app's own icon. Tint uses the app's themed icon where it has one.")
        }
        StudioHint("Swipe up on the home screen, or tap the grid in the dock, to see all apps. Long-press an app there to add it to the dock.", Modifier.padding(horizontal = 4.dp))
    }
}

// ---------------------------------------------------------------- About

@Composable
internal fun AboutPage(versionName: String) {
    val c = Studio.colors
    PageBody {
        StudioCard {
            StudioText("Versity", StudioType.heading)
            if (versionName.isNotBlank()) StudioText("Version $versionName", StudioType.small, c.muted)
            StudioText(
                "Themes, wallpapers, clocks and widgets for your home and lock screens. Versity goes online only to check GitHub for new versions, which you can turn off in Settings. Nothing you choose leaves your phone.",
                StudioType.body,
            )
        }
        StudioCard(title = "Fonts") {
            StudioHint(
                "Inter, Space Grotesk, Outfit, Roboto Mono, Cormorant Garamond, Playfair Display, Josefin Sans and Syne, under the SIL Open Font License 1.1. Licence texts are in the app's assets/licenses folder.",
            )
        }
    }
}

// ---------------------------------------------------------------- Full preview

/** Home and lock at a large size over a scrim. Tap anywhere to close. */
@Composable
internal fun FullPreview(settings: LauncherSettings, visible: Boolean, onClose: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(tween(StudioMotion.STANDARD)), exit = fadeOut(tween(StudioMotion.QUICK))) {
        BackHandler(onBack = onClose)
        val c = Studio.colors
        BoxWithConstraints(
            Modifier.fillMaxSize().background(c.scrim)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            val each = min((maxWidth - 48.dp) / 2, (maxHeight - 96.dp) * (360f / 780f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        HomePreview(settings, each, border = Color.White.copy(alpha = 0.4f))
                        StudioText("Home screen", StudioType.caption, Color.White)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LockPreview(settings, each, border = Color.White.copy(alpha = 0.4f))
                        StudioText("Lock screen", StudioType.caption, Color.White)
                    }
                }
                StudioText("Tap anywhere to close", StudioType.caption, Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
