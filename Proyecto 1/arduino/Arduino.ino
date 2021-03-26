#include <Wire.h>
#include <TinyGPS++.h>
#include "MAX30105.h"
#include "spo2_algorithm.h"
#include <SoftwareSerial.h>

MAX30105 particleSensor;
TinyGPSPlus gps;

SoftwareSerial wifi(13, 12); // (Rx,Tx) ---------------------------------pin ditital 13 y 12
SoftwareSerial gpsSerial(11,10); // (Rx,Tx) ---------------------------------pin ditital 11 y 10

#define MAX_BRIGHTNESS 255
#define BUZZER_PIN  8
#define PI 3.1415926535897932384626433832795

uint32_t irBuffer[100]; //infrared LED sensor data
uint32_t redBuffer[100]; //red LED sensor data

int32_t bufferLength; //data length
int32_t spo2; //SPO2 value
int8_t validSPO2; //indicator to show if the SPO2 calculation is valid
int32_t heartRate; //heart rate value
int8_t validHeartRate; //indicator to show if the heart rate calculation is valid
int outputpin= 0;//seleccionamos A0

int lectura_actual = 0;
float lectura_inicial[2];
float distancia = 0; 
int temperatura = 0;
unsigned long tiempo1 = 0;
unsigned long tiempo2 = 0;
unsigned long tiempo3 = 0;
unsigned long tiempoSegundos = 0;

//byte pulseLED = 11; //Must be on PWM pin
//byte readLED = 13; //Blinks with each data read

void setup()
{
  Serial.begin(57600); // initialize serial communication at 115200 bits per second:
  wifi.begin(115200);
  gpsSerial.begin(9600);
  // Initialize sensor
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) //Use default I2C port, 400kHz speed
  {
    Serial.println(F("MAX30105 was not found. Please check wiring/power. Need to restart"));
    while (1);
  }

  byte ledBrightness = 60; //Options: 0=Off to 255=50mA
  byte sampleAverage = 4; //Options: 1, 2, 4, 8, 16, 32
  byte ledMode = 2; //Options: 1 = Red only, 2 = Red + IR, 3 = Red + IR + Green
  byte sampleRate = 100; //Options: 50, 100, 200, 400, 800, 1000, 1600, 3200
  int pulseWidth = 411; //Options: 69, 118, 215, 411
  int adcRange = 4096; //Options: 2048, 4096, 8192, 16384

  particleSensor.setup(ledBrightness, sampleAverage, ledMode, sampleRate, pulseWidth, adcRange); //Configure sensor with these settings
  pinMode(BUZZER_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, HIGH);
}

void loop()
{
  if(wifi.available() && wifi.read() != -1)
  {
    String lectura = wifi.readString();
    Serial.print("start test");
    lectura_actual = 0;
    lectura_inicial[2];
    distancia = 0; 
    temperatura = 0;
    tiempo1 = 0;
    tiempo2 = 0;
    tiempo3 = 0;
    tiempoSegundos = 0;
    tiempo1 = millis();
    comenzar_lecturas();
  }
}

void comenzar_lecturas()
{
    bufferLength = 100; //buffer length of 100 stores 4 seconds of samples running at 25sps

  //read the first 100 samples, and determine the signal range
  for (byte i = 0 ; i < bufferLength ; i++)
  {
    while (particleSensor.available() == false) //do we have new data?
      particleSensor.check(); //Check the sensor for new data

    redBuffer [i] = particleSensor.getRed();
    irBuffer [i] = particleSensor.getIR();
    particleSensor.nextSample(); //We're finished with this sample so move to next sample
  }
  Serial.print("start to read");

  //calculate heart rate and SpO2 after first 100 samples (first 4 seconds of samples)
  maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);

  //Continuously taking samples from MAX30102. Heart rate and SpO2 are calculated every 1 second
  while (1)
  {
    //dumping the first 25 sets of samples in the memory and shift the last 75 sets of samples to the top
    for (byte i = 25; i < 100; i++)
    {
      redBuffer[i - 25] = redBuffer[i];
      irBuffer[i - 25] = irBuffer[i];
    }

    //take 25 sets of samples before calculating the heart rate.
    for (byte i = 75; i < 100; i++)
    {
      while (particleSensor.available() == false) //do we have new data?
        particleSensor.check(); //Check the sensor for new data

      redBuffer [i] = particleSensor. getRed ();
      irBuffer [i] = particleSensor. getIR ();
      particleSensor.nextSample(); //We're finished with this sample so move to next sample

      //send samples and calculation result to terminal program through UART
      if (validHeartRate && validSPO2)
      {
        float millivolts = (analogRead(outputpin)/1024.0) * 5000;
        tiempo2 = millis();
        temperatura = ((millivolts/10)- 32) * (5.0/9.0);        
        String salida = String(temperatura) + "," + String(spo2) + "," + String(heartRate) + ",";

        tiempo2 = millis();
        if(tiempo2 > tiempo3+1000)
        {
          tiempo3 = millis(); //Actualiza el tiempo actual
          if (gpsSerial.available() > 0)
            if (gps.encode(gpsSerial.read()))
              if(lectura_actual == 0)
              {
                get_values(lectura_inicial);
                lectura_actual = 1;  
              }else
              {
                float aux[2];
                get_values(aux);
                lectura_actual = 0;  
                float distancia1 = DistanciaKm(lectura_inicial[0], lectura_inicial[1], aux[0], aux[1]);
                distancia += distancia1;
                salida += String(distancia) + "," + String(tiempo2/1000) + ",0," + "#";
                Serial.print(salida);
                wifi.println(salida);
                if(heartRate > 100)
                {
                  inflar_membrana()
                  return;
                }
              }
        }
        
        if(tiempo2 > (tiempo1+60000)){  //Si ha pasado 1 minuto ejecuta el IF
            tiempo1 = millis(); //Actualiza el tiempo actual
            digitalWrite(BUZZER_PIN, LOW);
            delay(500);  
            digitalWrite(BUZZER_PIN, HIGH);
            salida += String(distancia) + "," + (tiempo2/1000) + ",1," + "#";
            Serial.print(salida);
            wifi.println(salida);
            if(heartRate > 100)
            {
              inflar_membrana()
              return;
            }
        }
      }
    }
    
    //After gathering 25 new samples recalculate HR and SP02
    maxim_heart_rate_and_oxygen_saturation(irBuffer, bufferLength, redBuffer, &spo2, &validSPO2, &heartRate, &validHeartRate);
  }
}

void inflar_membrana()
{
  //codigo membrana
}

void get_values(float arr[])
{
  if (gps.location.isValid())
  {
    float latitud = gps.location.lat(), longitud = gps.location.lng(), altitud = gps.altitude.meters();
    Serial.print("Latitude: ");
    Serial.println(latitud, 6);
    Serial.print("Longitude: ");
    Serial.println(longitud, 6);
    Serial.print("Altitude: ");
    Serial.println(altitud);
    arr[0] = longitud;
    arr[1] = latitud;
  }
  else
  {
    Serial.println("Location: Not Available");
  }
}

float DistanciaKm(float posOrigenLa, float posDestinoLa , float posOrigenLo, float posDestinoLo)
{
  float difLatitud = EnRadianes(posDestinoLa - posOrigenLa);
  float difLongitud = EnRadianes(posDestinoLo - posOrigenLo);
  float a = pow(sin(difLatitud/2), 2)+cos(EnRadianes(posOrigenLa))*cos(EnRadianes(posDestinoLa))*pow(sin(difLongitud/2),2);

  float c = 2 * atan2(sqrt(a), sqrt(1 - a));
}

float EnRadianes(float valor)
{
  return (PI / 180) * valor;
}
