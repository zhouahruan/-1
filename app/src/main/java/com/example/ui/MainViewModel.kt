package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.CommunityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScreenDestination {
    object MainTabs : ScreenDestination()
    data class PostDetail(val postId: String) : ScreenDestination()
    data class MiniAppSandbox(val miniAppId: String, val miniAppName: String) : ScreenDestination()
    data class GroupChat(val groupId: String, val groupName: String) : ScreenDestination()
    data class VideoPlayer(val videoUrl: String, val videoTitle: String = "视频") : ScreenDestination()
    data class ImageViewer(val images: List<String>, val initialIndex: Int = 0) : ScreenDestination()
    data class WebPage(val url: String, val title: String = "网页") : ScreenDestination()
}

class MainViewModel(val repository: CommunityRepository = CommunityRepository()) : ViewModel() {

    // Current navigation destination
    private val _currentDestination = MutableStateFlow<ScreenDestination>(ScreenDestination.MainTabs)
    val currentDestination: StateFlow<ScreenDestination> = _currentDestination.asStateFlow()

    // Current Tab Index (0: Home, 1: Discover, 2: Cloud Drive, 3: Search, 4: Profile)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // 当前登录用户（未登录为 null）
    val currentUser: StateFlow<Profile?> = repository.currentUser

    // Posts & Feed
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _selectedTag = MutableStateFlow("全部")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // 首页分类（来自帖子真实标签，不再硬编码）
    private val _tags = MutableStateFlow<List<String>>(listOf("全部"))
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _isLoadingFeed = MutableStateFlow(false)
    val isLoadingFeed: StateFlow<Boolean> = _isLoadingFeed.asStateFlow()

    // Announcements
    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    // Post Detail Screen State
    private val _currentPostDetail = MutableStateFlow<Post?>(null)
    val currentPostDetail: StateFlow<Post?> = _currentPostDetail.asStateFlow()

    private val _currentComments = MutableStateFlow<List<Comment>>(emptyList())
    val currentComments: StateFlow<List<Comment>> = _currentComments.asStateFlow()

    // Discover (MiniApps & Groups)
    private val _miniApps = MutableStateFlow<List<MiniApp>>(emptyList())
    val miniApps: StateFlow<List<MiniApp>> = _miniApps.asStateFlow()

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    // MiniApp 运行地址
    private val _sandboxUrl = MutableStateFlow("")
    val sandboxUrl: StateFlow<String> = _sandboxUrl.asStateFlow()

    private val _miniAppsError = MutableStateFlow<String?>(null)
    val miniAppsError: StateFlow<String?> = _miniAppsError.asStateFlow()

    private val _sandboxError = MutableStateFlow<String?>(null)
    val sandboxError: StateFlow<String?> = _sandboxError.asStateFlow()

    private val _cloudError = MutableStateFlow<String?>(null)
    val cloudError: StateFlow<String?> = _cloudError.asStateFlow()

    private val _cloudLoading = MutableStateFlow(false)
    val cloudLoading: StateFlow<Boolean> = _cloudLoading.asStateFlow()

    // Group Chat Messages
    private val _groupMessages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val groupMessages: StateFlow<List<GroupMessage>> = _groupMessages.asStateFlow()

    // Cloud Drive
    private val _userServer = MutableStateFlow<UserServer?>(null)
    val userServer: StateFlow<UserServer?> = _userServer.asStateFlow()

    private val _folders = MutableStateFlow<List<ServerFolder>>(emptyList())
    val folders: StateFlow<List<ServerFolder>> = _folders.asStateFlow()

    private val _files = MutableStateFlow<List<ServerFile>>(emptyList())
    val files: StateFlow<List<ServerFile>> = _files.asStateFlow()

    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    // Search
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    private val _searchPostsResult = MutableStateFlow<List<Post>>(emptyList())
    val searchPostsResult: StateFlow<List<Post>> = _searchPostsResult.asStateFlow()

    private val _searchUsersResult = MutableStateFlow<List<Profile>>(emptyList())
    val searchUsersResult: StateFlow<List<Profile>> = _searchUsersResult.asStateFlow()

    // 登录状态提示（登录弹窗使用）
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // 验证码发送状态（登录/注册共用）
    private val _otpSending = MutableStateFlow(false)
    val otpSending: StateFlow<Boolean> = _otpSending.asStateFlow()

    /** 验证码发送成功后自增，供界面重置倒计时 */
    private val _otpSentTicks = MutableStateFlow(0)
    val otpSentTicks: StateFlow<Int> = _otpSentTicks.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    // 红包状态（帖子 / 公告共用一套加载与抢包状态）
    private val _postRedPacket = MutableStateFlow<RedPacket?>(null)
    val postRedPacket: StateFlow<RedPacket?> = _postRedPacket.asStateFlow()

    private val _announcementRedPacket = MutableStateFlow<RedPacket?>(null)
    val announcementRedPacket: StateFlow<RedPacket?> = _announcementRedPacket.asStateFlow()

    private val _redPacketLoading = MutableStateFlow(false)
    val redPacketLoading: StateFlow<Boolean> = _redPacketLoading.asStateFlow()

    private val _redPacketGrabbing = MutableStateFlow(false)
    val redPacketGrabbing: StateFlow<Boolean> = _redPacketGrabbing.asStateFlow()

    // 公告详情弹窗
    private val _activeAnnouncement = MutableStateFlow<Announcement?>(null)
    val activeAnnouncement: StateFlow<Announcement?> = _activeAnnouncement.asStateFlow()

    // Dialog flags
    var showLoginDialog = MutableStateFlow(false)
    var showCreatePostDialog = MutableStateFlow(false)
    var showFeedbackDialog = MutableStateFlow(false)
    var showShareExtractDialog = MutableStateFlow(false)
    var showShareCreateDialog = MutableStateFlow(false)
    var targetShareFile = MutableStateFlow<ServerFile?>(null)
    var lastShareResult = MutableStateFlow<ShareResult?>(null)

    // User message / Toast snackbar
    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage: StateFlow<String?> = _snackBarMessage.asStateFlow()

    init {
        // 静默恢复上次登录状态；未登录（或恢复失败）也可以正常浏览帖子，不强制登录
        viewModelScope.launch {
            repository.restoreSession()
            loadPublicData()
            if (currentUser.value != null) loadCloudDrive()
        }
    }

    /** 无需登录即可浏览的公开内容 */
    private fun loadPublicData() {
        loadFeed()
        loadAnnouncements()
        loadDiscoverData()
        loadTags()
    }

    private fun loadAllData() {
        loadFeed()
        loadAnnouncements()
        loadDiscoverData()
        loadTags()
        loadCloudDrive()
    }

    /** 拉取首页分类标签（帖子 ai_tags 聚合） */
    fun loadTags() {
        viewModelScope.launch {
            val remote = repository.getTagOptions()
            _tags.value = listOf("全部") + remote.take(12)
            // 当前选中的分类在新列表里不存在时，退回「全部」
            if (remote.isNotEmpty() && _selectedTag.value != "全部" && !_tags.value.contains(_selectedTag.value)) {
                _selectedTag.value = "全部"
                loadFeed()
            }
        }
    }

    /** 需要登录的操作调用：未登录时弹出登录框并提示，返回 false */
    fun requireLogin(action: String = "该操作"): Boolean {
        if (currentUser.value != null) return true
        showToast("请先登录后再$action")
        showLoginDialog.value = true
        return false
    }

    fun showToast(msg: String) {
        _snackBarMessage.value = msg
    }

    fun clearToast() {
        _snackBarMessage.value = null
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
        _currentDestination.value = ScreenDestination.MainTabs
    }

    fun navigateTo(dest: ScreenDestination) {
        _currentDestination.value = dest
        when (dest) {
            is ScreenDestination.PostDetail -> loadPostDetail(dest.postId)
            is ScreenDestination.MiniAppSandbox -> loadMiniAppSandbox(dest.miniAppId)
            is ScreenDestination.GroupChat -> loadGroupChat(dest.groupId)
            is ScreenDestination.VideoPlayer -> {}
            is ScreenDestination.ImageViewer -> {}
            is ScreenDestination.WebPage -> {}
            ScreenDestination.MainTabs -> {}
        }
    }

    fun navigateBack() {
        _currentDestination.value = ScreenDestination.MainTabs
    }

    // ==================== FEED & POSTS ====================

    fun filterByTag(tag: String) {
        _selectedTag.value = tag
        loadFeed()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isLoadingFeed.value = true
            _posts.value = repository.getPosts(tagFilter = _selectedTag.value)
            _isLoadingFeed.value = false
        }
    }

    fun loadAnnouncements() {
        viewModelScope.launch {
            _announcements.value = repository.getAnnouncements()
        }
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _currentPostDetail.value = repository.getPostDetail(postId)
            _currentComments.value = repository.getComments(postId)
        }
        loadPostRedPacket(postId)
    }

    /** 帖子挂载的红包 */
    fun loadPostRedPacket(postId: String) {
        _postRedPacket.value = null
        _redPacketLoading.value = true
        viewModelScope.launch {
            _postRedPacket.value = repository.getRedPacket(postId = postId)
            _redPacketLoading.value = false
        }
    }

    /** 查看公告详情，并查询该公告是否挂载红包 */
    fun showAnnouncement(announcement: Announcement) {
        _activeAnnouncement.value = announcement
        _announcementRedPacket.value = null
        viewModelScope.launch {
            _announcementRedPacket.value = repository.getRedPacket(announcementId = announcement.id)
        }
    }

    fun dismissAnnouncement() {
        _activeAnnouncement.value = null
        _announcementRedPacket.value = null
    }

    /** 抢红包：抢到后需打开后端返回的积分接收地址 */
    fun grabRedPacket(packet: RedPacket) {
        if (!requireLogin("抢红包")) return
        if (_redPacketGrabbing.value) return
        viewModelScope.launch {
            _redPacketGrabbing.value = true
            try {
                val result = repository.grabRedPacket(packet.id)
                showToast("已抢到 ${result.amount} 积分，请在打开的页面完成领取")
                if (result.auth_url.isNotBlank()) {
                    navigateTo(ScreenDestination.WebPage(result.auth_url, "领取积分"))
                }
            } catch (e: Exception) {
                showToast("抢红包失败：${e.message}")
            } finally {
                _redPacketGrabbing.value = false
            }
        }
    }

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val (liked, newCount) = repository.togglePostLike(postId)
            _posts.value = _posts.value.map {
                if (it.id == postId) it.copy(isLikedByMe = liked, like_count = newCount) else it
            }
            if (_currentPostDetail.value?.id == postId) {
                _currentPostDetail.value = _currentPostDetail.value?.copy(isLikedByMe = liked, like_count = newCount)
            }
        }
    }

    fun toggleFavorite(postId: String) {
        viewModelScope.launch {
            val (favorited, newCount) = repository.togglePostFavorite(postId)
            _posts.value = _posts.value.map {
                if (it.id == postId) it.copy(isFavoritedByMe = favorited, favorite_count = newCount) else it
            }
            if (_currentPostDetail.value?.id == postId) {
                _currentPostDetail.value = _currentPostDetail.value?.copy(isFavoritedByMe = favorited, favorite_count = newCount)
            }
            showToast(if (favorited) "已加入收藏" else "已取消收藏")
        }
    }

    fun createPost(title: String, content: String, tags: List<String>, imageUrl: String?) {
        viewModelScope.launch {
            try {
                val post = repository.createPost(title, content, tags, imageUrl)
                _posts.value = listOf(post) + _posts.value
                showCreatePostDialog.value = false
                showToast("发布成功！")
            } catch (e: Exception) {
                showToast("发布失败: ${e.message}")
            }
        }
    }

    fun addComment(postId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val newComment = repository.addComment(postId, content)
                _currentComments.value = _currentComments.value + newComment
                _posts.value = _posts.value.map {
                    if (it.id == postId) it.copy(comment_count = it.comment_count + 1) else it
                }
                if (_currentPostDetail.value?.id == postId) {
                    _currentPostDetail.value = _currentPostDetail.value?.copy(
                        comment_count = (_currentPostDetail.value?.comment_count ?: 0) + 1
                    )
                }
                showToast("评论发表成功")
            } catch (e: Exception) {
                showToast("评论失败: ${e.message}")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
            _posts.value = _posts.value.filterNot { it.id == postId }
            if (_currentPostDetail.value?.id == postId) {
                navigateBack()
            }
            showToast("帖子已删除")
        }
    }

    // ==================== DISCOVER ====================

    fun loadDiscoverData() {
        viewModelScope.launch {
            try {
                _miniApps.value = repository.getMiniApps()
                _miniAppsError.value = null
            } catch (e: Exception) {
                _miniApps.value = emptyList()
                _miniAppsError.value = e.message ?: "轻应用加载失败"
            }
            _groups.value = repository.getGroups()
        }
    }

    private fun loadMiniAppSandbox(appId: String) {
        _sandboxUrl.value = ""
        _sandboxError.value = null
        viewModelScope.launch {
            try {
                // ZIP 类型走 miniapp-serve Edge Function，直链类型用 entry_url
                _sandboxUrl.value = repository.getMiniApp(appId).runUrl
            } catch (e: Exception) {
                _sandboxError.value = e.message ?: "小程序加载失败"
            }
        }
    }

    private fun loadGroupChat(groupId: String) {
        viewModelScope.launch {
            _groupMessages.value = repository.getGroupMessages(groupId)
        }
    }

    fun sendGroupChatMessage(groupId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val msg = repository.sendGroupMessage(groupId, content)
                _groupMessages.value = _groupMessages.value + msg
            } catch (e: Exception) {
                showToast("发送失败: ${e.message}")
            }
        }
    }

    // ==================== CLOUD DRIVE ====================

    fun loadCloudDrive(folderId: String? = null) {
        _currentFolderId.value = folderId
        if (currentUser.value == null) {
            _userServer.value = null
            _folders.value = emptyList()
            _files.value = emptyList()
            _cloudError.value = "请先登录后查看个人云盘"
            return
        }
        _cloudLoading.value = true
        _cloudError.value = null
        _userServer.value = null
        _folders.value = emptyList()
        _files.value = emptyList()
        viewModelScope.launch {
            try {
                val server = repository.getUserServer()
                _userServer.value = server
                _folders.value = repository.getServerFolders(server.id)
                _files.value = repository.getServerFiles(server.id, folderId)
            } catch (e: Exception) {
                _cloudError.value = e.message ?: "云盘加载失败"
            } finally {
                _cloudLoading.value = false
            }
        }
    }

    fun prepareShareFile(file: ServerFile) {
        targetShareFile.value = file
        lastShareResult.value = null
        showShareCreateDialog.value = true
    }

    fun confirmCreateShare(password: String?, maxDownloads: Int, expiresIn: String) {
        val file = targetShareFile.value ?: return
        viewModelScope.launch {
            try {
                val res = repository.createShareLink(file.id, password, maxDownloads, expiresIn)
                lastShareResult.value = res
                showToast("分享链接生成成功！提取码: ${res.code}")
            } catch (e: Exception) {
                showToast("分享失败: ${e.message}")
            }
        }
    }

    fun extractFileByCode(code: String, pass: String?, onResult: (ShareInfo) -> Unit) {
        viewModelScope.launch {
            try {
                val info = repository.extractShareInfo(code.trim())
                onResult(info)
            } catch (e: Exception) {
                showToast("查询失败: ${e.message ?: "请检查提取码"}")
            }
        }
    }

    fun downloadShareLink(code: String, pass: String?, onDirectUrl: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val url = repository.downloadShareFile(code.trim(), pass)
                onDirectUrl(url)
                showToast("直链解析成功，正在开始下载！")
            } catch (e: Exception) {
                showToast(e.message ?: "下载失败")
            }
        }
    }

    // ==================== SEARCH ====================

    fun onSearchKeywordChanged(kw: String) {
        _searchKeyword.value = kw
        if (kw.isNotBlank()) {
            viewModelScope.launch {
                _searchPostsResult.value = repository.searchPosts(kw)
                _searchUsersResult.value = repository.searchProfiles(kw)
            }
        } else {
            _searchPostsResult.value = emptyList()
            _searchUsersResult.value = emptyList()
        }
    }

    // ==================== PROFILE & ADMIN ====================

    /** 发送登录/注册验证码（注册 = 未注册手机号验证码首次登录）。 */
    fun sendLoginCode(phone: String) {
        if (_otpSending.value) return
        viewModelScope.launch {
            _otpSending.value = true
            _authError.value = null
            val res = repository.sendLoginOtp(phone)
            _otpSending.value = false
            if (res.isSuccess) {
                _otpSent.value = true
                _otpSentTicks.value += 1
                showToast("验证码已发送，请查收短信")
            } else {
                _authError.value = res.exceptionOrNull()?.message ?: "验证码发送失败，请稍后重试"
            }
        }
    }

    /** 验证码登录：未注册的手机号会自动完成注册并直接登录。 */
    fun verifyLoginCode(phone: String, code: String) {
        if (_authLoading.value) return
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = repository.loginWithOtp(phone, code)
            _authLoading.value = false
            if (res.isSuccess) {
                val (profile, isNewUser) = res.getOrThrow()
                showLoginDialog.value = false
                _otpSent.value = false
                loadAllData()
                showToast(if (isNewUser) "注册成功，欢迎加入雾点社区" else "登录成功，欢迎 ${profile.displayName}")
            } else {
                _authError.value = res.exceptionOrNull()?.message ?: "验证码校验失败"
            }
        }
    }

    /** 手机号 + 密码登录。 */
    fun loginWithPassword(phone: String, password: String) {
        if (_authLoading.value) return
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val res = repository.loginWithPassword(phone, password)
            _authLoading.value = false
            if (res.isSuccess) {
                showLoginDialog.value = false
                loadAllData()
                showToast("登录成功，欢迎 ${res.getOrNull()?.displayName}")
            } else {
                _authError.value = res.exceptionOrNull()?.message ?: "登录失败，请检查手机号与密码"
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }


    fun logout() {
        repository.logout()
        _posts.value = emptyList()
        _announcements.value = emptyList()
        _miniApps.value = emptyList()
        _groups.value = emptyList()
        _userServer.value = null
        _folders.value = emptyList()
        _files.value = emptyList()
        _currentFolderId.value = null
        _currentPostDetail.value = null
        _currentComments.value = emptyList()
        _groupMessages.value = emptyList()
        _miniAppsError.value = null
        _cloudError.value = null
        _sandboxError.value = null
        _authError.value = null
        _otpSent.value = false
        _postRedPacket.value = null
        _announcementRedPacket.value = null
        _activeAnnouncement.value = null
        _currentDestination.value = ScreenDestination.MainTabs
        _selectedTab.value = 0
        showToast("已退出登录")
    }

    fun submitFeedback(type: String, title: String, content: String, contact: String?) {
        viewModelScope.launch {
            val ok = repository.submitFeedback(type, title, content, contact)
            showFeedbackDialog.value = false
            showToast(if (ok) "感谢您的反馈，已提交至系统！" else "提交反馈失败")
        }
    }

    fun toggleUserRisk(targetUserId: String, currentStatus: String) {
        val newStatus = if (currentStatus == "normal") "flagged" else "normal"
        viewModelScope.launch {
            repository.updateUserRisk(targetUserId, newStatus, "管理员人工处置")
            showToast("已将用户风控状态更改为: $newStatus")
        }
    }
}
