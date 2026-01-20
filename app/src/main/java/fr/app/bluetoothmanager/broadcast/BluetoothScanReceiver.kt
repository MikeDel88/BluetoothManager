package fr.app.bluetoothmanager.broadcast

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothScanReceiver(
    private val onDeviceFound: (BluetoothDevice) -> Unit,
    private val onUpdatedConnected: (BluetoothDevice, Boolean) -> Unit,
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
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                Log.d("BT_CLASSIC", "Device Connected : ${device?.name} - ${device?.address}")
                device?.let {
                    onUpdatedConnected(it, true)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                Log.d("BT_CLASSIC", "Device Disconnected : ${device?.name} - ${device?.address}")
                device?.let {
                    onUpdatedConnected(it, false)
                }
            }
        }
    }
}