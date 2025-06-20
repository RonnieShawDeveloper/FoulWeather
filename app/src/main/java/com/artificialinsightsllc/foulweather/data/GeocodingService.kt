// app/src/main/java/com/artificialinsightsllc/foulweather/data/GeocodingService.kt
package com.artificialinsightsllc.foulweather.data

import android.util.Log
import com.artificialinsightsllc.foulweather.data.models.GeocodingResponse
import com.artificialinsightsllc.foulweather.data.models.LatLngLiteral
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A service class responsible for geocoding addresses (City, State, Zipcode)
 * into latitude and longitude coordinates using the Google Geocoding API.
 */
open class GeocodingService {

    private val httpClient = OkHttpClient()
    private val gson = Gson()

    // IMPORTANT: Replace with your actual Google Geocoding API Key.
    // In a real application, this should be stored securely, not hardcoded.
    // E.g., in local.properties and accessed via BuildConfig or a secrets management solution.
    private val GOOGLE_GEOCODING_API_KEY = "AIzaSyC522QbW8emAet_nK6lIdP50CrW6Xxm82c" // <<< REPLACE THIS

    private val GEOCODING_BASE_URL = "https://maps.googleapis.com/maps/api/geocode/json"
    private val TAG = "GeocodingService"

    /**
     * Converts a human-readable address query (e.g., "New York, NY" or "10001")
     * into latitude and longitude coordinates using the Google Geocoding API.
     *
     * @param address The address string to geocode.
     * @return A [LatLngLiteral] object containing latitude and longitude on success, or null if geocoding fails.
     */
    open suspend fun getLatLngFromAddress(address: String): LatLngLiteral? {
        // Encode the address to make it URL-safe
        val encodedAddress = URLEncoder.encode(address, "UTF-8")
        val url = "$GEOCODING_BASE_URL?address=$encodedAddress&key=$GOOGLE_GEOCODING_API_KEY"

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Geocoding API request failed: ${response.code} ${response.message}")
                        return@withContext null
                    }

                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body from Geocoding API.")
                        return@withContext null
                    }

                    val geocodingResponse = gson.fromJson(responseBody, GeocodingResponse::class.java)

                    if (geocodingResponse.status == "OK" && geocodingResponse.results.isNotEmpty()) {
                        // Return the location of the first (most relevant) result
                        Log.d(TAG, "Geocoding successful for '$address': ${geocodingResponse.results[0].geometry.location.lat}, ${geocodingResponse.results[0].geometry.location.lng}")
                        geocodingResponse.results[0].geometry.location
                    } else {
                        Log.e(TAG, "Geocoding API returned status: ${geocodingResponse.status} for address: '$address'")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during geocoding for '$address': ${e.message}", e)
                null
            }
        }
    }
}
