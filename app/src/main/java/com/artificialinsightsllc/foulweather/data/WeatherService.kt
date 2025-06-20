// app/src/main/java/com/artificialinsightsllc/foulweather/data/WeatherService.kt
package com.artificialinsightsllc.foulweather.data

import android.util.Log // Using standard Android Log
import com.artificialinsightsllc.foulweather.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson

/**
 * A service class for interacting with the National Weather Service API.
 *
 * MODIFIED: Added functions to fetch nearby observation stations and their latest observations.
 * ADDED: Function to fetch active alerts for a given forecast zone.
 * ADDED: Function to fetch specific NWS product texts (e.g., AFD).
 */
open class WeatherService { // Class is 'open' to allow extension/mocking

    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val TAG = "WeatherService"

    // Base URL for NWS API points
    private val NWS_POINTS_BASE_URL = "https://api.weather.gov/points/"
    private val NWS_STATIONS_BASE_URL = "https://api.weather.gov/stations/" // Base for station observations
    private val NWS_ALERTS_BASE_URL = "https://api.weather.gov/alerts/active/zone/" // Base for alerts by zone
    private val NWS_PRODUCTS_BASE_URL = "https://api.weather.gov/products/types/" // Base for product texts

    /**
     * Fetches the NWS gridpoint data for a given latitude and longitude.
     * This is the entry point to get the specific forecast URLs and observation station URL for a location.
     *
     * @param latitude The latitude of the desired location.
     * @param longitude The longitude of the desired location.
     * @return GridpointProperties object containing forecast URLs and relative location, or null if an error occurs.
     */
    open suspend fun getGridpointData(latitude: Double, longitude: Double): GridpointProperties? {
        return withContext(Dispatchers.IO) { // Perform network operation on IO dispatcher
            try {
                // Construct the NWS points URL directly using the provided lat/lon
                // NWS uses latitude,longitude format for points
                val url = "$NWS_POINTS_BASE_URL$latitude,$longitude"

                Log.d(TAG, "Fetching gridpoint data from: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)") // NWS requires a User-Agent
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching gridpoint data: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for gridpoint data.")
                        return@withContext null
                    }
                    gson.fromJson(responseBody, GridpointResponse::class.java).properties
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching gridpoint data: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches the daily weather forecast from the provided forecast URL.
     *
     * @param forecastUrl The URL for the daily forecast, obtained from GridpointProperties.
     * @return A [ForecastResponse] object on success, or null if an error occurs.
     */
    open suspend fun getDailyForecast(forecastUrl: String): ForecastResponse? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching daily forecast from: $forecastUrl")
                val request = Request.Builder()
                    .url(forecastUrl)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching daily forecast: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for daily forecast.")
                        return@withContext null
                    }
                    gson.fromJson(responseBody, ForecastResponse::class.java)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching daily forecast: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches the hourly weather forecast from the provided hourly forecast URL.
     *
     * @param hourlyForecastUrl The URL for the hourly forecast, obtained from GridpointProperties.
     * @return A list of HourlyPeriod objects representing hourly forecasts, or null if an error occurs.
     */
    open suspend fun getHourlyForecast(hourlyForecastUrl: String): List<HourlyPeriod>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching hourly forecast from: $hourlyForecastUrl")
                val request = Request.Builder()
                    .url(hourlyForecastUrl)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching hourly forecast: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for hourly forecast.")
                        return@withContext null
                    }
                    gson.fromJson(responseBody, HourlyForecastResponse::class.java).properties.periods
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching hourly forecast: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches a list of observation stations near a given gridpoint.
     * This URL comes from `GridpointProperties.observationStations`.
     *
     * @param stationsUrl The URL to the stations list (e.g., from `GridpointProperties.observationStations`).
     * @return A list of [StationFeature] objects on success, or null.
     */
    open suspend fun getNearbyObservationStations(stationsUrl: String): List<StationFeature>? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching nearby observation stations from: $stationsUrl")
                val request = Request.Builder()
                    .url(stationsUrl)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching stations list: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for stations list.")
                        return@withContext null
                    }
                    gson.fromJson(responseBody, StationListResponse::class.java).features
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching nearby stations: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches the latest weather observation for a specific station ID.
     *
     * @param stationId The identifier of the weather station (e.g., "KSEA", "KTBW").
     * @return A [LatestObservationProperties] object on success, or null.
     */
    open suspend fun getLatestObservation(stationId: String): LatestObservationProperties? { // ADDED this function
        return withContext(Dispatchers.IO) {
            try {
                val url = "$NWS_STATIONS_BASE_URL$stationId/observations/latest"
                Log.d(TAG, "Fetching latest observation for $stationId from: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching latest observation for $stationId: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for latest observation.")
                        return@withContext null
                    }
                    gson.fromJson(responseBody, LatestObservationResponse::class.java).properties
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching latest observation for $stationId: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches active weather alerts for a given NWS forecast zone.
     *
     * @param zoneId The NWS forecast zone ID (e.g., "FLZ051").
     * @return A list of [AlertFeature] objects (representing individual alerts) on success, or null.
     * If no alerts are found, an empty list is returned, not null.
     */
    open suspend fun getAlertsForZone(zoneId: String): List<AlertFeature>? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$NWS_ALERTS_BASE_URL$zoneId"
                Log.d(TAG, "Fetching alerts for zone $zoneId from: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching alerts for zone $zoneId: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for alerts.")
                        return@withContext emptyList() // Return empty list if no content
                    }
                    val alertsResponse = gson.fromJson(responseBody, AlertsResponse::class.java)
                    alertsResponse.features // Return the list of alerts
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching alerts for zone $zoneId: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches the latest product text for a given product type and NWS location ID.
     *
     * @param typeId The product type code (e.g., "AFD", "ZFP").
     * @param locationId The NWS Weather Forecast Office (WFO) identifier (e.g., "TBW").
     * @return A [ProductResponse] object containing the product text and metadata on success, or null if an error occurs.
     */
    open suspend fun getProductText(typeId: String, locationId: String): ProductResponse? {
        return withContext(Dispatchers.IO) {
            try {
                // Ensure the locationId is in the correct format (e.g., TBW, not KTBW)
                val formattedLocationId = locationId.removePrefix("K")
                val url = "$NWS_PRODUCTS_BASE_URL$typeId/locations/$formattedLocationId/latest"
                Log.d(TAG, "Fetching product text for type $typeId at location $formattedLocationId from: $url")

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "FoulWeatherApp (your_email@example.com)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Error fetching product text for $typeId/$formattedLocationId: ${response.code} ${response.message}")
                        return@withContext null
                    }
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response body for product text for $typeId/$formattedLocationId.")
                        return@withContext null // Return null if no content
                    }
                    gson.fromJson(responseBody, ProductResponse::class.java)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching product text for $typeId/$locationId: ${e.message}", e)
                null
            }
        }
    }
}
