package com.ortz.gestionequipaje.Equipaje

import Model.TipoEquipaje
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore
import com.google.firebase.analytics.FirebaseAnalytics

class AgregarEquipaje : AppCompatActivity() {

    private var idEditar: String? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_equipaje)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val edNombreEquipaje = findViewById<EditText>(R.id.edNombreEquipaje)
        val edPesoEquipaje = findViewById<EditText>(R.id.edPesoEquipaje)
        val edTarifaEquipaje = findViewById<EditText>(R.id.edTarifaEquipaje)
        val btnGuardarEquipaje = findViewById<Button>(R.id.btnGuardarEquipaje)
        val btnVolverMenuEquipaje = findViewById<Button>(R.id.btnVolverMenuEquipaje)


        idEditar = intent.getStringExtra("idEquipaje")

        if (idEditar != null) {
            val equipaje = DataStore.obtenerTipoEquipaje(idEditar!!)
            equipaje?.let {
                edNombreEquipaje.setText(it.nombre)
                edPesoEquipaje.setText(it.pesoMaximo.toString())
                edTarifaEquipaje.setText(it.tarifaBase.toString())
            }
            btnGuardarEquipaje.text = "Actualizar"
        }

        btnGuardarEquipaje.setOnClickListener { guardarTipoEquipaje() }
        btnVolverMenuEquipaje.setOnClickListener { finish() }
    }

    private fun guardarTipoEquipaje() {
        val edNombreEquipaje = findViewById<EditText>(R.id.edNombreEquipaje)
        val edPesoEquipaje = findViewById<EditText>(R.id.edPesoEquipaje)
        val edTarifaEquipaje = findViewById<EditText>(R.id.edTarifaEquipaje)

        val nombre = edNombreEquipaje.text.toString().trim()
        val pesoTexto = edPesoEquipaje.text.toString().trim()
        val tarifaTexto = edTarifaEquipaje.text.toString().trim()

        if (nombre.isEmpty() || pesoTexto.isEmpty() || tarifaTexto.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val peso = pesoTexto.toDoubleOrNull()
        val tarifa = tarifaTexto.toDoubleOrNull()

        if (peso == null || peso <= 0 || tarifa == null || tarifa <= 0) {
            Toast.makeText(this, "Peso y tarifa deben ser números válidos mayores a 0", Toast.LENGTH_SHORT).show()
            return
        }

        if (idEditar != null) {
            val actualizado = TipoEquipaje(
                id = idEditar!!,
                nombre = nombre,
                pesoMaximo = peso,
                tarifaBase = tarifa
            )
            DataStore.actualizarTipoEquipaje(actualizado)
            Toast.makeText(this, "Equipaje actualizado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            val nuevoTipo = TipoEquipaje(
                nombre = nombre,
                pesoMaximo = peso,
                tarifaBase = tarifa
            )
            DataStore.agregarTipoEquipaje(nuevoTipo)

            val bundle = Bundle().apply {
                putString("nombre_equipaje", nombre)
                putDouble("peso_maximo", peso)
                putDouble("tarifa_base", tarifa)
            }
            firebaseAnalytics.logEvent("tipo_equipaje_creado", bundle)

            Toast.makeText(this, "Equipaje registrado correctamente", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}