package com.example.arduinobt

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.*
import androidx.core.app.ActivityCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.hp.bluetoothjhr.BluetoothJhr
import com.google.android.gms.location.*
import org.json.JSONObject
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.system.exitProcess

class MainActivity2 : AppCompatActivity() {

    lateinit var bluetoothJhr: BluetoothJhr
    lateinit var btnStart: Button
    lateinit var btnFinish: Button
    lateinit var btnSalir: Button
    lateinit var txtNombreApellido: TextView
    lateinit var txtReloj: TextView
    lateinit var txtEstado: TextView
    lateinit var txtPeso: EditText

    lateinit var peso: String
    var inicio: Boolean = false
    var segundos: Int = 0
    var minutos: Int = 0
    var segT: String = "00"
    var minT: String = "00"
    var cont10seg: Int = 0

    var temperatura: Double = 0.0
    var oxigeno: Double = 0.0
    var revoluciones: Double = 0.0
    var contTemperatura: Int = 0
    var contOxigeno: Int = 0
    var contRevoluciones: Int = 0

    val boolean: Boolean = true
    val urlBase: String = "http://practica1arq2.azurewebsites.net/prueba.asmx/"
    var codigoIntento: String = "-1"
    var codigoUsuario: String = GlobalValues.codigo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        btnStart = findViewById(R.id.btn_start)
        btnFinish = findViewById(R.id.btn_finish)
        btnSalir = findViewById(R.id.btn_Salir)
        txtNombreApellido = findViewById(R.id.tvt_nombreApellido)
        txtReloj = findViewById(R.id.tvt_reloj)
        txtEstado = findViewById(R.id.tvt_nombre)
        txtPeso = findViewById(R.id.txt_pesoC)
        bluetoothJhr = BluetoothJhr(MainActivity::class.java,this)

        btnSalir.setOnClickListener {
            finish()
            exitProcess(0)
        }

        btnStart.setOnClickListener {
            inicio = true

            val url = urlBase + "IniciarIntentoCad"
            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                requestWebServiceStart(url, codigoUsuario)
            })

            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                txtEstado.text = "Enviando Datos..."
                peso = txtPeso.text.toString()
            })

            btnStart.isEnabled = false
            btnFinish.isEnabled = true
        }

        btnFinish.setOnClickListener {
            finalizar()
        }


        thread(start = true) {
            var datos: String = ""

            mostrarNombreUsuario()

            while (boolean) {
                Thread.sleep(1000)
                //datos = "80.3,79.56,96.8#"

                if (inicio) {
                    datos = bluetoothJhr.Rx()
                    cont10seg++

                    segundos++
                    controlarReloj()

                    if (cont10seg == 10) {
                        if (datos.contains("#")) {
                            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                                separarDatos(datos)
                            })
                        }
                        cont10seg = 0
                        bluetoothJhr.ResetearRx()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        bluetoothJhr.ConectaBluetooth()
        bluetoothJhr.ResetearRx()
    }

    override fun onPause(){
        super.onPause()

        bluetoothJhr.CierraConexion()
    }

    fun finalizar(){
        inicio = false
        segundos = 0
        minutos = 0
        segT = "00"
        minT = "00"

        cont10seg = 0

        val url = urlBase + "DetenerIntentoCad"
        this@MainActivity2.runOnUiThread(java.lang.Runnable {
            requestWebServiceFinish(url, codigoUsuario)
        })

        codigoIntento = "-1"

        this@MainActivity2.runOnUiThread(java.lang.Runnable {
            txtEstado.text = "Esperando..."
            txtReloj.text = "00:00"

            btnStart.isEnabled = true
            btnFinish.isEnabled = false
        })
    }

    fun mostrarNombreUsuario() {
        this@MainActivity2.runOnUiThread(java.lang.Runnable {
            if (GlobalValues.nombre != "-1") {
                txtNombreApellido.text = GlobalValues.nombre + " " + GlobalValues.apellido
            }
        })
    }

    fun controlarReloj() {
        if (segundos < 10) {
            segT = "0" + segundos
        } else {
            segT = segundos.toString()
        }

        if (segundos == 60) {
            segT = "00"
            segundos = 0
            minutos++
        }

        this@MainActivity2.runOnUiThread(java.lang.Runnable {
            txtReloj.text = "0" + minutos + ":" + segT
        })
    }
    //#0,1,2#
    fun separarDatos(datos: String) {
        var lineas = datos.split("#").toTypedArray()

        for (linea in lineas) {
            var valores = linea.split(",").toTypedArray()

            if (valores.size == 3) {
                try{
                    temperatura += valores[0].toDouble()
                    oxigeno += valores[1].toDouble()
                    revoluciones += valores[2].toDouble()

                    contTemperatura++
                    contOxigeno++
                    contRevoluciones++
                }catch(e: Exception){}
            }
        }

        try{
            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                val url = urlBase + "CargarInformacion"

                var t =  String.format("%.0f", temperatura / contTemperatura)
                var o =  String.format("%.0f", oxigeno / contOxigeno)
                var r =  String.format("%.0f", revoluciones / contRevoluciones)

                requestWebService(url, codigoUsuario, t,o,r);
                //Toast.makeText(this, t + ", " + o + ", " + r, Toast.LENGTH_LONG).show()
            })
        }catch(e: Exception){}
    }

    fun requestWebService(url:String, codigo:String, temp: String, oxi: String, rpm: String){
        val queque = Volley.newRequestQueue(this)
        val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
            //Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()
        }, Response.ErrorListener { error ->
            //Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): MutableMap<String, String> {
                val parameters: MutableMap<String, String> = HashMap()
                parameters.put("codigo",codigo);
                parameters.put("temperatura",temp);
                parameters.put("oxigeno",oxi);
                parameters.put("rpm",rpm);
                parameters.put("usuario","e");
                parameters.put("velocidad","0");
                parameters.put("distancia","0");
                parameters.put("codigoIntento","0");
                parameters.put("repeticion","0");
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
            //Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Inicado", Toast.LENGTH_SHORT).show()

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

    fun requestWebServiceFinish(url:String, codigo:String){
        val queque = Volley.newRequestQueue(this)
        val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
            //Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Finalizado", Toast.LENGTH_SHORT).show()

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