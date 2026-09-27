package com.example.zhshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhshop.data.model.*

@Composable
fun CreatePostDialog(
    boards: List<ForumBoard>,
    onDismiss: () -> Unit,
    onSubmit: (boardId: String, title: String, content: String) -> Unit
) {
    var selectedBoardId by remember { mutableStateOf(boards.firstOrNull()?.id ?: "") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发布极客新话题") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("选择板块:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    boards.forEach { b ->
                        FilterChip(
                            selected = selectedBoardId == b.id,
                            onClick = { selectedBoardId = b.id },
                            label = { Text(b.name, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("正文内容 (支持 Markdown 语法)") },
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSubmit(selectedBoardId, title, content)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("发布帖子")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun SponsorApplyDialog(
    onDismiss: () -> Unit,
    onSubmit: (channel: String, amount: Double, note: String) -> Unit
) {
    var channel by remember { mutableStateOf("微信赞助") }
    var amountText by remember { mutableStateOf("20") }
    var note by remember { mutableStateOf("支持 ZHShop 独立穿戴生态建设") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("赞助与商业支持")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("赞助方式:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("微信赞助", "支付宝", "爱发电").forEach { ch ->
                        FilterChip(
                            selected = channel == ch,
                            onClick = { channel = ch },
                            label = { Text(ch) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("赞助金额 (CNY)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("赞助寄语或留言") },
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "注: 赞助金将直接用于流云 CDN 边缘节点开销与 APK 解析服务器带宽。赞助后将发放「金牌赞助人」勋章并在专属榜单公示。",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 10.0
                    onSubmit(channel, amt, note)
                }
            ) {
                Text("提交凭证")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("返回")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ApkParserDialog(
    apkResult: ApkAnalysisResult?,
    isAnalyzing: Boolean,
    onDismiss: () -> Unit,
    onParse: (String) -> Unit
) {
    var inputUrl by remember { mutableStateOf("https://backend.appmiaoda.com/files/aurora_watch.apk") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("APK 解析工具")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("输入 APK 下载链接或文件地址:", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Button(
                    onClick = { onParse(inputUrl) },
                    enabled = !isAnalyzing && inputUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("解析中...")
                    } else {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("开始解析")
                    }
                }

                apkResult?.let { res ->
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("解析结果", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("应用名称: ${res.appName}", fontWeight = FontWeight.Bold)
                            Text("包名: ${res.packageName}", style = MaterialTheme.typography.bodySmall)
                            Text("版本: v${res.versionName} (${res.versionCode})", style = MaterialTheme.typography.bodySmall)
                            Text("SDK 要求: Min ${res.minSdk} ~ Target ${res.targetSdk}", style = MaterialTheme.typography.bodySmall)
                            Text("主 Activity: ${res.launchActivity}", style = MaterialTheme.typography.bodySmall)
                            Text("文件大小: ${(res.fileSize / 1024 / 1024)} MB", style = MaterialTheme.typography.bodySmall)
                            Text("MD5 摘要: ${res.apkMd5.take(16)}...", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("声明权限列表:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            res.permissions.forEach { perm ->
                                Text("• ${perm.replace("android.permission.", "")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiuYunDialog(
    liuYunInfo: LiuYunInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("云端存储服务")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("存储空间配额", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${liuYunInfo.usedStorageMb} MB / ${liuYunInfo.storageQuotaMb} MB", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearWavyProgressIndicator(
                            progress = { liuYunInfo.usedStorageMb.toFloat() / liuYunInfo.storageQuotaMb.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("服务积分: ${liuYunInfo.points}", style = MaterialTheme.typography.labelSmall)
                            Text("有效期至: ${liuYunInfo.vipExpiry}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text("开发文档", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                liuYunInfo.docs.forEach { doc ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(doc.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(doc.summary, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("知道了")
            }
        }
    )
}

@Composable
fun ClientUpdateDialog(
    clientUpdateInfo: ClientUpdateInfo,
    onDismiss: () -> Unit,
    onUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("发现客户端新版本")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "最新版本: ${clientUpdateInfo.latestVersion}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "当前运行环境已适配 Supabase 网关高可用架构与流云直传。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("更新日志:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = clientUpdateInfo.changelog,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdate) {
                Text("立即自更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后提醒")
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    currentUsername: String,
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (username: String, bio: String) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var bio by remember { mutableStateOf(currentBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑个人资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("极客昵称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("个性签名 / 开发者简介") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, bio) },
                enabled = username.isNotBlank()
            ) {
                Text("保存更新")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun AgreementViewerDialog(
    onDismiss: () -> Unit,
    onRevokeConsent: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: 免责声明, 1: 隐私政策
    var showRevokeConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("平台协议与隐私政策")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("免责声明") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("隐私政策") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (selectedTab == 0) AgreementContent.DISCLAIMER_DETAILS else AgreementContent.PRIVACY_POLICY_DETAILS,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.3f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("当前状态：已合规授权", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(
                        onClick = { showRevokeConfirm = true }
                    ) {
                        Text("撤销授权", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("已知晓并关闭")
            }
        }
    )

    if (showRevokeConfirm) {
        AlertDialog(
            onDismissRequest = { showRevokeConfirm = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("确认撤回协议授权？") },
            text = {
                Text("撤回同意后，根据国家法律法规要求，您将无法继续访问应用商店的下载与社区功能，并将立即退出应用。确定撤回吗？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeConfirm = false
                        onRevokeConsent()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认撤回")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRevokeConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun AuthDialog(
    onDismiss: () -> Unit,
    onLogin: (email: String, pass: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onRegister: (email: String, pass: String, username: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRegisterMode) "注册 ZHShop 账户" else "登录 ZHShop 账户")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMsg = null },
                        label = { Text("用户名 / 昵称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = null },
                    label = { Text("邮箱地址") },
                    placeholder = { Text("例如 user@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMsg = null },
                    label = { Text("密码 (不少于6位)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )

                errorMsg?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            errorMsg = null
                        },
                        enabled = !isLoading
                    ) {
                        Text(if (isRegisterMode) "已有账号？去登录" else "没有账号？免费注册")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isLoading,
                onClick = {
                    if (isRegisterMode) {
                        if (email.isBlank() || password.isBlank() || username.isBlank()) {
                            errorMsg = "请完整填写用户名、邮箱和密码"
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMsg = "密码长度至少需要 6 位"
                            return@Button
                        }
                        isLoading = true
                        errorMsg = null
                        onRegister(email.trim(), password, username.trim()) { success, msg ->
                            isLoading = false
                            if (!success) {
                                errorMsg = msg
                            }
                        }
                    } else {
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "请输入邮箱和密码"
                            return@Button
                        }
                        isLoading = true
                        errorMsg = null
                        onLogin(email.trim(), password) { success, msg ->
                            isLoading = false
                            if (!success) {
                                errorMsg = msg
                            }
                        }
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (isRegisterMode) "立即注册" else "立即登录")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("取消")
            }
        }
    )
}

