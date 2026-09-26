package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = CyanOnPrimaryDark,
    primaryContainer = CyanPrimaryContainerDark,
    onPrimaryContainer = CyanOnPrimaryContainerDark,
    secondary = CyanSecondaryDark,
    onSecondary = CyanOnSecondaryDark,
    secondaryContainer = CyanSecondaryContainerDark,
    onSecondaryContainer = CyanOnSecondaryContainerDark,
    tertiary = CyanTertiaryDark,
    onTertiary = CyanOnTertiaryDark,
    tertiaryContainer = CyanTertiaryContainerDark,
    onTertiaryContainer = CyanOnTertiaryContainerDark,
    background = CyanBackgroundDark,
    onBackground = CyanOnBackgroundDark,
    surface = CyanSurfaceDark,
    onSurface = CyanOnSurfaceDark,
    surfaceVariant = CyanSurfaceVariantDark,
    onSurfaceVariant = CyanOnSurfaceVariantDark,
    outline = CyanOutlineDark,
    outlineVariant = CyanOutlineVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = CyanOnPrimaryLight,
    primaryContainer = CyanPrimaryContainerLight,
    onPrimaryContainer = CyanOnPrimaryContainerLight,
    secondary = CyanSecondaryLight,
    onSecondary = CyanOnSecondaryLight,
    secondaryContainer = CyanSecondaryContainerLight,
    onSecondaryContainer = CyanOnSecondaryContainerLight,
    tertiary = CyanTertiaryLight,
    onTertiary = CyanOnTertiaryLight,
    tertiaryContainer = CyanTertiaryContainerLight,
    onTertiaryContainer = CyanOnTertiaryContainerLight,
    background = CyanBackgroundLight,
    onBackground = CyanOnBackgroundLight,
    surface = CyanSurfaceLight,
    onSurface = CyanOnSurfaceLight,
    surfaceVariant = CyanSurfaceVariantLight,
    onSurfaceVariant = CyanOnSurfaceVariantLight,
    outline = CyanOutlineLight,
    outlineVariant = CyanOutlineVariantLight,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Cyan is the explicit brand requirement from user prompt
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
