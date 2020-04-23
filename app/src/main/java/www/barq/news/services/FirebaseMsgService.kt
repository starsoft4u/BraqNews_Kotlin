package www.barq.news.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import www.barq.news.Constants.Companion.APP_KEY
import www.barq.news.Constants.Companion.NOTIFICATION_CHANNEL_ID
import www.barq.news.Defaults
import www.barq.news.R
import www.barq.news.activity.MainActivity

class FirebaseMsgService : FirebaseMessagingService() {
    override fun onNewToken(token: String?) {
        super.onNewToken(token)

        Log.d(APP_KEY, "New FCMToken received: $token")

        if (Defaults.notifyGlobal) {
            Defaults.notificaitons.forEach {
                FirebaseMessaging.getInstance().subscribeToTopic(it.toString())
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        Log.d(APP_KEY, "New message from: ${message?.from}")

        // Show notification
        val title = message?.data?.get("title") ?: message?.notification?.title
        val body = message?.data?.get("body") ?: message?.notification?.body
        val newsId = message?.data?.get("newsId")
        notification(title, body, newsId)
    }

    private fun notification(title: String?, body: String?, newsId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                APP_KEY,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel description"
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        Defaults.notificationSound?.let {
            notificationBuilder.setSound(Uri.parse(it))
        }

        notificationBuilder.priority = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NotificationManager.IMPORTANCE_HIGH
        } else {
            Notification.PRIORITY_HIGH
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("newsId", newsId)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        notificationBuilder.setContentIntent(pendingIntent)

        notificationManager.notify(1, notificationBuilder.build())
    }
}
