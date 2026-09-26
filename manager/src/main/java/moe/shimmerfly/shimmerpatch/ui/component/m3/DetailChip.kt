package moe.shimmerfly.shimmerpatch.ui.component.m3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import moe.shimmerfly.shimmerpatch.util.NeoPackageManager.PatchedType

/**
 * The tonal pair a patcher's chip is painted with.
 *
 * Both tones come from the expressive scheme MaterialKolor derives from the seed, so a chip follows
 * the wallpaper like the rest of the manager. Our own bundles take the primary container pair and
 * the patchers we can also read take the secondary one below it, so a row says whose patch it is
 * before its label is read. A type we cannot name stays neutral.
 */
@Composable
fun patcherTone(type: PatchedType?): Pair<Color, Color> = when (type) {
    PatchedType.SHIMMERPATCH ->
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    PatchedType.NPATCH, PatchedType.LSPATCH, PatchedType.FPA ->
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    else ->
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
}

/** The neutral pair for a chip that says something other than who patched the bundle. */
@Composable
fun neutralTone(): Pair<Color, Color> =
    MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant

/** A short label chip: a leading icon and a label, in the rounded language of the rows around it. */
@Composable
fun DetailChip(
    icon: ImageVector,
    text: String,
    container: Color,
    content: Color,
) {
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                // A chip is one unit: it moves to the next line rather than breaking apart.
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}
