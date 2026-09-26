package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.SupabaseConfig
import com.example.data.model.MiniApp
import com.example.data.model.PostAttachment
import com.example.data.model.PostMountedFile
import com.example.data.model.ReferencedPost

/** 用系统能力打开外部链接 / 下载直链 */
fun openExternal(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开该链接", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 帖子附件区：支持图片、视频、外部链接、云盘文件等多种格式。
 */
@Composable
fun PostAttachmentBlock(
    attachments: List<PostAttachment>?,
    onPlayVideo: (String) -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenCloudFile: (String) -> Unit,
    onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    maxImages: Int = 1,
    mediaHeight: Dp = 180.dp
) {
    val items = attachments?.filter { it.url.isNotBlank() } ?: return
    if (items.isEmpty()) return

    val allImages = items.filter { it.type == "image" }.map { it.url }
    val images = items.filter { it.type == "image" }.take(maxImages)
    val videos = items.filter { it.type == "video" }
    val links = items.filter { it.type == "link" }
    val files = items.filter { it.type == "cloud_file" || it.type == "file" }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        images.forEach { img ->
            AsyncImage(
                model = img.url,
                contentDescription = img.name ?: "帖子配图",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mediaHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val index = allImages.indexOf(img.url).coerceAtLeast(0)
                        onImageClick(allImages, index)
                    }
                    .testTag("post_image_attachment")
            )
        }

        videos.forEach { video ->
            VideoAttachmentCard(
                url = video.url,
                name = video.name,
                height = mediaHeight,
                onPlay = onPlayVideo
            )
        }

        links.forEach { link ->
            LinkAttachmentCard(url = link.url, name = link.name, onOpen = onOpenLink)
        }

        files.forEach { file ->
            AttachmentRow(
                icon = Icons.Outlined.InsertDriveFile,
                title = file.name ?: "云盘文件",
                subtitle = "点击打开下载",
                onClick = { onOpenCloudFile(file.url) }
            )
        }
    }
}

@Composable
private fun VideoAttachmentCard(
    url: String,
    name: String?,
    height: Dp,
    onPlay: (String) -> Unit
) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPlay(url) }
            .testTag("post_video_attachment")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(9999.dp),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "播放视频",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name?.ifBlank { null } ?: "视频",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LinkAttachmentCard(url: String, name: String?, onOpen: (String) -> Unit) {
    val host = runCatching { Uri.parse(url).host }.getOrNull() ?: url
    AttachmentRow(
        icon = Icons.Outlined.Link,
        title = name?.ifBlank { null } ?: host,
        subtitle = url.take(60),
        onClick = { onOpen(url) }
    )
}

@Composable
fun AttachmentRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "打开",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/** 帖子挂载的轻应用（红包、小游戏等互动玩法由此进入） */
@Composable
fun MiniAppMountCard(
    miniApp: MiniApp,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onLaunch,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("post_miniapp_mount")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!miniApp.icon_url.isNullOrBlank()) {
                AsyncImage(
                    model = miniApp.icon_url,
                    contentDescription = miniApp.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = miniApp.name.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = miniApp.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "挂载轻应用",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = miniApp.description?.ifBlank { null } ?: "点击立即游玩",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "运行",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** 引用的帖子（转帖） */
@Composable
fun ReferencedPostCard(
    ref: ReferencedPost,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("post_referenced_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(avatarUrl = ref.ref_author_avatar, name = ref.authorName, size = 22.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ref.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "引用",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ref.ref_title?.ifBlank { null } ?: "原帖已不可见",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!ref.ref_summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ref.ref_summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** 帖子挂载的云盘文件 */
@Composable
fun MountedFilesBlock(
    files: List<PostMountedFile>?,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val list = files?.mapNotNull { it.file }.orEmpty()
    if (list.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { file ->
            AttachmentRow(
                icon = Icons.Outlined.CloudQueue,
                title = file.name.ifBlank { "云盘文件" },
                subtitle = "${file.formattedSize} · 挂载自云盘",
                onClick = { onOpenFile("${SupabaseConfig.WEB_BASE_URL}/d/${file.id}") }
            )
        }
    }
}

