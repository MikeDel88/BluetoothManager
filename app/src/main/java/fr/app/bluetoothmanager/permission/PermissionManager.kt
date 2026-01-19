package fr.app.bluetoothmanager.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

interface IPermissionManager {
    fun hasPermission(): Boolean
    fun requestPermission(callback: (granted: Boolean) -> Unit)
    fun shouldShowRationale(): Boolean
}

/**
 * Gère une seule permission.
 */
class SinglePermissionManager(
    private val activity: ComponentActivity,
    private val permission: String,
    private val launcher: ActivityResultLauncher<String>
) : IPermissionManager {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                onPermissionResult = null
            }
        })
    }

    override fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermission(callback: (granted: Boolean) -> Unit) {
        if (hasPermission()) {
            callback(true)
            return
        }
        onPermissionResult = callback
        launcher.launch(permission)
    }

    override fun shouldShowRationale(): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    internal fun handleResult(granted: Boolean) {
        onPermissionResult?.invoke(granted)
        onPermissionResult = null
    }
}

/**
 * Gère un groupe de permissions (ex: Bluetooth Scan + Connect).
 */
class MultiplePermissionManager(
    private val activity: ComponentActivity,
    private val permissions: Array<String>,
    private val launcher: ActivityResultLauncher<Array<String>>
) : IPermissionManager {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    init {
        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                onPermissionResult = null
            }
        })
    }

    override fun hasPermission(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun requestPermission(callback: (granted: Boolean) -> Unit) {
        if (hasPermission()) {
            callback(true)
            return
        }
        onPermissionResult = callback
        launcher.launch(permissions)
    }

    override fun shouldShowRationale(): Boolean {
        return permissions.any { activity.shouldShowRequestPermissionRationale(it) }
    }

    internal fun handleResult(results: Map<String, Boolean>) {
        val allGranted = results.values.all { it }
        onPermissionResult?.invoke(allGranted)
        onPermissionResult = null
    }
}

class PermissionManagerFactory(private val activity: ComponentActivity) {

    private val singleManagers = mutableMapOf<String, SinglePermissionManager>()
    private val multipleManagers = mutableMapOf<Int, MultiplePermissionManager>()

    // Launchers pour permissions uniques
    private val bluetoothScanLauncher = createSingleLauncher(Manifest.permission.BLUETOOTH_SCAN)
    private val bluetoothAdvertiseLauncher = createSingleLauncher(Manifest.permission.BLUETOOTH_ADVERTISE)
    private val bluetoothConnectLauncher = createSingleLauncher(Manifest.permission.BLUETOOTH_CONNECT)
    private val fineLocationLauncher = createSingleLauncher(Manifest.permission.ACCESS_FINE_LOCATION)
    private val coarseLocationLauncher = createSingleLauncher(Manifest.permission.ACCESS_COARSE_LOCATION)

    // Launcher pour permissions multiples (Bluetooth complet)
    private val bluetoothGroupPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }

    private val bluetoothGroupLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        multipleManagers[bluetoothGroupPermissions.contentHashCode()]?.handleResult(results)
    }

    private fun createSingleLauncher(permission: String) = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        singleManagers[permission]?.handleResult(granted)
    }

    // Accesseurs pour permissions uniques
    fun bluetoothScan() = getOrCreateSingle(Manifest.permission.BLUETOOTH_SCAN, bluetoothScanLauncher)
    fun bluetoothAdvertise() = getOrCreateSingle(Manifest.permission.BLUETOOTH_ADVERTISE, bluetoothAdvertiseLauncher)
    fun bluetoothConnect() = getOrCreateSingle(Manifest.permission.BLUETOOTH_CONNECT, bluetoothConnectLauncher)
    fun fineLocation() = getOrCreateSingle(Manifest.permission.ACCESS_FINE_LOCATION, fineLocationLauncher)
    fun coarseLocation() = getOrCreateSingle(Manifest.permission.ACCESS_COARSE_LOCATION, coarseLocationLauncher)

    // Accesseur pour le groupe Bluetooth + Location
    fun bluetoothWithLocation(): IPermissionManager {
        val key = bluetoothGroupPermissions.contentHashCode()
        return multipleManagers.getOrPut(key) {
            MultiplePermissionManager(activity, bluetoothGroupPermissions, bluetoothGroupLauncher)
        }
    }

    // Accesseur pour le groupe Bluetooth complet original
    fun bluetoothFull(): IPermissionManager {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
        )
        val key = permissions.contentHashCode()
        return multipleManagers.getOrPut(key) {
            val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                multipleManagers[key]?.handleResult(results)
            }
            MultiplePermissionManager(activity, permissions, launcher)
        }
    }

    private fun getOrCreateSingle(permission: String, launcher: ActivityResultLauncher<String>): IPermissionManager {
        return singleManagers.getOrPut(permission) {
            SinglePermissionManager(activity, permission, launcher)
        }
    }
}
