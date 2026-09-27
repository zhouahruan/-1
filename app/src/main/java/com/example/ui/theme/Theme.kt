package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// 更大胆的 MD3 形状：统一大圆角，营造现代卡片与胶囊感
private val BoldShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFC7BFFF),
        onPrimary = Color(0xFF2B00A5),
        primaryContainer = Color(0xFF3E21C9),
        onPrimaryContainer = Color(0xFFE3DFFF),
        inversePrimary = Color(0xFF4A3AFF),
        secondary = Color(0xFFFFB0C9),
        onSecondary = Color(0xFF650034),
        secondaryContainer = Color(0xFF8E0048),
        onSecondaryContainer = Color(0xFFFFD9E3),
        tertiary = Color(0xFF82D6CD),
        onTertiary = Color(0xFF003731),
        tertiaryContainer = Color(0xFF005048),
        onTertiaryContainer = Color(0xFFB2F2E9),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF131316),
        onBackground = Color(0xFFE5E1E6),
        surface = Color(0xFF131316),
        onSurface = Color(0xFFE5E1E6),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceTint = Color(0xFFC7BFFF),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE5E1E6),
        inverseOnSurface = Color(0xFF313033),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF4A3AFF),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE3DFFF),
        onPrimaryContainer = Color(0xFF16006C),
        inversePrimary = Color(0xFFC7BFFF),
        secondary = Color(0xFFE91E63),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFD9E3),
        onSecondaryContainer = Color(0xFF3E001D),
        tertiary = Color(0xFF00897B),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFB2F2E9),
        onTertiaryContainer = Color(0xFF00201C),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFFDFBFF),
        onBackground = Color(0xFF1B1B1F),
        surface = Color(0xFFFDFBFF),
        onSurface = Color(0xFF1B1B1F),
        surfaceVariant = Color(0xFFE6E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceTint = Color(0xFF4A3AFF),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 更大胆的定制配色：默认关闭动态取色，保证品牌色始终生效
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BoldShapes,
        content = content,
    )
}
