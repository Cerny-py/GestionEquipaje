package com.ortz.gestionequipaje.Equipaje

import Model.TipoEquipaje
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R

class TipoEquipajeAdapter(
    private val lista: List<TipoEquipaje>,
    private val onActualizar: (TipoEquipaje) -> Unit,
    private val onEliminar: (TipoEquipaje) -> Unit
) : RecyclerView.Adapter<TipoEquipajeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreItem)
        val tvPeso: TextView = view.findViewById(R.id.tvPesoItem)
        val tvTarifa: TextView = view.findViewById(R.id.tvTarifaItem)
        val ivActualizar: ImageView = view.findViewById(R.id.ivActualizarEquipaje)
        val ivEliminar: ImageView = view.findViewById(R.id.ivEliminarEquipaje)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tipo_equipaje, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = item.nombre
        holder.tvPeso.text = "Peso máx: ${item.pesoMaximo} kg"
        holder.tvTarifa.text = "Tarifa base: $${item.tarifaBase}"

        holder.ivActualizar.setOnClickListener { onActualizar(item) }
        holder.ivEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount() = lista.size
}