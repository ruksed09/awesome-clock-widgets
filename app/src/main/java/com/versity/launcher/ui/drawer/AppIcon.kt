package com.versity.launcher.ui.drawer

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.versity.launcher.data.AppEntry
import com.versity.launcher.system.AppLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache = LruCache<String, ImageBitmap>(256)

private fun loadIcon(launcher: AppLauncher, app: AppEntry, px: Int): ImageBitmap? {
    val key = "${app.key}@$px"
    iconCache.get(key)?.let { return it }
    return runCatching { launcher.icon(app)?.toBitmap(px, px)?.asImageBitmap() }.getOrNull()
        ?.also { iconCache.put(key, it) }
}

@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val launcher = remember(context) { AppLauncher(context) }
    val px = with(LocalDensity.current) { size.roundToPx() }
    val cached = iconCache.get("${app.key}@$px")
    val bitmap by produceState(cached, app.key, px) {
        value = withContext(Dispatchers.Default) { loadIcon(launcher, app, px) }
    }
    Box(modifier.size(size)) {
        bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
