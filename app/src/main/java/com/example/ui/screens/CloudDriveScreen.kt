package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserServer
import com.example.ui.MainViewModel
import com.example.ui.components.CloudFileItem
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.components.FolderItem
import com.example.ui.components.GuestLoginScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDriveScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val server by viewModel.userServer.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val files by viewModel.files.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()
    val cloudError by viewModel.cloudError.collectAsState()
    val cloudLoading by viewModel.cloudLoading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // 云盘属于个人空间，未登录时引导登录（浏览帖子不受影响）
    if (currentUser == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            GuestLoginScreen(
                title = "登录后使用个人云盘",
                description = "每个账号都有免费云盘空间，可以上传文件并生成 6 位提取码分享给他人。",
                onLogin = { viewModel.showLoginDialog.value = true },
                icon = Icons.Filled.CloudQueue,
                modifier = Modifier.weight(1f)
            )
            // 提取他人分享的文件不需要登录
            TextButton(
                onClick = { viewModel.showShareExtractDialog.value = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("guest_extract_button")
            ) {
                Text("我有提取码，直接提取他人分享的文件")
            }
        }
        return
    }

    val currentServer = server ?: UserServer()
    val usedPct = currentServer.usedPercentage

    val progressColor = when {
        usedPct >= 0.95f -> MaterialTheme.colorScheme.error
        usedPct >= 0.80f -> Color(0xFFD97706) // Amber warning
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentServer.name, fontWeight = FontWeight.Bold)
                        Text(if (server == null) "登录后查看实时存储容量" else "个人云盘 • ${currentServer.formattedQuota} 配额", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                },
                navigationIcon = {
                    if (currentFolderId != null) {
                        IconButton(
                            onClick = { viewModel.loadCloudDrive(null) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回根目录")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.showShareExtractDialog.value = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("extract_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = "提取文件",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadCloudDrive(currentFolderId) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("drive_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (cloudLoading) {
                item { CircularProgressIndicator() }
            }
            if (cloudError != null) {
                item { Text("云盘加载失败：$cloudError", color = MaterialTheme.colorScheme.error) }
            }
            // Storage Quota Card
            if (server != null) item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudQueue,
                                        contentDescription = "云盘",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "存储空间配额",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "已用 ${currentServer.formattedUsed} / 总共 ${currentServer.formattedQuota}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${(usedPct * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { usedPct },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        if (usedPct >= 0.80f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (usedPct >= 0.95f) "⚠️ 云盘容量接近耗尽，请及时清理文件！" else "⚡ 云盘容量已达到 80%，建议合理规划空间",
                                style = MaterialTheme.typography.labelSmall,
                                color = progressColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Extraction Quick Action Banner
            item {
                Card(
                    onClick = { viewModel.showShareExtractDialog.value = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = "提取",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "收到了文件提取码？",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "点击在此输入 6 位提取码与密码，直达下载",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Current Directory Navigation
            if (server != null) item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentFolderId == null) "全部文件 (根目录)" else "当前文件夹",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Folders (only show in root)
            if (currentFolderId == null && folders.isNotEmpty()) {
                items(folders, key = { it.id }) { folder ->
                    FolderItem(
                        folder = folder,
                        onClick = { f -> viewModel.loadCloudDrive(f.id) }
                    )
                }
            }

            // Files
            if (server != null && !cloudLoading && cloudError == null && files.isEmpty() && (currentFolderId != null || folders.isEmpty())) {
                item {
                    EmptyPlaceholder(
                        title = "文件夹为空",
                        description = "当前目录下没有存储任何文件",
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.InsertDriveFile,
                                contentDescription = "无文件",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            } else {
                items(files, key = { it.id }) { file ->
                    CloudFileItem(
                        file = file,
                        onShareClick = { f ->
                            viewModel.prepareShareFile(f)
                        }
                    )
                }
            }
        }
    }
}
