package com.versity.launcher.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.theme.LauncherTheme

/** Fixed dock: phone and app drawer. Icons are drawn here, so no icon library is needed. */
@Composable
fun Dock(onPhone: () -> Unit, onApps: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        DockButton("Phone", TestTags.DOCK_PHONE, onPhone) { PhoneGlyph() }
        DockButton("All apps", TestTags.DOCK_APPS, onApps) { AppsGlyph() }
    }
}

@Composable
private fun DockButton(description: String, tag: String, onClick: () -> Unit, glyph: @Composable () -> Unit) {
    Box(
        Modifier
            .testTag(tag)
            .size(52.dp)
            .border(1.dp, LauncherTheme.colors.border, RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { glyph() }
}

@Composable
private fun PhoneGlyph() {
    val color = LauncherTheme.colors.onSurface
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val p = Path().apply {
            moveTo(w * 0.22f, w * 0.14f)
            lineTo(w * 0.38f, w * 0.14f)
            lineTo(w * 0.46f, w * 0.34f)
            lineTo(w * 0.36f, w * 0.44f)
            cubicTo(w * 0.42f, w * 0.58f, w * 0.52f, w * 0.68f, w * 0.66f, w * 0.74f)
            lineTo(w * 0.76f, w * 0.64f)
            lineTo(w * 0.86f, w * 0.78f)
            lineTo(w * 0.86f, w * 0.88f)
            cubicTo(w * 0.40f, w * 0.92f, w * 0.08f, w * 0.60f, w * 0.12f, w * 0.20f)
            close()
        }
        drawPath(p, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun AppsGlyph() {
    val color = LauncherTheme.colors.onSurface
    Canvas(Modifier.size(22.dp)) {
        val step = size.width / 4f
        for (row in 1..3) for (col in 1..3) {
            drawCircle(color, radius = 1.8.dp.toPx(), center = Offset(step * col, step * row))
        }
    }
}
