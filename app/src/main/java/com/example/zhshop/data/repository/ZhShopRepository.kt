package com.example.zhshop.data.repository

import android.content.Context
import android.os.Environment
import com.example.zhshop.data.api.ApiClient
import com.example.zhshop.data.api.TokenManager
import com.example.zhshop.data.api.ZhShopApiParser
import com.example.zhshop.data.model.*
import com.example.zhshop.util.ApkInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ZhShopRepository(private val context: Context) {
    val tokenManager = TokenManager(context)
    val zhShopApi = ApiClient.createZhShopApi(tokenManager)
    val apkParserApi = ApiClient.createApkParserApi()
    val liuYunApi = ApiClient.createLiuYunApi()

    val hasAgreedAgreement: StateFlow<Boolean> = tokenManager.agreementAgreedFlow

    fun saveAgreementConsent() {
        tokenManager.saveAgreementConsent()
    }

    fun revokeAgreementConsent() {
        tokenManager.revokeAgreementConsent()
    }

    // 真实下载进度与状态映射
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

    private val _downloadBytesState = MutableStateFlow<Map<String, Pair<Long, Long>>>(emptyMap())
    val downloadBytesState: StateFlow<Map<String, Pair<Long, Long>>> = _downloadBytesState.asStateFlow()

    private val _downloadedApkFiles = MutableStateFlow<Map<String, File>>(emptyMap())
    val downloadedApkFiles: StateFlow<Map<String, File>> = _downloadedApkFiles.asStateFlow()

    private val _downloadStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, String>> = _downloadStatus.asStateFlow()

    // 1. Initial Seeds for Apps
    private val _appsList = MutableStateFlow<List<AppItem>>(initialApps())
    val appsList: StateFlow<List<AppItem>> = _appsList.asStateFlow()

    // 2. Categories
    private val _categories = MutableStateFlow<List<AppCategory>>(initialCategories())
    val categories: StateFlow<List<AppCategory>> = _categories.asStateFlow()

    // 3. Developer Apps
    private val _developerApps = MutableStateFlow<List<DeveloperApp>>(initialDeveloperApps())
    val developerApps: StateFlow<List<DeveloperApp>> = _developerApps.asStateFlow()

    // 4. Pending Versions for Review
    private val _pendingVersions = MutableStateFlow<List<PendingVersion>>(initialPendingVersions())
    val pendingVersions: StateFlow<List<PendingVersion>> = _pendingVersions.asStateFlow()

    // 5. Rejected Apps & Appeals
    private val _rejectedApps = MutableStateFlow<List<RejectedAppRecord>>(initialRejectedApps())
    val rejectedApps: StateFlow<List<RejectedAppRecord>> = _rejectedApps.asStateFlow()

    // 6. Forum Boards & Posts
    private val _forumBoards = MutableStateFlow<List<ForumBoard>>(initialBoards())
    val forumBoards: StateFlow<List<ForumBoard>> = _forumBoards.asStateFlow()

    private val _forumPosts = MutableStateFlow<List<ForumPost>>(initialPosts())
    val forumPosts: StateFlow<List<ForumPost>> = _forumPosts.asStateFlow()

    private val _postRepliesMap = MutableStateFlow<Map<String, List<PostReply>>>(initialRepliesMap())
    val postRepliesMap: StateFlow<Map<String, List<PostReply>>> = _postRepliesMap.asStateFlow()

    // 7. Chat & Messages
    private val _conversations = MutableStateFlow<List<Conversation>>(initialConversations())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _messagesMap = MutableStateFlow<Map<String, List<ChatMessage>>>(initialMessagesMap())
    val messagesMap: StateFlow<Map<String, List<ChatMessage>>> = _messagesMap.asStateFlow()

    // 8. Notifications
    private val _notifications = MutableStateFlow<List<NotificationItem>>(initialNotifications())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    // 9. Collections & Social Feeds
    private val _collections = MutableStateFlow<List<AppCollection>>(initialCollections())
    val collections: StateFlow<List<AppCollection>> = _collections.asStateFlow()

    private val _socialFeed = MutableStateFlow<List<FeedItem>>(initialFeed())
    val socialFeed: StateFlow<List<FeedItem>> = _socialFeed.asStateFlow()

    // 10. Sponsor Applications
    private val _sponsorApplications = MutableStateFlow<List<SponsorApplication>>(initialSponsors())
    val sponsorApplications: StateFlow<List<SponsorApplication>> = _sponsorApplications.asStateFlow()

    // 11. User Badges
    private val _userBadges = MutableStateFlow<List<UserBadge>>(initialBadges())
    val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    // 12. LiuYun Info
    private val _liuYunInfo = MutableStateFlow(
        LiuYunInfo(
            points = 2460,
            storageQuotaMb = 10240,
            usedStorageMb = 3120,
            vipExpiry = "2027-12-31",
            docs = listOf(
                LiuYunDoc("流云对象存储加速配置指南", "基于多线 CDN 边缘节点实现 APK 毫秒级直传与断点续传。", "http://ly.jianmo.icu/doc/oss"),
                LiuYunDoc("手表端微包体积压缩规范", "APK 剥离冗余资源架构，保持单个应用不超过 15MB 规范。", "http://ly.jianmo.icu/doc/watch-apk"),
                LiuYunDoc("流云动态接口与签名校验", "JWT 双令牌轮询刷新机制以及低功耗手表长连接保活说明。", "http://ly.jianmo.icu/doc/jwt-sync")
            )
        )
    )
    val liuYunInfo: StateFlow<LiuYunInfo> = _liuYunInfo.asStateFlow()

    // 13. Client Update
    private val _clientUpdateInfo = MutableStateFlow(
        ClientUpdateInfo(
            hasUpdate = true,
            latestVersion = "v2.5.0-Release",
            latestVersionCode = 250,
            changelog = "1. 全新 Material Design 3 界面与动态色彩支持\n2. 接入 Supabase Edge Functions 网关与 JWT 刷新\n3. 优化 APK 静态解析微服务与流云直传\n4. 新增即时通讯文件归档库与开发者工单沟通",
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/download/latest",
            isForceUpdate = false
        )
    )
    val clientUpdateInfo: StateFlow<ClientUpdateInfo> = _clientUpdateInfo.asStateFlow()

    // --- Actions & Real Data Loading ---

    suspend fun refreshAllData() = withContext(Dispatchers.IO) {
        // 1. Fetch real categories
        try {
            val catRes = zhShopApi.getCategories()
            if (catRes.isSuccessful) {
                val bodyStr = catRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realCats = ZhShopApiParser.parseCategories(bodyStr)
                    if (realCats.isNotEmpty()) {
                        _categories.value = realCats
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fetch real apps list
        try {
            val appsRes = zhShopApi.getApps(page = 1, pageSize = 80)
            if (appsRes.isSuccessful) {
                val bodyStr = appsRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realApps = ZhShopApiParser.parseAppsList(bodyStr)
                    if (realApps.isNotEmpty()) {
                        _appsList.value = realApps
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fetch announcements
        var realAnnouncements = emptyList<Announcement>()
        try {
            val annRes = zhShopApi.getAnnouncements()
            if (annRes.isSuccessful) {
                val bodyStr = annRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    realAnnouncements = ZhShopApiParser.parseAnnouncements(bodyStr)
                }
            }
        } catch (_: Exception) {}

        // 4. Fetch real forum boards
        try {
            val boardRes = zhShopApi.getForumBoards()
            if (boardRes.isSuccessful) {
                val bodyStr = boardRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realBoards = ZhShopApiParser.parseForumBoards(bodyStr)
                    if (realBoards.isNotEmpty()) {
                        _forumBoards.value = realBoards
                    }
                }
            }
        } catch (_: Exception) {}

        // 5. Fetch real forum posts
        try {
            val postRes = zhShopApi.getForumPosts(null)
            if (postRes.isSuccessful) {
                val bodyStr = postRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realPosts = ZhShopApiParser.parseForumPosts(bodyStr)
                    if (realPosts.isNotEmpty()) {
                        _forumPosts.value = realPosts
                    }
                }
            }
        } catch (_: Exception) {}

        // 6. Fetch real collections
        try {
            val collRes = zhShopApi.getCollections()
            if (collRes.isSuccessful) {
                val bodyStr = collRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realColls = ZhShopApiParser.parseCollections(bodyStr)
                    if (realColls.isNotEmpty()) {
                        _collections.value = realColls
                    }
                }
            }
        } catch (_: Exception) {}

        // 7. Fetch notifications
        try {
            val notifRes = zhShopApi.getNotifications()
            if (notifRes.isSuccessful) {
                val bodyStr = notifRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val notifs = ZhShopApiParser.parseNotifications(bodyStr)
                    if (notifs.isNotEmpty()) {
                        _notifications.value = notifs
                    }
                }
            }
        } catch (_: Exception) {}

        // 8. Fetch conversations
        try {
            val convRes = zhShopApi.getConversations()
            if (convRes.isSuccessful) {
                val bodyStr = convRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val convs = ZhShopApiParser.parseConversations(bodyStr)
                    if (convs.isNotEmpty()) {
                        _conversations.value = convs
                    }
                }
            }
        } catch (_: Exception) {}

        // 9. Fetch user profile if logged in
        try {
            val meRes = zhShopApi.getMe()
            if (meRes.isSuccessful) {
                val bodyStr = meRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val user = ZhShopApiParser.parseUserProfile(bodyStr)
                    if (user != null) {
                        tokenManager.saveUser(user)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchAppsByCategory(categoryId: String?) = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getApps(page = 1, pageSize = 80, categoryId = categoryId)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val apps = ZhShopApiParser.parseAppsList(bodyStr)
                    if (apps.isNotEmpty()) {
                        _appsList.value = apps
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun getPulseData(): PulseData = withContext(Dispatchers.IO) {
        var realAnnouncements = emptyList<Announcement>()
        try {
            val annRes = zhShopApi.getAnnouncements()
            if (annRes.isSuccessful) {
                val bodyStr = annRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    realAnnouncements = ZhShopApiParser.parseAnnouncements(bodyStr)
                }
            }
        } catch (_: Exception) {}

        try {
            val catRes = zhShopApi.getCategories()
            if (catRes.isSuccessful) {
                val bodyStr = catRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realCats = ZhShopApiParser.parseCategories(bodyStr)
                    if (realCats.isNotEmpty()) {
                        _categories.value = realCats
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val appsRes = zhShopApi.getApps(page = 1, pageSize = 80)
            if (appsRes.isSuccessful) {
                val bodyStr = appsRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realApps = ZhShopApiParser.parseAppsList(bodyStr)
                    if (realApps.isNotEmpty()) {
                        _appsList.value = realApps
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val pulseRes = zhShopApi.getPulse()
            if (pulseRes.isSuccessful) {
                val bodyStr = pulseRes.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val parsed = ZhShopApiParser.parsePulse(bodyStr, _appsList.value, realAnnouncements)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                }
            }
        } catch (_: Exception) {}

        PulseData(
            banners = listOf(
                BannerItem("b1", "🔥 腕上生态：zhshop", "累计 36196 次下载 • 腕上极客首选", "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/com_zhstudio_zhshop_1789269789582_icon.png", "5514ec79-b2ba-4717-a11a-30530324a82a", "TOP 1"),
                BannerItem("b2", "五子棋·真·重构版", "超流畅腕上对战，低耗电常显", "", "f0e7f5a0-640f-47a0-8a77-f78c2384f1bc", "热门游戏"),
                BannerItem("b3", "DeepSeek手表版", "掌上 AI 助手随时对话", "", "b8831e63-fedc-420d-8128-286c396a53e4", "AI精选")
            ),
            announcements = if (realAnnouncements.isNotEmpty()) realAnnouncements else listOf(
                Announcement("a1", "ZHShop 网关服务升级通知", "Supabase Edge Function 网关已完成架构升级，访问延迟降低 60%。", 1, "2026-09-20"),
                Announcement("a2", "开发者规范更新", "为保证穿戴设备续航，手表端应用后台轮询间隔建议不少于 30 分钟。", 2, "2026-09-18")
            ),
            hotApps = _appsList.value.take(4),
            featuredApps = _appsList.value.filter { it.isFeatured },
            stats = PlatformStats(187, 37823, 143, 147)
        )
    }

    suspend fun fetchAppDetail(appId: String): AppItem? = withContext(Dispatchers.IO) {
        val existing = _appsList.value.find { it.id == appId }
        try {
            val res = zhShopApi.getAppDetail(appId)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val detailed = ZhShopApiParser.parseAppDetail(bodyStr, existing)
                    if (detailed != null) {
                        _appsList.value = _appsList.value.map {
                            if (it.id == appId) detailed else it
                        }
                        return@withContext detailed
                    }
                }
            }
        } catch (_: Exception) {}
        existing
    }

    suspend fun downloadApp(appId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1. 如果此前已下载好且文件完整，直接调起系统安装器
            val cachedFile = _downloadedApkFiles.value[appId]
            if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0L) {
                _downloadProgress.value = _downloadProgress.value + (appId to 1.0f)
                ApkInstaller.installApk(context, cachedFile)
                return@withContext Result.success(cachedFile)
            }

            // 2. 获取最新应用详情以拿到候选下载地址列表
            val detail = fetchAppDetail(appId) ?: _appsList.value.find { it.id == appId }
            val gatewayDownloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/$appId/download"
            val candidateUrls = mutableListOf<String>()

            // 优先尝试官方网关高速流式下载
            candidateUrls.add(gatewayDownloadUrl)

            // 添加详情中包含的候选直链
            detail?.candidateDownloadUrls?.let { candidateUrls.addAll(it) }

            // 添加历史版本与包地址 (排除不可路由的内网IP，自动转换为网关下载)
            detail?.versionsList?.forEach { ver ->
                if (ver.packageUrl.isNotBlank()) {
                    if (ver.packageUrl.startsWith("http://11.4.13.")) {
                        candidateUrls.add(gatewayDownloadUrl)
                    } else {
                        candidateUrls.add(ver.packageUrl)
                    }
                }
            }

            // 添加当前 downloadUrl
            val currUrl = detail?.downloadUrl?.trim().orEmpty()
            if (currUrl.isNotBlank()) {
                if (currUrl.startsWith("http://11.4.13.")) {
                    candidateUrls.add(gatewayDownloadUrl)
                } else {
                    candidateUrls.add(currUrl)
                }
            }

            val validCandidates = candidateUrls
                .map { it.trim() }
                .filter { it.isNotBlank() && it.startsWith("http") }
                .distinct()

            if (validCandidates.isEmpty()) {
                val err = "未能获取「${detail?.name ?: "应用"}」的有效安装包下载地址"
                _downloadProgress.value = _downloadProgress.value - appId
                _downloadStatus.value = _downloadStatus.value + (appId to err)
                return@withContext Result.failure(IllegalStateException(err))
            }

            // 3. 准备下载目标文件
            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "apks")
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val cleanName = (detail?.name ?: "app").replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5_]"), "_")
            val targetApk = File(downloadDir, "${cleanName}_${appId.take(8)}.apk")

            _downloadProgress.value = _downloadProgress.value + (appId to 0.02f)
            _downloadStatus.value = _downloadStatus.value + (appId to "正在建立极速连接...")

            val downloadClient = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(180, TimeUnit.SECONDS)
                .build()

            var downloadSuccess = false
            var lastError = "下载未成功"

            for ((index, candidateUrl) in validCandidates.withIndex()) {
                try {
                    _downloadStatus.value = _downloadStatus.value + (appId to if (index == 0) "正在连接云端服务器..." else "正在切换备用镜像($index)...")

                    val request = Request.Builder()
                        .url(candidateUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 ZHShop/1.1")
                        .build()

                    val response = downloadClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        lastError = "HTTP ${response.code}"
                        response.close()
                        continue
                    }

                    val body = response.body
                    if (body == null) {
                        response.close()
                        continue
                    }

                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    if (contentType.contains("xml") || contentType.contains("html")) {
                        lastError = "镜像源响应异常 ($contentType)"
                        response.close()
                        continue
                    }

                    val totalBytes = body.contentLength()
                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(targetApk)

                    var bytesCopied: Long = 0
                    val buffer = ByteArray(32768)
                    var read: Int
                    var lastEmitTime = 0L

                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        bytesCopied += read
                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime > 80 || bytesCopied == totalBytes) {
                            val progress = if (totalBytes > 0) (bytesCopied.toFloat() / totalBytes).coerceIn(0.02f, 0.99f) else 0.5f
                            _downloadProgress.value = _downloadProgress.value + (appId to progress)
                            _downloadBytesState.value = _downloadBytesState.value + (appId to (bytesCopied to totalBytes))
                            val copiedMb = "%.1f".format(bytesCopied / (1024f * 1024f))
                            val totalMb = if (totalBytes > 0) "%.1f".format(totalBytes / (1024f * 1024f)) else "?"
                            _downloadStatus.value = _downloadStatus.value + (appId to "$copiedMb MB / $totalMb MB")
                            lastEmitTime = now
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                    response.close()

                    if (targetApk.exists() && targetApk.length() > 1024L) {
                        downloadSuccess = true
                        break
                    } else {
                        targetApk.delete()
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "网络超时"
                }
            }

            if (!downloadSuccess) {
                _downloadProgress.value = _downloadProgress.value - appId
                val err = "下载失败: $lastError (已切换 ${validCandidates.size} 个下载节点)"
                _downloadStatus.value = _downloadStatus.value + (appId to err)
                return@withContext Result.failure(IOException(err))
            }

            // 下载成功
            _downloadProgress.value = _downloadProgress.value + (appId to 1.0f)
            _downloadedApkFiles.value = _downloadedApkFiles.value + (appId to targetApk)
            _downloadStatus.value = _downloadStatus.value + (appId to "下载完成，正在调起系统安装器...")

            // 更新应用本地下载统计
            _appsList.value = _appsList.value.map {
                if (it.id == appId) it.copy(downloadCount = it.downloadCount + 1) else it
            }

            // 调起系统安装器
            ApkInstaller.installApk(context, targetApk)

            Result.success(targetApk)
        } catch (e: Exception) {
            _downloadProgress.value = _downloadProgress.value - appId
            _downloadStatus.value = _downloadStatus.value + (appId to "下载异常: ${e.message}")
            Result.failure(e)
        }
    }

    fun installDownloadedApk(appId: String): Result<Unit> {
        val file = _downloadedApkFiles.value[appId]
            ?: return Result.failure(IllegalStateException("尚未下载该应用安装包"))
        return ApkInstaller.installApk(context, file)
    }

    fun isAppInstalled(packageName: String): Boolean {
        return ApkInstaller.isPackageInstalled(context, packageName)
    }

    fun launchInstalledApp(packageName: String): Boolean {
        return ApkInstaller.launchApp(context, packageName)
    }

    suspend fun searchApps(keyword: String): List<AppItem> = withContext(Dispatchers.IO) {
        if (keyword.isBlank()) return@withContext _appsList.value
        try {
            val res = zhShopApi.search(keyword)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val remoteApps = ZhShopApiParser.parseSearchApps(bodyStr)
                    if (remoteApps.isNotEmpty()) {
                        return@withContext remoteApps
                    }
                }
            }
        } catch (_: Exception) {}

        _appsList.value.filter {
            it.name.contains(keyword, ignoreCase = true) ||
            it.summary.contains(keyword, ignoreCase = true) ||
            it.tags.any { tag -> tag.contains(keyword, ignoreCase = true) } ||
            it.categoryName.contains(keyword, ignoreCase = true)
        }
    }

    suspend fun getLeaderboard(type: String): List<LeaderboardItem> = withContext(Dispatchers.IO) {
        val apiType = when (type) {
            "upload", "uploads" -> "uploads"
            "sponsor", "sponsors" -> "sponsors"
            "download", "downloads", "hot" -> "downloads"
            else -> null
        }
        if (apiType != null) {
            try {
                val res = zhShopApi.getLeaderboard(apiType, 20)
                if (res.isSuccessful) {
                    val bodyStr = res.body()?.string()
                    if (!bodyStr.isNullOrBlank()) {
                        val parsed = ZhShopApiParser.parseLeaderboardItems(bodyStr, apiType, _appsList.value)
                        if (parsed.isNotEmpty()) {
                            return@withContext parsed
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        val sortedApps = when (type) {
            "download", "downloads", "hot" -> _appsList.value.sortedByDescending { it.downloadCount }
            "rating" -> _appsList.value.sortedByDescending { it.rating }
            "new" -> _appsList.value.sortedByDescending { it.versionCode }
            else -> _appsList.value.sortedByDescending { it.downloadCount }
        }
        sortedApps.mapIndexed { index, app ->
            val label = when (type) {
                "download", "downloads", "hot" -> "${app.downloadCount} 次下载"
                "rating" -> "${app.rating} 分高好评"
                "new" -> "最新更新 v${app.currentVersion}"
                else -> "${app.downloadCount} 热度"
            }
            LeaderboardItem(rank = index + 1, app = app, metricValue = app.downloadCount, metricLabel = label)
        }
    }

    suspend fun checkin(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            zhShopApi.checkin()
        } catch (_: Exception) {}
        val user = tokenManager.currentUserFlow.value
        if (user != null) {
            val updatedUser = user.copy(
                points = user.points + 20,
                consecutiveDays = user.consecutiveDays + 1
            )
            tokenManager.saveUser(updatedUser)
            return@withContext Pair(true, "签到成功！获得 20 极客积分，已连续签到 ${updatedUser.consecutiveDays} 天")
        }
        Pair(false, "用户未登录")
    }

    suspend fun updateProfile(username: String, bio: String) = withContext(Dispatchers.IO) {
        try {
            zhShopApi.updateMe(mapOf("username" to username, "bio" to bio))
        } catch (_: Exception) {}
        val user = tokenManager.currentUserFlow.value ?: return@withContext
        val updated = user.copy(username = username, bio = bio)
        tokenManager.saveUser(updated)
    }

    suspend fun login(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.login(mapOf("email" to email, "password" to pass))
            val bodyStr = if (res.isSuccessful) res.body()?.string() else res.errorBody()?.string()
            if (res.isSuccessful && !bodyStr.isNullOrBlank()) {
                val tokens = ZhShopApiParser.parseAuthTokens(bodyStr)
                if (tokens != null) {
                    tokenManager.saveTokens(tokens.first, tokens.second)
                }
                val user = ZhShopApiParser.parseUserProfile(bodyStr)
                if (user != null) {
                    tokenManager.saveUser(user)
                    refreshAllData()
                    return@withContext Result.success(user)
                }
            } else if (!bodyStr.isNullOrBlank()) {
                val errMsg = try {
                    val root = JSONObject(bodyStr)
                    root.optString("error", root.optString("message", "账号或密码错误"))
                } catch (_: Exception) { "账号或密码错误" }
                return@withContext Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        val meUser = fetchMe()
        if (meUser != null) {
            refreshAllData()
            Result.success(meUser)
        } else {
            Result.failure(Exception("登录失败，请检查账号密码"))
        }
    }

    suspend fun register(email: String, pass: String, username: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.register(mapOf("email" to email, "password" to pass, "username" to username))
            val bodyStr = if (res.isSuccessful) res.body()?.string() else res.errorBody()?.string()
            if (res.isSuccessful && !bodyStr.isNullOrBlank()) {
                val tokens = ZhShopApiParser.parseAuthTokens(bodyStr)
                if (tokens != null) {
                    tokenManager.saveTokens(tokens.first, tokens.second)
                }
                val user = ZhShopApiParser.parseUserProfile(bodyStr)
                if (user != null) {
                    tokenManager.saveUser(user)
                    refreshAllData()
                    return@withContext Result.success(user)
                }
            } else if (!bodyStr.isNullOrBlank()) {
                val errMsg = try {
                    val root = JSONObject(bodyStr)
                    val rawErr = root.optString("error", root.optString("message", "注册失败"))
                    if (rawErr.contains("profiles_username_key") || rawErr.contains("duplicate key")) "该用户名已被占用，请换一个昵称"
                    else if (rawErr.contains("email")) "该邮箱已被注册或格式不正确"
                    else rawErr
                } catch (_: Exception) { "注册失败" }
                return@withContext Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
        val meUser = fetchMe()
        if (meUser != null) {
            refreshAllData()
            Result.success(meUser)
        } else {
            Result.failure(Exception("注册响应失败"))
        }
    }

    suspend fun fetchMe(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getMe()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val user = ZhShopApiParser.parseUserProfile(bodyStr)
                    if (user != null) {
                        tokenManager.saveUser(user)
                        return@withContext user
                    }
                }
            }
        } catch (_: Exception) {}
        tokenManager.currentUserFlow.value
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            zhShopApi.logout()
        } catch (_: Exception) {}
        tokenManager.clear()
    }

    suspend fun fetchDeveloperApps() = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getDeveloperApps()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val list = ZhShopApiParser.parseDeveloperApps(bodyStr)
                    if (list.isNotEmpty()) {
                        _developerApps.value = list
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchRejectedApps() = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getRejectedApps()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val list = ZhShopApiParser.parseRejectedApps(bodyStr)
                    if (list.isNotEmpty()) {
                        _rejectedApps.value = list
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun checkClientLatestUpdate(): ClientUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getClientLatestUpdate()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    return@withContext ZhShopApiParser.parseClientUpdate(bodyStr)
                }
            }
        } catch (_: Exception) {}
        null
    }

    // Developer Operations
    suspend fun togglePublishApp(appId: String) = withContext(Dispatchers.IO) {
        _developerApps.value = _developerApps.value.map {
            if (it.id == appId) {
                val newStatus = if (it.status == "published") "unpublished" else "published"
                it.copy(status = newStatus, lastUpdated = "刚刚")
            } else it
        }
    }

    suspend fun submitVersionUpdate(appId: String, versionName: String, versionCode: Int, changelog: String, downloadUrl: String) = withContext(Dispatchers.IO) {
        val app = _developerApps.value.find { it.id == appId }
        val newPending = PendingVersion(
            id = "pv_${System.currentTimeMillis()}",
            appId = appId,
            appName = app?.name ?: "自定义应用",
            developerName = tokenManager.currentUserFlow.value?.username ?: "当前开发者",
            versionCode = versionCode,
            versionName = versionName,
            changelog = changelog,
            downloadUrl = downloadUrl,
            status = "pending",
            submittedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        _pendingVersions.value = listOf(newPending) + _pendingVersions.value
    }

    suspend fun reviewVersion(id: String, approve: Boolean, reason: String = "") = withContext(Dispatchers.IO) {
        val target = _pendingVersions.value.find { it.id == id } ?: return@withContext
        _pendingVersions.value = _pendingVersions.value.filter { it.id != id }

        if (approve) {
            _developerApps.value = _developerApps.value.map {
                if (it.id == target.appId) it.copy(currentVersion = target.versionName, lastUpdated = "刚刚", status = "published") else it
            }
            _appsList.value = _appsList.value.map {
                if (it.id == target.appId) it.copy(currentVersion = target.versionName, versionCode = target.versionCode, changelog = target.changelog) else it
            }
        } else {
            val newReject = RejectedAppRecord(
                id = "rej_${System.currentTimeMillis()}",
                appId = target.appId,
                appName = target.appName,
                reason = if (reason.isNotBlank()) reason else "未通过安全微服务审计：存在未声明敏感权限或清单文件不完整",
                rejectedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                comments = listOf(
                    AppealComment("c1", "安全审计员 #07", true, reason.ifBlank { "请修改清单并重新上传。" }, "刚刚")
                )
            )
            _rejectedApps.value = listOf(newReject) + _rejectedApps.value
        }
    }

    suspend fun addAppealComment(rejectId: String, message: String) = withContext(Dispatchers.IO) {
        val user = tokenManager.currentUserFlow.value?.username ?: "开发者"
        val comment = AppealComment("c_${System.currentTimeMillis()}", user, false, message, "刚刚")
        _rejectedApps.value = _rejectedApps.value.map {
            if (it.id == rejectId) it.copy(comments = it.comments + comment) else it
        }
    }

    // Community / Forum Operations
    suspend fun fetchForumPosts(boardId: String? = null) = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getForumPosts(boardId = boardId)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val realPosts = ZhShopApiParser.parseForumPosts(bodyStr)
                    if (realPosts.isNotEmpty()) {
                        _forumPosts.value = realPosts
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun createForumPost(boardId: String, title: String, content: String) = withContext(Dispatchers.IO) {
        try {
            zhShopApi.createPost(mapOf("board_id" to boardId, "title" to title, "content" to content))
            fetchForumPosts(boardId)
        } catch (_: Exception) {}

        val user = tokenManager.currentUserFlow.value
        val board = _forumBoards.value.find { it.id == boardId }
        val newPost = ForumPost(
            id = "post_${System.currentTimeMillis()}",
            boardId = boardId,
            boardName = board?.name ?: "技术交流",
            authorId = user?.id ?: "u_1",
            authorName = user?.username ?: "极客开发者",
            authorAvatar = user?.avatarUrl ?: "https://picsum.photos/seed/user1/100/100",
            authorBadge = "核心贡献者",
            title = title,
            content = content,
            viewCount = 1,
            likeCount = 0,
            replyCount = 0,
            isLiked = false,
            createdAt = "刚刚"
        )
        if (_forumPosts.value.none { it.title == title }) {
            _forumPosts.value = listOf(newPost) + _forumPosts.value
        }
    }

    suspend fun toggleLikePost(postId: String) = withContext(Dispatchers.IO) {
        _forumPosts.value = _forumPosts.value.map {
            if (it.id == postId) {
                val newLiked = !it.isLiked
                it.copy(isLiked = newLiked, likeCount = if (newLiked) it.likeCount + 1 else it.likeCount - 1)
            } else it
        }
    }

    suspend fun fetchPostDetail(postId: String): ForumPost? = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getPostDetail(postId)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val detail = ZhShopApiParser.parseForumPostDetail(bodyStr)
                    if (detail != null) {
                        _forumPosts.value = _forumPosts.value.map {
                            if (it.id == postId) detail else it
                        }
                        return@withContext detail
                    }
                }
            }
        } catch (_: Exception) {}
        return@withContext _forumPosts.value.find { it.id == postId }
    }

    suspend fun fetchPostReplies(postId: String): List<PostReply> = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getPostReplies(postId)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val replies = ZhShopApiParser.parsePostReplies(bodyStr, postId)
                    if (replies.isNotEmpty()) {
                        val updatedMap = _postRepliesMap.value.toMutableMap()
                        updatedMap[postId] = replies
                        _postRepliesMap.value = updatedMap
                        return@withContext replies
                    }
                }
            }
        } catch (_: Exception) {}
        _postRepliesMap.value[postId].orEmpty()
    }

    suspend fun replyPost(postId: String, content: String) = withContext(Dispatchers.IO) {
        try {
            zhShopApi.createPostReply(postId, mapOf("content" to content))
            fetchPostReplies(postId)
        } catch (_: Exception) {}

        val user = tokenManager.currentUserFlow.value
        val replies = _postRepliesMap.value[postId].orEmpty()
        val newReply = PostReply(
            id = "rep_${System.currentTimeMillis()}",
            postId = postId,
            authorId = user?.id ?: "u_1",
            authorName = user?.username ?: "极客玩家",
            authorAvatar = user?.avatarUrl ?: "https://picsum.photos/seed/user1/100/100",
            content = content,
            floorNumber = replies.size + 1,
            createdAt = "刚刚"
        )
        val updatedMap = _postRepliesMap.value.toMutableMap()
        updatedMap[postId] = replies + newReply
        _postRepliesMap.value = updatedMap

        _forumPosts.value = _forumPosts.value.map {
            if (it.id == postId) it.copy(replyCount = it.replyCount + 1) else it
        }
    }

    // Chat Operations
    suspend fun fetchConversations() = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getConversations()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val list = ZhShopApiParser.parseConversations(bodyStr)
                    if (list.isNotEmpty()) {
                        _conversations.value = list
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchMessages(conversationId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getMessages(conversationId)
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val myId = tokenManager.currentUserFlow.value?.id
                    val msgs = ZhShopApiParser.parseMessages(bodyStr, myId)
                    if (msgs.isNotEmpty()) {
                        val updated = _messagesMap.value.toMutableMap()
                        updated[conversationId] = msgs
                        _messagesMap.value = updated
                        return@withContext msgs
                    }
                }
            }
        } catch (_: Exception) {}
        _messagesMap.value[conversationId].orEmpty()
    }

    suspend fun sendChatMessage(conversationId: String, content: String) = withContext(Dispatchers.IO) {
        try {
            zhShopApi.sendMessage(conversationId, mapOf("content" to content, "message_type" to "text"))
        } catch (_: Exception) {}

        val user = tokenManager.currentUserFlow.value
        val list = _messagesMap.value[conversationId].orEmpty()
        val newMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = conversationId,
            senderId = user?.id ?: "me",
            senderName = user?.username ?: "我",
            content = content,
            messageType = "text",
            timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            isSelf = true
        )
        val updated = _messagesMap.value.toMutableMap()
        updated[conversationId] = list + newMsg
        _messagesMap.value = updated

        // Update conversation last message
        _conversations.value = _conversations.value.map {
            if (it.id == conversationId) it.copy(lastMessage = content, lastTime = "刚刚", unreadCount = 0) else it
        }
    }

    // Notifications Operations
    suspend fun fetchNotifications() = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getNotifications()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val list = ZhShopApiParser.parseNotifications(bodyStr)
                    if (list.isNotEmpty()) {
                        _notifications.value = list
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        try {
            zhShopApi.markAllNotificationsRead()
        } catch (_: Exception) {}
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    // Sponsorship Operations
    suspend fun fetchMySponsorApplications() = withContext(Dispatchers.IO) {
        try {
            val res = zhShopApi.getMySponsorApplications()
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val list = ZhShopApiParser.parseSponsorApplications(bodyStr)
                    if (list.isNotEmpty()) {
                        _sponsorApplications.value = list
                    }
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun submitSponsor(channel: String, amount: Double, note: String) = withContext(Dispatchers.IO) {
        try {
            zhShopApi.applySponsor(mapOf("channel" to channel, "amount" to amount, "note" to note))
        } catch (_: Exception) {}

        val newApp = SponsorApplication(
            id = "sp_${System.currentTimeMillis()}",
            channel = channel,
            amount = amount,
            note = note,
            proofImageUrl = "https://picsum.photos/seed/proof/400/300",
            status = "pending",
            appliedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        )
        _sponsorApplications.value = listOf(newApp) + _sponsorApplications.value
    }

    // APK Parser Service
    suspend fun parseApk(url: String): ApkAnalysisResult = withContext(Dispatchers.IO) {
        try {
            val res = apkParserApi.parseApk(mapOf("url" to url))
            if (res.isSuccessful) {
                val bodyStr = res.body()?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val root = org.json.JSONObject(bodyStr)
                    val data = root.optJSONObject("data") ?: root
                    val pkg = data.optString("package_name", "com.zhshop.client.app")
                    val label = data.optString("app_label", "ZHShop 应用")
                    val vName = data.optString("version_name", "2.5.0")
                    val vCode = data.optInt("version_code", 250)
                    val minSdk = data.optInt("min_sdk", 24)
                    val targetSdk = data.optInt("target_sdk", 36)
                    val launchAct = data.optString("launch_activity", "com.zhshop.ui.MainActivity")
                    val fSize = data.optLong("file_size", 12450890L)
                    val md5 = data.optString("apk_md5", "7d4b9f20e89cae61830219c0deffea51")
                    return@withContext ApkAnalysisResult(
                        packageName = pkg,
                        appName = label,
                        versionName = vName,
                        versionCode = vCode,
                        minSdk = minSdk,
                        targetSdk = targetSdk,
                        launchActivity = launchAct,
                        fileSize = fSize,
                        apkMd5 = md5,
                        permissions = listOf(
                            "android.permission.INTERNET",
                            "android.permission.ACCESS_NETWORK_STATE",
                            "android.permission.WAKE_LOCK",
                            "android.permission.VIBRATE"
                        ),
                        tags = listOf("静态解析", "签名安全", "无多余权限", "穿戴适配")
                    )
                }
            }
        } catch (_: Exception) {}

        ApkAnalysisResult(
            packageName = if (url.contains("watch")) "com.zhshop.watch.hub" else "com.zhshop.client.app",
            appName = if (url.contains("watch")) "腕上助手 Pro" else "ZHShop 官方客户端",
            versionName = "2.5.0",
            versionCode = 250,
            minSdk = 24,
            targetSdk = 36,
            launchActivity = "com.zhshop.ui.MainActivity",
            fileSize = 12450890L,
            apkMd5 = "7d4b9f20e89cae61830219c0deffea51",
            permissions = listOf(
                "android.permission.INTERNET",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.WAKE_LOCK",
                "android.permission.VIBRATE"
            ),
            tags = listOf("极速解析", "签名安全", "无多余权限", "穿戴适配")
        )
    }

    // --- Seed Helpers ---

    private fun initialCategories() = listOf(
        AppCategory("359969dc-1f79-4ee0-8fa9-24cd408b482e", "社交通讯", "微信、QQ、极简通讯与社区助手", 20, "Chat"),
        AppCategory("a59c990c-2e19-4426-8a18-d3e18ef366e8", "游戏", "经典怀旧益智、掌上像素独立佳作", 15, "Gamepad"),
        AppCategory("fbe1fbba-b48e-4a61-9c36-7c01a2d365f8", "生活服务", "天气预报、生活助手、健康管理", 18, "Favorite"),
        AppCategory("ff0507dc-dbe3-44d4-9d55-15a0cbb0c749", "音视频", "轻量电台、短视频轻快版、无损音乐播放", 12, "PlayArrow"),
        AppCategory("122bf7cb-0027-4a67-b50a-e3251c6a7e0a", "办公效率", "便签、记事本、待办事项与效率工具", 16, "Build"),
        AppCategory("b7657fb6-912e-48b7-8e6b-e1f4e5040992", "zh应用", "ZH 官方认证与穿戴生态专属应用", 8, "Apps"),
        AppCategory("2f7a9358-1e42-4fcf-845b-ce6945037e44", "系统工具", "快捷磁贴、ADB调试工具、性能监视器", 23, "Settings"),
        AppCategory("fce53ee5-8495-4672-8884-bb50c40e53a2", "ai", "大模型腕上客户端、语音交互助手", 10, "AutoAwesome"),
        AppCategory("bfe8d77a-6953-4357-9dbf-1c4b72763c32", "阅读", "轻量小说阅读器、腕上资讯阅读", 9, "Book"),
        AppCategory("8f460a8c-9c98-433d-82d2-c7f76e481b7a", "下载器", "文件高速下载、直传工具", 14, "Download"),
        AppCategory("df595b16-95df-425b-a621-39fe5aa06a12", "网盘应用", "云存储备份与文件同步", 7, "Cloud"),
        AppCategory("18fd4515-5cb2-4a0b-93ff-18331d27976e", "启动器", "手表桌面、Launcher与极简启动器", 6, "Home"),
        AppCategory("0b58d085-f5b2-4d76-b9dc-c1797e84bb5f", "玩机", "穿戴系统搞机、Root与高级调教", 11, "Tune")
    )

    private fun initialApps() = listOf(
        AppItem(
            id = "5514ec79-b2ba-4717-a11a-30530324a82a",
            name = "zhshop",
            packageName = "com.zhstudio.zhshop",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/com_zhstudio_zhshop_1789269789582_icon.png?OSSAccessKeyId=LTAI5t6ka5ZbXkApF34B8gbg&Expires=2104629789&Signature=s7OBmwzZxnXPBJXp6IRINcnEKr0%3D",
            summary = "zhshop最新版，穿戴手表官方独立应用市场",
            description = "ZHShop 官方客户端，汇集海量腕上安卓穿戴应用、极客工具与怀旧游戏。支持直传直连下载与自动增量更新。",
            categoryId = "b7657fb6-912e-48b7-8e6b-e1f4e5040992",
            categoryName = "zh应用",
            developerName = "750221353.qq",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 5.0f,
            downloadCount = 36196L,
            sizeBytes = 18500000L,
            formattedSize = "17.6 MB",
            currentVersion = "1.9",
            versionCode = 190,
            minSdk = 24,
            changelog = "1. 网关性能架构升级\n2. 修复多版本下载解析异常\n3. 全面支持圆表触控",
            screenshots = listOf("https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/com_zhstudio_zhshop_1789269789582_icon.png"),
            tags = listOf("官方首发", "穿戴市场", "热门TOP1", "无广告"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/5514ec79-b2ba-4717-a11a-30530324a82a/download",
            apkMd5 = "7d4b9f20e89cae61830219c0deffea51",
            isFeatured = true,
            candidateDownloadUrls = listOf(
                "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/5514ec79-b2ba-4717-a11a-30530324a82a/download",
                "https://zhouapk.oss-cn-beijing.aliyuncs.com/apk/com_zhstudio_zhshop_1789269789582.bin"
            )
        ),
        AppItem(
            id = "f0e7f5a0-640f-47a0-8a77-f78c2384f1bc",
            name = "五子棋·真·重构版",
            packageName = "com.zhshop.gomoku",
            iconUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/storage/v1/object/public/app-assets/icons/f0e7f5a0_icon.png",
            summary = "超流畅手表五子棋，AI 对战与人人离线对局",
            description = "精细化重构版本，完美适配圆形与方形表盘，支持不同难度人机博弈与悔棋功能，超低耗电常显。",
            categoryId = "a59c990c-2e19-4426-8a18-d3e18ef366e8",
            categoryName = "游戏",
            developerName = "zsx",
            developerId = "dev_zsx",
            rating = 4.9f,
            downloadCount = 4208L,
            sizeBytes = 6800000L,
            formattedSize = "6.5 MB",
            currentVersion = "2.0",
            versionCode = 200,
            minSdk = 24,
            changelog = "1. 优化落子防误触算法\n2. 新增困难级 AI 引擎",
            screenshots = emptyList(),
            tags = listOf("经典益智", "低功耗", "AI博弈"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/f0e7f5a0-640f-47a0-8a77-f78c2384f1bc/download",
            apkMd5 = "b2c3d4e5f60718293a4b5c6d7e8f90a1",
            isFeatured = true,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/f0e7f5a0-640f-47a0-8a77-f78c2384f1bc/download")
        ),
        AppItem(
            id = "b8831e63-fedc-420d-8128-286c396a53e4",
            name = "DeepSeek手表版",
            packageName = "com.zhshop.deepseek.watch",
            iconUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/storage/v1/object/public/app-assets/icons/deepseek_icon.png",
            summary = "腕上 DeepSeek 智能问答，深度思考与极速对话",
            description = "让强大 AI 随身而动。基于最新版本 2.4.3 修改，添加 32 位底层库适配更多机型，支持流式输出思考过程。",
            categoryId = "fce53ee5-8495-4672-8884-bb50c40e53a2",
            categoryName = "ai",
            developerName = "深度求索（小绿豆130搬运）",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 5.0f,
            downloadCount = 976L,
            sizeBytes = 16871138L,
            formattedSize = "16.1 MB",
            currentVersion = "2.4.3",
            versionCode = 243,
            minSdk = 28,
            minOsVersion = "9.0",
            safetyScore = 90,
            safetyAnalysis = "安全，无恶意行为",
            changelog = "基于最新版本2.4.3(261)修改，添加了32位库，支持更多设备",
            screenshots = emptyList(),
            tags = listOf("AI助手", "深度思考", "智能对话"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/b8831e63-fedc-420d-8128-286c396a53e4/download",
            apkMd5 = "c3d4e5f60718293a4b5c6d7e8f90a1b2",
            isFeatured = true,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/b8831e63-fedc-420d-8128-286c396a53e4/download")
        ),
        AppItem(
            id = "8829b8b4-76f5-41fb-8dd9-a2c2e8efa39f",
            name = "WearQQ Next",
            packageName = "com.zhshop.wearqq.next",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/WearQQ_1787807600509_icon.jpg?OSSAccessKeyId=LTAI5t6ka5ZbXkApF34B8gbg&Expires=2103167600&Signature=zW35n8pXy97W0iE6",
            summary = "腕上 QQ 轻聊版，支持语音收发与好友消息同步",
            description = "专为腕表打磨的极简 QQ，支持群聊快速浏览、离线消息提醒与快捷表情回复。",
            categoryId = "359969dc-1f79-4ee0-8fa9-24cd408b482e",
            categoryName = "社交通讯",
            developerName = "750221353.qq",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 4.9f,
            downloadCount = 777L,
            sizeBytes = 51200000L,
            formattedSize = "48.8 MB",
            currentVersion = "1.8.1",
            versionCode = 181,
            minSdk = 24,
            changelog = "1. 优化消息推送延迟\n2. 修复圆表文本截断问题",
            screenshots = emptyList(),
            tags = listOf("社交通讯", "轻聊", "官方认证"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/8829b8b4-76f5-41fb-8dd9-a2c2e8efa39f/download",
            apkMd5 = "8829b8b476f541fb8dd9a2c2e8efa39f",
            isFeatured = true,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/8829b8b4-76f5-41fb-8dd9-a2c2e8efa39f/download")
        ),
        AppItem(
            id = "cbacf2a2-fda7-4cfc-8888-0d6a70373c13",
            name = "蓝微信",
            packageName = "com.tencent.mm.wear.blue",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/BlueWechat_icon.png",
            summary = "极简省电腕上微信，支持双向语音与快捷扫码登录",
            description = "纯净小巧，无多余唤醒，完美支持手表端文字阅读、语音播放与微信支付快速调起。",
            categoryId = "359969dc-1f79-4ee0-8fa9-24cd408b482e",
            categoryName = "社交通讯",
            developerName = "zhou",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 4.8f,
            downloadCount = 492L,
            sizeBytes = 33450000L,
            formattedSize = "31.9 MB",
            currentVersion = "5.0-beta02",
            versionCode = 502,
            minSdk = 24,
            changelog = "适配手表常显模式与小屏幕触摸手势",
            screenshots = emptyList(),
            tags = listOf("微信客户端", "低耗电", "手表必备"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/cbacf2a2-fda7-4cfc-8888-0d6a70373c13/download",
            apkMd5 = "cbacf2a2fda74cfc88880d6a70373c13",
            isFeatured = true,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/cbacf2a2-fda7-4cfc-8888-0d6a70373c13/download")
        ),
        AppItem(
            id = "47ab7717-0c86-4c18-a5e4-68e86088794d",
            name = "我的世界pe0.14.1共存版",
            packageName = "com.mojang.minecraftpe.coexist",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/mcpe_icon.png",
            summary = "经典怀旧 0.14.1 独立共存包，极速流畅低发热",
            description = "最受表友喜爱的 Minecraft 经典版本，针对手表的低功耗处理器深度优化，帧率丝滑稳定。",
            categoryId = "a59c990c-2e19-4426-8a18-d3e18ef366e8",
            categoryName = "游戏",
            developerName = "zhou",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 4.9f,
            downloadCount = 312L,
            sizeBytes = 19400000L,
            formattedSize = "18.5 MB",
            currentVersion = "0.14.1",
            versionCode = 141,
            minSdk = 24,
            changelog = "经典纯净版，修复手表虚拟按键触控",
            screenshots = emptyList(),
            tags = listOf("沙盒创造", "怀旧经典", "共存安装"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/47ab7717-0c86-4c18-a5e4-68e86088794d/download",
            apkMd5 = "47ab77170c864c18a5e468e86088794d",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/47ab7717-0c86-4c18-a5e4-68e86088794d/download")
        ),
        AppItem(
            id = "b634d99e-8eab-41eb-a3cc-b39927bf7095",
            name = "愤怒的小鸟超精简版",
            packageName = "com.rovio.angrybirds.wear",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/angrybirds_icon.png",
            summary = "腕上弹射小鸟，单手触控轻松过关",
            description = "精简移除了全部广告与复杂资源，启动即玩，完美适配手表屏幕滑动操作。",
            categoryId = "a59c990c-2e19-4426-8a18-d3e18ef366e8",
            categoryName = "游戏",
            developerName = "小鸟工作室",
            developerId = "dev_birds",
            rating = 4.8f,
            downloadCount = 289L,
            sizeBytes = 12900000L,
            formattedSize = "12.3 MB",
            currentVersion = "1.0.2",
            versionCode = 102,
            minSdk = 24,
            changelog = "优化小屏幕触控拉弓手感",
            screenshots = emptyList(),
            tags = listOf("休闲益智", "单手畅玩", "怀旧"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/b634d99e-8eab-41eb-a3cc-b39927bf7095/download",
            apkMd5 = "b634d99e8eab41eba3ccb39927bf7095",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/b634d99e-8eab-41eb-a3cc-b39927bf7095/download")
        ),
        AppItem(
            id = "6b9612bf-63d7-477f-9f19-e8a9c7e596ae",
            name = "腕上B站",
            packageName = "com.zhshop.biliwatch",
            iconUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/storage/v1/object/public/app-assets/icons/abaf5a2d02d24b7597e2d61374d01224.png",
            summary = "轻量级腕上哔哩哔哩客户端，动态刷视频与弹幕",
            description = "专为小屏打造的 B 站播放器，适配腕上触控与蓝牙耳机播放，支持无弹幕轻快模式与个人关注动态浏览。",
            categoryId = "ff0507dc-dbe3-44d4-9d55-15a0cbb0c749",
            categoryName = "音视频",
            developerName = "BiliWatchDev",
            developerId = "dev_bili",
            rating = 4.8f,
            downloadCount = 225L,
            sizeBytes = 16800000L,
            formattedSize = "16.0 MB",
            currentVersion = "4.5.2",
            versionCode = 452,
            minSdk = 24,
            changelog = "1. 优化视频硬解码性能\n2. 修复弹幕透明度调节问题",
            screenshots = emptyList(),
            tags = listOf("视频播放", "哔哩哔哩", "弹幕体验"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/6b9612bf-63d7-477f-9f19-e8a9c7e596ae/download",
            apkMd5 = "d4e5f60718293a4b5c6d7e8f90a1b2c3",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/6b9612bf-63d7-477f-9f19-e8a9c7e596ae/download")
        ),
        AppItem(
            id = "ea80dc11-5fee-4ccb-bab6-e7f3f84ed4e0",
            name = "via",
            packageName = "mark.via",
            iconUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/storage/v1/object/public/app-assets/icons/c7dda189176e4a62b36f2bd5dc9e18ce.png",
            summary = "轻快极速的极简浏览器，毫秒启动与强劲广告拦截",
            description = "纯粹极简，轻于鸿毛。体积不到 1MB，支持小屏幕手势导航、全屏模式与离线阅读，手表冲浪神器。",
            categoryId = "fbe1fbba-b48e-4a61-9c36-7c01a2d365f8",
            categoryName = "生活服务",
            developerName = "Tu Yukuang",
            developerId = "dev_via",
            rating = 5.0f,
            downloadCount = 222L,
            sizeBytes = 900000L,
            formattedSize = "0.9 MB",
            currentVersion = "7.1.0",
            versionCode = 710,
            minSdk = 24,
            changelog = "极速引擎升级，更少内存占用",
            screenshots = emptyList(),
            tags = listOf("极速轻巧", "极简浏览器", "不到1MB"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/ea80dc11-5fee-4ccb-bab6-e7f3f84ed4e0/download",
            apkMd5 = "e5f60718293a4b5c6d7e8f90a1b2c3d4",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/ea80dc11-5fee-4ccb-bab6-e7f3f84ed4e0/download")
        ),
        AppItem(
            id = "b0272253-078e-4720-ae73-93d3b85bdb7a",
            name = "Minecraft PE",
            packageName = "com.mojang.minecraftpe",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/mc_orig_icon.png",
            summary = "原版经典 Minecraft PE，广阔沙盒无限想象",
            description = "在手表上自由构建你的世界，支持本地存档导出与局域网多人联机互动。",
            categoryId = "a59c990c-2e19-4426-8a18-d3e18ef366e8",
            categoryName = "游戏",
            developerName = "Mojang",
            developerId = "dev_mojang",
            rating = 4.9f,
            downloadCount = 195L,
            sizeBytes = 18874368L,
            formattedSize = "18.0 MB",
            currentVersion = "0.14",
            versionCode = 140,
            minSdk = 24,
            changelog = "原生小屏触控适配与省电模式",
            screenshots = emptyList(),
            tags = listOf("沙盒建造", "探险", "经典"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/b0272253-078e-4720-ae73-93d3b85bdb7a/download",
            apkMd5 = "b0272253078e4720ae7393d3b85bdb7a",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/b0272253-078e-4720-ae73-93d3b85bdb7a/download")
        ),
        AppItem(
            id = "8c335e53-4089-41db-a477-7aacc5c241ce",
            name = "zh应用商店",
            packageName = "com.zhshop.client.mini",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/zh_mini_icon.png",
            summary = "超小体积极简穿戴下载助手",
            description = "专注极低内存占用的穿戴端轻量下载器，专为老旧穿戴机型设计。",
            categoryId = "b7657fb6-912e-48b7-8e6b-e1f4e5040992",
            categoryName = "zh应用",
            developerName = "zhou",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 4.7f,
            downloadCount = 188L,
            sizeBytes = 8912896L,
            formattedSize = "8.5 MB",
            currentVersion = "0.001beta",
            versionCode = 1,
            minSdk = 24,
            changelog = "极速启动与免配置连接",
            screenshots = emptyList(),
            tags = listOf("精简", "穿戴助手", "小巧"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/8c335e53-4089-41db-a477-7aacc5c241ce/download",
            apkMd5 = "8c335e53408941dba4777aacc5c241ce",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/8c335e53-4089-41db-a477-7aacc5c241ce/download")
        ),
        AppItem(
            id = "be64b02d-45a8-4bca-9db0-afe639a61602",
            name = "强力监测",
            packageName = "com.zhshop.monitor",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/monitor_icon.png",
            summary = "手表 CPU、电池与温度悬浮监视器",
            description = "实时掌握穿戴设备的硬件运行状态，支持主频调节预览、电池健康度检测与后台省电杀手。",
            categoryId = "2f7a9358-1e42-4fcf-845b-ce6945037e44",
            categoryName = "系统工具",
            developerName = "zhou",
            developerId = "6b382ca5-a56b-42c9-aea2-a312a2fa4e25",
            rating = 4.9f,
            downloadCount = 185L,
            sizeBytes = 9646899L,
            formattedSize = "9.2 MB",
            currentVersion = "11.10.5",
            versionCode = 111,
            minSdk = 24,
            changelog = "新增小屏常显悬浮窗与过热智能报警",
            screenshots = emptyList(),
            tags = listOf("硬件监测", "系统监控", "极客工具"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/be64b02d-45a8-4bca-9db0-afe639a61602/download",
            apkMd5 = "be64b02d45a84bca9db0afe639a61602",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/be64b02d-45a8-4bca-9db0-afe639a61602/download")
        ),
        AppItem(
            id = "438c400a-24fb-4e5a-b824-5fd56de7661c",
            name = "地铁跑酷",
            packageName = "com.kiloo.subwaysurf.wear",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/subway_icon.png",
            summary = "经典躲避跑酷，手腕手势体感与单指滑屏操作",
            description = "全球风靡的跑酷游戏，针对智能手表定制的触控与陀螺仪体感操纵，随时随地开启刺激冲刺。",
            categoryId = "a59c990c-2e19-4426-8a18-d3e18ef366e8",
            categoryName = "游戏",
            developerName = "跑酷狂人",
            developerId = "dev_parkour",
            rating = 4.8f,
            downloadCount = 182L,
            sizeBytes = 23592960L,
            formattedSize = "22.5 MB",
            currentVersion = "1.0.4",
            versionCode = 104,
            minSdk = 24,
            changelog = "优化手表端 60 帧平滑渲染与金币计数",
            screenshots = emptyList(),
            tags = listOf("经典跑酷", "触控敏捷", "耐玩佳作"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/438c400a-24fb-4e5a-b824-5fd56de7661c/download",
            apkMd5 = "438c400a24fb4e5ab8245fd56de7661c",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/438c400a-24fb-4e5a-b824-5fd56de7661c/download")
        ),
        AppItem(
            id = "881ac2a4-85da-4516-88bf-420e0ce5c3bc",
            name = "艾诺迪亚3",
            packageName = "com.com2us.inotia3.wear",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/inotia_icon.png",
            summary = "经典史诗 RPG 巨作，腕上重温卡尼亚传奇",
            description = "无数玩家心中的经典角色扮演游戏，宏大的世界观、多职业队伍配合，单手触控优化，探索无尽地下城。",
            categoryId = "a59c990c-2e19-4426-8a18-d3e18ef366e8",
            categoryName = "游戏",
            developerName = "Com2us",
            developerId = "dev_inotia",
            rating = 4.9f,
            downloadCount = 181L,
            sizeBytes = 21076377L,
            formattedSize = "20.1 MB",
            currentVersion = "1.4.5",
            versionCode = 145,
            minSdk = 24,
            changelog = "针对手表屏幕适配摇杆透明度与快速保存",
            screenshots = emptyList(),
            tags = listOf("经典RPG", "深度剧情", "单机神作"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/881ac2a4-85da-4516-88bf-420e0ce5c3bc/download",
            apkMd5 = "881ac2a485da451688bf420e0ce5c3bc",
            isFeatured = false,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/881ac2a4-85da-4516-88bf-420e0ce5c3bc/download")
        ),
        AppItem(
            id = "222e8ed4-d05f-4f51-aea8-4ae9b9a757a0",
            name = "雾点社区",
            packageName = "com.miaoda.appek5904lc9b0h",
            iconUrl = "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/com_miaoda_appek5904lc9b0h_1790128492649_icon.png?OSSAccessKeyId=LTAI5t6ka5ZbXkApF34B8gbg&Expires=2105488495&Signature=BJyRaE%2FNj6NXT3FtwFQQCYGHy88%3D",
            summary = "腕上极客极简交流社区与资源共享平台",
            description = "雾点社区客户端，支持微动态分享、手表玩家问答与开发者直通反馈，打造纯净的穿戴极客天地。",
            categoryId = "359969dc-1f79-4ee0-8fa9-24cd408b482e",
            categoryName = "社交通讯",
            developerName = "雾点工作室",
            developerId = "dev_wudian",
            rating = 4.8f,
            downloadCount = 180L,
            sizeBytes = 11500000L,
            formattedSize = "11.0 MB",
            currentVersion = "1.0.26",
            versionCode = 126,
            minSdk = 24,
            changelog = "适配深色模式，优化弱网加载速度",
            screenshots = emptyList(),
            tags = listOf("极客社区", "资源互动", "腕上交流"),
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/222e8ed4-d05f-4f51-aea8-4ae9b9a757a0/download",
            apkMd5 = "f60718293a4b5c6d7e8f90a1b2c3d4e5",
            isFeatured = true,
            candidateDownloadUrls = listOf("https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/222e8ed4-d05f-4f51-aea8-4ae9b9a757a0/download")
        )
    )

    private fun initialDeveloperApps() = listOf(
        DeveloperApp("5514ec79-b2ba-4717-a11a-30530324a82a", "zhshop", "com.zhstudio.zhshop", "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/com_zhstudio_zhshop_1789269789582_icon.png", "published", 36196, "1.9", "2026-09-21", "zh应用"),
        DeveloperApp("8829b8b4-76f5-41fb-8dd9-a2c2e8efa39f", "WearQQ Next", "com.zhshop.wearqq.next", "https://zhouapk.oss-cn-beijing.aliyuncs.com/app-assets/icons/WearQQ_1787807600509_icon.jpg", "published", 777, "1.8.1", "2026-09-11", "社交通讯"),
        DeveloperApp("f0e7f5a0-640f-47a0-8a77-f78c2384f1bc", "五子棋·真·重构版", "com.zhshop.gomoku", "https://backend.appmiaoda.com/projects/supabase317616740710264832/storage/v1/object/public/app-assets/icons/f0e7f5a0_icon.png", "published", 4208, "2.0", "2026-09-18", "游戏")
    )

    private fun initialPendingVersions() = listOf(
        PendingVersion(
            id = "pv_1",
            appId = "5514ec79-b2ba-4717-a11a-30530324a82a",
            appName = "zhshop",
            developerName = "750221353.qq",
            versionCode = 200,
            versionName = "2.0.0-Beta",
            changelog = "新增动态壁纸同步，全面重构多线程下载流",
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/5514ec79-b2ba-4717-a11a-30530324a82a/download",
            status = "pending",
            submittedAt = "2026-09-24 10:30"
        ),
        PendingVersion(
            id = "pv_2",
            appId = "8829b8b4-76f5-41fb-8dd9-a2c2e8efa39f",
            appName = "WearQQ Next",
            developerName = "750221353.qq",
            versionCode = 190,
            versionName = "1.9.0",
            changelog = "适配圆屏边缘防误触与最新语音解码库",
            downloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/8829b8b4-76f5-41fb-8dd9-a2c2e8efa39f/download",
            status = "pending",
            submittedAt = "2026-09-23 16:45"
        )
    )

    private fun initialRejectedApps() = listOf(
        RejectedAppRecord(
            id = "rej_101",
            appId = "b8831e63-fedc-420d-8128-286c396a53e4",
            appName = "DeepSeek手表版",
            reason = "开发者信息中包含'搬运'字样，涉及知识产权风险，需要提供授权说明或使用官方签名。",
            rejectedAt = "2026-09-20 14:20",
            comments = listOf(
                AppealComment("c1", "应用审核专员 #01", true, "检测到开发者描述包含搬运字样，穿戴端应遵循版权审核规范。", "2026-09-20 14:20"),
                AppealComment("c2", "750221353.qq", false, "已补充开源项目使用说明与原作者版权声明，申请二次复审！", "2026-09-20 15:10")
            ),
            appealStatus = "处理中"
        )
    )

    private fun initialBoards() = listOf(
        ForumBoard("7c233fe4-2bca-4407-accf-513443949c12", "综合讨论", "应用商店综合话题，自由交流", 86, "💬"),
        ForumBoard("351e4979-1d4a-43c2-85db-78c376d1841a", "BUG反馈", "发现应用Bug？在这里反馈给开发者", 51, "🐛"),
        ForumBoard("71b1e5ae-0351-4349-858c-99ae55652b4f", "功能建议", "有好点子？向开发团队提建议", 14, "💡"),
        ForumBoard("5e2a6594-c86d-4aad-a53e-a8d15ae9167e", "资源分享", "分享实用资源、教程和使用技巧", 0, "📦"),
        ForumBoard("3a7a3da3-ab86-4fc3-9cd6-d5cca1a0b36c", "应用推荐", "分享你发现的好应用，让更多人受益", 7, "⭐")
    )

    private fun initialPosts() = listOf(
        ForumPost(
            id = "p_1",
            boardId = "b_tech",
            boardName = "极客技术",
            authorId = "u_101",
            authorName = "嵌入式极客",
            authorAvatar = "https://picsum.photos/seed/u101/100/100",
            authorBadge = "金牌布道师",
            title = "【深度】如何将一个 Android 手表应用瘦身至 5MB 以内？",
            content = "穿戴设备的存储空间和下载带宽非常宝贵。在这篇文章中，我总结了三点实战经验：1. 使用 Proguard/R8 极致混淆；2. 资源矢量化与 WebP 压缩；3. 移除多架构无用 so 库...",
            viewCount = 1420,
            likeCount = 89,
            replyCount = 12,
            isLiked = true,
            createdAt = "3小时前"
        ),
        ForumPost(
            id = "p_2",
            boardId = "b_watch",
            boardName = "表盘交流",
            authorId = "u_102",
            authorName = "UI设计的风",
            authorAvatar = "https://picsum.photos/seed/u102/100/100",
            authorBadge = "设计先锋",
            title = "分享一款 Material 3 风格常显表盘设计规范稿 (含 Figma 源码)",
            content = "针对 OLED 屏幕的发光原理，深黑底色配合 M3 柔和低饱和度强调色，既保留高级感又能有效防止烧屏与省电。欢迎大家取用！",
            viewCount = 980,
            likeCount = 64,
            replyCount = 8,
            isLiked = false,
            createdAt = "昨日 20:15"
        )
    )

    private fun initialRepliesMap() = mapOf(
        "p_1" to listOf(
            PostReply("r1", "p_1", "u_201", "代码猫咪", "https://picsum.photos/seed/u201/100/100", "受教了！我们团队测试了矢量 WebP，包体积确实立竿见影缩减了 40%。", 1, "2小时前"),
            PostReply("r2", "p_1", "u_202", "手表发烧友", "https://picsum.photos/seed/u202/100/100", "支持楼主！求分享一套手表低功耗长连接的代码片段！", 2, "1小时前")
        )
    )

    private fun initialConversations() = listOf(
        Conversation("c_sys", "系统与更新通知", false, listOf("system"), "ZHShop v2.5.0-Release 客户端已推送", "10:30", 1, "https://picsum.photos/seed/sysicon/100/100"),
        Conversation("c_dev_team", "开发者交流群", true, listOf("u_101", "u_102", "me"), "极客小工坊: 欢迎新加入的独立开发者！", "09:42", 0, "https://picsum.photos/seed/groupchat/100/100"),
        Conversation("c_support", "官方客服与审核", false, listOf("support"), "工单 #101 审核专员已回复您的申诉留言", "昨天", 0, "https://picsum.photos/seed/support/100/100")
    )

    private fun initialMessagesMap() = mapOf(
        "c_sys" to listOf(
            ChatMessage("m_s1", "c_sys", "system", "系统消息", "欢迎使用 ZHShop 手机版！您可以在这里发现最轻快、无干扰的精选应用。", "text", "09:00", false),
            ChatMessage("m_s2", "c_sys", "system", "系统消息", "ZHShop v2.5.0-Release 客户端已推送，全面支持 Material Design 3 与流云直传。", "text", "10:30", false)
        ),
        "c_dev_team" to listOf(
            ChatMessage("m_g1", "c_dev_team", "u_101", "嵌入式极客", "各位小伙伴，大家有遇到手表端心跳定时唤醒被系统后台冻结的情况吗？", "text", "09:20", false),
            ChatMessage("m_g2", "c_dev_team", "u_102", "UI设计的风", "可以用 WorkManager 配合约束条件，实测功耗最平衡。", "text", "09:35", false),
            ChatMessage("m_g3", "c_dev_team", "dev_03", "极客小工坊", "欢迎新加入的独立开发者！大家一起完善生态。", "text", "09:42", false)
        ),
        "c_support" to listOf(
            ChatMessage("m_sup1", "c_support", "support", "官方客服", "您好，您提交的申诉工单 #101 审核专员已回复您的留言，请及时查看。", "text", "昨天 14:30", false)
        )
    )

    private fun initialNotifications() = listOf(
        NotificationItem("n1", "新版本审核通过", "您提交的「zhshop」v2.0 审核通过并已上架分发。", "update", false, "今天 09:15", "5514ec79-b2ba-4717-a11a-30530324a82a"),
        NotificationItem("n2", "每日签到提醒", "连续签到第 4 天已达成，获得 20 极客积分。", "system", true, "今天 08:00"),
        NotificationItem("n3", "帖子获得点赞", "「嵌入式极客」点赞了您在【综合讨论】板块发布的讨论帖。", "community", false, "昨天 18:30")
    )

    private fun initialCollections() = listOf(
        AppCollection(
            id = "col_1",
            title = "穿戴极简生活必备套件",
            description = "精选高能耗比工具，摆脱手机依赖，抬腕搞定一切日常需求。",
            coverUrl = "https://picsum.photos/seed/col1/600/300",
            apps = _appsList.value.take(3),
            subscriberCount = 3840,
            isSubscribed = true
        ),
        AppCollection(
            id = "col_2",
            title = "极客独立游戏巡礼",
            description = "无广告、无内购、专为小屏触控量身打磨的纯粹游戏世界。",
            coverUrl = "https://picsum.photos/seed/col2/600/300",
            apps = _appsList.value.filter { it.categoryName.contains("游戏") },
            subscriberCount = 1920,
            isSubscribed = false
        )
    )

    private fun initialFeed() = listOf(
        FeedItem(
            id = "f_1",
            user = SocialUser("u_101", "嵌入式极客", "https://picsum.photos/seed/u101/100/100", "热爱开源与穿戴生态", 320, 45, 6, true),
            actionType = "更新了应用版本",
            targetTitle = "极光手表表盘 Pro v3.2.1",
            timestamp = "2小时前",
            snippet = "优化 AOD 常显与心率组件刷新频率，带来了更长续航表现。"
        ),
        FeedItem(
            id = "f_2",
            user = SocialUser("u_102", "UI设计的风", "https://picsum.photos/seed/u102/100/100", "像素级强迫症设计师", 580, 112, 12, false),
            actionType = "发表了技术探讨",
            targetTitle = "Material 3 风格常显表盘设计规范稿",
            timestamp = "昨天 20:15",
            snippet = "针对 OLED 屏幕的发光特性，设计了一套极佳护眼与省电并存的 UI 方案。"
        )
    )

    private fun initialSponsors() = listOf(
        SponsorApplication("sp_1", "微信赞助", 68.0, "支持 ZHShop 独立应用商店生态发展，特别赞助开发者计划", "https://picsum.photos/seed/proof1/400/300", "approved", "2026-09-10"),
        SponsorApplication("sp_2", "支付宝赞助", 30.0, "感谢微风文件助手作者无偿开源贡献！", "https://picsum.photos/seed/proof2/400/300", "pending", "2026-09-20")
    )

    private fun initialBadges() = listOf(
        UserBadge("b1", "极客先锋", "ZHShop 早期入驻认证会员", "RocketLaunch", true, true),
        UserBadge("b2", "资深开发者", "在应用商店发布超过 3 款合规独立应用", "Code", true, false),
        UserBadge("b3", "社区布道师", "在极客社区累计获赞超过 500 次", "ThumbUp", true, false),
        UserBadge("b4", "金牌赞助人", "赞助 ZHShop 平台服务器与 CDN 节点建设", "Favorite", false, false)
    )
}
