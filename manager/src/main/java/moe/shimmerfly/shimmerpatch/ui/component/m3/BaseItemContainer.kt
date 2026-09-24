// Adapted from WeKit: app/src/main/java/dev/ujhhgtg/wekit/ui/content/m3/BaseItemContainer.kt
// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package moe.shimmerfly.shimmerpatch.ui.component.m3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import moe.shimmerfly.shimmerpatch.ui.util.backgroundAwareCardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

internal val LocalSettingsContainerOwnsBackground = compositionLocalOf { false }

@Composable
fun BaseItemContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Read the dynamic shape from the SegmentedColumn environment
    val baseShape = LocalSegmentedItemShape.current
    val backgroundColor = backgroundAwareCardColors().containerColor

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(baseShape)
            .background(backgroundColor),
    ) {
        CompositionLocalProvider(LocalSettingsContainerOwnsBackground provides true) { content() }
    }
}
