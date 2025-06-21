package com.artificialinsightsllc.foulweather

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.artificialinsightsllc.foulweather.data.FirebaseService
import com.artificialinsightsllc.foulweather.data.GeocodingService
import com.artificialinsightsllc.foulweather.data.WeatherService
import com.artificialinsightsllc.foulweather.ui.screens.LoginScreen
import com.artificialinsightsllc.foulweather.ui.screens.LocationSettingScreen
import com.artificialinsightsllc.foulweather.ui.screens.SignupInfo
import com.artificialinsightsllc.foulweather.ui.screens.SignupScreen
import com.artificialinsightsllc.foulweather.ui.screens.SplashScreen
import com.artificialinsightsllc.foulweather.ui.screens.WeatherDashboardScreen
import com.artificialinsightsllc.foulweather.ui.screens.SarcasticSummaryPlayerScreen
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import android.content.Intent
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest

// Firebase Remote Config imports
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.artificialinsightsllc.foulweather.BuildConfig // Added BuildConfig import

/**
 * The main entry point for the FoulWeather Android application.
 *
 * This activity sets up the Jetpack Compose UI content and defines the navigation
 * graph for the application, handling transitions between different screens
 * (Splash, Login, Signup, Location Setting, Weather Dashboard, and Sarcastic Summary Player).
 *
 * It orchestrates the initial app flow, checking for an existing authenticated user
 * and their saved location to decide the first user-facing screen. It also integrates
 * GeocodingService to obtain coordinates for user-specified locations.
 *
 * MODIFIED: Enhanced back navigation from SarcasticSummaryPlayerScreen to always go to dashboard.
 * FIXED: Ensured notification tap correctly navigates to SarcasticSummaryPlayerScreen.
 * FIXED: Resolved type mismatch for onNavigateToDashboard callback in SarcasticSummaryPlayerScreen.
 * FIXED: Removed firebaseService parameter from SarcasticSummaryPlayerScreen call in MainActivity.
 * FIXED: Refactored UI into a separate @Composable function to resolve 'Unresolved reference' for state.
 * FIXED: Resolved 'Unresolved reference showNotificationRationale' by proper state management with callbacks.
 * MODIFIED: Implemented MutableStateFlow for reactive intent handling from onNewIntent.
 * FIXED: Corrected boolean extraction from Intent extras to prevent ClassCastException.
 * ADDED: Debugging logs for intent processing in AppContent.
 * MODIFIED: Implemented an AlertDialog to explain notification permissions before the system prompt.
 * ADDED: Version check using Firebase Remote Config to enforce app updates.
 */
class MainActivity : ComponentActivity() {

    private val _newIntentFlow = MutableStateFlow<Intent?>(null)

    /**
     * Handles the FCM token logic: retrieving the token and saving it to the user's profile.
     * This function is called after notification permission is granted or if not required.
     *
     * @param userId The UID of the currently authenticated user.
     * @param firebaseService An instance of [FirebaseService] to interact with Firebase.
     * @param coroutineScope The [CoroutineScope] to launch asynchronous tasks.
     */
    private fun handleFcmTokenLogic(userId: String, firebaseService: FirebaseService, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            try {
                val token = firebaseService.getFCMToken()
                if (token != null) {
                    firebaseService.saveFCMTokenToUserProfile(userId, token)
                } else {
                    Log.e("MainActivity", "FCM token is null, cannot save to profile.")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during FCM token handling: ${e.message}", e)
            }
        }
    }

    /**
     * Called when the activity is first created.
     * Sets the Compose content and initializes the intent flow.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture the initial intent that launched the activity
        _newIntentFlow.value = intent

        setContent {
            AppContent(
                newIntentFlow = _newIntentFlow,
                context = this // Pass the activity context to AppContent
            )
        }
    }

    /**
     * Called when the activity receives a new intent.
     * Updates the [_newIntentFlow] to trigger re-composition and handle the new intent.
     *
     * @param intent The new [Intent] that was started for the activity.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the activity's current intent
        _newIntentFlow.value = intent // Emit the new intent to the flow
        Log.d("MainActivity", "onNewIntent called. Updated _newIntentFlow with new intent: ${intent.extras}") // DEBUG
    }

    /**
     * The main Composable function for the entire application's UI.
     * This function manages the navigation graph and handles global app logic like permissions and version checks.
     *
     * @param newIntentFlow A [MutableStateFlow] that emits new [Intent] objects when the activity receives them.
     * @param context The application [Context] for accessing package info.
     */
    @Composable
    private fun AppContent(newIntentFlow: MutableStateFlow<Intent?>, context: Context) {
        FoulWeatherTheme {
            Surface(
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                color = Color.Black // Set background color for the entire surface
            ) {
                // Initialize service instances, remembered across compositions
                val weatherService = remember { WeatherService() }
                val firebaseService = remember { FirebaseService() }
                val geocodingService = remember { GeocodingService() }

                val navController = rememberNavController() // Remember NavController for navigation

                val coroutineScope = rememberCoroutineScope() // CoroutineScope for launching async tasks
                val activity = context as Activity // Cast context to Activity for permission checks

                // State to control the visibility of our custom notification rationale AlertDialog
                var showNotificationPermissionRationaleDialog by remember { mutableStateOf(false) }

                // State to control the visibility of the mandatory upgrade dialog
                var showUpgradeRequiredDialog by remember { mutableStateOf(false) }

                // State to track if the version check has passed
                var isVersionCheckPassed by remember { mutableStateOf(false) }

                // Initialize Remote Config
                val remoteConfig = Firebase.remoteConfig
                val configSettings = remoteConfigSettings {
                    minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600 // Fetch every hour in production, immediately in debug
                }
                remoteConfig.setConfigSettingsAsync(configSettings)
                remoteConfig.setDefaultsAsync(mapOf("version" to BuildConfig.VERSION_NAME)) // Set default remote config version to app's current version

                // LaunchedEffect for Remote Config version check
                LaunchedEffect(Unit) {
                    val currentAppVersion = try {
                        // Ensure versionName is treated as non-nullable after extraction, defaulting to "0.0"
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0"
                    } catch (e: PackageManager.NameNotFoundException) {
                        Log.e("MainActivity", "Could not get package info: ${e.message}")
                        "0.0" // Default to 0.0 if version cannot be found
                    }

                    remoteConfig.fetchAndActivate()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // Corrected line 197: Provide a default "0.0" if remoteConfig.getString("version") is null
                                val remoteVersion = remoteConfig.getString("version") ?: "0.0"
                                Log.d("MainActivity", "Remote Config version: $remoteVersion, Current App version: $currentAppVersion")

                                val remoteVersionParts = remoteVersion.split(".").map { it.toIntOrNull() ?: 0 }
                                // Corrected line 198: currentAppVersion is already non-nullable due to earlier fix
                                val currentVersionParts = currentAppVersion.split(".").map { it.toIntOrNull() ?: 0 }

                                var needsUpgrade = false
                                for (i in 0 until maxOf(remoteVersionParts.size, currentVersionParts.size)) {
                                    val remotePart = remoteVersionParts.getOrElse(i) { 0 }
                                    val currentPart = currentVersionParts.getOrElse(i) { 0 }

                                    if (remotePart > currentPart) {
                                        needsUpgrade = true
                                        break
                                    } else if (remotePart < currentPart) {
                                        needsUpgrade = false // Current version is newer, no upgrade needed
                                        break
                                    }
                                    // If parts are equal, continue to the next part
                                }

                                if (needsUpgrade) {
                                    showUpgradeRequiredDialog = true
                                } else {
                                    isVersionCheckPassed = true
                                }
                            } else {
                                Log.e("MainActivity", "Failed to fetch Remote Config: ${task.exception?.message}")
                                // Allow app to proceed if Remote Config fetch fails to avoid blocking user
                                isVersionCheckPassed = true
                            }
                        }
                }

                // ActivityResultLauncher for requesting POST_NOTIFICATIONS permission
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    // Callback after the system permission dialog is dismissed
                    if (isGranted) {
                        Log.d("MainActivity", "Notification permission granted by system dialog.")
                        // If granted, proceed with FCM token handling
                        firebaseService.getCurrentUser()?.let { user ->
                            handleFcmTokenLogic(user.uid, firebaseService, coroutineScope)
                        }
                    } else {
                        Log.w("MainActivity", "Notification permission denied by user (via system dialog).")
                    }
                }

                // LaunchedEffect to manage initial permission check and dialog presentation
                LaunchedEffect(isVersionCheckPassed) {
                    if (isVersionCheckPassed) { // Only proceed with permission check if version check passed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // For Android 13 (API 33) and above, explicit POST_NOTIFICATIONS permission is needed
                            when {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED -> {
                                    // Permission already granted, proceed directly to FCM token logic
                                    Log.d("MainActivity", "Notification permission already granted. Proceeding with FCM token handling.")
                                    firebaseService.getCurrentUser()?.let { user ->
                                        handleFcmTokenLogic(user.uid, firebaseService, coroutineScope)
                                    }
                                }
                                else -> {
                                    // Permission not granted (either first time or previously denied without "Don't ask again")
                                    // Always show our custom rationale dialog first to explain why it's needed
                                    Log.d("MainActivity", "Notification permission not granted. Showing custom rationale dialog.")
                                    showNotificationPermissionRationaleDialog = true
                                }
                            }
                        } else {
                            // For API levels below 33, POST_NOTIFICATIONS permission is implicitly granted upon install
                            Log.d("MainActivity", "Notification permission not required for API < 33. Proceeding with FCM.")
                            firebaseService.getCurrentUser()?.let { user ->
                                handleFcmTokenLogic(user.uid, firebaseService, coroutineScope)
                            }
                        }
                    }
                }

                // Our custom AlertDialog that explains why notification permission is needed
                if (showNotificationPermissionRationaleDialog) {
                    AlertDialog(
                        onDismissRequest = { showNotificationPermissionRationaleDialog = false }, // Allow dismissing
                        title = { Text("Permission Required", color = MaterialTheme.colorScheme.primary) },
                        text = {
                            Text(
                                "Foul Weather needs notification permission to send you live weather alerts and important information, including your sarcastic weather summaries. Please allow notifications to get timely updates.",
                                color = Color.White
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    // When user clicks "Allow", dismiss our dialog and launch the system permission request
                                    showNotificationPermissionRationaleDialog = false
                                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            ) {
                                Text("Allow Notifications", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationPermissionRationaleDialog = false }) {
                                Text("No, Thanks", color = Color.White)
                            }
                        },
                        containerColor = Color.Black // Set background for AlertDialog
                    )
                }

                // Mandatory Upgrade Dialog
                if (showUpgradeRequiredDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Dialog is not dismissible, user must upgrade */ },
                        title = { Text("Upgrade Required", color = MaterialTheme.colorScheme.error) },
                        text = {
                            Text(
                                "A new version of Foul Weather is available. Please upgrade to continue using the app.",
                                color = Color.White
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    // Here you would typically direct the user to the app store
                                    // For now, we just log and exit (or user can close manually)
                                    Log.d("MainActivity", "User clicked Upgrade. Directing to app store (simulated).")
                                    activity.finish() // Close the app as a placeholder for actual upgrade
                                }
                            ) {
                                Text("Upgrade Now", color = Color.White)
                            }
                        },
                        containerColor = Color.Black // Set background for AlertDialog
                    )
                }


                // Only display NavHost if version check has passed and no upgrade is required
                if (isVersionCheckPassed && !showUpgradeRequiredDialog) {
                    // Handle navigation based on new intents (initial or subsequent)
                    LaunchedEffect(newIntentFlow) {
                        newIntentFlow.collectLatest { intent ->
                            intent?.let {
                                // Robustly get wfoId and audioReady, handling potential type mismatches
                                val wfoIdFromNotification = it.getStringExtra("wfoId")
                                val isAudioReadyNotification = it.getBooleanExtra("audioReady", false) // Correctly get boolean

                                Log.d("MainActivity", "AppContent LaunchedEffect received intent. Extras: ${it.extras}") // DEBUG
                                Log.d("MainActivity", "AppContent Extracted: wfoId=$wfoIdFromNotification, audioReady=$isAudioReadyNotification") // DEBUG

                                if (wfoIdFromNotification != null && isAudioReadyNotification) {
                                    Log.d("MainActivity", "Navigating from intent to SarcasticSummaryPlayerScreen for WFO: $wfoIdFromNotification")
                                    navController.navigate("sarcastic_summary_player/${wfoIdFromNotification}") {
                                        // Pop up to the dashboard to avoid stacking player screens
                                        popUpTo("weather_dashboard/{latitude}/{longitude}") {
                                            saveState = true // Save state of the dashboard if it exists
                                        }
                                        launchSingleTop = true // Prevent multiple copies of the destination
                                        restoreState = true // Restore state if popped up
                                    }
                                } else {
                                    // Only navigate to splash if it's the very first intent and not an audio-ready one
                                    // This prevents re-navigating to splash if the user is already on another screen
                                    if (navController.currentDestination?.route == null || navController.currentDestination?.route == "splash_placeholder") {
                                        Log.d("MainActivity", "Initial launch or no specific notification data. Navigating to splash.")
                                        navController.navigate("splash") {
                                            popUpTo("splash_placeholder") { inclusive = true } // Clear placeholder from back stack
                                            launchSingleTop = true // Only one splash screen instance
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Define the navigation graph for the application
                    NavHost(navController = navController, startDestination = "splash_placeholder") {
                        // Placeholder route for initial intent handling before determining the actual start destination
                        composable("splash_placeholder") { /* Empty placeholder for initial state */ }

                        // Splash Screen route
                        composable("splash") {
                            SplashScreen(
                                firebaseService = firebaseService,
                                onSplashFinished = { initialRoute ->
                                    // Ensure navigation only happens if we are still on the splash screen
                                    if (navController.currentDestination?.route == "splash") {
                                        navController.navigate(initialRoute) {
                                            popUpTo("splash") { inclusive = true } // Remove splash from back stack
                                        }
                                    }
                                }
                            )
                        }

                        // Login Screen route
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { email, password ->
                                    coroutineScope.launch {
                                        try {
                                            val user = firebaseService.loginUser(email, password)
                                            if (user != null) {
                                                // Handle FCM token logic after successful login
                                                handleFcmTokenLogic(user.uid, firebaseService, coroutineScope)

                                                // Check if user has a saved location
                                                val userProfile = firebaseService.getUserProfile(user.uid)
                                                val lat = userProfile?.get("latitude") as? Double
                                                val lon = userProfile?.get("longitude") as? Double

                                                if (lat != null && lon != null) {
                                                    // Navigate to dashboard with saved coordinates
                                                    val route = "weather_dashboard/$lat/$lon"
                                                    navController.navigate(route) {
                                                        popUpTo("login") { inclusive = true } // Remove login from back stack
                                                    }
                                                } else {
                                                    // User logged in but no location, go to location setting
                                                    Log.w("MainActivity", "User logged in but no location coordinates set, going to location setting.")
                                                    navController.navigate("location_setting") {
                                                        popUpTo("login") { inclusive = true } // Remove login from back stack
                                                    }
                                                }
                                            } else {
                                                Log.e("MainActivity", "Login failed: User is null.")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Login exception: ${e.message}", e)
                                            // Error handling for UI (e.g., show Toast/Snackbar)
                                        }
                                    }
                                },
                                onNavigateToSignup = {
                                    navController.navigate("signup") // Navigate to signup screen
                                }
                            )
                        }

                        // Signup Screen route
                        composable("signup") {
                            SignupScreen(
                                geocodingService = geocodingService,
                                onSignupSuccess = { signupInfo ->
                                    coroutineScope.launch {
                                        try {
                                            val newUser = firebaseService.registerUserAndSaveProfile(
                                                signupInfo.email,
                                                signupInfo.password,
                                                signupInfo
                                            )
                                            if (newUser != null) {
                                                // Handle FCM token logic after successful signup
                                                handleFcmTokenLogic(newUser.uid, firebaseService, coroutineScope)

                                                if (signupInfo.latitude != null && signupInfo.longitude != null) {
                                                    // Navigate to dashboard with newly geocoded location
                                                    val route = "weather_dashboard/${signupInfo.latitude}/${signupInfo.longitude}"
                                                    navController.navigate(route) {
                                                        popUpTo("signup") { inclusive = true } // Remove signup from back stack
                                                        popUpTo("login") { inclusive = true } // Also remove login if it was under signup
                                                    }
                                                } else {
                                                    // Signup successful but geocoding failed, go to location setting
                                                    Log.w("MainActivity", "Signup successful but no lat/lon from geocoding. Navigating to location setting.")
                                                    navController.navigate("location_setting") {
                                                        popUpTo("signup") { inclusive = true }
                                                        popUpTo("login") { inclusive = true }
                                                    }
                                                }
                                            } else {
                                                Log.e("MainActivity", "Signup failed: New user is null.")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MainActivity", "Signup exception: ${e.message}", e)
                                            // Error handling for UI
                                        }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo("signup") { inclusive = true } // Remove signup from back stack
                                    }
                                }
                            )
                        }

                        // Location Setting Screen route
                        composable("location_setting") {
                            LocationSettingScreen(
                                geocodingService = geocodingService,
                                onLocationSet = { locationQuery, latitude, longitude ->
                                    coroutineScope.launch {
                                        val currentUser = firebaseService.getCurrentUser()
                                        if (currentUser != null) {
                                            // Extract city/state from query (simplified for now)
                                            val parts = locationQuery.split(",").map { it.trim() }
                                            val city = parts.getOrNull(0) ?: ""
                                            val state = parts.getOrNull(1) ?: ""

                                            try {
                                                // Update user's location in Firestore
                                                firebaseService.updateCurrentUserLocation(
                                                    currentUser.uid, city, state, null, latitude, longitude
                                                )
                                                // Navigate to dashboard with new location
                                                val route = "weather_dashboard/$latitude/$longitude"
                                                navController.navigate(route) {
                                                    popUpTo("location_setting") { inclusive = true } // Remove location setting from back stack
                                                }
                                            } catch (e: Exception) {
                                                Log.e("MainActivity", "Failed to update user location: ${e.message}", e)
                                                // Error handling for UI
                                            }
                                        } else {
                                            // If no current user, navigate back to login
                                            navController.navigate("login") { popUpTo(0) }
                                        }
                                    }
                                }
                            )
                        }

                        // Weather Dashboard Screen route, taking latitude and longitude as arguments
                        composable(
                            route = "weather_dashboard/{latitude}/{longitude}",
                            arguments = listOf(
                                navArgument("latitude") { type = NavType.FloatType },
                                navArgument("longitude") { type = NavType.FloatType }
                            )
                        ) { backStackEntry ->
                            val latitude = backStackEntry.arguments?.getFloat("latitude")
                            val longitude = backStackEntry.arguments?.getFloat("longitude")

                            if (latitude != null && longitude != null) {
                                WeatherDashboardScreen(
                                    latitude = latitude.toDouble(),
                                    longitude = longitude.toDouble(),
                                    weatherService = weatherService,
                                    firebaseService = firebaseService,
                                    onNavigateToLocationSettings = {
                                        navController.navigate("location_setting") // Navigate to change location
                                    },
                                    navController = navController // Pass NavController for internal navigation (e.g., to SarcasticSummaryPlayerScreen)
                                )
                            } else {
                                // Fallback if arguments are missing, navigate to login
                                Log.e("MainActivity", "Missing latitude/longitude for weather dashboard. Navigating to login.")
                                navController.navigate("login") {
                                    popUpTo("splash_placeholder") { inclusive = true } // Clear back stack
                                    launchSingleTop = true
                                }
                            }
                        }

                        // Sarcastic Summary Player Screen route, taking WFO ID as argument
                        composable(
                            route = "sarcastic_summary_player/{wfoId}",
                            arguments = listOf(
                                navArgument("wfoId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val wfoId = backStackEntry.arguments?.getString("wfoId")
                            SarcasticSummaryPlayerScreen(
                                wfoIdentifier = wfoId,
                                onNavigateToDashboard = {
                                    coroutineScope.launch {
                                        val currentUser = firebaseService.getCurrentUser()
                                        if (currentUser != null) {
                                            val userProfile = firebaseService.getUserProfile(currentUser.uid)
                                            val lat = userProfile?.get("latitude") as? Double
                                            val lon = userProfile?.get("longitude") as? Double
                                            if (lat != null && lon != null) {
                                                // Navigate back to dashboard with current user's saved location
                                                navController.navigate("weather_dashboard/$lat/$lon") {
                                                    // Clear player screen and go back to a single dashboard instance
                                                    popUpTo("weather_dashboard/{latitude}/{longitude}") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            } else {
                                                Log.e("MainActivity", "No saved location found for user ${currentUser.uid} for dashboard navigation from player. Navigating to login.")
                                                navController.navigate("login") {
                                                    popUpTo("splash_placeholder") { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        } else {
                                            Log.e("MainActivity", "No authenticated user found for dashboard navigation from player. Navigating to login.")
                                            navController.navigate("login") {
                                                popUpTo("splash_placeholder") { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
