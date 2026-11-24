package com.versity.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.versity.launcher.system.Release
import com.versity.launcher.system.UpdateState
import com.versity.launcher.ui.TestTags
import com.versity.launcher.ui.studio.Glyph
import com.versity.launcher.ui.studio.Studio
import com.versity.launcher.ui.studio.StudioButton
import com.versity.launcher.ui.studio.StudioGlyph
import com.versity.launcher.ui.studio.StudioPrimaryButton
import com.versity.launcher.ui.studio.StudioSection
import com.versity.launcher.ui.studio.StudioSwitchRow
import com.versity.launcher.ui.studio.StudioText
import com.versity.launcher.ui.studio.StudioType
import com.versity.launcher.ui.studio.surface

/** What the update UI can ask for. */
internal class UpdateActions(
    val check: () -> Unit,
    val download: (Release) -> Unit,
    val install: (UpdateState.Ready) -> Unit,
)

/** First lines of the release notes, without Markdown marks, for a short "what's new". */
private fun summary(notes: String): String =
    notes.lineSequence().map { it.trim().removePrefix("- ").removePrefix("* ").replace("**", "").removePrefix("## ") }
        .filter { it.isNotBlank() && !it.startsWith("Versity ") && !it.startsWith("Download") && !it.startsWith("Signed") }
        .take(2).joinToString(" ")

/**
 * Banner shown only while there is something to do: an update to get, a download, or one to install.
 * [compact] stacks it for the tablet sidebar.
 */
@Composable
internal fun UpdateBanner(state: UpdateState, actions: UpdateActions, compact: Boolean = false) {
    val c = Studio.colors
    val release = when (state) {
        is UpdateState.Available -> state.release
        is UpdateState.Downloading -> state.release
        is UpdateState.Ready -> state.release
        is UpdateState.Failed -> state.release
        else -> null
    } ?: return
    val status = when (state) {
        is UpdateState.Downloading -> "Downloading, ${(state.progress * 100).toInt()}%"
        is UpdateState.Ready -> "Downloaded and checked. Ready to install."
        is UpdateState.Failed -> state.message
        else -> summary(release.notes).ifBlank { "A new version from GitHub." }
    }
    val statusColor = if (state is UpdateState.Failed) c.danger else c.muted
    Column(
        Modifier.testTag(TestTags.UPDATE_BANNER).fillMaxWidth().surface(c).padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (compact) {
            StudioText("Versity ${release.version} is available", StudioType.bodyStrong)
            StudioText(status, StudioType.small, statusColor, maxLines = 3)
            if (state is UpdateState.Downloading) ProgressLine(state.progress)
            UpdateButton(state, actions)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.background(c.brandSoft, RoundedCornerShape(8.dp)).padding(7.dp)) { StudioGlyph(Glyph.RESET, 16.dp, c.brandText) }
                Column(Modifier.weight(1f)) {
                    StudioText("Versity ${release.version} is available", StudioType.bodyStrong, maxLines = 1)
                    StudioText(status, StudioType.small, statusColor, maxLines = 2)
                }
                UpdateButton(state, actions)
            }
            if (state is UpdateState.Downloading) ProgressLine(state.progress)
        }
    }
}

@Composable
private fun UpdateButton(state: UpdateState, actions: UpdateActions) {
    when (state) {
        is UpdateState.Available -> StudioPrimaryButton("Download", Modifier.testTag(TestTags.UPDATE_ACTION)) { actions.download(state.release) }
        is UpdateState.Ready -> StudioPrimaryButton("Install", Modifier.testTag(TestTags.UPDATE_ACTION)) { actions.install(state) }
        is UpdateState.Failed -> state.release?.let { r -> StudioButton("Try again", Modifier.testTag(TestTags.UPDATE_ACTION)) { actions.download(r) } }
            ?: StudioButton("Try again", Modifier.testTag(TestTags.UPDATE_ACTION)) { actions.check() }
        is UpdateState.Downloading -> Unit
        else -> StudioButton("Check now", Modifier.testTag(TestTags.UPDATE_ACTION)) { actions.check() }
    }
}

@Composable
private fun ProgressLine(progress: Float) {
    val c = Studio.colors
    Box(
        Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 4.dp).height(3.dp).background(c.line, RoundedCornerShape(2.dp))
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(c.brand, RoundedCornerShape(2.dp)))
    }
}

/** Settings section: the version, what the last check found, the action that fits, and the automatic-check switch. */
@Composable
internal fun UpdatesSection(state: UpdateState, version: String, auto: Boolean, onAuto: (Boolean) -> Unit, actions: UpdateActions) {
    val c = Studio.colors
    StudioSection("Updates") {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                StudioText(if (version.isNotBlank()) "Versity $version" else "Versity", StudioType.body)
                StudioText(
                    when (state) {
                        UpdateState.Idle -> "Checks GitHub for new versions."
                        UpdateState.Checking -> "Checking…"
                        is UpdateState.UpToDate -> "You have the latest version."
                        is UpdateState.Available -> "Version ${state.release.version} is available."
                        is UpdateState.Downloading -> "Downloading ${state.release.version}, ${(state.progress * 100).toInt()}%"
                        is UpdateState.Ready -> "Version ${state.release.version} is ready to install."
                        is UpdateState.Failed -> state.message
                    },
                    StudioType.small, if (state is UpdateState.Failed) c.danger else c.muted,
                )
            }
            if (state != UpdateState.Checking) UpdateButton(state, actions)
        }
        if (state is UpdateState.Downloading) Box(Modifier.padding(start = 16.dp)) { ProgressLine(state.progress) }
        StudioSwitchRow(
            "Check automatically", auto, TestTags.UPDATE_AUTO, divider = true,
            detail = "When you open Versity, at most once an hour. Updates are only installed when you tap Install.",
        ) { onAuto(it) }
    }
}
