package com.ortz.gestionequipaje.Destinatario

import Model.Destinatario
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R

class DestinatarioAdapter(
    private val lista: List<Destinatario>,
    private val onActualizar: (Destinatario) -> Unit,
    private val onEliminar: (Destinatario) -> Unit
) : RecyclerView.Adapter<DestinatarioAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreItem)
        val tvDireccion: TextView = view.findViewById(R.id.tvDireccionItem)
        val tvTelefono: TextView = view.findViewById(R.id.tvTelefonoItem)
        val tvEmail: TextView = view.findViewById(R.id.tvEmailItem)
        val tvComuna: TextView = view.findViewById(R.id.tvComunaItem)
        val ivActualizar: ImageView = view.findViewById(R.id.ivActualizarDestinatario)
        val ivEliminar: ImageView = view.findViewById(R.id.ivEliminarDestinatario)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_destinatario, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.tvNombre.text = item.nombre
        holder.tvDireccion.text = item.direccion
        holder.tvTelefono.text = item.telefono
        holder.tvEmail.text = item.email
        holder.tvComuna.text = item.comuna

        holder.ivActualizar.setOnClickListener { onActualizar(item) }
        holder.ivEliminar.setOnClickListener { onEliminar(item) }
    }

    override fun getItemCount() = lista.size
}