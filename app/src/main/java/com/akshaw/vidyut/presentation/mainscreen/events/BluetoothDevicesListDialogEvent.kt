package com.akshaw.vidyut.presentation.mainscreen.events

sealed interface BluetoothDevicesListDialogEvent {
    object ShowDialog: BluetoothDevicesListDialogEvent
    object DismissDialog: BluetoothDevicesListDialogEvent
}