// app/src/main/java/com/artificialinsightsllc/foulweather/ui/screens/LoginScreen.kt
package com.artificialinsightsllc.foulweather.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope // ADDED: Import rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import kotlinx.coroutines.launch // ADDED: Import for launch

/**
 * Composable for the Login Screen.
 *
 * This screen provides UI for user login. It includes fields for email and password.
 *
 * @param onLoginSuccess Callback function invoked upon successful validation of input.
 * It provides the email and password entered by the user for authentication.
 * MODIFIED: This is now a suspend function to allow calling suspend functions directly within it.
 * @param onNavigateToSignup Callback to navigate to the signup screen if the user
 * doesn't have an account.
 */
@OptIn(ExperimentalMaterial3Api::class) // Opt-in for experimental Material 3 APIs like TopAppBar
@Composable
fun LoginScreen(
    onLoginSuccess: suspend (String, String) -> Unit, // MODIFIED: Added 'suspend' keyword here
    onNavigateToSignup: () -> Unit
) {
    // State variables for user input
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // State variable for password visibility
    var passwordVisible by remember { mutableStateOf(false) }

    // State for displaying error messages to the user
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope() // ADDED: Remember a CoroutineScope

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Alerts Login", color = Color.White) }, // Updated title
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color.Black),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to Foul Weather!",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Display error message
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Login Button
                    Button(
                        onClick = {
                            errorMessage = null // Clear previous errors
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter your email and password."
                            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Please enter a valid email address."
                            } else {
                                // MODIFIED: Call the suspend lambda within a coroutineScope.launch block
                                coroutineScope.launch {
                                    onLoginSuccess(email.trim(), password)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Text("Log In", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Navigate to Signup Button
                    TextButton(onClick = onNavigateToSignup) {
                        Text("Don't have an account? Sign Up", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

/**
 * Preview function for the LoginScreen Composable.
 */
@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    FoulWeatherTheme {
        LoginScreen(
            onLoginSuccess = { email, password ->
                println("Attempting login with: $email / $password")
            },
            onNavigateToSignup = {
                println("Navigate to Signup (Preview)")
            }
        )
    }
}
