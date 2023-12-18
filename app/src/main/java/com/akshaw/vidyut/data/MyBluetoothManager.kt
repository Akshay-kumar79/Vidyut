package com.akshaw.vidyut.data

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MyBluetoothManager @Inject constructor(@ApplicationContext private val context: Context) {
    
    companion object {
        private const val MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"
        private const val DEVICE_NAME = "Vidyut"
    }
    
    private val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private val _bluetoothDevices = MutableStateFlow(listOf<BluetoothDevice>())
    val bluetoothDevices = _bluetoothDevices.asStateFlow()
    
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected = _isBluetoothConnected.asStateFlow()
    
    private var socket: BluetoothSocket? = null
    private var currentDevice: BluetoothDevice? = null
    
    private val bluetoothStateReceiver = BluetoothStateReceiver { isConnected, bluetoothDevice ->
        currentDevice?.let {
            if (bluetoothDevice == it) {
                _isBluetoothConnected.update { isConnected }
            }
        }
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    
                    device?.let {
                        addIfNotExist(it)
                    }
                }
            }
        }
    }
    
    init {
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }
    
    /**
     *  Check bluetooth availability before calling this function
     */
    @Suppress("MissingPermission")
    fun getPairedDevice(): Result<Unit> {
        
        return if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter!!.bondedDevices
            pairedDevices?.let {
                it.forEach { device ->
                    addIfNotExist(device)
                }
            }
            bluetoothAdapter.startDiscovery()
            Result.success(Unit)
        } else {
            Result.failure(Exception("No Bluetooth connect permission"))
        }
    }
    
    @Suppress("MissingPermission")
    fun cancelDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }
    
    @Suppress("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): Result<Unit> = withContext(Dispatchers.IO) {
        
        if (device.name != DEVICE_NAME) {
            return@withContext Result.failure(Exception("Please choose \"$DEVICE_NAME\""))
        }
        
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return@withContext Result.failure(Exception("No Bluetooth connect permission"))
        }
        
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            return@withContext Result.failure(Exception("No Bluetooth scan permission"))
        }
        
        currentDevice = device
        socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID))
        
        bluetoothAdapter?.cancelDiscovery()
        
        return@withContext try {
            socket?.connect()
            _isBluetoothConnected.update { true }
            Result.success(Unit)
        } catch (e: java.lang.Exception) {
            Result.failure(Exception("Failed to connect: ${e.message}"))
        }
        
    }
    
    private fun hasPermission(permission: String): Boolean {
        if (permission == Manifest.permission.BLUETOOTH_CONNECT && Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            return true
        }
    
        if (permission == Manifest.permission.BLUETOOTH_SCAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.S){
            return true
        }
        
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun addIfNotExist(device: BluetoothDevice) {
        if (!_bluetoothDevices.value.any { it.address == device.address }) {
            _bluetoothDevices.value += device
        }
    }
    
    suspend fun write(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!isBluetoothConnected.value) {
                return@withContext Result.failure(Exception("Bluetooth not connected"))
            }
            
            val outputStream: OutputStream? = socket?.outputStream
            if (outputStream != null) {
                outputStream.write(bytes)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Bluetooth not connected"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
    
    @Suppress("MissingPermission")
    fun clear() {
        coroutineScope.cancel()
        socket?.close()
        bluetoothAdapter?.cancelDiscovery()
        context.unregisterReceiver(receiver)
        context.unregisterReceiver(bluetoothStateReceiver)
    }
    
}