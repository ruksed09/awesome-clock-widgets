package com.versity.launcher.util

import kotlin.math.roundToInt

/** The part of a source image, in source pixels, that fills the destination after a centre-crop. */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

fun centerCrop(srcW: Int, srcH: Int, dstW: Int, dstH: Int): CropRect {
    if (srcW <= 0 || srcH <= 0 || dstW <= 0 || dstH <= 0) return CropRect(0, 0, srcW.coerceAtLeast(0), srcH.coerceAtLeast(0))
    val srcAspect = srcW.toDouble() / srcH
    val dstAspect = dstW.toDouble() / dstH
    return if (srcAspect > dstAspect) {
        val w = (srcH * dstAspect).roundToInt().coerceIn(1, srcW)
        CropRect((srcW - w) / 2, 0, w, srcH)
    } else {
        val h = (srcW / dstAspect).roundToInt().coerceIn(1, srcH)
        CropRect(0, (srcH - h) / 2, srcW, h)
    }
}
