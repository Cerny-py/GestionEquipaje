package com.ortz.gestionequipaje.Destinatario

import Model.Destinatario
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore

class VerDestinatario : AppCompatActivity() {
    private lateinit var rvDestinatarios: RecyclerView
    private lateinit var adapter: DestinatarioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_destinatario)

        rvDestinatarios = findViewById(R.id.rvDestinatarios)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuDestinatario)

        rvDestinatarios.layoutManager = LinearLayoutManager(this)

        adapter = DestinatarioAdapter(
            lista = DataStore.destinatarios,
            onActualizar = { seleccionado ->
                val intent = Intent(this, AgregarDestinatario::class.java)
                intent.putExtra("idDestinatario", seleccionado.id)
                startActivity(intent)
            },
            onEliminar = { seleccionado -> confirmarEliminar(seleccionado) }
        )
        rvDestinatarios.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    private fun confirmarEliminar(destinatario: Destinatario) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar destinatario")
            .setMessage("¿Seguro que quieres eliminar a \"${destinatario.nombre}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                DataStore.eliminarDestinatario(destinatario.id)
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