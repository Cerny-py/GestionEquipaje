package com.ortz.gestionequipaje.Destinatario

import Model.Destinatario
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore
import com.google.firebase.analytics.FirebaseAnalytics

class AgregarDestinatario : AppCompatActivity() {

    private var idEditar: String? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var editandoMascara = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_destinatario)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val edNombre = findViewById<EditText>(R.id.edNombreDestinatario)
        val edDireccion = findViewById<EditText>(R.id.edDireccionDestinatario)
        val edTelefono = findViewById<EditText>(R.id.edTelefonoDestinatario)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarDestinatario)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuDestinatario)

        edTelefono.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (editandoMascara) return
                editandoMascara = true

                val soloNumeros = s.toString().replace(Regex("[^\\d]"), "")
                val formateado = formatearTelefono(soloNumeros)

                s?.replace(0, s.length, formateado)
                editandoMascara = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        idEditar = intent.getStringExtra("idDestinatario")

        if (idEditar != null) {
            val destinatario = DataStore.obtenerDestinatario(idEditar!!)
            destinatario?.let {
                edNombre.setText(it.nombre)
                edDireccion.setText(it.direccion)
                edTelefono.setText(it.telefono)
            }
            btnGuardar.text = "Actualizar"
        }

        btnGuardar.setOnClickListener { guardarDestinatario() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun formatearTelefono(numeros: String): String {
        var digitos = numeros
        if (!digitos.startsWith("56")) {
            digitos = "56$digitos"
        }
        digitos = digitos.take(11) // 56 + 9 dígitos = 11 máximo

        val sb = StringBuilder("+")
        for (i in digitos.indices) {
            sb.append(digitos[i])
            when (i) {
                1 -> sb.append(" ")
                2 -> sb.append(" ")
                6 -> sb.append(" ")
            }
        }
        return sb.toString().trim()
    }

    private fun guardarDestinatario() {
        val edNombre = findViewById<EditText>(R.id.edNombreDestinatario)
        val edDireccion = findViewById<EditText>(R.id.edDireccionDestinatario)
        val edTelefono = findViewById<EditText>(R.id.edTelefonoDestinatario)

        val nombre = edNombre.text.toString().trim()
        val direccion = edDireccion.text.toString().trim()
        val telefono = edTelefono.text.toString().trim()

        if (nombre.isEmpty() || direccion.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (telefono.length < 9) {
            Toast.makeText(this, "Ingresa un teléfono válido", Toast.LENGTH_SHORT).show()
            return
        }

        if (idEditar != null) {
            val actualizado = Destinatario(
                id = idEditar!!,
                nombre = nombre,
                direccion = direccion,
                telefono = telefono
            )
            DataStore.actualizarDestinatario(actualizado)
            Toast.makeText(this, "Destinatario actualizado correctamente", Toast.LENGTH_SHORT).show()
        } else {
            val nuevo = Destinatario(nombre = nombre, direccion = direccion, telefono = telefono)
            DataStore.agregarDestinatario(nuevo)

            val bundle = Bundle().apply {
                putString("nombre_destinatario", nombre)
            }
            firebaseAnalytics.logEvent("destinatario_creado", bundle)

            Toast.makeText(this, "Destinatario registrado correctamente", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}