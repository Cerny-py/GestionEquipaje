package com.ortz.gestionequipaje.Equipaje

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore

class VerEquipaje : AppCompatActivity() {
    private lateinit var rvEquipajes: RecyclerView
    private lateinit var adapter: TipoEquipajeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_equipaje)

        rvEquipajes = findViewById(R.id.rvEquipajes)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEquipaje)

        rvEquipajes.layoutManager = LinearLayoutManager(this)

        adapter = TipoEquipajeAdapter(
            lista = DataStore.tiposEquipaje,
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

    private fun confirmarEliminar(tipo: Model.TipoEquipaje) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar equipaje")
            .setMessage("¿Seguro que quieres eliminar \"${tipo.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                DataStore.eliminarTipoEquipaje(tipo.id)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}