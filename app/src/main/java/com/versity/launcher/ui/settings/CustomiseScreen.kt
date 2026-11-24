package com.versity.launcher.ui.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import com.versity.launcher.ui.backdrop.drawBackdrop
import com.versity.launcher.ui.home.HomeBackground
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.Backdrop
import com.versity.launcher.data.ClockStyle
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.home.WallpaperLayer
import com.versity.launcher.ui.home.components.BatteryBar
import com.versity.launcher.ui.home.components.BigAnalogueClock
import com.versity.launcher.ui.home.components.SingleLineClock
import com.versity.launcher.ui.home.components.StackedClock
import com.versity.launcher.ui.previewPalette
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.fontFamilyFor
import java.time.LocalTime

interface CustomiseActions {
    fun onPickWallpaper() {}
    fun onClearWallpaper() {}
    fun onDim(percent: Int) {}
    fun onClockStyle(style: ClockStyle) {}
    fun onFont(id: String) {}
    fun onPalette(id: String) {}
    fun onAccent(argb: Long?) {}
    fun onTheme(theme: ThemeBundle) {}
    fun onLayout(layout: HomeLayout) {}
    fun onLockStyle(enabled: Boolean) {}
    fun onBackdrop(backdrop: Backdrop) {}
    fun onPreviewLock() {}
    fun onSetLockWallpaper() {}
    fun onReset() {}
}

private val Corner = RoundedCornerShape(2.dp)
private val PreviewTime: LocalTime = LocalTime.of(4, 22)
private val AccentSwatches = listOf(
    0xFF8FA3FFL, 0xFF4FC3E8L, 0xFF5ED6A0L, 0xFFFFD166L,
    0xFFFF8A5CL, 0xFFFF6B8BL, 0xFFC792EAL, 0xFFE8EAF2L,
)
private fun backdropLabel(b: Backdrop) = when (b) {
    Backdrop.NONE -> "None"
    Backdrop.ORBIT -> "Orbit"
    Backdrop.RINGS -> "Rings"
    Backdrop.HORIZON -> "Horizon"
    Backdrop.WAVES -> "Waves"
}

private fun layoutLabel(l: HomeLayout) = when (l) {
    HomeLayout.CLASSIC -> "Classic"
    HomeLayout.MINIMAL -> "Minimal"
    HomeLayout.ORBIT -> "Orbit"
}
private fun ThemeBundle.matches(s: LauncherSettings) =
    s.paletteId == paletteId && s.fontId == fontId && s.clockStyle == clockStyle &&
        s.dimPercent == dimPercent && s.accentArgb == accentArgb && s.layout == layout && s.backdrop == backdrop

private fun clockStyleLabel(s: ClockStyle) = when (s) {
    ClockStyle.STACKED -> "Stacked"
    ClockStyle.SINGLE_LINE -> "Single line"
    ClockStyle.ANALOGUE -> "Analogue"
}

@Composable
fun CustomiseContent(
    settings: LauncherSettings,
    actions: CustomiseActions,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    lockWallpaperStatus: String? = null,
) = AppTheme(settings) {
    val colors = LauncherTheme.colors
    // Sliders hold the finger position locally and save on release, so dragging never waits on disk.
    // The preview reads the live value, so it still follows the finger.
    var dragDim by remember { mutableStateOf<Int?>(null) }
    var dragAccent by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(settings.dimPercent) { dragDim = null }
    LaunchedEffect(settings.accentArgb) { dragAccent = null }
    var showLockDialog by remember { mutableStateOf(false) }
    val live = settings.copy(dimPercent = dragDim ?: settings.dimPercent, accentArgb = dragAccent ?: settings.accentArgb)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.backgroundBottom)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        TopBar(onBack, actions::onReset)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 720.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Preview(live, Modifier.fillMaxWidth().heightIn(max = 560.dp).fillMaxHeight(0.8f))
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        Controls(settings, actions, live, { dragDim = it }, { dragAccent = it }, { showLockDialog = true }, lockWallpaperStatus)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Preview(live, Modifier.fillMaxWidth().height(220.dp))
                    Controls(settings, actions, live, { dragDim = it }, { dragAccent = it }, { showLockDialog = true }, lockWallpaperStatus)
                }
            }
        }
    }
    if (showLockDialog) {
        LockWallpaperDialog(
            onConfirm = { showLockDialog = false; actions.onSetLockWallpaper() },
            onCancel = { showLockDialog = false },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onReset: () -> Unit) {
    val colors = LauncherTheme.colors
    Row(
        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clickable(onClick = onBack)
                .semantics { contentDescription = "Back"; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(18.dp)) {
                val w = size.width
                val sw = 1.5.dp.toPx()
                drawLine(colors.onSurface, Offset(w * 0.65f, w * 0.15f), Offset(w * 0.3f, w * 0.5f), strokeWidth = sw)
                drawLine(colors.onSurface, Offset(w * 0.3f, w * 0.5f), Offset(w * 0.65f, w * 0.85f), strokeWidth = sw)
            }
        }
        Text("Customise", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(
            Modifier.testTag(TestTags.RESET).heightIn(min = 48.dp).clickable(onClick = onReset)
                .padding(horizontal = 12.dp).semantics { role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Text("Reset", color = colors.accent, fontFamily = LauncherTheme.font, fontSize = 13.sp)
        }
    }
}

/** Miniature of the home screen: shared background (wallpaper, dim, backdrop), clock in the chosen style, battery bar. */
@Composable
private fun Preview(settings: LauncherSettings, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    val centered = settings.layout == HomeLayout.ORBIT
    Box(
        modifier
            .testTag(TestTags.PREVIEW)
            .semantics { previewPalette = settings.paletteId }
            .clip(Corner)
            .border(1.dp, colors.border, Corner),
    ) {
        HomeBackground(settings)
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            when (settings.clockStyle) {
                ClockStyle.STACKED -> StackedClock(PreviewTime, true, Modifier.fillMaxWidth(), sizeRange = 28f..40f, centered = centered)
                ClockStyle.SINGLE_LINE -> SingleLineClock(PreviewTime, true, Modifier.fillMaxWidth(), sizeRange = 28f..38f, centered = centered)
                ClockStyle.ANALOGUE -> BigAnalogueClock(PreviewTime, Modifier.size(84.dp))
            }
            BatteryBar(72, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Controls(
    settings: LauncherSettings,
    actions: CustomiseActions,
    live: LauncherSettings,
    onDimDrag: (Int?) -> Unit,
    onAccentDrag: (Long?) -> Unit,
    onRequestLockWallpaper: () -> Unit,
    lockWallpaperStatus: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Section("Themes") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ThemePresets.themes, key = { it.id }) { theme ->
                    ThemeCard(theme, theme.matches(settings)) { actions.onTheme(theme) }
                }
            }
            Hint("A theme sets palette, font, clock, dim, accent, layout and backdrop. Your wallpaper and home apps stay.")
        }
        Section("Wallpaper") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Choice("Choose from device", false, TestTags.PICK_WALLPAPER) { actions.onPickWallpaper() }
                if (settings.wallpaperUri != null) {
                    Choice("Clear", false, TestTags.CLEAR_WALLPAPER) { actions.onClearWallpaper() }
                }
            }
            Hint(if (settings.wallpaperUri == null) "Using the default background" else "Custom wallpaper")
        }
        Section("Dim") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThinSlider(
                    value = live.dimPercent.toFloat(),
                    onChange = { onDimDrag(it.toInt()) },
                    onFinished = { actions.onDim(live.dimPercent) },
                    range = LauncherSettings.MIN_DIM.toFloat()..LauncherSettings.MAX_DIM.toFloat(),
                    modifier = Modifier.weight(1f).testTag(TestTags.DIM_SLIDER),
                )
                Text("${live.dimPercent}%", color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp, modifier = Modifier.width(40.dp))
            }
        }
        Section("Clock style") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePresets.clockStyles.forEach { style ->
                    Choice(clockStyleLabel(style), settings.clockStyle == style, TestTags.clockStyle(style.name)) { actions.onClockStyle(style) }
                }
            }
        }
        Section("Layout") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HomeLayout.entries.forEach { l ->
                    Choice(layoutLabel(l), settings.layout == l, "layout_${l.name}") { actions.onLayout(l) }
                }
            }
        }
        Section("Backdrop") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Backdrop.entries.forEach { b ->
                    Choice(backdropLabel(b), settings.backdrop == b, "backdrop_option_${b.name}") { actions.onBackdrop(b) }
                }
            }
        }
        Section("Colours") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePresets.palettes.forEach { p ->
                    PaletteTile(p.name, p.backgroundTop, p.backgroundBottom, p.accent, settings.paletteId == p.id, TestTags.palette(p.id)) {
                        actions.onPalette(p.id)
                    }
                }
            }
        }
        Section("Accent") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Palette", settings.accentArgb == null, TestTags.accent(null)) { actions.onAccent(null) }
                AccentSwatches.forEach { argb ->
                    Swatch(argb, settings.accentArgb == argb, TestTags.accent(argb)) { actions.onAccent(argb) }
                }
            }
            Spacer(Modifier.height(8.dp))
            HueSlider(live, actions, onAccentDrag)
        }
        Section("Font") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePresets.fonts.forEach { font ->
                    val family = remember(font.id) { fontFamilyFor(font) }
                    Choice(font.name, settings.fontId == font.id, TestTags.font(font.id), family = family) { actions.onFont(font.id) }
                }
            }
        }
        Section("Lock-style screen") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Off", !settings.lockStyleEnabled, TestTags.LOCK_TOGGLE + "_off") { actions.onLockStyle(false) }
                Choice("On", settings.lockStyleEnabled, TestTags.LOCK_TOGGLE + "_on") { actions.onLockStyle(true) }
            }
            Choice("Preview lock screen", false, TestTags.PREVIEW_LOCK) { actions.onPreviewLock() }
            Hint("A slide-up screen shown when you open the launcher after the screen turns off. It is cosmetic: it does not lock your phone.")
            Spacer(Modifier.height(4.dp))
            Choice("Set as system lock-screen wallpaper", false, TestTags.SET_LOCK_WALLPAPER) { onRequestLockWallpaper() }
            Hint(lockWallpaperStatus ?: "Applies this theme's background to your real lock screen. Your phone's own clock stays on top.")
        }
        Section("About") {
            Hint("Fonts: Inter, Space Grotesk, Outfit and Roboto Mono, licensed under the SIL Open Font License 1.1. Licence texts are bundled in the app's assets/licenses folder.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 11.sp, letterSpacing = 0.8.sp)
        content()
    }
}

@Composable
private fun Hint(text: String) =
    Text(text, color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)

@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    tag: String,
    family: FontFamily? = null,
    onClick: () -> Unit,
) {
    val colors = LauncherTheme.colors
    Box(
        Modifier
            .testTag(tag)
            .heightIn(min = 40.dp)
            .border(1.dp, if (selected) colors.accent else colors.border, Corner)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp)
            .semantics { this.selected = selected; role = Role.RadioButton },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) colors.accent else colors.onSurface,
            fontFamily = family ?: LauncherTheme.font,
            fontSize = 13.sp,
        )
    }
}

/** Gallery card: a 9:16 mini screen with the theme's gradient, backdrop and clock, then its name and tagline. */
@Composable
private fun ThemeCard(theme: ThemeBundle, selected: Boolean, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    val palette = ThemePresets.palette(theme.paletteId)
    val accent = Color(theme.accentArgb ?: palette.accent)
    val family = remember(theme.fontId) { fontFamilyFor(ThemePresets.font(theme.fontId)) }
    Column(
        Modifier
            .testTag(TestTags.theme(theme.id))
            .width(104.dp)
            .clickable(onClick = onClick)
            .semantics { this.selected = selected; role = Role.RadioButton },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .border(1.dp, if (selected) colors.accent else colors.border, Corner)
                .background(Brush.verticalGradient(listOf(Color(palette.backgroundTop), Color(palette.backgroundBottom)))),
        ) {
            Canvas(Modifier.fillMaxSize()) { drawBackdrop(theme.backdrop, palette, accent) }
            Text(
                "04:22",
                color = Color(palette.onSurface),
                fontFamily = family,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
            )
            Box(Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 52.dp).size(width = 22.dp, height = 2.dp).background(accent))
        }
        Text(theme.name, color = if (selected) colors.accent else colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(theme.tagline, color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

/** Plain rectangular confirmation (Material's AlertDialog has a 28dp shape that clashes with the rest of the UI). */
@Composable
private fun LockWallpaperDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = LauncherTheme.colors
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.fillMaxWidth().background(colors.surface, Corner).border(1.dp, colors.border, Corner).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Replace your lock-screen wallpaper?", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                "Your current lock-screen wallpaper will be replaced with this theme's background. " +
                    "Your phone's own clock and unlock screen stay on top. Your home screen is not changed.",
                color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 13.sp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)) {
                DialogAction("Cancel", TestTags.LOCK_WALLPAPER_CANCEL, colors.muted, onCancel)
                DialogAction("Replace", TestTags.LOCK_WALLPAPER_CONFIRM, colors.accent, onConfirm)
            }
        }
    }
}

@Composable
private fun DialogAction(label: String, tag: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.testTag(tag).heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 14.dp).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = color, fontFamily = LauncherTheme.font, fontSize = 13.sp) }
}

@Composable
private fun PaletteTile(name: String, top: Long, bottom: Long, accent: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Column(
        Modifier
            .testTag(tag)
            .width(84.dp)
            .border(1.dp, if (selected) colors.accent else colors.border, Corner)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .semantics { this.selected = selected; role = Role.RadioButton; contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(36.dp).background(Brush.verticalGradient(listOf(Color(top), Color(bottom))))) {
            Box(Modifier.align(Alignment.BottomStart).padding(4.dp).size(width = 16.dp, height = 3.dp).background(Color(accent)))
        }
        Text(name, color = if (selected) colors.accent else colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 12.sp)
    }
}

@Composable
private fun Swatch(argb: Long, selected: Boolean, tag: String, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Box(
        Modifier
            .testTag(tag)
            .size(40.dp)
            .border(1.dp, if (selected) colors.onSurface else colors.border, Corner)
            .padding(5.dp)
            .background(Color(argb))
            .clickable(onClick = onClick)
            .semantics { this.selected = selected; role = Role.RadioButton; contentDescription = "Accent colour" },
    )
}

@Composable
private fun HueSlider(settings: LauncherSettings, actions: CustomiseActions, onDrag: (Long?) -> Unit) {
    val current = settings.accentArgb ?: ThemePresets.palette(settings.paletteId).accent
    val hue = FloatArray(3).also { AndroidColor.colorToHSV(current.toInt(), it) }[0]
    val spectrum = remember { Brush.horizontalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 0.6f, 1f))) }) }
    Column {
        Text("Custom hue", color = LauncherTheme.colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)
        ThinSlider(
            value = hue,
            onChange = { h -> onDrag(AndroidColor.HSVToColor(floatArrayOf(h, 0.6f, 1f)).toLong() and 0xFFFFFFFFL) },
            onFinished = { actions.onAccent(settings.accentArgb) },
            range = 0f..359f,
            trackBrush = spectrum,
        )
    }
}

/** Slim slider: 2dp track, small rectangular thumb. Material's default is far heavier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThinSlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackBrush: Brush? = null,
    onFinished: () -> Unit = {},
) {
    val colors = LauncherTheme.colors
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        valueRange = range,
        modifier = modifier,
        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent, inactiveTrackColor = colors.border),
        thumb = { Box(Modifier.size(width = 8.dp, height = 20.dp).background(colors.accent, RoundedCornerShape(1.dp))) },
        track = { state ->
            val frac = (state.value - range.start) / (range.endInclusive - range.start)
            if (trackBrush != null) {
                Box(Modifier.fillMaxWidth().height(4.dp).background(trackBrush))
            } else {
                Box(Modifier.fillMaxWidth().height(2.dp).background(colors.border)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(colors.accent))
                }
            }
        },
    )
}
