package com.versity.launcher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.versity.launcher.ui.theme.LauncherTheme

/** Dropdown in the launcher's own style: modest corners, theme surface, hairline border, no shadow. */
@Composable
fun LauncherMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LauncherTheme.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(10.dp),
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, colors.border),
        content = content,
    )
}

@Composable
fun LauncherMenuItem(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    DropdownMenuItem(
        text = { Text(label, fontFamily = LauncherTheme.font, fontSize = 14.sp) },
        enabled = enabled,
        onClick = onClick,
        colors = MenuDefaults.itemColors(textColor = colors.onSurface, disabledTextColor = colors.muted),
    )
}
