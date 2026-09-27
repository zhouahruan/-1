package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MD3 UI 扩展图形库（Material Design 3 Extended Graphic Library）
 *
 * 本文件是「无限社区」的图标与图形统一入口，基于 Compose Material 3 +
 * `androidx.compose.material.icons.extended` 扩展图形包实现：
 *
 * 1. [Md3Icons]       —— 语义化图标注册表，全应用不再散落 import 具体图标；
 * 2. [Md3FileBadge]   —— 文件类型的「图标 + MD3 语义色」组合；
 * 3. [Md3IconTile] / [Md3FileIcon] —— 图标底板与文件类型图标；
 * 4. [Md3InfoBanner] / [Md3EmptyState] / [Md3CoverPlaceholder] / [Md3TagChip] —— MD3 风格图形组件。
 *
 * 新增界面时请优先复用这里的图形元素，保证整套 UI 视觉一致。
 */
object Md3Icons {

    /** 主框架底部导航图标（选中：Filled / 未选中：Outlined）。 */
    object Nav {
        val homeSelected: ImageVector = Icons.Filled.Home
        val home: ImageVector = Icons.Outlined.Home
        val discoverSelected: ImageVector = Icons.Filled.Explore
        val discover: ImageVector = Icons.Outlined.Explore
        val cloudDriveSelected: ImageVector = Icons.Filled.Cloud
        val cloudDrive: ImageVector = Icons.Outlined.Cloud
        val searchSelected: ImageVector = Icons.Filled.Search
        val search: ImageVector = Icons.Outlined.Search
        val profileSelected: ImageVector = Icons.Filled.Person
        val profile: ImageVector = Icons.Outlined.Person
    }

    /** 文件与云盘图形（扩展图形库的核心：文件类型识别）。 */
    object File {
        val folder: ImageVector = Icons.Filled.Folder
        val folderOpen: ImageVector = Icons.Outlined.FolderOpen
        val archive: ImageVector = Icons.Outlined.FolderZip
        val pdf: ImageVector = Icons.Outlined.PictureAsPdf
        val code: ImageVector = Icons.Outlined.Code
        val image: ImageVector = Icons.Outlined.Image
        val video: ImageVector = Icons.Outlined.Movie
        val audio: ImageVector = Icons.Outlined.MusicNote
        val document: ImageVector = Icons.Outlined.Description
        val sheet: ImageVector = Icons.Outlined.TableChart
        val slide: ImageVector = Icons.Outlined.Slideshow
        val app: ImageVector = Icons.Outlined.Android
        val unknown: ImageVector = Icons.Outlined.InsertDriveFile
        val download: ImageVector = Icons.Outlined.CloudDownload
        val upload: ImageVector = Icons.Outlined.CloudUpload
        val offline: ImageVector = Icons.Outlined.CloudOff
    }

    /** 操作类图形。 */
    object Action {
        val back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
        val share: ImageVector = Icons.Outlined.Share
        val refresh: ImageVector = Icons.Outlined.Refresh
        val add: ImageVector = Icons.Filled.Add
        val search: ImageVector = Icons.Outlined.Search
        val send: ImageVector = Icons.AutoMirrored.Filled.Send
        val delete: ImageVector = Icons.Filled.Delete
        val copy: ImageVector = Icons.Outlined.ContentCopy
        val chevronRight: ImageVector = Icons.Outlined.ChevronRight
        val play: ImageVector = Icons.Filled.PlayArrow
        val pause: ImageVector = Icons.Filled.Pause
        val replay: ImageVector = Icons.Filled.Replay
        val volumeOn: ImageVector = Icons.Outlined.VolumeUp
        val volumeOff: ImageVector = Icons.Outlined.VolumeOff
        val speed: ImageVector = Icons.Outlined.Speed
        val logout: ImageVector = Icons.AutoMirrored.Filled.Logout
        val link: ImageVector = Icons.Outlined.Link
    }

    /** 状态 / 提示类图形。 */
    object Status {
        val announcement: ImageVector = Icons.Filled.Campaign
        val tip: ImageVector = Icons.Outlined.Lightbulb
        val discussion: ImageVector = Icons.Outlined.Forum
        val group: ImageVector = Icons.Outlined.Group
        val security: ImageVector = Icons.Outlined.Security
        val info: ImageVector = Icons.Outlined.Info
        val feedback: ImageVector = Icons.Outlined.Feedback
        val empty: ImageVector = Icons.Outlined.Inbox
        val offline: ImageVector = Icons.Outlined.WifiOff
        val error: ImageVector = Icons.Outlined.ErrorOutline
    }

    /** 内容互动类图形（选中 / 未选中成对出现）。 */
    object Content {
        val liked: ImageVector = Icons.Filled.Favorite
        val like: ImageVector = Icons.Outlined.FavoriteBorder
        val favorited: ImageVector = Icons.Filled.Bookmark
        val favorite: ImageVector = Icons.Outlined.BookmarkBorder
        val comment: ImageVector = Icons.Outlined.ChatBubbleOutline
        val view: ImageVector = Icons.Outlined.Visibility
        val redPacket: ImageVector = Icons.Filled.CardGiftcard
    }
}

/** 文件类型图形样式：图标 + MD3 语义色 + 中文类型名。 */
data class Md3FileBadge(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val label: String
)

/**
 * 按文件名（后缀）解析出对应的 MD3 文件图形与语义色。
 * 未识别的类型回退到通用文件图标。
 */
@Composable
fun md3FileBadge(fileName: String): Md3FileBadge {
    val scheme = MaterialTheme.colorScheme
    val name = fileName.lowercase()
    return when {
        name.endsWithAny(".pdf") ->
            Md3FileBadge(Md3Icons.File.pdf, scheme.errorContainer, scheme.onErrorContainer, "PDF 文档")

        name.endsWithAny(".zip", ".rar", ".7z", ".tar", ".gz", ".bz2") ->
            Md3FileBadge(Md3Icons.File.archive, scheme.tertiaryContainer, scheme.onTertiaryContainer, "压缩包")

        name.endsWithAny(".kt", ".kts", ".java", ".json", ".xml", ".js", ".ts", ".py", ".html", ".css", ".md", ".yml", ".yaml", ".gradle") ->
            Md3FileBadge(Md3Icons.File.code, scheme.secondaryContainer, scheme.onSecondaryContainer, "代码文件")

        name.endsWithAny(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".svg", ".heic") ->
            Md3FileBadge(Md3Icons.File.image, scheme.primaryContainer, scheme.onPrimaryContainer, "图片")

        name.endsWithAny(".mp4", ".mkv", ".mov", ".avi", ".webm", ".flv", ".wmv") ->
            Md3FileBadge(Md3Icons.File.video, scheme.primaryContainer, scheme.onPrimaryContainer, "视频")

        name.endsWithAny(".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a") ->
            Md3FileBadge(Md3Icons.File.audio, scheme.tertiaryContainer, scheme.onTertiaryContainer, "音频")

        name.endsWithAny(".doc", ".docx", ".rtf", ".txt") ->
            Md3FileBadge(Md3Icons.File.document, scheme.secondaryContainer, scheme.onSecondaryContainer, "文档")

        name.endsWithAny(".xls", ".xlsx", ".csv") ->
            Md3FileBadge(Md3Icons.File.sheet, scheme.secondaryContainer, scheme.onSecondaryContainer, "表格")

        name.endsWithAny(".ppt", ".pptx") ->
            Md3FileBadge(Md3Icons.File.slide, scheme.tertiaryContainer, scheme.onTertiaryContainer, "演示文稿")

        name.endsWithAny(".apk", ".aab", ".ipa") ->
            Md3FileBadge(Md3Icons.File.app, scheme.primaryContainer, scheme.onPrimaryContainer, "应用安装包")

        else ->
            Md3FileBadge(Md3Icons.File.unknown, scheme.surfaceVariant, scheme.onSurfaceVariant, "文件")
    }
}

/** 带 MD3 语义底板的图标方块。 */
@Composable
fun Md3IconTile(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        color = containerColor,
        shape = shape,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/** 文件类型图标：直接传入文件名即可获得正确的 MD3 扩展图形与配色。 */
@Composable
fun Md3FileIcon(
    fileName: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    val badge = md3FileBadge(fileName)
    Md3IconTile(
        icon = badge.icon,
        contentDescription = badge.label,
        modifier = modifier,
        size = size,
        iconSize = iconSize,
        containerColor = badge.containerColor,
        contentColor = badge.contentColor
    )
}

/** MD3 提示条：图标 + 说明文字，替代纯 emoji 文案。 */
@Composable
fun Md3InfoBanner(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

/** 统一空状态：圆形图标底板 + 标题 + 描述（可选操作区）。 */
@Composable
fun Md3EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}

/** MD3 品牌渐变画笔：用于封面、头图等装饰性图形。 */
@Composable
fun md3BrandGradient(alpha: Float = 1f): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.linearGradient(
        listOf(
            scheme.primary.copy(alpha = alpha),
            scheme.tertiary.copy(alpha = alpha)
        )
    )
}

/** 渐变封面占位图：无封面时用扩展图形 + 渐变底替代空白图片。 */
@Composable
fun Md3CoverPlaceholder(
    icon: ImageVector,
    label: String? = null,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(md3BrandGradient(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
            if (!label.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** MD3 标签胶囊：可带前置扩展图标。 */
@Composable
fun Md3TagChip(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun String.endsWithAny(vararg suffixes: String): Boolean =
    suffixes.any { suffix -> endsWith(suffix) }
