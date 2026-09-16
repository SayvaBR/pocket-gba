package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.SharedPreferences
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

private val AccentValues = listOf("blue", "purple", "cyan", "green", "orange", "pink")

fun pocketAccentColor(index: Int): Color =
    when (index) {
        1 -> Color(0xFF8B7CFF)
        2 -> Color(0xFF36C5F0)
        3 -> Color(0xFF44D7A8)
        4 -> Color(0xFFFFA24A)
        5 -> Color(0xFFFF6B9A)
        else -> Color(0xFF4DA3FF)
    }

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember(context) { SharedPreferencesHelper.getSharedPreferences(context) }
    var accentName by remember(preferences) {
        mutableStateOf(preferences.getString("pocket_theme_accent", "blue") ?: "blue")
    }

    DisposableEffect(preferences) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == "pocket_theme_accent") {
                    accentName = sharedPreferences.getString(key, "blue") ?: "blue"
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val accentIndex = AccentValues.indexOf(accentName).coerceAtLeast(0)
    val accent = pocketAccentColor(accentIndex)

    val colors =
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.20f),
            onPrimaryContainer = Color.White,
            secondary = accent,
            onSecondary = Color.White,
            secondaryContainer = accent.copy(alpha = 0.14f),
            onSecondaryContainer = Color(0xFFF5F7FA),
            tertiary = accent,
            onTertiary = Color.White,
            background = Color(0xFF07111E),
            onBackground = Color(0xFFF5F7FA),
            surface = Color(0xFF0B1725),
            onSurface = Color(0xFFF5F7FA),
            surfaceVariant = Color(0xFF101E2D),
            onSurfaceVariant = Color(0xFFA9B7C9),
            outline = Color(0xFF294057),
            outlineVariant = Color(0xFF1B2C3E),
            error = Color(0xFFFF6B7A),
            onError = Color.White,
            errorContainer = Color(0xFF3A1720),
            onErrorContainer = Color(0xFFFFD9DE),
            inverseSurface = Color(0xFFF3F6FA),
            inverseOnSurface = Color(0xFF101820),
            inversePrimary = accent,
            surfaceTint = Color.Transparent,
            scrim = Color.Black,
        )

    MaterialTheme(colorScheme = colors) {
        content()
    }
}
