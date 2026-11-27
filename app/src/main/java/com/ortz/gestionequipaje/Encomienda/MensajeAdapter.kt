package com.ortz.gestionequipaje.Encomienda

import Model.Mensaje
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ortz.gestionequipaje.R
import java.text.SimpleDateFormat
import java.util.Locale

class MensajeAdapter(
    private val lista: List<Mensaje>
) : RecyclerView.Adapter<MensajeAdapter.ViewHolder>() {

    var miUid = ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTexto: TextView = view.findViewById(R.id.tvTextoMensaje)
        val tvHora: TextView = view.findViewById(R.id.tvHoraMensaje)
    }

    override fun getItemViewType(position: Int): Int {
        return if (lista[position].usuarioId == miUid) TIPO_ENVIADO else TIPO_RECIBIDO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TIPO_ENVIADO) {
            R.layout.item_mensaje_enviado
        } else {
            R.layout.item_mensaje_recibido
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mensaje = lista[position]
        holder.tvTexto.text = mensaje.texto

        val rol = when (mensaje.emisor) {
            "repartidor" -> "Repartidor"
            "admin" -> "Administración"
            else -> "Cliente"
        }
        val fecha = mensaje.fecha?.toDate()
        val hora = if (fecha != null) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(fecha)
        } else {
            ""
        }
        holder.tvHora.text = "$rol  $hora"
    }

    override fun getItemCount() = lista.size

    companion object {
        private const val TIPO_ENVIADO = 1
        private const val TIPO_RECIBIDO = 2
    }
}