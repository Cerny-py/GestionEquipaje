package com.ortz.gestionequipaje.Equipaje

import Model.TipoEquipaje
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R

class VerEquipaje : AppCompatActivity() {

    private lateinit var rvEquipajes: RecyclerView
    private lateinit var adapter: TipoEquipajeAdapter
    private lateinit var btnVolver: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val lista = mutableListOf<TipoEquipaje>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_equipaje)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        rvEquipajes = findViewById(R.id.rvEquipajes)
        btnVolver = findViewById(R.id.btnVolverMenuEquipaje)

        rvEquipajes.layoutManager = LinearLayoutManager(this)

        adapter = TipoEquipajeAdapter(
            lista = lista,
            onActualizar = { tipoSeleccionado ->
                val intent = Intent(this, AgregarEquipaje::class.java)
                intent.putExtra("idEquipaje", tipoSeleccionado.id)
                startActivity(intent)
            },
            onEliminar = { tipoSeleccionado ->
                confirmarEliminar(tipoSeleccionado)
            }
        )
        rvEquipajes.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarLista()
    }

    private fun cargarLista() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "No se pudo verificar tu sesión", Toast.LENGTH_SHORT).show()
            return
        }

        ocupado(true)
        db.collection("tiposEquipaje")
            .whereEqualTo("usuarioId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                lista.clear()
                for (doc in snapshot) {
                    doc.toObject(TipoEquipaje::class.java)?.let { lista.add(it) }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar los equipajes: ${it.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { ocupado(false) }
    }

    private fun confirmarEliminar(tipo: TipoEquipaje) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar equipaje")
            .setMessage("¿Seguro que quieres eliminar \"${tipo.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarTipoEquipaje(tipo) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarTipoEquipaje(tipo: TipoEquipaje) {
        if (!hayRed()) {
            Toast.makeText(this, "Sin conexión a internet, intenta más tarde", Toast.LENGTH_SHORT).show()
            return
        }

        ocupado(true)
        db.collection("tiposEquipaje").document(tipo.id)
            .delete()
            .addOnSuccessListener {
                lista.remove(tipo)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Equipaje eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { ocupado(false) }
    }

    private fun ocupado(estado: Boolean) {
        btnVolver.isEnabled = !estado
    }

    private fun hayRed(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val red = cm.activeNetwork ?: return false
        val capacidades = cm.getNetworkCapabilities(red) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}