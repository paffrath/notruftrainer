package koeln.paffrath.notruftrainer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.IBinder
import androidx.core.content.ContextCompat

class PowerButtonDetectorService : Service() {

    private val pressTimestamps = ArrayDeque<Long>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_SCREEN_ON) {
                onScreenToggle()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun onScreenToggle() {
        val now = System.currentTimeMillis()
        pressTimestamps.removeAll { now - it > WINDOW_MS }
        pressTimestamps.addLast(now)
        if (pressTimestamps.size >= REQUIRED_EVENTS) {
            pressTimestamps.clear()
            triggerEmergencyCall()
        }
    }

    private fun triggerEmergencyCall() {
        val mapping = findMapping("112") ?: return
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${mapping.actualTarget}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(callIntent)
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Notruf-Erkennung", NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) }
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Notruftrainer aktiv")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "notruf_detector"
        private const val REQUIRED_EVENTS = 5  // 5 Einschalt-/Ausschalt-Events = 5 Power-Drücke
        private const val WINDOW_MS = 3000L
    }
}
