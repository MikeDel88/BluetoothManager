package fr.app.bluetoothmanager.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.app.bluetoothmanager.state.ScanState
import fr.app.bluetoothmanager.broadcast.BluetoothScanReceiver
import fr.app.bluetoothmanager.broadcast.BluetoothState
import fr.app.bluetoothmanager.broadcast.BluetoothStateReceiver
import fr.app.bluetoothmanager.model.Device
import fr.app.bluetoothmanager.model.DeviceType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothViewModel : ViewModel() {

    /* ---------- Bluetooth state ---------- */

    private val _bluetoothState =
        MutableStateFlow(BluetoothState.UNKNOWN)
    val bluetoothState: StateFlow<BluetoothState> =
        _bluetoothState.asStateFlow()

    /* ---------- Scan state ---------- */

    private val _scanState =
        MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> =
        _scanState.asStateFlow()

    private val _devices =
        MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> =
        _devices.asStateFlow()

    private val _boundedDevices =
        MutableStateFlow<List<Device>>(emptyList())
    val boundedDevices: StateFlow<List<Device>> =
        _boundedDevices.asStateFlow()

    /* ---------- Receiver ---------- */

    private val stateReceiver = BluetoothStateReceiver {
        _bluetoothState.value = it
        if (it != BluetoothState.ON) {
            stopScanInternal()
        }
    }

    private val bluetoothScanReceiver = BluetoothScanReceiver { bluetoothDevice ->
        @SuppressLint("MissingPermission")
        majDevices(bluetoothDevice)
    }

    /* ---------- BLE ---------- */

    private var bleScanner: BluetoothLeScanner? = null

    private val bleCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val bluetoothDevice = result.device ?: return
            majDevices(bluetoothDevice)

        }

        override fun onScanFailed(errorCode: Int) {
            _scanState.value =
                ScanState.Error("BLE scan error: $errorCode")
        }
    }

    /* ---------- Public API ---------- */

    fun startListening(context: Context) {
        val appContext = context.applicationContext
        appContext.registerReceiver(stateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        appContext.registerReceiver(bluetoothScanReceiver, filter)
        updateInitialState()
    }

    fun stopListening(context: Context) {
        stopScanInternal()
        context.applicationContext.unregisterReceiver(stateReceiver)
        context.applicationContext.unregisterReceiver(bluetoothScanReceiver)
    }

    @SuppressLint("MissingPermission")
    fun startScan(context: Context) {
        if (_scanState.value == ScanState.Scanning) return

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            _scanState.value = ScanState.Error("Bluetooth OFF")
            return
        }

        bleScanner = adapter.bluetoothLeScanner
        _devices.value = emptyList()
        _scanState.value = ScanState.Scanning

        bleScanner?.startScan(bleCallback)
        startDiscovery(context)

        viewModelScope.launch {
            delay(40_000)
            stopScanInternal()
        }

    }

    @SuppressLint("MissingPermission")
    fun startDiscovery(context: Context) {
        val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter ?: return

        if(bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.bondedDevices.forEach { bluetoothDevice ->
            val newDevice = bluetoothDevice.toDevice()
            _boundedDevices.update { list ->
                if (list.none { it.address == newDevice.address }) {
                    list + newDevice
                } else list

            }
        }

        bluetoothAdapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.cancelDiscovery()
    }

    fun stopScan() {
        stopScanInternal()
    }

    /* ---------- Internal ---------- */

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        bleScanner?.stopScan(bleCallback)
        bleScanner = null
        stopDiscovery()
        _scanState.value = ScanState.Idle
    }

    private fun updateInitialState() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        _bluetoothState.value = when {
            adapter == null -> BluetoothState.UNKNOWN
            adapter.isEnabled -> BluetoothState.ON
            else -> BluetoothState.OFF
        }
    }

    private fun BluetoothDevice.toDevice() = Device(
        name = name,
        address = address,
        bonded = bondState == BluetoothDevice.BOND_BONDED,
        connected = false,
        type = DeviceType.CLASSIC
    )

    private fun majDevices(bluetoothDevice: BluetoothDevice) {
        val newDevice = bluetoothDevice.toDevice()
        _devices.update { list ->
            if (list.none { it.address == newDevice.address }) {
                list + newDevice
            } else {
                // Mettre à jour le nom si possible
                list.map { if(it.address == newDevice.address) newDevice else it }
            }
        }
    }

    override fun onCleared() {
        stopScanInternal()
        super.onCleared()
    }
}