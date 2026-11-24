package com.versity.launcher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.versity.launcher.ui.components.LauncherMenu
import com.versity.launcher.ui.components.LauncherMenuItem
import com.versity.launcher.ui.home.HomeBackground
import androidx.compose.ui.graphics.Color
import com.versity.launcher.data.LauncherSettings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.data.AppEntry
import com.versity.launcher.ui.theme.LauncherTheme

/** Stateless drawer: a thin search field above an adaptive grid of installed apps. */
@Composable
fun AppDrawerContent(
    state: DrawerUiState,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
    onToggleFavourite: (AppEntry) -> Unit = {},
    settings: LauncherSettings = LauncherSettings.DEFAULT,
) {
    val colors = LauncherTheme.colors
    Box(modifier.fillMaxSize()) {
    // Same background as home, under a dark scrim so icons and labels stay readable on any wallpaper.
    HomeBackground(settings)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)))
    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding()
    ) {
        SearchField(state.query, onQueryChange, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        Box(Modifier.fillMaxSize()) {
            when {
                state.loading -> Unit
                state.apps.isEmpty() -> Text(
                    text = if (state.query.isBlank()) "No apps found" else "No apps match \"${state.query.trim()}\"",
                    color = colors.muted,
                    fontFamily = LauncherTheme.font,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 32.dp),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 76.dp),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars),
                ) {
                    items(state.apps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                        AppCell(app, onLaunch, app.key in state.favouriteKeys, state.homeFull, onToggleFavourite)
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppCell(
    app: AppEntry,
    onLaunch: (AppEntry) -> Unit,
    isFavourite: Boolean,
    homeFull: Boolean,
    onToggleFavourite: (AppEntry) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onLaunch(app) }, onLongClick = { menu = true })
                .padding(horizontal = 4.dp, vertical = 8.dp),
        ) {
            AppIcon(app, size = 48.dp)
            Text(
                text = app.label,
                color = LauncherTheme.colors.onSurface,
                fontFamily = LauncherTheme.font,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        LauncherMenu(expanded = menu, onDismiss = { menu = false }) {
            LauncherMenuItem(
                label = if (isFavourite) "Remove from home" else if (homeFull) "Home is full (8)" else "Add to home",
                enabled = isFavourite || !homeFull,
            ) { menu = false; onToggleFavourite(app) }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 14.sp),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(1.dp, colors.border, RoundedCornerShape(2.dp)),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text("Search apps", color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 14.sp)
                }
                inner()
            }
        },
    )
}
