package fr.app.bluetoothmanager.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.app.bluetoothmanager.R
import fr.app.bluetoothmanager.broadcast.BluetoothState
import fr.app.bluetoothmanager.state.ScanState
import fr.app.bluetoothmanager.viewmodel.BluetoothViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BluetoothAppContent(
    modifier: Modifier = Modifier,
    viewModel: BluetoothViewModel,
) {
    val state by viewModel.bluetoothState.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val boundedDevices by viewModel.boundedDevices.collectAsState()
    val scanState by viewModel.scanState.collectAsState()
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    val visibility by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        label = "visibility",
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val iconModifier = if(scanState == ScanState.Scanning) Modifier
        .graphicsLayer {
            alpha = visibility
        } else Modifier


    Column(modifier = modifier.padding(top = 100.dp, start = 16.dp, end = 16.dp)) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(100.dp), contentAlignment = Alignment.Center) {
            Icon(
                modifier = iconModifier.size(68.dp),
                painter = painterResource(when(state) {
                    BluetoothState.ON, BluetoothState.TURNING_ON -> R.drawable.bluetooth_24dp
                    BluetoothState.OFF, BluetoothState.TURNING_OFF -> R.drawable.bluetooth_disabled_24dp
                    BluetoothState.UNKNOWN -> R.drawable.bluetooth_disabled_24dp
                }),
                contentDescription =  when (state) {
                    BluetoothState.ON -> "Bluetooth activé"
                    BluetoothState.OFF -> "Bluetooth désactivé"
                    BluetoothState.TURNING_ON -> "Activation en cours..."
                    BluetoothState.TURNING_OFF -> "Désactivation en cours..."
                    BluetoothState.UNKNOWN -> "État inconnu"
                },
            )
        }

        if(state == BluetoothState.ON && scanState != ScanState.Scanning) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = { viewModel.startScan(context) }
                ) {
                    Text("Scanner les appareils")
                }
            }

        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Appareils associés")
        HorizontalDivider()
        LazyRow(contentPadding = PaddingValues(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(boundedDevices.size) {
                DeviceCard(modifier = Modifier.width(IntrinsicSize.Max), device = boundedDevices[it])
            }
        }

        Text("Appareils trouvés")
        HorizontalDivider()
        LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devices.size) {
                DeviceCard(modifier = Modifier.fillMaxWidth(), device = devices[it])
            }
        }
    }
}
