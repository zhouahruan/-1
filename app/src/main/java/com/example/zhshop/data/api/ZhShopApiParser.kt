package com.example.zhshop.data.api

import com.example.zhshop.data.model.*
import org.json.JSONArray
import org.json.JSONObject

object ZhShopApiParser {

    fun parseAppsList(jsonString: String): List<AppItem> {
        val result = mutableListOf<AppItem>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return emptyList()
            val list = data.optJSONArray("list") ?: return emptyList()
            for (i in 0 until list.length()) {
                val obj = list.optJSONObject(i) ?: continue
                val app = parseAppItemFromJson(obj, i)
                result.add(app)
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseAppItemFromJson(obj: JSONObject, fallbackIndex: Int = 0): AppItem {
        val id = obj.optString("id")
        val name = obj.optString("name", "未命名应用")
        val slug = obj.optString("slug", "")
        val shortDesc = obj.optString("short_desc", "轻量穿戴生态适配应用")
        val iconUrl = obj.optString("icon_url", "")
        val version = obj.optString("version", "1.0.0")
        val downloadCount = obj.optLong("download_count", 0L)
        val avgRating = obj.optDouble("avg_rating", 5.0).toFloat()
        val rating = if (avgRating <= 0f) 5.0f else avgRating

        val categoryObj = obj.optJSONObject("category")
        val categoryId = obj.optString("category_id").ifEmpty { categoryObj?.optString("id").orEmpty() }
        val categoryName = obj.optString("category_name").ifEmpty { categoryObj?.optString("name") ?: "应用" }

        val devObj = obj.optJSONObject("developer")
        val devName = devObj?.optString("username") ?: obj.optString("developer_name", "认证开发者")
        val devId = devObj?.optString("id") ?: obj.optString("developer_id", "")

        val status = obj.optString("status", "published")
        val updatedAt = obj.optString("updated_at", "2026-09-20")
        val dateDisplay = if (updatedAt.length >= 10) updatedAt.substring(0, 10) else updatedAt

        // Derive package name from slug or icon url
        var derivedPkg = "com.zhshop.$slug"
        if (iconUrl.contains("com_") || iconUrl.contains("com.")) {
            try {
                val match = Regex("""(com[._][a-zA-Z0-9_]+)""").find(iconUrl)
                if (match != null) {
                    derivedPkg = match.value.replace("_", ".")
                }
            } catch (_: Exception) {}
        }
        val explicitPkg = obj.optString("package_name")
        val finalPkg = if (explicitPkg.isNotBlank()) explicitPkg else if (derivedPkg.length >= 8) derivedPkg else "com.zhshop.app_$fallbackIndex"

        val tags = mutableListOf<String>()
        if (categoryName.isNotBlank()) tags.add(categoryName)
        if (downloadCount > 500) tags.add("热门推荐")
        if (name.contains("手表") || name.contains("腕") || shortDesc.contains("手表")) tags.add("腕上适配")
        tags.add("最新更新")

        val standardDownloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/$id/download"
        val explicitDownloadUrl = obj.optString("download_url").ifEmpty {
            val pkg = obj.optString("package_url")
            if (pkg.isNotBlank() && !pkg.startsWith("http://11.4.13.")) pkg else standardDownloadUrl
        }
        val changelog = obj.optString("changelog").ifEmpty { "适配最新手表屏幕分辨率与流畅触控交互" }
        val versionCode = obj.optInt("version_code", 100)

        val rawSize = obj.optLong("package_size", 0L)
        val sizeBytes = if (rawSize > 0) rawSize else (10485760L + (downloadCount % 5000000))
        val formattedSize = if (rawSize > 0) {
            "%.1f MB".format(rawSize / (1024f * 1024f))
        } else {
            "${"%.1f".format(8.5 + (downloadCount % 150) * 0.1)} MB"
        }

        return AppItem(
            id = id,
            name = name,
            packageName = finalPkg,
            iconUrl = iconUrl,
            summary = shortDesc,
            description = obj.optString("description", shortDesc),
            categoryId = categoryId,
            categoryName = categoryName,
            developerName = devName,
            developerId = devId,
            rating = rating,
            downloadCount = downloadCount,
            sizeBytes = sizeBytes,
            formattedSize = formattedSize,
            currentVersion = version,
            versionCode = versionCode,
            minSdk = 24,
            changelog = changelog,
            screenshots = if (iconUrl.isNotBlank()) listOf(iconUrl) else emptyList(),
            tags = tags,
            downloadUrl = explicitDownloadUrl,
            apkMd5 = obj.optString("apk_md5", ""),
            status = status,
            isFeatured = downloadCount > 500,
            releaseDate = dateDisplay,
            candidateDownloadUrls = listOf(standardDownloadUrl)
        )
    }

    fun parseAppDetail(jsonString: String, baseApp: AppItem?): AppItem? {
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return baseApp
            val id = data.optString("id")
            val name = data.optString("name", baseApp?.name ?: "应用")
            val desc = data.optString("description").ifEmpty { data.optString("short_desc", baseApp?.description ?: "暂无描述") }
            val shortDesc = data.optString("short_desc", baseApp?.summary ?: "轻量穿戴生态适配应用")
            val pkgName = data.optString("package_name").ifEmpty { baseApp?.packageName ?: "com.zhshop.$id" }
            val iconUrl = data.optString("icon_url", baseApp?.iconUrl ?: "")
            val ossUrl = data.optString("oss_url", "")
            val downloadCount = data.optLong("download_count", baseApp?.downloadCount ?: 0L)
            val avgRating = data.optDouble("avg_rating", 5.0).toFloat()
            val minOsVersion = data.optString("min_os_version", "5.0")
            val rawPackageSize = data.optLong("package_size", baseApp?.sizeBytes ?: 0L)
            val sizeBytes = if (rawPackageSize > 0) rawPackageSize else (baseApp?.sizeBytes ?: 12500000L)
            val formattedSize = if (rawPackageSize > 0) "%.1f MB".format(rawPackageSize / (1024f * 1024f)) else (baseApp?.formattedSize ?: "12.0 MB")
            val safetyScore = data.optInt("safety_score", 100)
            val safetyReportObj = data.optJSONObject("safety_report")
            val safetyAnalysis = safetyReportObj?.optString("analysis", "安全检测通过") ?: "安全检测通过"

            val permissionsStr = data.optString("permissions", "")
            val permissionsList = if (permissionsStr.isNotBlank()) {
                permissionsStr.split("\n", ",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                baseApp?.permissionsList ?: emptyList()
            }

            var latestVersion = data.optString("version", baseApp?.currentVersion ?: "1.0.0")
            var latestChangelog = data.optString("changelog", baseApp?.changelog ?: "常规优化与稳定性提升")

            val candidateUrls = mutableListOf<String>()
            val gatewayDownloadUrl = "https://backend.appmiaoda.com/projects/supabase317616740710264832/functions/v1/zhshop-api/android/apps/$id/download"
            candidateUrls.add(gatewayDownloadUrl)

            val versionHistory = mutableListOf<AppVersionHistory>()
            val versions = data.optJSONArray("versions")
            if (versions != null && versions.length() > 0) {
                for (i in 0 until versions.length()) {
                    val vObj = versions.optJSONObject(i) ?: continue
                    val vNum = vObj.optString("version")
                    val vLog = vObj.optString("changelog")
                    val vDate = vObj.optString("created_at")
                    val vPkgUrl = vObj.optString("package_url")

                    if (i == 0 && vNum.isNotBlank() && latestVersion.isBlank()) {
                        latestVersion = vNum
                    }
                    if (i == 0 && vLog.isNotBlank() && latestChangelog.isBlank()) {
                        latestChangelog = vLog
                    }

                    versionHistory.add(
                        AppVersionHistory(
                            version = vNum,
                            changelog = vLog.ifBlank { "常规维护与性能优化" },
                            createdAt = if (vDate.length >= 10) vDate.substring(0, 10) else vDate,
                            packageUrl = vPkgUrl
                        )
                    )

                    if (vPkgUrl.isNotBlank()) {
                        if (vPkgUrl.startsWith("http://11.4.13.")) {
                            candidateUrls.add(gatewayDownloadUrl)
                        } else {
                            candidateUrls.add(vPkgUrl)
                        }
                    }
                }
            }

            val rawPackageUrl = data.optString("package_url")
            if (rawPackageUrl.isNotBlank() && !rawPackageUrl.startsWith("http://11.4.13.")) {
                candidateUrls.add(rawPackageUrl)
            }
            if (ossUrl.isNotBlank()) {
                candidateUrls.add(ossUrl)
            }

            val screenshots = mutableListOf<String>()
            val screensArr = data.optJSONArray("screenshots")
            if (screensArr != null) {
                for (i in 0 until screensArr.length()) {
                    val item = screensArr.opt(i)
                    if (item is JSONObject) {
                        val u = item.optString("url")
                        if (u.isNotBlank()) screenshots.add(u)
                    } else if (item is String && item.isNotBlank() && item.startsWith("http")) {
                        screenshots.add(item)
                    }
                }
            }
            if (screenshots.isEmpty() && iconUrl.isNotBlank()) {
                screenshots.add(iconUrl)
            }

            val categoryObj = data.optJSONObject("category")
            val categoryName = categoryObj?.optString("name") ?: baseApp?.categoryName ?: "应用"
            val categoryId = categoryObj?.optString("id") ?: baseApp?.categoryId ?: ""

            val devObj = data.optJSONObject("developer")
            val devName = data.optString("developer_name").ifEmpty {
                devObj?.optString("username") ?: baseApp?.developerName ?: "官方开发者"
            }
            val devId = devObj?.optString("id") ?: baseApp?.developerId ?: ""

            val distinctCandidates = candidateUrls.distinct()
            val primaryDownloadUrl = distinctCandidates.firstOrNull() ?: gatewayDownloadUrl

            return (baseApp ?: parseAppItemFromJson(data)).copy(
                id = id,
                name = name,
                packageName = pkgName,
                iconUrl = iconUrl,
                summary = shortDesc,
                description = desc,
                categoryName = categoryName,
                categoryId = categoryId,
                developerName = devName,
                developerId = devId,
                currentVersion = latestVersion,
                changelog = latestChangelog,
                downloadUrl = primaryDownloadUrl,
                downloadCount = downloadCount,
                sizeBytes = sizeBytes,
                formattedSize = formattedSize,
                rating = if (avgRating <= 0f) 5.0f else avgRating,
                screenshots = screenshots,
                minOsVersion = minOsVersion,
                permissionsList = permissionsList,
                safetyScore = safetyScore,
                safetyAnalysis = safetyAnalysis,
                versionsList = versionHistory,
                candidateDownloadUrls = distinctCandidates
            )
        } catch (_: Exception) {
            return baseApp
        }
    }

    fun parsePulse(
        jsonString: String,
        currentApps: List<AppItem>,
        announcements: List<Announcement>
    ): PulseData? {
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return null

            // 1. Totals
            val totals = data.optJSONObject("totals")
            val totalApps = totals?.optLong("apps", 187L) ?: 187L
            val totalUsers = totals?.optLong("users", 143L) ?: 143L
            val totalDownloads = totals?.optLong("downloads", 37823L) ?: 37823L
            val forumPostsCount = totals?.optLong("forum_posts", 147L) ?: 147L
            val stats = PlatformStats(
                totalApps = totalApps,
                totalDownloads = totalDownloads,
                activeUsers = totalUsers,
                todayUpdates = forumPostsCount.toInt()
            )

            // 2. Top Downloads
            val topDownloadsList = mutableListOf<AppItem>()
            val topDownloadsArr = data.optJSONArray("top_downloads")
            if (topDownloadsArr != null) {
                for (i in 0 until topDownloadsArr.length()) {
                    val obj = topDownloadsArr.optJSONObject(i) ?: continue
                    val app = parseAppItemFromJson(obj, i)
                    topDownloadsList.add(app)
                }
            }

            // 3. Recently Added
            val recentlyAddedList = mutableListOf<AppItem>()
            val recentlyAddedArr = data.optJSONArray("recently_added")
            if (recentlyAddedArr != null) {
                for (i in 0 until recentlyAddedArr.length()) {
                    val obj = recentlyAddedArr.optJSONObject(i) ?: continue
                    val app = parseAppItemFromJson(obj, i)
                    recentlyAddedList.add(app)
                }
            }

            // 4. Trending this week
            val trendingList = mutableListOf<AppItem>()
            val trendingArr = data.optJSONArray("trending_this_week")
            if (trendingArr != null) {
                for (i in 0 until trendingArr.length()) {
                    val obj = trendingArr.optJSONObject(i) ?: continue
                    val app = parseAppItemFromJson(obj, i)
                    trendingList.add(app)
                }
            }

            // 5. Banners generated from top apps and live announcements
            val banners = mutableListOf<BannerItem>()
            topDownloadsList.take(3).forEachIndexed { idx, app ->
                banners.add(
                    BannerItem(
                        id = "b_pulse_$idx",
                        title = "🔥 榜首热榜：${app.name}",
                        subtitle = "累计 ${app.downloadCount} 次下载 • 腕上极客首选",
                        imageUrl = app.iconUrl,
                        targetAppId = app.id,
                        tag = "TOP ${idx + 1}"
                    )
                )
            }
            if (banners.isEmpty()) {
                banners.add(
                    BannerItem(
                        id = "b_default",
                        title = "ZHShop 腕上应用生态",
                        subtitle = "连接手表与手机，超轻量无感体验",
                        imageUrl = "https://picsum.photos/seed/zhbanner1/600/300",
                        targetAppId = null,
                        tag = "官方推荐"
                    )
                )
            }

            val hotApps = if (topDownloadsList.isNotEmpty()) topDownloadsList else currentApps.take(6)
            val featuredApps = if (recentlyAddedList.isNotEmpty()) recentlyAddedList else currentApps.filter { it.isFeatured }
            val latestUpdated = if (recentlyAddedList.isNotEmpty()) recentlyAddedList else currentApps.take(10)

            return PulseData(
                banners = banners,
                announcements = announcements,
                hotApps = hotApps,
                featuredApps = featuredApps,
                stats = stats,
                latestUpdatedApps = latestUpdated
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun parseCategories(jsonString: String): List<AppCategory> {
        val result = mutableListOf<AppCategory>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: root.optJSONObject("data")?.optJSONArray("categories")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val name = obj.optString("name")
                val icon = when {
                    name.contains("社交") || name.contains("通讯") -> "Chat"
                    name.contains("游戏") -> "Gamepad"
                    name.contains("工具") || name.contains("系统") -> "Build"
                    name.contains("音") || name.contains("视") -> "PlayArrow"
                    name.contains("ai", ignoreCase = true) -> "AutoAwesome"
                    name.contains("启动") || name.contains("桌面") || name.contains("腕") -> "Apps"
                    name.contains("网盘") || name.contains("下载") -> "CloudDownload"
                    name.contains("生活") -> "Favorite"
                    name.contains("理财") || name.contains("金融") -> "Payments"
                    name.contains("教育") || name.contains("学习") || name.contains("阅读") -> "MenuBook"
                    name.contains("效率") || name.contains("办公") -> "Work"
                    name.contains("壁纸") || name.contains("图像") -> "Palette"
                    else -> "Category"
                }
                result.add(
                    AppCategory(
                        id = id,
                        name = name,
                        description = obj.optString("description", "平台精选收录之 $name 类应用与穿戴组件"),
                        appCount = obj.optInt("app_count", 10 + (i * 3) % 20),
                        icon = icon
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseAnnouncements(jsonString: String): List<Announcement> {
        val result = mutableListOf<Announcement>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data") ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title", "系统通知")
                val content = obj.optString("content", "")
                val type = obj.optString("type", "info")
                val createdAt = obj.optString("created_at", "2026-09-20")
                val dateDisplay = if (createdAt.length >= 10) createdAt.substring(0, 10) else createdAt
                result.add(
                    Announcement(
                        id = id,
                        title = title,
                        content = content,
                        priority = if (type == "warning" || type == "danger") 3 else 1,
                        publishDate = dateDisplay
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseForumBoards(jsonString: String): List<ForumBoard> {
        val result = mutableListOf<ForumBoard>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data") ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val name = obj.optString("name")
                val desc = obj.optString("description")
                val icon = obj.optString("icon", "💬")
                val postCount = obj.optInt("post_count", 0)
                result.add(
                    ForumBoard(
                        id = id,
                        name = name,
                        description = desc,
                        postCount = postCount,
                        icon = icon
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseForumPosts(jsonString: String): List<ForumPost> {
        val result = mutableListOf<ForumPost>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return emptyList()
            val list = data.optJSONArray("list") ?: return emptyList()
            for (i in 0 until list.length()) {
                val obj = list.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title", "交流帖")
                val author = obj.optString("author_name", "极客用户")
                val authorId = obj.optString("user_id", "u_$i")
                val likeCount = obj.optInt("like_count", 0)
                val replyCount = obj.optInt("reply_count", 0)
                val viewCount = obj.optInt("view_count", 0)
                val isPinned = obj.optBoolean("is_pinned", false)
                val createdAt = obj.optString("created_at", "刚刚")
                val dateDisplay = if (createdAt.length >= 10) createdAt.substring(0, 10) else createdAt
                val boardObj = obj.optJSONObject("board")
                val boardName = boardObj?.optString("name") ?: "综合讨论"

                result.add(
                    ForumPost(
                        id = id,
                        boardId = boardObj?.optString("id") ?: "general",
                        boardName = boardName,
                        authorId = authorId,
                        authorName = author,
                        authorAvatar = "",
                        authorBadge = if (isPinned) "置顶" else "活跃用户",
                        title = title,
                        content = obj.optString("content", "点击查看社区讨论详细内容与楼层回复"),
                        viewCount = viewCount,
                        likeCount = likeCount,
                        replyCount = replyCount,
                        createdAt = dateDisplay
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseForumPostDetail(jsonString: String): ForumPost? {
        try {
            val root = JSONObject(jsonString)
            val obj = root.optJSONObject("data") ?: return null
            val id = obj.optString("id")
            val title = obj.optString("title", "交流帖")
            val content = obj.optString("content", "")
            val author = obj.optString("author_name", "极客用户")
            val authorId = obj.optString("user_id", "u_0")
            val likeCount = obj.optInt("like_count", 0)
            val replyCount = obj.optInt("reply_count", 0)
            val viewCount = obj.optInt("view_count", 0)
            val isPinned = obj.optBoolean("is_pinned", false)
            val createdAt = obj.optString("created_at", "刚刚")
            val dateDisplay = if (createdAt.length >= 10) createdAt.substring(0, 10) else createdAt
            val boardObj = obj.optJSONObject("board")
            val boardName = boardObj?.optString("name") ?: "综合讨论"

            return ForumPost(
                id = id,
                boardId = boardObj?.optString("id") ?: "general",
                boardName = boardName,
                authorId = authorId,
                authorName = author,
                authorAvatar = "",
                authorBadge = if (isPinned) "置顶" else "活跃用户",
                title = title,
                content = content,
                viewCount = viewCount,
                likeCount = likeCount,
                replyCount = replyCount,
                createdAt = dateDisplay
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun parseCollections(jsonString: String): List<AppCollection> {
        val result = mutableListOf<AppCollection>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return emptyList()
            val list = data.optJSONArray("list") ?: return emptyList()
            for (i in 0 until list.length()) {
                val obj = list.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title")
                val desc = obj.optString("description")
                val coverUrl = obj.optString("cover_url", "")
                val likeCount = obj.optInt("like_count", 0)

                result.add(
                    AppCollection(
                        id = id,
                        title = title,
                        description = desc,
                        coverUrl = coverUrl,
                        apps = emptyList(),
                        subscriberCount = likeCount,
                        isSubscribed = false
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseSearchApps(jsonString: String): List<AppItem> {
        val result = mutableListOf<AppItem>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return emptyList()
            val appsArr = data.optJSONArray("apps") ?: return emptyList()
            for (i in 0 until appsArr.length()) {
                val obj = appsArr.optJSONObject(i) ?: continue
                result.add(parseAppItemFromJson(obj, i))
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseLeaderboardItems(jsonString: String, type: String, knownApps: List<AppItem> = emptyList()): List<LeaderboardItem> {
        val result = mutableListOf<LeaderboardItem>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val rank = obj.optInt("rank", i + 1)
                
                if (type == "downloads" || type == "download" || type == "hot") {
                    val appObj = obj.optJSONObject("app") ?: obj
                    val appId = appObj.optString("id", appObj.optString("app_id", "lead_app_$i"))
                    val matchedApp = knownApps.find { it.id == appId || (it.packageName.isNotBlank() && it.packageName == appObj.optString("package_name")) }
                    val downloadCount = obj.optLong("download_count", appObj.optLong("download_count", obj.optLong("metric", 0L)))
                    val appItem = matchedApp?.copy(
                        downloadCount = if (downloadCount > 0) downloadCount else matchedApp.downloadCount
                    ) ?: parseAppItemFromJson(appObj, i).copy(
                        id = appId,
                        downloadCount = downloadCount
                    )
                    result.add(
                        LeaderboardItem(
                            rank = rank,
                            app = appItem,
                            metricValue = appItem.downloadCount,
                            metricLabel = "${appItem.downloadCount} 次下载"
                        )
                    )
                } else if (type == "uploads" || type == "upload") {
                    val username = obj.optString("username", obj.optString("author_name", "极客开发者"))
                    val metric = obj.optDouble("metric", obj.optDouble("upload_count", obj.optDouble("count", 0.0)))
                    val note = obj.optString("note", "优质穿戴生态创作者")
                    val label = "发布 ${metric.toInt()} 款应用 • $note"
                    val mockApp = AppItem(
                        id = "lead_dev_$i",
                        name = username,
                        packageName = "com.zhshop.user.$username",
                        iconUrl = obj.optString("avatar_url", "https://picsum.photos/seed/$username/128/128"),
                        summary = label,
                        description = "$username 的平台发布贡献记录",
                        categoryId = "c_user",
                        categoryName = "发布榜",
                        developerName = username,
                        developerId = obj.optString("user_id", ""),
                        rating = 5.0f,
                        downloadCount = metric.toLong(),
                        sizeBytes = 0L,
                        formattedSize = "",
                        currentVersion = "v${metric.toInt()}.0",
                        versionCode = metric.toInt(),
                        minSdk = 24,
                        changelog = "",
                        screenshots = emptyList(),
                        tags = listOf("发布榜 TOP$rank", "独立开发者"),
                        downloadUrl = "",
                        apkMd5 = ""
                    )
                    result.add(
                        LeaderboardItem(
                            rank = rank,
                            app = mockApp,
                            metricValue = metric.toLong(),
                            metricLabel = label
                        )
                    )
                } else {
                    // sponsors
                    val username = obj.optString("username", "爱心赞助者")
                    val metric = obj.optDouble("metric", obj.optDouble("amount", 0.0))
                    val note = obj.optString("note", "支持 ZHShop 独立服务器运转")
                    val label = "赞助 ¥${"%.2f".format(metric)} • $note"
                    val mockApp = AppItem(
                        id = "lead_sponsor_$i",
                        name = username,
                        packageName = "com.zhshop.sponsor.$username",
                        iconUrl = obj.optString("avatar_url", "https://picsum.photos/seed/$username/128/128"),
                        summary = label,
                        description = "$username 的平台赞助记录",
                        categoryId = "c_sponsor",
                        categoryName = "赞助榜",
                        developerName = username,
                        developerId = obj.optString("user_id", ""),
                        rating = 5.0f,
                        downloadCount = metric.toLong(),
                        sizeBytes = 0L,
                        formattedSize = "",
                        currentVersion = "Lv.${(metric / 10).toInt() + 1}",
                        versionCode = 1,
                        minSdk = 24,
                        changelog = "",
                        screenshots = emptyList(),
                        tags = listOf("赞助榜 TOP$rank", "荣誉赞助者"),
                        downloadUrl = "",
                        apkMd5 = ""
                    )
                    result.add(
                        LeaderboardItem(
                            rank = rank,
                            app = mockApp,
                            metricValue = metric.toLong(),
                            metricLabel = label
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseUserProfile(jsonString: String): UserProfile? {
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: root
            val userObj = data.optJSONObject("user") ?: data
            val id = userObj.optString("id")
            if (id.isBlank()) return null
            val username = userObj.optString("username", userObj.optString("name", "极客用户"))
            val email = userObj.optString("email", "")
            val avatarUrl = userObj.optString("avatar_url", "")
            val bio = userObj.optString("bio", "")
            val points = userObj.optInt("points", 100)
            val level = userObj.optString("level", "Lv.3 资深玩家")
            val isDev = userObj.optBoolean("is_developer", true)
            val isAdmin = userObj.optBoolean("is_admin", false)
            val days = userObj.optInt("consecutive_days", 3)
            return UserProfile(
                id = id,
                username = username,
                email = email,
                avatarUrl = avatarUrl,
                bio = bio,
                points = points,
                level = level,
                isDeveloper = isDev,
                isAdmin = isAdmin,
                consecutiveDays = days
            )
        } catch (_: Exception) {
            return null
        }
    }

    fun parseAuthTokens(jsonString: String): Pair<String, String>? {
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: root
            val access = data.optString("access_token").ifEmpty { data.optString("accessToken") }
            val refresh = data.optString("refresh_token").ifEmpty { data.optString("refreshToken") }
            if (access.isNotBlank()) {
                return Pair(access, refresh)
            }
        } catch (_: Exception) {}
        return null
    }

    fun parsePostReplies(jsonString: String, postId: String): List<PostReply> {
        val result = mutableListOf<PostReply>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val authorObj = obj.optJSONObject("author")
                val authorName = authorObj?.optString("username") ?: obj.optString("author_name", "极客玩家")
                val authorAvatar = authorObj?.optString("avatar_url") ?: obj.optString("author_avatar", "")
                val content = obj.optString("content", "")
                val createdAt = obj.optString("created_at", "刚刚")
                val dateDisplay = if (createdAt.length >= 10) createdAt.substring(0, 10) else createdAt
                val floor = obj.optInt("floor_num", obj.optInt("floor_number", i + 1))
                result.add(
                    PostReply(
                        id = id,
                        postId = postId,
                        authorId = obj.optString("user_id", "u_$i"),
                        authorName = authorName,
                        authorAvatar = authorAvatar,
                        content = content,
                        floorNumber = floor,
                        createdAt = dateDisplay
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseConversations(jsonString: String): List<Conversation> {
        val result = mutableListOf<Conversation>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("name", obj.optString("title", "公共交流群"))
                val type = obj.optString("type", "")
                val isGroup = type == "group" || obj.optBoolean("is_group", false)
                val lastMsgObj = obj.optJSONObject("last_message")
                val lastMsg = lastMsgObj?.optString("content") ?: obj.optString("last_message", "")
                val lastTimeRaw = lastMsgObj?.optString("created_at") ?: obj.optString("last_time", obj.optString("updated_at", "刚刚"))
                val lastTime = if (lastTimeRaw.length >= 16) lastTimeRaw.substring(11, 16) else lastTimeRaw
                val memberInfo = obj.optJSONObject("member_info")
                val unread = memberInfo?.optInt("unread_count", 0) ?: obj.optInt("unread_count", 0)
                val avatar = obj.optString("avatar_url", "")
                val targetUserId = obj.optString("target_user_id", null)
                result.add(
                    Conversation(
                        id = id,
                        title = title,
                        isGroup = isGroup,
                        participants = listOf("user_1"),
                        lastMessage = if (lastMsg.isBlank()) obj.optString("announcement", "欢迎来到公共大厅交流讨论！") else lastMsg,
                        lastTime = lastTime,
                        unreadCount = unread,
                        avatarUrl = avatar,
                        targetUserId = targetUserId
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseMessages(jsonString: String, currentUserId: String? = null): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val convId = obj.optString("conversation_id", "")
                val senderId = obj.optString("sender_id", "")
                val senderObj = obj.optJSONObject("sender")
                val senderName = senderObj?.optString("username")
                    ?: obj.optString("sender_name", if (senderId == currentUserId) "我" else "极客网友")
                val content = obj.optString("content", "")
                val msgType = obj.optString("type", obj.optString("message_type", "text"))
                val timestamp = obj.optString("created_at", "刚刚")
                val timeDisplay = if (timestamp.length >= 16) timestamp.substring(11, 16) else timestamp
                val isSelf = (currentUserId != null && senderId == currentUserId) || obj.optBoolean("is_self", false)
                result.add(
                    ChatMessage(
                        id = id,
                        conversationId = convId,
                        senderId = senderId,
                        senderName = senderName,
                        content = content,
                        messageType = msgType,
                        timestamp = timeDisplay,
                        isSelf = isSelf
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseNotifications(jsonString: String): List<NotificationItem> {
        val result = mutableListOf<NotificationItem>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title", "系统消息")
                val message = obj.optString("message", obj.optString("content", ""))
                val type = obj.optString("type", "system")
                val isRead = obj.optBoolean("is_read", false)
                val createdAt = obj.optString("created_at", "刚刚")
                val timeDisplay = if (createdAt.length >= 10) createdAt.substring(0, 10) else createdAt
                val targetAppId = obj.optString("target_app_id", null)
                result.add(
                    NotificationItem(
                        id = id,
                        title = title,
                        message = message,
                        type = type,
                        isRead = isRead,
                        timestamp = timeDisplay,
                        targetAppId = targetAppId
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseDeveloperApps(jsonString: String): List<DeveloperApp> {
        val result = mutableListOf<DeveloperApp>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val name = obj.optString("name", "我的应用")
                val pkg = obj.optString("package_name", "com.zhshop.app_$i")
                val icon = obj.optString("icon_url", "")
                val status = obj.optString("status", "published")
                val downloads = obj.optLong("download_count", 0L)
                val version = obj.optString("version", "1.0.0")
                val updatedAt = obj.optString("updated_at", "2026-09-20")
                val category = obj.optString("category_name", "实用工具")
                result.add(
                    DeveloperApp(
                        id = id,
                        name = name,
                        packageName = pkg,
                        iconUrl = icon,
                        status = status,
                        downloads = downloads,
                        currentVersion = version,
                        lastUpdated = if (updatedAt.length >= 10) updatedAt.substring(0, 10) else updatedAt,
                        category = category
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseRejectedApps(jsonString: String): List<RejectedAppRecord> {
        val result = mutableListOf<RejectedAppRecord>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val appId = obj.optString("app_id", id)
                val appName = obj.optString("app_name", obj.optString("name", "未过审应用"))
                val reason = obj.optString("reject_reason", obj.optString("reason", "未满足安全审核基准"))
                val rejectedAt = obj.optString("rejected_at", "2026-09-18")
                val commentsList = mutableListOf<AppealComment>()
                val commentsArr = obj.optJSONArray("comments")
                if (commentsArr != null) {
                    for (c in 0 until commentsArr.length()) {
                        val cObj = commentsArr.optJSONObject(c) ?: continue
                        commentsList.add(
                            AppealComment(
                                id = cObj.optString("id", "c_$c"),
                                author = cObj.optString("author", "审核员"),
                                isReviewer = cObj.optBoolean("is_reviewer", true),
                                message = cObj.optString("message", ""),
                                timestamp = cObj.optString("timestamp", "审核记录")
                            )
                        )
                    }
                }
                result.add(
                    RejectedAppRecord(
                        id = id,
                        appId = appId,
                        appName = appName,
                        reason = reason,
                        rejectedAt = if (rejectedAt.length >= 10) rejectedAt.substring(0, 10) else rejectedAt,
                        comments = commentsList
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseSponsorApplications(jsonString: String): List<SponsorApplication> {
        val result = mutableListOf<SponsorApplication>()
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONArray("data")
                ?: root.optJSONObject("data")?.optJSONArray("list")
                ?: return emptyList()
            for (i in 0 until data.length()) {
                val obj = data.optJSONObject(i) ?: continue
                result.add(
                    SponsorApplication(
                        id = obj.optString("id"),
                        channel = obj.optString("channel", "微信赞助"),
                        amount = obj.optDouble("amount", 10.0),
                        note = obj.optString("note", ""),
                        proofImageUrl = obj.optString("proof_image_url", ""),
                        status = obj.optString("status", "pending"),
                        appliedAt = obj.optString("applied_at", "2026-09-20")
                    )
                )
            }
        } catch (_: Exception) {}
        return result
    }

    fun parseClientUpdate(jsonString: String): ClientUpdateInfo? {
        try {
            val root = JSONObject(jsonString)
            val data = root.optJSONObject("data") ?: return null
            val hasUpdate = data.optBoolean("has_update", false)
            val updateObj = data.optJSONObject("update")
            if (hasUpdate && updateObj != null) {
                return ClientUpdateInfo(
                    hasUpdate = true,
                    latestVersion = updateObj.optString("version_name", "v1.1-Online"),
                    latestVersionCode = updateObj.optInt("version_code", 110),
                    changelog = updateObj.optString("changelog", "1. 真实接口数据全量同步与解析\n2. 优化手表端应用列表与下载"),
                    downloadUrl = updateObj.optString("download_url", "https://backend.appmiaoda.com/zhshop.apk"),
                    isForceUpdate = updateObj.optBoolean("force_update", false)
                )
            }
        } catch (_: Exception) {}
        return null
    }
}
