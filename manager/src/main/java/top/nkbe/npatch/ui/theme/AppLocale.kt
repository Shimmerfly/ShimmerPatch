package top.nkbe.npatch.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import android.text.TextUtils
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

private val LocalLanguageTag = staticCompositionLocalOf { "" }

/** Applies NPatch's in-app locale to Compose without changing the host process locale. */
@Composable
fun LocalizedContent(languageTag: String, content: @Composable () -> Unit) {
    LocalLanguageTagProvider(languageTag, content)
}

/** Re-establishes the locale inside dialog and dropdown windows. */
@Composable
fun LocalizedOverlay(content: @Composable () -> Unit) {
    LocalLanguageTagProvider(LocalLanguageTag.current, content)
}

@Composable
private fun LocalLanguageTagProvider(languageTag: String, content: @Composable () -> Unit) {
    val base = LocalConfiguration.current
    val context = LocalContext.current
    if (languageTag.isBlank()) {
        CompositionLocalProvider(LocalLanguageTag provides "", content = content)
        return
    }
    val localized = remember(languageTag, base) {
        Configuration(base).apply { setLocales(LocaleList.forLanguageTags(languageTag)) }
    }
    val localizedContext = remember(localized) { LocalizedContext(context, localized) }
    val locale = localized.locales.takeIf { it.size() > 0 }?.get(0) ?: Locale.getDefault()
    val direction = if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalLanguageTag provides languageTag,
        LocalConfiguration provides localized,
        LocalContext provides localizedContext,
        LocalLayoutDirection provides direction,
        content = content,
    )
}

private class LocalizedContext(base: Context, config: Configuration) : ContextWrapper(base) {
    private val localizedResources: Resources = base.createConfigurationContext(config).resources
    override fun getResources(): Resources = localizedResources
}
