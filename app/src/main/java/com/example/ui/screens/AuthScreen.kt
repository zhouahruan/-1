package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import kotlinx.coroutines.delay

/**
 * 登录 / 注册弹窗（后端为 phone-auth Edge Function）：
 *  - 验证码登录：手机号 + 短信验证码，未注册的手机号验证通过后自动完成注册；
 *  - 密码登录：手机号 + 密码（密码需在 Web 端设置过）。
 * 应用不强制登录，未登录用户可以浏览帖子。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val loading by viewModel.authLoading.collectAsState()
    val error by viewModel.authError.collectAsState()
    val otpSending by viewModel.otpSending.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()
    val otpTicks by viewModel.otpSentTicks.collectAsState()

    var codeMode by remember { mutableStateOf(true) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    LaunchedEffect(otpTicks) {
        if (otpTicks > 0) countdown = 60
    }

    val phoneValid = phone.filter { it.isDigit() }.length >= 11
    val canSendCode = phoneValid && !otpSending && countdown == 0
    val canSubmit = when {
        loading -> false
        codeMode -> phoneValid && code.length >= 4
        else -> phoneValid && password.isNotEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "登录",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("登录 / 注册", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryTabRow(
                    selectedTabIndex = if (codeMode) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = codeMode,
                        onClick = {
                            codeMode = true
                            viewModel.clearAuthError()
                        },
                        text = { Text("验证码登录/注册", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("login_tab_code")
                    )
                    Tab(
                        selected = !codeMode,
                        onClick = {
                            codeMode = false
                            viewModel.clearAuthError()
                        },
                        text = { Text("密码登录", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("login_tab_password")
                    )
                }

                Text(
                    text = if (codeMode) {
                        "输入手机号并获取短信验证码。若该手机号尚未注册，验证通过后将自动注册并登录。"
                    } else {
                        "使用手机号与密码登录（密码需已在网页端设置）。忘记密码可改用验证码登录。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        phone = input.filter { c -> c.isDigit() || c == '+' }.take(15)
                        viewModel.clearAuthError()
                    },
                    label = { Text("手机号") },
                    placeholder = { Text("请输入手机号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_phone_input")
                )

                if (codeMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { input -> code = input.filter { it.isDigit() }.take(6) },
                            label = { Text("短信验证码") },
                            placeholder = { Text("6 位验证码") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("login_code_input")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        FilledTonalButton(
                            onClick = { viewModel.sendLoginCode(phone) },
                            enabled = canSendCode,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("login_send_code_button")
                        ) {
                            Text(
                                text = when {
                                    otpSending -> "发送中"
                                    countdown > 0 -> "${countdown}s"
                                    otpSent -> "重新获取"
                                    else -> "获取验证码"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        placeholder = { Text("请输入登录密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )
                }

                if (error != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .testTag("login_error_text")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (codeMode) viewModel.verifyLoginCode(phone, code)
                    else viewModel.loginWithPassword(phone, password)
                },
                enabled = canSubmit,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_login_button")
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        loading -> "请稍候..."
                        codeMode -> "登录 / 注册"
                        else -> "登录"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("取消")
            }
        }
    )
}
