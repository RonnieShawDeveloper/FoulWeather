// app/src/main/java/com/artificialinsightsllc/foulweather/ui/screens/SplashScreen.kt
package com.artificialinsightsllc.foulweather.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artificialinsightsllc.foulweather.R // Import your R file to access drawables
import com.artificialinsightsllc.foulweather.data.FirebaseService // Import FirebaseService
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import kotlinx.coroutines.delay // Import for coroutine delay

/**
 * Composable for the Splash Screen.
 * Displays a graphic with fade-in/fade-out animation and then navigates
 * to the appropriate initial screen based on user authentication and saved location.
 *
 * @param firebaseService An instance of FirebaseService to check user authentication and profile.
 * @param onSplashFinished Callback function invoked after the splash animation completes,
 * providing the route to navigate to next (e.g., "login", "weather_dashboard/latitude/longitude").
 */
@Composable
fun SplashScreen(firebaseService: FirebaseService, onSplashFinished: (String) -> Unit) {
    // Animatable float for the alpha (opacity) of the splash image, for fading effects.
    val alpha = remember { Animatable(0f) }

    // State to hold the determined initial navigation route.
    var initialRoute by remember { mutableStateOf("login") }

    /**
     * LaunchedEffect is used to manage the splash screen's lifecycle and animations.
     * It runs only once when the composable enters the composition.
     */
    LaunchedEffect(Unit) {
        // 1. Fade In animation (0ms to 500ms)
        alpha.animateTo(1f, animationSpec = tween(500))

        // 2. Show for a duration (1000ms)
        delay(1000)

        // 3. Fade Out animation (500ms)
        alpha.animateTo(0f, animationSpec = tween(500))

        // --- Determine the initial navigation route based on user login status ---
        val currentUser = firebaseService.getCurrentUser()
        if (currentUser != null) {
            // User is logged in, try to fetch their stored location from Firestore
            val userProfile = firebaseService.getUserProfile(currentUser.uid)
            val storedLat = userProfile?.get("latitude") as? Double
            val storedLon = userProfile?.get("longitude") as? Double

            if (storedLat != null && storedLon != null) {
                // If user profile has saved coordinates, navigate to dashboard with them
                initialRoute = "weather_dashboard/$storedLat/$storedLon" // Use lat/lon for route
            } else {
                // User is logged in but no *geocoded* location found, go to location setting
                initialRoute = "location_setting"
            }
        } else {
            // No user logged in, go to login screen
            initialRoute = "login"
        }

        // Invoke the callback to trigger navigation in MainActivity after animations and checks
        onSplashFinished(initialRoute)
    }

    // UI layout for the splash screen
    Box(
        modifier = Modifier
            .fillMaxSize() // Fills the entire screen
            .background(Color.Black), // Black background as requested
        contentAlignment = Alignment.Center // Center the content (image)
    ) {
        // Display the splash screen graphic
        Image(
            painter = painterResource(id = R.drawable.splashscreen), // Reference to your drawable
            contentDescription = "App Logo", // Content description for accessibility
            modifier = Modifier
                .fillMaxWidth() // Make the image as wide as possible
                .alpha(alpha.value), // Apply the animated alpha for fading
            contentScale = ContentScale.Fit // Fit the image within the bounds
        )
    }
}

/**
 * Preview function for the SplashScreen Composable.
 * Removed all simulated data. Previews might not fully execute LaunchedEffect or navigate.
 */
@Preview(showBackground = true)
@Composable
fun PreviewSplashScreen() {
    FoulWeatherTheme {
        SplashScreen(
            firebaseService = object : FirebaseService() {
                // Completely empty mock for getCurrentUser - simulating no logged-in user by default for preview
                override fun getCurrentUser(): com.google.firebase.auth.FirebaseUser? = null

                // Completely empty mock for getUserProfile - simulating no user profile by default for preview
                override suspend fun getUserProfile(userId: String): Map<String, Any?>? = null
            },
            onSplashFinished = { route -> println("Splash finished, navigate to: $route") }
        )
    }
}
