package com.example.zhshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlin.math.abs

/**
 * 智能应用图标组件：
 * 1. 自动注入标准移动端 User-Agent 与缓存策略，解决部分 OSS 防盗链或 403 异常；
 * 2. 加载中展示轻量波浪态占位；
 * 3. 当云端图标链接失效、403 或无图标时，无缝回退到基于应用名的精致 M3 动态渐变字标，绝不显示白板或空白破损方块。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ZhAppIcon(
    iconUrl: String?,
    appName: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val cleanUrl = remember(iconUrl) {
        val u = iconUrl?.trim().orEmpty()
        if (u.startsWith("http://ly.jianmo.icu")) u.replace("http://", "https://") else u
    }

    val gradientColors = remember(appName) {
        getAppGradient(appName)
    }

    val initialChar = remember(appName) {
        appName.trim().firstOrNull()?.toString()?.uppercase() ?: "A"
    }

    if (cleanUrl.isBlank()) {
        FallbackAppBadge(
            initialChar = initialChar,
            gradientColors = gradientColors,
            modifier = modifier,
            shape = shape
        )
    } else {
        val request = remember(cleanUrl) {
            ImageRequest.Builder(context)
                .data(cleanUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 ZHShop/1.1")
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = appName,
            modifier = modifier.clip(shape),
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
            },
            error = {
                FallbackAppBadge(
                    initialChar = initialChar,
                    gradientColors = gradientColors,
                    modifier = Modifier.fillMaxSize(),
                    shape = shape
                )
            }
        )
    }
}

/**
 * 优雅的字标后备占位组件
 */
@Composable
fun FallbackAppBadge(
    initialChar: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialChar,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

/**
 * 用户头像组件，解决社区与聊天中无头像时的空白
 */
@Composable
fun ZhUserAvatar(
    avatarUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val context = LocalContext.current
    val cleanUrl = avatarUrl?.trim().orEmpty()
    val initial = remember(username) {
        username.trim().firstOrNull()?.toString()?.uppercase() ?: "U"
    }
    val gradient = remember(username) {
        getAppGradient(username)
    }

    if (cleanUrl.isBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.45f).sp
            )
        }
    } else {
        val request = remember(cleanUrl) {
            ImageRequest.Builder(context)
                .data(cleanUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) ZHShop/1.1")
                .crossfade(true)
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = username,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.45f).sp
                    )
                }
            }
        )
    }
}

/**
 * 分类图标辅助组件，当分类无图时提供有质感的 M3 标识
 */
@Composable
fun ZhCategoryIcon(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when {
        categoryName.contains("社交") || categoryName.contains("通讯") -> Icons.Rounded.Person to Color(0xFF3F51B5)
        categoryName.contains("游戏") -> Icons.Rounded.Apps to Color(0xFFE91E63)
        categoryName.contains("工具") || categoryName.contains("系统") -> Icons.Rounded.Android to Color(0xFF009688)
        else -> Icons.Rounded.Apps to MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun getAppGradient(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2)),
        listOf(Color(0xFF00B4DB), Color(0xFF0083B0)),
        listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
        listOf(Color(0xFFFF416C), Color(0xFFFF4B2B)),
        listOf(Color(0xFF5C258D), Color(0xFF4389A2)),
        listOf(Color(0xFFF857A6), Color(0xFFFF5858)),
        listOf(Color(0xFF2C3E50), Color(0xFF4CA1AF)),
        listOf(Color(0xFF654EA3), Color(0xFFEAAFC8)),
        listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))
    )
    val index = abs(seed.hashCode()) % palettes.size
    return palettes[index]
}
