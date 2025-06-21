// app/src/main/java/com/artificialinsightsllc/foulweather/ui/screens/WeatherDashboardScreen.kt
package com.artificialinsightsllc.foulweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// REMOVED: import coil.compose.AsyncImage
// ADDED: Glide imports
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import android.graphics.drawable.ColorDrawable // For simple placeholders/errors

import com.artificialinsightsllc.foulweather.data.WeatherService
// Explicit imports for all data models used in this file
import com.artificialinsightsllc.foulweather.data.models.AlertFeature
import com.artificialinsightsllc.foulweather.data.models.AlertProperties
import com.artificialinsightsllc.foulweather.data.models.CloudLayer
import com.artificialinsightsllc.foulweather.data.models.Dewpoint
import com.artificialinsightsllc.foulweather.data.models.Distance
import com.artificialinsightsllc.foulweather.data.models.Elevation
import com.artificialinsightsllc.foulweather.data.models.ForecastProperties
import com.artificialinsightsllc.foulweather.data.models.ForecastResponse
import com.artificialinsightsllc.foulweather.data.models.Geocode
import com.artificialinsightsllc.foulweather.data.models.Geometry
import com.artificialinsightsllc.foulweather.data.models.GridpointProperties
import com.artificialinsightsllc.foulweather.data.models.HourlyPeriod
import com.artificialinsightsllc.foulweather.data.models.LatestObservationProperties
import com.artificialinsightsllc.foulweather.data.models.LocationProperties
import com.artificialinsightsllc.foulweather.data.models.Period
import com.artificialinsightsllc.foulweather.data.models.ProbabilityOfPrecipitation
import com.artificialinsightsllc.foulweather.data.models.RelativeHumidity
import com.artificialinsightsllc.foulweather.data.models.RelativeLocation
import com.artificialinsightsllc.foulweather.data.models.StationFeature
import com.artificialinsightsllc.foulweather.data.models.StationProperties
import com.artificialinsightsllc.foulweather.data.models.ValueUnit
import com.artificialinsightsllc.foulweather.data.models.NWSProductType
import com.artificialinsightsllc.foulweather.data.models.ProductResponse
// End explicit imports
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import android.util.Log
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.clickable
import com.artificialinsightsllc.foulweather.data.FirebaseService
import com.artificialinsightsllc.foulweather.data.SpeechService
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.VolumeUp
import android.media.MediaPlayer
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavController // ADDED: Import NavController for navigation
import kotlinx.coroutines.delay // ADDED: Import for coroutine delay
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders


/**
 * Static list of NWS product types available for display.
 * This list is used to populate the "Weather Products" dropdown menu.
 */
val NWS_PRODUCT_TYPES = listOf(
    NWSProductType("AFD", "Area Forecast Discussion"),
    NWSProductType("ZFP", "Zone Forecast Product"),
    NWSProductType("CF6", "WFO Monthly/Daily Climate Data"),
    NWSProductType("CLI", "Climatological Report (Daily)"),
    NWSProductType("CWF", "Coastal Waters Forecast"),
    NWSProductType("FFH", "Headwater Guidance"),
    NWSProductType("FLS", "Flood Statement"),
    NWSProductType("FWF", "Routine Fire Wx Fcst"),
    NWSProductType("FWM", "Miscellaneous Fire Weather Product"),
    NWSProductType("FWN", "Fire Weather Notification"),
    NWSProductType("FWO", "Fire Weather Observation"),
    NWSProductType("FWS", "Suppression Forecast"),
    NWSProductType("FZL", "Freezing Level Data (RADAT)"),
    NWSProductType("HML", "AHPS XML"),
    NWSProductType("LCO", "Local Cooperative Observation"),
    NWSProductType("LSR", "Local Storm Report"),
    NWSProductType("MAN", "Rawinsonde Obs Mandatory Levels"),
    NWSProductType("MWS", "Marine Weather Statement"),
    NWSProductType("PFM", "Point Forecast Matrices"),
    NWSProductType("PNS", "Public Information Statement"),
    NWSProductType("RR1", "Hydro-Met Data Report Part 1"),
    NWSProductType("RR2", "Hydro-Met Data Report Part 2"),
    NWSProductType("RR3", "Hydro-Met Data Report Part 3"),
    NWSProductType("RRS", "HADS Data"),
    NWSProductType("RWR", "Regional Weather Roundup"),
    NWSProductType("SFT", "Tabular State Forecast"),
    NWSProductType("SGL", "Rawinsonde Obs Significant Levels"),
    NWSProductType("SMW", "Special Marine Warning"),
    NWSProductType("SPS", "Special Weather Statement"),
    NWSProductType("SVR", "Severe Thunderstorm Warning"),
    NWSProductType("SVS", "Severe Weather Statement"),
    NWSProductType("TMA", "Tsunami Tide/Seismic Message Ack"),
    NWSProductType("VFT", "TAF Verification"),
    NWSProductType("ZFP", "Zone Forecast Product")
)


/**
 * Composable for displaying the weather forecast dashboard.
 * This screen fetches and presents daily and hourly weather data for the specified location.
 *
 * MODIFIED: Now accepts latitude and longitude instead of a single locationQuery string.
 * This allows fetching live weather data based on geocoded coordinates.
 * MODIFIED: Added a hamburger menu with "Change Location" and "Area Forecast" options.
 * MODIFIED: Enhanced current conditions display with a scrollable row of detailed cards and last update time.
 * ADDED: Section for displaying nearby observation stations and their latest data.
 * MODIFIED: ObservationStationCard now displays station name below the identifier.
 * MODIFIED: Changed main title to "Forecast For" and entire page background to black.
 * MODIFIED: Adjusted "Current Weather" section title to "Forecast for {name}" and added short forecast.
 * MODIFIED: Changed main icon size and URL to 'large'.
 * MODIFIED: Changed forecast timestamp to "This forecast is valid from startTime to endTime".
 * REMOVED: Area Forecast dialog (replaced by specific product text dialog).
 * ADDED: "Click day for Details" subtitle to 7-Day Forecast section.
 * ADDED: Click functionality to DailyForecastCard to show an AlertDialog with detailed forecast.
 * ADDED: FirebaseService dependency.
 * ADDED: Logic to save the NWS forecast zone to the user's Firestore profile upon data fetch.
 * REMOVED: All mock data and mock FirebaseService implementation from Preview.
 * FIXED: Extracts only the zone ID (e.g., "FLZ149") from the forecastZone URL before saving to Firestore.
 * ADDED: Logic to fetch and display weather alerts based on the forecast zone.
 * ADDED: Top alert bar with dynamic background color based on alert severity.
 * ADDED: Individual alert cards below the current forecast area.
 * ADDED: Functionality to fetch and display NWS product texts in a scrollable dialog.
 * ADDED: Loading spinner for product text fetching.
 * ADDED: Storage of WFO identifier (e.g., "TBW") in Firestore.
 * FIXED: Restructured dropdown menu to directly list weather products, resolving nested composable errors.
 * ADDED: Horizontal scrolling for product text within the AlertDialog.
 * ADDED: Floating Action Button (FAB) to trigger sarcastic summary audio generation and playback.
 * ADDED: New dialog to display sarcastic summary loading/playback status.
 * ADDED: MediaPlayer integration for playing audio.
 * ADDED: DisposableEffect for MediaPlayer resource management.
 * MODIFIED: Changed temporary audio file extension from .mp3 to .wav to match WAV output.
 * MODIFIED: Implemented FCM WFO topic subscription/unsubscription logic.
 * MODIFIED: FAB now navigates to `SarcasticSummaryPlayerScreen` instead of generating audio directly.
 * ADDED: A sticky footer at the bottom with copyright information.
 * MODIFIED: Changed WFO identifier extraction to use `gridData.forecastOffice` and get the last 3 characters of the URL.
 * ADDED: Radar GIF display with a refresh button.
 * MODIFIED: Removed manual "Refresh Radar" button and added auto-refresh every 5 minutes for the radar GIF.
 * MODIFIED: Replaced the manual refresh button with text showing the last update time and auto-refresh status.
 *
 * @param latitude The latitude coordinate for the desired weather location.
 * @param longitude The longitude coordinate for the longitude location.
 * @param weatherService An instance of WeatherService to make API calls.
 * @param firebaseService An instance of FirebaseService to update user profile and manage FCM topics.
 * @param onNavigateToLocationSettings Callback to navigate to the Location Setting screen.
 * @param navController The NavController used for navigation within the app.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class) // ADDED ExperimentalGlideComposeApi opt-in
@Composable
fun WeatherDashboardScreen(
    latitude: Double,
    longitude: Double,
    weatherService: WeatherService,
    firebaseService: FirebaseService,
    onNavigateToLocationSettings: () -> Unit,
    navController: NavController // ADDED: NavController parameter
) {
    val context = LocalContext.current

    // Initialize SpeechService - No longer directly used for generation on this screen
    // val speechService = remember { SpeechService(weatherService, firebaseService) }

    // States to hold the fetched weather data
    var gridpointData by remember { mutableStateOf<GridpointProperties?>(null) }
    var dailyForecastResponse by remember { mutableStateOf<ForecastResponse?>(null) }
    var hourlyForecast by remember { mutableStateOf<List<HourlyPeriod>?>(null) }
    var nearbyObservations by remember { mutableStateOf<List<Pair<String, LatestObservationProperties>>>(emptyList()) }
    var activeAlerts by remember { mutableStateOf<List<AlertFeature>?>(null) }
    var issuingOfficeId by remember { mutableStateOf<String?>(null) }
    // State to manage loading indicator visibility
    var isLoading by remember { mutableStateOf(false) }
    // State to hold and display error messages
    var error: String? by remember { mutableStateOf(null) }

    // State for managing the dropdown menu's expanded state
    var menuExpanded by remember { mutableStateOf(false) }

    // State for managing the product text display dialog
    var showProductTextDialog by remember { mutableStateOf(false) }
    var productTextContent by remember { mutableStateOf<String?>(null) }
    var productTextLoading by remember { mutableStateOf(false) }
    var productTextIssuanceTime by remember { mutableStateOf<String?>(null) }
    var selectedProductType by remember { mutableStateOf<NWSProductType?>(null) }

    // State for radar GIF
    var radarStationId: String? by remember { mutableStateOf(null) }
    // Temporarily remove timestamp from model as per user request to focus on initial display
    var refreshRadarGifTrigger by remember { mutableStateOf(0) } // Keep this for future refresh re-implementation
    var lastRadarUpdateTime by remember { mutableStateOf<ZonedDateTime?>(null) } // Tracks when the radar GIF was last updated

    // State to store the previously subscribed WFO ID for unsubscription
    var previousWfoIdentifier by rememberSaveable { mutableStateOf<String?>(null) }

    // State for holding the Period data for the clicked daily forecast card
    var selectedDailyForecastPeriod by remember { mutableStateOf<Period?>(null) }
    // State for managing the visibility of the Daily Forecast Details dialog
    var showDailyForecastDetailDialog by remember { mutableStateOf(false) }

    // Coroutine scope for launching suspend functions (API calls)
    val coroutineScope = rememberCoroutineScope()

    // Get screen width for dynamic icon sizing
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    /**
     * LaunchedEffect is used to trigger data fetching when the `latitude` or `longitude` changes.
     * It ensures the API calls are made safely within a Composable and handle lifecycle.
     */
    LaunchedEffect(latitude, longitude) {
        // Reset states and show loading indicator
        isLoading = true
        error = null
        // Clear previous observations and alerts when new location is loaded
        nearbyObservations = emptyList()
        activeAlerts = null // Clear alerts
        issuingOfficeId = null // Clear issuing office ID
        radarStationId = null // Clear radar station ID

        coroutineScope.launch {
            try {
                // Step 1: Get gridpoint data for the location using provided lat/lon
                val newGridpointData = weatherService.getGridpointData(latitude, longitude)
                gridpointData = newGridpointData

                newGridpointData?.let { gridData ->
                    // Get current user for Firestore update
                    val currentUser = firebaseService.getCurrentUser()
                    var userForecastZone: String? = null

                    // Save forecastZone to Firestore if available and user is logged in
                    gridData.forecastZone?.let { fullZoneUrl ->
                        val zoneId = fullZoneUrl.substringAfterLast("/")
                        userForecastZone = zoneId
                        currentUser?.uid?.let { userId ->
                            firebaseService.updateUserForecastZone(userId, zoneId)
                        }
                    }

                    // Extract and store the WFO identifier (e.g., "MFL" from "https://api.weather.gov/offices/MFL")
                    gridData.forecastOffice?.let { officeUrl ->
                        val currentWfoId = officeUrl.substringAfterLast("/") // Extract last 3 letters from URL
                        issuingOfficeId = currentWfoId // Store the stripped WFO ID
                        currentUser?.uid?.let { userId ->
                            firebaseService.updateUserWFOIdentifier(userId, currentWfoId) // Save to Firestore

                            // FCM Topic Management: Unsubscribe from old, subscribe to new
                            if (previousWfoIdentifier != null && previousWfoIdentifier != currentWfoId) {
                                firebaseService.unsubscribeFromWfoTopic(previousWfoIdentifier!!)
                                Log.d("FCM", "Unsubscribed from old WFO topic: ${previousWfoIdentifier!!}")
                            }
                            if (currentWfoId.isNotBlank()) {
                                firebaseService.subscribeToWfoTopic(currentWfoId)
                                Log.d("FCM", "Subscribed to new WFO topic: $currentWfoId")
                            }
                            previousWfoIdentifier = currentWfoId // Update previous for next time
                        }
                    }

                    // Extract and store the Radar Station ID (e.g., "KTBW")
                    gridData.radarStation?.let { stationId ->
                        radarStationId = stationId // Store the radar station ID
                        currentUser?.uid?.let { userId ->
                            firebaseService.updateUserRadarStation(userId, stationId) // Save to Firestore
                        }
                    }
                    // refreshRadarGifTrigger++ // Do not trigger refresh for now
                    lastRadarUpdateTime = ZonedDateTime.now() // Set initial update time

                    // Step 2: If gridpoint data is successful, fetch daily forecast (full response)
                    val daily = weatherService.getDailyForecast(gridData.forecast)
                    dailyForecastResponse = daily

                    // Step 3: If gridpoint data is successful, fetch hourly forecast
                    val hourly = weatherService.getHourlyForecast(gridData.forecastHourly)
                    hourlyForecast = hourly

                    // Step 4: Fetch nearby observation stations and their latest observations
                    gridData.observationStations?.let { stationsUrl ->
                        val stations = weatherService.getNearbyObservationStations(stationsUrl)
                        val observationsList = mutableListOf<Pair<String, LatestObservationProperties>>()
                        stations?.take(8)?.forEach { stationFeature ->
                            weatherService.getLatestObservation(stationFeature.properties.stationIdentifier)?.let { obs ->
                                observationsList.add(Pair(stationFeature.properties.name, obs))
                            }
                        }
                        nearbyObservations = observationsList
                        Log.d("WeatherDashboard", "Fetched ${nearbyObservations.size} nearby observations.")
                    }

                    // Step 5: Fetch active alerts for the forecast zone
                    userForecastZone?.let { zone ->
                        val alerts = weatherService.getAlertsForZone(zone)
                        activeAlerts = alerts
                        Log.d("WeatherDashboard", "Fetched ${alerts?.size ?: 0} active alerts for zone $zone.")
                    }

                } ?: run {
                    // Set error if gridpoint data could not be retrieved
                    error = "Could not retrieve weather gridpoint data for ($latitude, $longitude). Check your API key and internet connection."
                }
            } catch (e: Exception) {
                // Catch any exceptions during the entire data fetching process
                error = "An error occurred: ${e.localizedMessage ?: "Unknown error"}. Please try again."
                // Log the full stack trace for debugging purposes
                Log.e("WeatherDashboard", "Error fetching weather data: ${e.stackTraceToString()}")
            } finally {
                isLoading = false // Hide loading indicator regardless of success or failure
            }
        }
    }

    // NEW: LaunchedEffect to auto-refresh the radar GIF every 5 minutes
    // Re-enabling the refresh mechanism
    LaunchedEffect(radarStationId) { // Re-launch if radar station changes
        if (radarStationId != null) {
            while (true) {
                delay(5 * 60 * 1000L) // Delay for 5 minutes (5 * 60 seconds * 1000 milliseconds)
                refreshRadarGifTrigger++ // Increment to force GlideImage to reload
                lastRadarUpdateTime = ZonedDateTime.now() // Update timestamp
                Log.d("WeatherDashboard", "Radar GIF auto-refresh triggered.")
            }
        }
    }

    // Scroll state for the entire screen content
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Weather Dashboard",
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.LightGray),
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Change Location") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToLocationSettings()
                                }
                            )
                            NWS_PRODUCT_TYPES.forEach { productType ->
                                DropdownMenuItem(
                                    text = { Text(productType.name) },
                                    onClick = {
                                        menuExpanded = false
                                        selectedProductType = productType
                                        productTextLoading = true
                                        showProductTextDialog = true
                                        productTextContent = null
                                        productTextIssuanceTime = null

                                        coroutineScope.launch {
                                            val officeId = issuingOfficeId
                                            if (officeId != null) {
                                                val productResponse = weatherService.getProductText(productType.code, officeId)
                                                if (productResponse != null && productResponse.productText.isNotBlank()) {
                                                    productTextContent = productResponse.productText
                                                    val zonedDateTime = ZonedDateTime.parse(productResponse.issuanceTime)
                                                    val formatter = DateTimeFormatter.ofPattern("MMM dd,yyyy HH:mm zzz", Locale.getDefault())
                                                    productTextIssuanceTime = "Issued: ${zonedDateTime.format(formatter)}"
                                                } else {
                                                    productTextContent = "No data available for this product type: ${productType.name} at location $officeId."
                                                }
                                            } else {
                                                productTextContent = "Weather Office ID not available. Please ensure your location is set correctly."
                                            }
                                            productTextLoading = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                )

                activeAlerts?.let { alerts ->
                    val highestSeverityColor = getHighestSeverityColor(alerts)
                    val alertMessage = if (alerts.isNotEmpty()) "Active Alerts - See Below" else "No active alerts"
                    val textColor = Color.White

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (alerts.isNotEmpty()) highestSeverityColor else Color.Green.copy(alpha = 0.5f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alertMessage,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Sticky footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(vertical = 4.dp), // Add vertical padding
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "(c) 2025 Artificial Insights, LLC.",
                    color = Color.Black,
                    fontSize = 9.sp, // Use 9.sp for font size
                    textAlign = TextAlign.Center
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // MODIFIED: Navigate to the SarcasticSummaryPlayerScreen
                    if (issuingOfficeId != null) {
                        navController.navigate("sarcastic_summary_player/${issuingOfficeId}")
                    } else {
                        Log.e("WeatherDashboard", "Cannot navigate to SarcasticSummaryPlayerScreen: WFO Identifier is null.")
                        // Optionally, show a Toast or AlertDialog to the user
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.VolumeUp, "Listen to Sarcastic Summary", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("Loading weather data...", modifier = Modifier.padding(top = 8.dp), color = Color.White)
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                Button(onClick = {
                    coroutineScope.launch {
                        isLoading = true
                        error = null
                        val newGridpointData = weatherService.getGridpointData(latitude, longitude)
                        gridpointData = newGridpointData
                        newGridpointData?.let { gridData ->
                            val currentUser = firebaseService.getCurrentUser()
                            var userForecastZone: String? = null

                            gridData.forecastZone?.let { fullZoneUrl ->
                                val zoneId = fullZoneUrl.substringAfterLast("/")
                                userForecastZone = zoneId
                                currentUser?.uid?.let { userId ->
                                    firebaseService.updateUserForecastZone(userId, zoneId)
                                }
                            }
                            gridData.forecastOffice?.let { officeUrl ->
                                val currentWfoId = officeUrl.substringAfterLast("/")
                                issuingOfficeId = currentWfoId
                                currentUser?.uid?.let { userId ->
                                    firebaseService.updateUserWFOIdentifier(userId, currentWfoId)

                                    if (previousWfoIdentifier != null && previousWfoIdentifier != currentWfoId) {
                                        firebaseService.unsubscribeFromWfoTopic(previousWfoIdentifier!!)
                                        Log.d("FCM", "Unsubscribed from old WFO topic (retry): ${previousWfoIdentifier!!}")
                                    }
                                    if (currentWfoId.isNotBlank()) {
                                        firebaseService.subscribeToWfoTopic(currentWfoId)
                                        Log.d("FCM", "Subscribed to new WFO topic (retry): $currentWfoId")
                                    }
                                    previousWfoIdentifier = currentWfoId
                                }
                            }
                            gridData.radarStation?.let { stationId ->
                                radarStationId = stationId
                                currentUser?.uid?.let { userId ->
                                    firebaseService.updateUserRadarStation(userId, stationId)
                                }
                            }
                            // Re-enable refresh on retry
                            refreshRadarGifTrigger++
                            lastRadarUpdateTime = ZonedDateTime.now() // Update time on retry

                            dailyForecastResponse = weatherService.getDailyForecast(gridData.forecast)
                            hourlyForecast = weatherService.getHourlyForecast(gridData.forecastHourly)
                            gridData.observationStations?.let { stationsUrl ->
                                val stations = weatherService.getNearbyObservationStations(stationsUrl)
                                val observations = mutableListOf<Pair<String, LatestObservationProperties>>()
                                stations?.take(8)?.forEach { stationFeature ->
                                    weatherService.getLatestObservation(stationFeature.properties.stationIdentifier)?.let { obs ->
                                        observations.add(Pair(stationFeature.properties.name, obs))
                                    }
                                }
                                nearbyObservations = observations
                            }
                            userForecastZone?.let { zone ->
                                activeAlerts = weatherService.getAlertsForZone(zone)
                            }
                        } ?: run {
                            error = "Could not retrieve weather gridpoint data for ($latitude, $longitude)."
                        }
                        isLoading = false
                    }
                }) {
                    Text("Retry")
                }
            } else if (gridpointData == null) {
                Text("No weather data found for ($latitude, $longitude).",
                    modifier = Modifier.padding(16.dp), color = Color.White)
            } else {
                gridpointData?.relativeLocation?.properties?.let {
                    Text(
                        text = "Forecast For\n${it.city}, ${it.state}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                dailyForecastResponse?.properties?.periods?.firstOrNull()?.let { currentPeriod ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Forecast for ${currentPeriod.name}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = currentPeriod.shortForecast,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.LightGray,
                            modifier = Modifier.padding(bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        val largeIconUrl = currentPeriod.icon.replace("?size=medium", "?size=large")
                        GlideImage( // Replaced AsyncImage with GlideImage
                            model = GlideUrl(
                                largeIconUrl,
                                LazyHeaders.Builder()
                                    .addHeader("User-Agent", "FoulWeatherApp (rdspromo@gmail.com)")
                                    .build()
                            ),
                            contentDescription = currentPeriod.shortForecast,
                            modifier = Modifier.size(screenWidth / 2),
                            requestBuilderTransform = {
                                // Add placeholder and error for main icon
                                it.placeholder(ColorDrawable(android.graphics.Color.GRAY))
                                    .error(ColorDrawable(android.graphics.Color.RED))
                            }
                        )
                        Text(
                            text = "${currentPeriod.temperature}°${currentPeriod.temperatureUnit}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 50.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        val startTime = ZonedDateTime.parse(currentPeriod.startTime)
                        val endTime = ZonedDateTime.parse(currentPeriod.endTime)
                        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

                        Text(
                            text = "This forecast is valid from ${startTime.format(timeFormatter)} to ${endTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = currentPeriod.detailedForecast,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        item {
                            CurrentConditionCard(
                                emoji = "💨", // Set Wind emoji
                                label = "WIND",
                                value = "${currentPeriod.windSpeed} ${currentPeriod.windDirection}"
                            )
                        }
                        currentPeriod.probabilityOfPrecipitation.value?.let { pop ->
                            item {
                                CurrentConditionCard(
                                    emoji = "🌧️",
                                    label = "RAIN CHANCE",
                                    value = "$pop%"
                                )
                            }
                        }
                        hourlyForecast?.firstOrNull()?.relativeHumidity?.value?.let { humidity ->
                            item {
                                CurrentConditionCard(
                                    emoji = "💧", // Set Humidity emoji
                                    label = "HUMIDITY",
                                    value = "$humidity%"
                                )
                            }
                        }
                        hourlyForecast?.firstOrNull()?.dewpoint?.value?.let { dewpointC ->
                            val dewpointF = String.format("%.1f", (dewpointC * 9/5) + 32)
                            item {
                                CurrentConditionCard(
                                    emoji = "🌡️",
                                    label = "DEWPOINT",
                                    value = "$dewpointF°F"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Radar GIF section
                if (radarStationId != null) {
                    val radarGifUrl = "https://radar.weather.gov/ridge/standard/${radarStationId}_loop.gif"
                    Text(
                        text = "Current Radar Loop",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(vertical = 8.dp)
                    )
                    GlideImage(
                        model = GlideUrl(
                            radarGifUrl + "?timestamp=${refreshRadarGifTrigger}", // Added timestamp back for refresh
                            LazyHeaders.Builder()
                                .addHeader("User-Agent", "FoulWeatherApp (rdspromo@gmail.com)")
                                .build()
                        ),
                        contentDescription = "Local Radar Loop",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(screenWidth),
                        contentScale = ContentScale.Fit,
                        requestBuilderTransform = {
                            it
                                .placeholder(ColorDrawable(android.graphics.Color.DKGRAY))
                                .error(ColorDrawable(android.graphics.Color.RED))
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Text indicating last update time and auto-refresh
                    lastRadarUpdateTime?.let {
                        val formatter = DateTimeFormatter.ofPattern("MMM dd,yyyy h:mm:ss a", Locale.getDefault())
                        Text(
                            text = "Last updated: ${it.format(formatter)} (Auto-refreshing every 5 minutes)", // Updated text
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }


                activeAlerts?.let { alerts ->
                    if (alerts.isNotEmpty()) {
                        Text(
                            text = "Active Alerts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(vertical = 8.dp)
                        )
                        alerts.forEach { alert ->
                            AlertCard(alert = alert)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                if (nearbyObservations.isNotEmpty()) {
                    Text(
                        text = "Nearby Observations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(nearbyObservations) { (stationName, observation) ->
                            ObservationStationCard(stationName = stationName, observation = observation)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }


                hourlyForecast?.let {
                    Text(
                        text = "Hourly Forecast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(vertical = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(it.take(24)) { hourlyPeriod ->
                            HourlyForecastCard(hourlyPeriod = hourlyPeriod)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                dailyForecastResponse?.properties?.periods?.let { dailyPeriods ->
                    Text(
                        text = "7-Day Forecast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Click day for Details",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color.LightGray,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 8.dp)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(dailyPeriods.drop(1)) { dailyPeriod ->
                            DailyForecastCard(
                                period = dailyPeriod,
                                onClick = {
                                    selectedDailyForecastPeriod = dailyPeriod
                                    showDailyForecastDetailDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProductTextDialog) {
        AlertDialog(
            onDismissRequest = { showProductTextDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = selectedProductType?.name ?: "Weather Product",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    productTextIssuanceTime?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, textAlign = TextAlign.Center)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (productTextLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        Text("Loading product...", color = Color.White)
                    } else {
                        productTextContent?.let {
                            Text(it, color = Color.White, textAlign = TextAlign.Start)
                        } ?: Text("No data available.", color = Color.White, textAlign = TextAlign.Start)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProductTextDialog = false }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color.Black
        )
    }

    selectedDailyForecastPeriod?.let { period ->
        if (showDailyForecastDetailDialog) {
            AlertDialog(
                onDismissRequest = { showDailyForecastDetailDialog = false },
                title = {
                    Text(
                        text = period.name,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val largeIconUrl = period.icon.replace("?size=medium", "?size=large")
                        GlideImage( // Replaced AsyncImage with GlideImage
                            model = GlideUrl(
                                largeIconUrl,
                                LazyHeaders.Builder()
                                    .addHeader("User-Agent", "FoulWeatherApp (rdspromo@gmail.com)")
                                    .build()
                            ),
                            contentDescription = period.shortForecast,
                            modifier = Modifier.size(screenWidth / 2),
                            requestBuilderTransform = {
                                // Add placeholder and error for detailed daily forecast icon
                                it.placeholder(ColorDrawable(android.graphics.Color.GRAY))
                                    .error(ColorDrawable(android.graphics.Color.RED))
                            }
                        )

                        Text(
                            text = "${period.temperature}°${period.temperatureUnit}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurrentConditionCard(
                                emoji = "�",
                                label = "WIND",
                                value = "${period.windSpeed} ${period.windDirection}"
                            )
                            period.probabilityOfPrecipitation.value?.let { pop ->
                                CurrentConditionCard(
                                    emoji = "🌧️",
                                    label = "RAIN CHANCE",
                                    value = "$pop%"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = period.detailedForecast,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    textAlign = TextAlign.Start,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDailyForecastDetailDialog = false }) {
                        Text("Close", color = Color.White)
                    }
                },
                containerColor = Color.Black
            )
        }
    }
}

/**
 * Composable for an individual current condition card (e.g., Wind, Humidity).
 * Displays an emoji, a label, and a value, centered in a small card.
 *
 * @param emoji The emoji string to display.
 * @param label The label for the condition (e.g., "WIND").
 * @param value The value of the condition (e.g., "5 mph SSE").
 */
@Composable
fun CurrentConditionCard(emoji: String, label: String, value: String) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp, modifier = Modifier.padding(bottom = 4.dp))
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = value, fontSize = 14.sp)
        }
    }
}

/**
 * Composable for an individual hourly forecast card.
 * Displays time, icon, temperature, and chance of precipitation.
 *
 * @param hourlyPeriod The HourlyPeriod data object to display.
 */
@OptIn(ExperimentalGlideComposeApi::class) // Added OptIn
@Composable
fun HourlyForecastCard(hourlyPeriod: HourlyPeriod) {
    val timeFormatter = DateTimeFormatter.ofPattern("h a", Locale.getDefault())
    val zonedDateTime = ZonedDateTime.parse(hourlyPeriod.startTime)

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Text(text = zonedDateTime.format(timeFormatter), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            GlideImage( // Replaced AsyncImage with GlideImage
                model = GlideUrl(
                    hourlyPeriod.icon,
                    LazyHeaders.Builder()
                        .addHeader("User-Agent", "FoulWeatherApp (rdspromo@gmail.com)")
                        .build()
                ),
                contentDescription = hourlyPeriod.shortForecast,
                modifier = Modifier.size(40.dp),
                requestBuilderTransform = {
                    // Add placeholder and error for hourly icon
                    it.placeholder(ColorDrawable(android.graphics.Color.LTGRAY))
                        .error(ColorDrawable(android.graphics.Color.RED))
                }
            )
            Text(text = "${hourlyPeriod.temperature}°${hourlyPeriod.temperatureUnit}", fontSize = 16.sp)
            hourlyPeriod.probabilityOfPrecipitation.value?.let { pop ->
                Text(text = "$pop%", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

/**
 * Composable for an individual daily forecast card.
 * Displays day/date, short forecast, icon, temperature, and chance of precipitation.
 *
 * @param period The Period data object to display.
 * @param onClick Lambda function to be invoked when the card is clicked.
 */
@OptIn(ExperimentalGlideComposeApi::class) // Added OptIn
@Composable
fun DailyForecastCard(period: Period, onClick: (Period) -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM dd", Locale.getDefault())
    val zonedDateTime = ZonedDateTime.parse(period.startTime)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(period) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (period.name.isNotBlank()) period.name else zonedDateTime.format(dateFormatter),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(text = period.shortForecast, fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            GlideImage( // Replaced AsyncImage with GlideImage
                model = GlideUrl(
                    period.icon,
                    LazyHeaders.Builder()
                        .addHeader("User-Agent", "FoulWeatherApp (rdspromo@gmail.com)")
                        .build()
                ),
                contentDescription = period.shortForecast,
                modifier = Modifier.size(50.dp),
                requestBuilderTransform = {
                    // Add placeholder and error for daily icon
                    it.placeholder(ColorDrawable(android.graphics.Color.LTGRAY))
                        .error(ColorDrawable(android.graphics.Color.RED))
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${period.temperature}°${period.temperatureUnit}",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            period.probabilityOfPrecipitation.value?.let { pop ->
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "$pop%", fontSize = 14.sp, color = Color.Blue)
            }
        }
    }
}

/**
 * Composable for displaying a single nearby observation station's data.
 *
 * @param stationName The human-readable name of the observation station.
 * @param observation The LatestObservationProperties object containing the observation data.
 */
@Composable
fun ObservationStationCard(stationName: String, observation: LatestObservationProperties) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = observation.station.substringAfterLast("/"),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = stationName,
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp),
                lineHeight = 9.sp,
                textAlign = TextAlign.Center
            )
            observation.textDescription?.let {
                Text(it, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            observation.temperature?.value?.let { tempC ->
                val tempF = String.format("%.0f", (tempC * 9/5) + 32)
                Text("🌡️ $tempF°F", fontSize = 14.sp)
            }
            observation.barometricPressure?.value?.let { mb ->
                val inHg = String.format("%.2f", mb / 33.864)
                Text("📊 ${inHg} inHg", fontSize = 14.sp)
            }
            if (observation.windSpeed?.value != null && observation.windDirection?.value != null) {
                val windSpeed = observation.windSpeed.value!!.toInt()
                val windDir = observation.windDirection.value!!.toInt()
                val cardinalDir = getCardinalDirection(windDir)
                Text("🌬️ ${windSpeed} mph ${cardinalDir}", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Composable for displaying a single active weather alert.
 * The card's background color dynamically changes based on the alert's severity.
 *
 * @param alert The [AlertFeature] object containing the alert details.
 */
@Composable
fun AlertCard(alert: AlertFeature) {
    val backgroundColor = getAlertSeverityColor(alert.properties.severity)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            alert.properties.headline?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } ?: Text(
                text = alert.properties.event,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "${alert.properties.event} for ${alert.properties.areaDesc}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = alert.properties.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            alert.properties.instruction?.let {
                Text(
                    text = "Instructions: $it",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            val sentTime = ZonedDateTime.parse(alert.properties.sent)
            val formatter = DateTimeFormatter.ofPattern("MMM dd,yyyy HH:mm z")
            Text(
                text = "Issued: ${sentTime.format(formatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Determines the background color for an alert card based on its severity.
 *
 * @param severity The severity string from the NWS API (e.g., "Extreme", "Severe", "Moderate", "Minor", "Unknown").
 * @return A [Color] object corresponding to the severity.
 */
fun getAlertSeverityColor(severity: String): Color {
    return when (severity.lowercase(Locale.getDefault())) {
        "extreme" -> Color(0xFF8B0000)
        "severe" -> Color(0xFFCC0000)
        "moderate" -> Color(0xFFFFA500)
        "minor", "unknown", "advisory", "watch" -> Color(0xFFCCCC00)
        else -> Color.Gray
    }.copy(alpha = 0.8f)
}

/**
 * Determines the highest severity color from a list of alerts.
 * This is used for the top alert status bar.
 *
 * @param alerts A list of [AlertFeature] objects.
 * @return The [Color] representing the highest severity among the alerts.
 */
fun getHighestSeverityColor(alerts: List<AlertFeature>): Color {
    if (alerts.isEmpty()) return Color.Green.copy(alpha = 0.5f)

    val severityOrder = listOf("extreme", "severe", "moderate", "minor", "unknown", "advisory", "watch")

    var highestSeverityIndex = severityOrder.size

    alerts.forEach { alert ->
        val currentSeverity = alert.properties.severity.lowercase(Locale.getDefault())
        val index = severityOrder.indexOf(currentSeverity)
        if (index != -1 && index < highestSeverityIndex) {
            highestSeverityIndex = index
        }
    }

    val highestSeverity = severityOrder.getOrNull(highestSeverityIndex) ?: "unknown"
    return getAlertSeverityColor(highestSeverity)
}

// Helper function to convert degrees to cardinal direction
private fun getCardinalDirection(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val normalizedDegrees = (degrees % 360 + 360) % 360
    return directions[(normalizedDegrees / 22.5).toInt()]
}

// Removed the entire PreviewWeatherDashboardScreen composable as it is no longer needed.