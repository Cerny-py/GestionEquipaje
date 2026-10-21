package com.ortz.gestionequipaje.Equipaje

import Model.TipoEquipaje
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Eventos
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Sesion

class AgregarEquipaje : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var idEditar: String? = null
    private var guardando = false

    private lateinit var edNombreEquipaje: EditText
    private lateinit var edPesoEquipaje: EditText
    private lateinit var edTarifaEquipaje: EditText
    private lateinit var edDescripcionEquipaje: EditText
    private lateinit var cbEmbalajeEspecial: MaterialCheckBox
    private lateinit var btnGuardarEquipaje: Button
    private lateinit var progreso: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_equipaje)

        edNombreEquipaje = findViewById(R.id.edNombreEquipaje)
        edPesoEquipaje = findViewById(R.id.edPesoEquipaje)
        edTarifaEquipaje = findViewById(R.id.edTarifaEquipaje)
        edDescripcionEquipaje = findViewById(R.id.edDescripcionEquipaje)
        cbEmbalajeEspecial = findViewById(R.id.cbEmbalajeEspecial)
        btnGuardarEquipaje = findViewById(R.id.btnGuardarEquipaje)
        progreso = findViewById(R.id.progresoEquipaje)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEquipaje)

        idEditar = intent.getStringExtra("idEquipaje")

        if (idEditar != null) {
            btnGuardarEquipaje.text = "Actualizar"
            cargarTipoEquipaje(idEditar!!)
        }

        btnGuardarEquipaje.setOnClickListener { guardarTipoEquipaje() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun mostrarCarga(estado: Boolean) {
        guardando = estado
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
        btnGuardarEquipaje.isEnabled = !estado
    }

    private fun cargarTipoEquipaje(id: String) {
        mostrarCarga(true)
        db.collection("tiposEquipaje").document(id).get()
            .addOnSuccessListener { doc ->
                val tipo = doc.toObject(TipoEquipaje::class.java)
                if (tipo == null) {
                    Toast.makeText(this, "El registro ya no existe", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                edNombreEquipaje.setText(tipo.nombre)
                edPesoEquipaje.setText(tipo.pesoMaximo.toString())
                edTarifaEquipaje.setText(tipo.tarifaBase.toString())
                edDescripcionEquipaje.setText(tipo.descripcion)
                cbEmbalajeEspecial.isChecked = tipo.requiereEmbalajeEspecial
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo cargar el tipo de equipaje", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnCompleteListener { mostrarCarga(false) }
    }

    private fun guardarTipoEquipaje() {
        if (guardando) return

        val nombre = edNombreEquipaje.text.toString().trim()
        val pesoTexto = edPesoEquipaje.text.toString().trim()
        val tarifaTexto = edTarifaEquipaje.text.toString().trim()
        val descripcion = edDescripcionEquipaje.text.toString().trim()
        val requiereEmbalaje = cbEmbalajeEspecial.isChecked

        if (nombre.isEmpty()) {
            edNombreEquipaje.error = "Ingresa un nombre"
            edNombreEquipaje.requestFocus()
            return
        }
        if (pesoTexto.isEmpty()) {
            edPesoEquipaje.error = "Ingresa el peso máximo"
            edPesoEquipaje.requestFocus()
            return
        }
        if (tarifaTexto.isEmpty()) {
            edTarifaEquipaje.error = "Ingresa la tarifa base"
            edTarifaEquipaje.requestFocus()
            return
        }

        val peso = pesoTexto.toDoubleOrNull()
        val tarifa = tarifaTexto.toDoubleOrNull()

        if (peso == null || peso !in 0.1..1000.0) {
            edPesoEquipaje.error = "Peso entre 0.1 y 1000 kg"
            edPesoEquipaje.requestFocus()
            return
        }
        if (tarifa == null || tarifa !in 1.0..1000000.0) {
            edTarifaEquipaje.error = "Tarifa entre 1 y 1.000.000"
            edTarifaEquipaje.requestFocus()
            return
        }

        val usuarioId = Sesion.uid()
        if (usuarioId.isEmpty()) {
            Toast.makeText(this, "No hay sesión de Firebase activa, reintenta", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga(true)

        val id = idEditar
        if (id == null) {
            val ref = db.collection("tiposEquipaje").document()
            val nuevoTipo = TipoEquipaje(
                id = ref.id,
                nombre = nombre,
                pesoMaximo = peso,
                tarifaBase = tarifa,
                descripcion = descripcion,
                requiereEmbalajeEspecial = requiereEmbalaje,
                usuarioId = usuarioId
            )
            ref.set(nuevoTipo)
                .addOnSuccessListener {
                    Eventos.registrar(this, "registro_creado", "tipo_equipaje")
                    Toast.makeText(this, "Equipaje registrado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e -> errorAlGuardar(e, "registrar") }
        } else {
            val datos = mapOf(
                "nombre" to nombre,
                "pesoMaximo" to peso,
                "tarifaBase" to tarifa,
                "descripcion" to descripcion,
                "requiereEmbalajeEspecial" to requiereEmbalaje
            )
            db.collection("tiposEquipaje").document(id).update(datos)
                .addOnSuccessListener {
                    Eventos.registrar(this, "registro_actualizado", "tipo_equipaje")
                    Toast.makeText(this, "Equipaje actualizado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e -> errorAlGuardar(e, "actualizar") }
        }
    }

    private fun errorAlGuardar(e: Exception, accion: String) {
        mostrarCarga(false)
        FirebaseCrashlytics.getInstance().recordException(e)
        Toast.makeText(this, "No se pudo $accion el tipo de equipaje, intenta nuevamente", Toast.LENGTH_LONG).show()
    }
}