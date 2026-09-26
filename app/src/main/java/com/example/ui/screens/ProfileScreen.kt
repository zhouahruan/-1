package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.GuestLoginScreen
import com.example.ui.components.RiskStatusChip
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentUserState by viewModel.currentUser.collectAsState()
    val user = currentUserState

    // 未登录：只展示登录引导；浏览帖子等公开内容不受影响
    if (user == null) {
        GuestLoginScreen(
            title = "未登录",
            description = "登录后可以发帖、评论、使用个人云盘与分享文件；未登录也能正常浏览社区内容。",
            onLogin = { viewModel.showLoginDialog.value = true }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "退出登录", tint = MaterialTheme.colorScheme.primary)
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserAvatar(
                                avatarUrl = user.avatar_url,
                                name = user.displayName,
                                size = 64.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.displayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    RoleBadge(
                                        role = user.role,
                                        isSuperAdmin = user.isSuperAdmin,
                                        isOfficial = user.is_official
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "账号: ${user.phone ?: "手机号未绑定"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                RiskStatusChip(status = user.risk_status)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = user.bio ?: "热爱开源，探索雾点社区的精彩",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    }
                }
            }

            // Admin Governance Panel (if admin)
            if (user.isAdmin) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = "管理权限",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (user.isSuperAdmin) "超级管理特权控制台" else "普通管理运维面板",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (user.isSuperAdmin) {
                                Text(
                                    text = "• 唯一权限：任命或撤销其他用户管理员身份\n• 唯一权限：删除其他管理员账号\n• 全站最高治理权限（手机尾号 34549 独占认证）",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            } else {
                                Text(
                                    text = "• 具备违规帖子/评论清理与用户反馈处理权限\n• 允许标记普通用户账号风控异常\n• 严格受限：禁止修改/删除管理员账号，禁止封禁超管",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.toggleUserRisk(user.id, user.risk_status)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text("标记当前账号风控状态 (当前: ${user.risk_status})")
                            }
                        }
                    }
                }
            }

            // General Features Menu
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        ProfileMenuItem(
                            icon = Icons.Outlined.CloudDownload,
                            title = "提取云盘分享文件",
                            subtitle = "输入 6 位分享码即可免密极速下载",
                            onClick = { viewModel.showShareExtractDialog.value = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ProfileMenuItem(
                            icon = Icons.Outlined.Feedback,
                            title = "用户反馈与建议工单",
                            subtitle = "直接提交功能建议或缺陷报告至后端",
                            onClick = { viewModel.showFeedbackDialog.value = true }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ProfileMenuItem(
                            icon = Icons.Outlined.Info,
                            title = "关于雾点社区与 API 协议",
                            subtitle = "基于 Supabase 云原生与 Material Design 3 青色架构",
                            onClick = {
                                viewModel.showToast("雾点社区 v1.0.0 (M3 Cyan Edition)")
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ProfileMenuItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "退出登录",
                            subtitle = "清除本机保存的登录状态，返回登录/注册页",
                            onClick = { viewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "查看",
            tint = MaterialTheme.colorScheme.outline
        )
    }
}
