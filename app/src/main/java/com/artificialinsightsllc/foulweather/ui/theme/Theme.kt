// app/src/main/java/com/artificialinsightsllc/foulweather/ui/theme/Theme.kt
package com.artificialinsightsllc.foulweather.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Defines the Light color scheme for the FoulWeather app using Material 3.
 *
 * This color scheme is used when the device is in light mode, or when
 * dynamic coloring is not available (e.g., on older Android versions).
 * It uses custom primary, secondary, and tertiary colors defined in Colors.kt.
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,       // Main brand color for light theme
    secondary = PurpleGrey40, // Secondary brand color
    tertiary = Pink40         // Complementary color
    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Defines the Dark color scheme for the FoulWeather app using Material 3.
 *
 * This color scheme is used when the device is in dark mode, or when
 * dynamic coloring is not available. It uses custom primary, secondary,
 * and tertiary colors suitable for a dark theme background.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,       // Main brand color for dark theme
    secondary = PurpleGrey80, // Secondary brand color
    tertiary = Pink80         // Complementary color
    /* Other default colors to override
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    */
)

/**
 * The main Composable function for applying the FoulWeather theme to your app.
 *
 * This theme automatically adapts to system dark mode settings and uses dynamic
 * color where available (Android 12+).
 *
 * @param darkTheme Determines if the dark theme should be used. Defaults to system setting.
 * @param dynamicColor Determines if dynamic color should be used (Android 12+). Defaults to true.
 * @param content The composable content to which the theme should be applied.
 */
@Composable
fun FoulWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Checks if the system is in dark mode
    // Dynamic color is available on Android 12 (S) and above
    dynamicColor: Boolean = true, // Enables dynamic color if available
    content: @Composable () -> Unit
) {
    // Determine which color scheme to use based on darkTheme and dynamicColor settings
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        // Set the status bar color to match the primary color of the theme
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Adjust system bar icons for light/dark theme to ensure visibility
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // Apply the MaterialTheme with the selected color scheme, typography, and shapes
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Uses typography defined in Type.kt
        content = content
    )
}
