package com.versity.launcher.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import kotlin.math.max

/**
 * The user's own wallpaper photos. Each pick is saved as a private copy (scaled to at most 2880px on
 * the long edge), so it can be applied again later even after the picker's access has expired.
 */
object PhotoLibrary {
    private const val DIR = "photos"
    private const val MAX_EDGE = 2880

    fun folder(context: Context) = File(context.filesDir, DIR).apply { mkdirs() }

    /** Copies [uri] into the library and returns the copy's file URI. Call off the main thread. */
    fun add(context: Context, uri: Uri): String {
        val target = File(folder(context), "photo_${System.currentTimeMillis()}.jpg")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_EDGE) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        if (bitmap != null) {
            val scale = (MAX_EDGE.toFloat() / max(bitmap.width, bitmap.height)).coerceAtMost(1f)
            // The copy drops EXIF, so apply the camera's rotation to the pixels now.
            val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation(context, uri)) }
            val sized = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            target.outputStream().use { sized.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } else {
            // Not decodable here (an unusual format): keep the original bytes; the wallpaper layer tries again.
            context.contentResolver.openInputStream(uri)!!.use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        return Uri.fromFile(target).toString()
    }

    private fun rotation(context: Context, uri: Uri): Float = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)

    /** Deletes a library copy. URIs that are not library copies are left alone. */
    fun remove(context: Context, uri: String) {
        val file = Uri.parse(uri).path?.let(::File) ?: return
        if (file.parentFile?.canonicalPath == folder(context).canonicalPath) file.delete()
    }

    /** Deletes copies no longer listed in [keep], plus copies left by versions before 0.5. */
    fun prune(context: Context, keep: Collection<String>) {
        val kept = keep.mapNotNull { Uri.parse(it).path?.let(::File)?.canonicalPath }.toSet()
        folder(context).listFiles()?.filterNot { it.canonicalPath in kept }?.forEach { it.delete() }
        context.filesDir.listFiles { f -> f.name.startsWith("wallpaper_") && f.canonicalPath !in kept }?.forEach { it.delete() }
    }

    /** True when the photo is mostly light, so text on it should be dark. Call off the main thread. */
    fun isLight(context: Context, uri: String): Boolean = runCatching {
        val small = context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 32 })
        } ?: return false
        var sum = 0.0
        val w = small.width
        val h = small.height
        // The upper half matters most: that is where the clock sits.
        for (y in 0 until h / 2) for (x in 0 until w) {
            val p = small.getPixel(x, y)
            sum += (0.2126 * ((p shr 16) and 0xFF) + 0.7152 * ((p shr 8) and 0xFF) + 0.0722 * (p and 0xFF)) / 255.0
        }
        sum / (w * (h / 2).coerceAtLeast(1)) > 0.6
    }.getOrDefault(false)
}
