package com.ortz.gestionequipaje.Encomienda

import Model.Encomienda
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Eventos
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Sesion

class VerEncomienda : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val lista = mutableListOf<Encomienda>()
    private var ocupado = false

    private lateinit var rvEncomiendas: RecyclerView
    private lateinit var adapter: EncomiendaAdapter
    private lateinit var progreso: ProgressBar
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_encomienda)

        rvEncomiendas = findViewById(R.id.rvEncomiendas)
        progreso = findViewById(R.id.progresoListaEncomienda)
        tvVacio = findViewById(R.id.tvVacioEncomienda)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEncomienda)

        rvEncomiendas.layoutManager = LinearLayoutManager(this)

        adapter = EncomiendaAdapter(
            lista = lista,
            onActualizar = { seleccionada ->
                val intent = Intent(this, AgregarEncomienda::class.java)
                intent.putExtra("idEncomienda", seleccionada.id)
                intent.putExtra("destinatarioId", seleccionada.destinatarioId)
                intent.putExtra("tipoEquipajeId", seleccionada.tipoEquipajeId)
                intent.putExtra("peso", seleccionada.peso)
                intent.putExtra("valorDeclarado", seleccionada.valorDeclarado)
                intent.putExtra("tarifaCalculada", seleccionada.tarifaCalculada)
                intent.putExtra("fechaRegistro", seleccionada.fechaRegistro)
                startActivity(intent)
            },
            onEliminar = { seleccionada -> confirmarEliminar(seleccionada) }
        )
        rvEncomiendas.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarEncomiendas()
    }

    private fun mostrarCarga(estado: Boolean) {
        ocupado = estado
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
    }

    private fun cargarEncomiendas() {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga(true)
        db.collection("encomiendas")
            .whereEqualTo("usuarioId", Sesion.uid())
            .get()
            .addOnCompleteListener { task ->
                mostrarCarga(false)
                if (task.isSuccessful) {
                    lista.clear()
                    for (doc in task.result) {
                        lista.add(doc.toObject(Encomienda::class.java).copy(id = doc.id))
                    }
                    lista.sortByDescending { it.fechaRegistro }
                    adapter.notifyDataSetChanged()
                    tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    Eventos.registrar(this, "listado_consultado", "encomienda")
                } else {
                    task.exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    Toast.makeText(this, "No se pudieron cargar las encomiendas", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun confirmarEliminar(encomienda: Encomienda) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar encomienda")
            .setMessage("¿Seguro que quieres eliminar esta encomienda?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarEncomienda(encomienda.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEncomienda(id: String) {
        if (id.isEmpty()) {
            Toast.makeText(this, "El registro no tiene un id válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (ocupado) return
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga(true)
        db.collection("encomiendas").document(id).delete()
            .addOnSuccessListener {
                Eventos.registrar(this, "registro_eliminado", "encomienda")
                Toast.makeText(this, "Encomienda eliminada", Toast.LENGTH_SHORT).show()
                cargarEncomiendas()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo eliminar la encomienda", Toast.LENGTH_SHORT).show()
            }
    }
}