package org.matrix.vector.manager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.random.Random
import kotlinx.coroutines.delay
import org.matrix.vector.manager.R
import org.matrix.vector.manager.ui.components.ambience.AmbienceKind
import org.matrix.vector.manager.ui.components.ambience.AmbientSurface

/** The four states the framework can be in, plus the moment before we know. */
enum class FrameworkState {
    Checking,
    Active,
    Degraded,
    Inactive,

    /**
     * The framework is running, and this manager cannot talk to it.
     *
     * Distinct from [Inactive], which means there is no framework. Here there is one, it pushed
     * us a binder, and that binder speaks a different generation of `IManagerService` — so every
     * transaction would fail and the honest thing to say is that the two builds are out of step,
     * not that nothing is installed. Reached only through `ServiceLocator.peerDescriptor`.
     */
    Mismatched,
}

/**
 * The framework's state, as the top of the app.
 *
 * There is no app bar above it and no separate status row. A bar naming the app spends a whole row
 * on what the launcher icon, the task switcher and the system already say; that row goes instead to
 * the one thing that is genuinely unknown on opening the app.
 *
 * The header is full-bleed and runs under the status bar, tinted by state, with only its bottom
 * corners rounded so it reads as a single pane hanging from the top edge rather than a card
 * floating on a background. Under Material You the tint comes from the wallpaper, which is what
 * makes it feel like part of the device rather than part of an app.
 *
 * Because hue is the user's wallpaper's to choose, state is *also* carried by shape, icon, label
 * and motion — see [StatusIndicator]. Colour alone is never the signal.
 */
@Composable
fun StatusHeader(
    state: FrameworkState,
    version: String?,
    apiVersion: Int?,
    hasUpdate: Boolean,
    onOpenUpdate: () -> Unit,
    ambience: AmbienceKind,
    /** Whether the badge should still be showing that it opens something. See [StatusIndicator]. */
    hintStatus: Boolean,
    onOpenStatus: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenLanguage: () -> Unit,
    onBrandTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    val container by
        animateColorAsState(
            when (state) {
                FrameworkState.Active -> colors.primaryContainer
                FrameworkState.Degraded -> colors.tertiaryContainer
                FrameworkState.Inactive -> colors.errorContainer
                FrameworkState.Mismatched -> colors.errorContainer
                FrameworkState.Checking -> colors.surfaceContainer
            },
            animationSpec = tween(420),
            label = "headerContainer",
        )
    val onContainer by
        animateColorAsState(
            when (state) {
                FrameworkState.Active -> colors.onPrimaryContainer
                FrameworkState.Degraded -> colors.onTertiaryContainer
                FrameworkState.Inactive -> colors.onErrorContainer
                FrameworkState.Mismatched -> colors.onErrorContainer
                FrameworkState.Checking -> colors.onSurfaceVariant
            },
            animationSpec = tween(420),
            label = "headerOnContainer",
        )

    // The brand rides with the state, so the two read as one sentence: *Vector — Active*. The name
    // is set lighter than the state, so the eye still lands on the word that changes.
    val stateWord =
        stringResource(
            when (state) {
                FrameworkState.Active -> R.string.status_active
                FrameworkState.Degraded -> R.string.status_degraded
                FrameworkState.Inactive -> R.string.status_inactive
                FrameworkState.Mismatched -> R.string.status_mismatched
                FrameworkState.Checking -> R.string.status_checking
            }
        )
    val brand = stringResource(R.string.app_name)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                // Square at the top so it meets the screen edge, rounded at the bottom so it
                // reads as one pane hanging from it.
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(
                    // A shallow wash rather than a flat fill: enough depth that the pane has a
                    // top and a bottom, far short of a decorative gradient.
                    Brush.verticalGradient(
                        listOf(container, container.copy(alpha = 0.82f).compositeOverSurface())
                    )
                )
    ) {
        // matchParentSize, NOT fillMaxSize: a Box child that fills its maximum constraint drags
        // the Box to full height with it, and the header would swallow the whole screen.
        // matchParentSize sizes to whatever the *content* settled on without influencing it.
        AmbientSurface(
            kind = ambience,
            tint = onContainer,
            modifier = Modifier.matchParentSize(),
        )

        Column(
            modifier =
                Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 20.dp)
        ) {
            // The ambient surface gets the upper part of the pane to itself; the status settles at
            // the bottom, where it sits on the surface rather than floating above a gap.
            Spacer(Modifier.height(66.dp))

            Row(verticalAlignment = Alignment.Top) {
                // The indicator is the details button. It is the thing the user is already
                // looking at when they wonder *why* it says what it says, so it should be the
                // thing that answers — a separate chevron was a second control for one intent.
                //
                // Centred on the *headline* rather than on the whole block: against the block it
                // lands level with the gap between "Vector Active" and the version line and reads
                // as belonging to neither, while against the headline it sits square with the word
                // it is the state of.
                Box(
                    modifier = Modifier.height(HEADLINE_ROW),
                    contentAlignment = Alignment.Center,
                ) {
                    StatusIndicator(
                        state = state,
                        tint = onContainer,
                        hint = hintStatus,
                        onClick = onOpenStatus,
                        contentDescription = stringResource(R.string.status_open_details),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    // The buttons live *inside* the headline row rather than beside the whole
                    // block, so the stack and the state word share a centre line by construction
                    // and the eye reads one row rather than three loose objects.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            // The wordmark is its own target, because something is hidden behind
                            // it.
                            Text(
                                text = brand,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Normal,
                                color = onContainer.copy(alpha = 0.62f),
                                modifier =
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onBrandTap,
                                    ),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stateWord,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = onContainer,
                            )
                        }
                        // Neither is a gear. What they open governs how the app *presents*
                        // itself — its colours and its language — rather than what it does, and
                        // the icons should say so. Stacked because they belong together.
                        // Deliberately tighter than a default icon button: the pair sets the
                        // height of the row it shares with the wordmark, and at the standard 48 dp
                        // each it would push the version line a finger's width from the name it
                        // belongs to.
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onOpenAppearance,
                                modifier = Modifier.size(ICON_BUTTON),
                            ) {
                                Icon(
                                    Icons.Rounded.Palette,
                                    contentDescription = stringResource(R.string.appearance_title),
                                    tint = onContainer,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                            IconButton(
                                onClick = onOpenLanguage,
                                modifier = Modifier.size(ICON_BUTTON),
                            ) {
                                Icon(
                                    Icons.Rounded.Language,
                                    contentDescription = stringResource(R.string.language_title),
                                    tint = onContainer,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                    }
                    val detail =
                        buildList {
                                version?.let { add(it) }
                                apiVersion?.let { add("API $it") }
                            }
                            .joinToString("  ·  ")
                    if (detail.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        // The version line becomes the way in to the update, because it is the
                        // thing the mark is attached to: a reader who has noticed that their
                        // version is marked has already looked at exactly the right words.
                        UpdatableVersion(
                            text = detail,
                            hasUpdate = hasUpdate,
                            color = onContainer.copy(alpha = 0.75f),
                            markColor = onContainer,
                            // Tappable whether or not there is an update. Checking on demand is
                            // a thing people do, and a control that only exists once there is news
                            // cannot be found before there is any — so the answer "you are up to
                            // date" would have been the one answer unreachable from here.
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onOpenUpdate,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The indicator. Its corner radius animates between three values — a rounded square when active, a
 * softer form when degraded, a circle when inactive — so a state change is legible as motion
 * rather than as a colour swap alone.
 *
 * When active it breathes: a slow, low-amplitude pulse that reads as "running" at a glance, and
 * stops dead in the other two states, so stillness itself carries meaning.
 *
 * It is also the door to the System status page — the settings for how to open Vector are behind it
 * and nothing else leads there — and a tick does not look like a door. So while [hint] is set the
 * tick turns into a gear for ten seconds every thirty, which is the one symbol everybody already
 * reads as "there are settings here", and turns back. See #856.
 *
 * Only the tick. The other three states are being *reported*, urgently in two of them, and a badge
 * that wanders off into a gear while it is saying the framework is not running would be trading the
 * message for the hint.
 */
@Composable
private fun StatusIndicator(
    state: FrameworkState,
    tint: Color,
    hint: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val corner by
        animateFloatAsState(
            when (state) {
                FrameworkState.Active -> 34f
                FrameworkState.Degraded -> 42f
                else -> 50f
            },
            animationSpec = tween(420),
            label = "indicatorCorner",
        )

    val breathing = rememberInfiniteTransition(label = "indicatorBreath")
    val pulse by
        breathing.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
            label = "indicatorPulse",
        )

    val icon =
        when (state) {
            FrameworkState.Active -> Icons.Rounded.Check
            FrameworkState.Degraded -> Icons.Rounded.PriorityHigh
            FrameworkState.Inactive -> Icons.Rounded.Close
            // Not a Close: the framework is there. It is this manager that cannot reach it.
            FrameworkState.Mismatched -> Icons.Rounded.PriorityHigh
            FrameworkState.Checking -> null
        }

    val hinting = hint && state == FrameworkState.Active
    var asGear by remember { mutableStateOf(false) }
    // An Animatable rather than a target the composition sets, because a turn has to be able to
    // *stop where it is*: the wheel that has just spun a full turn and drawn a still one next must
    // hold that angle, and an animation driven from a remembered target would spring back to it.
    val spin = remember { Animatable(0f) }

    LaunchedEffect(hinting) {
        // Cancelled and restarted whenever the framework leaves or re-enters Active, which is also
        // what puts the badge back to a tick mid-hint rather than leaving a gear over a red header.
        if (!hinting) {
            asGear = false
            return@LaunchedEffect
        }
        while (true) {
            delay(HINT_PERIOD_MS)
            asGear = true
            repeat(HINT_TURNS) {
                // A coin per turn rather than a steady spin. A gear that simply rotates for ten
                // seconds is decoration and the eye files it away as such by the second cycle; one
                // that turns, stops, thinks and turns again reads as something being *operated*,
                // and it is the stopping that makes the next turn worth looking at.
                if (Random.nextBoolean()) {
                    spin.animateTo(
                        spin.value + FULL_TURN,
                        animationSpec = tween(HINT_TURN_MS, easing = LinearEasing),
                    )
                } else {
                    delay(HINT_TURN_MS.toLong())
                }
            }
            asGear = false
            // Wound back into a single turn between hints, so an app left open for an afternoon
            // does not accumulate an angle large enough to lose its own fraction.
            spin.snapTo(spin.value.mod(FULL_TURN))
        }
    }

    // One number for the whole tick-to-gear swap: what fades, what shrinks, what turns into what.
    // Timed like the header's colour and corner transitions, since it is the same badge changing.
    val morph by
        animateFloatAsState(if (asGear) 1f else 0f, tween(MORPH_MS), label = "indicatorMorph")

    Box(
        modifier =
            Modifier.size(52.dp)
                .scale(if (state == FrameworkState.Active) pulse else 1f)
                .clip(RoundedCornerShape(percent = corner.toInt()))
                // Lifted while the gear is out. The badge is asking to be pressed at that moment,
                // and a fill a shade stronger is how every other control on the screen says so.
                .background(tint.copy(alpha = lerp(RESTING_FILL, HINTING_FILL, morph)))
                .clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        // Both glyphs are laid out; `morph` decides which is visible. The transforms live in a
        // `graphicsLayer` block, which re-runs in the draw phase when the state it reads changes,
        // so the spin never invalidates the composition — which matters most for `spin`, whose
        // value moves on every frame of a turn.
        if (icon != null) {
            // The label beside it already names the state, and the box carries the description,
            // so the glyph must not be announced a third time.
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier =
                    Modifier.size(26.dp).graphicsLayer {
                        alpha = 1f - morph
                        // Away rather than out: the tick shrinks and twists as the gear arrives
                        // over it, so the two read as one object changing rather than two swapped.
                        // The twist is the departing tick's alone, deliberately — a turn of the
                        // *gear* means a coin came up heads, and nothing else may spend one.
                        val leaving = lerp(1f, 0.6f, morph)
                        scaleX = leaving
                        scaleY = leaving
                        rotationZ = -MORPH_TURN * morph
                    },
            )
        }
        if (state == FrameworkState.Active) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = null,
                tint = tint,
                modifier =
                    Modifier.size(26.dp).graphicsLayer {
                        alpha = morph
                        val arriving = lerp(0.6f, 1f, morph)
                        scaleX = arriving
                        scaleY = arriving
                        // `spin` and nothing else. The wheel arrives and leaves at exactly the
                        // angle it is resting at, so every degree it ever turns through was asked
                        // for by a coin — which is the whole point of tossing one. It used to pick
                        // up the tick's twist on the way in and give it back on the way out, and
                        // that made the gear turn on every hint whatever the coins said.
                        rotationZ = spin.value
                    },
            )
        }
    }
}

/** Keeps the gradient's lower stop opaque; a translucent stop would show the list scrolling under. */
@Composable
private fun Color.compositeOverSurface(): Color {
    val surface = MaterialTheme.colorScheme.surface
    return Color(
        red = red * alpha + surface.red * (1 - alpha),
        green = green * alpha + surface.green * (1 - alpha),
        blue = blue * alpha + surface.blue * (1 - alpha),
        alpha = 1f,
    )
}

/** One of the two stacked buttons beside the wordmark. */
private val ICON_BUTTON = 38.dp

// --- the badge's gear hint ------------------------------------------------------------------
// Long enough apart that the header is a still object most of the time — this sits above whatever
// the reader came to Home to read — and long enough at a time to be noticed by someone who was
// looking elsewhere when it began.

/** How long the badge rests as a tick between hints. */
private const val HINT_PERIOD_MS = 30_000L

/** Each hint is [HINT_TURNS] of these, so ten seconds as a gear. */
private const val HINT_TURN_MS = 2_000

private const val HINT_TURNS = 5

private const val FULL_TURN = 360f

/** The tick-to-gear cross-dissolve, timed like the header's colour and corner transitions. */
private const val MORPH_MS = 420

/**
 * How far the *tick* turns as it hands over, in degrees. Enough to read as a twist.
 *
 * Not applied to the gear. See the two `graphicsLayer` blocks: the gear's only source of rotation
 * is the coin, so a hint whose five tosses all come up tails shows a wheel that never moves.
 */
private const val MORPH_TURN = 60f

/** The badge's fill against the header, at rest and while it is asking to be pressed. */
private const val RESTING_FILL = 0.15f

private const val HINTING_FILL = 0.24f

/**
 * The height of the row the wordmark shares with those buttons.
 *
 * Derived rather than guessed, because the status indicator is centred against it: the stack is
 * what makes that row taller than its text, so if the buttons change size the indicator has to
 * follow or it stops lining up with the word it belongs to.
 */
private val HEADLINE_ROW = ICON_BUTTON * 2
