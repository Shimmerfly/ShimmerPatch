package top.nkbe.npatch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import top.nkbe.npatch.config.DEFAULT_CUSTOM_COLOR

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LSPTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    useMonet: Boolean = false,
    customColor: Int = DEFAULT_CUSTOM_COLOR,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val dynamic = useMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var scheme = when {
        dynamic && isDarkTheme -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> remember(customColor, isDarkTheme) { SeedScheme.of(customColor, isDarkTheme) }
    }
    if (isDarkTheme && amoledBlack) scheme = scheme.toAmoled()

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
