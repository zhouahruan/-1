package com.example.zhshop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.zhshop.data.model.AppCategory
import com.example.zhshop.data.model.AppItem
import com.example.zhshop.data.model.LeaderboardItem
import com.example.zhshop.ui.components.ZhAppIcon
import com.example.zhshop.ui.components.ZhCategoryIcon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryLeaderboardScreen(
    categories: List<AppCategory>,
    leaderboardItems: List<LeaderboardItem>,
    leaderboardType: String,
    onLeaderboardTypeChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onAppClick: (AppItem) -> Unit,
    onDownloadClick: (AppItem) -> Unit,
    downloadProgress: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    var selectedTopTab by remember { mutableStateOf(1) } // 0: 分类, 1: 排行

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("category_leaderboard_screen")
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedTopTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTopTab == 0,
                onClick = { selectedTopTab = 0 },
                text = { Text("分类", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTopTab == 1,
                onClick = { selectedTopTab = 1 },
                text = { Text("排行", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTopTab == 0) {
            // Category Catalog Grid
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(categories) { cat ->
                    Card(
                        onClick = { onCategoryClick(cat.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (cat.id) {
                                        "c_wear" -> Icons.Rounded.Watch
                                        "c_tool" -> Icons.Rounded.Build
                                        "c_social" -> Icons.Rounded.Chat
                                        "c_media" -> Icons.Rounded.PlayArrow
                                        "c_game" -> Icons.Rounded.Gamepad
                                        else -> Icons.Rounded.Settings
                                    },
                                    contentDescription = cat.name,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cat.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "${cat.appCount} 款",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Leaderboard Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Leaderboard Type Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "hot" to "🔥 热门榜",
                        "download" to "⚡ 下载榜",
                        "upload" to "👑 发布榜",
                        "sponsor" to "💖 赞助榜",
                        "rating" to "★ 好评榜",
                        "new" to "🚀 新品榜"
                    ).forEach { (typeKey, label) ->
                        FilterChip(
                            selected = leaderboardType == typeKey,
                            onClick = { onLeaderboardTypeChange(typeKey) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(leaderboardItems) { item ->
                        val isTop3 = item.rank <= 3
                        val rankColor = when (item.rank) {
                            1 -> Color(0xFFFFD700) // Gold
                            2 -> Color(0xFFC0C0C0) // Silver
                            3 -> Color(0xFFCD7F32) // Bronze
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }

                        val progress = downloadProgress[item.app.id]
                        val isInstalled = progress != null && progress >= 1f
                        val isDownloading = progress != null && progress < 1f

                        ElevatedCard(
                            onClick = { onAppClick(item.app) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rank Badge
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isTop3) rankColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${item.rank}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isTop3) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                ZhAppIcon(
                                    iconUrl = item.app.iconUrl,
                                    appName = item.app.name,
                                    modifier = Modifier.size(50.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.app.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${item.app.categoryName} • ${item.metricLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                OutlinedButton(
                                    onClick = { if (!isInstalled && !isDownloading) onDownloadClick(item.app) },
                                    modifier = Modifier.height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(17.dp),
                                    enabled = !isDownloading
                                ) {
                                    if (isDownloading) {
                                        CircularWavyProgressIndicator(
                                            progress = { progress ?: 0f },
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (isInstalled) {
                                        Text(
                                            text = "打开",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "下载",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (isDownloading) {
                                LinearWavyProgressIndicator(
                                    progress = { progress ?: 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .padding(horizontal = 12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
