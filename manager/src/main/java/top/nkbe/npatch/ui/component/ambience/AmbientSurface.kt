package top.nkbe.npatch.ui.component.ambience

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.android.awaitFrame
import android.content.Context
import androidx.compose.ui.platform.LocalContext

/**
 * The header's living background.
 *
 * Draws whichever [AmbienceRenderer] is selected and hands it taps. It sits *behind* the header's
 * content, so the settings and details buttons above it keep working normally — only the open
 * space responds.
 *
 * The frame loop parks itself on a single frame per wake-up whenever the renderer reports nothing
 * moving, and [AmbienceKind.None] skips the loop entirely. A status header is on screen for as long
 * as someone reads the activity feed, so an idle animation is not free.
 */
@Composable
fun AmbientSurface(kind: AmbienceKind, tint: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember(context) {
        context.getSharedPreferences("header_ambience", Context.MODE_PRIVATE)
    }
    // Restored before the first frame, so the header comes back the size it was left rather than
    // snapping from the default once the setting loads.
    val renderer =
        remember(kind) {
            rendererFor(kind)?.apply {
                scale = settings.getFloat("scale_${kind.key}", 1f)
                speed = settings.getFloat("speed_${kind.key}", 1f)
                variant = settings.getInt("variant_${kind.key}", 0)
            }
        } ?: return
    val haptics = LocalHapticFeedback.current

    // Bumped every frame purely to invalidate the Canvas; the renderer owns the real state.
    var frame by remember(kind) { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Drives the simulation. Suspends while nothing is moving rather than spinning on frames that
    // would draw an identical picture.
    androidx.compose.runtime.LaunchedEffect(kind) {
        var last = 0L
        while (true) {
            if (!renderer.isAnimating) {
                // Cheap park: one frame per wake-up until something starts moving again.
                awaitFrame()
                last = 0L
                continue
            }
            withFrameNanos { now ->
                val dt = if (last == 0L) 16f else (now - last) / 1_000_000f
                last = now
                renderer.update(dt.coerceAtMost(64f), canvasSize)
                frame += 1f
            }
        }
    }

    val measurer = rememberTextMeasurer()
    // The matrix renderer draws text, which a DrawScope cannot measure on its own.
    (renderer as? MatrixRenderer)?.textMeasurer = measurer

    Canvas(
        modifier =
            modifier
                // Purely decorative, and it sits behind labelled controls — announcing it would
                // add noise to every pass over the header.
                .clearAndSetSemantics {}
                .pointerInput(kind) {
                    detectTapGestures(
                        // Only where it means something: passing a handler makes every single tap
                        // wait for the double-tap timeout before it fires.
                        onDoubleTap =
                            if (!renderer.hasVariants) null
                            else {
                                {
                                    renderer.onDoubleTap()
                                    settings.edit().putInt("variant_${kind.key}", renderer.variant).apply()
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                }
                            },
                        onTap = { offset ->
                            renderer.onTap(offset, size.toSize())
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        },
                        onLongPress = { offset ->
                            renderer.onLongPress(offset, size.toSize())
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        // A long press is only held while the finger is down, so the release has
                        // to come from the press handler rather than from onLongPress returning.
                        onPress = {
                            tryAwaitRelease()
                            renderer.onRelease()
                        },
                    )
                }
                .pointerInput(kind) {
                    // Drag and pinch, in one pass so they cannot fight each other. Taps are
                    // handled above; this gesture detector deliberately ignores them.
                    detectTransformGestures(panZoomLock = false) { centroid, pan, gestureZoom, _ ->
                        if (gestureZoom != 1f) {
                            // The renderer clamps to its own range, so what is stored is what it
                            // settled on rather than what the fingers asked for.
                            renderer.scale *= gestureZoom
                            settings.edit().putFloat("scale_${kind.key}", renderer.scale).apply()
                        }
                        if (pan != Offset.Zero) {
                            renderer.onDrag(pan, centroid, size.toSize())
                            settings.edit().putFloat("speed_${kind.key}", renderer.speed).apply()
                        }
                    }
                }
    ) {
        canvasSize = size
        // Read so Compose redraws when the frame counter advances.
        @Suppress("UNUSED_EXPRESSION") frame
        with(renderer) { render(tint) }
    }
}
