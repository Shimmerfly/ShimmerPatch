// Ported from WeKit ui/content/M3Blur.kt; see docs/UI_SOURCES.md.
package moe.shimmerfly.shimmerpatch.ui.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported

@Composable
fun rememberMaterial3BlurBackdrop(enabled: Boolean = true): LayerBackdrop? {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || !isRenderEffectSupported()) return null
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun LayerBackdrop?.m3AppBarColor(): Color =
    if (this != null) Color.Transparent else MaterialTheme.colorScheme.surfaceContainer

@Composable
fun Modifier.m3AppBarBlur(
    backdrop: LayerBackdrop?,
    enabled: Boolean = true,
    blurRadius: Float = 25f,
    blendAlpha: Float = 0.8f,
    shape: Shape = RectangleShape,
): Modifier {
    if (!enabled || backdrop == null) return this
    val blendColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = blendAlpha)
    return then(
        Modifier.textureBlur(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            colors = BlurColors(),
        )
            // Keep the reference's M3 tint on the ordinary canvas. A missing backdrop frame
            // (including renderer recreation) must never make text under the bar readable.
            .background(blendColor, shape),
    )
}

/** Records the modified content into [backdrop] so an app-bar blur layer can sample it. */
fun Modifier.m3BackdropLayer(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null) layerBackdrop(backdrop) else this
