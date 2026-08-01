package top.nkbe.npatch.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.nkbe.npatch.ui.component.ambience.AmbienceKind
import top.nkbe.npatch.ui.component.ambience.AmbientSurface

/** NPatch adaptation of Vector Manager's full-bleed framework status header. */
@Composable
fun VectorStatusHeader(
    active: Boolean,
    status: String,
    version: String,
    apiVersion: Int,
    onStatusClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLanguageClick: () -> Unit,
    ambience: String = "circuit",
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (active) colors.primaryContainer else colors.tertiaryContainer,
        animationSpec = tween(420),
        label = "statusHeaderContainer",
    )
    val onContainer by animateColorAsState(
        if (active) colors.onPrimaryContainer else colors.onTertiaryContainer,
        animationSpec = tween(420),
        label = "statusHeaderContent",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(container, container.copy(alpha = 0.84f).compositeOverSurface())
                )
            ),
    ) {
        AmbientSurface(
            kind = AmbienceKind.from(ambience),
            tint = onContainer,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 20.dp),
        ) {
            Spacer(Modifier.height(66.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.height(76.dp), contentAlignment = Alignment.Center) {
                    StatusIndicator(
                        active = active,
                        tint = onContainer,
                        description = status,
                        onClick = onStatusClick,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "NPatch",
                                style = MaterialTheme.typography.headlineSmall,
                                color = onContainer.copy(alpha = 0.62f),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = onContainer,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = onAppearanceClick, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Rounded.Palette, contentDescription = null, tint = onContainer, modifier = Modifier.size(21.dp))
                            }
                            IconButton(onClick = onLanguageClick, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Rounded.Language, contentDescription = null, tint = onContainer, modifier = Modifier.size(21.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$version  ·  API $apiVersion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onContainer.copy(alpha = 0.76f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    active: Boolean,
    tint: Color,
    description: String,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "statusBreath")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "statusPulse",
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(if (active) pulse else 1f)
            .clip(RoundedCornerShape(percent = if (active) 34 else 50))
            .background(tint.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (active) Icons.Rounded.Check else Icons.Rounded.Close,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp),
        )
    }
}

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
