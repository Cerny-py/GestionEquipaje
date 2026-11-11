package com.ortz.gestionequipaje.Encomienda

import Model.Destinatario
import Model.TipoEquipaje
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Eventos
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Sesion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.ortz.gestionequipaje.data.Distancia

class AgregarEncomienda : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val destinatarios = mutableListOf<Destinatario>()
    private val tiposEquipaje = mutableListOf<TipoEquipaje>()

    private lateinit var spnDestinatario: AutoCompleteTextView
    private lateinit var spnTipoEquipaje: AutoCompleteTextView
    private lateinit var edPeso: TextInputEditText
    private lateinit var edValorDeclarado: TextInputEditText
    private lateinit var tvTarifaCalculada: TextView
    private lateinit var btnGuardar: Button

    private var posicionDestinatario = -1
    private var posicionTipoEquipaje = -1
    private var tarifaActual = 0.0
    private var idEditar: String? = null
    private var fechaOriginal = ""
    private var guardando = false
    private var distanciaKm: Double? = null
    private var buscandoDistancia = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_encomienda)

        spnDestinatario = findViewById(R.id.spnDestinatarioId)
        spnTipoEquipaje = findViewById(R.id.spnTipoEquipajeId)
        edPeso = findViewById(R.id.edPeso)
        edValorDeclarado = findViewById(R.id.edValorDeclarado)
        tvTarifaCalculada = findViewById(R.id.tvTarifaCalculada)
        btnGuardar = findViewById(R.id.btnGuardarEncomienda)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEncomienda)

        idEditar = intent.getStringExtra("idEncomienda")
        if (idEditar != null) {
            btnGuardar.text = "Actualizar"
        }

        cargarCatalogos()

        edPeso.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                recalcularTarifa()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        spnTipoEquipaje.setOnItemClickListener { _, _, position, _ ->
            posicionTipoEquipaje = position
            recalcularTarifa()
        }

        spnDestinatario.setOnItemClickListener { _, _, position, _ ->
            posicionDestinatario = position
            calcularDistancia(destinatarios[position].comuna)
        }

        btnGuardar.setOnClickListener { guardarEncomienda() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun cargarCatalogos() {
        val usuarioId = Sesion.uid()
        if (usuarioId.isEmpty()) {
            Toast.makeText(this, "No hay sesión de Firebase activa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("destinatarios").whereEqualTo("usuarioId", usuarioId).get()
            .addOnCompleteListener { tareaDestinatarios ->
                if (!tareaDestinatarios.isSuccessful) {
                    errorCatalogos(tareaDestinatarios.exception)
                    return@addOnCompleteListener
                }
                destinatarios.clear()
                for (doc in tareaDestinatarios.result) {
                    destinatarios.add(
                        Destinatario(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            direccion = doc.getString("direccion") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            email = doc.getString("email") ?: "",
                            comuna = doc.getString("comuna") ?: ""
                        )
                    )
                }

                db.collection("tiposEquipaje").whereEqualTo("usuarioId", usuarioId).get()
                    .addOnCompleteListener { tareaTipos ->
                        if (!tareaTipos.isSuccessful) {
                            errorCatalogos(tareaTipos.exception)
                            return@addOnCompleteListener
                        }
                        tiposEquipaje.clear()
                        for (doc in tareaTipos.result) {
                            tiposEquipaje.add(doc.toObject(TipoEquipaje::class.java).copy(id = doc.id))
                        }
                        catalogosListos()
                    }
            }
    }

    private fun errorCatalogos(e: Exception?) {
        e?.let { FirebaseCrashlytics.getInstance().recordException(it) }
        Toast.makeText(this, "No se pudieron cargar los datos necesarios", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun catalogosListos() {
        if (destinatarios.isEmpty() || tiposEquipaje.isEmpty()) {
            Toast.makeText(
                this,
                "Debes registrar al menos un destinatario y un tipo de equipaje antes de crear una encomienda",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        spnDestinatario.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, destinatarios.map { it.nombre })
        )
        spnTipoEquipaje.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tiposEquipaje.map { it.nombre })
        )

        if (idEditar != null) {
            precargarDatos()
        }
    }

    private fun precargarDatos() {
        val idDestinatario = intent.getStringExtra("destinatarioId")
        val idTipo = intent.getStringExtra("tipoEquipajeId")

        posicionDestinatario = destinatarios.indexOfFirst { it.id == idDestinatario }
        posicionTipoEquipaje = tiposEquipaje.indexOfFirst { it.id == idTipo }

        if (posicionDestinatario >= 0) {
            spnDestinatario.setText(destinatarios[posicionDestinatario].nombre, false)
            calcularDistancia(destinatarios[posicionDestinatario].comuna)
        }
        if (posicionTipoEquipaje >= 0) {
            spnTipoEquipaje.setText(tiposEquipaje[posicionTipoEquipaje].nombre, false)
        }

        edPeso.setText(intent.getDoubleExtra("peso", 0.0).toString())
        edValorDeclarado.setText(intent.getDoubleExtra("valorDeclarado", 0.0).toString())
        tarifaActual = intent.getDoubleExtra("tarifaCalculada", 0.0)
        fechaOriginal = intent.getStringExtra("fechaRegistro") ?: ""
        tvTarifaCalculada.text = "Tarifa: $${tarifaActual.toInt()}"
    }

    private fun calcularDistancia(comuna: String) {
        distanciaKm = null
        buscandoDistancia = true
        tvTarifaCalculada.text = "Calculando distancia..."

        if (!Red.hayInternet(this)) {
            buscandoDistancia = false
            recalcularTarifa()
            return
        }

        Thread {
            var km: Double? = null
            try {
                km = Distancia.buscarKm(comuna)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                val vigente = destinatarios.getOrNull(posicionDestinatario)?.comuna == comuna
                if (vigente) {
                    distanciaKm = km
                    buscandoDistancia = false
                    recalcularTarifa()
                }
            }
        }.start()
    }

    private fun recalcularTarifa() {
        if (posicionTipoEquipaje < 0 || posicionTipoEquipaje >= tiposEquipaje.size) {
            tvTarifaCalculada.text = "Tarifa: $0"
            tarifaActual = 0.0
            return
        }

        val tipoSeleccionado = tiposEquipaje[posicionTipoEquipaje]
        val peso = edPeso.text.toString().toDoubleOrNull() ?: 0.0
        val km = distanciaKm ?: 0.0

        val subtotal = tipoSeleccionado.tarifaBase + (km * 30) + (peso * 800)
        val iva = subtotal * 0.19
        tarifaActual = subtotal + iva

        val nota = if (distanciaKm == null) {
            " (estimada, sin distancia)"
        } else {
            " (${km.toInt()} km, IVA incluido)"
        }
        tvTarifaCalculada.text = "Tarifa: $${tarifaActual.toInt()}$nota"
    }

    private fun guardarEncomienda() {
        if (guardando) return
        if (buscandoDistancia) {
            Toast.makeText(this, "Espera, calculando la distancia...", Toast.LENGTH_SHORT).show()
            return
        }

        val pesoTexto = edPeso.text.toString().trim()
        val valorTexto = edValorDeclarado.text.toString().trim()

        if (posicionDestinatario < 0 || posicionDestinatario >= destinatarios.size) {
            Toast.makeText(this, "Selecciona un destinatario", Toast.LENGTH_SHORT).show()
            return
        }
        if (posicionTipoEquipaje < 0 || posicionTipoEquipaje >= tiposEquipaje.size) {
            Toast.makeText(this, "Selecciona un tipo de equipaje", Toast.LENGTH_SHORT).show()
            return
        }
        if (pesoTexto.isEmpty() || valorTexto.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val peso = pesoTexto.toDoubleOrNull()
        val valorDeclarado = valorTexto.toDoubleOrNull()

        if (peso == null || peso <= 0 || valorDeclarado == null || valorDeclarado <= 0) {
            Toast.makeText(this, "Peso y valor deben ser números válidos mayores a 0", Toast.LENGTH_SHORT).show()
            return
        }

        val destinatario = destinatarios[posicionDestinatario]
        val tipo = tiposEquipaje[posicionTipoEquipaje]

        if (peso > tipo.pesoMaximo) {
            Toast.makeText(
                this,
                "El peso supera el máximo permitido para ${tipo.nombre} (${tipo.pesoMaximo} kg)",
                Toast.LENGTH_LONG
            ).show()
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

        val fecha = if (fechaOriginal.isNotEmpty()) {
            fechaOriginal
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        val encomienda = hashMapOf(
            "destinatarioId" to destinatario.id,
            "destinatarioNombre" to destinatario.nombre,
            "tipoEquipajeId" to tipo.id,
            "tipoEquipajeNombre" to tipo.nombre,
            "peso" to peso,
            "valorDeclarado" to valorDeclarado,
            "tarifaCalculada" to tarifaActual,
            "fechaRegistro" to fecha,
            "usuarioId" to usuarioId
        )

        guardando = true
        btnGuardar.isEnabled = false

        val id = idEditar
        if (id == null) {
            db.collection("encomiendas").add(encomienda)
                .addOnSuccessListener {
                    Eventos.registrar(this, "registro_creado", "encomienda")
                    Toast.makeText(this, "Encomienda registrada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e -> errorAlGuardar(e, "registrar") }
        } else {
            db.collection("encomiendas").document(id).set(encomienda)
                .addOnSuccessListener {
                    Eventos.registrar(this, "registro_actualizado", "encomienda")
                    Toast.makeText(this, "Encomienda actualizada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e -> errorAlGuardar(e, "actualizar") }
        }
    }

    private fun errorAlGuardar(e: Exception, accion: String) {
        guardando = false
        btnGuardar.isEnabled = true
        FirebaseCrashlytics.getInstance().recordException(e)
        Toast.makeText(this, "No se pudo $accion la encomienda, intenta nuevamente", Toast.LENGTH_LONG).show()
    }
}