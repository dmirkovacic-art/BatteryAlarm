package hr.kobie.batteryalarm.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable

object BatteryMessageSender {

    private const val TAG = "BatteryMessageSender"
    private const val MESSAGE_PATH = "/battery_alarm"

    fun sendBatteryAlert(context: Context, batteryLevel: Int) {

        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "Broj povezanih čvorova: ${nodes.size}")

                if (nodes.isEmpty()) {
                    Log.w(TAG, "Nema povezanog mobitela")
                    return@addOnSuccessListener
                }

                val message = batteryLevel.toString().toByteArray()

                nodes.forEach { node ->
                    messageClient.sendMessage(
                        node.id,
                        MESSAGE_PATH,
                        message
                    ).addOnSuccessListener {
                        Log.d(TAG, "Poruka poslana: $batteryLevel%")
                    }.addOnFailureListener { error ->
                        Log.e(TAG, "Slanje nije uspjelo", error)
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Nije moguće dohvatiti povezane uređaje", error)
            }
    }
}