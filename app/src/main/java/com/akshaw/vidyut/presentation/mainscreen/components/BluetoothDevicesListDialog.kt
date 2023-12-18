package com.akshaw.vidyut.presentation.mainscreen.components

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BluetoothDevicesListDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    bluetoothDevices: List<BluetoothDevice>,
    connect: (device: BluetoothDevice) -> Unit
) {
    
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Column {
                Log.v("MYTAG", "device: ${bluetoothDevices.map { it.name }}")
                bluetoothDevices.forEach {
                    BluetoothDeviceItem(
                        bluetoothDevice = it,
                        onClick = {
                            connect(it)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}