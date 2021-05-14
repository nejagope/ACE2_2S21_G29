#include <Wire.h>  // Wire library - used for I2C communication
#include <SoftwareSerial.h>
SoftwareSerial arduino1(6, 7); // (Rx,Tx) ---------------------------------pin ditital 3 y 4

int ADXL345 = 0x53; // The ADXL345 sensor I2C address
float X_out, Y_out, z_out;  // Outputs

#define offsetX   6.5//8.5       // OFFSET values
#define offsetY   -7//-4
#define offsetZ   8//-4

#define gainX     259.5//0.87     // GAIN factors
#define gainY     264//0.41
#define gainZ     250//0.41


float threshold = 0.45; // redefinir el umbral para cambiar
float xval[100] = {0};
float yval[100] = {0};
float zval[100] = {0};
float xavg, yavg, zavg;
int steps, flag = 0;

void setup() {
  Serial.begin(57600); // Initiate serial communication for printing the results on the Serial monitor
  arduino1.begin(115200);
  Wire.begin(); // Initiate the Wire library
  // Set ADXL345 in measuring mode
  Wire.beginTransmission(ADXL345); // Start communicating with the device 
  Wire.write(0x2D); // Access/ talk to POWER_CTL Register - 0x2D
  // Enable measurement
  Wire.write(8); // (8dec -> 0000 1000 binary) Bit D3 High for measuring enable 
  Wire.endTransmission();
  calibrate();
}

void loop()
{
  int acc = 0;
  float totvect[100] = {0};
  float totave[100] = {0};
  float xaccl[100] = {0};
  float yaccl[100] = {0};
  float zaccl[100] = {0};
  for (int a = 0; a < 100; a++)
  {
    xaccl[a] = aceleracionX();
    ///*
    Serial.print("aceleracion en x: ");
    Serial.println(xaccl[a]);
    delay(1);
    //*/
    yaccl[a] = aceleracionY();
    ///*
    Serial.print("aceleracion en y: ");
    Serial.println(yaccl[a]);
    delay(1);
    //*/
    zaccl[a] = aceleracionZ() / -1;
    ///*
    Serial.print("aceleracion en z: ");
    Serial.println(zaccl[a]);
    delay(1);
    //*/
    totvect[a] = sqrt(((xaccl[a] - xavg) * (xaccl[a] - xavg)) + ((yaccl[a] - yavg) * (yaccl[a] - yavg)) + ((zval[a] - zavg) * (zval[a] - zavg)));
    totave[a] = (totvect[a] + totvect[a - 1]) / 2 ;
    ///*
    Serial.println("totave[a]");
    Serial.println(totave[a]);
    //delay(100);
    //*/
    if (totave[a] > threshold && flag == 0)
    {
      steps = steps + 1;
      flag = 1;
    }
    if (totave[a] < threshold   && flag == 1)
    {
      flag = 0;
    }
    Serial.print("\nsteps: ");
    Serial.println(steps);
    arduino1.println(steps);
    delay(500);
  }
}
void calibrate()
{
  float sum = 0;
  float sum1 = 0;
  float sum2 = 0;
  for (int i = 0; i < 100; i++) {
    xval[i] = aceleracionX();
    sum = xval[i] + sum;
  }
  delay(100);
  xavg = sum / 100.0;
  for (int j = 0; j < 100; j++)
  {
    yval[j] = aceleracionY();
    sum1 = yval[j] + sum1;
  }
  yavg = sum1 / 100.0;
  delay(100);
  for (int q = 0; q < 100; q++)
  {
    zval[q] = aceleracionZ();
    sum2 = zval[q] + sum2;
  }
  zavg = sum2 / 100.0;
  delay(100);
}

float aceleracionX()
{
    Wire.beginTransmission(ADXL345);
    Wire.write(0x32); // Start with register 0x32 (ACCEL_XOUT_H)
    Wire.endTransmission(false);
    Wire.requestFrom(ADXL345, 6, true); // Read 6 registers total, each axis value is stored in 2 registers
    X_out = ( Wire.read()| Wire.read() << 8); // X-axis value
    Y_out = ( Wire.read()| Wire.read() << 8); // Y-axis value
    z_out = ( Wire.read()| Wire.read() << 8); // Z-axis value    
    return (X_out - offsetX)/gainX ;//*1000;//-0.01;
}
float aceleracionY()
{
    Wire.beginTransmission(ADXL345);
    Wire.write(0x32); // Start with register 0x32 (ACCEL_XOUT_H)
    Wire.endTransmission(false);
    Wire.requestFrom(ADXL345, 6, true); // Read 6 registers total, each axis value is stored in 2 registers
    X_out = ( Wire.read()| Wire.read() << 8); // X-axis value
    Y_out = ( Wire.read()| Wire.read() << 8); // Y-axis value
    z_out = ( Wire.read()| Wire.read() << 8); // Z-axis value
    return (Y_out - offsetY)/gainY ;//*1000;//+ 0.04;
}
float aceleracionZ()
{
    Wire.beginTransmission(ADXL345);
    Wire.write(0x32); // Start with register 0x32 (ACCEL_XOUT_H)
    Wire.endTransmission(false);
    Wire.requestFrom(ADXL345, 6, true); // Read 6 registers total, each axis value is stored in 2 registers
    X_out = ( Wire.read()| Wire.read() << 8); // X-axis value
    Y_out = ( Wire.read()| Wire.read() << 8); // Y-axis value
    z_out = ( Wire.read()| Wire.read() << 8); // Z-axis value
    return (z_out - offsetZ)/gainZ ;//*1000;//+ 0.04;
}
