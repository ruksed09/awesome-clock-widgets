package com.versity.launcher.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.versity.launcher.ui.components.LauncherMenu
import com.versity.launcher.ui.components.LauncherMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.AppEntry
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.drawer.AppIcon
import com.versity.launcher.ui.theme.LauncherTheme

/** Apps the user pinned from the drawer. Tap launches; long-press offers "Remove from home". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavouritesRow(
    apps: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onRemove: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (apps.isEmpty()) return
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4,
    ) {
        apps.forEach { app -> FavouriteCell(app, onLaunch, onRemove) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavouriteCell(app: AppEntry, onLaunch: (AppEntry) -> Unit, onRemove: (AppEntry) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        // The tag sits on the clickable node itself so tests and TalkBack find the same element.
        Column(
            Modifier
                .testTag(TestTags.favourite(app.key))
                .width(72.dp)
                .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { menu = true })
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(app, size = 44.dp)
            Text(
                app.label,
                color = LauncherTheme.colors.onSurface,
                fontFamily = LauncherTheme.font,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        LauncherMenu(expanded = menu, onDismiss = { menu = false }) {
            LauncherMenuItem("Remove from home") { menu = false; onRemove(app) }
        }
    }
}
