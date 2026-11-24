package com.versity.launcher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.ThemeBundle
import com.versity.launcher.domain.ThemeCategory
import com.versity.launcher.domain.ThemePresets
import com.versity.launcher.domain.Wallpapers
import com.versity.launcher.domain.isLight
import com.versity.launcher.domain.lockLook
import com.versity.launcher.domain.withTheme
import com.versity.launcher.system.PhotoLibrary
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.HomePreview
import com.versity.launcher.ui.home.WallpaperLayer
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioCheckRow
import com.versity.launcher.ui.studio.StudioChip
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioLabel
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioTextButton
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Grid tiles grow on tablets so a wide screen shows bigger previews rather than a sea of small ones. */
private fun tileMin(width: Dp, phone: Dp, tablet: Dp): Dp = if (width >= 700.dp) tablet else phone

// ---------------------------------------------------------------- Themes

@Composable
internal fun ThemesPage(settings: LauncherSettings, isDefault: Boolean, onOpen: (ThemeBundle) -> Unit) {
    var category by rememberSaveable { mutableStateOf<ThemeCategory?>(null) }
    val list = remember(category) { ThemePresets.themes.filter { category == null || it.category == category } }
    val home = currentHomeTheme(settings)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = tileMin(maxWidth, 116.dp, 156.dp)),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StudioChip("All", category == null, TestTags.category("ALL")) { category = null }
                        ThemeCategory.entries.forEach { cat ->
                            StudioChip(cat.label, category == cat, TestTags.category(cat.name)) { category = cat }
                        }
                    }
                    if (home == null) {
                        StudioHint(
                            if (isDefault) "Pick a theme to set the look of your home and lock screens." else "Pick a theme to style your wallpapers and Versity's lock screen.",
                            Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
            items(list, key = { it.id }) { theme ->
                val onHome = home?.id == theme.id
                val onLock = settings.lockThemeId == theme.id || (settings.lockThemeId == null && onHome && settings.lockStyleEnabled)
                ThemeCard(theme, Places(onHome, onLock)) { onOpen(theme) }
            }
        }
    }
}

/** Gallery card: the real home screen in this theme, scaled down, then its name and where it is applied. */
@Composable
private fun ThemeCard(theme: ThemeBundle, places: Places, onClick: () -> Unit) {
    val c = Studio.colors
    val look = remember(theme.id) { LauncherSettings.DEFAULT.withTheme(theme, useThemeWallpaper = true) }
    Column(
        Modifier
            .testTag(TestTags.theme(theme.id))
            .surface(c)
            .then(if (places.any) Modifier.border(1.5.dp, c.brand, StudioCardCorner) else Modifier)
            .clip(StudioCardCorner)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = places.any
                if (places.any) stateDescription = "Current on ${places.label.lowercase()}"
            },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
            HomePreview(look, maxWidth, onClick = onClick)
            CurrentTag(places.any, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }
        Column(Modifier.padding(horizontal = 4.dp).padding(bottom = 4.dp)) {
            StudioText(theme.name, StudioType.small.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
            StudioText(if (places.any) places.label else theme.tagline, StudioType.caption, if (places.any) c.brandText else c.muted, maxLines = 2)
        }
    }
}

// ---------------------------------------------------------------- Wallpapers

@Composable
internal fun WallpapersPage(settings: LauncherSettings, actions: CustomiseActions, isDefault: Boolean, onOpen: (String) -> Unit) {
    val ids = rememberPhoneWallpaperIds()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = tileMin(maxWidth, 100.dp, 136.dp)),
            contentPadding = PaddingValues(start = Gutter, end = Gutter, top = 4.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "photos_h") {
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        StudioLabel("Your photos", Modifier.padding(start = 4.dp))
                        StudioHint(
                            if (isDefault) "Use a photo on Versity's screens and as your phone's wallpaper."
                            else "Set any photo as your phone's home or lock-screen wallpaper.",
                            Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                    }
                    if (isDefault && settings.wallpaperUri != null && settings.wallpaperChosen) {
                        StudioTextButton("No wallpaper", Modifier.testTag(TestTags.CLEAR_WALLPAPER)) { actions.onClearWallpaper() }
                    }
                }
            }
            item(key = "add_photo") { AddPhotoTile { actions.onPickWallpaper() } }
            items(settings.photos, key = { "photo_$it" }) { uri ->
                WallpaperTile(uri, "Your photo", null, placesOf(uri, settings, isDefault, ids), TestTags.photo(uri)) { onOpen(uri) }
            }
            Wallpapers.collections.forEach { collection ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "h_$collection") {
                    StudioLabel(collection, Modifier.padding(start = 4.dp, top = 10.dp))
                }
                items(Wallpapers.all.filter { it.collection == collection }, key = { it.id }) { wp ->
                    WallpaperTile(wp.uri, wp.name, wp.thumb, placesOf(wp.uri, settings, isDefault, ids), TestTags.wallpaper(wp.id)) { onOpen(wp.uri) }
                }
            }
        }
    }
}

private val TileShape = RoundedCornerShape(10.dp)

/** A dashed-feel tile that opens the photo picker. */
@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    val c = Studio.colors
    Column(
        Modifier.testTag(TestTags.PICK_WALLPAPER).clip(TileShape).clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button; contentDescription = "Add a photo" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(9f / 19.5f).clip(TileShape).background(c.panel).border(1.dp, c.lineStrong, TileShape),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.background(c.surface, RoundedCornerShape(10.dp)).border(1.dp, c.line, RoundedCornerShape(10.dp)).padding(10.dp)) {
                    StudioGlyph(Glyph.PLUS, 18.dp, c.brandText)
                }
                StudioText("Add photo", StudioType.caption, c.muted)
            }
        }
        StudioText("From your phone", StudioType.small.copy(fontWeight = FontWeight.Medium), maxLines = 1, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

@Composable
private fun WallpaperTile(uri: String, name: String, thumb: Int?, places: Places, tag: String, onClick: () -> Unit) {
    val c = Studio.colors
    Column(
        Modifier
            .testTag(tag)
            .clip(TileShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = name
                selected = places.any
                if (places.any) stateDescription = "Current on ${places.label.lowercase()}"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 19.5f)
                .clip(TileShape)
                .background(c.panel)
                .border(if (places.any) 2.dp else 1.dp, if (places.any) c.brand else c.line, TileShape),
        ) {
            if (thumb != null) {
                Image(painterResource(thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                WallpaperLayer(uri, Modifier.fillMaxSize())
            }
            CurrentTag(places.any, Modifier.align(Alignment.BottomStart).padding(6.dp))
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            StudioText(name, StudioType.small.copy(fontWeight = FontWeight.Medium), maxLines = 1)
            if (places.any) StudioText(places.label, StudioType.caption, c.brandText, maxLines = 1)
        }
    }
}

/** Palette that keeps text readable on a wallpaper: light wallpapers need dark ink and vice versa. */
private fun readablePalette(settings: LauncherSettings, light: Boolean): String? {
    val currentLight = ThemePresets.palette(settings.paletteId).isLight()
    return when {
        light && !currentLight -> "porcelain"
        !light && currentLight -> "abyss"
        else -> null
    }
}

/** The bare image in a phone-shaped frame, for previews where Versity's own screens are not in play. */
@Composable
private fun WallpaperFrame(uri: String, width: Dp) {
    val shape = RoundedCornerShape(width * 0.08f)
    Box(Modifier.width(width).aspectRatio(9f / 19.5f).clip(shape).background(Color.Black).border(1.dp, Color.Black.copy(alpha = 0.12f), shape)) {
        WallpaperLayer(uri, Modifier.fillMaxSize())
    }
}

@Composable
internal fun WallpaperApplySheet(
    wp: WallpaperChoice,
    settings: LauncherSettings,
    isDefault: Boolean,
    onDismiss: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onApply: (WallpaperApplyRequest) -> Unit,
) {
    val context = LocalContext.current
    var target by rememberSaveable(wp.uri) { mutableStateOf(ApplyTarget.BOTH) }
    var setPhoneWallpaper by rememberSaveable(wp.uri) { mutableStateOf(true) }
    var setLockWallpaper by rememberSaveable(wp.uri) { mutableStateOf(true) }
    // Photos are measured; bundled wallpapers know whether they are light.
    val light by produceState(wp.light, wp.uri) {
        if (wp.isPhoto) value = withContext(Dispatchers.IO) { PhotoLibrary.isLight(context, wp.uri) }
    }
    val suggested = if (isDefault) readablePalette(settings, light) else null
    var matchText by rememberSaveable(wp.uri) { mutableStateOf(true) }
    val palette = suggested?.takeIf { matchText }
    // In use on Versity's own screens: the copy must stay, so it cannot be removed here.
    val inUse = settings.wallpaperUri == wp.uri || settings.lockWallpaperUri == wp.uri

    val homeAfter = if (target.home) settings.copy(wallpaperUri = wp.uri, paletteId = palette ?: settings.paletteId) else settings
    val lockAfter = if (target.lock) settings.lockLook().copy(wallpaperUri = wp.uri, paletteId = palette ?: settings.lockLook().paletteId)
    else homeAfter.lockLook()

    SheetScaffold(
        label = wp.collection, title = wp.name,
        subtitle = if (isDefault) "Wallpaper" else "Sets your phone's wallpaper",
        tag = TestTags.WALLPAPER_SHEET,
        onDismiss = onDismiss,
        confirm = "Set wallpaper",
        secondary = if (wp.isPhoto && !inUse) "Remove" to { onRemovePhoto(wp.uri) } else null,
        onConfirm = {
            onApply(
                WallpaperApplyRequest(
                    wallpaper = wp, target = target,
                    // Without Versity as the home app, the phone's own wallpaper is the whole point.
                    setPhoneWallpaper = target.home && (setPhoneWallpaper || !isDefault),
                    setLockWallpaper = target.lock && (setLockWallpaper || !isDefault),
                    paletteId = palette,
                ),
            )
        },
    ) {
        if (isDefault) {
            SheetPreviews(homeAfter.copy(lockThemeId = null, lockWallpaperUri = null), lockAfter.copy(lockThemeId = null, lockWallpaperUri = null), target.home, target.lock || target.home)
        } else {
            PreviewStage {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val each = min((maxWidth - 16.dp) / 2, 124.dp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                        LabelledPreview("Home screen", target.home) { WallpaperFrame(wp.uri, each) }
                        LabelledPreview("Lock screen", target.lock) { WallpaperFrame(wp.uri, each) }
                    }
                }
            }
        }
        TargetPicker(target) { target = it }
        Column {
            if (isDefault) {
                if (target.home) StudioCheckRow("Also set as phone wallpaper", "Shows behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
                if (target.lock) StudioCheckRow("Also set as lock-screen wallpaper", "Replaces your current one. Your phone's clock stays on top.", setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER) { setLockWallpaper = it }
                if (suggested != null) {
                    StudioCheckRow(
                        if (light) "Use dark text for this light wallpaper" else "Use light text for this dark wallpaper",
                        "Keeps the clock and labels easy to read.", matchText, TestTags.OPT_MATCH_TEXT,
                    ) { matchText = it }
                }
            } else {
                StudioHint("Your home screen app keeps its own icons and widgets; only the wallpaper changes. Some phones' theme stores can cover the lock-screen wallpaper.")
            }
        }
    }
}

/** Theme sheet: previews, where to apply, and what else to change on the phone. */
@Composable
internal fun ThemeApplySheet(
    theme: ThemeBundle,
    settings: LauncherSettings,
    isDefault: Boolean,
    onDismiss: () -> Unit,
    onApply: (ThemeApplyRequest) -> Unit,
) {
    var target by rememberSaveable(theme.id) { mutableStateOf(ApplyTarget.BOTH) }
    var useThemeWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var setPhoneWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }
    var showLockScreen by rememberSaveable(theme.id) { mutableStateOf(isDefault) }
    var setLockWallpaper by rememberSaveable(theme.id) { mutableStateOf(true) }

    // The user has a wallpaper of their own (photo, or one picked in Wallpapers) that this theme would replace.
    val ownWallpaper = settings.wallpaperChosen && settings.wallpaperUri != null && settings.wallpaperUri != theme.wallpaperUri
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
                    setPhoneWallpaper = target.home && (setPhoneWallpaper || !isDefault),
                    showLockScreen = target.lock && showLockScreen,
                    setLockWallpaper = target.lock && (setLockWallpaper || !isDefault),
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
            if (target.home && isDefault) {
                StudioCheckRow("Also set as phone wallpaper", "Shows behind apps and in recents.", setPhoneWallpaper, TestTags.OPT_PHONE_WALLPAPER) { setPhoneWallpaper = it }
            }
            if (target.lock) {
                StudioCheckRow(
                    "Show themed lock screen while locked",
                    if (isDefault) "Appears over your lock screen. Unlocking still needs your PIN, fingerprint or face."
                    else "Works while Versity is your home app. Unlocking still needs your PIN, fingerprint or face.",
                    showLockScreen, TestTags.OPT_LOCK_SCREEN,
                ) { showLockScreen = it }
                if (isDefault) {
                    StudioCheckRow(
                        "Also set as lock-screen wallpaper",
                        "Replaces your current one. Your phone's clock stays on top.",
                        setLockWallpaper, TestTags.OPT_LOCK_WALLPAPER,
                    ) { setLockWallpaper = it }
                }
            }
            if (!isDefault) {
                StudioHint("Sets the theme's wallpaper on your phone. The clock and widgets show when Versity is your home app; add a Versity widget to see them on any home screen.")
            }
        }
    }
}
