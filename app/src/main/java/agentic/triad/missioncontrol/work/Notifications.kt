package agentic.triad.missioncontrol.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** The native digest surface — the analog of the web client's Telegram message. */
object Notifications {
    private const val CHANNEL_ID = "triad_broadcasts"
    private const val NOTIF_ID = 4001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "TRIAD broadcasts", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Checkup verdict + alert digests" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    /** Post the digest; a no-op (returns false) if POST_NOTIFICATIONS isn't granted (API 33+). */
    fun postDigest(context: Context, title: String, body: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        ensureChannel(context)
        val notif = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(body))
            .setContentText(body.lineSequence().firstOrNull() ?: body)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
        return true
    }
}
