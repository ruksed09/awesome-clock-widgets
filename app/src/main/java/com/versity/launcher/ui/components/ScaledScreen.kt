package com.versity.launcher.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import com.versity.launcher.ui.home.LocalWallpaperScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.domain.lockLook
import com.versity.launcher.ui.home.HomeContent
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.lock.LockStyleContent
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Size every preview is laid out at before scaling: a typical phone in dp. */
val PreviewScreenWidth = 360.dp
val PreviewScreenHeight = 780.dp

private val PreviewNow: LocalDateTime = LocalDateTime.of(2026, 11, 11, 11, 11)
private val Corner = RoundedCornerShape(4.dp)

/**
 * Lays [content] out at a full phone size and draws it scaled down to [width], so previews show the
 * real screen rather than an approximation. The content never receives touches or accessibility focus;
 * [onClick], when set, makes the whole preview one button.
 */
@Composable
fun ScaledScreen(
    width: Dp,
    modifier: Modifier = Modifier,
    border: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clip(Corner)
            .then(if (border != null) Modifier.border(1.dp, border, Corner) else Modifier),
    ) {
        Box(
            Modifier
                .clearAndSetSemantics { }
                .layout { measurable, _ ->
                    val fullW = PreviewScreenWidth.roundToPx()
                    val fullH = PreviewScreenHeight.roundToPx()
                    val scale = width.toPx() / fullW
                    val placeable = measurable.measure(Constraints.fixed(fullW, fullH))
                    layout((fullW * scale).roundToInt(), (fullH * scale).roundToInt()) {
                        placeable.placeWithLayer(0, 0) {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(0f, 0f)
                        }
                    }
                },
        ) {
            val scale = with(LocalDensity.current) { width.toPx() / PreviewScreenWidth.toPx() }
            CompositionLocalProvider(LocalWallpaperScale provides scale) { content() }
        }
        // Topmost layer: takes every touch so the preview's own buttons never fire.
        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() } },
                ),
        )
    }
}

/** The home screen as it would look with [settings]. */
@Composable
fun HomePreview(settings: LauncherSettings, width: Dp, modifier: Modifier = Modifier, border: Color? = null, onClick: (() -> Unit)? = null) =
    ScaledScreen(width, modifier, border, onClick) {
        HomeContent(HomeUiState(settings = settings, now = PreviewNow, isDefaultLauncher = true))
    }

/** The lock screen as it would look with [settings] (its lock theme applied, if any). */
@Composable
fun LockPreview(settings: LauncherSettings, width: Dp, modifier: Modifier = Modifier, border: Color? = null, onClick: (() -> Unit)? = null) =
    ScaledScreen(width, modifier, border, onClick) {
        LockStyleContent(HomeUiState(settings = settings.lockLook(), now = PreviewNow, isDefaultLauncher = true), onUnlock = {})
    }
