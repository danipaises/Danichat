package com.bitchat.android.ui.theme

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// Standard UI semantics live in Material so stock components and custom Bitchat composables
// share one source of truth. LocalBitchatPalette below only supplies app-specific extra colors.
internal val DarkBitchatColorScheme = darkColorScheme(
    primary = Color(0xFFC5BAFF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332661),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFF5AA94),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF5C3329),
    onSecondaryContainer = Color(0xFFFFDAD0),
    tertiary = DarkBitchatPalette.accentOrange,
    onTertiary = Color.Black,
    background = Color(0xFF14121C),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1D1929),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2C263B),
    onSurfaceVariant = Color(0xFFCAC3DA),
    outline = Color(0xFF4A435B),
    outlineVariant = Color(0xFF342D45),
    error = Color(0xFFFF453A),
    onError = Color.Black
)

internal val LightBitchatColorScheme = lightColorScheme(
    primary = Color(0xFF5B4ACB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DFFF),
    onPrimaryContainer = Color(0xFF20105D),
    secondary = Color(0xFF994831),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = Color(0xFF3B0B00),
    tertiary = LightBitchatPalette.accentOrange,
    onTertiary = Color.Black,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1D1929),
    surface = Color(0xFFF5F1FF),
    onSurface = Color(0xFF1D1929),
    surfaceVariant = Color(0xFFEBE4F5),
    onSurfaceVariant = Color(0xFF4D455C),
    outline = Color(0xFF7E748D),
    outlineVariant = Color(0xFFCEC4DE),
    error = Color(0xFFD70015),
    onError = Color.White
)

@Composable
fun BitchatTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    // App-level override from ThemePreferenceManager
    val themePref by ThemePreferenceManager.themeFlow.collectAsState(initial = ThemePreference.System)
    val shouldUseDark = when (darkTheme) {
        true -> true
        false -> false
        null -> when (themePref) {
            ThemePreference.Dark -> true
            ThemePreference.Light -> false
            ThemePreference.System -> isSystemInDarkTheme()
        }
    }

    val colorScheme = if (shouldUseDark) DarkBitchatColorScheme else LightBitchatColorScheme
    val palette = if (shouldUseDark) DarkBitchatPalette else LightBitchatPalette

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (!shouldUseDark) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (!shouldUseDark) {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else 0
            }
            window.navigationBarColor = colorScheme.background.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }

    CompositionLocalProvider(LocalBitchatPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
