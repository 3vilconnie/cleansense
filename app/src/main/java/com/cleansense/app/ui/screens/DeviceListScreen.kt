package com.cleansense.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleansense.app.bluetooth.DeviceItem
import com.cleansense.app.ui.components.CleanSenseLayout
import com.cleansense.app.ui.theme.CsError
import com.cleansense.app.ui.theme.CsMint
import com.cleansense.app.ui.theme.CsSurface
import com.cleansense.app.ui.theme.CsSurfaceDivider
import com.cleansense.app.ui.theme.CsTextPrimary
import com.cleansense.app.ui.theme.CsTextSecondary

/** Pantalla "Menú de dispositivos": lista de dispositivos Bluetooth cercanos. */
@Composable
fun DeviceListScreen(
    devices: List<DeviceItem>,
    isScanning: Boolean,
    connectingAddress: String?,
    error: String?,
    onDeviceClick: (DeviceItem) -> Unit,
    onRescan: () -> Unit,
    onExit: () -> Unit,
) {
    CleanSenseLayout(onExit = onExit) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Selecciona un dispositivo para conectar.",
                color = CsTextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                color = CsSurface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (devices.isEmpty()) {
                    Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isScanning) "Buscando dispositivos cercanos…"
                            else "No se encontraron dispositivos.",
                            color = CsTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 380.dp)) {
                        itemsIndexed(devices, key = { _, d -> d.address }) { index, device ->
                            if (index > 0) HorizontalDivider(color = CsSurfaceDivider)
                            DeviceRow(
                                device = device,
                                isConnecting = connectingAddress == device.address,
                                enabled = connectingAddress == null,
                                onClick = { onDeviceClick(device) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = CsMint,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Buscando…", color = CsTextSecondary, fontSize = 12.sp)
                }
            } else {
                TextButton(onClick = onRescan, enabled = connectingAddress == null) {
                    Text("Buscar de nuevo", color = CsMint, fontSize = 13.sp)
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = CsError, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceItem,
    isConnecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(device.name, color = CsTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(
                text = when {
                    isConnecting -> "Conectando…"
                    device.isDemo -> "Sensor simulado · solo para pruebas"
                    device.paired -> "Dispositivo Bluetooth · vinculado"
                    else -> "Dispositivo Bluetooth"
                },
                color = if (isConnecting) CsMint else CsTextSecondary,
                fontSize = 12.sp
            )
        }
        if (isConnecting) {
            CircularProgressIndicator(
                color = CsMint,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
