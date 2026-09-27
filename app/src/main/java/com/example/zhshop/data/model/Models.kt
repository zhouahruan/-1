package com.example.zhshop.data.model

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserProfile?
)

data class UserProfile(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String = "",
    val bio: String = "",
    val roles: List<String> = listOf("user"),
    val points: Int = 120,
    val level: String = "Lv.3 资深玩家",
    val consecutiveDays: Int = 4,
    val isDeveloper: Boolean = true,
    val isAdmin: Boolean = false
)

data class AppVersionHistory(
    val version: String,
    val changelog: String,
    val createdAt: String,
    val packageUrl: String = ""
)

data class AppItem(
    val id: String,
    val name: String,
    val packageName: String,
    val iconUrl: String,
    val summary: String,
    val description: String,
    val categoryId: String,
    val categoryName: String,
    val developerName: String,
    val developerId: String,
    val rating: Float,
    val downloadCount: Long,
    val sizeBytes: Long,
    val formattedSize: String,
    val currentVersion: String,
    val versionCode: Int,
    val minSdk: Int,
    val changelog: String,
    val screenshots: List<String>,
    val tags: List<String>,
    val downloadUrl: String,
    val apkMd5: String,
    val status: String = "published",
    val isFeatured: Boolean = false,
    val releaseDate: String = "2026-09-15",
    val minOsVersion: String = "5.0",
    val permissionsList: List<String> = emptyList(),
    val safetyScore: Int = 100,
    val safetyAnalysis: String = "安全",
    val versionsList: List<AppVersionHistory> = emptyList(),
    val candidateDownloadUrls: List<String> = emptyList()
)

data class AppCategory(
    val id: String,
    val name: String,
    val description: String,
    val appCount: Int,
    val icon: String
)

data class BannerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val targetAppId: String? = null,
    val tag: String = "热门推荐"
)

data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val priority: Int = 1,
    val publishDate: String = "2026-09-20"
)

data class PlatformStats(
    val totalApps: Long = 12480,
    val totalDownloads: Long = 8520300,
    val activeUsers: Long = 46920,
    val todayUpdates: Int = 48
)

data class PulseData(
    val banners: List<BannerItem>,
    val announcements: List<Announcement>,
    val hotApps: List<AppItem>,
    val featuredApps: List<AppItem>,
    val stats: PlatformStats,
    val latestUpdatedApps: List<AppItem> = emptyList()
)

data class LeaderboardItem(
    val rank: Int,
    val app: AppItem,
    val metricValue: Long,
    val metricLabel: String
)

data class DeveloperApp(
    val id: String,
    val name: String,
    val packageName: String,
    val iconUrl: String,
    val status: String, // published, unpublished, under_review, rejected
    val downloads: Long,
    val currentVersion: String,
    val lastUpdated: String,
    val category: String = "实用工具"
)

data class VersionUpdateRequest(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val fileSize: Long,
    val apkMd5: String
)

data class PendingVersion(
    val id: String,
    val appId: String,
    val appName: String,
    val developerName: String,
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val downloadUrl: String,
    val status: String = "pending",
    val submittedAt: String = "2026-09-21 09:30"
)

data class RejectedAppRecord(
    val id: String,
    val appId: String,
    val appName: String,
    val reason: String,
    val rejectedAt: String,
    val comments: List<AppealComment>,
    val appealStatus: String = "可申诉"
)

data class AppealComment(
    val id: String,
    val author: String,
    val isReviewer: Boolean,
    val message: String,
    val timestamp: String
)

data class ForumBoard(
    val id: String,
    val name: String,
    val description: String,
    val postCount: Int,
    val icon: String
)

data class ForumPost(
    val id: String,
    val boardId: String,
    val boardName: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val authorBadge: String? = null,
    val title: String,
    val content: String,
    val images: List<String> = emptyList(),
    val viewCount: Int,
    val likeCount: Int,
    val replyCount: Int,
    val isLiked: Boolean = false,
    val createdAt: String
)

data class PostReply(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val content: String,
    val floorNumber: Int,
    val createdAt: String
)

data class UserBadge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean,
    val isEquipped: Boolean = false
)

data class SocialUser(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val bio: String,
    val followerCount: Int,
    val followingCount: Int,
    val appCount: Int,
    val isFollowing: Boolean
)

data class FeedItem(
    val id: String,
    val user: SocialUser,
    val actionType: String, // 发布了新应用, 更新了版本, 点赞了帖子
    val targetTitle: String,
    val timestamp: String,
    val snippet: String
)

data class AppReview(
    val id: String,
    val appId: String,
    val author: String,
    val avatar: String,
    val rating: Float,
    val content: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val replies: List<ReviewReply> = emptyList(),
    val createdAt: String
)

data class ReviewReply(
    val id: String,
    val author: String,
    val content: String,
    val createdAt: String
)

data class AppCollection(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val apps: List<AppItem>,
    val subscriberCount: Int,
    val isSubscribed: Boolean
)

data class PresenceStatus(
    val userId: String,
    val username: String,
    val status: String, // online, busy, offline
    val lastActive: String
)

data class Conversation(
    val id: String,
    val title: String,
    val isGroup: Boolean,
    val participants: List<String>,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int,
    val avatarUrl: String,
    val targetUserId: String? = null
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val messageType: String = "text", // text, image, file
    val timestamp: String,
    val isSelf: Boolean,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: String? = null
)

data class ChatFileArchive(
    val id: String,
    val fileName: String,
    val fileType: String,
    val fileSize: String,
    val sentBy: String,
    val timestamp: String,
    val downloadUrl: String
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // system, update, community
    val isRead: Boolean,
    val timestamp: String,
    val targetAppId: String? = null
)

data class SponsorApplication(
    val id: String,
    val channel: String, // 微信赞助, 支付宝赞助, 爱发电
    val amount: Double,
    val note: String,
    val proofImageUrl: String,
    val status: String, // pending, approved, rejected
    val appliedAt: String
)

data class ApkAnalysisResult(
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Int,
    val minSdk: Int,
    val targetSdk: Int,
    val launchActivity: String,
    val fileSize: Long,
    val apkMd5: String,
    val permissions: List<String>,
    val tags: List<String>
)

data class ClientUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val latestVersionCode: Int,
    val changelog: String,
    val downloadUrl: String,
    val isForceUpdate: Boolean
)

data class LiuYunInfo(
    val points: Int,
    val storageQuotaMb: Long,
    val usedStorageMb: Long,
    val vipExpiry: String,
    val docs: List<LiuYunDoc>
)

data class LiuYunDoc(
    val title: String,
    val summary: String,
    val link: String
)
