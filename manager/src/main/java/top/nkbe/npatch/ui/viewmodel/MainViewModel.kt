package top.nkbe.npatch.ui.viewmodel

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import top.nkbe.npatch.config.ThemeConfig
import top.nkbe.npatch.config.ThemeSettings
import top.nkbe.npatch.ui.page.Navigator

/** Activity lifetime owns loaded theme and pending navigation results. */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    var hasBoundNavigation = false
    val navigator = Navigator(mutableListOf())
    val snackbarHostState = SnackbarHostState()
    val theme: StateFlow<ThemeSettings?> = ThemeConfig.getThemeFlow(application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
