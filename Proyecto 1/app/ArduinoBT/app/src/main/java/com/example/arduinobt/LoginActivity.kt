package com.example.arduinobt

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin: Button = findViewById(R.id.btn_login)
        val edtUsu: EditText = findViewById(R.id.edt_usu)
        val edtPass: EditText = findViewById(R.id.edt_pass)


        btnLogin.setOnClickListener {
            val queque = Volley.newRequestQueue(this)
            val url = "http://practica1arq2.azurewebsites.net/prueba.asmx/ConsultaLogin"
            val stringRequest = object: StringRequest(Request.Method.POST, url, Response.Listener { response ->
                val res = JSONObject(response.substring(75,response.toString().length-9)).get("consultar_formularios").toString()

                val resultado = (JSONObject(res.toString()).get("resultado")).toString().toIntOrNull()

                if (resultado != 0){
                    Toast.makeText(this, "Datos incorrectos, por favor pruebe de nuevo", Toast.LENGTH_SHORT).show()
                }else{
                    val informacion = (JSONObject(res.toString()).get("informacion")).toString()
                    val codigo = (JSONObject(informacion.toString()).get("codigo")).toString()

                    GlobalValues.codigo = codigo
                    val intent:Intent = Intent(this, MainActivity2::class.java)
                    startActivity(intent)
                }

            }, Response.ErrorListener { error ->
                Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
            }) {
                override fun getParams(): MutableMap<String, String> {
                    val parameters: MutableMap<String, String> = HashMap()
                    parameters.put("codigo",edtUsu.text.toString());
                    parameters.put("pwd",edtPass.text.toString());
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

}