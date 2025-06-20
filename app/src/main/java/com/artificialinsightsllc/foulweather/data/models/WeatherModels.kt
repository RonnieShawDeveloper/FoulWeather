// app/src/main/java/com/artificialinsightsllc/foulweather/data/models/WeatherModels.kt
package com.artificialinsightsllc.foulweather.data.models

import com.google.gson.annotations.SerializedName

/**
 * Data classes for parsing responses from the National Weather Service (NWS) API.
 * These mirror the JSON structures provided in the prompt.
 *
 * MODIFIED: Added data classes for weather alerts.
 * ADDED: Data classes for NWS product text (e.g., Area Forecast Discussion).
 * FIXED: Corrected the type of '@context' in ProductResponse back to a custom object (ProductContext)
 * to match the *actual JSON response* provided, which shows it as a JSON object, not an array of strings.
 * RE-ADDED: ProductContext data class.
 * MODIFIED: Added 'forecastOffice' to GridpointProperties.
 */

// --- General Location Data Model (from initial Lat/Long lookup) ---

data class RelativeLocation(
    val type: String,
    val geometry: Geometry,
    val properties: LocationProperties
)

data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

data class LocationProperties(
    val city: String,
    val state: String,
    val distance: Distance
)

data class Distance(
    val unitCode: String,
    val value: Double
)

// --- Gridpoint Data Model (to get forecast URLs) ---

data class GridpointResponse(
    val properties: GridpointProperties
)

data class GridpointProperties(
    val forecast: String,
    val forecastHourly: String,
    val forecastZone: String,
    val county: String,
    val fireWeatherZone: String,
    val timeZone: String,
    val radarStation: String, // This is the KXXX identifier, like KTBW
    val relativeLocation: RelativeLocation,
    val observationStations: String?, // ADDED: URL to nearby observation stations
    val forecastOffice: String? // ADDED: URL to the forecast office, e.g., "https://api.weather.gov/offices/MFL"
)

// --- Daily/Period Forecast Data Model ---

data class ForecastResponse(
    val properties: ForecastProperties
)

data class ForecastProperties(
    val units: String,
    val forecastGenerator: String,
    val generatedAt: String,
    val updateTime: String,
    val validTimes: String,
    val elevation: Elevation,
    val periods: List<Period>
)

data class Elevation(
    val unitCode: String,
    val value: Double
)

data class Period(
    val number: Int,
    val name: String,
    val startTime: String,
    val endTime: String,
    val isDaytime: Boolean,
    val temperature: Int,
    val temperatureUnit: String,
    val temperatureTrend: String?,
    val probabilityOfPrecipitation: ProbabilityOfPrecipitation,
    val windSpeed: String,
    val windDirection: String,
    val icon: String,
    val shortForecast: String,
    val detailedForecast: String
)

data class ProbabilityOfPrecipitation(
    val unitCode: String,
    val value: Int? // Can be null in some responses
)

// --- Hourly Forecast Data Model (similar structure to daily but with additional fields) ---

data class HourlyForecastResponse(
    val properties: HourlyForecastProperties
)

data class HourlyForecastProperties(
    val units: String,
    val forecastGenerator: String,
    val generatedAt: String,
    val updateTime: String,
    val validTimes: String,
    val elevation: Elevation,
    val periods: List<HourlyPeriod>
)

data class HourlyPeriod(
    val number: Int,
    val name: String,
    val startTime: String,
    val endTime: String,
    val isDaytime: Boolean,
    val temperature: Int,
    val temperatureUnit: String,
    val temperatureTrend: String?,
    val probabilityOfPrecipitation: ProbabilityOfPrecipitation,
    val dewpoint: Dewpoint?,
    val relativeHumidity: RelativeHumidity?,
    val windSpeed: String,
    val windDirection: String,
    val icon: String,
    val shortForecast: String,
    val detailedForecast: String?
)

data class Dewpoint(
    val unitCode: String,
    val value: Double
)

data class RelativeHumidity(
    val unitCode: String,
    val value: Int
)

// --- Geocoding API Data Models ---

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

data class GeocodingResult(
    val address_components: List<AddressComponent>,
    val formatted_address: String,
    val geometry: GeocodingGeometry,
    val place_id: String,
    val types: List<String>
)

data class AddressComponent(
    val long_name: String,
    val short_name: String,
    val types: List<String>
)

data class GeocodingGeometry(
    val bounds: Bounds?,
    val location: LatLngLiteral,
    val location_type: String,
    val viewport: Bounds
)

data class LatLngLiteral(
    val lat: Double,
    val lng: Double
)

data class Bounds(
    val northeast: LatLngLiteral,
    val southwest: LatLngLiteral
)

// --- NWS Station and Observation Data Models ---

// Response for /gridpoints/{wfo}/{x},{y}/stations
data class StationListResponse(
    val features: List<StationFeature>
)

data class StationFeature(
    val id: String, // URL to the station metadata
    val properties: StationProperties
)

data class StationProperties(
    @SerializedName("@id") val atId: String, // The actual URL of the station
    val stationIdentifier: String, // The station ID (e.g., "KSEA")
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Elevation,
    val timeZone: String,
    val forecastOffice: String,
    val county: String,
    val fireWeatherZone: String
    // Potentially more fields like "priorStation" if needed
)

// Response for /stations/{stationId}/observations/latest
data class LatestObservationResponse(
    val properties: LatestObservationProperties
)

data class LatestObservationProperties(
    @SerializedName("@id") val atId: String,
    val station: String, // URL to station metadata
    val timestamp: String,
    val rawMessage: String,
    val textDescription: String?, // Human-readable description
    val icon: String?,
    val temperature: ValueUnit?,
    val dewpoint: ValueUnit?,
    val windDirection: ValueUnit?, // Angle in degrees
    val windSpeed: ValueUnit?, // Speed
    val windGust: ValueUnit?,
    val barometricPressure: ValueUnit?, // Barometric Pressure
    val seaLevelPressure: ValueUnit?, // Sea Level Pressure (most common)
    val visibility: ValueUnit?,
    val relativeHumidity: ValueUnit?,
    val windChill: ValueUnit?,
    val heatIndex: ValueUnit?,
    val presentWeather: List<PresentWeather>?, // Array of weather phenomena
    val clouds: List<CloudLayer>?,
    val precipitationLastHour: ValueUnit?,
    val precipitationLast3Hours: ValueUnit?,
    val precipitationLast6Hours: ValueUnit?
)

data class ValueUnit(
    val unitCode: String,
    val value: Double? // Value can be null sometimes
)

data class PresentWeather(
    val intensity: String?,
    val rawString: String?,
    val inVicinity: Boolean?,
    val descript: String?,
    val coverage: String?,
    val weather: String?,
    val visibility: ValueUnit?
)

data class CloudLayer(
    val amount: String, // e.g., "FEW", "SCT", "BKN", "OVC"
    val height: ValueUnit? // Height of cloud base
)

// --- NWS Weather Alerts Data Models ---

data class AlertsResponse(
    val features: List<AlertFeature>,
    val title: String,
    val updated: String
)

data class AlertFeature(
    val id: String,
    val properties: AlertProperties,
    val geometry: AlertGeometry? // Can be null for some alerts
)

data class AlertProperties(
    val id: String,
    val areaDesc: String,
    val geocode: Geocode,
    val affectedZones: List<String>,
    val references: List<Reference>,
    val sent: String,
    val effective: String,
    val onset: String?,
    val expires: String,
    val ends: String?,
    val status: String,
    val messageType: String,
    val category: String,
    val severity: String,
    val certainty: String,
    val urgency: String,
    val event: String, // e.g., "Heat Advisory", "Flood Warning"
    val sender: String,
    val senderName: String,
    val headline: String?,
    val description: String,
    val instruction: String?,
    val response: String?,
    val parameters: Map<String, List<String>>
)

data class Geocode(
    val UGC: List<String>,
    val SAFEZONES: List<String>
)

data class Reference(
    val id: String,
    val identifier: String,
    val sender: String,
    val sent: String
)

data class AlertGeometry(
    val type: String,
    val coordinates: List<List<List<List<Double>>>> // Can be complex for polygons
)

// --- NWS Product Text Data Models ---

data class ProductResponse(
    // The top-level @context for the product text API is an object, not an array.
    // This is confirmed by the actual JSON response provided by the user.
    @SerializedName("@context") val context: ProductContext, // FIXED: Changed back to ProductContext
    @SerializedName("@id") val atId: String,
    val id: String,
    val wmoCollectiveId: String?, // Can be null
    val issuingOffice: String, // KTBW
    val issuanceTime: String, // ISO 8601 string
    val productCode: String, // AFD
    val productName: String, // Area Forecast Discussion
    val productText: String // The actual text content
)

// RE-ADDED: Data class to represent the '@context' object structure
data class ProductContext(
    @SerializedName("@version") val version: String,
    @SerializedName("@vocab") val vocab: String
)

// Data class to represent the product types we want to display in the menu
data class NWSProductType(
    val code: String,
    val name: String
)
