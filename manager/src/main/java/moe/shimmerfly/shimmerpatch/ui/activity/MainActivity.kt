package moe.shimmerfly.shimmerpatch.ui.activity

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.shimmerfly.shimmerpatch.LSPApplication
import moe.shimmerfly.shimmerpatch.config.Configs
import moe.shimmerfly.shimmerpatch.config.ThemeConfig
import moe.shimmerfly.shimmerpatch.config.ThemeMode
import moe.shimmerfly.shimmerpatch.ui.page.AboutScreen
import moe.shimmerfly.shimmerpatch.ui.page.LocalNavigator
import moe.shimmerfly.shimmerpatch.ui.page.MainScreen
import moe.shimmerfly.shimmerpatch.ui.page.MainTab
import moe.shimmerfly.shimmerpatch.ui.page.NewPatchScreen
import moe.shimmerfly.shimmerpatch.ui.page.Route
import moe.shimmerfly.shimmerpatch.ui.page.SelectAppsScreen
import moe.shimmerfly.shimmerpatch.ui.page.WelcomeScreen
import moe.shimmerfly.shimmerpatch.ui.page.rememberM3NavEffects
import moe.shimmerfly.shimmerpatch.ui.theme.LSPTheme
import moe.shimmerfly.shimmerpatch.ui.util.LocalBackgroundImagePath
import moe.shimmerfly.shimmerpatch.ui.util.LocalCardBackgroundAlpha
import moe.shimmerfly.shimmerpatch.ui.util.LocalFloatingGlassBottomBar
import moe.shimmerfly.shimmerpatch.ui.util.LocalFloatingGlassBottomBarBlur
import moe.shimmerfly.shimmerpatch.ui.util.LocalSnackbarHost
import moe.shimmerfly.shimmerpatch.ui.util.LocalThemeSettings
import moe.shimmerfly.shimmerpatch.ui.viewmodel.MainViewModel
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = LSPApplication.normalizeLanguageTag(prefs.getString("language", "") ?: "")
        super.attachBaseContext(LSPApplication.applyLocale(newBase, language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var isThemeLoaded = false
        // Match InstallerX: reveal the first complete themed frame from the system splash.
        splashScreen.setKeepOnScreenCondition { !isThemeLoaded }
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { false },
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ) { false }
        )

        setContent {
            val systemIsDark = isSystemInDarkTheme()
            val supportsFloatingGlassBottomBarBlur = ThemeConfig.isFloatingGlassBottomBarBlurSupported()

            val mainViewModel = viewModel<MainViewModel>()
            val loadedTheme by mainViewModel.theme.collectAsState()
            val themeState = loadedTheme ?: return@setContent
            SideEffect { isThemeLoaded = true }
            val isDark = when (themeState.themeMode) {
                ThemeMode.SYSTEM -> systemIsDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    ) { isDark }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            LSPTheme(
                isDarkTheme = isDark,
                useMonet = themeState.useMonet,
                customColor = themeState.customColor
            ) {
                CompositionLocalProvider(
                    LocalThemeSettings provides themeState,
                    LocalBackgroundImagePath provides themeState.backgroundImageUri,
                    LocalCardBackgroundAlpha provides (themeState.cardBackgroundAlphaPercent / 100f),
                    LocalFloatingGlassBottomBar provides themeState.useFloatingGlassBottomBar,
                    LocalFloatingGlassBottomBarBlur provides (
                        themeState.useFloatingGlassBottomBarBlur && supportsFloatingGlassBottomBarBlur
                    ),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val snackbarHostState = mainViewModel.snackbarHostState
                        val startRoute = remember {
                            if (Configs.welcomeSeen) Route.Main() else Route.Welcome()
                        }
                        val backStack = rememberNavBackStack<Route>(startRoute)
                        // A killed process cannot resume native patch work or picker callbacks.
                        // Restore stable destinations; configuration changes retain the live VMs.
                        if (!mainViewModel.hasBoundNavigation) {
                            if (savedInstanceState != null) {
                                backStack.removeAll { it is Route.NewPatch || it is Route.SelectApps }
                                if (backStack.isEmpty()) backStack.add(startRoute)
                            }
                            mainViewModel.hasBoundNavigation = true
                        }
                        val navigator = mainViewModel.navigator
                        navigator.attachBackStack(backStack)
                        val startMainRoute = startRoute as? Route.Main
                        var selectedMainTab by rememberSaveable {
                            mutableIntStateOf(startMainRoute?.initialTab ?: MainTab.Home.ordinal)
                        }
                        var navigationHeight by remember { mutableStateOf(0.dp) }
                        var selectedManageTab by rememberSaveable {
                            mutableIntStateOf(startMainRoute?.initialManageTab ?: 0)
                        }

                        CompositionLocalProvider(
                            LocalSnackbarHost provides snackbarHostState,
                            LocalNavigator provides navigator
                        ) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = {
                                    if (backStack.size > 1) navigator.pop()
                                    else if (selectedMainTab != MainTab.Home.ordinal && backStack.lastOrNull() is Route.Main) {
                                        selectedMainTab = MainTab.Home.ordinal
                                    } else finish()
                                },
                                transition = NavTransitions.MiuixDefault,
                                effects = rememberM3NavEffects(),
                            ) {
                                entry<Route.Main> {
                                    MainScreen(
                                        navigator = navigator,
                                        selectedTab = selectedMainTab,
                                        selectedManageTab = selectedManageTab,
                                        onSelectedTabChange = { selectedMainTab = it },
                                        onSelectedManageTabChange = { selectedManageTab = it },
                                        onNavigationBarHeightChanged = { navigationHeight = it },
                                    )
                                }

                                entry<Route.About>(swipeDismiss = NavSwipeDirection.LeftToRight) {
                                    AboutScreen(onBack = { navigator.pop() })
                                }

                                entry<Route.Welcome>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
                                    WelcomeScreen(
                                        reviewMode = route.reviewMode,
                                        onFinish = {
                                            backStack.clear()
                                            backStack.add(Route.Main())
                                        },
                                        onReturn = { navigator.pop() }
                                    )
                                }

                                entry<Route.NewPatch> { route ->
                                    NewPatchScreen(id = route.id, data = route.data)
                                }

                                entry<Route.SelectApps>(swipeDismiss = NavSwipeDirection.LeftToRight) { route ->
                                    SelectAppsScreen(
                                        multiSelect = route.multiSelect,
                                        initialSelected = route.initialSelected
                                    )
                                }
                            }
                            val snackbarBottom = if (backStack.lastOrNull() is Route.Main) navigationHeight
                                else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            SnackbarHost(
                                hostState = snackbarHostState,
                                modifier = Modifier.align(Alignment.BottomCenter).imePadding()
                                    .padding(start = 16.dp, end = 16.dp, bottom = snackbarBottom),
                            )
                            BackHandler(enabled = backStack.size == 1 && backStack.lastOrNull() is Route.Main && selectedMainTab != MainTab.Home.ordinal) {
                                selectedMainTab = MainTab.Home.ordinal
                            }
                        }
                    }
                }
            }
        }
    }
}
