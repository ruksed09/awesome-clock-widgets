package com.versity.launcher.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.versity.launcher.R
import com.versity.launcher.data.LauncherSettings
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.home.components.HomeWidgetLooks
import com.versity.launcher.ui.home.components.WidgetView
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioCardCorner
import com.versity.launcher.ui.studio.StudioCorner
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioHint
import com.versity.launcher.ui.studio.StudioRow
import com.versity.launcher.ui.studio.StudioSection
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface
import com.versity.launcher.widget.WidgetSpec
import com.versity.launcher.widget.WidgetStore
import com.versity.launcher.widget.WidgetType
import com.versity.launcher.widget.Widgets

@Composable
internal fun WidgetsPage(
    settings: LauncherSettings,
    widgetStatus: String?,
    onCreate: (WidgetType) -> Unit,
    onEdit: (StudioTarget) -> Unit,
    onArrange: () -> Unit,
) {
    val c = Studio.colors
    val context = LocalContext.current
    val store = remember(context) { WidgetStore(context) }
    var placed by remember { mutableStateOf(emptyList<Pair<Int, WidgetType>>()) }
    LaunchedEffect(Unit) { placed = Widgets.placed(context) }
    val looksVersion = HomeWidgetLooks.version.intValue

    PageBody(max = 1040.dp) {
        StudioHint(
            "Design a widget, then add it to your home screen. Widgets keep time on their own, so they never wake your phone.",
            Modifier.padding(horizontal = 4.dp),
        )
        if (widgetStatus != null) {
            StudioText(widgetStatus, StudioType.small, c.ink, Modifier.fillMaxWidth().background(c.brandSoft, StudioCorner).padding(12.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = when {
                maxWidth >= 840.dp -> 4
                maxWidth < 300.dp -> 1
                else -> 2
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Clocks" to false, "Collection" to true).forEach { (label, collection) ->
                    com.versity.launcher.ui.studio.StudioLabel(label, Modifier.padding(start = 4.dp, top = 4.dp))
                    WidgetType.entries.filter { it.collection == collection }.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { type -> TypeCard(type, Modifier.weight(1f)) { onCreate(type) } }
                            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        if (settings.homeWidgets.isNotEmpty()) {
            val specs = remember(settings.homeWidgets, looksVersion) { settings.homeWidgets.map { it to store.home(it.id) } }
            StudioSection("On Versity's home screen") {
                specs.forEachIndexed { i, (w, spec) ->
                    PlacedRow(
                        spec ?: WidgetSpec.default(WidgetType.DIGITAL), divider = i > 0, tag = TestTags.placedWidget("h${w.id}"),
                    ) { onEdit(StudioTarget.Home(w.id)) }
                }
            }
            StudioButton("Move and resize on the home screen", glyph = Glyph.MOVE) { onArrange() }
        }

        if (placed.isNotEmpty()) {
            StudioSection("On your home screen app") {
                placed.forEachIndexed { i, (id, type) ->
                    PlacedRow(store.system(id, type), divider = i > 0, tag = TestTags.placedWidget("s$id")) { onEdit(StudioTarget.System(id, type)) }
                }
            }
        }
        StudioHint(
            "You can also add Versity widgets from your home screen app's widget list. The studio opens so you can style each one, and long-pressing a placed widget lets you edit it again.",
            Modifier.padding(horizontal = 4.dp),
        )
    }
}

/** A widget family: its default look, live, on a wallpaper crop, then its name. */
@Composable
private fun TypeCard(type: WidgetType, modifier: Modifier, onClick: () -> Unit) {
    val c = Studio.colors
    val spec = remember(type) { WidgetSpec.default(type) }
    Column(
        modifier
            .testTag(TestTags.widgetType(type.name))
            .surface(c)
            .clip(StudioCardCorner)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
    ) {
        Box(
            Modifier.fillMaxWidth().height(128.dp).padding(6.dp).clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.wp_mesh_ink_thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            val (w, h) = if (spec.aspect >= 1.6f) 200 to 96 else 104 to 104
            val s = minOf(140f / w, 100f / h)
            Box(Modifier.size((w * s).dp, (h * s).dp)) {
                Box(Modifier.requiredSize(w.dp, h.dp).graphicsLayer { scaleX = s; scaleY = s }) { WidgetView(spec, Modifier.fillMaxSize()) }
            }
            // "Add" sits on the preview, so the name below has the card's full width.
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).background(c.surface.copy(alpha = 0.92f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) { StudioGlyph(Glyph.PLUS, 14.dp, c.brandText) }
        }
        Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)) {
            StudioText(type.title, StudioType.cardTitle, maxLines = 1)
            StudioText(type.description, StudioType.small, c.muted, maxLines = 2, minLines = 2)
        }
    }
}

/** A placed widget: a small live preview, its family and layout, and a way to edit it. */
@Composable
private fun PlacedRow(spec: WidgetSpec, divider: Boolean, tag: String, onClick: () -> Unit) {
    val c = Studio.colors
    StudioRow(
        title = spec.type.title,
        detail = spec.style.label,
        divider = divider,
        modifier = Modifier.testTag(tag),
        trailing = {
            Box(
                Modifier.size(width = 72.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)).background(c.panel).heightIn(min = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.wp_mesh_ink_thumb), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                val (w, h) = if (spec.aspect >= 1.6f) 180 to 84 else 90 to 90
                val s = minOf(68f / w, 36f / h)
                Box(Modifier.size((w * s).dp, (h * s).dp)) {
                    Box(Modifier.requiredSize(w.dp, h.dp).graphicsLayer { scaleX = s; scaleY = s }) { WidgetView(spec, Modifier.fillMaxSize()) }
                }
            }
        },
        onClick = onClick,
    )
}
