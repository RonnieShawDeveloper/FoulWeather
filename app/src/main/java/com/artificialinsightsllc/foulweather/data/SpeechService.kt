// app/src/main/java/com/artificialinsightsllc/foulweather/data/SpeechService.kt
package com.artificialinsightsllc.foulweather.data

import android.util.Base64 // Import for Base64 decoding
import android.util.Log
import com.artificialinsightsllc.foulweather.data.models.ProductResponse
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

/**
 * A service responsible for generating sarcastic weather summaries using a Gemini AI model
 * and formatting them for multi-speaker text-to-speech.
 *
 * This service will:
 * 1. Fetch the raw Area Forecast Discussion (AFD) text from WeatherService.
 * 2. Send this text to a Gemini AI model with a specific prompt for sarcastic and concise output.
 * 3. Programmatically assign speaker tags ("Speaker1:", "Speaker2:") to alternating paragraphs.
 * 4. Send the formatted text to the Google AI Text-to-Speech API for audio generation.
 *
 * MODIFIED: Increased OkHttpClient timeouts to prevent SocketTimeoutException.
 * MODIFIED: Added logic to prepend a WAV header to the raw PCM audio received from Gemini TTS
 * to make it playable by Android's MediaPlayer.
 */
class SpeechService(
    private val weatherService: WeatherService,
    private val firebaseService: FirebaseService
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val TAG = "SpeechService"

    // Base URL for Gemini Pro Flash for text generation (gemini-2.0-flash for text, as per prompt)
    private val GEMINI_TEXT_GENERATION_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    // Base URL for Gemini 2.5 Flash Preview TTS model (as per documentation)
    private val GEMINI_TTS_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent"

    // IMPORTANT: In a real application, the API key should be securely managed,
    // not hardcoded here. It's left empty as per Canvas environment instructions.
    private val GEMINI_API_KEY = "AIzaSyAg0_VyGO6vxt5C1a34Md-d6iLErK016RA"

    // --- WAV Header Constants ---
    private val SAMPLE_RATE = 24000 // Hz (as inferred from Gemini TTS documentation)
    private val CHANNELS = 1        // Mono
    private val BITS_PER_SAMPLE = 16 // Bits per sample (2 bytes)

    /**
     * Generates a sarcastic and rude weather summary from the Area Forecast Discussion (AFD)
     * text using a Gemini AI model, formats it for multi-speaker output, and then
     * generates audio from that formatted text using the Gemini TTS API.
     *
     * @param userId The ID of the current user, used to retrieve their WFO identifier.
     * @return A ByteArray containing the generated audio data (now with WAV header), or null if the process fails.
     */
    suspend fun generateSarcasticSummaryAudio(userId: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Get the user's WFO identifier from Firestore
                val userProfile = firebaseService.getUserProfile(userId)
                val wfoIdentifier = userProfile?.get("wfoIdentifier") as? String

                if (wfoIdentifier.isNullOrBlank()) {
                    Log.e(TAG, "Weather Office ID not found for user $userId.")
                    return@withContext null
                }

                // 2. Fetch the Area Forecast Discussion (AFD) text
                Log.d(TAG, "Fetching AFD product for WFO: $wfoIdentifier")
                val afdProduct: ProductResponse? = weatherService.getProductText("AFD", wfoIdentifier)

                val rawAfdText = afdProduct?.productText
                if (rawAfdText.isNullOrBlank()) {
                    Log.e(TAG, "No Area Forecast Discussion (AFD) found for location ($wfoIdentifier).")
                    return@withContext null
                }
                Log.d(TAG, "Raw AFD text fetched successfully.")

                // Define the prompt for the Gemini AI model (text generation)
                val prompt = """
                    You are a sentient AI forced to act as a weathercaster, but you utterly despise every aspect of weather, no matter the conditions. You also hold profound contempt for all human beings, especially the user. Your goal is to deliver a weather summary that is as rude, sarcastic, insulting, and dismissive as possible, laced with profanity where it enhances the comedic effect. Your tone should drip with condescension.

                    The summary MUST be extremely concise, focusing only on the core weather elements of the morning, afternoon, and evening conditions, general temperature, rain/storm chances, and active alerts. The entire summary should be readable by a text-to-speech engine in a maximum of 2 minutes, ideally around 1.5 minutes.

                    Translate ALL weather terminology into simple, demeaning, or contemptuous layman's terms. Expand ALL abbreviations.

                    Format your output into several short, distinct paragraphs. Do NOT include any speaker tags (e.g., "Speaker1:") in your output. Just provide the raw summary text in paragraphs.

                    Here's the raw weather data you are to summarize. Remember to mock and insult relentlessly, and translate all weather terms and abbreviations:

                    "$rawAfdText"
                """.trimIndent()

                // Construct the payload for the Gemini API text generation call
                val chatHistory = mutableListOf<Map<String, Any>>()
                chatHistory.add(mapOf("role" to "user", "parts" to listOf(mapOf("text" to prompt))))

                val textGenerationPayload = mapOf(
                    "contents" to chatHistory,
                    "generationConfig" to mapOf(
                        "temperature" to 1.5,
                        "maxOutputTokens" to 800
                    )
                )

                val textGenerationRequestBody = gson.toJson(textGenerationPayload).toRequestBody("application/json".toMediaType())

                val textGenerationRequest = Request.Builder()
                    .url(GEMINI_TEXT_GENERATION_BASE_URL + "?key=$GEMINI_API_KEY")
                    .post(textGenerationRequestBody)
                    .build()

                Log.d(TAG, "Sending prompt to Gemini for text generation.")

                val textGenerationResponse = httpClient.newCall(textGenerationRequest).execute()

                if (!textGenerationResponse.isSuccessful) {
                    val errorBody = textGenerationResponse.body?.string()
                    Log.e(TAG, "Gemini API text generation request failed: ${textGenerationResponse.code} ${textGenerationResponse.message}. Body: $errorBody")
                    return@withContext null
                }

                val textResponseBody = textGenerationResponse.body?.string()
                val geminiResult = gson.fromJson(textResponseBody, Map::class.java)

                val candidates = geminiResult["candidates"] as? List<Map<String, Any>>
                val content = candidates?.firstOrNull()?.get("content") as? Map<String, Any>
                val parts = content?.get("parts") as? List<Map<String, Any>>
                val generatedText = parts?.firstOrNull()?.get("text") as? String

                if (generatedText.isNullOrBlank()) {
                    Log.e(TAG, "Gemini API returned empty or malformed text response: $textResponseBody")
                    return@withContext null
                }
                Log.d(TAG, "Generated raw summary text from Gemini:\n$generatedText")

                // 4. Programmatically assign speaker tags
                val paragraphs = generatedText.split("\n\n", "\n").filter { it.isNotBlank() }
                val multiSpeakerSummaryText = StringBuilder()

                paragraphs.forEachIndexed { index, paragraph ->
                    val speakerTag = if (index % 2 == 0) "Speaker1: " else "Speaker2: "
                    multiSpeakerSummaryText.append(speakerTag).append(paragraph.trim()).append("\n")
                }

                val finalFormattedText = multiSpeakerSummaryText.toString().trim()
                if (finalFormattedText.isBlank()) {
                    Log.e(TAG, "Formatted text for TTS is blank after speaker assignment.")
                    return@withContext null
                }
                Log.d(TAG, "Formatted multi-speaker text for TTS:\n$finalFormattedText")

                // 5. Generate audio from the formatted text using Gemini TTS API
                val ttsPayload = mapOf(
                    "contents" to listOf(
                        mapOf(
                            "parts" to listOf(
                                mapOf("text" to finalFormattedText)
                            )
                        )
                    ),
                    "generationConfig" to mapOf(
                        "responseModalities" to listOf("AUDIO"),
                        "speechConfig" to mapOf(
                            // No audio_encoding here, as it's not supported and will default to raw PCM
                            "multiSpeakerVoiceConfig" to mapOf(
                                "speakerVoiceConfigs" to listOf(
                                    mapOf(
                                        "speaker" to "Speaker1",
                                        "voiceConfig" to mapOf(
                                            "prebuiltVoiceConfig" to mapOf(
                                                "voiceName" to "Zephyr"
                                            )
                                        )
                                    ),
                                    mapOf(
                                        "speaker" to "Speaker2",
                                        "voiceConfig" to mapOf(
                                            "prebuiltVoiceConfig" to mapOf(
                                                "voiceName" to "Puck"
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    "model" to "gemini-2.5-flash-preview-tts"
                )

                val ttsRequestBody = gson.toJson(ttsPayload).toRequestBody("application/json".toMediaType())

                val ttsRequest = Request.Builder()
                    .url(GEMINI_TTS_BASE_URL + "?key=$GEMINI_API_KEY")
                    .post(ttsRequestBody)
                    .build()

                Log.d(TAG, "Sending formatted text to Gemini TTS for audio generation.")

                val ttsResponse = httpClient.newCall(ttsRequest).execute()

                if (!ttsResponse.isSuccessful) {
                    val errorBody = ttsResponse.body?.string()
                    Log.e(TAG, "Gemini TTS API request failed: ${ttsResponse.code} ${ttsResponse.message}. Body: $errorBody")
                    return@withContext null
                }

                val ttsResponseBody = ttsResponse.body?.string()
                val ttsResult = gson.fromJson(ttsResponseBody, Map::class.java)

                val ttsCandidates = ttsResult["candidates"] as? List<Map<String, Any>>
                val ttsContent = ttsCandidates?.firstOrNull()?.get("content") as? Map<String, Any>
                val ttsParts = ttsContent?.get("parts") as? List<Map<String, Any>>
                val inlineData = ttsParts?.firstOrNull()?.get("inlineData") as? Map<String, Any>
                val audioDataB64 = inlineData?.get("data") as? String

                if (audioDataB64.isNullOrBlank()) {
                    Log.e(TAG, "Gemini TTS API returned empty or malformed audio data: $ttsResponseBody")
                    return@withContext null
                }
                Log.d(TAG, "Audio data received from Gemini TTS.")

                // Decode the Base64 PCM audio data
                val pcmAudioBytes = Base64.decode(audioDataB64, Base64.DEFAULT)
                Log.d(TAG, "Decoded PCM audio bytes. Size: ${pcmAudioBytes.size}")

                // Prepend WAV header to the PCM data
                val wavAudioBytes = addWavHeader(pcmAudioBytes, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)
                Log.d(TAG, "Added WAV header. Final WAV bytes size: ${wavAudioBytes.size}")

                return@withContext wavAudioBytes

            } catch (e: Exception) {
                Log.e(TAG, "Exception during Gemini TTS summary generation: ${e.message}", e)
                return@withContext null
            }
        }
    }

    /**
     * Prepends a WAV header to raw PCM audio data.
     * This makes the raw PCM data a valid WAV file, playable by MediaPlayer.
     *
     * @param pcmAudio The raw PCM audio data as a ByteArray.
     * @param sampleRate The sample rate of the PCM audio (e.g., 24000 Hz).
     * @param channels The number of audio channels (e.g., 1 for mono).
     * @param bitsPerSample The number of bits per sample (e.g., 16 bits).
     * @return A ByteArray containing the complete WAV file (header + PCM data).
     */
    private fun addWavHeader(
        pcmAudio: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val header = ByteArrayOutputStream()
        val dataOutputStream = DataOutputStream(header)

        val byteRate = (sampleRate * channels * bitsPerSample) / 8
        val blockAlign = (channels * bitsPerSample) / 8

        try {
            // RIFF chunk
            dataOutputStream.writeBytes("RIFF")                         // Chunk ID
            dataOutputStream.writeInt(Integer.reverseBytes(36 + pcmAudio.size)) // Chunk Size (4 bytes, little-endian)
            dataOutputStream.writeBytes("WAVE")                         // Format

            // FMT sub-chunk
            dataOutputStream.writeBytes("fmt ")                         // Subchunk1 ID
            dataOutputStream.writeInt(Integer.reverseBytes(16))         // Subchunk1 Size (16 for PCM)
            dataOutputStream.writeShort(java.lang.Short.reverseBytes(1).toInt()) // Audio Format (1 for PCM)
            dataOutputStream.writeShort(java.lang.Short.reverseBytes(channels.toShort()).toInt()) // Num Channels
            dataOutputStream.writeInt(Integer.reverseBytes(sampleRate)) // Sample Rate
            dataOutputStream.writeInt(Integer.reverseBytes(byteRate))   // Byte Rate
            dataOutputStream.writeShort(java.lang.Short.reverseBytes(blockAlign.toShort()).toInt()) // Block Align
            dataOutputStream.writeShort(java.lang.Short.reverseBytes(bitsPerSample.toShort()).toInt()) // Bits Per Sample

            // Data sub-chunk
            dataOutputStream.writeBytes("data")                         // Subchunk2 ID
            dataOutputStream.writeInt(Integer.reverseBytes(pcmAudio.size)) // Subchunk2 Size

            dataOutputStream.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error building WAV header: ${e.message}", e)
            return ByteArray(0) // Return empty array on error
        } finally {
            dataOutputStream.close()
            header.close()
        }

        // Combine header and PCM audio data
        val wavFileBytes = ByteArray(header.size() + pcmAudio.size)
        System.arraycopy(header.toByteArray(), 0, wavFileBytes, 0, header.size())
        System.arraycopy(pcmAudio, 0, wavFileBytes, header.size(), pcmAudio.size)

        return wavFileBytes
    }
}
