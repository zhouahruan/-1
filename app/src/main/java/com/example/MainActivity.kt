package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.zhshop.ui.ZhShopViewModel
import com.example.zhshop.ui.components.*
import com.example.zhshop.ui.screens.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ZhShopApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZhShopApp(
    viewModel: ZhShopViewModel = viewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val pulseData by viewModel.pulseData.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val filteredApps by viewModel.filteredApps.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadStatus by viewModel.downloadStatus.collectAsStateWithLifecycle()
    val downloadedApkFiles by viewModel.downloadedApkFiles.collectAsStateWithLifecycle()
    val leaderboardType by viewModel.leaderboardType.collectAsStateWithLifecycle()
    val leaderboardItems by viewModel.leaderboardItems.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val developerApps by viewModel.developerApps.collectAsStateWithLifecycle()
    val pendingVersions by viewModel.pendingVersions.collectAsStateWithLifecycle()
    val rejectedApps by viewModel.rejectedApps.collectAsStateWithLifecycle()

    val forumBoards by viewModel.forumBoards.collectAsStateWithLifecycle()
    val forumPosts by viewModel.forumPosts.collectAsStateWithLifecycle()
    val postRepliesMap by viewModel.postRepliesMap.collectAsStateWithLifecycle()
    val socialFeed by viewModel.socialFeed.collectAsStateWithLifecycle()
    val userBadges by viewModel.userBadges.collectAsStateWithLifecycle()

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val messagesMap by viewModel.messagesMap.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    val liuYunInfo by viewModel.liuYunInfo.collectAsStateWithLifecycle()
    val clientUpdateInfo by viewModel.clientUpdateInfo.collectAsStateWithLifecycle()
    val apkResult by viewModel.apkResult.collectAsStateWithLifecycle()
    val isAnalyzingApk by viewModel.isAnalyzingApk.collectAsStateWithLifecycle()

    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val selectedPost by viewModel.selectedPost.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()

    val showDevWorkbench by viewModel.showDeveloperWorkbench.collectAsStateWithLifecycle()
    val showCreatePost by viewModel.showCreatePostDialog.collectAsStateWithLifecycle()
    val showSponsor by viewModel.showSponsorDialog.collectAsStateWithLifecycle()
    val showApkParser by viewModel.showApkParserDialog.collectAsStateWithLifecycle()
    val showLiuYun by viewModel.showLiuYunDialog.collectAsStateWithLifecycle()
    val showClientUpdate by viewModel.showClientUpdateDialog.collectAsStateWithLifecycle()
    val showEditProfile by viewModel.showEditProfileDialog.collectAsStateWithLifecycle()
    val showAgreementViewer by viewModel.showAgreementViewerDialog.collectAsStateWithLifecycle()
    val showAuth by viewModel.showAuthDialog.collectAsStateWithLifecycle()
    val hasAgreedAgreement by viewModel.hasAgreedAgreement.collectAsStateWithLifecycle()

    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearToast()
            }
        }
    }

    // 核心安全防护门禁：未同意协议前，绝不渲染主商店界面，从源头杜绝弹窗被绕过
    if (!hasAgreedAgreement) {
        AgreementGateScreen(
            onAgree = { viewModel.agreeToAgreement() }
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(
                                text = "ZHShop",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "手机版",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Text(
                            text = when (selectedTab) {
                                0 -> "发现好应用"
                                1 -> "分类与排行榜"
                                2 -> "社区动态交流"
                                3 -> "私信与通知"
                                else -> "个人与设置"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showApkParserDialog.value = true },
                        modifier = Modifier.testTag("top_apk_parser_btn")
                    ) {
                        Icon(Icons.Rounded.Android, contentDescription = "APK解析")
                    }
                    IconButton(
                        onClick = { viewModel.showClientUpdateDialog.value = true },
                        modifier = Modifier.testTag("top_client_update_btn")
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = "检查更新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    icon = { Icon(Icons.Rounded.Storefront, contentDescription = "商店") },
                    label = { Text("商店") },
                    modifier = Modifier.testTag("nav_tab_store")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    icon = { Icon(Icons.Rounded.Leaderboard, contentDescription = "排行") },
                    label = { Text("排行") },
                    modifier = Modifier.testTag("nav_tab_leaderboard")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    icon = { Icon(Icons.Rounded.Groups, contentDescription = "社区") },
                    label = { Text("社区") },
                    modifier = Modifier.testTag("nav_tab_community")
                )
                val unreadNoticeCount = notifications.count { !it.isRead }
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadNoticeCount > 0) {
                                    Badge { Text("$unreadNoticeCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = "消息")
                        }
                    },
                    label = { Text("消息") },
                    modifier = Modifier.testTag("nav_tab_messages")
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.selectTab(4) },
                    icon = { Icon(Icons.Rounded.Person, contentDescription = "我的") },
                    label = { Text("我的") },
                    modifier = Modifier.testTag("nav_tab_mine")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> StoreScreen(
                    pulseData = pulseData,
                    apps = filteredApps,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    downloadProgress = downloadProgress,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onAppClick = { viewModel.openAppDetail(it) },
                    onDownloadClick = { viewModel.downloadApp(it) }
                )
                1 -> CategoryLeaderboardScreen(
                    categories = categories,
                    leaderboardItems = leaderboardItems,
                    leaderboardType = leaderboardType,
                    onLeaderboardTypeChange = { viewModel.setLeaderboardType(it) },
                    onCategoryClick = {
                        viewModel.selectCategory(it)
                        viewModel.selectTab(0)
                    },
                    onAppClick = { viewModel.openAppDetail(it) },
                    onDownloadClick = { viewModel.downloadApp(it) },
                    downloadProgress = downloadProgress
                )
                2 -> CommunityScreen(
                    user = currentUser,
                    boards = forumBoards,
                    posts = forumPosts,
                    socialFeed = socialFeed,
                    badges = userBadges,
                    onCheckin = { viewModel.performCheckin() },
                    onOpenPost = { viewModel.openPostDetail(it) },
                    onLikePost = { viewModel.toggleLikePost(it) },
                    onCreatePostClick = { viewModel.showCreatePostDialog.value = true }
                )
                3 -> MessagesScreen(
                    conversations = conversations,
                    notifications = notifications,
                    onOpenConversation = { viewModel.openChat(it) },
                    onMarkAllNotificationsRead = { viewModel.markAllNotificationsRead() }
                )
                4 -> MineScreen(
                    user = currentUser,
                    onOpenDeveloperWorkbench = { viewModel.showDeveloperWorkbench.value = true },
                    onOpenLiuYun = { viewModel.showLiuYunDialog.value = true },
                    onOpenApkParser = { viewModel.showApkParserDialog.value = true },
                    onOpenSponsor = { viewModel.showSponsorDialog.value = true },
                    onCheckClientUpdate = { viewModel.showClientUpdateDialog.value = true },
                    onEditProfile = { viewModel.showEditProfileDialog.value = true },
                    onOpenAgreement = { viewModel.showAgreementViewerDialog.value = true },
                    onOpenAuth = { viewModel.showAuthDialog.value = true },
                    onLogout = { viewModel.logout() }
                )
            }

            // Floating Expressive Download Banner
            val activeDownloadEntry = downloadProgress.entries.firstOrNull { it.value < 1.0f }
            if (activeDownloadEntry != null) {
                val activeApp = allApps.find { it.id == activeDownloadEntry.key }
                    ?: filteredApps.find { it.id == activeDownloadEntry.key }
                if (activeApp != null) {
                    val prog = activeDownloadEntry.value
                    val statusText = downloadStatus[activeApp.id] ?: "${(prog * 100).toInt()}%"
                    Card(
                        onClick = { viewModel.openAppDetail(activeApp) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .testTag("floating_download_card")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ZhAppIcon(
                                    iconUrl = activeApp.iconUrl,
                                    appName = activeApp.name,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "正在下载 ${activeApp.name}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                CircularWavyProgressIndicator(
                                    progress = { prog },
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearWavyProgressIndicator(
                                progress = { prog },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }

        // App Detail Sheet
        selectedApp?.let { app ->
            AppDetailSheet(
                app = app,
                downloadProgress = downloadProgress[app.id],
                downloadStatus = downloadStatus[app.id],
                isInstalled = viewModel.isAppInstalled(app.packageName),
                hasLocalApk = downloadedApkFiles.containsKey(app.id),
                onDismiss = { viewModel.closeAppDetail() },
                onDownload = { viewModel.downloadApp(app) }
            )
        }

        // Developer Workbench Sheet
        if (showDevWorkbench) {
            DeveloperWorkbenchSheet(
                developerApps = developerApps,
                pendingVersions = pendingVersions,
                rejectedApps = rejectedApps,
                onDismiss = { viewModel.showDeveloperWorkbench.value = false },
                onTogglePublish = { viewModel.togglePublishApp(it) },
                onSubmitUpdate = { appId, verName, verCode, logs, url ->
                    viewModel.submitVersionUpdate(appId, verName, verCode, logs, url)
                },
                onReviewVersion = { id, approve, reason ->
                    viewModel.reviewPendingVersion(id, approve, reason)
                },
                onAddAppealComment = { rejectId, msg ->
                    viewModel.addAppealComment(rejectId, msg)
                },
                onOpenApkParser = {
                    viewModel.showDeveloperWorkbench.value = false
                    viewModel.showApkParserDialog.value = true
                }
            )
        }

        // Chat Detail Sheet
        activeConversation?.let { conv ->
            ChatDetailSheet(
                conversation = conv,
                messages = messagesMap[conv.id].orEmpty(),
                onDismiss = { viewModel.closeChat() },
                onSendMessage = { viewModel.sendChatMessage(it) }
            )
        }

        // Post Detail Sheet
        selectedPost?.let { post ->
            PostDetailSheet(
                post = post,
                replies = postRepliesMap[post.id].orEmpty(),
                onDismiss = { viewModel.closePostDetail() },
                onLike = { viewModel.toggleLikePost(post.id) },
                onSendReply = { viewModel.replyPost(post.id, it) }
            )
        }

        // Modals & Dialogs
        if (showCreatePost) {
            CreatePostDialog(
                boards = forumBoards,
                onDismiss = { viewModel.showCreatePostDialog.value = false },
                onSubmit = { boardId, title, content ->
                    viewModel.createForumPost(boardId, title, content)
                }
            )
        }

        if (showSponsor) {
            SponsorApplyDialog(
                onDismiss = { viewModel.showSponsorDialog.value = false },
                onSubmit = { channel, amount, note ->
                    viewModel.submitSponsor(channel, amount, note)
                }
            )
        }

        if (showApkParser) {
            ApkParserDialog(
                apkResult = apkResult,
                isAnalyzing = isAnalyzingApk,
                onDismiss = { viewModel.showApkParserDialog.value = false },
                onParse = { viewModel.parseApk(it) }
            )
        }

        if (showLiuYun) {
            LiuYunDialog(
                liuYunInfo = liuYunInfo,
                onDismiss = { viewModel.showLiuYunDialog.value = false }
            )
        }

        if (showClientUpdate) {
            ClientUpdateDialog(
                clientUpdateInfo = clientUpdateInfo,
                onDismiss = { viewModel.showClientUpdateDialog.value = false },
                onUpdate = {
                    viewModel.showClientUpdateDialog.value = false
                    viewModel.downloadApp(
                        com.example.zhshop.data.model.AppItem(
                            id = "client_update",
                            name = "ZHShop 官方客户端",
                            packageName = "com.aistudio.zhshop.app",
                            iconUrl = "https://picsum.photos/seed/zhshop/128/128",
                            summary = "客户端更新安装包",
                            description = "更新至 v2.5.0-Release",
                            categoryId = "c_sys",
                            categoryName = "系统工具",
                            developerName = "ZHShop 官方团队",
                            developerId = "dev_official",
                            rating = 5.0f,
                            downloadCount = 1000000,
                            sizeBytes = 15000000,
                            formattedSize = "14.3 MB",
                            currentVersion = "2.5.0",
                            versionCode = 250,
                            minSdk = 24,
                            changelog = clientUpdateInfo.changelog,
                            screenshots = emptyList(),
                            tags = listOf("官方自更新"),
                            downloadUrl = clientUpdateInfo.downloadUrl,
                            apkMd5 = "7d4b9f20e89cae61830219c0deffea51"
                        )
                    )
                }
            )
        }

        if (showEditProfile) {
            EditProfileDialog(
                currentUsername = currentUser?.username ?: "",
                currentBio = currentUser?.bio ?: "",
                onDismiss = { viewModel.showEditProfileDialog.value = false },
                onSave = { username, bio ->
                    viewModel.updateProfile(username, bio)
                }
            )
        }

        if (showAgreementViewer) {
            AgreementViewerDialog(
                onDismiss = { viewModel.showAgreementViewerDialog.value = false },
                onRevokeConsent = {
                    viewModel.revokeAgreementConsent()
                }
            )
        }

        if (showAuth) {
            AuthDialog(
                onDismiss = { viewModel.showAuthDialog.value = false },
                onLogin = { email, pass, onResult ->
                    viewModel.login(email, pass) { success, msg ->
                        onResult(success, msg)
                        if (success) {
                            viewModel.showAuthDialog.value = false
                        }
                    }
                },
                onRegister = { email, pass, username, onResult ->
                    viewModel.register(email, pass, username) { success, msg ->
                        onResult(success, msg)
                        if (success) {
                            viewModel.showAuthDialog.value = false
                        }
                    }
                }
            )
        }
    }
}
