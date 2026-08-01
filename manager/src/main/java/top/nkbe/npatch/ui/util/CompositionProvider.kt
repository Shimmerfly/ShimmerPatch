package top.nkbe.npatch.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect

const val BG_SURFACE_ALPHA = 0.6f
private const val BG_OVERLAY_ALPHA = 0.35f
private const val HAZE_TINT_ALPHA = 0.8f

val LocalSnackbarHost = compositionLocalOf<SnackbarHostState> {
    error("CompositionLocal LocalSnackbarController not present")
}

val LocalBackgroundImagePath = compositionLocalOf { "" }

val LocalCardBackgroundAlpha = compositionLocalOf { BG_SURFACE_ALPHA }

val LocalFloatingGlassBottomBar = compositionLocalOf { false }

val LocalFloatingGlassBottomBarBlur = compositionLocalOf { true }

@Composable
fun backgroundAwareCardColors(
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundAlpha: Float = LocalCardBackgroundAlpha.current,
): CardColors {
    val adjusted = if (LocalBackgroundImagePath.current.isNotEmpty()) {
        color.copy(alpha = backgroundAlpha)
    } else {
        color
    }
    return CardDefaults.cardColors(
        containerColor = adjusted,
        contentColor = contentColor,
    )
}

@Composable
fun backgroundAwareColor(
    color: Color,
    backgroundAlpha: Float = BG_SURFACE_ALPHA,
): Color {
    return if (LocalBackgroundImagePath.current.isNotEmpty()) {
        color.copy(alpha = backgroundAlpha)
    } else {
        color
    }
}

@Composable
fun backgroundAwareHazeStyle(
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    backgroundAlpha: Float = BG_SURFACE_ALPHA,
    tintAlpha: Float = HAZE_TINT_ALPHA,
): HazeBlurStyle {
    val hasBackground = LocalBackgroundImagePath.current.isNotEmpty()
    return HazeBlurStyle(
        backgroundColor = if (hasBackground) Color.Transparent else surfaceColor,
        colorEffect = HazeColorEffect.tint(
            surfaceColor.copy(alpha = if (hasBackground) backgroundAlpha else tintAlpha)
        )
    )
}
