package org.lsposed.npatch.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import kotlinx.coroutines.launch
import org.lsposed.npatch.R
import org.lsposed.npatch.ui.component.CenterTopBar
import org.lsposed.npatch.ui.page.destinations.SelectAppsScreenDestination
import org.lsposed.npatch.ui.page.manage.AppManageBody
import org.lsposed.npatch.ui.page.manage.AppManageFab
import org.lsposed.npatch.ui.page.manage.ModuleManageBody

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Destination
@Composable
fun ManageScreen(
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<SelectAppsScreenDestination, SelectAppsResult>
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    val tabTitles = listOf(stringResource(R.string.apps), stringResource(R.string.modules))

    Scaffold(
        topBar = { CenterTopBar(stringResource(BottomBarDestination.Manage.label)) },
        floatingActionButton = {
            if (pagerState.currentPage == 0) AppManageFab(navigator)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage])
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        },
                    )
                }
            }

            HorizontalPager(
                count = tabTitles.size,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> AppManageBody(navigator, resultRecipient)
                    1 -> ModuleManageBody()
                }
            }
        }
    }
}
