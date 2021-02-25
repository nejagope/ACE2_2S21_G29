#define RXD2 16 //desfinimos el pin 16 como serial de entrada
#define TXD2 17 //desfinimos el pin 16 como serial de entrada

#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)           //verifica que este activado el bt
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif

void setup() {
  Serial.begin(115200); //Serial por default
  SerialBT.begin("Grupo29"); //Nombres del dispositivo BT
  Serial.println("The device started, now you can pair it with bluetooth!");
  Serial2.begin(9600, SERIAL_8N1, RXD2, TXD2); //iniciamos el nuevo puerto serial y setiamos parametros
}

void loop() {
  if(Serial2.available() && Serial2.read() != -1)
  {
    String lectura = Serial2.readString();
    for(int i = 0 ; i < lectura.length(); i++)
    {
      SerialBT.write(lectura[i]);
      Serial.write(lectura[i]);
      delay(50);
    }
  }

}
