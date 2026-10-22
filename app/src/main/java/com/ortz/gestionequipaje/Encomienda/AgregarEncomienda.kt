package com.ortz.gestionequipaje.Encomienda

import Model.Encomienda
import Model.EstadoEncomienda
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
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore
import com.google.firebase.analytics.FirebaseAnalytics

class AgregarEncomienda : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var spnDestinatario: AutoCompleteTextView
    private lateinit var spnTipoEquipaje: AutoCompleteTextView
    private lateinit var edPeso: TextInputEditText
    private lateinit var edValorDeclarado: TextInputEditText
    private lateinit var tvTarifaCalculada: TextView
    private lateinit var tvEstado: TextView

    private var posicionDestinatario = -1
    private var posicionTipoEquipaje = -1
    private var tarifaActual = 0.0

    private var idEditar: String? = null
    private var estadoActual = EstadoEncomienda.PENDIENTE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_encomienda)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        spnDestinatario = findViewById(R.id.spnDestinatarioId)
        spnTipoEquipaje = findViewById(R.id.spnTipoEquipajeId)
        edPeso = findViewById(R.id.edPeso)
        edValorDeclarado = findViewById(R.id.edValorDeclarado)
        tvTarifaCalculada = findViewById(R.id.tvTarifaCalculada)

        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEncomienda)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarEncomienda)

        if (DataStore.destinatarios.isEmpty() || DataStore.tiposEquipaje.isEmpty()) {
            Toast.makeText(
                this,
                "Debes registrar al menos un destinatario y un tipo de equipaje antes de crear una encomienda",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        cargarDropdowns()

        idEditar = intent.getStringExtra("idEncomienda")

        if (idEditar != null) {
            precargarDatos(idEditar!!)
            btnGuardar.text = "Actualizar"
        }

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
        }

        btnGuardar.setOnClickListener { guardarEncomienda() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun cargarDropdowns() {
        val nombresDestinatarios = DataStore.destinatarios.map { it.nombre }
        spnDestinatario.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresDestinatarios)
        )

        val nombresTipos = DataStore.tiposEquipaje.map { it.nombre }
        spnTipoEquipaje.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombresTipos)
        )
    }

    private fun precargarDatos(id: String) {
        val encomienda = DataStore.obtenerEncomienda(id) ?: return

        posicionDestinatario = DataStore.destinatarios.indexOfFirst { it.id == encomienda.destinatarioId }
        posicionTipoEquipaje = DataStore.tiposEquipaje.indexOfFirst { it.id == encomienda.tipoEquipajeId }

        if (posicionDestinatario >= 0) {
            spnDestinatario.setText(DataStore.destinatarios[posicionDestinatario].nombre, false)
        }
        if (posicionTipoEquipaje >= 0) {
            spnTipoEquipaje.setText(DataStore.tiposEquipaje[posicionTipoEquipaje].nombre, false)
        }

        edPeso.setText(encomienda.peso.toString())
        edValorDeclarado.setText(encomienda.valorDeclarado.toString())
        tarifaActual = encomienda.tarifaCalculada
        tvTarifaCalculada.text = "Tarifa: $${tarifaActual.toInt()}"
    }

    private fun recalcularTarifa() {
        if (posicionTipoEquipaje < 0) {
            tvTarifaCalculada.text = "Tarifa: $0"
            tarifaActual = 0.0
            return
        }

        val tipoSeleccionado = DataStore.tiposEquipaje[posicionTipoEquipaje]
        val peso = edPeso.text.toString().toDoubleOrNull() ?: 0.0

        val subtotal = tipoSeleccionado.tarifaBase + (peso * 500)
        val iva = subtotal * 0.19
        tarifaActual = subtotal + iva
        tvTarifaCalculada.text = "Tarifa: $${tarifaActual.toInt()} (IVA incluido)"
    }

    private fun guardarEncomienda() {
        val pesoTexto = edPeso.text.toString().trim()
        val valorTexto = edValorDeclarado.text.toString().trim()

        if (posicionDestinatario < 0) {
            Toast.makeText(this, "Selecciona un destinatario", Toast.LENGTH_SHORT).show()
            return
        }
        if (posicionTipoEquipaje < 0) {
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

        val destinatarioSeleccionado = DataStore.destinatarios[posicionDestinatario]
        val tipoSeleccionado = DataStore.tiposEquipaje[posicionTipoEquipaje]

        if (peso > tipoSeleccionado.pesoMaximo) {
            Toast.makeText(
                this,
                "El peso supera el máximo permitido para ${tipoSeleccionado.nombre} (${tipoSeleccionado.pesoMaximo} kg)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (idEditar != null) {
            val actualizada = Encomienda(
                id = idEditar!!,
                clienteId = "cliente_actual",
                destinatarioId = destinatarioSeleccionado.id,
                tipoEquipajeId = tipoSeleccionado.id,
                peso = peso,
                valorDeclarado = valorDeclarado,
                tarifaCalculada = tarifaActual,
            )
            DataStore.actualizarEncomienda(actualizada)
            Toast.makeText(this, "Encomienda actualizada correctamente", Toast.LENGTH_SHORT).show()
        } else {
            val nuevaEncomienda = Encomienda(
                clienteId = "cliente_actual",
                destinatarioId = destinatarioSeleccionado.id,
                tipoEquipajeId = tipoSeleccionado.id,
                peso = peso,
                valorDeclarado = valorDeclarado,
                tarifaCalculada = tarifaActual,
            )
            DataStore.agregarEncomienda(nuevaEncomienda)

            Toast.makeText(this, "Encomienda registrada correctamente", Toast.LENGTH_SHORT).show()
        }

        finish()
    }
}