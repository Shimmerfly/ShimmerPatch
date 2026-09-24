// MaterialExpressiveTheme and seed generation follow WeKit's ModuleAppTheme / SeedResolver.
package moe.shimmerfly.shimmerpatch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.dynamicColorScheme
import moe.shimmerfly.shimmerpatch.config.DEFAULT_CUSTOM_COLOR

@Composable
fun LSPTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    useMonet: Boolean = false,
    customColor: Int = DEFAULT_CUSTOM_COLOR,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        useMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> remember(customColor, isDarkTheme) {
            dynamicColorScheme(seedColor = Color(customColor), isDark = isDarkTheme)
        }
    }
    MaterialExpressiveTheme(colorScheme = colors, motionScheme = MotionScheme.expressive(), content = content)
}
