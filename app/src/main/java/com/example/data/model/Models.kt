package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Profile(
    val id: String = "",
    val nickname: String? = null,
    val avatar_url: String? = null,
    val bio: String? = null,
    val phone: String? = null,
    val role: String = "user", // "admin" | "user"
    val risk_status: String = "normal", // "normal" | "flagged"
    val risk_reason: String? = null,
    val is_official: Boolean = false,
    val created_at: String? = null
) {
    val displayName: String
        get() = nickname?.ifBlank { null } ?: "用户_${id.takeLast(4)}"

    val isSuperAdmin: Boolean
        get() {
            val cleanPhone = phone?.filter { it.isDigit() } ?: ""
            return cleanPhone == "8613719934549" || cleanPhone == "13719934549"
        }

    val isNormalAdmin: Boolean
        get() = role == "admin" && !isSuperAdmin

    val isAdmin: Boolean
        get() = isSuperAdmin || role == "admin"
}

@JsonClass(generateAdapter = true)
data class Author(
    val id: String = "",
    val nickname: String? = null,
    val avatar_url: String? = null,
    val bio: String? = null,
    val role: String = "user"
) {
    val displayName: String
        get() = nickname?.ifBlank { null } ?: "用户_${id.takeLast(4)}"
}

@JsonClass(generateAdapter = true)
data class PostAttachment(
    val type: String = "image", // "image" | "video" | "link" | "cloud_file"
    val url: String = "",
    val name: String? = null
)

/** 帖子挂载的云盘文件（post_server_files → server_files） */
@JsonClass(generateAdapter = true)
data class PostMountedFile(
    @Json(name = "file") val file: MountedFileRef? = null
)

@JsonClass(generateAdapter = true)
data class MountedFileRef(
    val id: String = "",
    val name: String = "",
    @Json(name = "size_bytes") val size_bytes: Long = 0L
) {
    val formattedSize: String
        get() = when {
            size_bytes >= 1024 * 1024 -> String.format("%.1f MB", size_bytes.toDouble() / (1024 * 1024))
            size_bytes >= 1024 -> String.format("%.1f KB", size_bytes.toDouble() / 1024)
            else -> "$size_bytes B"
        }
}

/** 引用/转发的被引帖子（post_reference_view 视图） */
@JsonClass(generateAdapter = true)
data class ReferencedPost(
    @Json(name = "ref_id") val ref_id: String = "",
    @Json(name = "ref_title") val ref_title: String? = null,
    @Json(name = "ref_status") val ref_status: String? = null,
    @Json(name = "ref_summary") val ref_summary: String? = null,
    @Json(name = "ref_author_id") val ref_author_id: String? = null,
    @Json(name = "ref_author_nickname") val ref_author_nickname: String? = null,
    @Json(name = "ref_author_avatar") val ref_author_avatar: String? = null
) {
    val authorName: String
        get() = ref_author_nickname?.ifBlank { null } ?: "原帖作者"
}

@JsonClass(generateAdapter = true)
data class Post(
    val id: String = "",
    val user_id: String = "",
    val group_id: String? = null,
    val title: String = "",
    val content: String = "",
    val ai_summary: String? = null,
    val ai_tags: List<String>? = null,
    val ai_score: Int? = null,
    val recommend_status: String? = "normal",
    val attachments: List<PostAttachment>? = null,
    /** 挂载的轻应用（红包、小游戏等通过挂载轻应用实现） */
    @Json(name = "miniapp_id") val miniapp_id: String? = null,
    @Json(name = "mini_app") val mini_app: MiniApp? = null,
    /** 引用的帖子 id 与视图数据 */
    @Json(name = "referenced_post_id") val referenced_post_id: String? = null,
    @Json(name = "referenced_post") val referenced_post: ReferencedPost? = null,
    /** 挂载的云盘文件 */
    @Json(name = "mounted_files") val mounted_files: List<PostMountedFile>? = null,
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val favorite_count: Int = 0,
    val view_count: Int = 0,
    val status: String = "published",
    val short_code: String? = null,
    val created_at: String? = null,
    val author: Author? = null,
    val isLikedByMe: Boolean = false,
    val isFavoritedByMe: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Comment(
    val id: String = "",
    val post_id: String = "",
    val user_id: String = "",
    val parent_id: String? = null,
    val root_comment_id: String? = null,
    val reply_to_user_id: String? = null,
    val content: String = "",
    val created_at: String? = null,
    val author: Author? = null
)

@JsonClass(generateAdapter = true)
data class MiniApp(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val icon_url: String? = null,
    val developer: String? = null,
    /** 真实表中为整数版本号（如 1、2） */
    val live_version: Int = 1,
    val status: String = "approved",
    /** "zip" = 上传的 ZIP 安装包，由 miniapp-serve 托管；"link" = 外部直链 */
    val source_type: String? = null,
    val entry_url: String? = null,
    val storage_path: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
) {
    val current_version: String
        get() = live_version.toString()

    val developerName: String
        get() = developer?.ifBlank { null } ?: "独立开发者"

    /** 运行地址：直链类型用 entry_url，ZIP 包走 miniapp-serve Edge Function */
    val runUrl: String
        get() = if (source_type == "link" && !entry_url.isNullOrBlank()) {
            entry_url
        } else {
            "${com.example.data.api.SupabaseConfig.FUNCTIONS_URL}/miniapp-serve/$id/v$live_version/index.html"
        }
}

@JsonClass(generateAdapter = true)
data class MiniAppVersion(
    val id: String = "",
    val miniapp_id: String = "",
    val version: String = "1.0.0",
    val code_bundle: String = "",
    val status: String = "approved"
)

@JsonClass(generateAdapter = true)
data class UserServer(
    val id: String = "",
    val owner_id: String? = null,
    val name: String = "我的云盘",
    val port: Int? = null,
    val quota_bytes: Long = 10485760L, // 10MB default
    val used_bytes: Long = 0L,
    val status: String = "active",
    val created_at: String? = null
) {
    val usedPercentage: Float
        get() = if (quota_bytes > 0) (used_bytes.toFloat() / quota_bytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedUsed: String
        get() = formatBytes(used_bytes)

    val formattedQuota: String
        get() = formatBytes(quota_bytes)

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }
}

@JsonClass(generateAdapter = true)
data class ServerFolder(
    val id: String = "",
    val server_id: String = "",
    val owner_id: String? = null,
    val name: String = "",
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class ServerFile(
    val id: String = "",
    val server_id: String = "",
    val owner_id: String? = null,
    val folder_id: String? = null,
    val name: String = "",
    val size_bytes: Long = 0L,
    val storage_path: String = "",
    val mime_type: String = "application/octet-stream",
    val download_count: Int = 0,
    val created_at: String? = null
) {
    val formattedSize: String
        get() = when {
            size_bytes >= 1024 * 1024 -> String.format("%.1f MB", size_bytes.toDouble() / (1024 * 1024))
            size_bytes >= 1024 -> String.format("%.1f KB", size_bytes.toDouble() / 1024)
            else -> "$size_bytes B"
        }

    val publicDownloadUrl: String
        get() = if (storage_path.isNotBlank()) {
            "${com.example.data.api.SupabaseConfig.BASE_URL}/storage/v1/object/public/user-server-files/$storage_path"
        } else ""
}

@JsonClass(generateAdapter = true)
data class ShareResult(
    val code: String = "",
    val shareUrl: String = "",
    val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ShareInfo(
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val needPassword: Boolean = false,
    val isExpired: Boolean = false,
    val remainingDownloads: Int = 10,
    val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val avatar_url: String? = null,
    val member_count: Int = 1,
    val owner_id: String? = null,
    val announcement: String? = null,
    val created_at: String? = null
)

@JsonClass(generateAdapter = true)
data class GroupMessage(
    val id: String = "",
    val group_id: String = "",
    val user_id: String = "",
    val content: String = "",
    val created_at: String? = null,
    val sender: Author? = null
)

@JsonClass(generateAdapter = true)
data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val images: List<String>? = null,
    val video_url: String? = null,
    val linked_post_id: String? = null,
    val linked_miniapp_id: String? = null,
    val created_at: String? = null,
    /** 该公告关联的红包（接口返回后再填充） */
    val red_packet: RedPacket? = null
)

@JsonClass(generateAdapter = true)
data class RedPacketClaim(
    val amount: Int = 0,
    val nickname: String? = null,
    val avatar_url: String? = null,
    val confirmed_at: String? = null
)

/** 积分红包（后端 points-proxy: redpacket_get） */
@JsonClass(generateAdapter = true)
data class RedPacket(
    val id: String = "",
    /** lucky = 拼手气红包 */
    val type: String = "lucky",
    /** active | finished | expired */
    val status: String = "",
    val sender_id: String? = null,
    val created_at: String? = null,
    val expires_at: String? = null,
    val total_count: Int = 0,
    val unit_amount: Int? = null,
    val remain_count: Int = 0,
    val total_amount: Int = 0,
    val remain_amount: Int = 0,
    /** 当前用户是否已评论（部分红包需评论后才能抢） */
    val has_commented: Boolean = false,
    val claims: List<RedPacketClaim>? = null,
    val my_claim: RedPacketClaim? = null
) {
    val isGrabbable: Boolean
        get() = status == "active" && my_claim == null && remain_count > 0

    val statusText: String
        get() = when {
            my_claim != null -> "已领取 ${my_claim.amount} 积分"
            status == "expired" -> "红包已过期"
            remain_count <= 0 || status == "finished" -> "红包已抢完"
            else -> "还剩 $remain_count/$total_count 个，共 $total_amount 积分"
        }
}

/** 抢红包返回结果（需打开 auth_url 完成积分发放） */
@JsonClass(generateAdapter = true)
data class RedPacketGrabResult(
    @Json(name = "claim_id") val claim_id: String = "",
    val amount: Int = 0,
    @Json(name = "auth_url") val auth_url: String = ""
)
