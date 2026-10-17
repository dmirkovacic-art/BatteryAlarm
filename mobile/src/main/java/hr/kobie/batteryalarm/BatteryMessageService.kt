
package hr.kobie.batteryalarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class BatteryMessageService : WearableListenerService() {

    companion object {
        private const val CHANNEL_ID = "watch_battery_alerts"
        private const val TAG = "BatteryAlarm"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        if (messageEvent.path != "/battery_alarm") return

        val batteryLevel = String(messageEvent.data)
            .toIntOrNull() ?: return

        Log.d(TAG, "Primljeno upozorenje sa sata: $batteryLevel%")

        showBatteryNotification(batteryLevel)
    }

    private fun showBatteryNotification(level: Int) {

        val manager = getSystemService(
            NotificationManager::class.java
        )

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Baterija sata",
            NotificationManager.IMPORTANCE_HIGH
        )

        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Battery Alarm – Xiaomi Watch")
            .setContentText("Baterija sata pala je na $level%!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Dopuštenje za obavijesti nije odobreno")
            return
        }

        manager.notify(1000 + level, notification)
    }
}
