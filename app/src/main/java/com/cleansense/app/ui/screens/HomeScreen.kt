package com.cleansense.app.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cleansense.app.bluetooth.BluetoothController
import com.cleansense.app.ui.components.CleanSenseLayout
import com.cleansense.app.ui.components.PrimaryButton
import com.cleansense.app.ui.theme.CsError
import com.cleansense.app.ui.theme.CsTextSecondary

/**
 * Pantalla 02 · Aplicación desconectada de sensor.
 * Al tocar "Conectar dispositivo" se piden permisos, se pide activar el
 * Bluetooth si está apagado y luego se abre la lista de dispositivos.
 */
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(
    message: String?,
    isBluetoothSupported: Boolean,
    isBluetoothEnabled: () -> Boolean,
    onReady: () -> Unit,
    onExit: () -> Unit,
    /** En debug (modo demo) se abre la lista aunque no haya Bluetooth o permisos. */
    allowWithoutBluetooth: Boolean = false,
) {
    val context = LocalContext.current
    var localMessage by remember { mutableStateOf<String?>(null) }

    val enableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isBluetoothEnabled() || allowWithoutBluetooth) onReady()
        else localMessage = "Activa el Bluetooth para continuar."
    }

    val continueFlow: () -> Unit = {
        when {
            !isBluetoothSupported && allowWithoutBluetooth -> onReady()
            !isBluetoothSupported -> localMessage = "Este teléfono no tiene Bluetooth."
            !isBluetoothEnabled() ->
                enableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> onReady()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            continueFlow()
        } else if (allowWithoutBluetooth) {
            onReady()
        } else {
            localMessage = "Se necesitan los permisos de Bluetooth para buscar dispositivos."
        }
    }

    CleanSenseLayout(onExit = onExit) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                text = "Conectar dispositivo",
                onClick = {
                    localMessage = null
                    val permissions = BluetoothController.requiredPermissions()
                    val allGranted = permissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) continueFlow() else permissionLauncher.launch(permissions)
                }
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Asegúrate de tener Bluetooth activado.",
                color = CsTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            (localMessage ?: message)?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = CsError, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
