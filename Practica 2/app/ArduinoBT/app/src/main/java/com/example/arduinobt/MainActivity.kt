package com.example.arduinobt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import com.example.hp.bluetoothjhr.BluetoothJhr
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btn_home: Button = findViewById(R.id.btn_home)
        val lista_dispositivos : ListView = findViewById(R.id.lista_dispositivos)

        val bluetoothJhr = BluetoothJhr(this, lista_dispositivos)
        bluetoothJhr.EncenderBluetooth()

        lista_dispositivos.setOnItemClickListener{adapterView, view, i, l ->
            bluetoothJhr.Disp_Seleccionado(view, i, MainActivity2::class.java)
        }

        btn_home.setOnClickListener{
            finish()
        }
    }
}