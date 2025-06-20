// app/src/main/java/com/artificialinsightsllc/foulweather/ui/screens/SignupScreen.kt
package com.artificialinsightsllc.foulweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artificialinsightsllc.foulweather.data.GeocodingService // ADDED: Import GeocodingService
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import kotlinx.coroutines.launch // ADDED: Import for coroutineScope.launch
import androidx.compose.foundation.rememberScrollState // ADDED
import androidx.compose.foundation.verticalScroll // ADDED

/**
 * Data class to hold the signup information.
 * This will be passed to the callback once the user attempts to sign up.
 * MODIFIED: Added 'password' field and optional latitude/longitude.
 */
data class SignupInfo(
    val email: String,
    val password: String,
    val city: String,
    val state: String,
    val zipcode: String? = null,
    val latitude: Double? = null, // ADDED: Latitude field for geocoded location
    val longitude: Double? = null // ADDED: Longitude field for geocoded location
)

/**
 * Composable for the Signup Screen.
 * This screen allows new users to register an account by providing
 * email, password, and their initial location details.
 *
 * @param geocodingService An instance of GeocodingService to convert address to coordinates.
 * @param onSignupSuccess Callback function invoked upon successful validation of input.
 * It provides the SignupInfo object, now including geocoded coordinates if successful.
 * @param onNavigateToLogin Callback to navigate back to the login screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    geocodingService: GeocodingService, // ADDED: geocodingService parameter
    onSignupSuccess: (SignupInfo) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // State variables for user input
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verifyPassword by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zipcode by remember { mutableStateOf("") }

    // State variables for password visibility
    var passwordVisible by remember { mutableStateOf(false) }
    var verifyPasswordVisible by remember { mutableStateOf(false) }

    // State for displaying error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isGeocodingLoading by remember { mutableStateOf(false) } // New state for geocoding loading

    val coroutineScope = rememberCoroutineScope() // CoroutineScope for launching suspend functions
    val scrollState = rememberScrollState() // ADDED: Scroll state for vertical scrolling

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState), // ADDED: Make the column scrollable
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Join Foul Weather",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        trailingIcon = {
                            val image = if (passwordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Verify Password Input
                    OutlinedTextField(
                        value = verifyPassword,
                        onValueChange = { verifyPassword = it },
                        label = { Text("Verify Password") },
                        visualTransformation = if (verifyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        trailingIcon = {
                            val image = if (verifyPasswordVisible)
                                Icons.Filled.Visibility
                            else Icons.Filled.VisibilityOff
                            val description = if (verifyPasswordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { verifyPasswordVisible = !verifyPasswordVisible }) {
                                Icon(imageVector = image, description)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp)) // Spacer for visual grouping

                    Text("Your Location (for initial setup)", style = MaterialTheme.typography.titleMedium)

                    // City Input
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // State or Zipcode Input
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State (e.g., FL) or Zipcode") }, // Combined input
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display error message
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Signup Button
                    Button(
                        onClick = {
                            errorMessage = null // Clear previous errors
                            if (email.isBlank() || password.isBlank() || verifyPassword.isBlank() || city.isBlank() || state.isBlank()) {
                                errorMessage = "Please fill all required fields."
                            } else if (password != verifyPassword) {
                                errorMessage = "Passwords do not match."
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Please enter a valid email address."
                            } else {
                                // Start geocoding and then trigger signup
                                isGeocodingLoading = true // Show loading indicator if desired
                                coroutineScope.launch {
                                    val locationQuery = if (state.length == 5 && state.all { it.isDigit() }) {
                                        state // If state looks like a zipcode, use it directly
                                    } else {
                                        "$city, $state" // Otherwise, combine city and state
                                    }

                                    val latLng = geocodingService.getLatLngFromAddress(locationQuery)
                                    isGeocodingLoading = false

                                    if (latLng != null) {
                                        // Pass geocoded coordinates along with signup info
                                        onSignupSuccess(SignupInfo(
                                            email.trim(),
                                            password,
                                            city.trim(),
                                            state.trim(),
                                            zipcode.trim().takeIf { it.isNotBlank() },
                                            latLng.lat, // Pass actual latitude
                                            latLng.lng // Pass actual longitude
                                        ))
                                    } else {
                                        errorMessage = "Could not find coordinates for the entered location. Please check and try again."
                                        // Allow signup to proceed without lat/lon if desired, or block it.
                                        // For now, we'll show an error and prevent signup without valid coordinates.
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        contentPadding = PaddingValues(12.dp),
                        enabled = !isGeocodingLoading // Disable button while geocoding
                    ) {
                        Text(if (isGeocodingLoading) "Locating..." else "Sign Up", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Navigate to Login Button
                    TextButton(onClick = onNavigateToLogin) {
                        Text("Already have an account? Log In", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/**
 * Preview function for the SignupScreen Composable.
 */
@Preview(showBackground = true)
@Composable
fun PreviewSignupScreen() {
    FoulWeatherTheme {
        SignupScreen(
            geocodingService = object : GeocodingService() { // Mock GeocodingService for preview
                override suspend fun getLatLngFromAddress(address: String): com.artificialinsightsllc.foulweather.data.models.LatLngLiteral? {
                    // Simulate a geocoding result for preview
                    return com.artificialinsightsllc.foulweather.data.models.LatLngLiteral(34.0522, -118.2437) // Example: Los Angeles
                }
            },
            onSignupSuccess = { signupInfo ->
                println("Signup successful for: ${signupInfo.email} with password: ${signupInfo.password} at ${signupInfo.city}, ${signupInfo.state} (Lat: ${signupInfo.latitude}, Lon: ${signupInfo.longitude})")
            },
            onNavigateToLogin = {
                println("Navigate back to Login")
            }
        )
    }
}
