// app/src/main/java/com/artificialinsightsllc/foulweather/data/FirebaseService.kt
package com.artificialinsightsllc.foulweather.data

import android.util.Log // Using standard Android Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.artificialinsightsllc.foulweather.ui.screens.SignupInfo
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging

/**
 * A service class to handle Firebase Authentication and Firestore operations,
 * now also managing Firebase Cloud Messaging (FCM) tokens and topic subscriptions,
 * and user forecast zone information.
 *
 * This class provides methods for:
 * - User registration with email and password.
 * - Storing initial user profile data (like email and location) in Firestore.
 * - Retrieving the current authenticated user.
 * - Logging out a user.
 * - Retrieving the FCM device token.
 * - Saving the FCM token to the user's Firestore document.
 * - Subscribing to an FCM topic.
 * - UPDATED: Saving the user's NWS forecast zone to their profile.
 * - ADDED: Saving the user's NWS Weather Forecast Office (WFO) identifier to their profile.
 * - ADDED: Methods to subscribe and unsubscribe from WFO-specific FCM topics.
 * - ADDED: Method to save the radar station identifier to the user's profile.
 *
 * It uses Kotlin Coroutines for asynchronous operations with Firebase APIs,
 * leveraging `tasks.await()` for a more synchronous-like coding style.
 */
open class FirebaseService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance()

    // Define a TAG for logging messages
    private val TAG = "FirebaseService"

    // Companion object for Firestore collection names and FCM topic
    companion object {
        private const val USERS_COLLECTION = "users"
        private const val FOULWEATHER_TOPIC = "foulweather" // General topic, less relevant now
        private const val WFO_TOPIC_PREFIX = "wfo_" // Prefix for WFO-specific topics
    }

    /**
     * Registers a new user with email and password using Firebase Authentication.
     * After successful registration, it also saves the user's initial signup information
     * (email, city, state, zipcode, lat, lon) to a Firestore document.
     *
     * @param email The user's email address.
     * @param password The user's chosen password.
     * @param signupInfo The SignupInfo object containing initial user details like city, state, zipcode, lat, lon.
     * @return The newly registered FirebaseUser on success, or null if registration fails.
     * @throws Exception if an error occurs during authentication or Firestore operation.
     */
    suspend fun registerUserAndSaveProfile(
        email: String,
        password: String,
        signupInfo: SignupInfo
    ): FirebaseUser? {
        return try {
            // 1. Create user with email and password
            val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = userCredential.user

            firebaseUser?.let { user ->
                // 2. Save user profile data to Firestore
                val userProfile = hashMapOf(
                    "email" to signupInfo.email,
                    "city" to signupInfo.city,
                    "state" to signupInfo.state,
                    "zipcode" to signupInfo.zipcode,
                    "latitude" to signupInfo.latitude, // Store geocoded latitude
                    "longitude" to signupInfo.longitude, // Store geocoded longitude
                    "createdAt" to System.currentTimeMillis() // Timestamp
                )

                firestore.collection(USERS_COLLECTION)
                    .document(user.uid) // Use user's UID as document ID for easy lookup
                    .set(userProfile) // Use set() to create or overwrite the document
                    .await()

                Log.d(TAG, "User registered and profile saved: ${user.uid}")
                user
            } ?: run {
                Log.e(TAG, "User credential user is null after registration.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user or saving profile: ${e.message}", e)
            throw e // Re-throw the exception for UI to handle
        }
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @return The authenticated FirebaseUser on success, or null if authentication fails.
     * @throws Exception if an error occurs during authentication.
     */
    suspend fun loginUser(email: String, password: String): FirebaseUser? {
        return try {
            val userCredential = auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "User logged in: ${userCredential.user?.uid}")
            userCredential.user
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in user: ${e.message}", e)
            throw e // Re-throw the exception for UI to handle
        }
    }

    /**
     * Retrieves the currently authenticated FirebaseUser.
     *
     * @return The current FirebaseUser, or null if no user is authenticated.
     */
    open fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Logs out the currently authenticated user.
     */
    fun logoutUser() {
        auth.signOut()
        Log.d(TAG, "User logged out.")
    }

    /**
     * Updates the location (city, state, zipcode, lat, lon) for the current user in Firestore.
     * This method will create the document if it does not exist, or update/merge data if it does.
     *
     * @param userId The UID of the user whose location is to be updated.
     * @param city The new city.
     * @param state The new state.
     * @param zipcode The new zipcode (nullable).
     * @param latitude The new latitude (nullable, to be set after geocoding).
     * @param longitude The new longitude (nullable, to be set after geocoding).
     * @throws Exception if an error occurs during Firestore operation.
     */
    suspend fun updateCurrentUserLocation(
        userId: String,
        city: String,
        state: String,
        zipcode: String?,
        latitude: Double?,
        longitude: Double?
    ) {
        try {
            val updates = hashMapOf<String, Any?>(
                "city" to city,
                "state" to state,
                "zipcode" to zipcode,
                "latitude" to latitude,
                "longitude" to longitude
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge()) // Use set with merge option
                .await()
            Log.d(TAG, "User location updated/created for $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user location: ${e.message}", e)
            throw e
        }
    }

    /**
     * Updates the NWS forecast zone for the current user in Firestore.
     * This method will merge the zone information into the existing user document.
     *
     * @param userId The UID of the user whose forecast zone is to be updated.
     * @param zone The NWS forecast zone string (e.g., "FLZ051").
     * @throws Exception if an error occurs during Firestore operation.
     */
    open suspend fun updateUserForecastZone(userId: String, zone: String) {
        try {
            val updates = hashMapOf<String, Any?>(
                "forecastZone" to zone,
                "forecastZoneLastUpdated" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d(TAG, "User forecast zone updated for $userId: $zone")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user forecast zone for $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Updates the NWS Weather Forecast Office (WFO) identifier for the current user in Firestore.
     * This method will merge the WFO identifier into the existing user document.
     *
     * @param userId The UID of the user whose WFO identifier is to be updated.
     * @param wfoId The NWS WFO identifier (e.g., "TBW").
     * @throws Exception if an error occurs during Firestore operation.
     */
    open suspend fun updateUserWFOIdentifier(userId: String, wfoId: String) {
        try {
            val updates = hashMapOf<String, Any?>(
                "wfoIdentifier" to wfoId,
                "wfoIdentifierLastUpdated" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d(TAG, "User WFO identifier updated for $userId: $wfoId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user WFO identifier for $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Updates the NWS Radar Station identifier for the current user in Firestore.
     * This method will merge the radar station identifier into the existing user document.
     *
     * @param userId The UID of the user whose radar station identifier is to be updated.
     * @param radarStationId The NWS Radar Station identifier (e.g., "KTBW").
     * @throws Exception if an error occurs during Firestore operation.
     */
    open suspend fun updateUserRadarStation(userId: String, radarStationId: String) {
        try {
            val updates = hashMapOf<String, Any?>(
                "radarStation" to radarStationId,
                "radarStationLastUpdated" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge())
                .await()
            Log.d(TAG, "User radar station updated for $userId: $radarStationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user radar station for $userId: ${e.message}", e)
            throw e
        }
    }


    /**
     * Fetches the user's profile data from Firestore.
     *
     * @param userId The UID of the user to fetch.
     * @return A map of user profile data, or null if not found.
     * @throws Exception if an error occurs during Firestore fetch.
     */
    open suspend fun getUserProfile(userId: String): Map<String, Any?>? {
        return try {
            val documentSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            documentSnapshot.data
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile for $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Retrieves the current FCM registration token.
     * This token identifies the specific device installation for push notifications.
     *
     * @return The FCM token string on success, or null if an error occurs.
     */
    suspend fun getFCMToken(): String? {
        return try {
            val token = firebaseMessaging.token.await()
            Log.d(TAG, "FCM Token retrieved: $token")
            token
        } catch (e: Exception) {
            Log.e(TAG, "Error getting FCM token: ${e.message}", e)
            null
        }
    }

    /**
     * Saves the FCM registration token to the current user's Firestore document.
     * This allows sending direct messages to this specific device.
     *
     * @param userId The UID of the user.
     * @param token The FCM registration token.
     * @throws Exception if an error occurs during Firestore operation.
     */
    suspend fun saveFCMTokenToUserProfile(userId: String, token: String) {
        try {
            val updates = hashMapOf<String, Any?>(
                "fcmToken" to token,
                "fcmTokenLastUpdated" to System.currentTimeMillis()
            )
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(updates, SetOptions.merge()) // Use set with merge option to update specific fields
                .await()
            Log.d(TAG, "FCM token saved for user $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving FCM token for user $userId: ${e.message}", e)
            throw e
        }
    }

    /**
     * Subscribes the current device to the general "foulweather" FCM topic.
     * This allows sending broadcast messages to all users subscribed to this topic.
     *
     * Note: This topic might become less relevant if we move to WFO-specific topics.
     * @throws Exception if an error occurs during subscription.
     */
    suspend fun subscribeToFoulWeatherTopic() {
        try {
            firebaseMessaging.subscribeToTopic(FOULWEATHER_TOPIC).await()
            Log.d(TAG, "Subscribed to '$FOULWEATHER_TOPIC' topic")
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to '$FOULWEATHER_TOPIC' topic: ${e.message}", e)
            throw e
        }
    }

    /**
     * Subscribes the current device to an FCM topic specific to a Weather Forecast Office (WFO).
     *
     * @param wfoIdentifier The WFO identifier (e.g., "TBW").
     * @throws Exception if an error occurs during subscription.
     */
    suspend fun subscribeToWfoTopic(wfoIdentifier: String) {
        val topic = "$WFO_TOPIC_PREFIX$wfoIdentifier"
        try {
            firebaseMessaging.subscribeToTopic(topic).await()
            Log.d(TAG, "Subscribed to WFO topic: '$topic'")
        } catch (e: Exception) {
            Log.e(TAG, "Error subscribing to WFO topic '$topic': ${e.message}", e)
            throw e
        }
    }

    /**
     * Unsubscribes the current device from an FCM topic specific to a Weather Forecast Office (WFO).
     *
     * @param wfoIdentifier The WFO identifier (e.g., "TBW").
     * @throws Exception if an error occurs during unsubscription.
     */
    suspend fun unsubscribeFromWfoTopic(wfoIdentifier: String) {
        val topic = "$WFO_TOPIC_PREFIX$wfoIdentifier"
        try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            Log.d(TAG, "Unsubscribed from WFO topic: '$topic'")
        } catch (e: Exception) {
            Log.e(TAG, "Error unsubscribing from WFO topic '$topic': ${e.message}", e)
            throw e
        }
    }
}
