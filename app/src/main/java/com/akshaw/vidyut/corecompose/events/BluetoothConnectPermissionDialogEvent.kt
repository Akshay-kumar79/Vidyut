package com.akshaw.vidyut.corecompose.events

sealed interface BluetoothConnectPermissionDialogEvent{
    
    object ShowDialog: BluetoothConnectPermissionDialogEvent
    object DismissDialog: BluetoothConnectPermissionDialogEvent
    
}