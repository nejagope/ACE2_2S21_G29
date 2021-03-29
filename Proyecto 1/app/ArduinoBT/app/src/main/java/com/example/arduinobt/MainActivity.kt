package com.example.arduinobt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import com.example.hp.bluetoothjhr.BluetoothJhr

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val lista_dispositivos : ListView = findViewById(R.id.lista_dispositivos)

        val bluetoothJhr = BluetoothJhr(this, lista_dispositivos)
        bluetoothJhr.EncenderBluetooth()

        lista_dispositivos.setOnItemClickListener{adapterView, view, i, l ->
            bluetoothJhr.Disp_Seleccionado(view, i, MainActivity2::class.java)
        }

        //Toast.makeText(this, "Bienvenido", Toast.LENGTH_LONG).show()
    }
}