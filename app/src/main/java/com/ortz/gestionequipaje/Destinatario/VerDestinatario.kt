package com.ortz.gestionequipaje.Destinatario

import Model.Destinatario
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

class VerDestinatario : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val lista = mutableListOf<Destinatario>()
    private var ocupado = false

    private lateinit var rvDestinatarios: RecyclerView
    private lateinit var adapter: DestinatarioAdapter
    private lateinit var progreso: ProgressBar
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_destinatario)

        rvDestinatarios = findViewById(R.id.rvDestinatarios)
        progreso = findViewById(R.id.progresoListaDestinatario)
        tvVacio = findViewById(R.id.tvVacioDestinatario)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuDestinatario)

        rvDestinatarios.layoutManager = LinearLayoutManager(this)

        adapter = DestinatarioAdapter(
            lista = lista,
            onActualizar = { seleccionado ->
                val intent = Intent(this, AgregarDestinatario::class.java)
                intent.putExtra("idDestinatario", seleccionado.id)
                intent.putExtra("nombre", seleccionado.nombre)
                intent.putExtra("direccion", seleccionado.direccion)
                intent.putExtra("telefono", seleccionado.telefono)
                intent.putExtra("email", seleccionado.email)
                intent.putExtra("comuna", seleccionado.comuna)
                startActivity(intent)
            },
            onEliminar = { seleccionado -> confirmarEliminar(seleccionado) }
        )
        rvDestinatarios.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarDestinatarios()
    }

    private fun mostrarCarga(estado: Boolean) {
        ocupado = estado
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
    }

    private fun cargarDestinatarios() {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga(true)
        db.collection("destinatarios")
            .whereEqualTo("usuarioId", Sesion.uid())
            .get()
            .addOnCompleteListener { task ->
                mostrarCarga(false)
                if (task.isSuccessful) {
                    lista.clear()
                    for (document in task.result) {
                        lista.add(
                            Destinatario(
                                id = document.id,
                                nombre = document.getString("nombre") ?: "",
                                direccion = document.getString("direccion") ?: "",
                                telefono = document.getString("telefono") ?: "",
                                email = document.getString("email") ?: "",
                                comuna = document.getString("comuna") ?: ""
                            )
                        )
                    }
                    lista.sortBy { it.nombre.lowercase() }
                    adapter.notifyDataSetChanged()
                    tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    Eventos.registrar(this, "listado_consultado", "destinatario")
                } else {
                    task.exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    Toast.makeText(this, "No se pudieron cargar los destinatarios", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun confirmarEliminar(destinatario: Destinatario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar destinatario")
            .setMessage("¿Seguro que quieres eliminar a \"${destinatario.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarDestinatario(destinatario.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarDestinatario(id: String) {
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
        db.collection("destinatarios").document(id).delete()
            .addOnSuccessListener {
                Eventos.registrar(this, "registro_eliminado", "destinatario")
                Toast.makeText(this, "Destinatario eliminado", Toast.LENGTH_SHORT).show()
                cargarDestinatarios()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo eliminar el destinatario", Toast.LENGTH_SHORT).show()
            }
    }
}