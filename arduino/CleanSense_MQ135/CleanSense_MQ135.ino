/*
  CleanSense - Arduino UNO/Nano + MQ-135 + HC-05

  Envía por Bluetooth una línea de texto con la lectura del MQ-135 (0 a 1023)
  cada 500 ms, por ejemplo:
      312
      315
      402
  La app de Android interpreta esas lecturas (aire normal, COV, volviendo a la normalidad).

  CONEXIONES
  MQ-135:  VCC -> 5V   |  GND -> GND  |  A0 -> A0
  HC-05:   VCC -> 5V   |  GND -> GND
           TXD -> pin 10 del Arduino
           RXD <- pin 11 del Arduino A TRAVÉS de un divisor de voltaje
                  (pin 11 -> resistencia 1k -> RXD, y RXD -> resistencia 2k -> GND)
                  porque el RX del HC-05 trabaja a 3.3 V.

  El HC-05 viene de fábrica a 9600 baudios y con PIN 1234 (a veces 0000).
  El MQ-135 necesita calentarse: deja el circuito encendido 1-2 minutos antes de
  conectar la app (la primera vez, idealmente varias horas).
*/

#include <SoftwareSerial.h>

const uint8_t PIN_BT_RX = 10;   // Arduino recibe  <- TXD del HC-05
const uint8_t PIN_BT_TX = 11;   // Arduino envía   -> RXD del HC-05 (con divisor)
const uint8_t PIN_MQ135 = A0;

const unsigned long INTERVALO_MS = 500;  // cada cuánto se envía una lectura
const uint8_t MUESTRAS = 10;             // lecturas promediadas por envío

SoftwareSerial bluetooth(PIN_BT_RX, PIN_BT_TX);
unsigned long ultimoEnvio = 0;

int leerMQ135() {
  long suma = 0;
  for (uint8_t i = 0; i < MUESTRAS; i++) {
    suma += analogRead(PIN_MQ135);
    delay(5);
  }
  return (int)(suma / MUESTRAS);
}

void setup() {
  Serial.begin(9600);      // Monitor serie del PC (para depurar)
  bluetooth.begin(9600);   // Velocidad por defecto del HC-05
  pinMode(PIN_MQ135, INPUT);
  Serial.println(F("CleanSense listo. Enviando lecturas del MQ-135..."));
}

void loop() {
  unsigned long ahora = millis();
  if (ahora - ultimoEnvio >= INTERVALO_MS) {
    ultimoEnvio = ahora;
    int valor = leerMQ135();
    bluetooth.println(valor);   // -> app Android
    Serial.println(valor);      // -> monitor serie
  }
}
