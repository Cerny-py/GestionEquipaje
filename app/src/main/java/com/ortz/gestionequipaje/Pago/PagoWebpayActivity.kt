package com.ortz.gestionequipaje.Pago

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Sesion
import com.ortz.gestionequipaje.data.TransbankHelper

class PagoWebpayActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var webView: WebView
    private lateinit var progreso: ProgressBar
    private var tokenActual: String? = null
    private var idEncomienda: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pago_webpay)

        webView = findViewById(R.id.wvPago)
        progreso = findViewById(R.id.progresoPago)
        webView.settings.javaScriptEnabled = true

        idEncomienda = intent.getStringExtra("idEncomienda") ?: ""
        val monto = intent.getDoubleExtra("monto", 0.0).toLong()

        if (idEncomienda.isEmpty() || monto <= 0) {
            Toast.makeText(this, "Datos de pago inválidos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        iniciarPago(idEncomienda, monto)
    }

    private fun iniciarPago(id: String, monto: Long) {
        mostrarCarga(true)
        Thread {
            try {
                val inicio = TransbankHelper.crearTransaccion(
                    buyOrder = id,
                    sessionId = Sesion.uid(),
                    monto = monto
                )
                tokenActual = inicio.token
                runOnUiThread { cargarWebview(inicio.url, inicio.token) }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                runOnUiThread {
                    mostrarCarga(false)
                    Toast.makeText(this, "No se pudo iniciar el pago", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }.start()
    }

    private fun cargarWebview(url: String, token: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                val destino = request.url.toString()
                return if (destino.startsWith(com.ortz.gestionequipaje.data.TransbankConfig.RETURN_URL)) {
                    confirmarPago()
                    true
                } else {
                    false
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                mostrarCarga(false)
            }
        }
        webView.postUrl(url, "token_ws=$token".toByteArray())
    }

    private fun confirmarPago() {
        mostrarCarga(true)
        webView.visibility = View.GONE
        val token = tokenActual ?: return

        Thread {
            var resultado: TransbankHelper.ResultadoPago? = null
            try {
                resultado = TransbankHelper.confirmarTransaccion(token)
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            runOnUiThread {
                mostrarCarga(false)
                if (resultado != null && resultado.aprobado) {
                    actualizarPagado(true, "Pago aprobado")
                } else {
                    actualizarPagado(false, "Pago rechazado, intenta nuevamente")
                }
            }
        }.start()
    }

    private fun actualizarPagado(aprobado: Boolean, mensaje: String) {
        if (!aprobado) {
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db.collection("encomiendas").document(idEncomienda)
            .set(mapOf("pagado" to true), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "Pago aprobado, pero no se pudo guardar en la app", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun mostrarCarga(estado: Boolean) {
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
    }
}