package com.versity.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.versity.launcher.data.AppEntry
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.components.LauncherMenu
import com.versity.launcher.ui.components.LauncherMenuItem
import com.versity.launcher.ui.drawer.AppIcon
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.drawGlyph
import com.versity.launcher.ui.theme.LauncherTheme

private val DockShape = RoundedCornerShape(20.dp)
private val IconSize = 46.dp
private val Gap = 10.dp

/** How many favourite icons fit in a dock [width] wide, next to the all-apps button. */
fun dockCapacity(width: Dp): Int = (((width - 28.dp - IconSize) / (IconSize + Gap)).toInt()).coerceIn(0, 6)

/**
 * The dock: up to [capacity] favourite apps without labels, then the all-apps button, on a slim
 * translucent panel with a hairline edge. With no favourites it offers the phone and all apps.
 * Long-press a favourite to remove it.
 */
@Composable
fun Dock(
    favourites: List<AppEntry>,
    capacity: Int,
    onLaunch: (AppEntry) -> Unit,
    onRemove: (AppEntry) -> Unit,
    onPhone: () -> Unit,
    onApps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauncherTheme.colors
    Row(
        modifier
            .testTag(TestTags.DOCK)
            .clip(DockShape)
            .background(colors.onSurface.copy(alpha = 0.08f), DockShape)
            .border(1.dp, colors.onSurface.copy(alpha = 0.12f), DockShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val apps = favourites.take(capacity)
        if (apps.isEmpty()) {
            DockGlyphButton("Phone", "dock_phone", Glyph.PHONE, onPhone)
        } else {
            apps.forEach { DockApp(it, onLaunch, onRemove) }
        }
        DockGlyphButton("All apps", TestTags.DOCK_APPS, Glyph.APPS, onApps)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockApp(app: AppEntry, onLaunch: (AppEntry) -> Unit, onRemove: (AppEntry) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .testTag(TestTags.dockApp(app.key))
                .size(IconSize)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { menu = true })
                .semantics { contentDescription = app.label; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) { AppIcon(app, size = IconSize) }
        LauncherMenu(expanded = menu, onDismiss = { menu = false }) {
            LauncherMenuItem("Remove from dock") { menu = false; onRemove(app) }
        }
    }
}

@Composable
private fun DockGlyphButton(description: String, tag: String, glyph: Glyph, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    Box(
        Modifier
            .testTag(tag)
            .size(IconSize)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.onSurface.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) { drawGlyph(glyph, colors.onSurface) }
    }
}

/** Small grabber above the dock, hinting that a swipe up opens all apps. */
@Composable
fun SwipeHint(modifier: Modifier = Modifier) {
    Box(modifier.width(28.dp).height(3.dp).background(LauncherTheme.colors.onSurface.copy(alpha = 0.35f), RoundedCornerShape(2.dp)))
}
