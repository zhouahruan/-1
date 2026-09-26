package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.AnnouncementBanner
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val isLoading by viewModel.isLoadingFeed.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "雾点社区",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "探索技术前沿 • 共享微服务与沙箱",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.selectTab(3) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadFeed() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("home_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (viewModel.requireLogin("发帖")) viewModel.showCreatePostDialog.value = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "发帖") },
                text = { Text("发新帖", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("create_post_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Announcement banner
            if (announcements.isNotEmpty()) {
                item {
                    AnnouncementBanner(
                        announcements = announcements,
                        onAnnouncementClick = { ann ->
                            // 弹窗展示公告全文（可能带红包）
                            viewModel.showAnnouncement(ann)
                        }
                    )
                }
            }

            // 分类筛选（来自帖子真实标签）
            if (tags.size > 1) {
                item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        val isSelected = tag == selectedTag
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.filterByTag(tag) },
                            label = { Text(tag, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("tag_chip_$tag")
                        )
                    }
                }
                }
            }

            // Loading indicator or Post items
            if (isLoading && posts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (posts.isEmpty()) {
                item {
                    EmptyPlaceholder(
                        title = "暂无相关帖子",
                        description = "该分类下暂无内容，点击右下角按钮发布第一篇帖子吧！",
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = "空",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    )
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        onPostClick = { postId ->
                            viewModel.navigateTo(ScreenDestination.PostDetail(postId))
                        },
                        onLikeClick = { postId ->
                            if (viewModel.requireLogin("点赞")) viewModel.toggleLike(postId)
                        },
                        onFavoriteClick = { postId ->
                            if (viewModel.requireLogin("收藏")) viewModel.toggleFavorite(postId)
                        },
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

            item {
                Spacer(modifier = Modifier.height(72.dp)) // Space for FAB
            }
        }
    }
}
