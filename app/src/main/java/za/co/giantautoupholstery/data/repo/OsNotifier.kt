package za.co.giantautoupholstery.data.repo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import za.co.giantautoupholstery.domain.LocalNotifier
import javax.inject.Inject
import javax.inject.Singleton

/** LOCAL OS notifications for on-device events. This is not remote push (no FCM token registration is configured). */
@Singleton
class OsNotifier @Inject constructor(@ApplicationContext private val context: Context) : LocalNotifier {
    private val channelId = "booking_updates"

    override fun notify(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Booking updates", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val n = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), n) } catch (_: SecurityException) {}
    }
}
