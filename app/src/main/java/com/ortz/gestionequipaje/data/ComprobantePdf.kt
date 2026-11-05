package com.ortz.gestionequipaje.data

import Model.Encomienda
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ComprobantePdf {

    private const val ANCHO = 595
    private const val ALTO = 842

    fun generar(context: Context, encomienda: Encomienda): File {
        val documento = PdfDocument()
        val infoPagina = PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create()
        val pagina = documento.startPage(infoPagina)
        dibujar(pagina.canvas, encomienda)
        documento.finishPage(pagina)

        val carpeta = File(context.cacheDir, "comprobantes")
        if (!carpeta.exists()) {
            carpeta.mkdirs()
        }
        val archivo = File(carpeta, "comprobante_${encomienda.id}.pdf")

        try {
            FileOutputStream(archivo).use { salida -> documento.writeTo(salida) }
        } finally {
            documento.close()
        }
        return archivo
    }

    private fun dibujar(canvas: Canvas, e: Encomienda) {
        val naranja = Color.parseColor("#FF6D00")
        val oscuro = Color.parseColor("#212121")
        val gris = Color.parseColor("#757575")

        val fondoTitulo = Paint().apply { color = naranja }
        canvas.drawRect(0f, 0f, ANCHO.toFloat(), 100f, fondoTitulo)

        val titulo = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Comprobante de encomienda", 40f, 55f, titulo)

        val subtitulo = Paint().apply {
            color = Color.WHITE
            textSize = 13f
            isAntiAlias = true
        }
        canvas.drawText("Gestión de equipaje y encomiendas", 40f, 80f, subtitulo)

        val etiqueta = Paint().apply {
            color = gris
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valor = Paint().apply {
            color = oscuro
            textSize = 15f
            isAntiAlias = true
        }
        val linea = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        val filas = listOf(
            "N° DE ENCOMIENDA" to e.id,
            "FECHA DE REGISTRO" to formatearFecha(e.fechaRegistro),
            "DESTINATARIO" to e.destinatarioNombre,
            "TIPO DE EQUIPAJE" to e.tipoEquipajeNombre,
            "PESO" to "${e.peso} kg",
            "VALOR DECLARADO" to "$${formatearPesos(e.valorDeclarado)}"
        )

        var y = 160f
        for ((nombre, dato) in filas) {
            canvas.drawText(nombre, 40f, y, etiqueta)
            canvas.drawText(recortar(dato, 48), 40f, y + 22f, valor)
            canvas.drawLine(40f, y + 36f, ANCHO - 40f, y + 36f, linea)
            y += 62f
        }

        val cajaTotal = Paint().apply { color = Color.parseColor("#FFF3E0") }
        canvas.drawRect(40f, y + 10f, ANCHO - 40f, y + 90f, cajaTotal)

        val textoTotal = Paint().apply {
            color = oscuro
            textSize = 14f
            isAntiAlias = true
        }
        canvas.drawText("Tarifa total (IVA incluido)", 60f, y + 45f, textoTotal)

        val montoTotal = Paint().apply {
            color = naranja
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("$${formatearPesos(e.tarifaCalculada)}", ANCHO - 60f, y + 70f, montoTotal)

        val pie = Paint().apply {
            color = gris
            textSize = 10f
            isAntiAlias = true
        }
        val ahora = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Documento generado el $ahora desde la app Gestión de Equipaje", 40f, ALTO - 40f, pie)
    }

    fun abrir(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

        val ver = Intent(Intent.ACTION_VIEW)
        ver.setDataAndType(uri, "application/pdf")
        ver.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            context.startActivity(ver)
        } catch (e: ActivityNotFoundException) {
            compartir(context, archivo)
        }
    }

    fun compartir(context: Context, archivo: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)

        val enviar = Intent(Intent.ACTION_SEND)
        enviar.type = "application/pdf"
        enviar.putExtra(Intent.EXTRA_STREAM, uri)
        enviar.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(enviar, "Compartir comprobante"))
    }

    private fun formatearPesos(monto: Double): String {
        return NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL")).format(monto.toLong())
    }

    private fun formatearFecha(fecha: String): String {
        val partes = fecha.split("-")
        return if (partes.size == 3) "${partes[2]}-${partes[1]}-${partes[0]}" else fecha
    }

    private fun recortar(texto: String, max: Int): String {
        return if (texto.length > max) texto.take(max - 1) + "…" else texto
    }
}