package hr.kobie.batteryalarm.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class BatteryMonitorService : Service() {

    companion object {
        private const val CHANNEL_ID = "battery_monitor"
        private const val NOTIFICATION_ID = 1001
    }
    private var previousBatteryLevel = -1
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)

            if (level < 0 || scale <= 0) return

            val newLevel = level * 100 / scale


            val crossedThreshold = listOf(15, 10, 5)
                .firstOrNull { threshold ->
                    previousBatteryLevel > threshold &&
                            newLevel <= threshold
                }

            if (crossedThreshold != null) {
                serviceScope.launch {
                    testAlarm(context)
                }

                BatteryMessageSender.sendBatteryAlert(
                    context,
                    crossedThreshold
                )
            }


            previousBatteryLevel = newLevel
        }
    }

    override fun onCreate() {
        super.onCreate()

        val manager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Battery Alarm Monitor",
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(channel)

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Alarm")
            .setContentText("Praćenje baterije je aktivno")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
    }

    override fun onDestroy() {
        unregisterReceiver(batteryReceiver)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}