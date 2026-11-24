package com.versity.launcher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.versity.launcher.R
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.Wallpapers
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.home.components.WidgetView
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetType

private data class NavEntry(val page: CustomisePage, val title: String, val subtitle: String)

@Composable
internal fun HubPage(settings: LauncherSettings, isDefault: Boolean, go: (CustomisePage) -> Unit, onSetDefault: () -> Unit) {
    val c = Studio.colors
    val home = currentHomeTheme(settings)
    val entries = listOf(
        NavEntry(CustomisePage.THEMES, "Themes", "${ThemePresets.themes.size} complete looks"),
        NavEntry(CustomisePage.WALLPAPERS, "Wallpapers", "${Wallpapers.all.size} images, or your own photo"),
        NavEntry(CustomisePage.WIDGETS, "Widgets", "Clocks, dates and calendars"),
        NavEntry(CustomisePage.SETTINGS, "Settings", "Clock, colours, lock and apps"),
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Four across on tablets, two on phones, one on very narrow screens (folding phones' cover displays).
        val columns = when {
            maxWidth >= 840.dp -> 4
            maxWidth < 300.dp -> 1
            else -> 2
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max = 1040.dp)
                .padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppliedSummary(home?.name ?: "Your own mix", settings.lockThemeId?.let(ThemePresets::theme)?.name ?: "Same as home")
            entries.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { e -> NavCard(e, Modifier.weight(1f)) { go(e.page) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (!isDefault) {
                Row(
                    Modifier.fillMaxWidth().surface(c).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StudioGlyph(Glyph.HOME, 20.dp, c.ink)
                    Column(Modifier.weight(1f)) {
                        StudioText("Use Versity as your home app", StudioType.bodyStrong)
                        StudioText("Your clock, widgets and theme on the home screen.", StudioType.small, c.muted)
                    }
                    StudioPrimaryButton("Set", Modifier.testTag(TestTags.DEFAULT_BANNER_SET), onClick = onSetDefault)
                }
            }
            StudioHint("Long-press your home screen to come back here.", Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
        }
    }
}

/** What is applied where, at a glance. */
@Composable
private fun AppliedSummary(home: String, lock: String) {
    val c = Studio.colors
    Row(Modifier.fillMaxWidth().surface(c)) {
        listOf("Home screen" to home, "Lock screen" to lock).forEachIndexed { i, (label, value) ->
            if (i > 0) Box(Modifier.padding(vertical = 12.dp).width(1.dp).height(32.dp).background(c.line))
            Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 10.dp)) {
                StudioText(label, StudioType.caption, c.muted, maxLines = 1)
                StudioText(value, StudioType.bodyStrong, maxLines = 1)
            }
        }
    }
}

/** Navigation card: an image of what is inside, then a title and one line of detail. */
@Composable
private fun NavCard(entry: NavEntry, modifier: Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    Column(
        modifier
            .testTag(TestTags.nav(entry.page.name))
            .surface(c)
            .clip(StudioCardCorner)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Box(
            Modifier.fillMaxWidth().height(116.dp).padding(6.dp).clip(RoundedCornerShape(8.dp)).background(c.panel),
            contentAlignment = Alignment.Center,
        ) { NavVisual(entry.page) }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StudioText(entry.title, StudioType.cardTitle, maxLines = 1)
                StudioText(entry.subtitle, StudioType.small, c.muted, maxLines = 2, minLines = 2)
            }
            StudioGlyph(Glyph.CHEVRON, 14.dp, c.faint)
        }
    }
}

@Composable
private fun NavVisual(page: CustomisePage) {
    when (page) {
        CustomisePage.THEMES -> Thumbs(listOf(R.drawable.wp_leviathan_thumb, R.drawable.wp_glass_bloom_thumb, R.drawable.wp_ringed_giant_thumb), 44)
        CustomisePage.WALLPAPERS -> Thumbs(listOf(R.drawable.wp_misty_peaks_thumb, R.drawable.wp_iridescent_thumb, R.drawable.wp_calm_sea_thumb, R.drawable.wp_mesh_ink_thumb), 34)
        CustomisePage.WIDGETS -> MiniWidgets()
        else -> SettingsSketch()
    }
}

/** A row of wallpaper thumbnails with fine white edges. */
@Composable
private fun Thumbs(res: List<Int>, widthDp: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        res.forEachIndexed { i, id ->
            val tall = i == res.size / 2
            Image(
                painterResource(id), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = widthDp.dp, height = if (tall) 96.dp else 84.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(6.dp)),
            )
        }
    }
}

/** Two real widgets over a wallpaper crop. */
@Composable
private fun MiniWidgets() {
    val digital = remember { WidgetSpec.default(WidgetType.DIGITAL) }
    val date = remember { WidgetSpec.default(WidgetType.DATE) }
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
        Image(painterResource(R.drawable.wp_leviathan_thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Scaled(digital, 180, 84, 0.5f)
            Scaled(date, 90, 90, 0.66f)
        }
    }
}

@Composable
private fun Scaled(spec: WidgetSpec, w: Int, h: Int, s: Float) {
    Box(Modifier.size((w * s).dp, (h * s).dp)) {
        Box(Modifier.requiredSize(w.dp, h.dp).graphicsLayer { scaleX = s; scaleY = s }) { WidgetView(spec, Modifier.fillMaxSize()) }
    }
}

/** Three calm placeholder rows, like the settings list itself. */
@Composable
private fun SettingsSketch() {
    val c = Studio.colors
    Column(Modifier.width(132.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(Glyph.CLOCK to 0.7f, Glyph.PALETTE to 0.55f, Glyph.LOCK to 0.62f).forEach { (glyph, frac) ->
            Row(
                Modifier.fillMaxWidth().height(24.dp).background(c.surface, RoundedCornerShape(6.dp)).border(1.dp, c.line, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StudioGlyph(glyph, 12.dp, c.muted)
                Box(Modifier.fillMaxWidth(frac).height(4.dp).background(c.lineStrong, RoundedCornerShape(2.dp)))
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(width = 14.dp, height = 8.dp).background(c.brand.copy(alpha = 0.85f), RoundedCornerShape(4.dp)))
            }
        }
    }
}
