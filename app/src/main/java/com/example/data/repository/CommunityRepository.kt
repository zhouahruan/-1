package com.example.data.repository

import com.example.data.api.CommunityApiClient
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommunityRepository(val apiClient: CommunityApiClient = CommunityApiClient()) {

    // 当前登录用户；未登录时为 null，不提供任何演示账号
    private val _currentUser = MutableStateFlow<Profile?>(null)
    val currentUser: StateFlow<Profile?> = _currentUser.asStateFlow()

    private val _userLikedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userLikedPostIds: StateFlow<Set<String>> = _userLikedPostIds.asStateFlow()

    private val _userFavoritedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val userFavoritedPostIds: StateFlow<Set<String>> = _userFavoritedPostIds.asStateFlow()

    // 内存缓存（仅存放接口成功返回的数据）
    private val cachedPosts = mutableListOf<Post>()
    private val cachedComments = mutableMapOf<String, MutableList<Comment>>()
    private val cachedGroupMessages = mutableMapOf<String, MutableList<GroupMessage>>()

    // ==================== USER & AUTH ====================



    suspend fun restoreSession(): Profile? {
        if (!apiClient.restoreSession()) return null
        val userId = apiClient.currentUserId ?: return null
        val profile = apiClient.getProfile(userId).getOrElse {
            apiClient.clearSession()
            return null
        }
        _currentUser.value = profile
        return profile
    }

    fun logout() {
        apiClient.logOut()
        _currentUser.value = null
        _userLikedPostIds.value = emptySet()
        _userFavoritedPostIds.value = emptySet()
    }


    /** 发送登录/注册短信验证码。 */
    suspend fun sendLoginOtp(phone: String): Result<Unit> = apiClient.sendPhoneOtp(phone)

    /** 验证码登录：手机号首次登录时后端会自动完成注册，isNewUser 为 true。 */
    suspend fun loginWithOtp(phone: String, code: String): Result<Pair<Profile, Boolean>> =
        apiClient.verifyPhoneOtp(phone, code).map { result ->
            _currentUser.value = result.profile
            result.profile to result.isNewUser
        }

    /** 手机号 + 密码登录。 */
    suspend fun loginWithPassword(phone: String, pass: String): Result<Profile> =
        apiClient.passwordLogin(phone, pass).map { result ->
            _currentUser.value = result.profile
            result.profile
        }

    suspend fun updateProfile(nickname: String?, bio: String?, avatarUrl: String?): Boolean {
        val curr = _currentUser.value ?: return false
        val ok = apiClient.updateProfile(curr.id, nickname, bio, avatarUrl).getOrElse { return false }
        if (!ok) return false
        _currentUser.value = curr.copy(
            nickname = nickname ?: curr.nickname,
            bio = bio ?: curr.bio,
            avatar_url = avatarUrl ?: curr.avatar_url
        )
        return true
    }

    // ==================== POSTS ====================

    suspend fun getPosts(page: Int = 0, tagFilter: String? = null): List<Post> {
        val result = apiClient.getPosts(page, 20, tagFilter)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            val remotePosts = result.getOrThrow()
            // Merge with local state
            return remotePosts.map { p ->
                p.copy(
                    isLikedByMe = _userLikedPostIds.value.contains(p.id),
                    isFavoritedByMe = _userFavoritedPostIds.value.contains(p.id)
                )
            }
        }
        // Fallback to cached seed posts
        val list = if (!tagFilter.isNullOrBlank() && tagFilter != "全部") {
            cachedPosts.filter { it.ai_tags?.contains(tagFilter) == true }
        } else {
            cachedPosts
        }
        return list.map { p ->
            p.copy(
                isLikedByMe = _userLikedPostIds.value.contains(p.id),
                isFavoritedByMe = _userFavoritedPostIds.value.contains(p.id)
            )
        }
    }

    /** 首页分类：来自真实帖子的 ai_tags 聚合；接口失败时只保留「全部」。 */
    suspend fun getTagOptions(): List<String> =
        apiClient.getRecentTagOptions().getOrElse { emptyList() }

    suspend fun getPostDetail(postId: String): Post? {
        apiClient.incrementPostViews(postId)
        val result = apiClient.getPostDetail(postId)
        val basePost = result.getOrNull() ?: cachedPosts.find { it.id == postId } ?: return null
        return basePost.copy(
            view_count = basePost.view_count + 1,
            isLikedByMe = _userLikedPostIds.value.contains(basePost.id),
            isFavoritedByMe = _userFavoritedPostIds.value.contains(basePost.id)
        )
    }

    suspend fun createPost(title: String, content: String, tags: List<String>, imageUrl: String?): Post {
        val curr = _currentUser.value ?: throw IllegalStateException("请先登录后再发帖")
        val result = apiClient.createPost(curr.id, title, content, tags, imageUrl)
        val newPost = result.getOrThrow()
        cachedPosts.add(0, newPost)
        return newPost
    }

    suspend fun togglePostLike(postId: String): Pair<Boolean, Int> {
        val currentLiked = _userLikedPostIds.value.contains(postId)
        val newLiked = !currentLiked
        val newSet = if (newLiked) _userLikedPostIds.value + postId else _userLikedPostIds.value - postId
        _userLikedPostIds.value = newSet

        // Call backend RPC
        apiClient.togglePostLike(postId)

        // Update local count
        val postIndex = cachedPosts.indexOfFirst { it.id == postId }
        val newCount = if (postIndex >= 0) {
            val p = cachedPosts[postIndex]
            val updatedCount = (p.like_count + if (newLiked) 1 else -1).coerceAtLeast(0)
            cachedPosts[postIndex] = p.copy(like_count = updatedCount)
            updatedCount
        } else {
            if (newLiked) 1 else 0
        }
        return Pair(newLiked, newCount)
    }

    suspend fun togglePostFavorite(postId: String): Pair<Boolean, Int> {
        val currentFav = _userFavoritedPostIds.value.contains(postId)
        val newFav = !currentFav
        val newSet = if (newFav) _userFavoritedPostIds.value + postId else _userFavoritedPostIds.value - postId
        _userFavoritedPostIds.value = newSet

        // Call backend RPC
        apiClient.togglePostFavorite(postId)

        val postIndex = cachedPosts.indexOfFirst { it.id == postId }
        val newCount = if (postIndex >= 0) {
            val p = cachedPosts[postIndex]
            val updatedCount = (p.favorite_count + if (newFav) 1 else -1).coerceAtLeast(0)
            cachedPosts[postIndex] = p.copy(favorite_count = updatedCount)
            updatedCount
        } else {
            if (newFav) 1 else 0
        }
        return Pair(newFav, newCount)
    }

    suspend fun deletePost(postId: String): Boolean {
        apiClient.deletePost(postId)
        cachedPosts.removeAll { it.id == postId }
        return true
    }

    // ==================== COMMENTS ====================

    suspend fun getComments(postId: String): List<Comment> {
        val result = apiClient.getComments(postId)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            return result.getOrThrow()
        }
        return cachedComments[postId] ?: emptyList()
    }

    suspend fun addComment(postId: String, content: String): Comment {
        val curr = _currentUser.value ?: throw IllegalStateException("请先登录后再评论")
        val result = apiClient.addComment(postId, curr.id, content)
        val newComment = result.getOrThrow()
        val list = cachedComments.getOrPut(postId) { mutableListOf() }
        list.add(newComment)

        // Increment post comment count locally
        val idx = cachedPosts.indexOfFirst { it.id == postId }
        if (idx >= 0) {
            cachedPosts[idx] = cachedPosts[idx].copy(comment_count = cachedPosts[idx].comment_count + 1)
        }
        return newComment
    }

    // ==================== SEARCH ====================

    suspend fun searchPosts(keyword: String): List<Post> {
        val result = apiClient.searchPosts(keyword)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            return result.getOrThrow()
        }
        val kw = keyword.lowercase()
        return cachedPosts.filter {
            it.title.lowercase().contains(kw) || it.content.lowercase().contains(kw) || it.ai_tags?.any { t -> t.lowercase().contains(kw) } == true
        }
    }

    suspend fun searchProfiles(keyword: String): List<Profile> {
        val result = apiClient.searchProfiles(keyword)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            return result.getOrThrow()
        }
        val kw = keyword.lowercase()
        val allUsers = listOfNotNull(_currentUser.value)
        return allUsers.filter { it.displayName.lowercase().contains(kw) || it.phone?.contains(kw) == true }
    }

    // ==================== MINI-APPS ====================

    suspend fun getMiniApps(): List<MiniApp> = apiClient.getMiniApps().getOrThrow()

    /** 取单个小程序（包含运行地址所需的 entry_url / live_version） */
    suspend fun getMiniApp(miniAppId: String): MiniApp = apiClient.getMiniApp(miniAppId).getOrThrow()

    // ==================== CLOUD DRIVE ====================

    suspend fun getUserServer(): UserServer {
        val userId = apiClient.currentUserId ?: throw IllegalStateException("请先登录后查看个人云盘")
        return apiClient.getUserServer(userId).getOrThrow()
    }

    suspend fun getServerFolders(serverId: String): List<ServerFolder> =
        apiClient.getServerFolders(serverId).getOrThrow()

    suspend fun getServerFiles(serverId: String, folderId: String? = null): List<ServerFile> =
        apiClient.getServerFiles(serverId, folderId).getOrThrow()

    suspend fun createShareLink(fileId: String, password: String?, maxDownloads: Int, expiresIn: String): ShareResult {
        if (apiClient.currentUserId == null) throw IllegalStateException("请先登录再创建分享")
        return apiClient.createShareLink(fileId, password, maxDownloads, expiresIn).getOrThrow()
    }

    suspend fun extractShareInfo(code: String): ShareInfo {
        return apiClient.getShareInfo(code).getOrThrow()
    }

    suspend fun downloadShareFile(code: String, password: String?): String {
        return apiClient.downloadShare(code, password).getOrThrow()
    }

    // ==================== GROUPS & CHAT ====================

    suspend fun getGroups(): List<Group> = apiClient.getGroups().getOrElse { emptyList() }

    suspend fun getGroupMessages(groupId: String): List<GroupMessage> {
        val result = apiClient.getGroupMessages(groupId)
        if (result.isSuccess && result.getOrNull()?.isNotEmpty() == true) {
            return result.getOrThrow()
        }
        return cachedGroupMessages[groupId] ?: emptyList()
    }

    suspend fun sendGroupMessage(groupId: String, content: String): GroupMessage {
        val curr = _currentUser.value ?: throw IllegalStateException("请先登录后再发送消息")
        val result = apiClient.sendGroupMessage(groupId, curr.id, content)
        val newMsg = result.getOrThrow()
        val list = cachedGroupMessages.getOrPut(groupId) { mutableListOf() }
        list.add(newMsg)
        return newMsg
    }

    // ==================== ANNOUNCEMENTS & FEEDBACK ====================

    /** 帖子/公告挂载的红包，没有则返回 null */
    suspend fun getRedPacket(postId: String? = null, announcementId: String? = null): RedPacket? =
        apiClient.getRedPacket(postId, announcementId).getOrNull()

    /** 抢红包 */
    suspend fun grabRedPacket(packetId: String): RedPacketGrabResult =
        apiClient.grabRedPacket(packetId).getOrThrow()

    suspend fun getAnnouncements(): List<Announcement> =
        apiClient.getAnnouncements().getOrElse { emptyList() }

    suspend fun submitFeedback(type: String, title: String, content: String, contact: String?): Boolean {
        val curr = _currentUser.value ?: throw IllegalStateException("请先登录后再提交反馈")
        return apiClient.submitFeedback(curr.id, type, title, content, contact).isSuccess
    }

    // ==================== ADMIN GOVERNANCE ====================

    suspend fun updateUserRisk(targetUserId: String, status: String, reason: String?): Boolean {
        return apiClient.updateUserRiskStatus(targetUserId, status, reason).isSuccess
    }
}
