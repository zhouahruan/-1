package com.example.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.MainViewModel

private const val MAX_SCALE = 5f

/**
 * 全屏图片查看器：左右滑动切换、双指缩放、双击放大/还原、保存到相册、分享链接。
 */
@Composable
fun ImageViewerScreen(
    images: List<String>,
    initialIndex: Int,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) {
        viewModel.navigateBack()
        return
    }

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, images.lastIndex)
    ) { images.size }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showChrome by remember { mutableStateOf(true) }

    // 切换图片时重置缩放
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("image_viewer_screen")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 8.dp
        ) { page ->
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, MAX_SCALE)
                offset = if (scale <= 1f) Offset.Zero else offset + panChange
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(transformState)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showChrome = !showChrome },
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = images[page],
                    contentDescription = "图片 ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }
        }

        if (showChrome) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("image_viewer_close")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { saveImageToGallery(context, images[pagerState.currentPage]) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("image_viewer_save")
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = "保存到相册", tint = Color.White)
                }
                IconButton(
                    onClick = { shareImageLink(context, images[pagerState.currentPage]) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("image_viewer_share")
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "分享", tint = Color.White)
                }
            }

            Text(
                text = "双指缩放 · 双击放大 · 左右滑动切换",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

/** 通过系统下载器保存到相册（不需要额外存储权限） */
private fun saveImageToGallery(context: Context, url: String) {
    try {
        val fileName = "wudian_${System.currentTimeMillis()}.jpg"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("保存图片")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName)
            .setMimeType("image/jpeg")
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "已开始保存到相册", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareImageLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        context.startActivity(Intent.createChooser(intent, "分享图片链接"))
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show()
    }
}
