package com.cleansense.app.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/** Dispositivo mostrado en la lista. */
data class DeviceItem(
    val name: String,
    val address: String,
    val paired: Boolean,
    /** true solo para el sensor simulado del modo demo. */
    val isDemo: Boolean = false,
)

/**
 * Maneja todo lo relacionado con Bluetooth clásico:
 * buscar dispositivos cercanos, conectarse por SPP (puerto serie) a un HC-05
 * y leer las líneas de texto que envía el Arduino.
 */
class BluetoothController(context: Context) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        appContext.getSystemService(BluetoothManager::class.java)?.adapter

    private val _devices = MutableStateFlow<List<DeviceItem>>(emptyList())
    val devices: StateFlow<List<DeviceItem>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var pendingSocket: BluetoothSocket? = null
    private var receiverRegistered = false

    val isSupported: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = IntentCompat.getParcelableExtra(
                        intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
                    ) ?: return
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    addDevice(device, name)
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _isScanning.value = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _isScanning.value = false
            }
        }
    }

    // ---------------------------------------------------------------- Búsqueda

    /** Muestra los dispositivos ya vinculados y empieza a buscar dispositivos cercanos. */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = adapter ?: return
        registerReceiverIfNeeded()
        _devices.value = sort(pairedDevices())
        if (!hasScanPermission()) return
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        _isScanning.value = adapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (hasScanPermission()) adapter?.cancelDiscovery()
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    private fun pairedDevices(): List<DeviceItem> {
        if (!hasConnectPermission()) return emptyList()
        return adapter?.bondedDevices.orEmpty().map {
            DeviceItem(name = it.name ?: it.address, address = it.address, paired = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, nameFromIntent: String?) {
        val name = nameFromIntent
            ?: (if (hasConnectPermission()) device.name else null)
            ?: return // Se ignoran dispositivos sin nombre (el HC-05 siempre tiene nombre).
        val paired = hasConnectPermission() && device.bondState == BluetoothDevice.BOND_BONDED
        val item = DeviceItem(name = name, address = device.address, paired = paired)
        _devices.update { current ->
            if (current.any { it.address == item.address }) current else sort(current + item)
        }
    }

    /** Los módulos "HC-xx" primero, luego los vinculados, luego por nombre. */
    private fun sort(list: List<DeviceItem>) = list.sortedWith(
        compareByDescending<DeviceItem> { it.name.startsWith("HC", ignoreCase = true) }
            .thenByDescending { it.paired }
            .thenBy { it.name.lowercase() }
    )

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        // Son broadcasts del sistema Bluetooth, por eso el receptor es "exported".
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
    }

    // ---------------------------------------------------------------- Conexión

    /** Abre el puerto serie Bluetooth (SPP) con el dispositivo indicado. */
    @SuppressLint("MissingPermission")
    suspend fun connect(address: String): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = adapter
            ?: return@withContext Result.failure(IOException("Bluetooth no disponible"))
        if (!hasConnectPermission()) {
            return@withContext Result.failure(SecurityException("Falta el permiso BLUETOOTH_CONNECT"))
        }
        disconnect()
        // Buscar dispositivos hace muy lenta la conexión, así que se detiene antes.
        if (hasScanPermission()) adapter.cancelDiscovery()

        try {
            val device = adapter.getRemoteDevice(address)
            val primary = device.createRfcommSocketToServiceRecord(SPP_UUID)
            pendingSocket = primary
            val connected = try {
                primary.connect()
                primary
            } catch (first: IOException) {
                closeQuietly(primary)
                ensureActive()
                // Plan B usado por muchos HC-05: abrir directamente el canal RFCOMM 1.
                val fallback = try {
                    device.javaClass
                        .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                        .invoke(device, 1) as BluetoothSocket
                } catch (e: Exception) {
                    throw first
                }
                pendingSocket = fallback
                try {
                    fallback.connect()
                    fallback
                } catch (second: IOException) {
                    closeQuietly(fallback)
                    throw second
                }
            }
            if (!isActive) {
                closeQuietly(connected)
                throw CancellationException("Conexión cancelada")
            }
            socket = connected
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            pendingSocket = null
        }
    }

    /**
     * Entrega cada línea de texto que llega desde el Arduino.
     * El flujo termina cuando se pierde o se cierra la conexión.
     */
    fun incomingLines(): Flow<String> = flow {
        val input = socket?.inputStream ?: return@flow
        val reader = input.bufferedReader(Charsets.US_ASCII)
        while (true) {
            val line = try {
                reader.readLine()
            } catch (e: IOException) {
                null
            } ?: break
            val clean = line.trim()
            if (clean.isNotEmpty()) emit(clean)
        }
    }.flowOn(Dispatchers.IO)

    fun disconnect() {
        pendingSocket?.let(::closeQuietly)
        pendingSocket = null
        socket?.let(::closeQuietly)
        socket = null
    }

    fun release() {
        stopDiscovery()
        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistered = false
        }
        disconnect()
    }

    // ---------------------------------------------------------------- Permisos

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasConnectPermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || granted(Manifest.permission.BLUETOOTH_CONNECT)

    private fun hasScanPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) granted(Manifest.permission.BLUETOOTH_SCAN)
        else granted(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun closeQuietly(s: BluetoothSocket) {
        try {
            s.close()
        } catch (_: IOException) {
        }
    }

    companion object {
        /** UUID estándar del perfil puerto serie (SPP), el que usa el HC-05. */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Permisos que hay que pedir en tiempo de ejecución según la versión de Android. */
        fun requiredPermissions(): Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }
    }
}
