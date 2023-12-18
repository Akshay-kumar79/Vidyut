package com.akshaw.vidyut.presentation.mainscreen

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akshaw.vidyut.corecompose.events.BluetoothConnectPermissionDialogEvent
import com.akshaw.vidyut.data.MyBluetoothManager
import com.akshaw.vidyut.presentation.mainscreen.events.BluetoothDevicesListDialogEvent
import com.akshaw.vidyut.presentation.mainscreen.events.MainScreenEvent
import com.autoponix.m100bps.corecompose.UiEvent
import com.autoponix.m100bps.corecompose.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val myBluetoothManager: MyBluetoothManager
) : ViewModel() {
    
    /** Bluetooth */
    val bluetoothDevices = myBluetoothManager.bluetoothDevices
    val isBluetoothConnected = myBluetoothManager.isBluetoothConnected
    
    private val _isBluetoothListDialogShowing = MutableStateFlow(false)
    val isBluetoothListDialogShowing = _isBluetoothListDialogShowing.asStateFlow()
    
    private val _connectingDialogShowing = MutableStateFlow(false)
    val connectingDialogShowing = _connectingDialogShowing.asStateFlow()
    
    /** Permissions */
    private val _hasBluetoothConnectPermission = MutableStateFlow(true)
    val hasBluetoothConnectPermission = _hasBluetoothConnectPermission.asStateFlow()
    
    private val _isReRequestBluetoothConnectPermDialogVisible = MutableStateFlow(false)
    val isReRequestBluetoothConnectPermDialogVisible = _isReRequestBluetoothConnectPermDialogVisible.asStateFlow()
    
    
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()
    
    
    init {
        viewModelScope.launch {
            isBluetoothConnected.collectLatest {
                _connectingDialogShowing.update { false }
            }
        }
    }
    
    fun onEvent(event: MainScreenEvent) {
        when (event) {
            is MainScreenEvent.ChangeHasBluetoothConnectPermission -> {
                _hasBluetoothConnectPermission.value = event.hasPermission
            }
            
            is MainScreenEvent.OnPermissionResult -> {
                when (event.permission) {
                    Manifest.permission.BLUETOOTH_CONNECT -> {
                        if (event.isGranted)
                            _hasBluetoothConnectPermission.value = true
                        else
                            _isReRequestBluetoothConnectPermDialogVisible.value = true
                    }
                }
            }
            
            MainScreenEvent.OnScanBluetoothDevice -> {
                startScanningBluetoothDevice()
            }
            
            is MainScreenEvent.OnBluetoothDeviceClick -> {
                connectToDevice(event.device)
                _isBluetoothListDialogShowing.update { false }
            }
        }
    }
    
    fun onEvent(event: BluetoothDevicesListDialogEvent) {
        when (event) {
            BluetoothDevicesListDialogEvent.ShowDialog -> {
                _isBluetoothListDialogShowing.update { true }
            }
            
            BluetoothDevicesListDialogEvent.DismissDialog -> {
                _isBluetoothListDialogShowing.update { false }
                myBluetoothManager.cancelDiscovery()
            }
        }
    }
    
    fun onEvent(event: BluetoothConnectPermissionDialogEvent) {
        when (event) {
            BluetoothConnectPermissionDialogEvent.ShowDialog -> {
                _isReRequestBluetoothConnectPermDialogVisible.value = true
            }
            
            BluetoothConnectPermissionDialogEvent.DismissDialog -> {
                _isReRequestBluetoothConnectPermDialogVisible.value = false
            }
        }
    }
    
    
    private fun startScanningBluetoothDevice() {
        viewModelScope.launch {
            if (isBluetoothConnected.value) {
                showSnackbar(UiText.DynamicString("Bluetooth is already connected"))
                return@launch
            }
            
            if (getPairedDevice()) {
                _isBluetoothListDialogShowing.update { true }
            }
        }
    }
    
    /**
     *  Check bluetooth availability before calling this function
     */
    private suspend fun getPairedDevice(): Boolean {
        val result = myBluetoothManager.getPairedDevice()
            .onFailure {
                showSnackbar(UiText.DynamicString(it.message ?: "Something went wrong"))
            }
        
        return result.isSuccess
    }
    
    private fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            _connectingDialogShowing.update { true }
            myBluetoothManager.connectToDevice(device)
                .onSuccess {
                    _connectingDialogShowing.update { false }
                }
                .onFailure {
                    _connectingDialogShowing.update { false }
                    viewModelScope.launch {
                        showSnackbar(UiText.DynamicString(it.message ?: "Something went wrong"))
                    }
                }
        }
    }
    
    fun turnOnLed() = viewModelScope.launch {
        myBluetoothManager.write("a".toByteArray())
            .onFailure {
                showSnackbar(UiText.DynamicString(it.message ?: "Something went wrong"))
            }
    }
    
    fun turnOffLed() = viewModelScope.launch {
        myBluetoothManager.write("b".toByteArray())
            .onFailure {
                showSnackbar(UiText.DynamicString(it.message ?: "Something went wrong"))
            }
    }
    
    private suspend fun showSnackbar(message: UiText) {
        _uiEvent.send(
            UiEvent.ShowSnackBar(message)
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        myBluetoothManager.clear()
    }
    
}