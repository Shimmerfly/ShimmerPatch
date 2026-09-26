// SPDX-License-Identifier: GPL-3.0-only
// Grouped About page adapted from InstallerX-Revived AboutPage.kt and WeKit M3 widgets.
// Copyright (C) 2025-2026 InstallerX Revived contributors. See docs/UI_SOURCES.md.
package moe.shimmerfly.shimmerpatch.ui.page

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import moe.shimmerfly.shimmerpatch.R
import moe.shimmerfly.shimmerpatch.share.LSPConfig
import moe.shimmerfly.shimmerpatch.ui.component.*
import moe.shimmerfly.shimmerpatch.ui.component.m3.BaseWidget
import moe.shimmerfly.shimmerpatch.ui.component.m3.SegmentedColumn
import moe.shimmerfly.shimmerpatch.ui.util.*

private data class AboutLink(
    val title: String,
    val summary: String,
    val url: String,
    val icon: ImageVector? = null,
    val imageUrl: String? = null,
    val imageRes: Int? = null,
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val backdrop = rememberMaterial3BlurBackdrop(LocalFloatingGlassBottomBarBlur.current)
    // The avatars are remote. Asking for them during composition would put the network stack on
    // the frame that opens the page; handing them over one frame later keeps the arrival smooth.
    var avatarsReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { avatarsReady = true }
    val links = rememberAboutLinks()
    val acknowledgments = rememberAcknowledgmentLinks(avatarsReady)
    ShimmerPatchScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ShimmerPatchTopAppBar(
                title = stringResource(R.string.home_about),
                scrollBehavior = scrollBehavior,
                navigationIcon = { ExpressiveBackButton(onClick = onBack) },
                modifier = Modifier.m3AppBarBlur(backdrop),
                color = backdrop.m3AppBarColor(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().m3BackdropLayer(backdrop),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection),
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Through Coil so the 512px artwork is decoded off the main thread.
                    AsyncImage(
                        model = R.drawable.ic_launcher_artwork,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.extraLarge),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text("${LSPConfig.instance.VERSION_NAME} (${LSPConfig.instance.VERSION_CODE})", style = MaterialTheme.typography.labelLarge)
                    Text(stringResource(R.string.home_description), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item {
                SegmentedColumn {
                    item {
                        AboutLinkItem(
                            AboutLink(
                                title = "NkBe",
                                summary = stringResource(R.string.about_author_summary),
                                url = AUTHOR_GITHUB_URL,
                                imageUrl = if (avatarsReady) AUTHOR_AVATAR_URL else null,
                            ),
                            context::openUri,
                        )
                    }
                }
            }
            item {
                SegmentedColumn(title = stringResource(R.string.about_disclaimer_title)) {
                    item {
                        BaseWidget(
                            title = stringResource(R.string.about_disclaimer_title),
                            description = stringResource(R.string.about_disclaimer_body),
                            icon = Icons.Outlined.Security,
                        )
                    }
                }
            }
            item {
                SegmentedColumn(title = stringResource(R.string.about_links_title)) {
                    links.forEach { link -> item(key = link.title) { AboutLinkItem(link, context::openUri) } }
                }
            }
            item {
                SegmentedColumn(title = stringResource(R.string.about_acknowledgments_title)) {
                    acknowledgments.forEach { link -> item(key = link.title) { AboutLinkItem(link, context::openUri) } }
                }
            }
        }
    }
}

@Composable
private fun AboutLinkItem(link: AboutLink, onLinkClick: (String) -> Unit) {
    BaseWidget(
        title = link.title,
        description = link.summary,
        icon = link.icon,
        onClick = { onLinkClick(link.url) },
        trailingContent = {
            when {
                link.imageUrl != null -> AsyncImage(link.imageUrl, null, Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                link.imageRes != null -> AsyncImage(link.imageRes, null, Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            }
        },
    )
}

@Composable
private fun rememberAboutLinks(): List<AboutLink> {
    val websiteTitle = stringResource(R.string.about_official_website)
    val websiteSummary = stringResource(R.string.about_link_website_summary)
    val githubSummary = stringResource(R.string.about_link_github_summary)
    val telegramSummary = stringResource(R.string.about_link_telegram_summary)

    return remember(websiteTitle, websiteSummary, githubSummary, telegramSummary) {
        listOf(
            AboutLink(
                title = websiteTitle,
                summary = websiteSummary,
                url = ABOUT_WEBSITE_URL,
                icon = Icons.Outlined.Public
            ),
            AboutLink(
                title = "GitHub",
                summary = githubSummary,
                url = GITHUB_URL,
                icon = Icons.Outlined.Code
            ),
            AboutLink(
                title = "Telegram",
                summary = telegramSummary,
                url = TELEGRAM_URL,
                icon = Icons.AutoMirrored.Outlined.Send
            )
        )
    }
}

@Composable
private fun rememberAcknowledgmentLinks(avatarsReady: Boolean): List<AboutLink> {
    val rovo89 = stringResource(R.string.about_ack_rovo89_summary)
    val lsposed = stringResource(R.string.about_ack_lsposed_team_summary)
    val jingMatrix = stringResource(R.string.about_ack_jingmatrix_summary)
    val lspatch = stringResource(R.string.about_ack_lspatch_summary)
    val libxposed = stringResource(R.string.about_ack_libxposed_summary)
    val winter = stringResource(R.string.about_ack_winter_summary)
    val m558 = stringResource(R.string.about_ack_m558_summary)
    val community = stringResource(R.string.about_ack_community_summary)

    return remember(avatarsReady, rovo89, jingMatrix, lsposed, lspatch, libxposed, winter, m558, community) {
        // An avatar URL only reaches the list once the page has arrived.
        fun avatar(url: String) = if (avatarsReady) url else null
        listOf(
            AboutLink(
                title = "rovo89",
                summary = rovo89,
                url = "https://github.com/rovo89/XposedBridge",
                imageUrl = avatar(ROVO89_AVATAR_URL)
            ),
            AboutLink(
                title = "JingMatrix",
                summary = jingMatrix,
                url = "https://github.com/JingMatrix/Vector",
                imageUrl = avatar(JING_MATRIX_AVATAR_URL)
            ),
            AboutLink(
                title = "LSPosed",
                summary = lsposed,
                url = "https://github.com/LSPosed/LSPosed",
                imageUrl = avatar(LSPOSED_TEAM_AVATAR_URL)
            ),
            AboutLink(
                title = "LSPatch",
                summary = lspatch,
                url = "https://github.com/LSPosed/LSPatch",
                imageUrl = avatar(LSPATCH_AVATAR_URL)
            ),
            AboutLink(
                title = "libxposed",
                summary = libxposed,
                url = "https://github.com/libxposed/api",
                imageUrl = avatar(LIBXPOSED_AVATAR_URL)
            ),
            AboutLink(
                title = "winter",
                summary = winter,
                url = TELEGRAM_URL,
                imageRes = R.drawable.winter
            ),
            AboutLink(
                title = "M558",
                summary = m558,
                url = TELEGRAM_URL,
                imageRes = R.drawable.m558
            ),
            AboutLink(
                title = "Community",
                summary = community,
                url = GITHUB_URL,
                icon = Icons.Outlined.Favorite
            )
        )
    }
}

private fun Context.openUri(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
}

private const val ABOUT_WEBSITE_URL = "https://www.nkbe.top"
private const val GITHUB_URL = "https://github.com/7723mod/NPatch"
private const val TELEGRAM_URL = "https://t.me/NPatch"
private const val AUTHOR_GITHUB_URL = "https://github.com/HSSkyBoy"
private const val AUTHOR_AVATAR_URL = "https://avatars.githubusercontent.com/u/122550437?s=256"
private const val ROVO89_AVATAR_URL = "https://avatars.githubusercontent.com/u/1573299?s=256"
private const val JING_MATRIX_AVATAR_URL = "https://avatars.githubusercontent.com/u/24476093?s=256"
private const val LSPOSED_TEAM_AVATAR_URL = "https://avatars.githubusercontent.com/u/75879071?s=256&v=4"
private const val LSPATCH_AVATAR_URL =
    "https://raw.githubusercontent.com/LSPosed/LSPatch/refs/heads/master/manager/src/main/ic_launcher-playstore.png"
private const val LIBXPOSED_AVATAR_URL = "https://avatars.githubusercontent.com/u/85155136?s=128"
