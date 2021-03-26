  
#define RXD2 16 //desfinimos el pin 16 como serial de entrada
#define TXD2 17 //desfinimos el pin 16 como serial de entrada

#include "BluetoothSerial.h"
BluetoothSerial SerialBT;

#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)           //verifica que este activado el bt
#error Bluetooth is not enabled! Please run `make menuconfig` to and enable it
#endif
boolean start_test = false;
void setup() {
  Serial.begin(9600); //Serial por default
  SerialBT.begin("Grupo29"); //Nombres del dispositivo BT
  Serial.println("The device started, now you can pair it with bluetooth!");
  Serial2.begin(115200, SERIAL_8N1, RXD2, TXD2); //iniciamos el nuevo puerto serial y setiamos parametros
}

void loop() {
  if(SerialBT.available() && !start_test)
  {
    //inicia prueba
    Serial2.println("start");
    Serial2.println("start");
    start_test = true;
    break;
  }else if(Serial2.available() && Serial2.read() != -1 && start_test)
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
