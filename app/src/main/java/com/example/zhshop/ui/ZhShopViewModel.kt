package com.example.zhshop.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zhshop.data.model.*
import com.example.zhshop.data.repository.ZhShopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ZhShopViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ZhShopRepository(application)

    // Current navigation tab: 0: 探索商店, 1: 分类排行, 2: 极客社区, 3: 消息通知, 4: 我的空间
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Pulse / Home State
    private val _pulseData = MutableStateFlow<PulseData?>(null)
    val pulseData: StateFlow<PulseData?> = _pulseData.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // All Apps
    val allApps: StateFlow<List<AppItem>> = repository.appsList

    // Filtered Apps
    val filteredApps: StateFlow<List<AppItem>> = combine(
        repository.appsList,
        _searchQuery,
        _selectedCategoryId
    ) { apps, query, categoryId ->
        var list = apps
        if (!categoryId.isNullOrBlank()) {
            list = list.filter { it.categoryId == categoryId }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.summary.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Leaderboard
    private val _leaderboardType = MutableStateFlow("hot") // hot, download, rating, new
    val leaderboardType: StateFlow<String> = _leaderboardType.asStateFlow()

    private val _leaderboardItems = MutableStateFlow<List<LeaderboardItem>>(emptyList())
    val leaderboardItems: StateFlow<List<LeaderboardItem>> = _leaderboardItems.asStateFlow()

    // Detail Bottom Sheet & Modals
    private val _selectedApp = MutableStateFlow<AppItem?>(null)
    val selectedApp: StateFlow<AppItem?> = _selectedApp.asStateFlow()

    private val _selectedPost = MutableStateFlow<ForumPost?>(null)
    val selectedPost: StateFlow<ForumPost?> = _selectedPost.asStateFlow()

    private val _activeConversation = MutableStateFlow<Conversation?>(null)
    val activeConversation: StateFlow<Conversation?> = _activeConversation.asStateFlow()

    // Modals visibility
    val showDeveloperWorkbench = MutableStateFlow(false)
    val showCreatePostDialog = MutableStateFlow(false)
    val showSponsorDialog = MutableStateFlow(false)
    val showApkParserDialog = MutableStateFlow(false)
    val showLiuYunDialog = MutableStateFlow(false)
    val showClientUpdateDialog = MutableStateFlow(false)
    val showEditProfileDialog = MutableStateFlow(false)
    val showAgreementViewerDialog = MutableStateFlow(false)
    val showAuthDialog = MutableStateFlow(false)

    // Agreement security state
    val hasAgreedAgreement: StateFlow<Boolean> = repository.hasAgreedAgreement

    fun agreeToAgreement() {
        repository.saveAgreementConsent()
        _toastMessage.value = "已同意用户协议与隐私政策"
    }

    fun revokeAgreementConsent() {
        repository.revokeAgreementConsent()
        _toastMessage.value = "已撤回协议同意，需重新授权方可使用"
    }

    // APK Analysis result
    private val _apkResult = MutableStateFlow<ApkAnalysisResult?>(null)
    val apkResult: StateFlow<ApkAnalysisResult?> = _apkResult.asStateFlow()

    private val _isAnalyzingApk = MutableStateFlow(false)
    val isAnalyzingApk: StateFlow<Boolean> = _isAnalyzingApk.asStateFlow()

    // Snack messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Delegated repository states
    val currentUser = repository.tokenManager.currentUserFlow
    val categories = repository.categories
    val downloadProgress = repository.downloadProgress
    val downloadBytesState = repository.downloadBytesState
    val downloadedApkFiles = repository.downloadedApkFiles
    val downloadStatus = repository.downloadStatus
    val developerApps = repository.developerApps
    val pendingVersions = repository.pendingVersions
    val rejectedApps = repository.rejectedApps
    val forumBoards = repository.forumBoards
    val forumPosts = repository.forumPosts
    val postRepliesMap = repository.postRepliesMap
    val conversations = repository.conversations
    val messagesMap = repository.messagesMap
    val notifications = repository.notifications
    val collections = repository.collections
    val socialFeed = repository.socialFeed
    val sponsorApplications = repository.sponsorApplications
    val userBadges = repository.userBadges
    val liuYunInfo = repository.liuYunInfo
    val clientUpdateInfo = repository.clientUpdateInfo

    init {
        loadPulse()
        updateLeaderboard(_leaderboardType.value)
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        if (_selectedCategoryId.value == categoryId) {
            _selectedCategoryId.value = null
        } else {
            _selectedCategoryId.value = categoryId
        }
        viewModelScope.launch {
            repository.fetchAppsByCategory(_selectedCategoryId.value)
        }
    }

    fun setLeaderboardType(type: String) {
        _leaderboardType.value = type
        updateLeaderboard(type)
    }

    private fun updateLeaderboard(type: String) {
        viewModelScope.launch {
            _leaderboardItems.value = repository.getLeaderboard(type)
        }
    }

    fun loadPulse() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshAllData()
            _pulseData.value = repository.getPulseData()
            updateLeaderboard(_leaderboardType.value)
            _isRefreshing.value = false
        }
    }

    fun openAppDetail(app: AppItem) {
        _selectedApp.value = app
        viewModelScope.launch {
            val detailed = repository.fetchAppDetail(app.id)
            if (detailed != null && _selectedApp.value?.id == app.id) {
                _selectedApp.value = detailed
            }
        }
    }

    fun closeAppDetail() {
        _selectedApp.value = null
    }

    fun downloadApp(app: AppItem) {
        viewModelScope.launch {
            if (repository.isAppInstalled(app.packageName)) {
                val launched = repository.launchInstalledApp(app.packageName)
                if (launched) {
                    _toastMessage.value = "已启动「${app.name}」"
                    return@launch
                }
            }

            val cached = repository.downloadedApkFiles.value[app.id]
            if (cached != null && cached.exists() && cached.length() > 0L) {
                _toastMessage.value = "安装包已存在，正在调起系统安装..."
                repository.installDownloadedApk(app.id)
                return@launch
            }

            _toastMessage.value = "正在极速下载「${app.name}」..."
            val result = repository.downloadApp(app.id)
            result.onSuccess {
                _toastMessage.value = "「${app.name}」下载完毕，已调起系统安装器"
            }.onFailure { err ->
                _toastMessage.value = "下载失败: ${err.message}"
            }
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return repository.isAppInstalled(packageName)
    }

    fun installApk(appId: String) {
        repository.installDownloadedApk(appId)
    }

    fun launchInstalledApp(packageName: String) {
        repository.launchInstalledApp(packageName)
    }

    fun performCheckin() {
        viewModelScope.launch {
            val (success, msg) = repository.checkin()
            _toastMessage.value = msg
        }
    }

    fun updateProfile(username: String, bio: String) {
        viewModelScope.launch {
            repository.updateProfile(username, bio)
            showEditProfileDialog.value = false
            _toastMessage.value = "资料更新成功"
        }
    }

    // Developer functions
    fun togglePublishApp(appId: String) {
        viewModelScope.launch {
            repository.togglePublishApp(appId)
            _toastMessage.value = "应用状态已更新"
        }
    }

    fun submitVersionUpdate(appId: String, versionName: String, versionCode: Int, changelog: String, url: String) {
        viewModelScope.launch {
            repository.submitVersionUpdate(appId, versionName, versionCode, changelog, url)
            _toastMessage.value = "版本提审已提交，正在进入审核队列"
        }
    }

    fun reviewPendingVersion(id: String, approve: Boolean, reason: String = "") {
        viewModelScope.launch {
            repository.reviewVersion(id, approve, reason)
            _toastMessage.value = if (approve) "审核已通过并上架" else "已驳回版本"
        }
    }

    fun addAppealComment(rejectId: String, message: String) {
        viewModelScope.launch {
            repository.addAppealComment(rejectId, message)
            _toastMessage.value = "申诉回复已发送"
        }
    }

    // Community functions
    fun openPostDetail(post: ForumPost) {
        _selectedPost.value = post
        viewModelScope.launch {
            repository.fetchPostReplies(post.id)
        }
    }

    fun closePostDetail() {
        _selectedPost.value = null
    }

    fun createForumPost(boardId: String, title: String, content: String) {
        viewModelScope.launch {
            repository.createForumPost(boardId, title, content)
            showCreatePostDialog.value = false
            _toastMessage.value = "帖子发布成功！"
        }
    }

    fun toggleLikePost(postId: String) {
        viewModelScope.launch {
            repository.toggleLikePost(postId)
            if (_selectedPost.value?.id == postId) {
                _selectedPost.value = repository.forumPosts.value.find { it.id == postId }
            }
        }
    }

    fun replyPost(postId: String, content: String) {
        viewModelScope.launch {
            repository.replyPost(postId, content)
            if (_selectedPost.value?.id == postId) {
                _selectedPost.value = repository.forumPosts.value.find { it.id == postId }
            }
            _toastMessage.value = "楼层回复成功"
        }
    }

    // Chat functions
    fun openChat(conversation: Conversation) {
        _activeConversation.value = conversation
        viewModelScope.launch {
            repository.fetchMessages(conversation.id)
        }
    }

    fun closeChat() {
        _activeConversation.value = null
    }

    fun sendChatMessage(content: String) {
        val conv = _activeConversation.value ?: return
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(conv.id, content)
        }
    }

    fun login(email: String, pass: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.login(email, pass)
            result.onSuccess {
                _toastMessage.value = "欢迎回来，${it.username}！"
                onDone(true, "登录成功")
            }.onFailure {
                _toastMessage.value = "登录失败: ${it.message}"
                onDone(false, it.message ?: "登录失败")
            }
        }
    }

    fun register(email: String, pass: String, username: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = repository.register(email, pass, username)
            result.onSuccess {
                _toastMessage.value = "注册成功，欢迎加入 ZHShop！"
                onDone(true, "注册成功")
            }.onFailure {
                _toastMessage.value = "注册失败: ${it.message}"
                onDone(false, it.message ?: "注册失败")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _toastMessage.value = "已退出登录"
        }
    }

    fun checkClientUpdate() {
        viewModelScope.launch {
            val update = repository.checkClientLatestUpdate()
            if (update != null && update.hasUpdate) {
                _toastMessage.value = "发现新版本: ${update.latestVersion}"
                showClientUpdateDialog.value = true
            } else {
                _toastMessage.value = "当前已是最新客户端"
            }
        }
    }

    // Notification functions
    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            _toastMessage.value = "所有通知已标记为已读"
        }
    }

    // Sponsor
    fun submitSponsor(channel: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.submitSponsor(channel, amount, note)
            showSponsorDialog.value = false
            _toastMessage.value = "赞助申请已提交，感谢对 ZHShop 的支持！"
        }
    }

    // APK parser
    fun parseApk(url: String) {
        viewModelScope.launch {
            _isAnalyzingApk.value = true
            _apkResult.value = repository.parseApk(url)
            _isAnalyzingApk.value = false
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
