package com.ortz.gestionequipaje.Equipaje

import Model.TipoEquipaje
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R

class AgregarEquipaje : AppCompatActivity() {

    private var idEditar: String? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var edNombreEquipaje: EditText
    private lateinit var edPesoEquipaje: EditText
    private lateinit var edTarifaEquipaje: EditText
    private lateinit var edDescripcionEquipaje: EditText
    private lateinit var cbEmbalajeEspecial: MaterialCheckBox
    private lateinit var btnGuardarEquipaje: Button
    private lateinit var btnVolverMenuEquipaje: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_equipaje)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        edNombreEquipaje = findViewById(R.id.edNombreEquipaje)
        edPesoEquipaje = findViewById(R.id.edPesoEquipaje)
        edTarifaEquipaje = findViewById(R.id.edTarifaEquipaje)
        edDescripcionEquipaje = findViewById(R.id.edDescripcionEquipaje)
        cbEmbalajeEspecial = findViewById(R.id.cbEmbalajeEspecial)
        btnGuardarEquipaje = findViewById(R.id.btnGuardarEquipaje)
        btnVolverMenuEquipaje = findViewById(R.id.btnVolverMenuEquipaje)

        // Asegura una sesión (si ya existe una en otra parte de la app, esto no hace nada)
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }

        idEditar = intent.getStringExtra("idEquipaje")

        if (idEditar != null) {
            btnGuardarEquipaje.text = "Actualizar"
            cargarTipoEquipaje(idEditar!!)
        }

        btnGuardarEquipaje.setOnClickListener { guardarTipoEquipaje() }
        btnVolverMenuEquipaje.setOnClickListener { finish() }
    }

    private fun cargarTipoEquipaje(id: String) {
        ocupado(true)
        db.collection("tiposEquipaje").document(id).get()
            .addOnSuccessListener { doc ->
                val tipo = doc.toObject(TipoEquipaje::class.java)
                tipo?.let {
                    edNombreEquipaje.setText(it.nombre)
                    edPesoEquipaje.setText(it.pesoMaximo.toString())
                    edTarifaEquipaje.setText(it.tarifaBase.toString())
                    edDescripcionEquipaje.setText(it.descripcion)
                    cbEmbalajeEspecial.isChecked = it.requiereEmbalajeEspecial
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo cargar el tipo de equipaje", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { ocupado(false) }
    }

    private fun guardarTipoEquipaje() {
        val nombre = edNombreEquipaje.text.toString().trim()
        val pesoTexto = edPesoEquipaje.text.toString().trim()
        val tarifaTexto = edTarifaEquipaje.text.toString().trim()
        val descripcion = edDescripcionEquipaje.text.toString().trim()
        val requiereEmbalaje = cbEmbalajeEspecial.isChecked

        if (nombre.isEmpty()) {
            edNombreEquipaje.error = "Ingresa un nombre"
            return
        }
        if (pesoTexto.isEmpty()) {
            edPesoEquipaje.error = "Ingresa el peso máximo"
            return
        }
        if (tarifaTexto.isEmpty()) {
            edTarifaEquipaje.error = "Ingresa la tarifa base"
            return
        }

        val peso = pesoTexto.toDoubleOrNull()
        val tarifa = tarifaTexto.toDoubleOrNull()

        if (peso == null || peso !in 0.1..1000.0) {
            edPesoEquipaje.error = "Peso entre 0.1 y 1000 kg"
            return
        }
        if (tarifa == null || tarifa !in 1.0..1000000.0) {
            edTarifaEquipaje.error = "Tarifa entre 1 y 1.000.000"
            return
        }

        if (!hayRed()) {
            Toast.makeText(this, "Sin conexión a internet, intenta más tarde", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "No se pudo verificar tu sesión, intenta de nuevo", Toast.LENGTH_SHORT).show()
            return
        }

        ocupado(true)

        if (idEditar != null) {
            val datos = mapOf(
                "nombre" to nombre,
                "pesoMaximo" to peso,
                "tarifaBase" to tarifa,
                "descripcion" to descripcion,
                "requiereEmbalajeEspecial" to requiereEmbalaje
            )
            db.collection("tiposEquipaje").document(idEditar!!)
                .update(datos)
                .addOnSuccessListener {
                    Toast.makeText(this, "Equipaje actualizado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al actualizar: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener { ocupado(false) }
        } else {
            val ref = db.collection("tiposEquipaje").document()
            val nuevoTipo = TipoEquipaje(
                id = ref.id,
                nombre = nombre,
                pesoMaximo = peso,
                tarifaBase = tarifa,
                descripcion = descripcion,
                requiereEmbalajeEspecial = requiereEmbalaje,
                usuarioId = uid
            )
            ref.set(nuevoTipo)
                .addOnSuccessListener {
                    val bundle = Bundle().apply {
                        putString("nombre_equipaje", nombre)
                        putDouble("peso_maximo", peso)
                        putDouble("tarifa_base", tarifa)
                    }
                    firebaseAnalytics.logEvent("tipo_equipaje_creado", bundle)
                    Toast.makeText(this, "Equipaje registrado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al registrar: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener { ocupado(false) }
        }
    }

    private fun ocupado(estado: Boolean) {
        btnGuardarEquipaje.isEnabled = !estado
        btnVolverMenuEquipaje.isEnabled = !estado
    }

    private fun hayRed(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val red = cm.activeNetwork ?: return false
        val capacidades = cm.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}