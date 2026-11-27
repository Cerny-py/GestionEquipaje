package com.ortz.gestionequipaje.Admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ortz.gestionequipaje.Encomienda.ChatEncomienda
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Rol

class EntregasActivity : AppCompatActivity() {

    private data class Entrega(
        val id: String,
        val destinatario: String,
        val texto: String,
        val fecha: String
    )

    private val db = FirebaseFirestore.getInstance()
    private val entregas = mutableListOf<Entrega>()
    private val textos = mutableListOf<String>()

    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var progreso: ProgressBar
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entregas)

        if (Rol.actual != Rol.ADMIN && Rol.actual != Rol.REPARTIDOR) {
            Toast.makeText(this, "No tienes acceso a esta pantalla", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvTitulo = findViewById<TextView>(R.id.tvTituloEntregas)
        val lvEntregas = findViewById<ListView>(R.id.lvEntregas)
        val btnVolver = findViewById<Button>(R.id.btnVolverEntregas)
        progreso = findViewById(R.id.progresoEntregas)
        tvVacio = findViewById(R.id.tvVacioEntregas)

        tvTitulo.text = if (Rol.actual == Rol.ADMIN) "Todas las encomiendas" else "Mis entregas"

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, textos)
        lvEntregas.adapter = adapter

        lvEntregas.setOnItemClickListener { _, _, position, _ ->
            mostrarOpciones(entregas[position])
        }
        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (Rol.actual == Rol.ADMIN || Rol.actual == Rol.REPARTIDOR) {
            cargar()
        }
    }

    private fun cargar() {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        val consulta: Query = if (Rol.actual == Rol.ADMIN) {
            db.collection("encomiendas")
        } else {
            db.collection("encomiendas").whereEqualTo("repartidorEmail", Rol.email())
        }

        progreso.visibility = View.VISIBLE
        consulta.get().addOnCompleteListener { task ->
            progreso.visibility = View.GONE
            if (task.isSuccessful) {
                entregas.clear()
                for (doc in task.result) {
                    val destinatario = doc.getString("destinatarioNombre") ?: ""
                    val tipo = doc.getString("tipoEquipajeNombre") ?: ""
                    val estado = doc.getString("estado") ?: "registrada"
                    val repartidor = doc.getString("repartidorNombre") ?: ""
                    val quien = if (repartidor.isEmpty()) "sin asignar" else repartidor
                    entregas.add(
                        Entrega(
                            doc.id,
                            destinatario,
                            "$destinatario  |  $tipo\nEstado: ${estado.uppercase()}  |  Repartidor: $quien",
                            doc.getString("fechaRegistro") ?: ""
                        )
                    )
                }
                entregas.sortByDescending { it.fecha }

                textos.clear()
                for (e in entregas) {
                    textos.add(e.texto)
                }
                adapter.notifyDataSetChanged()
                tvVacio.visibility = if (entregas.isEmpty()) View.VISIBLE else View.GONE
            } else {
                task.exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                Toast.makeText(this, "No se pudieron cargar las encomiendas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarOpciones(entrega: Entrega) {
        if (Rol.actual == Rol.ADMIN) {
            AlertDialog.Builder(this)
                .setTitle(entrega.destinatario)
                .setItems(arrayOf("Asignar repartidor", "Abrir chat")) { _, opcion ->
                    if (opcion == 0) elegirRepartidor(entrega) else abrirChat(entrega)
                }
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle(entrega.destinatario)
                .setItems(arrayOf("Abrir chat", "Marcar en tránsito", "Marcar entregada")) { _, opcion ->
                    when (opcion) {
                        0 -> abrirChat(entrega)
                        1 -> cambiarEstado(entrega, "en_transito")
                        else -> cambiarEstado(entrega, "entregado")
                    }
                }
                .show()
        }
    }

    private fun abrirChat(entrega: Entrega) {
        val intent = Intent(this, ChatEncomienda::class.java)
        intent.putExtra("idEncomienda", entrega.id)
        intent.putExtra("destinatarioNombre", entrega.destinatario)
        startActivity(intent)
    }

    private fun cambiarEstado(entrega: Entrega, nuevoEstado: String) {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("encomiendas").document(entrega.id).update("estado", nuevoEstado)
            .addOnSuccessListener {
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
                cargar()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo cambiar el estado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun elegirRepartidor(entrega: Entrega) {
        db.collection("repartidores").whereEqualTo("activo", true).get()
            .addOnSuccessListener { resultado ->
                val correos = mutableListOf<String>()
                val nombres = mutableListOf<String>()
                for (doc in resultado) {
                    correos.add(doc.id)
                    nombres.add(doc.getString("nombre") ?: doc.id)
                }

                if (nombres.isEmpty()) {
                    Toast.makeText(this, "Primero registra un repartidor activo", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                AlertDialog.Builder(this)
                    .setTitle("Asignar repartidor")
                    .setItems(nombres.toTypedArray()) { _, posicion ->
                        asignar(entrega, correos[posicion], nombres[posicion])
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudieron cargar los repartidores", Toast.LENGTH_SHORT).show()
            }
    }

    private fun asignar(entrega: Entrega, correo: String, nombre: String) {
        val datos = hashMapOf<String, Any>(
            "repartidorEmail" to correo,
            "repartidorNombre" to nombre
        )
        db.collection("encomiendas").document(entrega.id).update(datos)
            .addOnSuccessListener {
                Toast.makeText(this, "Asignada a $nombre", Toast.LENGTH_SHORT).show()
                cargar()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo asignar el repartidor", Toast.LENGTH_SHORT).show()
            }
    }
}