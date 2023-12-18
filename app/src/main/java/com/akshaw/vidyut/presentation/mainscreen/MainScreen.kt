package com.akshaw.vidyut.presentation.mainscreen

import android.Manifest
import com.akshaw.vidyut.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.hilt.navigation.compose.hiltViewModel
import com.akshaw.vidyut.corecompose.components.BluetoothConnectPermissionTextProvider
import com.akshaw.vidyut.corecompose.components.ComposeLifecycle
import com.akshaw.vidyut.corecompose.components.PermissionDialog
import com.akshaw.vidyut.corecompose.events.BluetoothConnectPermissionDialogEvent
import com.akshaw.vidyut.presentation.mainscreen.components.BluetoothDevicesListDialog
import com.akshaw.vidyut.presentation.mainscreen.events.BluetoothDevicesListDialogEvent
import com.akshaw.vidyut.presentation.mainscreen.events.MainScreenEvent
import com.autoponix.m100bps.corecompose.UiEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    shouldShowRequestPermissionRationale: (permission: String) -> Boolean,
    openAppSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
    val connectingDialogShowing by viewModel.connectingDialogShowing.collectAsState()
    val isBluetoothListDialogShowing by viewModel.isBluetoothListDialogShowing.collectAsState()
    
    val bluetoothDevices by viewModel.bluetoothDevices.collectAsState()
    val hasBluetoothConnectPermission by viewModel.hasBluetoothConnectPermission.collectAsState()
    val isReRequestBluetoothConnectPermDialogVisible by viewModel.isReRequestBluetoothConnectPermDialogVisible.collectAsState()
    
    MainScreenImp(
        bluetoothDevices = bluetoothDevices,
        isBluetoothConnected = isBluetoothConnected,
        snackbarHostState = snackbarHostState,
        hasBluetoothConnectPermission = hasBluetoothConnectPermission,
        isBluetoothListDialogShowing = isBluetoothListDialogShowing,
        uiEvent = viewModel.uiEvent,
        isReRequestBluetoothConnectPermDialogVisible = isReRequestBluetoothConnectPermDialogVisible,
        onEvent = viewModel::onEvent,
        onBlEvent = viewModel::onEvent,
        onBlConnectPermDialogEvent = viewModel::onEvent,
        shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale,
        openAppSettings = openAppSettings,
        turnOnLed = viewModel::turnOnLed,
        turnOffLed = viewModel::turnOffLed,
    )
}

@Composable
private fun MainScreenImp(
    bluetoothDevices: List<BluetoothDevice>,
    isBluetoothConnected: Boolean,
    snackbarHostState: SnackbarHostState,
    hasBluetoothConnectPermission: Boolean,
    isBluetoothListDialogShowing: Boolean,
    uiEvent: Flow<UiEvent>,
    isReRequestBluetoothConnectPermDialogVisible: Boolean,
    onEvent: (event: MainScreenEvent) -> Unit,
    onBlEvent: (event: BluetoothDevicesListDialogEvent) -> Unit,
    onBlConnectPermDialogEvent: (event: BluetoothConnectPermissionDialogEvent) -> Unit,
    shouldShowRequestPermissionRationale: (permission: String) -> Boolean,
    openAppSettings: () -> Unit,
    turnOnLed: () -> Unit,
    turnOffLed: () -> Unit
) {
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java).adapter
    
    val enableBluetooth = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            onEvent(MainScreenEvent.OnScanBluetoothDevice)
        } else {
            Toast.makeText(context, "Failed to turn on bluetooth", Toast.LENGTH_SHORT).show()
        }
    }
    
    val bluetoothScanPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onEvent(MainScreenEvent.OnPermissionResult(Manifest.permission.BLUETOOTH_SCAN, granted))
        
        if (granted) {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetooth.launch(enableBtIntent)
            } else {
                onEvent(MainScreenEvent.OnScanBluetoothDevice)
            }
        }
    }
    
    val bluetoothConnectPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onEvent(MainScreenEvent.OnPermissionResult(Manifest.permission.BLUETOOTH_CONNECT, granted))
        
        if (granted) {
            if (
                !context.hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                bluetoothScanPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                return@rememberLauncherForActivityResult
            }
            
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetooth.launch(enableBtIntent)
            } else {
                onEvent(MainScreenEvent.OnScanBluetoothDevice)
            }
        }
    }
    
    val setHasPermissions = {
        val bluetoothConnectPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        else
            true
        
        onEvent(MainScreenEvent.ChangeHasBluetoothConnectPermission(bluetoothConnectPermission))
    }
    
    LaunchedEffect(key1 = true) {
        setHasPermissions()
        
        uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackBar -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }
                
                else -> Unit
            }
        }
    }
    
    // Lifecycle callbacks
    ComposeLifecycle(
        onResume = {
            setHasPermissions()
        }
    )
    
    Column {
        Spacer(modifier = Modifier.height(16.dp))
        if (isBluetoothConnected) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
//                Image(
//                    painter = painterResource(id = R.drawable.ic_granted_permission_icon),
//                    contentDescription = "",
//                )
//
//                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Bluetooth is connected",
                )
            }
        } else {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                text = "Bluetooth is not connected",
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            onClick = {
                if (bluetoothAdapter == null) {
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Bluetooth not available")
                    }
                    return@Button
                }
                
                if (!hasBluetoothConnectPermission) {
                    if (
                        !context.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ) {
                        bluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        return@Button
                    }
                }
                
                if (
                    !context.hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    bluetoothScanPermissionLauncher.launch(Manifest.permission.BLUETOOTH_SCAN)
                    return@Button
                }
                
                if (!bluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetooth.launch(enableBtIntent)
                } else {
                    onEvent(MainScreenEvent.OnScanBluetoothDevice)
                }
            }
        ) {
            Text(text = "Scan bluetooth")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            onClick = {
                turnOnLed()
            }
        ) {
            Text(text = "Turn on LED")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            onClick = {
                turnOffLed()
            }
        ) {
            Text(text = "Turn off LED")
        }
    }
    
    if (isBluetoothListDialogShowing) {
        BluetoothDevicesListDialog(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            onDismiss = {
                onBlEvent(BluetoothDevicesListDialogEvent.DismissDialog)
            },
            bluetoothDevices = bluetoothDevices,
            connect = {
                onEvent(MainScreenEvent.OnBluetoothDeviceClick(it))
            }
        )
    }
    
    if (isReRequestBluetoothConnectPermDialogVisible) {
        PermissionDialog(
            permissionTextProvider = BluetoothConnectPermissionTextProvider(),
            isPermanentlyDeclined = !shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT),
            onDismiss = { onBlConnectPermDialogEvent(BluetoothConnectPermissionDialogEvent.DismissDialog) },
            onOkClick = {
                onBlConnectPermDialogEvent(BluetoothConnectPermissionDialogEvent.DismissDialog)
                bluetoothConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            },
            onGoToAppSettingsClick = {
                onBlConnectPermDialogEvent(BluetoothConnectPermissionDialogEvent.DismissDialog)
                openAppSettings()
            }
        )
    }
}

@Preview
@Composable
private fun MainScreenPreview() {

}

private fun Context.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}