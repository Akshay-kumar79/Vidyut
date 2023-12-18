package com.akshaw.vidyut.presentation.mainscreen.components

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
@Suppress("MissingPermission")
fun BluetoothDeviceItem(
    modifier: Modifier = Modifier,
    bluetoothDevice: BluetoothDevice,
    onClick: (device: BluetoothDevice) -> Unit
) {
    
    if (bluetoothDevice.name != null)
        Row(
            modifier = modifier
                .clickable {
                    onClick(bluetoothDevice)
                }
                .padding(vertical = 16.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .padding(start = 32.dp),
                text = bluetoothDevice.name,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    
}