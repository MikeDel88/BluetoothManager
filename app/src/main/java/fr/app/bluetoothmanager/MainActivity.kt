package fr.app.bluetoothmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.app.bluetoothmanager.permission.PermissionManagerFactory
import fr.app.bluetoothmanager.screen.BluetoothAppContent
import fr.app.bluetoothmanager.ui.theme.BluetoothManagerTheme
import fr.app.bluetoothmanager.viewmodel.BluetoothViewModel
import fr.app.bluetoothmanager.viewmodel.BluetoothViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var permissionFactory: PermissionManagerFactory

    private val bluetoothPermissionManager by lazy {
        permissionFactory.bluetoothWithLocation()
    }

    private val bluetoothViewModel: BluetoothViewModel by viewModels {
        BluetoothViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        permissionFactory = PermissionManagerFactory(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluetoothManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var hasPermission by remember { mutableStateOf(bluetoothPermissionManager.hasPermission()) }
                    if(hasPermission)
                        BluetoothAppContent(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            viewModel = bluetoothViewModel
                        )
                    else
                        bluetoothPermissionManager.requestPermission { granted ->
                            hasPermission = granted
                        }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(bluetoothPermissionManager.hasPermission())
            bluetoothViewModel.startListening(this)
    }

    override fun onStop() {
        bluetoothViewModel.stopListening(this)
        bluetoothViewModel.stopScan()
        super.onStop()
    }
}



