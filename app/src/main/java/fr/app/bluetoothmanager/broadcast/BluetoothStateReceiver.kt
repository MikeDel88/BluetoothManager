package fr.app.bluetoothmanager.broadcast

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

enum class BluetoothState {
    OFF,
    TURNING_ON,
    ON,
    TURNING_OFF,
    UNKNOWN
}

class BluetoothStateReceiver(
    private val onStateChanged: (BluetoothState) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if(intent == null) return

        if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return

        val state = when (
            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        ) {
            BluetoothAdapter.STATE_ON -> BluetoothState.ON
            BluetoothAdapter.STATE_OFF -> BluetoothState.OFF
            BluetoothAdapter.STATE_TURNING_ON -> BluetoothState.TURNING_ON
            BluetoothAdapter.STATE_TURNING_OFF -> BluetoothState.TURNING_OFF
            else -> BluetoothState.UNKNOWN
        }

        onStateChanged(state)
    }
}