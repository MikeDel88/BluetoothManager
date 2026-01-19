package fr.app.bluetoothmanager.broadcast

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothScanReceiver(
    private val onDeviceFound: (BluetoothDevice) -> Unit,
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("BT_CLASSIC", "Action reçue: ${intent?.action}")
        when (intent?.action) {

            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                Log.d("BT_CLASSIC", "Found: ${device?.name} - ${device?.address}")

                device?.let {
                    onDeviceFound(it)
                }
            }
        }
    }
}