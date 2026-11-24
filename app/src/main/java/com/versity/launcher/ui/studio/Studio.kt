package com.versity.launcher.ui.studio

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.ui.theme.LiningFigures
import com.versity.launcher.ui.theme.fontFamilyFor

/*
 * Studio: the launcher's own interface (hub, pages, widget studio, sheets, dialogs, plain drawer).
 * Clean and neutral: flat surfaces, 1dp hairlines instead of shadows, one cobalt accent, Inter.
 * Corners are modest (cards 12dp, controls 8dp). Every text token passes WCAG AA in both modes.
 */

@Immutable
data class StudioColors(
    val dark: Boolean,
    /** Screen background. */
    val page: Color,
    /** Recessed wells: preview stages, segmented tracks, image placeholders. */
    val panel: Color,
    /** Cards, sheets, dialogs, inputs. */
    val surface: Color,
    val ink: Color,
    val muted: Color,
    /** Placeholders and decorative marks only, never body text. */
    val faint: Color,
    val line: Color,
    val lineStrong: Color,
    /** Unchecked control outlines (3:1 against surface). */
    val control: Color,
    /** The one accent: primary buttons, selection, focus. */
    val brand: Color,
    /** Accent-coloured text and icons on page/surface. */
    val brandText: Color,
    val onBrand: Color,
    /** Tinted fill for selected states. */
    val brandSoft: Color,
    val danger: Color,
    val scrim: Color,
)

val StudioLight = StudioColors(
    dark = false,
    page = Color(0xFFF6F6F7), panel = Color(0xFFEEEEF0), surface = Color(0xFFFFFFFF),
    ink = Color(0xFF17171A), muted = Color(0xFF65656E), faint = Color(0xFFA3A3AB),
    line = Color(0xFFE6E6E9), lineStrong = Color(0xFFD5D5DA), control = Color(0xFF85858F),
    brand = Color(0xFF3E5BD8), brandText = Color(0xFF3550C8), onBrand = Color.White,
    brandSoft = Color(0xFFEEF1FC), danger = Color(0xFFD13B3B), scrim = Color(0x6617171A),
)

val StudioDark = StudioColors(
    dark = true,
    page = Color(0xFF0E0E10), panel = Color(0xFF1B1B1F), surface = Color(0xFF151518),
    ink = Color(0xFFEDEDF0), muted = Color(0xFF9E9EA7), faint = Color(0xFF6A6A73),
    line = Color(0xFF26262B), lineStrong = Color(0xFF34343B), control = Color(0xFF74747D),
    brand = Color(0xFF5B76F0), brandText = Color(0xFF9DB0FF), onBrand = Color.White,
    brandSoft = Color(0xFF1A2040), danger = Color(0xFFFF6B66), scrim = Color(0x99000000),
)

/** Motion: one signature curve for on-screen movement, plus decelerate in and accelerate out. */
object StudioMotion {
    val Signature = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val Enter = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Exit = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    const val QUICK = 150
    const val STANDARD = 250
    const val SLOW = 320
}

/** Cards 12dp, controls 8dp, chips 8dp. */
val StudioCardCorner = RoundedCornerShape(12.dp)
val StudioCorner = RoundedCornerShape(8.dp)
val StudioChipCorner = RoundedCornerShape(8.dp)

private val LocalStudioColors = staticCompositionLocalOf { StudioLight }
private val LocalStudioFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

object Studio {
    val colors: StudioColors @Composable get() = LocalStudioColors.current
    val font: FontFamily @Composable get() = LocalStudioFont.current
}

/** Resolves System/Light/Dark against the phone's setting. */
@Composable
fun studioIsDark(mode: UiMode): Boolean = when (mode) {
    UiMode.SYSTEM -> isSystemInDarkTheme()
    UiMode.LIGHT -> false
    UiMode.DARK -> true
}

/** Every token crossfades when the mode changes, so the switch reads as one calm change. */
@Composable
private fun animated(target: StudioColors): StudioColors {
    @Composable fun a(c: Color) = animateColorAsState(c, tween(StudioMotion.STANDARD), label = "studio").value
    return StudioColors(
        dark = target.dark, page = a(target.page), panel = a(target.panel), surface = a(target.surface),
        ink = a(target.ink), muted = a(target.muted), faint = a(target.faint), line = a(target.line),
        lineStrong = a(target.lineStrong), control = a(target.control), brand = a(target.brand),
        brandText = a(target.brandText), onBrand = a(target.onBrand), brandSoft = a(target.brandSoft),
        danger = a(target.danger), scrim = a(target.scrim),
    )
}

@Composable
fun StudioTheme(mode: UiMode, font: FontFamily? = null, content: @Composable () -> Unit) {
    val colors = animated(if (studioIsDark(mode)) StudioDark else StudioLight)
    val family = font ?: remember { fontFamilyFor(ThemePresets.font("inter")) }
    val scheme = if (colors.dark) {
        darkColorScheme(primary = colors.brand, background = colors.page, surface = colors.surface, onSurface = colors.ink, onSurfaceVariant = colors.muted, outline = colors.line)
    } else {
        lightColorScheme(primary = colors.brand, background = colors.page, surface = colors.surface, onSurface = colors.ink, onSurfaceVariant = colors.muted, outline = colors.line)
    }
    CompositionLocalProvider(
        LocalStudioColors provides colors,
        LocalStudioFont provides family,
        LocalTextStyle provides TextStyle(fontFamily = family, color = colors.ink, fontFeatureSettings = LiningFigures),
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

// ---------------------------------------------------------------- Type

object StudioType {
    val display = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val title = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.15).sp)
    val heading = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val cardTitle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
    val label = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.7.sp)
    val body = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
    val bodyStrong = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
    val small = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
    val caption = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
}

@Composable
fun StudioText(
    text: String,
    style: TextStyle,
    color: Color = Studio.colors.ink,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) = Text(
    text, style = style.copy(fontFamily = Studio.font, fontFeatureSettings = LiningFigures), color = color, modifier = modifier,
    maxLines = maxLines, minLines = minLines, overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
)

/** Small capitalised section label. */
@Composable
fun StudioLabel(text: String, modifier: Modifier = Modifier) =
    StudioText(text.uppercase(), StudioType.label, Studio.colors.muted, modifier)

@Composable
fun StudioHint(text: String, modifier: Modifier = Modifier) =
    StudioText(text, StudioType.small, Studio.colors.muted, modifier)

// ---------------------------------------------------------------- Surfaces

/** Flat surface with a hairline edge. */
fun Modifier.surface(colors: StudioColors, shape: androidx.compose.ui.graphics.Shape = StudioCardCorner): Modifier =
    this.background(colors.surface, shape).border(1.dp, colors.line, shape)

/** Settings card with an optional title. */
@Composable
fun StudioCard(modifier: Modifier = Modifier, title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = Studio.colors
    Column(
        modifier
            .fillMaxWidth()
            .surface(c)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) StudioText(title, StudioType.cardTitle, modifier = Modifier.semantics { heading() })
        content()
    }
}

/** A titled group of rows: label above, rows in one card divided by hairlines. */
@Composable
fun StudioSection(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Studio.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) StudioLabel(title, Modifier.padding(start = 4.dp).semantics { heading() })
        Column(Modifier.fillMaxWidth().surface(c).clip(StudioCardCorner)) { content() }
    }
}

@Composable
fun StudioDivider(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().height(1.dp).background(Studio.colors.line))

/**
 * Settings row: leading glyph, title with optional detail, trailing value or control, and a chevron
 * when it opens a page. 56dp minimum height; the whole row is the touch target.
 */
@Composable
fun StudioRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    glyph: Glyph? = null,
    value: String? = null,
    chevron: Boolean = true,
    divider: Boolean = false,
    titleColor: Color = Studio.colors.ink,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Studio.colors
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // The value never takes more than a third of the row, so the title keeps room on narrow phones.
        val valueMax = (maxWidth * 0.34f).coerceAtMost(180.dp)
        Column(Modifier.fillMaxWidth()) {
            if (divider) StudioDivider(Modifier.padding(start = if (glyph != null) 52.dp else 16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .semantics(mergeDescendants = true) { if (onClick != null) role = Role.Button },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (glyph != null) StudioGlyph(glyph, 20.dp, c.ink)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    StudioText(title, StudioType.body, titleColor, maxLines = 2)
                    if (detail != null) StudioText(detail, StudioType.small, c.muted, maxLines = 3)
                }
                if (value != null) StudioText(value, StudioType.small, c.muted, Modifier.widthIn(max = valueMax), maxLines = 2)
                trailing?.invoke()
                if (chevron && onClick != null) StudioGlyph(Glyph.CHEVRON, 14.dp, c.faint)
            }
        }
    }
}

// ---------------------------------------------------------------- Buttons

/** Solid accent button: 40dp inside a 48dp touch row. */
@Composable
fun StudioPrimaryButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Studio.colors
    Box(
        modifier
            .heightIn(min = 48.dp)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(40.dp)
                .clip(StudioCorner)
                .background(if (enabled) c.brand else c.panel)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { StudioText(label, StudioType.bodyStrong, if (enabled) c.onBrand else c.faint, maxLines = 1) }
    }
}

/** Outlined secondary button. */
@Composable
fun StudioButton(label: String, modifier: Modifier = Modifier, glyph: Glyph? = null, onClick: () -> Unit) {
    val c = Studio.colors
    Box(modifier.heightIn(min = 48.dp).semantics { role = Role.Button }, contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .height(40.dp)
                .clip(StudioCorner)
                .background(c.surface)
                .border(1.dp, c.lineStrong, StudioCorner)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (glyph != null) StudioGlyph(glyph, 16.dp, c.ink)
            StudioText(label, StudioType.bodyStrong, c.ink, maxLines = 1)
        }
    }
}

/** Borderless text button, for secondary actions in bars. */
@Composable
fun StudioTextButton(label: String, modifier: Modifier = Modifier, color: Color = Studio.colors.muted, onClick: () -> Unit) {
    Box(
        modifier.heightIn(min = 48.dp).clip(StudioCorner).clickable(onClick = onClick).padding(horizontal = 12.dp).semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { StudioText(label, StudioType.bodyStrong, color, maxLines = 1) }
}

/** Ghost icon button: a glyph with a 48dp touch target and a subtle ripple. */
@Composable
fun StudioIconButton(description: String, modifier: Modifier = Modifier, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        modifier
            .size(48.dp)
            .clip(StudioCorner)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 20.dp),
                onClick = onClick,
            )
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { icon() }
}

/** Sun in light mode, moon in dark; turns a little as it switches. */
@Composable
fun ThemeToggleIcon(dark: Boolean) {
    val turn by animateFloatAsState(if (dark) -30f else 0f, tween(StudioMotion.STANDARD, easing = StudioMotion.Signature), label = "turn")
    Box(Modifier.rotate(turn)) { StudioGlyph(if (dark) Glyph.MOON else Glyph.SUN, 18.dp, Studio.colors.ink) }
}

/** Back chevron for page headers. */
@Composable
fun BackGlyph() = StudioGlyph(Glyph.BACK, 18.dp, Studio.colors.ink)

// ---------------------------------------------------------------- Selection

/**
 * Segmented control: a recessed track with a flat raised thumb that slides between options.
 * Equal-width segments; 36dp visual height inside a 48dp touch row.
 */
@Composable
fun <T> StudioSegmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tag: (T) -> String = { "" },
) {
    val c = Studio.colors
    BoxWithConstraints(modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
        val count = options.size.coerceAtLeast(1)
        val inner = maxWidth - 6.dp
        val segment = inner / count
        val index = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(segment * index, tween(280, easing = StudioMotion.Signature), label = "seg")
        val thumbShape = RoundedCornerShape(6.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(c.panel, StudioCorner)
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .offset(x = x)
                    .width(segment)
                    .fillMaxHeight()
                    .background(if (c.dark) Color(0xFF2A2A30) else Color.White, thumbShape)
                    .border(1.dp, if (c.dark) Color(0xFF34343B) else c.line, thumbShape),
            )
            Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
                options.forEach { o ->
                    val on = o == selected
                    val tint by animateColorAsState(if (on) c.ink else c.muted, tween(StudioMotion.QUICK), label = "segText")
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(if (tag(o).isNotEmpty()) Modifier.testTag(tag(o)) else Modifier)
                            .selectable(selected = on, role = Role.Tab, onClick = { onSelect(o) }),
                        contentAlignment = Alignment.Center,
                    ) {
                        StudioText(label(o), if (on) StudioType.small.copy(fontWeight = FontWeight.Medium) else StudioType.small, tint, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Choice chip for wrapping option lists (styles, fonts, categories). */
@Composable
fun StudioChip(
    label: String,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    family: FontFamily? = null,
    onClick: () -> Unit,
) {
    val c = Studio.colors
    val border by animateColorAsState(if (selected) c.brand else c.lineStrong, tween(StudioMotion.QUICK), label = "chipBorder")
    val fill by animateColorAsState(if (selected) c.brandSoft else c.surface, tween(StudioMotion.QUICK), label = "chipFill")
    Box(
        modifier
            .testTag(tag)
            .heightIn(min = 36.dp)
            .clip(StudioChipCorner)
            .background(fill, StudioChipCorner)
            .border(1.dp, border, StudioChipCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = StudioType.small.copy(fontFamily = family ?: Studio.font, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, fontFeatureSettings = LiningFigures),
            color = if (selected) c.brandText else c.ink,
            maxLines = 1,
        )
    }
}

/** Checkbox row: 18dp box with a drawn tick, label and optional hint; the whole row toggles. */
@Composable
fun StudioCheckRow(label: String, hint: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val c = Studio.colors
    val fill by animateColorAsState(if (checked) c.brand else c.surface, tween(StudioMotion.QUICK), label = "checkFill")
    val edge by animateColorAsState(if (checked) c.brand else c.control, tween(StudioMotion.QUICK), label = "checkEdge")
    val tick by animateFloatAsState(if (checked) 1f else 0f, tween(StudioMotion.QUICK), label = "tick")
    val box = RoundedCornerShape(5.dp)
    Row(
        Modifier
            .testTag(tag)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(StudioCorner)
            .clickable { onChange(!checked) }
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { role = Role.Checkbox; stateDescription = if (checked) "Checked" else "Not checked" },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = if (hint == null) Alignment.CenterVertically else Alignment.Top,
    ) {
        Box(
            Modifier.padding(top = if (hint == null) 0.dp else 1.dp).size(18.dp)
                .background(fill, box).border(1.5.dp, edge, box),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(10.dp)) {
                if (tick > 0f) {
                    val p = Path().apply {
                        moveTo(size.width * 0.1f, size.height * 0.54f)
                        lineTo(size.width * 0.4f, size.height * 0.82f)
                        lineTo(size.width * 0.92f, size.height * 0.2f)
                    }
                    drawPath(p, c.onBrand.copy(alpha = tick), style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StudioText(label, StudioType.body)
            if (hint != null) StudioText(hint, StudioType.small, c.muted)
        }
    }
}

/** Slim switch: 36x20 track, 16dp knob. */
@Composable
fun StudioSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val c = Studio.colors
    val track by animateColorAsState(if (checked) c.brand else c.lineStrong, tween(StudioMotion.QUICK), label = "switchTrack")
    val x by animateDpAsState(if (checked) 18.dp else 2.dp, tween(StudioMotion.STANDARD, easing = StudioMotion.Signature), label = "switchKnob")
    Box(modifier.size(width = 36.dp, height = 20.dp).background(track, RoundedCornerShape(10.dp))) {
        Box(Modifier.offset(x = x, y = 2.dp).size(16.dp).background(Color.White, RoundedCornerShape(8.dp)))
    }
}

/** A settings row that toggles a switch; the whole row is the target. */
@Composable
fun StudioSwitchRow(
    title: String,
    checked: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    glyph: Glyph? = null,
    divider: Boolean = false,
    onChange: (Boolean) -> Unit,
) {
    val c = Studio.colors
    Column(modifier.fillMaxWidth()) {
        if (divider) StudioDivider(Modifier.padding(start = if (glyph != null) 52.dp else 16.dp))
        Row(
            Modifier
                .testTag(tag)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (glyph != null) StudioGlyph(glyph, 20.dp, c.ink)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                StudioText(title, StudioType.body, maxLines = 2)
                if (detail != null) StudioText(detail, StudioType.small, c.muted)
            }
            StudioSwitch(checked)
        }
    }
}

/** Small tinted tag, for "Home" / "Lock" markers. */
@Composable
fun StudioTag(text: String, modifier: Modifier = Modifier) {
    val c = Studio.colors
    StudioText(
        text, StudioType.caption.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp), c.brandText,
        modifier.background(c.brandSoft, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
        maxLines = 1,
    )
}

/** Slim slider: 3dp track, flat 18dp thumb, 48dp touch height. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    trackBrush: Brush? = null,
    onFinished: () -> Unit = {},
    description: String? = null,
) {
    val c = Studio.colors
    Slider(
        value = value.coerceIn(range.start, range.endInclusive),
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        valueRange = range,
        modifier = modifier.heightIn(min = 48.dp).then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        colors = SliderDefaults.colors(thumbColor = c.brand, activeTrackColor = c.brand, inactiveTrackColor = c.line),
        thumb = {
            Box(
                Modifier.size(18.dp)
                    .background(Color.White, RoundedCornerShape(9.dp))
                    .border(1.dp, if (c.dark) c.lineStrong else Color(0xFFCBCBD2), RoundedCornerShape(9.dp)),
            )
        },
        track = { state ->
            val frac = (state.value - range.start) / (range.endInclusive - range.start)
            if (trackBrush != null) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(trackBrush))
            } else {
                Box(Modifier.fillMaxWidth().height(3.dp).background(c.line, RoundedCornerShape(2.dp))) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(c.brand, RoundedCornerShape(2.dp)))
                }
            }
        },
    )
}

// ---------------------------------------------------------------- Glyphs

/** The studio's small line-icon set, drawn in code so no icon library is needed. */
enum class Glyph {
    BACK, CLOSE, CHEVRON, SUN, MOON, PLUS, CHECK, THEMES, WALLPAPER, WIDGETS, SETTINGS, CLOCK, LOCK, APPS,
    PALETTE, LAYOUT, INFO, RESET, HOME, EDIT, TRASH, MOVE, PHONE, TYPE, CALENDAR,
}

@Composable
fun StudioGlyph(glyph: Glyph, size: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) { drawGlyph(glyph, color) }
}

/** Draws [glyph] on a 24-unit grid scaled to the canvas, with 1.6-unit round strokes. */
fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val u = size.minDimension / 24f
    val sw = 1.6f * u
    val stroke = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, p(x1, y1), p(x2, y2), sw, StrokeCap.Round)
    fun poly(vararg pts: Float, close: Boolean = false) {
        val path = Path().apply {
            moveTo(pts[0] * u, pts[1] * u)
            var i = 2
            while (i < pts.size) { lineTo(pts[i] * u, pts[i + 1] * u); i += 2 }
            if (close) close()
        }
        drawPath(path, color, style = stroke)
    }
    fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        drawRoundRect(color, p(x, y), Size(w * u, h * u), CornerRadius(r * u), style = stroke)
    when (glyph) {
        Glyph.BACK -> poly(15f, 5f, 8f, 12f, 15f, 19f)
        Glyph.CHEVRON -> poly(9f, 5f, 16f, 12f, 9f, 19f)
        Glyph.CLOSE -> { line(6f, 6f, 18f, 18f); line(18f, 6f, 6f, 18f) }
        Glyph.PLUS -> { line(12f, 5f, 12f, 19f); line(5f, 12f, 19f, 12f) }
        Glyph.CHECK -> poly(5f, 12.5f, 10f, 17.5f, 19f, 7f)
        Glyph.SUN -> {
            drawCircle(color, 4f * u, p(12f, 12f), style = stroke)
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
                line(12f + dx * 7f, 12f + dy * 7f, 12f + dx * 9.2f, 12f + dy * 9.2f)
            }
        }
        Glyph.MOON -> {
            val moon = Path().apply { addOval(Rect(p(12f, 12f), 8f * u)) }
            val bite = Path().apply { addOval(Rect(p(17f, 7.5f), 6.5f * u)) }
            drawPath(Path.combine(PathOperation.Difference, moon, bite), color, style = stroke)
        }
        Glyph.THEMES -> {
            val c = p(12f, 12f); val r = 8.5f * u; val k = r * 0.2f
            val star = Path().apply {
                moveTo(c.x, c.y - r)
                quadraticTo(c.x + k, c.y - k, c.x + r, c.y)
                quadraticTo(c.x + k, c.y + k, c.x, c.y + r)
                quadraticTo(c.x - k, c.y + k, c.x - r, c.y)
                quadraticTo(c.x - k, c.y - k, c.x, c.y - r)
                close()
            }
            drawPath(star, color, style = stroke)
        }
        Glyph.WALLPAPER -> {
            rect(3.5f, 4.5f, 17f, 15f, 2.5f)
            poly(4f, 17f, 9.5f, 11.5f, 13f, 15f, 15.5f, 12.5f, 20f, 17f)
            drawCircle(color, 1.4f * u, p(15.5f, 8.5f))
        }
        Glyph.WIDGETS -> {
            rect(4f, 4f, 7f, 7f, 1.8f); rect(13f, 4f, 7f, 7f, 1.8f)
            rect(4f, 13f, 7f, 7f, 1.8f); rect(13f, 13f, 7f, 7f, 1.8f)
        }
        Glyph.SETTINGS -> listOf(7f to 9f, 12f to 15f, 17f to 7f).forEachIndexed { i, (y, k) ->
            val yy = 6f + i * 6f
            line(4f, yy, 20f, yy)
            drawCircle(color, 2.2f * u, p(k, yy))
        }
        Glyph.CLOCK -> {
            drawCircle(color, 8.5f * u, p(12f, 12f), style = stroke)
            poly(12f, 7.5f, 12f, 12f, 15f, 14f)
        }
        Glyph.LOCK -> {
            rect(5f, 10.5f, 14f, 10f, 2.2f)
            val arc = Path().apply { moveTo(8f * u, 10.5f * u); lineTo(8f * u, 8f * u); cubicTo(8f * u, 3f * u, 16f * u, 3f * u, 16f * u, 8f * u); lineTo(16f * u, 10.5f * u) }
            drawPath(arc, color, style = stroke)
        }
        Glyph.APPS -> for (r in 0..2) for (cc in 0..2) drawCircle(color, 1.5f * u, p(6f + cc * 6f, 6f + r * 6f))
        Glyph.PALETTE -> {
            drawCircle(color, 8.5f * u, p(12f, 12f), style = stroke)
            drawCircle(color, 1.3f * u, p(8.5f, 10f)); drawCircle(color, 1.3f * u, p(12f, 7.5f)); drawCircle(color, 1.3f * u, p(15.5f, 10f))
            drawCircle(color, 2f * u, p(14f, 15.5f), style = stroke)
        }
        Glyph.LAYOUT -> { rect(4f, 4f, 16f, 16f, 2.5f); line(4f, 9.5f, 20f, 9.5f); line(10f, 9.5f, 10f, 20f) }
        Glyph.INFO -> {
            drawCircle(color, 8.5f * u, p(12f, 12f), style = stroke)
            line(12f, 11f, 12f, 16.5f); drawCircle(color, 1.1f * u, p(12f, 7.8f))
        }
        Glyph.RESET -> {
            val arc = Path().apply { arcTo(Rect(p(12f, 12f), 7.5f * u), -60f, 300f, true) }
            drawPath(arc, color, style = stroke)
            poly(15.75f, 2.5f, 15.75f, 5.5f, 18.75f, 5.5f)
        }
        Glyph.HOME -> { poly(4f, 11f, 12f, 4f, 20f, 11f); poly(6.5f, 9.5f, 6.5f, 20f, 17.5f, 20f, 17.5f, 9.5f) }
        Glyph.EDIT -> { poly(5f, 19f, 5.5f, 15f, 15.5f, 5f, 19f, 8.5f, 9f, 18.5f, close = true); line(13f, 7.5f, 16.5f, 11f) }
        Glyph.TRASH -> {
            line(4.5f, 6.5f, 19.5f, 6.5f); poly(9.5f, 6.5f, 10f, 4f, 14f, 4f, 14.5f, 6.5f)
            poly(6.5f, 6.5f, 7.5f, 20f, 16.5f, 20f, 17.5f, 6.5f)
        }
        Glyph.MOVE -> {
            line(12f, 3.5f, 12f, 20.5f); line(3.5f, 12f, 20.5f, 12f)
            poly(9.5f, 6f, 12f, 3.5f, 14.5f, 6f); poly(9.5f, 18f, 12f, 20.5f, 14.5f, 18f)
            poly(6f, 9.5f, 3.5f, 12f, 6f, 14.5f); poly(18f, 9.5f, 20.5f, 12f, 18f, 14.5f)
        }
        Glyph.PHONE -> {
            val path = Path().apply {
                moveTo(5.5f * u, 4f * u); lineTo(9f * u, 4f * u); lineTo(10.5f * u, 8.5f * u); lineTo(8.5f * u, 10f * u)
                cubicTo(9.5f * u, 12.5f * u, 11.5f * u, 14.5f * u, 14f * u, 15.5f * u)
                lineTo(15.5f * u, 13.5f * u); lineTo(20f * u, 15f * u); lineTo(20f * u, 18.5f * u)
                cubicTo(11f * u, 19.5f * u, 4.5f * u, 13f * u, 5.5f * u, 4f * u); close()
            }
            drawPath(path, color, style = stroke)
        }
        Glyph.TYPE -> { line(5f, 6f, 19f, 6f); line(12f, 6f, 12f, 19f); line(9.5f, 19f, 14.5f, 19f) }
        Glyph.CALENDAR -> {
            rect(4f, 5.5f, 16f, 14.5f, 2.5f); line(4f, 10f, 20f, 10f)
            line(8.5f, 3.5f, 8.5f, 7f); line(15.5f, 3.5f, 15.5f, 7f)
        }
    }
}
