package com.cleansense.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleansense.app.bluetooth.BluetoothController
import com.cleansense.app.bluetooth.DeviceItem
import com.cleansense.app.data.UserRepository
import com.cleansense.app.sensor.AirQualityAnalyzer
import com.cleansense.app.sensor.AirReading
import com.cleansense.app.sensor.DemoSensor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { LOGIN, HOME, DEVICES, SENSOR }

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val users = UserRepository(app)
    private val bluetooth = BluetoothController(app)
    private val analyzer = AirQualityAnalyzer()

    /** Modo demo: sensor simulado para el emulador. Solo en compilaciones debug. */
    val isDemoAvailable: Boolean = BuildConfig.DEBUG

    private val _screen = MutableStateFlow(Screen.LOGIN)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _homeMessage = MutableStateFlow<String?>(null)
    val homeMessage: StateFlow<String?> = _homeMessage.asStateFlow()

    val devices: StateFlow<List<DeviceItem>> = bluetooth.devices
        .map { found -> withDemo(found) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, withDemo(emptyList()))

    private fun withDemo(found: List<DeviceItem>) =
        if (isDemoAvailable) listOf(DemoSensor.DEVICE) + found else found
    val isScanning: StateFlow<Boolean> = bluetooth.isScanning

    private val _connectingAddress = MutableStateFlow<String?>(null)
    val connectingAddress: StateFlow<String?> = _connectingAddress.asStateFlow()

    private val _deviceError = MutableStateFlow<String?>(null)
    val deviceError: StateFlow<String?> = _deviceError.asStateFlow()

    private val _connectedName = MutableStateFlow("")
    val connectedName: StateFlow<String> = _connectedName.asStateFlow()

    private val _reading = MutableStateFlow(analyzer.current)
    val reading: StateFlow<AirReading> = _reading.asStateFlow()

    private var connectionJob: Job? = null

    val isBluetoothSupported: Boolean get() = bluetooth.isSupported
    fun isBluetoothEnabled(): Boolean = bluetooth.isEnabled

    // ------------------------------------------------------------ Cuentas

    fun login(username: String, password: String): String? {
        val user = username.trim()
        return when {
            user.isEmpty() || password.isEmpty() -> "Ingresa tu usuario y contraseña."
            !users.userExists(user) -> "Ese usuario no existe. Toca \"Registrarse\" para crearlo."
            !users.checkPassword(user, password) -> "Contraseña incorrecta."
            else -> {
                onLoggedIn(user)
                null
            }
        }
    }

    fun register(username: String, password: String, confirm: String): String? {
        val user = username.trim()
        return when {
            user.length < 3 -> "El usuario debe tener al menos 3 caracteres."
            password.length < 4 -> "La contraseña debe tener al menos 4 caracteres."
            password != confirm -> "Las contraseñas no coinciden."
            users.userExists(user) -> "Ese usuario ya existe."
            else -> {
                users.createUser(user, password)
                onLoggedIn(user)
                null
            }
        }
    }

    private fun onLoggedIn(user: String) {
        _currentUser.value = user
        _homeMessage.value = null
        _screen.value = Screen.HOME
    }

    fun logout() {
        stopConnection()
        bluetooth.stopDiscovery()
        _currentUser.value = null
        _screen.value = Screen.LOGIN
    }

    // ------------------------------------------------------------ Bluetooth

    fun openDeviceList() {
        _homeMessage.value = null
        _deviceError.value = null
        _screen.value = Screen.DEVICES
        bluetooth.startDiscovery()
    }

    fun rescan() {
        _deviceError.value = null
        bluetooth.startDiscovery()
    }

    fun closeDeviceList() {
        bluetooth.stopDiscovery()
        stopConnection()
        _screen.value = Screen.HOME
    }

    fun connect(device: DeviceItem) {
        if (_connectingAddress.value != null) return
        bluetooth.stopDiscovery()
        _deviceError.value = null
        _connectingAddress.value = device.address

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            if (!device.isDemo) {
                val result = bluetooth.connect(device.address)
                if (result.isFailure) {
                    _connectingAddress.value = null
                    _deviceError.value = "No se pudo conectar con ${device.name}. " +
                        "Revisa que esté encendido y vinculado (PIN 1234 o 0000)."
                    return@launch
                }
            }
            _connectingAddress.value = null

            _connectedName.value = device.name
            analyzer.reset()
            _reading.value = analyzer.current
            _screen.value = Screen.SENSOR

            // El modo demo lee del sensor simulado en vez del HC-05.
            val lines = if (device.isDemo) DemoSensor.lines() else bluetooth.incomingLines()
            try {
                lines.collect { line ->
                    AirQualityAnalyzer.parse(line)?.let { value ->
                        _reading.value = analyzer.process(value)
                    }
                }
            } finally {
                bluetooth.disconnect()
            }

            // Si llegamos aquí sin que el usuario tocara "Salir", la conexión se cortó.
            if (_screen.value == Screen.SENSOR) {
                _homeMessage.value = "Se perdió la conexión con ${device.name}."
                _screen.value = Screen.HOME
            }
        }
    }

    /** Botón "Salir" en la pantalla del sensor. */
    fun disconnect() {
        _screen.value = Screen.HOME
        stopConnection()
    }

    private fun stopConnection() {
        connectionJob?.cancel()
        connectionJob = null
        bluetooth.disconnect()
        _connectingAddress.value = null
    }

    override fun onCleared() {
        bluetooth.release()
        super.onCleared()
    }
}
