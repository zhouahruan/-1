package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.Md3Icons
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * 自研视频播放内核。
 *
 * 完全不使用系统 `VideoView` / `MediaController`，也不引入 ExoPlayer / Media3 等第三方库：
 * 由 [MediaPlayer] 直接驱动 [SurfaceView] 的 Surface，播放状态通过 Compose 状态对外暴露，
 * 控制条全部由 Compose 自己绘制（见 [WudianVideoPlayer]）。
 *
 * 所有调用都发生在主线程（Compose 主线程有 Looper），`prepareAsync()` 不阻塞 UI。
 */
internal class WudianVideoEngine {

    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var isEnded by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var positionMs by mutableStateOf(0)
        private set
    var durationMs by mutableStateOf(0)
        private set
    var videoWidth by mutableStateOf(0)
        private set
    var videoHeight by mutableStateOf(0)
        private set
    var speed by mutableStateOf(1f)
        private set
    var muted by mutableStateOf(false)
        private set

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var url: String? = null
    private var autoPlay = true

    val hasError: Boolean get() = errorMessage != null

    /** 视频宽高比，未就绪时为 null（此时按全屏铺满显示）。 */
    val aspectRatio: Float?
        get() = if (videoWidth > 0 && videoHeight > 0) videoWidth.toFloat() / videoHeight.toFloat() else null

    /** SurfaceView 创建 / 变化 / 销毁时调用。 */
    fun setSurface(newSurface: Surface?) {
        surface = newSurface
        val mp = player
        if (mp != null) {
            try {
                mp.setSurface(newSurface)
            } catch (_: IllegalStateException) {
                // 播放器状态已变化，忽略
            }
        } else if (newSurface != null) {
            openPlayer()
        }
    }

    /** 载入视频。Surface 尚未就绪时会先标记缓冲，Surface 就绪后自动开始。 */
    fun load(videoUrl: String, autoPlayWhenReady: Boolean = true) {
        closePlayer()
        url = videoUrl
        autoPlay = autoPlayWhenReady
        positionMs = 0
        durationMs = 0
        videoWidth = 0
        videoHeight = 0
        isEnded = false
        isPlaying = false
        errorMessage = null
        if (surface == null) {
            isBuffering = true
        } else {
            openPlayer()
        }
    }

    fun retry() {
        val current = url ?: return
        load(current, autoPlay)
    }

    private fun openPlayer() {
        val source = url ?: return
        val target = surface ?: run {
            isBuffering = true
            return
        }
        closePlayer()
        isBuffering = true
        errorMessage = null

        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
            )
            mp.setSurface(target)
            mp.setDataSource(source)
            mp.setOnPreparedListener { prepared ->
                durationMs = max(prepared.duration, 0)
                videoWidth = prepared.videoWidth
                videoHeight = prepared.videoHeight
                isBuffering = false
                applyVolume()
                if (autoPlay) {
                    prepared.start()
                    isPlaying = true
                }
                applySpeedIfPlaying()
            }
            mp.setOnCompletionListener {
                isPlaying = false
                isEnded = true
                positionMs = durationMs
            }
            mp.setOnErrorListener { _, what, extra ->
                isPlaying = false
                isBuffering = false
                errorMessage = "视频播放失败（错误码 $what/$extra）：文件不存在或格式不受支持"
                true
            }
            mp.prepareAsync()
        } catch (t: Throwable) {
            isBuffering = false
            errorMessage = "无法打开视频：${t.message ?: t::class.java.simpleName}"
            closePlayer()
        }
    }

    /** 音量：静音用 setVolume(0f, 0f)，不依赖任何第三方库。 */
    private fun applyVolume() {
        val mp = player ?: return
        val volume = if (muted) 0f else 1f
        try {
            mp.setVolume(volume, volume)
        } catch (_: Throwable) {
        }
    }

    /**
     * 倍速：MediaPlayer 通过 [PlaybackParams] 控制（API 23+）。
     * 只在正在播放时下发，避免部分机型在暂停态设置后意外自动续播；
     * 暂停期间用户改的倍速会在下次 [play] 时生效。
     */
    private fun applySpeedIfPlaying() {
        val mp = player ?: return
        val playing = try {
            mp.isPlaying
        } catch (_: Throwable) {
            false
        }
        if (!playing) return
        try {
            mp.playbackParams = PlaybackParams().setSpeed(speed)
        } catch (_: Throwable) {
        }
    }

    fun togglePlay() {
        if (isPlaying) pause() else play()
    }

    fun play() {
        val mp = player ?: return
        if (isEnded) {
            seekTo(0)
        }
        try {
            mp.start()
            isPlaying = true
            isEnded = false
        } catch (_: IllegalStateException) {
        }
        applyVolume()
        applySpeedIfPlaying()
    }

    fun pause() {
        val mp = player ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
            }
            isPlaying = false
        } catch (_: IllegalStateException) {
        }
    }

    fun seekTo(ms: Int) {
        val mp = player ?: return
        val upper = if (durationMs > 0) durationMs else max(ms, 0)
        val target = ms.coerceIn(0, upper)
        try {
            mp.seekTo(target)
            positionMs = target
            if (target < durationMs) isEnded = false
        } catch (_: IllegalStateException) {
        }
    }

    fun replay() {
        seekTo(0)
        play()
    }

    fun changeSpeed(value: Float) {
        speed = value
        applySpeedIfPlaying()
    }

    fun toggleMute() {
        muted = !muted
        applyVolume()
    }

    /** 由 UI 定时调用，同步播放进度。 */
    fun refreshPosition() {
        val mp = player ?: return
        if (errorMessage != null) return
        try {
            if (mp.isPlaying) {
                positionMs = mp.currentPosition
                if (durationMs <= 0) durationMs = max(mp.duration, 0)
            }
        } catch (_: IllegalStateException) {
        }
    }

    private fun closePlayer() {
        val mp = player ?: return
        player = null
        isPlaying = false
        try {
            if (mp.isPlaying) mp.stop()
        } catch (_: Throwable) {
        }
        try {
            mp.reset()
        } catch (_: Throwable) {
        }
        try {
            mp.release()
        } catch (_: Throwable) {
        }
    }

    fun release() {
        closePlayer()
        surface = null
        url = null
        isBuffering = false
    }
}

/** 可选倍速。 */
private val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * 自制全屏视频播放器：自研内核 + Compose 控制条。
 *
 * 功能：播放 / 暂停 / 重播、进度拖动、时间显示、倍速切换、静音、缓冲动画、
 * 错误提示与重新加载、点击画面切换控制条、播放中 3.5 秒自动隐藏控制条、保持屏幕常亮。
 */
@Composable
fun WudianVideoPlayer(
    url: String,
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val engine = remember { WudianVideoEngine() }
    var controlsVisible by remember { mutableStateOf(true) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    // 播放中保持屏幕常亮
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // 载入与释放播放器
    DisposableEffect(url) {
        engine.load(url)
        onDispose { engine.release() }
    }

    // 播放进度轮询
    LaunchedEffect(Unit) {
        while (true) {
            engine.refreshPosition()
            delay(if (engine.isPlaying) 250 else 500)
        }
    }

    // 播放中自动隐藏控制条
    LaunchedEffect(controlsVisible, engine.isPlaying, engine.isEnded, engine.hasError) {
        if (controlsVisible && engine.isPlaying && !engine.isEnded && !engine.hasError) {
            delay(3500)
            controlsVisible = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_screen")
    ) {
        // 画面按视频宽高比缩放，避免拉伸变形
        val ratio = engine.aspectRatio
        val boxRatio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 0f
        val videoModifier = when {
            ratio == null -> Modifier.fillMaxSize()
            ratio >= boxRatio -> Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)

            else -> Modifier
                .fillMaxHeight()
                .aspectRatio(ratio)
        }

        AndroidView(
            factory = { context ->
                SurfaceView(context).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {
                            engine.setSurface(holder.surface)
                        }

                        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                            engine.setSurface(holder.surface)
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            engine.setSurface(null)
                        }
                    })
                }
            },
            onRelease = { engine.setSurface(null) },
            modifier = Modifier
                .align(Alignment.Center)
                .then(videoModifier)
        )

        // 点击画面显示 / 隐藏控制条
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { controlsVisible = !controlsVisible })
                }
        )

        if (engine.isBuffering && !engine.hasError) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        engine.errorMessage?.let { message ->
            Surface(
                color = Color(0xE6212121),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Md3Icons.Status.error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = { engine.retry() },
                        modifier = Modifier.testTag("video_retry")
                    ) {
                        Icon(
                            imageVector = Md3Icons.Action.refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("重新加载")
                    }
                }
            }
        }

        // 中央播放 / 暂停 / 重播
        if (!engine.isBuffering && !engine.hasError &&
            (controlsVisible || !engine.isPlaying || engine.isEnded)
        ) {
            FilledIconButton(
                onClick = {
                    if (engine.isEnded) engine.replay() else engine.togglePlay()
                    controlsVisible = true
                },
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xCCFFFFFF),
                    contentColor = Color(0xFF111111)
                ),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .testTag("video_play_pause")
            ) {
                Icon(
                    imageVector = when {
                        engine.isEnded -> Md3Icons.Action.replay
                        engine.isPlaying -> Md3Icons.Action.pause
                        else -> Md3Icons.Action.play
                    },
                    contentDescription = if (engine.isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // 顶部标题栏
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("video_player_back")
            ) {
                Icon(
                    imageVector = Md3Icons.Action.back,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // 底部自制控制条
        AnimatedVisibility(
            visible = controlsVisible && !engine.hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xB3000000))
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatPlaybackTime(engine.positionMs),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = scrubFraction
                            ?: if (engine.durationMs > 0) {
                                (engine.positionMs.toFloat() / engine.durationMs).coerceIn(0f, 1f)
                            } else {
                                0f
                            },
                        onValueChange = { scrubFraction = it },
                        onValueChangeFinished = {
                            scrubFraction?.let { fraction ->
                                engine.seekTo((fraction * engine.durationMs).toInt())
                            }
                            scrubFraction = null
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x66FFFFFF)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .testTag("video_seek_bar")
                    )
                    Text(
                        text = formatPlaybackTime(engine.durationMs),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (engine.isEnded) engine.replay() else engine.togglePlay() },
                        modifier = Modifier.testTag("video_play_pause_bar")
                    ) {
                        Icon(
                            imageVector = if (engine.isPlaying) Md3Icons.Action.pause else Md3Icons.Action.play,
                            contentDescription = if (engine.isPlaying) "暂停" else "播放",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { engine.replay() },
                        modifier = Modifier.testTag("video_replay")
                    ) {
                        Icon(
                            imageVector = Md3Icons.Action.replay,
                            contentDescription = "重播",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            val index = PLAYBACK_SPEEDS.indexOf(engine.speed)
                            val next = PLAYBACK_SPEEDS[(index + 1).mod(PLAYBACK_SPEEDS.size)]
                            engine.changeSpeed(next)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                        modifier = Modifier.testTag("video_speed")
                    ) {
                        Icon(
                            imageVector = Md3Icons.Action.speed,
                            contentDescription = "倍速",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = formatPlaybackSpeed(engine.speed), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { engine.toggleMute() },
                        modifier = Modifier.testTag("video_mute")
                    ) {
                        Icon(
                            imageVector = if (engine.muted) Md3Icons.Action.volumeOff else Md3Icons.Action.volumeOn,
                            contentDescription = if (engine.muted) "取消静音" else "静音",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/** 将毫秒格式化为 mm:ss 或 h:mm:ss。 */
private fun formatPlaybackTime(ms: Int): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** 倍速文案，例如 1.0x / 1.25x。 */
private fun formatPlaybackSpeed(value: Float): String =
    if (value == value.toInt().toFloat()) "${value.toInt()}.0x" else "${value}x"
