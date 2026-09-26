package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WudianCommunityApp(viewModel)
            }
        }
    }
}

data class NavigationTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun WudianCommunityApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val snackBarMsg by viewModel.snackBarMessage.collectAsState()

    val showCreatePost by viewModel.showCreatePostDialog.collectAsState()
    val showCreateShare by viewModel.showShareCreateDialog.collectAsState()
    val showExtractShare by viewModel.showShareExtractDialog.collectAsState()
    val showFeedback by viewModel.showFeedbackDialog.collectAsState()
    val showLogin by viewModel.showLoginDialog.collectAsState()
    val activeAnnouncement by viewModel.activeAnnouncement.collectAsState()
    val announcementRedPacket by viewModel.announcementRedPacket.collectAsState()
    val redPacketLoading by viewModel.redPacketLoading.collectAsState()
    val redPacketGrabbing by viewModel.redPacketGrabbing.collectAsState()

    // 首页列表滚动位置保存在此（避免进入子页后返回时回到顶部）
    val homeListState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackBarMsg) {
        snackBarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // Handle back button on sub-screens
    if (currentDestination != ScreenDestination.MainTabs) {
        BackHandler {
            viewModel.navigateBack()
        }
    }

    // 未登录也可以直接浏览帖子；需要登录的操作会主动弹出登录框

    val navTabs = listOf(
        NavigationTabItem("首页", Icons.Filled.Home, Icons.Outlined.Home, "tab_nav_home"),
        NavigationTabItem("发现", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_nav_discover"),
        NavigationTabItem("云盘", Icons.Filled.Cloud, Icons.Outlined.Cloud, "tab_nav_cloud"),
        NavigationTabItem("搜索", Icons.Filled.Search, Icons.Outlined.Search, "tab_nav_search"),
        NavigationTabItem("我的", Icons.Filled.Person, Icons.Outlined.Person, "tab_nav_profile")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentDestination == ScreenDestination.MainTabs) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    navTabs.forEachIndexed { index, item ->
                        val isSelected = selectedTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(index) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(item.tag)
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentDestination == ScreenDestination.MainTabs) innerPadding else PaddingValues(0.dp))
        ) {
            when (val dest = currentDestination) {
                ScreenDestination.MainTabs -> {
                    when (selectedTab) {
                        0 -> HomeScreen(viewModel = viewModel, listState = homeListState)
                        1 -> DiscoverScreen(viewModel = viewModel)
                        2 -> CloudDriveScreen(viewModel = viewModel)
                        3 -> SearchScreen(viewModel = viewModel)
                        4 -> ProfileScreen(viewModel = viewModel)
                    }
                }
                is ScreenDestination.PostDetail -> {
                    PostDetailScreen(viewModel = viewModel)
                }
                is ScreenDestination.MiniAppSandbox -> {
                    MiniAppSandboxScreen(
                        miniAppName = dest.miniAppName,
                        viewModel = viewModel
                    )
                }
                is ScreenDestination.GroupChat -> {
                    GroupChatScreen(
                        groupId = dest.groupId,
                        groupName = dest.groupName,
                        viewModel = viewModel
                    )
                }
                is ScreenDestination.VideoPlayer -> {
                    VideoPlayerScreen(
                        url = dest.videoUrl,
                        title = dest.videoTitle,
                        viewModel = viewModel
                    )
                }
                is ScreenDestination.ImageViewer -> {
                    ImageViewerScreen(
                        images = dest.images,
                        initialIndex = dest.initialIndex,
                        viewModel = viewModel
                    )
                }
                is ScreenDestination.WebPage -> {
                    WebPageScreen(
                        url = dest.url,
                        title = dest.title,
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Dialogs
    if (showCreatePost) {
        CreatePostDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showCreatePostDialog.value = false }
        )
    }

    if (showCreateShare) {
        ShareCreateDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showShareCreateDialog.value = false }
        )
    }

    if (showExtractShare) {
        ShareExtractDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showShareExtractDialog.value = false }
        )
    }

    if (showFeedback) {
        FeedbackDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showFeedbackDialog.value = false }
        )
    }

    if (showLogin) {
        LoginDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.showLoginDialog.value = false }
        )
    }

    activeAnnouncement?.let { announcement ->
        AnnouncementDialog(
            announcement = announcement,
            packet = announcementRedPacket,
            packetLoading = redPacketLoading,
            packetGrabbing = redPacketGrabbing,
            onGrab = { packet -> viewModel.grabRedPacket(packet) },
            onOpenLinkedPost = { postId ->
                viewModel.dismissAnnouncement()
                viewModel.navigateTo(ScreenDestination.PostDetail(postId))
            },
            onDismiss = { viewModel.dismissAnnouncement() }
        )
    }
}
