package com.ortz.gestionequipaje.Equipaje

import Model.TipoEquipaje
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

class VerEquipaje : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val lista = mutableListOf<TipoEquipaje>()
    private var ocupado = false

    private lateinit var rvEquipajes: RecyclerView
    private lateinit var adapter: TipoEquipajeAdapter
    private lateinit var progreso: ProgressBar
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_equipaje)

        rvEquipajes = findViewById(R.id.rvEquipajes)
        progreso = findViewById(R.id.progresoListaEquipaje)
        tvVacio = findViewById(R.id.tvVacioEquipaje)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEquipaje)

        rvEquipajes.layoutManager = LinearLayoutManager(this)

        adapter = TipoEquipajeAdapter(
            lista = lista,
            onActualizar = { seleccionado ->
                val intent = Intent(this, AgregarEquipaje::class.java)
                intent.putExtra("idEquipaje", seleccionado.id)
                startActivity(intent)
            },
            onEliminar = { seleccionado -> confirmarEliminar(seleccionado) }
        )
        rvEquipajes.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarLista()
    }

    private fun mostrarCarga(estado: Boolean) {
        ocupado = estado
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
    }

    private fun actualizarVacio() {
        tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun cargarLista() {
        val usuarioId = Sesion.uid()
        if (usuarioId.isEmpty()) {
            Toast.makeText(this, "No hay sesión de Firebase activa, reintenta", Toast.LENGTH_SHORT).show()
            actualizarVacio()
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            actualizarVacio()
            return
        }

        mostrarCarga(true)
        db.collection("tiposEquipaje")
            .whereEqualTo("usuarioId", usuarioId)
            .get()
            .addOnCompleteListener { task ->
                mostrarCarga(false)
                if (task.isSuccessful) {
                    lista.clear()
                    for (doc in task.result) {
                        lista.add(doc.toObject(TipoEquipaje::class.java).copy(id = doc.id))
                    }
                    lista.sortBy { it.nombre.lowercase() }
                    adapter.notifyDataSetChanged()
                    actualizarVacio()
                    Eventos.registrar(this, "listado_consultado", "tipo_equipaje")
                } else {
                    task.exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    Toast.makeText(this, "No se pudieron cargar los tipos de equipaje", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun confirmarEliminar(tipo: TipoEquipaje) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar equipaje")
            .setMessage("¿Seguro que quieres eliminar \"${tipo.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarTipoEquipaje(tipo.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarTipoEquipaje(id: String) {
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
        db.collection("tiposEquipaje").document(id).delete()
            .addOnSuccessListener {
                Eventos.registrar(this, "registro_eliminado", "tipo_equipaje")
                Toast.makeText(this, "Equipaje eliminado", Toast.LENGTH_SHORT).show()
                mostrarCarga(false)
                cargarLista()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo eliminar el tipo de equipaje", Toast.LENGTH_SHORT).show()
            }
    }
}