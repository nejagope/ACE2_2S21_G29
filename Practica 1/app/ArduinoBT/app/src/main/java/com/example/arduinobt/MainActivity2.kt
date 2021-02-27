package com.example.arduinobt

import android.content.Intent
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

class MainActivity2 : AppCompatActivity() {

    lateinit var bluetoothJhr: BluetoothJhr
    val boolean:Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val nombre :TextView = findViewById(R.id.tvt_nombre)
        val temp :TextView = findViewById(R.id.tvt_temp)
        val oxigeno :TextView = findViewById(R.id.tvt_hmd)
        val rpm :TextView = findViewById(R.id.tvt_pulso)
        val info : EditText = findViewById(R.id.edt_info)

        //nombre.text = GlobalValues.nombre + " " + GlobalValues.apellido
        var codigo = GlobalValues.codigo

        bluetoothJhr = BluetoothJhr(MainActivity::class.java,this)

        thread(start = true){
            var datos: String = ""

            while(boolean){
                Thread.sleep(1000)
                datos = bluetoothJhr.Rx()
                //datos = "12,15,28#"

                //this@MainActivity2.runOnUiThread(java.lang.Runnable {
                    if(datos.contains("#")){
                        var lineas = datos.split("#").toTypedArray()

                        for (linea in lineas){
                            var valores = linea.split(",").toTypedArray()

                            if(valores.size==3){
                                var t = valores[0]
                                var o = valores[1]
                                var r = valores[2]

                                //temp.text = t + " grados"
                                //oxigeno.text = o + " do"
                                //rpm.text = r + " ppm"

                                //info.setText(linea.toString())

                                this@MainActivity2.runOnUiThread(java.lang.Runnable {
                                    val url = "http://practica1arq2.azurewebsites.net/prueba.asmx/CargarInformacion"
                                    //Toast.makeText(this, codigo, Toast.LENGTH_LONG).show()
                                    requestWebService(url, codigo, t, o, r)
                                })
                            }
                        }
                        bluetoothJhr.ResetearRx()
                    }
                //})
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

    fun requestWebService(url:String, codigo:String, temperatura: String, oxigeno: String, rpm: String){
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