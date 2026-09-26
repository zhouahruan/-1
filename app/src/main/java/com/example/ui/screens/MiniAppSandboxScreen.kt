package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MiniAppSandboxScreen(
    miniAppName: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val sandboxUrl by viewModel.sandboxUrl.collectAsState()
    val sandboxError by viewModel.sandboxError.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(miniAppName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("miniapp_sandbox_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { webViewRef?.reload() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("miniapp_reload")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重新载入")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (sandboxError != null) {
            Text(
                text = "小程序运行失败：$sandboxError",
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        } else if (sandboxUrl.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)
                    webViewClient = WebViewClient()
                    // 运行地址由后端 miniapp-serve 提供（ZIP 包由服务端解包托管）
                    loadUrl(sandboxUrl)
                    webViewRef = this
                }
            },
            update = { _ -> /* 地址在进入本页时已确定；刷新由顶部按钮触发 */ },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
