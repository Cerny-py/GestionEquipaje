package com.ortz.gestionequipaje.Encomienda

import Model.Encomienda
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Entrega
class EncomiendaAdapter(
    private val lista: List<Encomienda>,
    private val onActualizar: (Encomienda) -> Unit,
    private val onEliminar: (Encomienda) -> Unit,
    private val onAgendar: (Encomienda) -> Unit,
    private val onPdf: (Encomienda) -> Unit,
    private val onChat: (Encomienda) -> Unit
) : RecyclerView.Adapter<EncomiendaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivChat: ImageView = view.findViewById(R.id.ivChatEncomienda)
        val tvDestinatario: TextView = view.findViewById(R.id.tvDestinatarioItem)
        val tvTipoEquipaje: TextView = view.findViewById(R.id.tvTipoEquipajeItem)
        val tvTarifa: TextView = view.findViewById(R.id.tvTarifaItem)
        val ivPdf: ImageView = view.findViewById(R.id.ivPdfEncomienda)
        val ivAgendar: ImageView = view.findViewById(R.id.ivAgendarEncomienda)
        val ivActualizar: ImageView = view.findViewById(R.id.ivActualizarEncomienda)
        val ivEliminar: ImageView = view.findViewById(R.id.ivEliminarEncomienda)
        val tvEstado: TextView = view.findViewById(R.id.tvEstadoItem)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_encomienda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.tvDestinatario.text = item.destinatarioNombre
        holder.tvTipoEquipaje.text = "${item.tipoEquipajeNombre} · ${item.peso} kg"
        val llegada = if (item.fechaEntregaEstimada.isNotEmpty()) {
            " · Llega ${Entrega.formatoLegible(item.fechaEntregaEstimada)}"
        } else {
            ""
        }
        holder.tvTarifa.text = "Tarifa: $${item.tarifaCalculada.toInt()}$llegada"
        holder.ivPdf.setOnClickListener { onPdf(item) }
        holder.ivActualizar.setOnClickListener { onActualizar(item) }
        holder.ivEliminar.setOnClickListener { onEliminar(item) }
        holder.tvEstado.text = item.estado.uppercase()
        holder.ivAgendar.setOnClickListener { onAgendar(item) }
        holder.ivChat.setOnClickListener { onChat(item) }
    }

    override fun getItemCount() = lista.size
}