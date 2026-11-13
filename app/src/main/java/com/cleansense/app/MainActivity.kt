package com.cleansense.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cleansense.app.ui.screens.DeviceListScreen
import com.cleansense.app.ui.screens.HomeScreen
import com.cleansense.app.ui.screens.LoginScreen
import com.cleansense.app.ui.screens.SensorScreen
import com.cleansense.app.ui.theme.CleanSenseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            CleanSenseTheme {
                CleanSenseApp(onExitApp = { finish() })
            }
        }
    }
}

@Composable
fun CleanSenseApp(
    onExitApp: () -> Unit,
    vm: MainViewModel = viewModel(),
) {
    val screen by vm.screen.collectAsStateWithLifecycle()

    Crossfade(targetState = screen, animationSpec = tween(350), label = "screen") { current ->
        when (current) {
            Screen.LOGIN -> LoginScreen(
                onLogin = vm::login,
                onRegister = vm::register,
                onExit = onExitApp,
            )

            Screen.HOME -> {
                val message by vm.homeMessage.collectAsStateWithLifecycle()
                HomeScreen(
                    message = message,
                    isBluetoothSupported = vm.isBluetoothSupported,
                    isBluetoothEnabled = vm::isBluetoothEnabled,
                    onReady = vm::openDeviceList,
                    onExit = vm::logout,
                    allowWithoutBluetooth = vm.isDemoAvailable,
                )
            }

            Screen.DEVICES -> {
                val devices by vm.devices.collectAsStateWithLifecycle()
                val scanning by vm.isScanning.collectAsStateWithLifecycle()
                val connecting by vm.connectingAddress.collectAsStateWithLifecycle()
                val error by vm.deviceError.collectAsStateWithLifecycle()
                BackHandler { vm.closeDeviceList() }
                DeviceListScreen(
                    devices = devices,
                    isScanning = scanning,
                    connectingAddress = connecting,
                    error = error,
                    onDeviceClick = vm::connect,
                    onRescan = vm::rescan,
                    onExit = vm::closeDeviceList,
                )
            }

            Screen.SENSOR -> {
                val reading by vm.reading.collectAsStateWithLifecycle()
                val name by vm.connectedName.collectAsStateWithLifecycle()
                BackHandler { vm.disconnect() }
                SensorScreen(
                    deviceName = name,
                    reading = reading,
                    onExit = vm::disconnect,
                )
            }
        }
    }
}
