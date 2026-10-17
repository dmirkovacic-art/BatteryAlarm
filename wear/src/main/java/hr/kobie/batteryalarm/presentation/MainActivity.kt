/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package hr.kobie.batteryalarm.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import hr.kobie.batteryalarm.R
import hr.kobie.batteryalarm.presentation.theme.BatteryAlarmTheme
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.wear.compose.material.Button
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.media.MediaPlayer
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.media.AudioAttributes
import android.os.Build
import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, BatteryMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val batteryManager =
                getSystemService(BATTERY_SERVICE) as android.os.BatteryManager

            val batteryLevel = batteryManager.getIntProperty(
                android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY
            )

            WearApp(greetingName = "Battery: $batteryLevel%")
        }
    }
}


@Composable
fun WearApp(greetingName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var previousBatteryLevel by remember {
        mutableIntStateOf(-1)
    }

    var batteryLevel by remember {
        mutableIntStateOf(
            context.getSystemService(BatteryManager::class.java)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        )
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(
                        BatteryManager.EXTRA_LEVEL, -1
                    )
                    val scale = intent.getIntExtra(
                        BatteryManager.EXTRA_SCALE, 100
                    )

                    if (level >= 0 && scale > 0) {
                        val newLevel = level * 100 / scale

                        previousBatteryLevel = newLevel
                        batteryLevel = newLevel
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    BatteryAlarmTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Battery: $batteryLevel%")

                Button(
                    onClick = {
                        scope.launch {
                            testAlarm(context)
                        }
                    },
                    modifier = Modifier.size(100.dp)
                ) {
                    Text(
                        text = "TEST\nALARM",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}


suspend fun testAlarm(context: Context) {
    val vibrator = context.getSystemService(
        Context.VIBRATOR_SERVICE
    ) as Vibrator

    val player = MediaPlayer()

    try {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()
        )

        val afd = context.resources.openRawResourceFd(
            R.raw.battery_alarm
        )

        afd.use {
            player.setDataSource(
                it.fileDescriptor,
                it.startOffset,
                it.length
            )
        }

        player.prepare()
        player.start()

        repeat(3) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    300,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
            delay(600)
        }

        while (player.isPlaying) {
            delay(100)
        }
    } finally {
        player.release()
    }
}


private fun testNotification(context: Context) {
    val manager = context.getSystemService(
        Context.NOTIFICATION_SERVICE
    ) as NotificationManager

    val channelId = "battery_alarm_test_v1"

    val soundUri = RingtoneManager.getDefaultUri(
        RingtoneManager.TYPE_NOTIFICATION
    )

    val channel = NotificationChannel(
        channelId,
        "Battery Alarm Test",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Test zvučnih upozorenja baterije"
        setSound(
            soundUri,
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        enableVibration(true)
    }

    manager.createNotificationChannel(channel)

    if (Build.VERSION.SDK_INT >= 33 &&
        context.checkSelfPermission(
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        (context as? Activity)?.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            100
        )
        return
    }

    val notification = Notification.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Battery Alarm")
        .setContentText("Test zvučne obavijesti")
        .setAutoCancel(true)
        .build()

    manager.notify(1001, notification)
}