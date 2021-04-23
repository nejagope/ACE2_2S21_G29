#include <SoftwareSerial.h>
volatile int NumPulsos; //variable para la cantidad de pulsos recibidos
int PinSensor = 2;    //Sensor conectado en el pin 2
float factor_conversion=7.11; //para convertir de frecuencia a caudal
float volumen=0;
long dt=0; //variación de tiempo por cada bucle
long t0=0; //millis() del bucle anterior

SoftwareSerial wifi(3, 4); // (Rx,Tx) ---------------------------------pin ditital 3 y 4
//---Función que se ejecuta en interrupción---------------
void ContarPulsos ()  
{ 
  NumPulsos++;  //incrementamos la variable de pulsos
} 

//---Función para obtener frecuencia de los pulsos--------
int ObtenerFrecuecia() 
{
  int frecuencia;
  NumPulsos = 0;   //Ponemos a 0 el número de pulsos
  interrupts();    //Habilitamos las interrupciones
  delay(1000);   //muestra de 1 segundo
  noInterrupts(); //Deshabilitamos  las interrupciones
  frecuencia=NumPulsos; //Hz(pulsos por segundo)
  return frecuencia;
}

void setup() 
{ 
  wifi.begin(115200);
  Serial.begin(9600); 
  pinMode(PinSensor, INPUT); 
  attachInterrupt(0,ContarPulsos,RISING);//(Interrupción 0(Pin2),función,Flanco de subida)
  t0=millis();
} 

void loop ()    
{
  float frecuencia=ObtenerFrecuecia(); //obtenemos la frecuencia de los pulsos en Hz
  float caudal_L_m=frecuencia/factor_conversion; //calculamos el caudal en mL/m
  caudal_L_m = caudal_L_m*1000;
  dt=millis()-t0; //calculamos la variación de tiempo
  t0=millis();
  volumen=volumen+(caudal_L_m/60)*(dt/1000); // volumen(mL)=caudal(mL/s)*tiempo(s)
  float caudal_L_m_o=(caudal_L_m/60)*(dt/1000);
  caudal_L_m_o = (caudal_L_m_o*0.21/(dt/1000))*60;
   //-----Enviamos por el puerto serie---------------
  Serial.print ("Caudal oxigeno: "); 
  Serial.print (caudal_L_m_o,3); 
  Serial.print ("L/min\tCaudal aire: "); 
  Serial.print (caudal_L_m,3); 
  Serial.print ("L/min\tVolumen: "); 
  Serial.print (volumen,3); 
  Serial.print ("L"); 
  Serial.println ();
  String salida = String(caudal_L_m_o) + "," + String(caudal_L_m) + "," + String(volumen) + "#";
  wifi.println(salida);  
  Serial.println (salida);
}
