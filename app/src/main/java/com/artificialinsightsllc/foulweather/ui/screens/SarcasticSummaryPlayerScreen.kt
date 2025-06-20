// app/src/main/java/com/artificialinsightsllc/foulweather/ui/screens/SarcasticSummaryPlayerScreen.kt
package com.artificialinsightsllc.foulweather.ui.screens

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.artificialinsightsllc.foulweather.ui.theme.FoulWeatherTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.storage.StorageException
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.artificialinsightsllc.foulweather.R


/**
 * Composable for playing pre-generated sarcastic weather summaries from Firebase Storage.
 * This screen is the landing page for FCM notifications and the FAB action from the dashboard.
 *
 * @param wfoIdentifier The Weather Forecast Office (WFO) identifier (e.g., "TBW")
 * for which to fetch and play the audio summary.
 * @param onNavigateToDashboard Callback to navigate back to the dashboard or appropriate main screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SarcasticSummaryPlayerScreen(
    wfoIdentifier: String?,
    onNavigateToDashboard: () -> Unit
    // Removed firebaseService parameter
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val storageRef = Firebase.storage.reference

    var audioPlaybackState by remember { mutableStateOf<AudioPlaybackState>(AudioPlaybackState.Loading) }
    var mediaPlayer: MediaPlayer? by remember { mutableStateOf(null) }
    var tempAudioFile: File? by remember { mutableStateOf(null) } // Store reference to temp file

    // State to control visibility of the "Audio Generating" dialog
    var showAudioGeneratingDialog by remember { mutableStateOf(false) }

    // DisposableEffect for MediaPlayer lifecycle management
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            tempAudioFile?.delete() // Clean up the temporary file on dispose
            Log.d("SummaryPlayer", "MediaPlayer released and temp file deleted on dispose.")
        }
    }

    LaunchedEffect(wfoIdentifier) {
        if (wfoIdentifier == null) {
            audioPlaybackState = AudioPlaybackState.Error("No WFO identifier provided.")
            return@LaunchedEffect
        }

        audioPlaybackState = AudioPlaybackState.Loading
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val audioFileName = "$wfoIdentifier.wav"
                val path = "sarcastic_summaries/$audioFileName"
                val fileRef = storageRef.child(path)

                tempAudioFile = File(context.cacheDir, audioFileName)

                Log.d("SummaryPlayer", "Attempting to download audio from: $path")
                val bytesDownloaded = fileRef.getFile(tempAudioFile!!).await()
                Log.d("SummaryPlayer", "Downloaded ${bytesDownloaded.bytesTransferred} bytes to ${tempAudioFile!!.absolutePath}")

                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempAudioFile!!.absolutePath)
                    prepareAsync()
                    setOnPreparedListener { mp ->
                        mp.start()
                        audioPlaybackState = AudioPlaybackState.Playing
                        Log.d("SummaryPlayer", "Audio playback started.")
                    }
                    setOnCompletionListener { mp ->
                        mp.release()
                        mediaPlayer = null
                        audioPlaybackState = AudioPlaybackState.Idle
                        Log.d("SummaryPlayer", "Audio playback completed and MediaPlayer released.")
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.e("SummaryPlayer", "MediaPlayer error: what=$what, extra=$extra")
                        mp.release()
                        mediaPlayer = null
                        audioPlaybackState = AudioPlaybackState.Error("Error playing audio. Code: $what, Extra: $extra")
                        false
                    }
                }
            } catch (e: StorageException) {
                // Handle StorageException specifically for object not found
                if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    Log.d("SummaryPlayer", "Audio file not found for $wfoIdentifier. Showing generating dialog.")
                    audioPlaybackState = AudioPlaybackState.Generating // Set state to generating
                    showAudioGeneratingDialog = true // Show the specific dialog
                } else {
                    Log.e("SummaryPlayer", "Storage error downloading or playing audio: ${e.message}", e)
                    audioPlaybackState = AudioPlaybackState.Error("Failed to load audio: ${e.localizedMessage ?: "Unknown storage error"}")
                }
            } catch (e: Exception) {
                Log.e("SummaryPlayer", "General error downloading or playing audio: ${e.message}", e)
                audioPlaybackState = AudioPlaybackState.Error("Failed to load audio: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    Scaffold(
        containerColor = Color.Black, // Set background color of the scaffold to black
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Foul Weather Forecast for ${wfoIdentifier ?: "Your Area"}",
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.LightGray),
                navigationIcon = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        // Use a Box to layer the background image and the content column
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize() // Make the Box fill available space
                .padding(paddingValues) // Apply padding from scaffold
                .background(Color.Black) // Ensure the entire background is black for blending
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.audio_background), // Set your background image
                contentDescription = null, // Decorative image, no content description needed
                modifier = Modifier.fillMaxWidth(), // Fill the entire width of the Box
                contentScale = ContentScale.FillWidth, // Scale to fill width, maintaining aspect ratio
                alignment = Alignment.Center // Center the image horizontally
            )

            // Original content column, placed on top of the image
            Column(
                modifier = Modifier
                    .fillMaxSize() // Make the Column fill the entire Box
                    .padding(16.dp) // Add padding to avoid content being too close to edges
                    .background(Color.Black.copy(alpha = 0.7f)), // ADDED: Semi-transparent black background
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (audioPlaybackState) {
                    AudioPlaybackState.Loading -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading and playing audio...", color = Color.White)
                    }
                    AudioPlaybackState.Playing -> {
                        Text("Playing summary...", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        // Basic playback controls (optional, given short summaries)
                        Row(horizontalArrangement = Arrangement.Center) {
                            Button(onClick = { mediaPlayer?.pause(); audioPlaybackState = AudioPlaybackState.Paused }) {
                                Text("Pause")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { mediaPlayer?.stop(); audioPlaybackState = AudioPlaybackState.Idle }) {
                                Text("Stop")
                            }
                        }
                    }
                    AudioPlaybackState.Paused -> {
                        Text("Paused.", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            Button(onClick = { mediaPlayer?.start(); audioPlaybackState = AudioPlaybackState.Playing }) {
                                Text("Resume")
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(onClick = { mediaPlayer?.stop(); audioPlaybackState = AudioPlaybackState.Idle }) {
                                Text("Stop")
                            }
                        }
                    }
                    AudioPlaybackState.Idle -> {
                        Text("Summary ready. Press play to listen.", color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            mediaPlayer?.apply {
                                if (!isPlaying) {
                                    try {
                                        // Reset and re-prepare if stopped or new instance
                                        reset()
                                        tempAudioFile?.absolutePath?.let {
                                            setDataSource(it)
                                            prepareAsync()
                                            audioPlaybackState = AudioPlaybackState.Loading // Set to loading while preparing
                                        } ?: run {
                                            Log.e("SummaryPlayer", "Temp audio file is null, cannot replay.")
                                            audioPlaybackState = AudioPlaybackState.Error("Audio file not found for replay.")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SummaryPlayer", "Error preparing for replay: ${e.message}", e)
                                        audioPlaybackState = AudioPlaybackState.Error("Error replaying: ${e.localizedMessage}")
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                            Text("Play")
                        }
                    }
                    AudioPlaybackState.Generating -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Audio summary is being generated...", color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You will receive a notification when it's available.", color = Color.White, textAlign = TextAlign.Center)
                    }
                    is AudioPlaybackState.Error -> {
                        Text("Error: ${audioPlaybackState.message}", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateToDashboard) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }

    if (showAudioGeneratingDialog) {
        AlertDialog(
            onDismissRequest = {
                showAudioGeneratingDialog = false
                onNavigateToDashboard() // Navigate back to dashboard on dismiss
            },
            title = {
                Text(
                    text = "Audio Summary Generating",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "The sarcastic audio summary for your area is currently being generated. This may take up to 30 minutes.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "You will receive a notification on this device when it's ready for listening.",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAudioGeneratingDialog = false
                    onNavigateToDashboard() // Navigate back to dashboard when OK is pressed
                }) {
                    Text("OK", color = Color.White)
                }
            },
            containerColor = Color.Black // Set background color for the dialog
        )
    }
}

// Define possible states for audio playback
sealed class AudioPlaybackState(val message: String? = null) {
    object Loading : AudioPlaybackState("Loading audio...")
    object Playing : AudioPlaybackState("Playing audio...")
    object Paused : AudioPlaybackState("Audio paused.")
    object Idle : AudioPlaybackState("Ready to play.")
    object Generating : AudioPlaybackState("Generating audio...")
    class Error(message: String) : AudioPlaybackState(message)
}

/**
 * Preview function for the SarcasticSummaryPlayerScreen Composable.
 */
@Preview(showBackground = true)
@Composable
fun PreviewSarcasticSummaryPlayerScreen() {
    FoulWeatherTheme {
        // No FirebaseService needed for preview due to simplified callback
        SarcasticSummaryPlayerScreen(
            wfoIdentifier = "TBW",
            onNavigateToDashboard = { Log.d("Preview", "Navigating to dashboard (simplified)") }
        )
    }
}
