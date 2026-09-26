package moe.shimmerfly.shimmerpatch.ui.page

import top.yukonga.miuix.kmp.nav.core.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation3 路由定义。
 * 包含主页容器 (Main) 和其他全屏页面。
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data class Main(
        val initialTab: Int = MainTab.Home.ordinal,
        val initialManageTab: Int = 0
    ) : Route

    @Serializable data object About : Route

    @Serializable
    data class Welcome(
        val reviewMode: Boolean = false
    ) : Route
    
    @Serializable 
    data class NewPatch(
        val id: Int, 
        val data: String? = null
    ) : Route

    @Serializable 
    data class SelectApps(
        val multiSelect: Boolean, 
        val initialSelected: List<String>? = null
    ) : Route

    /** One patched app: what patched it, what it embeds and what can be done about it. */
    @Serializable
    data class AppDetail(
        val packageName: String
    ) : Route
}
