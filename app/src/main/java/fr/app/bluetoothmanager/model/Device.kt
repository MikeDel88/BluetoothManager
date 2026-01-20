package fr.app.bluetoothmanager.model

enum class DeviceType {
    CLASSIC,
    BLE
}

data class Device(
    val name: String?,
    val address: String,
    val bonded: Boolean,
    val connected: Boolean,
    val type: DeviceType
)