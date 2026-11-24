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
import com.versity.launcher.ui.home.ClockBlock
import com.versity.launcher.ui.home.HomeBackground
import com.versity.launcher.ui.home.HomeDateFormat
import com.versity.launcher.ui.home.components.BatteryBar
import com.versity.launcher.ui.home.components.CenteredClock
import com.versity.launcher.ui.home.components.showsOwnDate
import com.versity.launcher.ui.home.components.DateRing
import com.versity.launcher.ui.home.HomeUiState
import com.versity.launcher.ui.home.components.NotificationPill
import com.versity.launcher.ui.home.components.OrbitHeader
import com.versity.launcher.ui.theme.AppTheme
import com.versity.launcher.ui.theme.LauncherTheme
import com.versity.launcher.ui.theme.animationsEnabled

/** Distance, as a share of the screen height, the user must drag up before it unlocks. */
private const val UNLOCK_FRACTION = 0.25f

/**
 * A lock-style screen shown inside the launcher. It is cosmetic: it does not secure the phone and the
 * system lock screen is unaffected. Unlocking is distance-only, so a quick flick of a few pixels never counts.
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
                    if (-dragPx > heightPx * UNLOCK_FRACTION) {
                        currentUnlock()
                    } else if (animate) {
                        animate(dragPx, 0f, animationSpec = tween(160)) { v, _ -> dragPx = v }
                    } else {
                        dragPx = 0f
                    }
                },
            ),
    ) {
        HomeBackground(state.settings)
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .graphicsLayer {
                    translationY = dragPx
                    alpha = 1f - ((-dragPx / heightPx) * 1.6f).coerceIn(0f, 1f)
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            when (state.settings.layout) {
                // Orbit: centred date and clock, widget row, chip.
                HomeLayout.ORBIT -> {
                    OrbitHeader(state.now, state.is24Hour, state.settings, state.batteryPercent)
                    Spacer(Modifier.weight(1f))
                    NotificationPill()
                    Spacer(Modifier.height(32.dp))
                }
                // Classic: left-aligned clock and date, chip, then battery and date ring above the handle.
                HomeLayout.CLASSIC -> {
                    Column(Modifier.fillMaxWidth()) { ClockBlock(state) }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth()) { NotificationPill() }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        BatteryBar(state.batteryPercent, Modifier.weight(1f))
                        Spacer(Modifier.width(24.dp))
                        DateRing(state.now.toLocalDate(), Modifier.size(76.dp))
                    }
                    Spacer(Modifier.height(32.dp))
                }
                // Minimal: just the date, a centred clock and the handle.
                HomeLayout.MINIMAL -> {
                    Spacer(Modifier.weight(0.6f))
                    if (!state.settings.showsOwnDate) {
                        Text(
                            state.now.format(HomeDateFormat),
                            color = colors.onSurface.copy(alpha = 0.85f),
                            fontFamily = LauncherTheme.font,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    CenteredClock(state.settings, state.now, state.is24Hour)
                    Spacer(Modifier.weight(1f))
                }
            }
            LockHandle()
        }
    }
}

@Composable
private fun LockHandle() {
    val colors = LauncherTheme.colors
    Column(Modifier.testTag(TestTags.LOCK_HANDLE), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.width(40.dp).height(2.dp).background(colors.onSurface.copy(alpha = 0.6f)))
        Text(
            "Slide up to unlock",
            color = colors.muted,
            fontFamily = LauncherTheme.font,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
        )
    }
}
