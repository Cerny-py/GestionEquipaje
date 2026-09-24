package com.ortz.gestionequipaje.Encomienda

import Model.Encomienda
import Model.EstadoEncomienda
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.DataStore

class EncomiendaAdapter(
    private val lista: List<Encomienda>,
    private val onActualizar: (Encomienda) -> Unit,
    private val onEliminar: (Encomienda) -> Unit
) : RecyclerView.Adapter<EncomiendaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDestinatario: TextView = view.findViewById(R.id.tvDestinatarioItem)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoItem)
        val tvTipoEquipaje: TextView = view.findViewById(R.id.tvTipoEquipajeItem)
        val tvTarifa: TextView = view.findViewById(R.id.tvTarifaItem)
        val ivActualizar: ImageView = view.findViewById(R.id.ivActualizarEncomienda)
        val ivEliminar: ImageView = view.findViewById(R.id.ivEliminarEncomienda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_encomienda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        val destinatario = DataStore.obtenerDestinatario(item.destinatarioId)
        val tipoEquipaje = DataStore.obtenerTipoEquipaje(item.tipoEquipajeId)

        holder.tvDestinatario.text = destinatario?.nombre ?: "Destinatario eliminado"
        holder.tvTipoEquipaje.text = if (tipoEquipaje != null) {
            "${tipoEquipaje.nombre} · ${item.peso} kg"
        } else {
            "Tipo de equipaje eliminado"
        }
        holder.tvTarifa.text = "Tarifa: $${item.tarifaCalculada.toInt()}"
        holder.ivActualizar.setOnClickListener { onActualizar(item) }
        holder.ivEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount() = lista.size
}