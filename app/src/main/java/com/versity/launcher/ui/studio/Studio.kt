package com.versity.launcher.ui.studio

import android.os.Build
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.data.UiMode
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.ui.home.HomeBackground
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LiningFigures
import com.versity.launcher.ui.theme.fontFamilyFor

/*
 * Studio: the launcher's own interface (Customise hub and pages, sheets, dialogs, plain drawer).
 * Minimal layout, frosted-glass surfaces over a blurred copy of the user's home background, and a
 * light clay finish on controls (soft gradient, inner highlight, coloured contact shadow).
 * Graphite neutrals, one violet brand, Inter. Every text token passes WCAG AA in both modes.
 */

@Immutable
data class StudioColors(
    val dark: Boolean,
    /** Fallback screen background, and the tint laid over the blurred wallpaper. */
    val page: Color,
    /** Recessed wells: preview stage, segmented track. */
    val panel: Color,
    /** Opaque surfaces: sheets, dialogs, inputs. */
    val surface: Color,
    /** Translucent card fill over the blurred backdrop. */
    val glass: Color,
    /** Top and bottom of the glass edge: a lit rim fading downwards. */
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val shadow: Color,
    val ink: Color,
    val muted: Color,
    /** Placeholders and decorative glyphs only, never body text. */
    val faint: Color,
    val line: Color,
    val lineStrong: Color,
    /** Unchecked control outlines (3:1 against surface). */
    val control: Color,
    /** Fills: primary button, checked boxes, indicator bars. */
    val brand: Color,
    /** Lighter top of the clay brand gradient. */
    val brandLight: Color,
    /** Brand-coloured text and icons on page/surface. */
    val brandText: Color,
    val onBrand: Color,
    /** Tinted backgrounds for selected and informational states. */
    val brandSoft: Color,
    val scrim: Color,
)

val StudioLight = StudioColors(
    dark = false,
    page = Color(0xFFF4F5F8), panel = Color(0xFFECEDF2), surface = Color(0xFFFFFFFF),
    glass = Color(0xA8FFFFFF), glassRimTop = Color(0xF2FFFFFF), glassRimBottom = Color(0x59FFFFFF),
    shadow = Color(0x2A2B2560),
    ink = Color(0xFF111827), muted = Color(0xFF5F6672), faint = Color(0xFF9CA3AF),
    line = Color(0xFFE3E5EA), lineStrong = Color(0xFFD1D5DB), control = Color(0xFF848A96),
    brand = Color(0xFF5847DB), brandLight = Color(0xFF7564EE), brandText = Color(0xFF4F3ED2), onBrand = Color.White,
    brandSoft = Color(0xFFEEEBFC), scrim = Color(0x66111827),
)

val StudioDark = StudioColors(
    dark = true,
    page = Color(0xFF0E0F13), panel = Color(0xFF14151B), surface = Color(0xFF17181F),
    glass = Color(0x8C1C1D26), glassRimTop = Color(0x2EFFFFFF), glassRimBottom = Color(0x0AFFFFFF),
    shadow = Color(0x66000000),
    ink = Color(0xFFE8E9EE), muted = Color(0xFF9497A3), faint = Color(0xFF6B6E7A),
    line = Color(0xFF262833), lineStrong = Color(0xFF363948), control = Color(0xFF6B6E7A),
    brand = Color(0xFF5847DB), brandLight = Color(0xFF7262F0), brandText = Color(0xFFA99FFF), onBrand = Color.White,
    brandSoft = Color(0xFF221E40), scrim = Color(0x99000000),
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

/** Soft but not bubbly: cards 16dp, controls 12dp, chips 10dp. */
val StudioCardCorner = RoundedCornerShape(16.dp)
val StudioCorner = RoundedCornerShape(12.dp)
val StudioChipCorner = RoundedCornerShape(10.dp)

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
        glass = a(target.glass), glassRimTop = a(target.glassRimTop), glassRimBottom = a(target.glassRimBottom), shadow = a(target.shadow),
        ink = a(target.ink), muted = a(target.muted), faint = a(target.faint), line = a(target.line),
        lineStrong = a(target.lineStrong), control = a(target.control), brand = a(target.brand), brandLight = a(target.brandLight),
        brandText = a(target.brandText), onBrand = a(target.onBrand), brandSoft = a(target.brandSoft), scrim = a(target.scrim),
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

/**
 * The page behind every studio screen: the user's own home background, heavily blurred and tinted,
 * so glass cards have something real to sit on. Below Android 12 there is no blur, so the tint is denser.
 */
@Composable
fun GlassBackdrop(settings: LauncherSettings, modifier: Modifier = Modifier) {
    val c = Studio.colors
    val canBlur = Build.VERSION.SDK_INT >= 31
    Box(modifier.fillMaxSize().background(c.page)) {
        AppTheme(settings.copy(dimPercent = 0)) {
            HomeBackground(
                settings.copy(dimPercent = 0),
                Modifier.fillMaxSize().then(if (canBlur) Modifier.blur(56.dp, BlurredEdgeTreatment.Rectangle) else Modifier),
            )
        }
        Box(Modifier.fillMaxSize().background(c.page.copy(alpha = if (canBlur) (if (c.dark) 0.66f else 0.62f) else 0.9f)))
    }
}

// ---------------------------------------------------------------- Type

object StudioType {
    val display = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
    val title = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val heading = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val cardTitle = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
    val label = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
    val body = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val bodyStrong = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    val small = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val caption = TextStyle(fontSize = 11.sp, lineHeight = 15.sp)
}

@Composable
fun StudioText(text: String, style: TextStyle, color: Color = Studio.colors.ink, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) =
    Text(text, style = style.copy(fontFamily = Studio.font, fontFeatureSettings = LiningFigures), color = color, modifier = modifier, maxLines = maxLines)

/** Small capitalised section label. */
@Composable
fun StudioLabel(text: String, modifier: Modifier = Modifier) =
    StudioText(text.uppercase(), StudioType.label, Studio.colors.muted, modifier)

@Composable
fun StudioHint(text: String, modifier: Modifier = Modifier) =
    StudioText(text, StudioType.small, Studio.colors.muted, modifier)

// ---------------------------------------------------------------- Surfaces

/** Frosted glass: translucent fill, a lit top rim fading down, and a soft drop shadow. */
fun Modifier.glass(colors: StudioColors, shape: Shape = StudioCardCorner, elevation: Dp = 10.dp): Modifier =
    this
        .shadow(elevation, shape, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
        .background(colors.glass, shape)
        .border(1.dp, Brush.verticalGradient(listOf(colors.glassRimTop, colors.glassRimBottom)), shape)

/** Grouped settings card on glass, with an optional title row. */
@Composable
fun StudioCard(modifier: Modifier = Modifier, title: String? = null, content: @Composable () -> Unit) {
    val c = Studio.colors
    Column(
        modifier
            .fillMaxWidth()
            .glass(c)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) StudioText(title, StudioType.cardTitle)
        content()
    }
}

@Composable
fun StudioDivider(modifier: Modifier = Modifier) =
    Box(modifier.fillMaxWidth().height(1.dp).background(Studio.colors.line.copy(alpha = if (Studio.colors.dark) 1f else 0.8f)))

/**
 * Clay tile: a small rounded square with a soft top-lit gradient, an inner highlight and a coloured
 * contact shadow. Used for icons on navigation cards and the brand mark.
 */
@Composable
fun ClayTile(top: Color, bottom: Color, size: Dp = 40.dp, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(size * 0.3f)
    Box(
        Modifier
            .size(size)
            .shadow(8.dp, shape, clip = false, ambientColor = bottom.copy(alpha = 0.5f), spotColor = bottom.copy(alpha = 0.55f))
            .background(Brush.verticalGradient(listOf(top, bottom)), shape)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f))), shape)
            .drawWithContent {
                // Inner glow along the lower edge, the "pressed clay" look.
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)), startY = this.size.height * 0.55f),
                    cornerRadius = CornerRadius(size.toPx() * 0.3f),
                )
                drawContent()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------- Buttons

@Composable
private fun pressScale(source: MutableInteractionSource, pressed: Float = 0.97f): Float {
    val down by source.collectIsPressedAsState()
    return animateFloatAsState(if (down) pressed else 1f, tween(if (down) 90 else StudioMotion.QUICK), label = "press").value
}

/** Clay primary button: brand gradient, inner highlight, coloured shadow. 44dp inside a 48dp touch row. */
@Composable
fun StudioPrimaryButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .scale(pressScale(source))
                .height(42.dp)
                .shadow(8.dp, StudioCorner, clip = false, ambientColor = c.brand.copy(alpha = 0.6f), spotColor = c.brand.copy(alpha = 0.6f))
                .background(Brush.verticalGradient(listOf(c.brandLight, c.brand)), StudioCorner)
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.02f))), StudioCorner)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) { StudioText(label, StudioType.bodyStrong, c.onBrand, maxLines = 1) }
    }
}

/** Glass secondary button. */
@Composable
fun StudioButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .scale(pressScale(source))
                .height(40.dp)
                .glass(c, StudioCorner, elevation = 4.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) { StudioText(label, StudioType.bodyStrong, c.ink, maxLines = 1) }
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

/** 36dp glass square icon button, 48dp touch target. */
@Composable
fun StudioIconButton(description: String, modifier: Modifier = Modifier, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val c = Studio.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(48.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.scale(pressScale(source, 0.92f)).size(36.dp).glass(c, StudioChipCorner, elevation = 4.dp),
            contentAlignment = Alignment.Center,
        ) { icon() }
    }
}

/** Sun in light mode, moon in dark; turns a little as it switches. */
@Composable
fun ThemeToggleIcon(dark: Boolean) {
    val c = Studio.colors
    val turn by animateFloatAsState(if (dark) -30f else 0f, tween(StudioMotion.STANDARD, easing = StudioMotion.Signature), label = "turn")
    Canvas(Modifier.size(16.dp).rotate(turn)) {
        val s = 1.5.dp.toPx()
        val ctr = center
        if (dark) {
            val r = size.minDimension * 0.42f
            val moon = Path().apply { addOval(androidx.compose.ui.geometry.Rect(ctr, r)) }
            val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(ctr + Offset(r * 0.55f, -r * 0.45f), r * 0.85f)) }
            drawPath(Path.combine(PathOperation.Difference, moon, bite), c.ink, style = Stroke(s, join = StrokeJoin.Round))
        } else {
            val r = size.minDimension * 0.2f
            drawCircle(c.ink, r, ctr, style = Stroke(s))
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                drawLine(c.ink, ctr + dir * (r * 1.75f), ctr + dir * (r * 2.35f), s, StrokeCap.Round)
            }
        }
    }
}

/** Back chevron for page headers. */
@Composable
fun BackGlyph() {
    val c = Studio.colors
    Canvas(Modifier.size(16.dp)) {
        val w = size.width
        val sw = 1.7.dp.toPx()
        drawLine(c.ink, Offset(w * 0.62f, w * 0.18f), Offset(w * 0.3f, w * 0.5f), sw, StrokeCap.Round)
        drawLine(c.ink, Offset(w * 0.3f, w * 0.5f), Offset(w * 0.62f, w * 0.82f), sw, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------- Selection

/**
 * Segmented control: a recessed track with a raised clay indicator that slides between options.
 * Equal-width segments; 40dp visual height inside a 48dp touch row.
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
        val x by animateDpAsState(segment * index, tween(300, easing = StudioMotion.Signature), label = "seg")
        val thumbShape = RoundedCornerShape(9.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(if (c.dark) Color.Black.copy(alpha = 0.28f) else Color(0x14111827), StudioCorner)
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .offset(x = x)
                    .width(segment)
                    .fillMaxHeight()
                    .shadow(3.dp, thumbShape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
                    .background(if (c.dark) Color(0xFF2A2B36) else Color.White, thumbShape)
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = if (c.dark) 0.12f else 0.9f), Color.Transparent)), thumbShape),
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
                        StudioText(label(o), if (on) StudioType.bodyStrong else StudioType.body, tint, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Rounded-rectangle choice chip for wrapping option lists (fonts, backdrops, categories). */
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
    val border by animateColorAsState(if (selected) c.brand else c.glassRimTop, tween(StudioMotion.QUICK), label = "chipBorder")
    val fill by animateColorAsState(if (selected) c.brandSoft else c.glass, tween(StudioMotion.QUICK), label = "chipFill")
    Box(
        modifier
            .testTag(tag)
            .heightIn(min = 38.dp)
            .clip(StudioChipCorner)
            .background(fill, StudioChipCorner)
            .border(1.dp, border, StudioChipCorner)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = StudioType.body.copy(fontFamily = family ?: Studio.font, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal),
            color = if (selected) c.brandText else c.ink,
            maxLines = 1,
        )
    }
}

/** Checkbox row: 20dp clay box with a drawn tick, label and optional hint; the whole row toggles. */
@Composable
fun StudioCheckRow(label: String, hint: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val c = Studio.colors
    val fill by animateColorAsState(if (checked) c.brand else c.surface, tween(StudioMotion.QUICK), label = "checkFill")
    val edge by animateColorAsState(if (checked) c.brand else c.control, tween(StudioMotion.QUICK), label = "checkEdge")
    val tick by animateFloatAsState(if (checked) 1f else 0f, tween(StudioMotion.QUICK), label = "tick")
    val box = RoundedCornerShape(6.dp)
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
            Modifier.padding(top = if (hint == null) 0.dp else 1.dp).size(20.dp)
                .shadow(if (checked) 4.dp else 0.dp, box, clip = false, ambientColor = c.brand, spotColor = c.brand)
                .background(fill, box).border(1.5.dp, edge, box),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(11.dp)) {
                if (tick > 0f) {
                    val p = Path().apply {
                        moveTo(size.width * 0.12f, size.height * 0.54f)
                        lineTo(size.width * 0.4f, size.height * 0.8f)
                        lineTo(size.width * 0.9f, size.height * 0.22f)
                    }
                    drawPath(p, c.onBrand.copy(alpha = tick), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StudioText(label, StudioType.body)
            if (hint != null) StudioText(hint, StudioType.caption, c.muted)
        }
    }
}

/** Small tinted tag, for "Home" / "Lock" markers. */
@Composable
fun StudioTag(text: String) {
    val c = Studio.colors
    StudioText(
        text, StudioType.caption.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp), c.brandText,
        Modifier.background(c.brandSoft, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
        maxLines = 1,
    )
}
