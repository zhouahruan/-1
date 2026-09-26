package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Announcement
import com.example.data.model.RedPacket
import com.example.data.model.ShareInfo
import com.example.ui.components.RedPacketCard
import com.example.ui.MainViewModel

@Composable
fun CreatePostDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tagInput by remember { mutableStateOf("Android") }
    var imageUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发布新帖子", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("帖子标题") },
                    placeholder = { Text("输入引人注目的标题...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("正文内容") },
                    placeholder = { Text("分享你的观点、经验或问题...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_content_input")
                )
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    label = { Text("分类标签 (逗号分隔)") },
                    placeholder = { Text("例如: Android, 技术分享, 架构") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("配图 URL (可选)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        val tags = tagInput.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }
                        viewModel.createPost(title, content, tags, imageUrl.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_create_post_button")
            ) {
                Text("发布")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
fun ShareCreateDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val file by viewModel.targetShareFile.collectAsState()
    val result by viewModel.lastShareResult.collectAsState()
    val context = LocalContext.current

    var password by remember { mutableStateOf("") }
    var expiresIn by remember { mutableStateOf("1d") }
    var maxDownloads by remember { mutableStateOf("10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Share, contentDescription = "分享", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("创建文件加密分享", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "分享文件: ${file?.name ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                if (result == null) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("提取密码 (留空则免密)") },
                        placeholder = { Text("可选4-8位密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maxDownloads,
                        onValueChange = { maxDownloads = it },
                        label = { Text("最大下载限制次数") },
                        placeholder = { Text("10") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("有效期:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1h" to "1小时", "1d" to "1天", "permanent" to "永久").forEach { (key, label) ->
                            FilterChip(
                                selected = expiresIn == key,
                                onClick = { expiresIn = key },
                                label = { Text(label) }
                            )
                        }
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🎉 6 位提取码已生成:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result!!.code,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "分享直链: ${result!!.shareUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (result == null) {
                Button(
                    onClick = {
                        val maxD = maxDownloads.toIntOrNull() ?: 10
                        viewModel.confirmCreateShare(password.ifBlank { null }, maxD, expiresIn)
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("立即生成提取码")
                }
            } else {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("雾点提取码", "【雾点社区云盘】提取码: ${result!!.code}，链接: ${result!!.shareUrl}")
                        clipboard.setPrimaryClip(clip)
                        viewModel.showToast("提取码及链接已复制到剪贴板！")
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "复制", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制并完成")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun ShareExtractDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var shareInfo by remember { mutableStateOf<ShareInfo?>(null) }
    var downloadDirectUrl by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = "提取", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("云盘文件提取", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.trim()
                        shareInfo = null
                        downloadDirectUrl = null
                    },
                    label = { Text("6位文件提取码") },
                    placeholder = { Text("例如: 7k2M9p") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("extract_code_input")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("提取密码 (若无则留空)") },
                    placeholder = { Text("可选密码") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (shareInfo != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📁 文件名称: ${shareInfo!!.fileName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            if (shareInfo!!.remainingDownloads >= 0) Text("📦 剩余下载额度: ${shareInfo!!.remainingDownloads} 次", style = MaterialTheme.typography.bodySmall)
                            if (downloadDirectUrl != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("🔗 已加入下载队列", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeInput.isNotBlank()) {
                        if (shareInfo == null) {
                            viewModel.extractFileByCode(codeInput, passwordInput) { info ->
                                shareInfo = info
                            }
                        } else {
                            viewModel.downloadShareLink(codeInput, passwordInput) { url ->
                                val parsed = Uri.parse(url)
                                if (parsed.scheme != "https") {
                                    viewModel.showToast("下载地址无效，请重试")
                                } else {
                                    try {
                                        val request = DownloadManager.Request(parsed)
                                            .setTitle(shareInfo?.fileName ?: "共享文件")
                                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, shareInfo?.fileName?.replace(Regex("[/\\\\]"), "_") ?: "共享文件")
                                        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        manager.enqueue(request)
                                        downloadDirectUrl = url
                                        viewModel.showToast("已加入下载队列")
                                    } catch (e: Exception) {
                                        viewModel.showToast("无法保存文件：${e.message}")
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = codeInput.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("submit_extract_button")
            ) {
                Text(if (shareInfo == null) "查询提取" else "开始直链下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("取消")
            }
        }
    )
}

@Composable
fun FeedbackDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf("suggestion") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交意见与反馈", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("反馈类型:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("suggestion" to "功能建议", "bug" to "缺陷报错", "other" to "其他").forEach { (key, label) ->
                        FilterChip(
                            selected = type == key,
                            onClick = { type = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("反馈主题") },
                    placeholder = { Text("简述您遇到的问题或建议") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("详细描述") },
                    placeholder = { Text("请详细说明复现步骤或期望效果...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("联系方式 (手机/邮箱)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        viewModel.submitFeedback(type, title, content, contact.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("提交")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                Text("取消")
            }
        }
    )
}

/**
 * 公告详情弹窗：展示公告全文，若该公告挂载了积分红包则直接展示红包卡片。
 */
@Composable
fun AnnouncementDialog(
    announcement: Announcement,
    packet: RedPacket?,
    packetLoading: Boolean,
    packetGrabbing: Boolean,
    onGrab: (RedPacket) -> Unit,
    onOpenLinkedPost: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(announcement.title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = announcement.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!announcement.linked_post_id.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onOpenLinkedPost(announcement.linked_post_id) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("查看相关帖子")
                    }
                }

                when {
                    packetLoading -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在查询红包...", style = MaterialTheme.typography.bodySmall)
                    }
                    packet != null -> RedPacketCard(
                        packet = packet,
                        grabbing = packetGrabbing,
                        onGrab = onGrab
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("关闭")
            }
        }
    )
}
