package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.components.WudianVideoPlayer

/**
 * 全屏视频播放页。
 *
 * 使用自研播放器（[WudianVideoPlayer]）：内核为 MediaPlayer + SurfaceView 直驱，
 * 控制条由 Compose 自己绘制，不再依赖系统 VideoView / MediaController。
 * 相比此前的 WebView 方案，也避免了 HTML5 视频层无法合成导致「只有声音、画面全黑」的问题。
 */
@Composable
fun VideoPlayerScreen(
    url: String,
    title: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    WudianVideoPlayer(
        url = url,
        title = title,
        onBack = { viewModel.navigateBack() },
        modifier = modifier
    )
}
