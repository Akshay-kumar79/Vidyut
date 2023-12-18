package com.akshaw.vidyut.presentation.mainscreen.events

import android.bluetooth.BluetoothDevice

sealed interface MainScreenEvent {
    data class ChangeHasBluetoothConnectPermission(val hasPermission: Boolean) : MainScreenEvent
    data class OnPermissionResult(val permission: String, val isGranted: Boolean) : MainScreenEvent
    object OnScanBluetoothDevice : MainScreenEvent
    data class OnBluetoothDeviceClick(val device: BluetoothDevice) : MainScreenEvent
}