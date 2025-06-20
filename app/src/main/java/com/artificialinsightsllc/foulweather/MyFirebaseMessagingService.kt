// app/src/main/java/com/artificialinsightsllc/foulweather/MyFirebaseMessagingService.kt
package com.artificialinsightsllc.foulweather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.artificialinsightsllc.foulweather.data.FirebaseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // IMPORTANT: Custom Channel ID from strings.xml
    private val CHANNEL_ID by lazy { getString(R.string.default_notification_channel_id) }
    // IMPORTANT: Custom Sound URI from resources
    private val CUSTOM_SOUND_URI by lazy { Uri.parse("android.resource://" + packageName + "/" + R.raw.newforecast) }

    /**
     * Called when the service is first created.
     * This is the perfect place to create notification channels.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MyFirebaseMessagingService onCreate called. Creating notification channel.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.foul_weather_channel_name), // NOW USING THE NEW STRING RESOURCE
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for weather alerts and important updates."
                setSound(CUSTOM_SOUND_URI, audioAttributes)
                Log.d(TAG, "NotificationChannel sound set for API >= O: ${CUSTOM_SOUND_URI}")
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "NotificationChannel created/updated via onCreate. ID: ${CHANNEL_ID}")
        }
    }

    /**
     * Called if the FCM registration token is updated.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        val firebaseService = FirebaseService()
        firebaseService.getCurrentUser()?.let { user ->
            scope.launch {
                try {
                    firebaseService.saveFCMTokenToUserProfile(user.uid, token)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save new FCM token for user ${user.uid}: ${e.message}")
                }
            }
        }
    }

    /**
     * Called when an FCM message is received.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message received at: ${System.currentTimeMillis()}")

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val wfoId = remoteMessage.data["wfoId"]
            val audioReady = remoteMessage.data["audioReady"]?.toBoolean() ?: false

            Log.d(TAG, "Extracted from data payload: wfoId=$wfoId, audioReady=$audioReady")

            if (wfoId != null && audioReady) {
                sendNotification(
                    messageTitle = remoteMessage.notification?.title ?: "New Forecast Summary Ready!",
                    messageBody = remoteMessage.notification?.body ?: "Your sarcastic weather summary for ${wfoId} is available.",
                    wfoId = wfoId,
                    audioReady = audioReady
                )
            } else {
                remoteMessage.notification?.let {
                    sendNotification(
                        messageTitle = it.title,
                        messageBody = it.body
                    )
                }
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     */
    private fun sendNotification(messageTitle: String?, messageBody: String?, wfoId: String? = null, audioReady: Boolean = false) {
        Log.d(TAG, "sendNotification called for title: '$messageTitle', body: '$messageBody'")
        Log.d(TAG, "sendNotification extras: wfoId=$wfoId, audioReady=$audioReady")

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (wfoId != null) {
                putExtra("wfoId", wfoId)
            }
            putExtra("audioReady", audioReady)
            Log.d(TAG, "Intent created with extras: ${this.extras}")
        }

        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        Log.d(TAG, "Using Notification Channel ID for builder: $CHANNEL_ID")

        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(messageTitle ?: "Foul Weather Alert")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(CUSTOM_SOUND_URI)
            .setContentIntent(pendingIntent)
            .setLargeIcon(largeIcon)
            .setColor(resources.getColor(R.color.purple_500, null))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
        Log.d(TAG, "Notification issued with channel ID: ${CHANNEL_ID}.")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}