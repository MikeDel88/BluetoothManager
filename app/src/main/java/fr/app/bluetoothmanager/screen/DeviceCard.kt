package fr.app.bluetoothmanager.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.app.bluetoothmanager.model.Device
import fr.app.bluetoothmanager.model.DeviceType
import fr.app.bluetoothmanager.ui.theme.BluetoothManagerTheme


@Preview
@Composable
private fun DeviceCardPreview() {
    BluetoothManagerTheme() {
        DeviceCard(
            modifier = Modifier.width(IntrinsicSize.Max),
            device = Device(
                name = "Test",
                address = "0:0:0:0:0:0",
                connected = true,
                bonded = true,
                type = DeviceType.CLASSIC
            )
        )
    }
}

@Composable
fun DeviceCard(modifier: Modifier = Modifier, device: Device) {
    Card(modifier = modifier.height(IntrinsicSize.Max).padding(8.dp)) {
        val smallSize = MaterialTheme.typography.bodySmall.fontSize
        val mediumSize = MaterialTheme.typography.bodyMedium.fontSize

        Row(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name ?: "Unknown", fontSize = mediumSize)
                Text(text = device.address, fontSize = smallSize)
                Text(text = device.type.toString().lowercase(), fontSize = smallSize)
            }
            Column(modifier = Modifier.fillMaxHeight().padding(start = 16.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
                Text(text = if(device.connected) "Connecté" else "Déconnecté", fontSize = smallSize)
                Text(text = if(device.bonded) "Appairé" else "Dissocié", fontSize = smallSize)
            }
        }
    }
}