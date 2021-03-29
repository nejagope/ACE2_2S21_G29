package com.example.arduinobt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.hp.bluetoothjhr.BluetoothJhr
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MainActivity2 : AppCompatActivity() {

    //lateinit var bluetoothJhr: BluetoothJhr
    lateinit var btnStart: Button
    lateinit var btnFinish: Button
    lateinit var btnSalir: Button

    val boolean:Boolean = true
    val urlBase: String = "http://practica1arq2.azurewebsites.net/prueba.asmx/"
    var codigoIntento: String = "-1"
    var repeticion: Int = 1
    var contador: Int = 0

    var tiempoPrueba: Double = 1.0
    var ultimoTiempo: Double = 0.0
    var contador10Seg: Int = 0

    var temperatura: Double = 0.0
    var oxigeno: Double = 0.0
    var revoluciones: Double = 0.0
    var velocidad: Double = 0.0
    var distancia: Double = 0.0
    var contTemperatura: Int = 0
    var contOxigeno: Int = 0
    var contRevoluciones: Int = 0
    var contVelocidad: Int = 0
    var contDistancia: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        var codigoUsuario = GlobalValues.codigo
        btnStart = findViewById(R.id.btn_start)
        btnFinish = findViewById(R.id.btn_finish)
        btnSalir = findViewById(R.id.btn_Salir)
        //bluetoothJhr = BluetoothJhr(MainActivity::class.java,this)

        btnSalir.setOnClickListener{
            finish()
            exitProcess(0)
        }

        btnStart.setOnClickListener{
            val url = urlBase + "IniciarIntento"
            requestWebServiceStart(url, codigoUsuario)
            contador10Seg = 0

            val thread = Thread(Runnable {
                this@MainActivity2.runOnUiThread(java.lang.Runnable {
                    var cont = 0
                    while(cont < 10) {
                        Thread.sleep(100)
                        Toast.makeText(this, "1", Toast.LENGTH_SHORT).show()
                        cont++
                    }
                })
            })

            thread.start()

            btnStart.isEnabled = false
            btnFinish.isEnabled = true
        }

        btnFinish.setOnClickListener{
            val url = urlBase + "DetenerIntento"
            requestWebServiceFinish(url, codigoUsuario, "R")

            val thread = Thread(Runnable {
                this@MainActivity2.runOnUiThread(java.lang.Runnable {
                    var cont = 0
                    while(cont < 10) {
                        Thread.sleep(100)
                        Toast.makeText(this, "0", Toast.LENGTH_SHORT).show()
                        cont++
                    }
                })
            })

            thread.start()

            codigoIntento = "-1"
            repeticion = 1
            contador = 0

            btnStart.isEnabled = true
            btnFinish.isEnabled = false
        }

        thread(start = true){
            var datos: String = ""

            while(boolean){
                Thread.sleep(1000)
                contador10Seg++
                //datos = bluetoothJhr.Rx()
                datos = "12,15,28,0.94,"+ (tiempoPrueba++) + ",1#"

                if(datos.contains("#")){
                    var lineas = datos.split("#").toTypedArray()

                    for (linea in lineas){
                        var valores = linea.split(",").toTypedArray()

                        if(valores.size==6){
                            var tiempoActual = valores[4]
                            var tiempoFinal = tiempoActual.toDouble() - ultimoTiempo

                            temperatura += valores[0].toDouble()
                            oxigeno += valores[1].toDouble()
                            revoluciones += valores[2].toDouble()
                            distancia += valores[3].toDouble()
                            velocidad += calcularVelocidad(valores[3].toDouble(), tiempoFinal)

                            contTemperatura++
                            contOxigeno++
                            contRevoluciones++
                            contDistancia++
                            contVelocidad++

                            var finalizacion = valores[5]

                            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                                if(contador10Seg==10){
                                    val url = urlBase + "CargarInformacion"

                                    var t = temperatura / contTemperatura
                                    var o = oxigeno / contOxigeno
                                    var r = revoluciones / contRevoluciones
                                    var v = velocidad / contVelocidad

                                    if(codigoIntento != "-1") {
                                        /*requestWebService(url, codigoUsuario, t.toString(), o.toString(), r.toString(), "0","0","0","0")
                                        //Toast.makeText(this, ""+codigoUsuario+" "+t+" "+o+" "+r+" E 0 0 0 0" , Toast.LENGTH_SHORT).show()
                                    }else{*/
                                        //Toast.makeText(this, ""+codigoUsuario+" "+t+" "+o+" "+r+" E "+v+" "+distancia+" "+codigoIntento+" "+repeticion, Toast.LENGTH_SHORT).show()
                                        requestWebService(url, codigoUsuario, t.toString(), o.toString(), r.toString(), v.toString() ,distancia.toString(), codigoIntento, repeticion.toString())

                                        if(finalizacion == "1"){
                                            repeticion++
                                        }

                                        //Ha completado satisfactoriamente 1 intento
                                        if(repeticion == 20){
                                            requestWebServiceFinish(urlBase + "DetenerIntento", codigoUsuario, "E")

                                            codigoIntento = "-1"
                                            repeticion = 1
                                            contador = 0

                                            requestWebServiceStart(urlBase + "IniciarIntento", codigoUsuario)

                                            repeticion = 0
                                        }
                                    }

                                    temperatura = 0.0
                                    oxigeno = 0.0
                                    revoluciones = 0.0
                                    velocidad = 0.0
                                    distancia = 0.0
                                    contTemperatura = 0
                                    contOxigeno = 0
                                    contRevoluciones = 0
                                    contVelocidad = 0
                                    contDistancia = 0

                                    contador10Seg = 0
                                }
                            })

                            ultimoTiempo = tiempoActual.toDouble()
                        }
                    }
                    //bluetoothJhr.ResetearRx()
                }
            }
        }
    }

    /*override fun onResume() {
        super.onResume()

        bluetoothJhr.ConectaBluetooth()
        bluetoothJhr.ResetearRx()
    }

    override fun onPause(){
        super.onPause()

        bluetoothJhr.CierraConexion()
    }*/

    /**
     * Función que calcula la velocidad en base a la distancia y el tiempo
     * @param dist Distancia en kilometros
     * @param t Tiempo en segundo
     * @return distancia en m/s
     * */
    fun calcularVelocidad(dist: Double, t: Double ): Double {
        return (dist*1000) / t
    }

    fun requestWebService(url:String, codigo:String, temperatura: String, oxigeno: String, rpm: String, velocidad: String, distancia: String, codIntento: String, repeticion: String){
        val queque = Volley.newRequestQueue(this)
        val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
            Toast.makeText(this, response.toString(), Toast.LENGTH_LONG).show()
        }, Response.ErrorListener { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show()
        }) {
            override fun getParams(): MutableMap<String, String> {
                val parameters: MutableMap<String, String> = HashMap()
                parameters.put("codigo",codigo);
                parameters.put("temperatura",temperatura);
                parameters.put("oxigeno",oxigeno);
                parameters.put("rpm",rpm);
                parameters.put("usuario","e");
                parameters.put("velocidad",velocidad);
                parameters.put("distancia",distancia);
                parameters.put("codigoIntento",codIntento);
                parameters.put("repeticion",repeticion);
                return parameters;
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers.put("Content-Type","application/x-www-form-urlencoded")
                return headers
            }
        }
        queque.add(stringRequest)
    }

    fun requestWebServiceStart(url:String, codigo:String){
        val queque = Volley.newRequestQueue(this)
        val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
            Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()

            val res = JSONObject(response.substring(75,response.toString().length-9)).get("consultar_formularios").toString()
            val resultadoStart = (JSONObject(res.toString()).get("resultado")).toString().toIntOrNull()

            if (resultadoStart != 0){
                Toast.makeText(this, "Error!!!, por favor pruebe de nuevo", Toast.LENGTH_SHORT).show()
            }else{
                codigoIntento = (JSONObject(res.toString()).get("codigo")).toString()
            }

        }, Response.ErrorListener { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): MutableMap<String, String> {
                val parameters: MutableMap<String, String> = HashMap()
                parameters.put("codigo",codigo);
                return parameters;
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers.put("Content-Type","application/x-www-form-urlencoded")
                return headers
            }
        }
        queque.add(stringRequest)
    }

    fun requestWebServiceFinish(url:String, codigo:String, estado:String){
        val queque = Volley.newRequestQueue(this)
        val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
            Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()

            val res = JSONObject(response.substring(75,response.toString().length-9)).get("consultar_formularios").toString()
            val resultadoStart = (JSONObject(res.toString()).get("resultado")).toString().toIntOrNull()

            if (resultadoStart != 0){
                Toast.makeText(this, "Error!!!, por favor pruebe de nuevo", Toast.LENGTH_SHORT).show()
            }else{
                codigoIntento = "-1"
            }

        }, Response.ErrorListener { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): MutableMap<String, String> {
                val parameters: MutableMap<String, String> = HashMap()
                parameters.put("codigo",codigo);
                parameters.put("estado", estado)
                return parameters;
            }

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers: MutableMap<String, String> = HashMap()
                headers.put("Content-Type","application/x-www-form-urlencoded")
                return headers
            }
        }
        queque.add(stringRequest)
    }

}