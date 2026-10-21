package com.ortz.gestionequipaje

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics

class DiagnosticoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostico)

        val btnErrorNoFatal = findViewById<Button>(R.id.btnErrorNoFatal)
        val btnCrashPrueba = findViewById<Button>(R.id.btnCrashPrueba)
        val btnVolver = findViewById<Button>(R.id.btnVolverDiagnostico)

        val esDiagnostico = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!esDiagnostico) {
            btnCrashPrueba.visibility = View.GONE
        }

        btnErrorNoFatal.setOnClickListener {
            try {
                "abc".toInt()
            } catch (e: NumberFormatException) {
                val crashlytics = FirebaseCrashlytics.getInstance()
                crashlytics.setCustomKey("tipo_entidad", "destinatario")
                crashlytics.log("Prueba de error no fatal desde Diagnostico")
                crashlytics.recordException(e)
                Toast.makeText(this, "Error no fatal registrado, la app sigue funcionando", Toast.LENGTH_LONG).show()
            }
        }

        btnCrashPrueba.setOnClickListener {
            throw RuntimeException("Test Crash")
        }

        btnVolver.setOnClickListener { finish() }
    }
}