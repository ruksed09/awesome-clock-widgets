package com.versity.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.Wallpapers
import com.versity.launcher.system.UpdateState
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioCorner
import com.versity.launcher.ui.studio.StudioDivider
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface

/** Sections shown in the tablet sidebar, in order, with their glyphs. */
internal val TopSections = listOf(
    CustomisePage.THEMES to Glyph.THEMES,
    CustomisePage.WALLPAPERS to Glyph.WALLPAPER,
    CustomisePage.WIDGETS to Glyph.WIDGETS,
    CustomisePage.SETTINGS to Glyph.SETTINGS,
)

/** The top-level section a page belongs to; the hub opens on Themes. */
internal fun sectionOf(page: CustomisePage): CustomisePage = when {
    page == CustomisePage.HUB -> CustomisePage.THEMES
    page.parent == CustomisePage.SETTINGS -> CustomisePage.SETTINGS
    else -> page
}

/** Sidebar width for a screen [screenWidth] wide: roomy on large tablets, compact on small ones. */
internal fun sidebarWidth(screenWidth: Dp): Dp = if (screenWidth >= 1000.dp) 280.dp else 232.dp

/**
 * The tablet sidebar: brand and light/dark switch, the four sections, then what is applied, any update
 * and, when Versity is not the home app, a way to set it. Replaces the phone hub on wide screens.
 */
@Composable
internal fun Sidebar(
    width: Dp,
    selected: CustomisePage,
    settings: LauncherSettings,
    isDefault: Boolean,
    update: UpdateState,
    updateActions: UpdateActions,
    onSelect: (CustomisePage) -> Unit,
    onSetDefault: () -> Unit,
    dark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val c = Studio.colors
    Column(Modifier.width(width).fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StudioText("Versity", StudioType.display, modifier = Modifier.semantics { heading() })
                StudioText("Themes, wallpapers and widgets", StudioType.small, c.muted, maxLines = 2)
            }
            ThemeToggle(dark, onToggleTheme)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            TopSections.forEach { (page, glyph) ->
                val detail = when (page) {
                    CustomisePage.THEMES -> "${ThemePresets.themes.size}"
                    CustomisePage.WALLPAPERS -> "${Wallpapers.all.size + settings.photos.size}"
                    else -> null
                }
                NavItem(page.title, glyph, detail, page == selected, Modifier.testTag(TestTags.nav(page.name))) { onSelect(page) }
            }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UpdateBanner(update, updateActions, compact = true)
            Column(Modifier.fillMaxWidth().surface(c).padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StudioLabel("Applied")
                SummaryLine("Home screen", homeSummary(settings))
                StudioDivider()
                SummaryLine("Lock screen", lockSummary(settings, isDefault))
            }
            if (!isDefault) {
                Column(Modifier.fillMaxWidth().surface(c).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)) {
                    StudioText("Use Versity as your home app", StudioType.bodyStrong)
                    StudioText("Your clock, widgets and theme on the home screen.", StudioType.small, c.muted)
                    StudioPrimaryButton("Set as home app", Modifier.testTag(TestTags.DEFAULT_BANNER_SET), onClick = onSetDefault)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column {
        StudioText(label, StudioType.caption, Studio.colors.muted, maxLines = 1)
        StudioText(value, StudioType.bodyStrong, maxLines = 1)
    }
}

@Composable
private fun NavItem(title: String, glyph: Glyph, detail: String?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(StudioCorner)
            .background(if (selected) c.brandSoft else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .semantics(mergeDescendants = true) { role = Role.Tab; this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StudioGlyph(glyph, 18.dp, if (selected) c.brandText else c.ink)
        StudioText(title, if (selected) StudioType.bodyStrong else StudioType.body, if (selected) c.brandText else c.ink, Modifier.weight(1f), maxLines = 1)
        if (detail != null) StudioText(detail, StudioType.small, c.muted, maxLines = 1)
    }
}

/** Title bar for content beside the sidebar: no back arrow for a top-level section. */
@Composable
internal fun PaneHeader(title: String, onBack: (() -> Unit)?, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(start = if (onBack == null) Gutter + 4.dp else 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) com.versity.launcher.ui.studio.StudioIconButton("Back", onClick = onBack) { com.versity.launcher.ui.studio.BackGlyph() }
        StudioText(title, StudioType.title, modifier = Modifier.weight(1f).padding(start = 2.dp).semantics { heading() }, maxLines = 1)
        action?.invoke()
    }
}

/** Hairline between panes. */
@Composable
internal fun PaneDivider() = Box(Modifier.width(1.dp).fillMaxHeight().background(Studio.colors.line))
