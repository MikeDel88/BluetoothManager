package fr.app.bluetoothmanager.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.IntentFilter
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.app.bluetoothmanager.broadcast.BluetoothScanReceiver
import fr.app.bluetoothmanager.broadcast.BluetoothState
import fr.app.bluetoothmanager.broadcast.BluetoothStateReceiver
import fr.app.bluetoothmanager.model.Device
import fr.app.bluetoothmanager.model.DeviceType
import fr.app.bluetoothmanager.state.ScanState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothViewModel : ViewModel() {

    /* ---------- Bluetooth state ---------- */

    private var bluetoothDevices: MutableList<BluetoothDevice> = mutableListOf()

//    companion object {
//        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-008")
//    }

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

    private val bluetoothScanReceiver = BluetoothScanReceiver(
        onDeviceFound =  { bluetoothDevice ->
            @SuppressLint("MissingPermission")
            majDevices(bluetoothDevice, DeviceType.CLASSIC)
        },
        onUpdatedConnected = ::onConnectedChange
    )

    /* ---------- BLE ---------- */

    private var bleScanner: BluetoothLeScanner? = null

    private val bleCallback = object : ScanCallback() {

        @SuppressLint("MissingPermission")
        override fun onScanResult(
            callbackType: Int,
            result: ScanResult
        ) {
            val bluetoothDevice = result.device ?: return
            majDevices(bluetoothDevice, DeviceType.BLE)

        }

        override fun onScanFailed(errorCode: Int) {
            _scanState.value =
                ScanState.Error("BLE scan error: $errorCode")
        }
    }

    /* ---------- Public API ---------- */

    fun startListening(context: Context) {
        val appContext = context.applicationContext

        val bleFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        appContext.registerReceiver(stateReceiver, bleFilter)

        val classicfilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        appContext.registerReceiver(bluetoothScanReceiver, classicfilter)
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

        val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val adapter: BluetoothAdapter? = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) {
            _scanState.value = ScanState.Error("Bluetooth OFF")
            return
        }

        bleScanner = adapter.bluetoothLeScanner
        bluetoothDevices = mutableListOf()
        _devices.value = emptyList()
        _scanState.value = ScanState.Scanning

        bleScanner?.startScan(bleCallback)
        startDiscovery(adapter)

        loadBondedDevices(adapter)
        getConnectedDevice(context, adapter)

        viewModelScope.launch {
            delay(40_000)
            stopScanInternal()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun pairDevice(device: Device) {
        val bluetoothDevice = bluetoothDevices.find { it.address == device.address }
        bluetoothDevice?.let {
            when(device.type) {
                DeviceType.CLASSIC -> {
                    bluetoothDevice.createBond()
                }
                DeviceType.BLE -> {

                }
            }
        }
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

    @SuppressLint("MissingPermission")
    private fun startDiscovery(bluetoothAdapter: BluetoothAdapter) {
        if(bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }

        bluetoothAdapter.startDiscovery()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun loadBondedDevices(adapter: BluetoothAdapter) {
        adapter.bondedDevices.forEach { bluetoothDevice ->
            val newDevice = bluetoothDevice.toDevice(DeviceType.CLASSIC)
            _boundedDevices.update { list ->
                if (list.none { it.address == newDevice.address }) {
                    list + newDevice
                } else list

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopDiscovery() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        adapter?.cancelDiscovery()
    }

    private fun getConnectedDevice(context: Context, adapter: BluetoothAdapter) {
        updateConnected(context.applicationContext, adapter, listOf(BluetoothProfile.STATE_CONNECTED)) { connected ->
            _boundedDevices.update { list ->
                list.map { device ->
                    device.copy(
                        connected = connected.any { it.address == device.address }
                    )
                }
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun BluetoothDevice.toDevice(type: DeviceType) = Device(
        name = name,
        address = address,
        bonded = bondState == BluetoothDevice.BOND_BONDED,
        connected = false,
        type = type
    )

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun majDevices(bluetoothDevice: BluetoothDevice, type: DeviceType) {
        val newDevice = bluetoothDevice.toDevice(type)
        _devices.update { list ->
            if (list.none { it.address == newDevice.address }) {
                list + newDevice
            } else {
                // Mettre à jour le nom si possible
                list.map { if(it.address == newDevice.address) newDevice else it }
            }
        }
        bluetoothDevices.add(bluetoothDevice)
    }

    /**
     * Met à jour la liste des appareils connectés en fonction de l'état de la connexion.
     */
    private fun onConnectedChange(bluetoothDevice: BluetoothDevice, isConnected: Boolean) {
        val existing = _boundedDevices.value.find { it.address == bluetoothDevice.address }
        existing?.let {
            _boundedDevices.update { list ->
                list.map { if(it.address == existing.address) it.copy(connected = isConnected) else it }
            }
        }
    }

    /**
     * Met à jour la liste des appareils connectés en fonction des profils.
     */
    private fun updateConnected(
        context: Context, adapter:
        BluetoothAdapter,
        profile: List<Int>,
        onConnected: (List<BluetoothDevice>) -> Unit
    ) {
        profile.forEach {
            adapter.getProfileProxy(
                context,
                object : BluetoothProfile.ServiceListener {

                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        val connected = proxy.connectedDevices
                        onConnected(connected)

                        adapter.closeProfileProxy(profile, proxy)
                    }

                    override fun onServiceDisconnected(profile: Int) {}
                },
                it
            )
        }
    }

    override fun onCleared() {
        stopScanInternal()
        super.onCleared()
    }
}