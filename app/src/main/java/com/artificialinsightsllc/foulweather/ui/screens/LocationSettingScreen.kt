// app/src/main/java/com/artificialinsightsllc/foulweather/ui/screens/LocationSettingScreen.kt
package com.artificialinsightsllc.foulweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator // Added for loading indicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope // Added for coroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artificialinsightsllc.foulweather.data.GeocodingService // Added: Import GeocodingService
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import kotlinx.coroutines.launch // Added: Import for coroutineScope.launch

/**
 * Composable for setting the user's location.
 * Allows input of City/State or Zipcode and performs geocoding to get coordinates.
 *
 * @param geocodingService An instance of GeocodingService to convert address to coordinates.
 * @param onLocationSet Lambda function invoked when the "Get Weather" button is pressed
 * with a valid location input. It passes the original location query string,
 * and the geocoded latitude and longitude.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingScreen(
    geocodingService: GeocodingService, // ADDED: geocodingService parameter
    onLocationSet: (String, Double, Double) -> Unit // MODIFIED: Now passes locationQuery, lat, lon
) {
    var locationInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGeocodingLoading by remember { mutableStateOf(false) } // State for geocoding loading

    val coroutineScope = rememberCoroutineScope() // CoroutineScope for launching suspend functions

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Your Location", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Enter your City, State or Zipcode:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = locationInput,
                onValueChange = { locationInput = it },
                label = { Text("e.g., New York, NY or 10001") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    errorMessage = null
                    if (locationInput.isNotBlank()) {
                        isGeocodingLoading = true // Show loading indicator
                        coroutineScope.launch { // Launch coroutine for geocoding
                            val latLng = geocodingService.getLatLngFromAddress(locationInput.trim())
                            isGeocodingLoading = false // Hide loading indicator

                            if (latLng != null) {
                                onLocationSet(locationInput.trim(), latLng.lat, latLng.lng) // Pass lat/lon
                            } else {
                                errorMessage = "Could not find coordinates for the entered location. Please check and try again."
                            }
                        }
                    } else {
                        errorMessage = "Please enter a location."
                    }
                },
                modifier = Modifier.fillMaxWidth(0.7f),
                contentPadding = PaddingValues(12.dp),
                enabled = !isGeocodingLoading // Disable button while geocoding
            ) {
                Text(if (isGeocodingLoading) "Locating..." else "Get Weather", fontSize = 18.sp)
            }
        }
    }
}

/**
 * Preview function for the LocationSettingScreen Composable.
 */
@Preview(showBackground = true)
@Composable
fun PreviewLocationSettingScreen() {
    FoulWeatherTheme {
        LocationSettingScreen(
            geocodingService = object : GeocodingService() { // Mock GeocodingService for preview
                override suspend fun getLatLngFromAddress(address: String): com.artificialinsightsllc.foulweather.data.models.LatLngLiteral? {
                    // Simulate a geocoding result for preview
                    return com.artificialinsightsllc.foulweather.data.models.LatLngLiteral(34.0522, -118.2437) // Example: Los Angeles
                }
            },
            onLocationSet = { query, lat, lon ->
                println("Location set in preview: $query (Lat: $lat, Lon: $lon)")
            }
        )
    }
}
