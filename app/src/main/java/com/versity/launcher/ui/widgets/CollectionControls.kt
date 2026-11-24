package com.versity.launcher.ui.widgets

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.versity.launcher.LauncherApplication
import com.versity.launcher.data.AppEntry
import com.versity.launcher.data.filterApps
import com.versity.launcher.system.PhotoLibrary
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.drawer.AppIcon
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCard
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioCorner
import com.versity.launcher.ui.studio.StudioDivider
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioSegmented
import com.versity.launcher.ui.studio.StudioSlider
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface
import com.versity.launcher.widget.Collection
import com.versity.launcher.widget.CollectionArt
import com.versity.launcher.widget.WidgetBackground
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStyle
import com.versity.launcher.widget.WidgetType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Emerald and the greens and greys of the reference, ahead of the general swatches. */
private val CollectionInks = listOf(0xFF7DD69BL, 0xFF4CC38AL, 0xFFE8EEE9L) + InkSwatches.filterNot { it == 0xFFFFFFFFL }
private val ShapeSwatches = listOf(0xFF060607L, 0xFF0E1913L, 0xFF1C1C20L, 0xFF263D31L, 0xFF3A5546L, 0xFF3A3A40L, 0xFF1F2430L, 0xFFF4F1EAL, 0xFFFFFFFFL)

/** Slot names for the radial launcher, in [CollectionArt.Slots] order. */
private val SlotNames = listOf("Top", "Left", "Centre", "Right", "Bottom")

/** Apps that suit a new launcher, in reference order; the rest is filled from the app list. */
private val Suggested = listOf(
    "com.whatsapp", "com.instagram.android", "com.spotify.music", "org.telegram.messenger", "com.google.android.youtube",
    "com.android.chrome", "com.google.android.apps.photos", "com.google.android.dialer", "com.google.android.apps.messaging", "com.google.android.gm",
)

/** The studio's options for the collection: countdown event, apps, logo, colours and proportions. */
@Composable
internal fun CollectionControls(spec: WidgetSpec, set: (WidgetSpec) -> Unit) {
    val context = LocalContext.current
    val apps by produceState(emptyList<AppEntry>()) {
        value = runCatching { (context.applicationContext as LauncherApplication).container.appsRepository.loadApps() }.getOrDefault(emptyList())
    }
    // A new launcher, shortcut or badge starts with real apps rather than empty circles.
    val slots = when (spec.type) {
        WidgetType.LAUNCHER -> CollectionArt.slotCount(spec)
        WidgetType.SHORTCUT, WidgetType.BADGE -> 1
        else -> 0
    }
    // Switching to the spinner tops its extra places up the same way, keeping the apps already chosen.
    LaunchedEffect(apps.isNotEmpty(), slots) {
        if (apps.isEmpty() || slots == 0 || spec.type == WidgetType.BADGE) return@LaunchedEffect
        val chosen = spec.apps.take(slots)
        if (chosen.size >= slots && chosen.all { it.isNotBlank() }) return@LaunchedEffect
        if (spec.apps.isNotEmpty() && spec.type != WidgetType.LAUNCHER) return@LaunchedEffect
        val byPackage = apps.groupBy { it.packageName }
        val spare = (Suggested.mapNotNull { byPackage[it]?.first() } + apps).map { it.key }.distinct().filter { it !in chosen }.iterator()
        val filled = List(slots) { i -> chosen.getOrNull(i)?.takeIf { it.isNotBlank() } ?: if (spare.hasNext()) spare.next() else "" }
        if (filled != spec.apps) set(spec.copy(apps = filled + spec.apps.drop(slots)))
    }

    when (spec.type) {
        WidgetType.COUNTDOWN -> EventCard(spec, set)
        WidgetType.LAUNCHER, WidgetType.SHORTCUT -> AppsCard(spec, set, apps, slots)
        WidgetType.BADGE -> LogoCard(spec, set, apps)
        else -> Unit
    }
    ColourCard(spec, set)
    if (spec.type != WidgetType.COUNTDOWN) ProportionsCard(spec, set)
}

// ------------------------------------------------------------ Countdown

@Composable
private fun EventCard(spec: WidgetSpec, set: (WidgetSpec) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val target = Instant.ofEpochMilli(Collection.target(spec)).atZone(zone).toLocalDateTime()
    fun setTarget(t: LocalDateTime) = set(spec.copy(target = t.atZone(zone).toInstant().toEpochMilli()))
    StudioCard(title = "Event") {
        StudioLabel("Name")
        StudioTextField(spec.title, { set(spec.copy(title = it.take(24))) }, "Countdown", Modifier.testTag(TestTags.COUNTDOWN_TITLE))
        StudioLabel("Date and time")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StudioButton(target.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), Modifier.testTag(TestTags.COUNTDOWN_DATE), glyph = Glyph.CALENDAR) {
                DatePickerDialog(context, { _, y, m, d -> setTarget(target.withYear(y).withMonth(m + 1).withDayOfMonth(d)) }, target.year, target.monthValue - 1, target.dayOfMonth).show()
            }
            StudioButton(target.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)), glyph = Glyph.CLOCK) {
                TimePickerDialog(context, { _, h, min -> setTarget(target.withHour(h).withMinute(min).withSecond(0)) }, target.hour, target.minute, DateFormat.is24HourFormat(context)).show()
            }
        }
        if (spec.target > 0) StudioTextButton("Count to New Year instead", color = Studio.colors.brandText) { set(spec.copy(target = 0)) }
        StudioHint("Days, hours, minutes and seconds count down on their own, without waking your phone.")
    }
}

// ------------------------------------------------------------ Apps

@Composable
private fun AppsCard(spec: WidgetSpec, set: (WidgetSpec) -> Unit, apps: List<AppEntry>, slots: Int) {
    var picking by remember { mutableStateOf<Int?>(null) }
    val byKey = remember(apps) { apps.associateBy { it.key } }
    StudioCard(title = if (slots > 1) "Apps" else "App") {
        Column {
            repeat(slots) { i ->
                if (i > 0) StudioDivider()
                val app = spec.apps.getOrNull(i)?.let { byKey[it] }
                val name = when {
                    slots == 1 -> "Opens"
                    slots == CollectionArt.SPIN_SLOTS -> "App ${i + 1}"
                    else -> SlotNames[i]
                }
                AppRow(name, app, Modifier.testTag(TestTags.widgetSlot(i))) { picking = i }
            }
        }
    }
    picking?.let { slot ->
        AppPicker(apps, onDismiss = { picking = null }) { picked ->
            // Apps beyond this style's places are kept, so switching back and forth loses nothing.
            val list = MutableList(maxOf(slots, spec.apps.size)) { spec.apps.getOrNull(it).orEmpty() }
            list[slot] = picked.key
            set(spec.copy(apps = list))
            picking = null
        }
    }
}

@Composable
private fun AppRow(label: String, app: AppEntry?, modifier: Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).clip(StudioCorner).clickable(onClick = onClick).padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StudioText(label, StudioType.small, c.muted, Modifier.width(56.dp), maxLines = 1)
        if (app != null) AppIcon(app, 32.dp) else Box(Modifier.size(32.dp).background(c.panel, RoundedCornerShape(16.dp)))
        StudioText(app?.label ?: "Choose an app", StudioType.body, if (app != null) c.ink else c.muted, Modifier.weight(1f), maxLines = 1)
        StudioGlyph(Glyph.CHEVRON, 14.dp, c.faint)
    }
}

/** A searchable list of installed apps. */
@Composable
private fun AppPicker(apps: List<AppEntry>, onDismiss: () -> Unit, onPick: (AppEntry) -> Unit) {
    var query by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        val c = Studio.colors
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).background(c.surface, StudioCardCorner).border(1.dp, c.line, StudioCardCorner).padding(12.dp)) {
            StudioText("Choose an app", StudioType.title, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            StudioTextField(query, { query = it }, "Search apps", Modifier.testTag(TestTags.APP_SEARCH))
            LazyColumn(Modifier.padding(top = 8.dp)) {
                items(filterApps(apps, query), key = { it.key }) { app ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(StudioCorner).clickable { onPick(app) }.padding(horizontal = 6.dp)
                            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = app.label },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppIcon(app, 34.dp)
                        StudioText(app.label, StudioType.body, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ Badge logo

@Composable
private fun LogoCard(spec: WidgetSpec, set: (WidgetSpec) -> Unit, apps: List<AppEntry>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var picking by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val stored = withContext(Dispatchers.IO) { runCatching { PhotoLibrary.add(context, uri) }.getOrNull() }
            if (stored != null) set(spec.copy(image = stored))
        }
    }
    val app = remember(apps, spec.apps) { spec.apps.firstOrNull()?.let { k -> apps.firstOrNull { it.key == k } } }
    StudioCard(title = "Logo") {
        StudioHint("Your own picture or logo, or an app's icon. Tapping the badge opens the app, if you choose one.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StudioButton(if (spec.image != null) "Change picture" else "Choose picture", Modifier.testTag(TestTags.BADGE_IMAGE), glyph = Glyph.WALLPAPER) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            if (spec.image != null) StudioTextButton("Remove", color = Studio.colors.danger) { set(spec.copy(image = null)) }
        }
        StudioDivider()
        AppRow("App", app, Modifier.testTag(TestTags.widgetSlot(0))) { picking = true }
        if (spec.apps.isNotEmpty()) StudioTextButton("No app", color = Studio.colors.muted) { set(spec.copy(apps = emptyList())) }
    }
    if (picking) AppPicker(apps, onDismiss = { picking = false }) { set(spec.copy(apps = listOf(it.key))); picking = false }
}

// ------------------------------------------------------------ Colours and proportions

@Composable
private fun ColourCard(spec: WidgetSpec, set: (WidgetSpec) -> Unit) {
    StudioCard(title = "Colour") {
        when (spec.type) {
            WidgetType.COUNTDOWN -> {
                StudioLabel("Accent: title and days")
                Swatches(CollectionInks, spec.accentColor) { set(spec.copy(accentColor = it)) }
                HueSlider(spec.accentColor) { set(spec.copy(accentColor = it)) }
                StudioLabel("Timer")
                Swatches(CollectionInks, spec.textColor) { set(spec.copy(textColor = it)) }
            }
            WidgetType.BADGE -> {
                StudioLabel("Outer shape")
                Swatches(ShapeSwatches, spec.backgroundColor) { set(spec.copy(backgroundColor = it)) }
                StudioLabel("Ring")
                Swatches(ShapeSwatches, spec.accentColor) { set(spec.copy(accentColor = it)) }
                StudioLabel("Centre")
                Swatches(ShapeSwatches, spec.textColor) { set(spec.copy(textColor = it)) }
            }
            else -> {
                // Launcher and shortcut: the shape behind the apps can be dropped so they float on the wallpaper.
                StudioLabel(if (spec.type == WidgetType.LAUNCHER) "Background shape" else "Background disc")
                StudioSegmented(
                    WidgetBackground.entries, spec.background, { it.label }, { set(spec.copy(background = it)) },
                    tag = { TestTags.widgetBackground(it.name) },
                )
                if (spec.background == WidgetBackground.SOLID) {
                    Swatches(ShapeSwatches, spec.backgroundColor) { set(spec.copy(backgroundColor = it)) }
                }
                StudioLabel("Rings")
                Swatches(ShapeSwatches, spec.textColor) { set(spec.copy(textColor = it)) }
                StudioLabel("Behind the icons")
                Swatches(ShapeSwatches, spec.accentColor) { set(spec.copy(accentColor = it)) }
            }
        }
        val hasFill = spec.type == WidgetType.BADGE || spec.background != WidgetBackground.NONE
        if (spec.type != WidgetType.COUNTDOWN && hasFill) {
            LabelledSlider("Opacity", spec.opacity, 20..100, "%") { set(spec.copy(opacity = it)) }
        }
    }
}

@Composable
private fun ProportionsCard(spec: WidgetSpec, set: (WidgetSpec) -> Unit) {
    StudioCard(title = "Size and borders") {
        LabelledSlider(if (spec.type == WidgetType.BADGE) "Logo" else "Icons", spec.iconScale, 40..100, "%") { set(spec.copy(iconScale = it)) }
        if (spec.type == WidgetType.LAUNCHER) LabelledSlider("Spacing", spec.spacing, 0..30, "") { set(spec.copy(spacing = it)) }
        val badge = spec.type == WidgetType.BADGE
        // The badge always keeps its ring band; the apps can go without rings.
        val counts = if (badge) listOf(1, 2, 3) else listOf(0, 1, 2, 3)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StudioText("Rings", StudioType.body, modifier = Modifier.width(76.dp))
            StudioSegmented(
                counts, spec.rings.coerceIn(counts.first(), 3), { if (it == 0) "None" else "$it" }, { set(spec.copy(rings = it)) },
                Modifier.weight(1f), tag = { "rings_$it" },
            )
        }
        if (badge || spec.rings > 0) LabelledSlider("Ring width", spec.ringWidth, 1..10, "") { set(spec.copy(ringWidth = it)) }
        if (!badge) ToggleLine("Round icons", spec.roundIcons) { set(spec.copy(roundIcons = it)) }
        if (spec.type == WidgetType.LAUNCHER && spec.style != WidgetStyle.RADIAL_SPINNER) {
            ToggleLine("Larger centre app", spec.featured) { set(spec.copy(featured = it)) }
        }
        ToggleLine("Soft shadow", spec.shadow) { set(spec.copy(shadow = it)) }
    }
}

@Composable
private fun LabelledSlider(label: String, value: Int, range: IntRange, unit: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StudioText(label, StudioType.body, modifier = Modifier.width(76.dp))
        StudioSlider(
            value.toFloat(), { onChange(it.toInt()) }, range.first.toFloat()..range.last.toFloat(),
            Modifier.weight(1f), description = "$label, $value",
        )
        StudioText("$value$unit", StudioType.small, Studio.colors.muted, Modifier.width(44.dp).padding(start = 8.dp))
    }
}

// ------------------------------------------------------------ Text field

/** Thin text input: 44dp, hairline border, no heavy focus ring. */
@Composable
internal fun StudioTextField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val c = Studio.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = StudioType.body.copy(color = c.ink, fontFamily = Studio.font),
        cursorBrush = SolidColor(c.brand),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = modifier.fillMaxWidth().height(44.dp).surface(c, StudioCorner),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) StudioText(placeholder, StudioType.body, c.faint, maxLines = 1)
                inner()
            }
        },
    )
}
