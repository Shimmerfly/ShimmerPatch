// Ported from WeKit ui/navigation/M3NavEffects.kt; see docs/UI_SOURCES.md.
package top.nkbe.npatch.ui.page

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import top.nkbe.npatch.ui.util.rememberDeviceCornerRadius
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects

@Composable
fun rememberM3NavEffects(): NavDisplayEffects {
    val cornerRadius = rememberDeviceCornerRadius(defaultRadius = 32.dp)
    val backdropColor = MaterialTheme.colorScheme.surfaceContainer
    return remember(cornerRadius, backdropColor) {
        NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = cornerRadius,
            cornerClipMode = NavCornerClipMode.Leading,
            dimAmount = 0.5f,
            backdropColor = backdropColor,
            blockInputDuringTransition = false,
        )
    }
}
