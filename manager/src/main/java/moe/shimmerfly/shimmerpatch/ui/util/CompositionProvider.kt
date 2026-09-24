package moe.shimmerfly.shimmerpatch.ui.util

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import moe.shimmerfly.shimmerpatch.config.ThemeSettings

const val BG_SURFACE_ALPHA = 0.6f
val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }
val LocalThemeSettings = compositionLocalOf<ThemeSettings> { error("Theme settings have not loaded") }
val LocalBackgroundImagePath = compositionLocalOf { "" }
val LocalCardBackgroundAlpha = compositionLocalOf { BG_SURFACE_ALPHA }
val LocalFloatingGlassBottomBar = compositionLocalOf { false }
val LocalFloatingGlassBottomBarBlur = compositionLocalOf { true }

@Composable
fun backgroundAwareCardColors(
    color: Color = MaterialTheme.colorScheme.surfaceBright,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundAlpha: Float = LocalCardBackgroundAlpha.current,
): CardColors = CardDefaults.cardColors(
    containerColor = backgroundAwareColor(color, backgroundAlpha),
    contentColor = contentColor,
)

@Composable
fun backgroundAwareColor(color: Color, backgroundAlpha: Float = LocalCardBackgroundAlpha.current): Color =
    if (LocalBackgroundImagePath.current.isNotEmpty()) color.copy(alpha = backgroundAlpha) else color
