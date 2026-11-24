package com.versity.launcher.ui.lock

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.ui.TestTags
import com.versity.launcher.data.HomeLayout
import com.versity.launcher.ui.home.HomeBackground
import com.versity.launcher.ui.home.components.PlacedClock
import com.versity.launcher.ui.home.components.CentredClockBlock
import androidx.compose.foundation.shape.RoundedCornerShape
import com.versity.launcher.ui.home.components.showsOwnDate
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.animationsEnabled

/** Distance, as a share of the screen height, the user must drag up before it unlocks. */
private const val UNLOCK_FRACTION = 0.25f

/**
 * The themed lock screen. It is shown over the system lock screen only while the phone is locked, and
 * unlocking goes through the phone's own PIN, pattern, fingerprint or face check. It never weakens the
 * phone's security. Unlocking is distance-only, so a quick flick of a few pixels never counts.
 */
@Composable
fun LockStyleContent(state: HomeUiState, onUnlock: () -> Unit, modifier: Modifier = Modifier) = AppTheme(state.settings) {
    val colors = LauncherTheme.colors
    val currentUnlock by rememberUpdatedState(onUnlock)
    val animate = animationsEnabled()
    var dragPx by remember { mutableFloatStateOf(0f) } // 0 or negative
    var heightPx by remember { mutableFloatStateOf(1f) }
    val dragState = rememberDraggableState { delta -> dragPx = (dragPx + delta).coerceAtMost(0f) }

    Box(
        modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .testTag(TestTags.LOCK_SCREEN)
            // Not clickable: a clickable root would merge its children's semantics into one node.
            .semantics { customActions = listOf(CustomAccessibilityAction("Unlock") { currentUnlock(); true }) }
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    // Past the threshold, ask to unlock; the system may show PIN or fingerprint first.
                    // Either way the screen settles back, so a cancelled prompt leaves it intact.
                    if (-dragPx > heightPx * UNLOCK_FRACTION) currentUnlock()
                    if (animate) {
                        animate(dragPx, 0f, animationSpec = tween(160)) { v, _ -> dragPx = v }
                    } else {
                        dragPx = 0f
                    }
                },
            ),
    ) {
        HomeBackground(state.settings)
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .graphicsLayer {
                    translationY = dragPx
                    alpha = 1f - ((-dragPx / heightPx) * 1.6f).coerceIn(0f, 1f)
                },
        ) {
        state.settings.clockPlacement?.let { PlacedClock(it, state.settings, state.now, state.is24Hour) }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.settings.layout) {
                // Centered: date and clock in the middle of the screen.
                HomeLayout.ORBIT -> {
                    Spacer(Modifier.weight(0.8f))
                    CentredClockBlock(state.now, state.is24Hour, state.settings)
                    Spacer(Modifier.weight(1f))
                }
                // Minimal: date and clock in the upper third.
                HomeLayout.MINIMAL -> {
                    Spacer(Modifier.weight(0.35f))
                    CentredClockBlock(state.now, state.is24Hour, state.settings)
                    Spacer(Modifier.weight(1f))
                }
            }
            LockHandle()
        }
        }
    }
}

@Composable
private fun LockHandle() {
    val colors = LauncherTheme.colors
    Column(Modifier.testTag(TestTags.LOCK_HANDLE), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(36.dp).height(4.dp).background(colors.onSurface.copy(alpha = 0.55f), RoundedCornerShape(2.dp)))
        Text(
            "Swipe up to unlock",
            color = colors.onSurface.copy(alpha = 0.7f),
            fontFamily = LauncherTheme.font,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
    }
}
