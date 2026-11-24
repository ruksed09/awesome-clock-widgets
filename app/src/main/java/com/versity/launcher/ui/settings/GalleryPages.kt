package com.versity.launcher.ui.settings

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.domain.ThemeCategory
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.WallpaperOption
import com.versity.launcher.domain.Wallpapers
import com.versity.launcher.domain.isLight
import com.versity.launcher.domain.lockLook
import com.versity.launcher.domain.withTheme
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.HomePreview
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioCheckRow
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface

// ---------------------------------------------------------------- Themes

@Composable
internal fun ThemesPage(settings: LauncherSettings, onOpen: (ThemeBundle) -> Unit) {
    var category by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    val list = remember(category) { ThemePresets.themes.filter { category == null || it.category == category } }
    val home = currentHomeTheme(settings)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 116.dp),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioChip("All", category == null, TestTags.category("ALL")) { category = null }
                ThemeCategory.entries.forEach { cat ->
                    StudioChip(cat.label, category == cat, TestTags.category(cat.name)) { category = cat }
                }
            }
        }
        items(list, key = { it.id }) { theme ->
            ThemeCard(
                theme = theme,
                onHome = home?.id == theme.id,
                onLock = settings.lockThemeId == theme.id || (settings.lockThemeId == null && home?.id == theme.id),
                onClick = { onOpen(theme) },
            )
        }
    }
}

/** Gallery card: the real home screen in this theme, scaled down, then its name and tagline. */
@Composable
private fun ThemeCard(theme: ThemeBundle, onHome: Boolean, onLock: Boolean, onClick: () -> Unit) {
    val c = Studio.colors
    val applied = onHome || onLock
    val look = remember(theme.id) { LauncherSettings.DEFAULT.withTheme(theme, useThemeWallpaper = true) }
    val where = listOfNotNull("home screen".takeIf { onHome }, "lock screen".takeIf { onLock })
    Column(
        Modifier
            .testTag(TestTags.theme(theme.id))
            .surface(c)
            .then(if (applied) Modifier.border(1.5.dp, c.brand, StudioCardCorner) else Modifier)
            .clip(StudioCardCorner)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = applied
                if (where.isNotEmpty()) stateDescription = "Applied to ${where.joinToString(" and ")}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
            HomePreview(look, maxWidth, onClick = onClick)
            AppliedTags(onHome, onLock, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }
        Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp)) {
            StudioText(theme.name, StudioType.small.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            StudioText(theme.tagline, StudioType.caption, c.muted, maxLines = 2)
        }
    }
}

// ---------------------------------------------------------------- Wallpapers

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WallpapersPage(settings: LauncherSettings, actions: CustomiseActions, onOpen: (WallpaperOption) -> Unit) {
    val c = Studio.colors
    val homeWp = Wallpapers.byUri(settings.wallpaperUri)
    val lockWp = Wallpapers.byUri(settings.lockLook().wallpaperUri)
    val photo = settings.wallpaperUri != null && homeWp == null
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 100.dp),
        contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }, key = "photo") {
            Row(
                Modifier.fillMaxWidth().surface(c).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StudioGlyph(Glyph.WALLPAPER, 20.dp, c.ink)
                Column(Modifier.weight(1f)) {
                    StudioText("Your photo", StudioType.bodyStrong)
                    StudioText(if (photo) "Your photo is on the home screen." else "Use any photo from your phone.", StudioType.small, c.muted)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End), verticalArrangement = Arrangement.Center) {
                    if (settings.wallpaperUri != null) {
                        StudioTextButton("None", Modifier.testTag(TestTags.CLEAR_WALLPAPER)) { actions.onClearWallpaper() }
                    }
                    StudioButton(if (photo) "Change" else "Choose", Modifier.testTag(TestTags.PICK_WALLPAPER)) { actions.onPickWallpaper() }
                }
            }
        }
        Wallpapers.collections.forEach { collection ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "h_$collection") {
                StudioLabel(collection, Modifier.padding(start = 4.dp, top = 10.dp))
            }
            items(Wallpapers.all.filter { it.collection == collection }, key = { it.id }) { wp ->
                WallpaperTile(wp, onHome = homeWp?.id == wp.id, onLock = lockWp?.id == wp.id) { onOpen(wp) }
            }
        }
    }
}

@Composable
private fun WallpaperTile(wp: WallpaperOption, onHome: Boolean, onLock: Boolean, onClick: () -> Unit) {
    val c = Studio.colors
    val applied = onHome || onLock
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .testTag(TestTags.wallpaper(wp.id))
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = wp.name
                selected = applied
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 19.5f)
                .clip(shape)
                .border(if (applied) 2.dp else 1.dp, if (applied) c.brand else c.line, shape),
        ) {
            Image(painterResource(wp.thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            AppliedTags(onHome, onLock, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }
        StudioText(wp.name, StudioType.small.copy(fontWeight = FontWeight.Medium), maxLines = 1, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

/** Palette that keeps text readable on [wp]: light wallpapers need dark ink and vice versa. */
private fun readablePalette(settings: LauncherSettings, wp: WallpaperOption): String? {
    val currentLight = ThemePresets.palette(settings.paletteId).isLight()
    return when {
        wp.light && !currentLight -> "porcelain"
        !wp.light && currentLight -> "abyss"
        else -> null
    }
}

@Composable
internal fun WallpaperApplySheet(
    wp: WallpaperOption,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (WallpaperApplyRequest) -> Unit,
) {
    var target by rememberSaveable(wp.id) { mutableStateOf(ApplyTarget.BOTH) }
    var setPhoneWallpaper by rememberSaveable(wp.id) { mutableStateOf(true) }
    var setLockWallpaper by rememberSaveable(wp.id) { mutableStateOf(true) }
    val suggested = readablePalette(settings, wp)
    var matchText by rememberSaveable(wp.id) { mutableStateOf(true) }
    val palette = suggested?.takeIf { matchText }

    val homeAfter = if (target.home) settings.copy(wallpaperUri = wp.uri, paletteId = palette ?: settings.paletteId) else settings
    val lockAfter = if (target.lock) settings.lockLook().copy(wallpaperUri = wp.uri, paletteId = palette ?: settings.lockLook().paletteId)
    else homeAfter.lockLook()

    SheetScaffold(
        label = wp.collection, title = wp.name, subtitle = "Wallpaper", tag = TestTags.WALLPAPER_SHEET,
        onDismiss = onDismiss,
        confirm = "Set wallpaper",
        onConfirm = {
            onApply(
                WallpaperApplyRequest(
                    wallpaper = wp, target = target,
                    setPhoneWallpaper = target.home && setPhoneWallpaper,
                    setLockWallpaper = target.lock && setLockWallpaper,
                    paletteId = palette,
                ),
            )
        },
    ) {
        SheetPreviews(homeAfter.copy(lockThemeId = null, lockWallpaperUri = null), lockAfter.copy(lockThemeId = null, lockWallpaperUri = null), target.home, target.lock || target.home)
        TargetPicker(target) { target = it }
        Column {
            if (target.home) StudioCheckRow("Also set as phone wallpaper", "Shows behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
            if (target.lock) StudioCheckRow("Also set as lock-screen wallpaper", "Replaces your current one. Your phone's clock stays on top.", setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER) { setLockWallpaper = it }
            if (suggested != null) {
                StudioCheckRow(
                    if (wp.light) "Use dark text for this light wallpaper" else "Use light text for this dark wallpaper",
                    "Keeps the clock and labels easy to read.", matchText, TestTags.OPT_MATCH_TEXT,
                ) { matchText = it }
            }
        }
    }
}

/** Theme sheet: previews, where to apply, and what else to change on the phone. */
@Composable
internal fun ThemeApplySheet(
    theme: ThemeBundle,
    settings: LauncherSettings,
    onDismiss: () -> Unit,
    onApply: (ThemeApplyRequest) -> Unit,
) {
    var target by rememberSaveable(theme.id) { mutableStateOf(ApplyTarget.BOTH) }
    var useThemeWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setPhoneWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var showLockScreen by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setLockWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }

    // The user has a wallpaper of their own (photo, or one picked in Wallpapers) that this theme would replace.
    val ownWallpaper = settings.wallpaperUri != null && settings.wallpaperUri != theme.wallpaperUri
    val replace = !ownWallpaper || useThemeWallpaper
    val themedHome = settings.withTheme(theme, useThemeWallpaper = replace)
    val homeAfter = if (target.home) themedHome else settings
    val lockAfter = when (target) {
        // Home only: a lock screen that follows home changes too; one with its own theme does not.
        ApplyTarget.HOME -> if (settings.lockThemeId == null && settings.lockWallpaperUri == null) themedHome else settings.lockLook()
        ApplyTarget.LOCK -> settings.withTheme(theme, useThemeWallpaper = true)
        ApplyTarget.BOTH -> themedHome
    }.copy(lockThemeId = null, lockWallpaperUri = null)
    val lockChanges = target.lock || (target.home && settings.lockThemeId == null)

    SheetScaffold(
        label = theme.category.label, title = theme.name, subtitle = theme.tagline, tag = TestTags.APPLY_SHEET,
        onDismiss = onDismiss, confirm = "Apply theme",
        onConfirm = {
            onApply(
                ThemeApplyRequest(
                    theme = theme,
                    target = target,
                    replaceWallpaper = target.home && replace,
                    setPhoneWallpaper = target.home && setPhoneWallpaper,
                    showLockScreen = target.lock && showLockScreen,
                    setLockWallpaper = target.lock && setLockWallpaper,
                ),
            )
        },
    ) {
        SheetPreviews(homeAfter.copy(lockThemeId = null, lockWallpaperUri = null), lockAfter, target.home, lockChanges)
        TargetPicker(target) { target = it }
        Column {
            if (target.home && ownWallpaper) {
                StudioCheckRow(
                    if (theme.wallpaperId != null) "Use the theme's wallpaper instead of mine" else "Use the theme's background instead of my wallpaper",
                    null, useThemeWallpaper, TestTags.OPT_THEME_BACKGROUND,
                ) { useThemeWallpaper = it }
            }
            if (target.home) {
                StudioCheckRow("Also set as phone wallpaper", "Shows behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
            }
            if (target.lock) {
                StudioCheckRow(
                    "Show themed lock screen while locked",
                    "Appears over your lock screen. Unlocking still needs your PIN, fingerprint or face.",
                    showLockScreen, TestTags.OPT_LOCK_SCREEN,
                ) { showLockScreen = it }
                StudioCheckRow(
                    "Also set as lock-screen wallpaper",
                    "Replaces your current one. Your phone's clock stays on top.",
                    setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER,
                ) { setLockWallpaper = it }
            }
        }
    }
}
