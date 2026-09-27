package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.GroupItem
import com.example.ui.components.MiniAppCard
import com.example.ui.theme.Md3EmptyState
import com.example.ui.theme.Md3Icons
import com.example.ui.theme.Md3InfoBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val miniApps by viewModel.miniApps.collectAsState()
    val miniAppsError by viewModel.miniAppsError.collectAsState()
    val groups by viewModel.groups.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0: MiniApps, 1: Groups

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("探索与发现", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDiscoverData() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("discover_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryTabRow(
                selectedTabIndex = activeSubTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("轻应用市场 (${miniApps.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_miniapps")
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("社区群组 (${groups.size})", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_groups")
                )
            }

            if (activeSubTab == 0) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        // MD3 扩展图形库：用扩展图标替代 emoji 提示
                        Md3InfoBanner(
                            icon = Md3Icons.Status.tip,
                            text = "轻应用基于原生 Android WebView 安全沙箱运行，无需安装，点击即刻运行，即开即用。",
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (miniAppsError != null) {
                        item { Text("轻应用加载失败：$miniAppsError", color = MaterialTheme.colorScheme.error) }
                    } else if (miniApps.isEmpty()) {
                        item {
                            Md3EmptyState(
                                icon = Md3Icons.Status.empty,
                                title = "暂无已上架的小程序",
                                description = "轻应用市场上还没有内容，稍后再来看看吧。"
                            )
                        }
                    }

                    items(miniApps, key = { it.id }) { app ->
                        MiniAppCard(
                            miniApp = app,
                            onLaunch = {
                                viewModel.navigateTo(ScreenDestination.MiniAppSandbox(app.id, app.name))
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Md3InfoBanner(
                            icon = Md3Icons.Status.discussion,
                            text = "社区即时通讯频道，与各领域的同行和同好畅聊技术动态、架构方案与设计经验。",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    if (groups.isEmpty()) {
                        item {
                            Md3EmptyState(
                                icon = Md3Icons.Status.group,
                                title = "暂无社区群组",
                                description = "群组会陆续开放，敬请期待。"
                            )
                        }
                    }

                    items(groups, key = { it.id }) { group ->
                        GroupItem(
                            group = group,
                            onEnterChat = {
                                viewModel.navigateTo(ScreenDestination.GroupChat(group.id, group.name))
                            }
                        )
                    }
                }
            }
        }
    }
}
