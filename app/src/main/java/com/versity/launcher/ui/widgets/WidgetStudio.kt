package com.versity.launcher.ui.widgets

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.home.WallpaperLayer
import com.versity.launcher.ui.home.components.WidgetView
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioCard
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioCorner
import com.versity.launcher.ui.studio.StudioDivider
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioIconButton
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioSegmented
import com.versity.launcher.ui.studio.StudioSlider
import com.versity.launcher.ui.studio.StudioSwitch
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.widget.DateStyle
import com.versity.launcher.widget.FaceShape
import com.versity.launcher.widget.HandStyle
import com.versity.launcher.widget.TimeFormat
import com.versity.launcher.widget.WidgetAlign
import com.versity.launcher.widget.WidgetBackground
import com.versity.launcher.widget.WidgetCorner
import com.versity.launcher.widget.WidgetOrnament
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
import com.versity.launcher.widget.WidgetTypeface
import com.versity.launcher.widget.WidgetTypefaces

/** Text and accent colours: light inks for dark wallpapers first, then dark inks for light ones. */
val InkSwatches = listOf(
    0xFFFFFFFFL, 0xFFF4ECDCL, 0xFFDCEBFAL, 0xFFC9F2E1L, 0xFFF2D3A7L, 0xFFFF8A70L,
    0xFFF7A8C4L, 0xFFC7B8FFL, 0xFF8FA3FFL, 0xFF5B76F0L, 0xFF3A3A40L, 0xFF111114L,
)

/** Panel and face colours. */
val PanelSwatches = listOf(
    0xFF111114L, 0xFF1F2430L, 0xFF2B2024L, 0xFF0E2A2EL, 0xFF3E5BD8L, 0xFFE8D5B5L, 0xFFF4F1EAL, 0xFFFFFFFFL,
)

/** Where the preview sits: on the user's wallpaper, or on plain light or dark to check contrast. */
private enum class Stage(val label: String) { WALLPAPER("Wallpaper"), LIGHT("Light"), DARK("Dark") }

/** Preview sizes in home-screen cells. */
private enum class CellSize(val label: String, val cols: Int, val rows: Int) {
    S("2×1", 2, 1), M("2×2", 2, 2), L("4×2", 4, 2), XL("4×3", 4, 3);

    val widthDp: Int get() = cols * 80
    val heightDp: Int get() = rows * 88
}

private fun defaultCell(spec: WidgetSpec): CellSize = if (spec.aspect >= 1.6f) CellSize.L else CellSize.M

/**
 * The widget studio: a live preview over the user's wallpaper and every option for one widget. Used in
 * Customise (new and existing widgets) and by home screens when a widget is placed or re-edited, so the
 * look is always designed in one place.
 */
@Composable
fun WidgetStudio(
    initial: WidgetSpec,
    title: String,
    confirmLabel: String,
    wallpaperUri: String?,
    onConfirm: (WidgetSpec) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    destructive: Pair<String, () -> Unit>? = null,
    compactConfirmLabel: String? = null,
) {
    val c = Studio.colors
    var spec by remember(initial) { mutableStateOf(initial) }
    BackHandler(onBack = onClose)
    Column(
        modifier
            .fillMaxSize()
            .testTag(TestTags.WIDGET_STUDIO)
            .background(c.page)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudioIconButton("Close", onClick = onClose) { StudioGlyph(Glyph.CLOSE, 18.dp, c.ink) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                StudioText(title, StudioType.title, maxLines = 1, modifier = Modifier.semantics { heading() })
                StudioText(spec.type.title, StudioType.caption, c.muted, maxLines = 1)
            }
            // Narrow phones get the short label, so the title keeps its room.
            val narrow = LocalConfiguration.current.screenWidthDp < 400
            StudioPrimaryButton(if (narrow) compactConfirmLabel ?: confirmLabel else confirmLabel, Modifier.testTag(TestTags.STUDIO_CONFIRM)) { onConfirm(spec) }
        }
        StudioDivider()
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxHeight().padding(20.dp)) {
                        PreviewPane(spec, wallpaperUri, Modifier.fillMaxSize())
                    }
                    Column(
                        Modifier.width(420.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { Controls(spec, { spec = it }, message, destructive) }
                }
            } else {
                val previewHeight = min(maxHeight * 0.46f, 340.dp)
                Column(Modifier.fillMaxSize()) {
                    PreviewPane(spec, wallpaperUri, Modifier.fillMaxWidth().height(previewHeight).padding(12.dp))
                    StudioDivider()
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { Controls(spec, { spec = it }, message, destructive) }
                }
            }
        }
    }
}

@Composable
private fun PreviewPane(spec: WidgetSpec, wallpaperUri: String?, modifier: Modifier) {
    val c = Studio.colors
    var stage by rememberSaveable { mutableStateOf(Stage.WALLPAPER) }
    var cell by remember(spec.type, spec.aspect >= 1.6f) { mutableStateOf(defaultCell(spec)) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(StudioCardCorner)
                .border(1.dp, c.line, StudioCardCorner)
                .background(
                    when (stage) {
                        Stage.LIGHT -> Brush.verticalGradient(listOf(Color(0xFFF3F1EC), Color(0xFFE2DED6)))
                        Stage.DARK -> Brush.verticalGradient(listOf(Color(0xFF1C1D22), Color(0xFF0B0B0D)))
                        Stage.WALLPAPER -> Brush.verticalGradient(listOf(Color(0xFF1B3F66), Color(0xFF06142A)))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (stage == Stage.WALLPAPER) WallpaperLayer(wallpaperUri, Modifier.fillMaxSize())
            val w = cell.widthDp.dp
            val h = cell.heightDp.dp
            val scale = minOf(1f, (maxWidth - 24.dp) / w, (maxHeight - 24.dp) / h)
            Box(Modifier.size(w * scale, h * scale), contentAlignment = Alignment.Center) {
                Box(Modifier.requiredSize(w, h).graphicsLayer { scaleX = scale; scaleY = scale }) {
                    WidgetView(spec, Modifier.fillMaxSize())
                }
            }
        }
        // Side by side when there is room; stacked on narrow phones so neither control is cramped.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (maxWidth >= 440.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { StudioSegmented(CellSize.entries, cell, { it.label }, { cell = it }) }
                    Box(Modifier.weight(1f)) { StudioSegmented(Stage.entries, stage, { it.label }, { stage = it }) }
                }
            } else {
                Column {
                    StudioSegmented(CellSize.entries, cell, { it.label }, { cell = it })
                    StudioSegmented(Stage.entries, stage, { it.label }, { stage = it })
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controls(spec: WidgetSpec, set: (WidgetSpec) -> Unit, message: String?, destructive: Pair<String, () -> Unit>?) {
    val c = Studio.colors
    if (message != null) {
        StudioText(message, StudioType.small, c.ink, Modifier.fillMaxWidth().background(c.brandSoft, StudioCorner).padding(12.dp))
    }

    StudioCard(title = "Layout") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WidgetStyle.of(spec.type).forEach { style -> StyleTile(spec, style) { set(spec.copy(style = style)) } }
        }
    }

    if (spec.usesTypeface) {
        StudioCard(title = "Typeface") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                WidgetTypefaces.all.forEach { tf ->
                    val family = remember(tf.id) { familyFor(tf) }
                    StudioChip(tf.name, spec.typeface == tf.id, TestTags.widgetFont(tf.id), family = family) { set(spec.copy(typeface = tf.id)) }
                }
            }
        }
    }

    StudioCard(title = "Colour") {
        StudioLabel(if (spec.type == WidgetType.ANALOGUE) "Dial and hands" else "Text")
        Swatches(InkSwatches, spec.textColor) { set(spec.copy(textColor = it)) }
        StudioLabel(
            when (spec.type) {
                WidgetType.ANALOGUE -> "Seconds hand and accents"
                WidgetType.DATE -> "Accent: weekday and today"
                else -> "Accent: ornament and highlights"
            },
        )
        Swatches(InkSwatches, spec.accentColor) { set(spec.copy(accentColor = it)) }
        HueSlider(spec.accentColor) { set(spec.copy(accentColor = it)) }
        val twoToneLabel = when (spec.type) {
            WidgetType.ANALOGUE -> "Minute hand in the accent"
            WidgetType.DATE -> if (spec.style == WidgetStyle.DATE_LINE) "Date in the accent" else "Day number in the accent"
            else -> "Minutes in the accent"
        }
        if (spec.style != WidgetStyle.MONTH) {
            ToggleLine(twoToneLabel, spec.twoTone) { set(spec.copy(twoTone = it)) }
        }
        if (spec.type == WidgetType.ANALOGUE && Build.VERSION.SDK_INT < 31) {
            StudioHint("On this Android version the hands are white or near-black, whichever suits the text colour. The dial uses your exact colours.")
        }
    }

    StudioCard(title = "Background") {
        StudioSegmented(
            WidgetBackground.entries, spec.background, { it.label }, { set(spec.copy(background = it)) },
            tag = { TestTags.widgetBackground(it.name) },
        )
        if (spec.background == WidgetBackground.SOLID) {
            StudioLabel("Panel colour")
            Swatches(PanelSwatches, spec.backgroundColor) { set(spec.copy(backgroundColor = it)) }
        }
        if (spec.type == WidgetType.ANALOGUE) {
            ToggleLine("Filled face behind the dial", spec.dialFace) { set(spec.copy(dialFace = it)) }
            if (spec.dialFace) {
                StudioSegmented(FaceShape.entries, spec.faceShape, { it.label }, { set(spec.copy(faceShape = it)) })
                if (spec.background != WidgetBackground.SOLID) {
                    StudioLabel("Face colour")
                    Swatches(PanelSwatches, spec.backgroundColor) { set(spec.copy(backgroundColor = it)) }
                }
            }
        }
        val hasFill = spec.background != WidgetBackground.NONE || (spec.type == WidgetType.ANALOGUE && spec.dialFace)
        if (hasFill) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudioText("Opacity", StudioType.body, modifier = Modifier.width(72.dp))
                StudioSlider(
                    spec.opacity.toFloat(), { set(spec.copy(opacity = it.toInt())) }, 0f..100f,
                    Modifier.weight(1f), description = "Opacity, ${spec.opacity} percent",
                )
                StudioText("${spec.opacity}%", StudioType.small, c.muted, Modifier.width(44.dp).padding(start = 8.dp))
            }
        }
        if (spec.background != WidgetBackground.NONE || spec.border) {
            StudioLabel("Corners")
            StudioSegmented(WidgetCorner.entries, spec.corner, { it.label }, { set(spec.copy(corner = it)) })
        }
        ToggleLine("Hairline edge", spec.border) { set(spec.copy(border = it)) }
    }

    val showsAlign = spec.type != WidgetType.ANALOGUE
    if (showsAlign || spec.showsTime || spec.showsDate || spec.usesOrnament || spec.type == WidgetType.ANALOGUE) {
        StudioCard(title = if (spec.type == WidgetType.ANALOGUE) "Hands" else "Text and time") {
            if (showsAlign) {
                StudioLabel("Alignment")
                StudioSegmented(WidgetAlign.entries, spec.align, { it.label }, { set(spec.copy(align = it)) })
                if (Build.VERSION.SDK_INT < 31 && spec.style != WidgetStyle.MONTH) {
                    StudioHint("On this Android version text stays centred on the home screen.")
                }
            }
            if (spec.showsTime) {
                StudioLabel("Clock")
                StudioSegmented(TimeFormat.entries, spec.timeFormat, { it.label }, { set(spec.copy(timeFormat = it)) })
            }
            if (spec.showsDate && spec.style != WidgetStyle.DATE_LINE) {
                StudioLabel("Date")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DateStyle.entries.forEach { d -> StudioChip(d.label, spec.dateStyle == d, "date_${d.name}") { set(spec.copy(dateStyle = d)) } }
                }
            }
            if (spec.usesOrnament) {
                StudioLabel("Ornament")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WidgetOrnament.entries.forEach { o -> StudioChip(o.label, spec.ornament == o, "ornament_${o.name}") { set(spec.copy(ornament = o)) } }
                }
            }
            if (spec.type == WidgetType.ANALOGUE) {
                StudioLabel("Hand style")
                StudioSegmented(HandStyle.entries, spec.hands, { it.label }, { set(spec.copy(hands = it)) }, tag = { "hands_${it.name}" })
                if (Build.VERSION.SDK_INT >= 31) {
                    ToggleLine("Seconds hand", spec.seconds) { set(spec.copy(seconds = it)) }
                } else {
                    StudioHint("A seconds hand needs Android 12 or newer.")
                }
            }
        }
    }

    if (destructive != null) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            StudioTextButton(destructive.first, color = c.danger, onClick = destructive.second)
        }
    }
    Spacer(Modifier.height(24.dp))
}

/** A style choice drawn as the real widget, small, on a neutral panel. */
@Composable
private fun StyleTile(spec: WidgetSpec, style: WidgetStyle, onClick: () -> Unit) {
    val c = Studio.colors
    val selected = spec.style == style
    val preview = remember(spec, style) {
        // Bare text on a dark tile reads best; keep the user's colours so the tile matches the result.
        spec.copy(style = style, background = WidgetBackground.NONE, border = false)
    }
    Column(
        Modifier
            .testTag(TestTags.widgetStyle(style.name))
            .width(104.dp)
            .clip(StudioCardCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = style.label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2D36), Color(0xFF141519))))
                .border(if (selected) 2.dp else 1.dp, if (selected) c.brand else c.line, RoundedCornerShape(10.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            val (w, h) = if (preview.aspect >= 1.6f) 180.dp to 80.dp else 90.dp to 90.dp
            val s = minOf(92.dp / w, 56.dp / h)
            Box(Modifier.size(w * s, h * s)) {
                Box(Modifier.requiredSize(w, h).graphicsLayer { scaleX = s; scaleY = s }) {
                    WidgetView(preview.copy(textColor = legible(preview.textColor), accentColor = legible(preview.accentColor)), Modifier.fillMaxSize())
                }
            }
        }
        StudioText(style.label, StudioType.small, if (selected) c.brandText else c.ink, maxLines = 1)
    }
}

/** Dark inks would vanish on the dark style tiles, so tiles show them light. */
private fun legible(argb: Long): Long = if (com.versity.launcher.widget.WidgetRenderer.luminance(argb) < 0.35) 0xFFF2F2F4 else argb

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Swatches(options: List<Long>, selected: Long, onPick: (Long) -> Unit) {
    val c = Studio.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { argb ->
            val on = (argb and 0xFFFFFFFFL) == (selected and 0xFFFFFFFFL)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .border(if (on) 2.dp else 1.dp, if (on) c.brand else c.lineStrong, RoundedCornerShape(9.dp))
                    .selectable(selected = on, role = Role.RadioButton) { onPick(argb) }
                    .padding(if (on) 4.dp else 3.dp)
                    .background(Color(argb), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .semantics { contentDescription = "Colour #%06X".format(argb and 0xFFFFFF) },
            )
        }
    }
}

@Composable
private fun HueSlider(current: Long, onPick: (Long) -> Unit) {
    val hsv = FloatArray(3).also { AndroidColor.colorToHSV(current.toInt(), it) }
    val spectrum = remember { Brush.horizontalGradient((0..6).map { Color(AndroidColor.HSVToColor(floatArrayOf(it * 60f, 0.55f, 1f))) }) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        StudioText("Custom", StudioType.body, modifier = Modifier.width(72.dp))
        StudioSlider(
            value = hsv[0],
            onChange = { h -> onPick(AndroidColor.HSVToColor(floatArrayOf(h, 0.55f, 1f)).toLong() and 0xFFFFFFFFL) },
            range = 0f..359f,
            trackBrush = spectrum,
            modifier = Modifier.weight(1f),
            description = "Custom accent hue",
        )
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(StudioCorner)
            .selectable(selected = checked, role = Role.Switch) { onChange(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StudioText(label, StudioType.body, modifier = Modifier.weight(1f))
        StudioSwitch(checked)
    }
}

/** Compose family for a widget typeface: the bundled font at the widget's weight. */
@OptIn(ExperimentalTextApi::class)
fun familyFor(tf: WidgetTypeface): FontFamily = FontFamily(
    Font(
        resId = ThemePresets.font(tf.fontId).resId,
        weight = FontWeight(tf.weight.coerceIn(100, 900)),
        variationSettings = FontVariation.Settings(FontVariation.weight(tf.weight)),
    ),
)
