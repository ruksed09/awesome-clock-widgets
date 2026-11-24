package com.versity.launcher.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps the last decoded wallpapers so returning from the drawer doesn't flash the gradient while it re-decodes. */
private val wallpaperCache = android.util.LruCache<String, ImageBitmap>(2)

/** Draws the chosen wallpaper cropped to fill. Draws nothing for null or an unreadable URI. */
@Composable
fun WallpaperLayer(uri: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val cacheKey = "$uri@${size.width}x${size.height}"
    val bitmap by produceState<ImageBitmap?>(if (uri == null) null else wallpaperCache.get(cacheKey), cacheKey) {
        if (uri == null || size == IntSize.Zero) {
            value = null
        } else {
            val hit = wallpaperCache.get(cacheKey)
            value = hit ?: withContext(Dispatchers.IO) { decodeDownsampled(context, uri.toUri(), size.width, size.height) }
                ?.also { wallpaperCache.put(cacheKey, it) }
        }
    }
    Box(modifier.onSizeChanged { size = it }) {
        bitmap?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
    }
}

/** BitmapFactory with a power-of-two sample size, so a 50 MP photo never decodes at full size. */
internal fun decodeDownsampled(context: Context, uri: Uri, reqW: Int, reqH: Int): ImageBitmap? = runCatching {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= 28) {
        // ImageDecoder applies EXIF orientation itself and handles HEIC.
        return@runCatching ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val scale = max(1f, min(info.size.width / reqW.toFloat(), info.size.height / reqH.toFloat()))
            decoder.setTargetSize((info.size.width / scale).roundToInt(), (info.size.height / scale).roundToInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }.asImageBitmap()
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= reqW && bounds.outHeight / (sample * 2) >= reqH) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return@runCatching null
    // API 26-27: BitmapFactory ignores the EXIF orientation tag, so rotate by hand.
    val degrees = resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    val result = if (degrees == 0f) decoded else Bitmap.createBitmap(
        decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true,
    )
    result.asImageBitmap()
}.getOrNull()

/**
 * Keeps the picked wallpaper readable after a restart. First choice is a persistable read grant
 * on the picked URI; if the provider refuses, the image is copied into app-private storage.
 * Returns the string to store in settings.
 */
fun persistWallpaper(context: Context, uri: Uri): String {
    try {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return uri.toString()
    } catch (_: Exception) {
        // Fall through to the private copy.
    }
    val target = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.img")
    context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
    context.filesDir.listFiles { f -> f.name.startsWith("wallpaper_") && f != target }?.forEach { it.delete() }
    return Uri.fromFile(target).toString()
}
