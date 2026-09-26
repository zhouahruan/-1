package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.PostCard
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val keyword by viewModel.searchKeyword.collectAsState()
    val postResults by viewModel.searchPostsResult.collectAsState()
    val userResults by viewModel.searchUsersResult.collectAsState()

    var searchTab by remember { mutableStateOf(0) } // 0: Posts, 1: Users

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全局检索", fontWeight = FontWeight.Bold) },
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
            // Search Input
            OutlinedTextField(
                value = keyword,
                onValueChange = { viewModel.onSearchKeywordChanged(it) },
                placeholder = { Text("搜索帖子内容、标题或社区用户...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "搜索", tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (keyword.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.onSearchKeywordChanged("") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Filled.Clear, contentDescription = "清空")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_input_field")
            )

            PrimaryTabRow(
                selectedTabIndex = searchTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = searchTab == 0,
                    onClick = { searchTab = 0 },
                    text = { Text("帖子 (${postResults.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = searchTab == 1,
                    onClick = { searchTab = 1 },
                    text = { Text("用户 (${userResults.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (keyword.isBlank()) {
                EmptyPlaceholder(
                    title = "输入关键词开始探索",
                    description = "支持模糊搜索帖子标题、正文及社区用户昵称",
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                )
            } else if (searchTab == 0) {
                if (postResults.isEmpty()) {
                    EmptyPlaceholder(
                        title = "未找到相关帖子",
                        description = "请尝试更换其他关键词重新检索",
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "无结果",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(postResults, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                onPostClick = { viewModel.navigateTo(ScreenDestination.PostDetail(it)) },
                                onLikeClick = { if (viewModel.requireLogin("点赞")) viewModel.toggleLike(it) },
                                onFavoriteClick = { if (viewModel.requireLogin("收藏")) viewModel.toggleFavorite(it) },
                                onLaunchMiniApp = { app ->
                                    viewModel.navigateTo(ScreenDestination.MiniAppSandbox(app.id, app.name))
                                },
                                onPlayVideo = { url ->
                                    viewModel.navigateTo(ScreenDestination.VideoPlayer(url))
                                },
                                onImageClick = { list, index ->
                                    viewModel.navigateTo(ScreenDestination.ImageViewer(list, index))
                                }
                            )
                        }
                    }
                }
            } else {
                if (userResults.isEmpty()) {
                    EmptyPlaceholder(
                        title = "未找到相关用户",
                        description = "请尝试更换其他用户昵称或手机尾号",
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "无用户",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(userResults, key = { it.id }) { user ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    UserAvatar(
                                        avatarUrl = user.avatar_url,
                                        name = user.displayName,
                                        size = 46.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            RoleBadge(role = user.role)
                                        }
                                        Text(
                                            text = user.bio ?: "这个用户很神秘，暂无签名",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
