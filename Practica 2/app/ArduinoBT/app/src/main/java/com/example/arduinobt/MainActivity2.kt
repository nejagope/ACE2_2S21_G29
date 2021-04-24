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
    var ultimoAire: Double = 0.0

    var volAire: Double = 0.0
    var contAire: Int = 0
    var inhalar: Boolean = false

    var aireInhaladoAcumulado: Double = 0.0

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

            val url = urlBase + "IniciarIntentoVo2"
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
                /*datos = "0.78,0,100#213.14,0,200#213,0,300#41,0,400#1000,0,500#55220,0,600#55220,0,700#55220,0,800#55220,0,900#55220,0,901#55220,0,902#55220,0,903#"+
                        "55220,0,904#55220,0,905#55220,0,906#55220,0,907#55220,0,908#55220,0,909#55220,0,910#55220,0,911#1000,0,912#800,0,913#600,0,914#0,0,915#2,0,916#56,0,917#" +
                        "523,0,918#1000,0,919#26546,0,920#56465,0,921#5135,0,922#15465,0,923#453465,0,924#65465,0,925#4654123,0,926#564654,0,927#1000,0,928#546,0,929#54,0,930#0,0,931#"*/

                if (inicio) {
                    datos = bluetoothJhr.Rx()
                    cont10seg++

                    segundos++
                    controlarReloj()

                    if(minutos==5){
                        var vo2max = calcVO2MAx()
                        this@MainActivity2.runOnUiThread(java.lang.Runnable {
                            val url = urlBase + "CargarInformacionVo2"
                            var oxi = String.format("%.0f", vo2max);
                            Toast.makeText(this, "F " + oxi, Toast.LENGTH_SHORT).show()
                            requestWebService(url, codigoUsuario, "F", oxi, codigoIntento, "5")
                        })

                        Thread.sleep(500)
                        finalizar()

                    }else{
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
        ultimoAire = 0.0

        volAire = 0.0
        contAire = 0
        inhalar = false

        aireInhaladoAcumulado = 0.0

        val url = urlBase + "DetenerIntentoVo2"
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

        if(minutos == 5){
            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                txtReloj.text = "00:00"
            })
        }else{
            this@MainActivity2.runOnUiThread(java.lang.Runnable {
                txtReloj.text = "0" + minutos + ":" + segT
            })
        }
    }

    fun separarDatos(datos: String) {
        var lineas = datos.split("#").toTypedArray()
        var aireActual = 0.0
        var aireFinal = 0.0
        var init = 0
        var ultimoValor: Double? = 0.0

        for (linea in lineas) {
            var valores = linea.split(",").toTypedArray()

            if (valores.size == 3) {
                if(valores[0].toDoubleOrNull()!=null){
                    if (valores[0].toDouble() > 1000.00) {
                        aireActual = valores[2].toDouble()

                        if (init==0) {
                            aireFinal = aireActual
                        } else {
                            aireFinal = aireActual - ultimoAire
                        }

                        acumularVolumenAire(aireFinal)
                    }else if(valores[0].toDouble() <= 1000.00 && ultimoValor.toString().toDouble()>1000){
                        var totalAire = calculoVolumenInhaladoExalado()

                        var inhala = "I"
                        if (inhalar) {
                            aireInhaladoAcumulado += totalAire
                            inhala = "I"
                        }else{
                            totalAire *= -1
                            inhala = "E"
                        }

                        inhalar = !inhalar

                        this@MainActivity2.runOnUiThread(java.lang.Runnable {
                            val url = urlBase + "CargarInformacionVo2"
                            var aire = String.format("%.0f", totalAire);
                            //Toast.makeText(this, inhala + " " + aire, Toast.LENGTH_SHORT).show()
                            //Toast.makeText(this, datos, Toast.LENGTH_SHORT).show()
                            requestWebService(url, codigoUsuario, inhala, aire, codigoIntento, (minutos+1).toString())
                        })

                        volAire = 0.0
                        contAire = 0
                    }
                }
            }

            init++
            ultimoAire = aireActual

            if(valores[0].toDoubleOrNull()!=null){
                ultimoValor = valores[0].toDoubleOrNull()
            }
        }
    }

    fun calcVO2MAx():Double{
        aireInhaladoAcumulado /= 1000
        var oxigeno = aireInhaladoAcumulado * 210
        var vo2max = oxigeno/5

        if(peso.toDoubleOrNull()!=null){
            vo2max /= peso.toDouble()
        }

        return vo2max
    }

    fun acumularVolumenAire(aireFinal: Double){
        volAire += aireFinal
        contAire++
    }

    fun calculoVolumenInhaladoExalado():Double{
        var totalAire =  String.format("%.2f", volAire / contAire).toDouble()
        var aireLt = String.format("%.2f", totalAire / 1000).toDouble()

        return totalAire
    }

    fun requestWebService(url:String, codigo:String, tipo: String, valor02: String, codIntento: String, minuto: String){
        val queque = Volley.newRequestQueue(this)
        val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
            Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()
        }, Response.ErrorListener { error ->
            Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
        }) {
            override fun getParams(): MutableMap<String, String> {
                val parameters: MutableMap<String, String> = HashMap()
                parameters.put("codigo",codigo);
                parameters.put("tipo",tipo);
                parameters.put("valor02",valor02);
                parameters.put("codigoIntento",codIntento);
                parameters.put("minuto",minuto);
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

    fun requestWebServiceFinish(url:String, codigo:String){
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