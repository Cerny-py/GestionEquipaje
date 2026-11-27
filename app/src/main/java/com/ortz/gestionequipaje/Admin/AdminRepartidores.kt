package com.ortz.gestionequipaje.Admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Rol
import com.ortz.gestionequipaje.data.Sesion

class AdminRepartidores : AppCompatActivity() {

    private data class Repartidor(val email: String, val nombre: String, val activo: Boolean)

    private val db = FirebaseFirestore.getInstance()
    private val repartidores = mutableListOf<Repartidor>()
    private val textos = mutableListOf<String>()

    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var progreso: ProgressBar
    private lateinit var tvVacio: TextView
    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_repartidores)

        if (Rol.actual != Rol.ADMIN) {
            Toast.makeText(this, "Solo el administrador puede entrar aquí", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etNombre = findViewById(R.id.etNombreRepartidor)
        etCorreo = findViewById(R.id.etCorreoRepartidor)
        progreso = findViewById(R.id.progresoRepartidores)
        tvVacio = findViewById(R.id.tvVacioRepartidores)
        val lvRepartidores = findViewById<ListView>(R.id.lvRepartidores)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarRepartidor)
        val btnVolver = findViewById<Button>(R.id.btnVolverRepartidores)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, textos)
        lvRepartidores.adapter = adapter

        lvRepartidores.setOnItemClickListener { _, _, position, _ ->
            mostrarOpciones(repartidores[position])
        }
        btnRegistrar.setOnClickListener { registrar() }
        btnVolver.setOnClickListener { finish() }

        cargar()
    }

    private fun cargar() {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        progreso.visibility = View.VISIBLE
        db.collection("repartidores").get()
            .addOnCompleteListener { task ->
                progreso.visibility = View.GONE
                if (task.isSuccessful) {
                    repartidores.clear()
                    for (doc in task.result) {
                        repartidores.add(
                            Repartidor(
                                doc.id,
                                doc.getString("nombre") ?: "",
                                doc.getBoolean("activo") ?: false
                            )
                        )
                    }
                    repartidores.sortBy { it.nombre.lowercase() }

                    textos.clear()
                    for (r in repartidores) {
                        val estado = if (r.activo) "Activo" else "Inactivo"
                        textos.add("${r.nombre}\n${r.email}  |  $estado")
                    }
                    adapter.notifyDataSetChanged()
                    tvVacio.visibility = if (repartidores.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    task.exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    Toast.makeText(this, "No se pudieron cargar los repartidores", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registrar() {
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim().lowercase()

        if (nombre.isEmpty()) {
            etNombre.error = "Ingresa el nombre"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreo.error = "Ingresa un correo válido"
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        val datos = hashMapOf<String, Any>(
            "email" to correo,
            "nombre" to nombre,
            "activo" to true,
            "creadoPor" to Sesion.uid()
        )

        progreso.visibility = View.VISIBLE
        db.collection("repartidores").document(correo).set(datos)
            .addOnSuccessListener {
                etNombre.setText("")
                etCorreo.setText("")
                Toast.makeText(this, "Repartidor registrado", Toast.LENGTH_SHORT).show()
                cargar()
            }
            .addOnFailureListener { e ->
                progreso.visibility = View.GONE
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo registrar el repartidor", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarOpciones(repartidor: Repartidor) {
        val opciones = arrayOf(if (repartidor.activo) "Desactivar" else "Activar", "Eliminar")
        AlertDialog.Builder(this)
            .setTitle(repartidor.nombre)
            .setItems(opciones) { _, opcion ->
                if (opcion == 0) {
                    cambiarActivo(repartidor)
                } else {
                    confirmarEliminar(repartidor)
                }
            }
            .show()
    }

    private fun cambiarActivo(repartidor: Repartidor) {
        db.collection("repartidores").document(repartidor.email)
            .update("activo", !repartidor.activo)
            .addOnSuccessListener { cargar() }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo actualizar el repartidor", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmarEliminar(repartidor: Repartidor) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar repartidor")
            .setMessage("¿Seguro que quieres eliminar a ${repartidor.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("repartidores").document(repartidor.email).delete()
                    .addOnSuccessListener { cargar() }
                    .addOnFailureListener { e ->
                        FirebaseCrashlytics.getInstance().recordException(e)
                        Toast.makeText(this, "No se pudo eliminar el repartidor", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}