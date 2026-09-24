package com.ortz.gestionequipaje.Encomienda

import Model.Encomienda
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore

class VerEncomienda : AppCompatActivity() {
    private lateinit var rvEncomiendas: RecyclerView
    private lateinit var adapter: EncomiendaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_encomienda)

        rvEncomiendas = findViewById(R.id.rvEncomiendas)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEncomienda)

        rvEncomiendas.layoutManager = LinearLayoutManager(this)

        adapter = EncomiendaAdapter(
            lista = DataStore.encomiendas,
            onActualizar = { seleccionada ->
                val intent = Intent(this, AgregarEncomienda::class.java)
                intent.putExtra("idEncomienda", seleccionada.id)
                startActivity(intent)
            },
            onEliminar = { seleccionada -> confirmarEliminar(seleccionada) }
        )
        rvEncomiendas.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    private fun confirmarEliminar(encomienda: Encomienda) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar encomienda")
            .setMessage("¿Seguro que quieres eliminar esta encomienda?")
            .setPositiveButton("Eliminar") { _, _ ->
                DataStore.eliminarEncomienda(encomienda.id)
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