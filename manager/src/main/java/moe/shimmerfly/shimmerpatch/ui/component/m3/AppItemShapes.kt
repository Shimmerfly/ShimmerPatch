// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2025-2026 InstallerX Revived contributors
// Ported from InstallerX-Revived ui/theme/Shape.kt.
package moe.shimmerfly.shimmerpatch.ui.component.m3

import androidx.compose.foundation.shape.RoundedCornerShape

// Define shapes for the segmented list style.

val topShape = RoundedCornerShape(
    topStart = CornerRadius,
    topEnd = CornerRadius,
    bottomStart = ConnectionRadius,
    bottomEnd = ConnectionRadius,
)
val middleShape = RoundedCornerShape(ConnectionRadius)
val bottomShape = RoundedCornerShape(
    topStart = ConnectionRadius,
    topEnd = ConnectionRadius,
    bottomStart = CornerRadius,
    bottomEnd = CornerRadius,
)
val singleShape = RoundedCornerShape(CornerRadius)
