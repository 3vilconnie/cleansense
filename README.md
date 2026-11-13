# CleanSense

App de Android (Kotlin + Jetpack Compose) que se conecta por Bluetooth a un Arduino con
módulo **HC-05** y sensor **MQ-135**, y muestra en vivo si el aire es normal, si hay olores
(compuestos orgánicos volátiles, COV) o si está volviendo a niveles normales.

Los colores y pantallas siguen el diseño de Figma *CleanSense*.

## Pantallas

| Pantalla | Qué hace |
|---|---|
| 01 · Inicio de sesión | Iniciar sesión o **Registrarse** (crea una cuenta local en el teléfono). |
| 02 · Desconectada | Botón central **Conectar dispositivo**: pide permisos, enciende el Bluetooth y busca dispositivos. "Salir" cierra la sesión. |
| Menú de dispositivos | Lista de dispositivos vinculados y cercanos (los `HC-xx` salen primero). Toca uno para conectarte. |
| 03 · Sensor activo | Tarjeta que cambia de color y texto según el aire: **aire normal** (verde), **Mandar a lavar** (ocre, COV detectados) y **Volviendo a niveles base** (celeste). "Salir" desconecta. |

## Abrir en Android Studio

1. Android Studio → **File › Open** → elige la carpeta `CleanSense`.
2. Espera a que termine el *Gradle Sync* (descarga Gradle 8.11.1 y las librerías la primera vez).
   Si Android Studio ofrece actualizar el Android Gradle Plugin, puedes aceptar.
3. Conecta tu teléfono con **Depuración USB** activada y pulsa ▶ **Run**.

> El Bluetooth no funciona en el emulador: usa un teléfono real.

**Si el proyecto no sincroniza**, haz esto: crea un proyecto nuevo con la plantilla
**Empty Activity**, nombre `CleanSense` y paquete `com.cleansense.app`. Luego copia encima
la carpeta `app/src/main` de este proyecto y agrega a `app/build.gradle.kts` las dependencias
`lifecycle-runtime-compose` y `lifecycle-viewmodel-compose` (están en `gradle/libs.versions.toml`).

## Probar en el emulador (modo demo)

El emulador no tiene Bluetooth real, así que en las compilaciones **debug** (las que se
instalan con ▶ Run) la lista de dispositivos incluye **"CleanSense Demo"**, un sensor simulado.

1. Crea un emulador en **Device Manager** (por ejemplo Pixel 8, API 35) y pulsa ▶ **Run**.
2. Regístrate e inicia sesión.
3. Toca **Conectar dispositivo**. Si pide permisos o encender Bluetooth puedes aceptar o
   rechazar: en debug la lista se abre igual.
4. Toca **CleanSense Demo**. La pantalla recorre sola un ciclo de ~35 s:
   *Calibrando → aire normal → Mandar a lavar → Volviendo a niveles base → aire normal*.

El modo demo **no aparece** en la versión release (Build › Generate Signed App Bundle / APK).
Su ciclo se define en `sensor/DemoSensor.kt`.

## Estructura

```
app/src/main/java/com/cleansense/app/
├─ MainActivity.kt              Navegación entre pantallas
├─ MainViewModel.kt             Estado de la app (sesión, Bluetooth, lecturas)
├─ data/UserRepository.kt       Cuentas locales (contraseña con hash PBKDF2 + sal)
├─ bluetooth/BluetoothController.kt  Búsqueda, conexión SPP con el HC-05 y lectura
├─ sensor/AirQualityAnalyzer.kt Convierte lecturas del MQ-135 en estados de aire
├─ sensor/DemoSensor.kt         Sensor simulado para el emulador (solo debug)
└─ ui/
   ├─ theme/                    Colores del Figma
   ├─ components/Components.kt  Botones, campos, layout común
   └─ screens/                  Login, Inicio, Dispositivos, Sensor
arduino/CleanSense_MQ135/       Sketch para el Arduino
```

## Arduino

Sube `arduino/CleanSense_MQ135/CleanSense_MQ135.ino` con el IDE de Arduino.

| Componente | Pin | Arduino |
|---|---|---|
| MQ-135 | VCC / GND / A0 | 5V / GND / A0 |
| HC-05 | VCC / GND | 5V / GND |
| HC-05 | TXD | pin 10 |
| HC-05 | RXD | pin 11 **con divisor** (1 kΩ en serie y 2 kΩ a GND) |

El Arduino envía una línea por lectura (`312\n`) cada 500 ms a 9600 baudios.
La app acepta cualquier línea que contenga un número (`312`, `MQ:312`, `312.5`).

**Vincular el HC-05:** en el teléfono ve a Ajustes › Bluetooth, busca `HC-05` y vincúlalo
con el PIN `1234` (o `0000`). También puedes tocarlo directamente desde la lista de la app.

## Cómo decide el estado del aire

1. Al conectarse, la app toma 10 lecturas (≈5 s) para conocer el valor del **aire limpio**
   (línea base). Durante ese tiempo la tarjeta dice *Calibrando*: ten el sensor en aire limpio.
2. Después compara cada lectura (suavizada) con esa base:
   - sube **60** o más sobre la base → **Mandar a lavar** (COV / olores)
   - estando en COV, baja de **45** sobre la base → **Volviendo a niveles base**
   - baja de **20** sobre la base → **aire normal**

Cada MQ-135 es distinto. Si detecta olores con demasiada o muy poca facilidad, ajusta los
valores en `AirQualityAnalyzer` (`vocDelta`, `recoverDelta`, `normalDelta`). La pantalla del
sensor muestra abajo la lectura actual y la base para ayudarte a calibrar.

## Permisos

- Android 12 o superior: *Dispositivos cercanos* (`BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`).
- Android 11 o inferior: *Ubicación* (Android la exige para buscar Bluetooth) y la ubicación
  del teléfono debe estar activada para que aparezcan dispositivos nuevos.
