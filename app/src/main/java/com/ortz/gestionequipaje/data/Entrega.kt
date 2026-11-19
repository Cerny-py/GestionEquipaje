package com.ortz.gestionequipaje.data

import Model.Encomienda
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Entrega {

    private val formatoIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun diasHabilesPorDistancia(km: Double?): Int {
        if (km == null) return 3
        return 1 + (km / 300).toInt()
    }

    fun calcularFecha(fechaBase: String, diasHabiles: Int): String {
        val calendario = Calendar.getInstance()
        val base = try {
            formatoIso.parse(fechaBase)
        } catch (e: Exception) {
            null
        }
        if (base != null) {
            calendario.time = base
        }

        var agregados = 0
        while (agregados < diasHabiles) {
            calendario.add(Calendar.DAY_OF_YEAR, 1)
            val dia = calendario.get(Calendar.DAY_OF_WEEK)
            if (dia != Calendar.SATURDAY && dia != Calendar.SUNDAY) {
                agregados++
            }
        }
        return formatoIso.format(calendario.time)
    }

    fun formatoLegible(fechaIso: String): String {
        val partes = fechaIso.split("-")
        return if (partes.size == 3) "${partes[2]}-${partes[1]}-${partes[0]}" else fechaIso
    }

    fun agendar(context: Context, encomienda: Encomienda) {
        val fechaIso = if (encomienda.fechaEntregaEstimada.isNotEmpty()) {
            encomienda.fechaEntregaEstimada
        } else {
            calcularFecha(encomienda.fechaRegistro, 3)
        }
        agendarFecha(context, fechaIso, encomienda.destinatarioNombre, encomienda.id)
    }

    fun agendarFecha(context: Context, fechaIso: String, destinatario: String, idEncomienda: String) {
        val dia = Calendar.getInstance()
        val fecha = try {
            formatoIso.parse(fechaIso)
        } catch (e: Exception) {
            null
        }
        if (fecha != null) {
            dia.time = fecha
        }

        val inicio = dia.clone() as Calendar
        inicio.set(Calendar.HOUR_OF_DAY, 9)
        inicio.set(Calendar.MINUTE, 0)
        inicio.set(Calendar.SECOND, 0)

        val fin = dia.clone() as Calendar
        fin.set(Calendar.HOUR_OF_DAY, 18)
        fin.set(Calendar.MINUTE, 0)
        fin.set(Calendar.SECOND, 0)

        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, "Llegada estimada de encomienda - Gestión de Equipaje")
            .putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Destinatario: $destinatario\nN° de encomienda: $idEncomienda\n" +
                        "Fecha aproximada, calculada según la distancia del envío."
            )
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio.timeInMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fin.timeInMillis)

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No hay una app de calendario instalada", Toast.LENGTH_LONG).show()
        }
    }
}