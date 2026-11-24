package com.versity.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.theme.LauncherTheme
import java.time.LocalDate

private val Corner = RoundedCornerShape(2.dp)

@Composable
fun BatteryBar(percent: Int?, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    val label = percent?.let { "$it%" } ?: "--"
    Column(
        modifier
            .testTag(TestTags.BATTERY_BAR)
            .semantics { contentDescription = "Battery $label" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Battery", color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)
            Text(label, color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(colors.border, Corner)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent ?: 0) / 100f)
                    .background(colors.accent, Corner),
            )
        }
    }
}

/** Day of month as a thin progress ring, with "day / days-in-month" beside the number. */
@Composable
fun DateRing(date: LocalDate, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    val total = date.lengthOfMonth()
    val progress = date.dayOfMonth / total.toFloat()
    Box(
        modifier
            .testTag(TestTags.DATE_RING)
            .aspectRatio(1f)
            .semantics { contentDescription = "Day ${date.dayOfMonth} of $total" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxHeight().aspectRatio(1f)) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(colors.border, 0f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke))
            drawArc(
                colors.accent, -90f, 360f * progress, false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Butt),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${date.dayOfMonth}", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text("/ $total", color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 10.sp)
        }
    }
}

/**
 * Rectangular notification chip. v1 requests no notification access, so it shows a dash
 * instead of an invented count.
 */
@Composable
fun NotificationPill(modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    Row(
        modifier
            .border(1.dp, colors.border, Corner)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(6.dp).background(colors.accent))
        Text("Notifications", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 12.sp)
        Text("—", color = colors.muted, fontFamily = LauncherTheme.font, fontSize = 12.sp)
    }
}

/** Battery card used by the Orbit layout: glyph, percentage, label and a thin bar. */
@Composable
fun BatteryLevel(percent: Int?, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    val label = percent?.let { "$it%" } ?: "--"
    Column(
        modifier
            .testTag(TestTags.BATTERY_BAR)
            .semantics { contentDescription = "Battery level $label" },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(width = 12.dp, height = 18.dp)) {
                val sw = 1.5.dp.toPx()
                drawRect(colors.onSurface, topLeft = androidx.compose.ui.geometry.Offset(sw / 2, 2.dp.toPx()),
                    size = Size(size.width - sw, size.height - 2.dp.toPx() - sw / 2), style = Stroke(sw))
                drawRect(colors.onSurface, topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.3f, 0f), size = Size(size.width * 0.4f, 2.dp.toPx()))
                val fill = (percent ?: 0) / 100f
                val inner = size.height - 2.dp.toPx() - sw * 2
                drawRect(colors.onSurface, topLeft = androidx.compose.ui.geometry.Offset(sw * 1.5f, 2.dp.toPx() + sw + inner * (1 - fill)),
                    size = Size(size.width - sw * 3, inner * fill))
            }
            Text(label, color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Text("Battery Level", color = colors.onSurface, fontFamily = LauncherTheme.font, fontSize = 16.sp)
        Box(Modifier.fillMaxWidth().height(4.dp).background(colors.border, Corner)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((percent ?: 0) / 100f).background(colors.onSurface, Corner))
        }
    }
}
