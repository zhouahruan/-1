package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel
import com.example.ui.ScreenDestination
import com.example.ui.components.MiniAppMountCard
import com.example.ui.components.MountedFilesBlock
import com.example.ui.components.PostAttachmentBlock
import com.example.ui.components.RedPacketCard
import com.example.ui.components.ReferencedPostCard
import com.example.ui.components.RoleBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.openExternal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val post by viewModel.currentPostDetail.collectAsState()
    val comments by viewModel.currentComments.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    // 未登录也可以浏览帖子详情与评论
    val me = currentUser
    val context = LocalContext.current
    val postRedPacket by viewModel.postRedPacket.collectAsState()
    val redPacketGrabbing by viewModel.redPacketGrabbing.collectAsState()

    var newCommentText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("帖子详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("post_detail_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val canDelete = me?.isAdmin == true || (me != null && post?.user_id == me.id)
                    if (canDelete && post != null) {
                        IconButton(
                            onClick = { viewModel.deletePost(post!!.id) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_post_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "删除帖子",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (me == null) {
                    Button(
                        onClick = { viewModel.showLoginDialog.value = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("guest_comment_login_button")
                    ) {
                        Text("登录后参与评论", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCommentText,
                            onValueChange = { newCommentText = it },
                            placeholder = { Text("发表友善评论...", fontSize = 14.sp) },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comment_input_field"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank() && post != null) {
                                    viewModel.addComment(post!!.id, newCommentText)
                                    newCommentText = ""
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("send_comment_button"),
                            enabled = newCommentText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "发送",
                                tint = if (newCommentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (post == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val p = post!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Author Header
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserAvatar(
                            avatarUrl = p.author?.avatar_url,
                            name = p.author?.displayName ?: "用户",
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = p.author?.displayName ?: "匿名用户",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RoleBadge(role = p.author?.role)
                            }
                            Text(
                                text = "发布于 ${p.created_at ?: "刚刚"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Title
                item {
                    Text(
                        text = p.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // AI Summary
                if (!p.ai_summary.isNullOrBlank()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "AI 摘要速读",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = p.ai_summary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Full Content
                item {
                    Text(
                        text = p.content,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 26.sp
                    )
                }

                // 引用帖 / 挂载轻应用 / 多格式附件（图片、视频、链接、云盘文件）
                val referenced = p.referenced_post
                val mountedApp = p.mini_app
                if (referenced != null || mountedApp != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (referenced != null) {
                                ReferencedPostCard(
                                    ref = referenced,
                                    onClick = { viewModel.navigateTo(ScreenDestination.PostDetail(referenced.ref_id)) }
                                )
                            }
                            if (mountedApp != null) {
                                MiniAppMountCard(
                                    miniApp = mountedApp,
                                    onLaunch = {
                                        viewModel.navigateTo(ScreenDestination.MiniAppSandbox(mountedApp.id, mountedApp.name))
                                    }
                                )
                            }
                        }
                    }
                }

                if (!p.attachments.isNullOrEmpty()) {
                    item {
                        PostAttachmentBlock(
                            attachments = p.attachments,
                            onPlayVideo = { url -> viewModel.navigateTo(ScreenDestination.VideoPlayer(url)) },
                            onImageClick = { list, index ->
                                viewModel.navigateTo(ScreenDestination.ImageViewer(list, index))
                            },
                            onOpenLink = { openExternal(context, it) },
                            onOpenCloudFile = { openExternal(context, it) },
                            maxImages = 9,
                            mediaHeight = 240.dp
                        )
                    }
                }

                if (!p.mounted_files.isNullOrEmpty()) {
                    item {
                        MountedFilesBlock(
                            files = p.mounted_files,
                            onOpenFile = { openExternal(context, it) }
                        )
                    }
                }

                // Tags
                if (!p.ai_tags.isNullOrEmpty()) {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            p.ai_tags.forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 积分红包（帖子挂载，来自 points-proxy redpacket_get）
                val redPacket = postRedPacket
                if (redPacket != null) {
                    item {
                        RedPacketCard(
                            packet = redPacket,
                            grabbing = redPacketGrabbing,
                            onGrab = { packet -> viewModel.grabRedPacket(packet) }
                        )
                    }
                }

                // Interaction Stats Row
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Button
                        val likeTint by animateColorAsState(
                            targetValue = if (p.isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "like"
                        )
                        TextButton(
                            onClick = {
                                if (viewModel.requireLogin("点赞")) viewModel.toggleLike(p.id)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (p.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "赞",
                                tint = likeTint
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${p.like_count} 赞", color = likeTint, fontWeight = FontWeight.Bold)
                        }

                        // Favorite Button
                        val favTint by animateColorAsState(
                            targetValue = if (p.isFavoritedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "fav"
                        )
                        TextButton(
                            onClick = {
                                if (viewModel.requireLogin("收藏")) viewModel.toggleFavorite(p.id)
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (p.isFavoritedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "收藏",
                                tint = favTint
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (p.isFavoritedByMe) "已收藏" else "收藏", color = favTint, fontWeight = FontWeight.Bold)
                        }

                        // View Count
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "阅读",
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${p.view_count} 阅读", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                // Comments Header
                item {
                    Text(
                        text = "评论 (${comments.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Comments List
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = "暂无评论，快来抢沙发吧~",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(
                                        avatarUrl = comment.author?.avatar_url,
                                        name = comment.author?.displayName ?: "访客",
                                        size = 32.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = comment.author?.displayName ?: "访客",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = comment.created_at ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = comment.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
