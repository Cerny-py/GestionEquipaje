package com.ortz.gestionequipaje.Encomienda

import Model.Encomienda
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.ComprobantePdf
import com.ortz.gestionequipaje.data.DriveHelper
import com.ortz.gestionequipaje.data.Eventos
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Sesion
import java.io.File

class VerEncomienda : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val lista = mutableListOf<Encomienda>()
    private var ocupado = false
    private var subiendoDrive = false
    private var archivoPendiente: File? = null

    private lateinit var rvEncomiendas: RecyclerView
    private lateinit var adapter: EncomiendaAdapter
    private lateinit var progreso: ProgressBar
    private lateinit var tvVacio: TextView

    // Se ejecuta cuando el usuario responde la pantalla "¿Permitir acceso a Drive?"
    private val pedirPermisoDrive = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { resultado ->
        val archivo = archivoPendiente
        if (resultado.resultCode == Activity.RESULT_OK && archivo != null) {
            try {
                val auth = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(resultado.data)
                val token = auth.accessToken
                if (token != null) {
                    subirADrive(token, archivo)
                } else {
                    terminarSubida()
                    Toast.makeText(this, "No se obtuvo el permiso de Drive", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                terminarSubida()
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo autorizar el acceso a Drive", Toast.LENGTH_LONG).show()
            }
        } else {
            terminarSubida()
            Toast.makeText(this, "Debes aceptar el permiso de Drive para subir el archivo", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ver_encomienda)

        rvEncomiendas = findViewById(R.id.rvEncomiendas)
        progreso = findViewById(R.id.progresoListaEncomienda)
        tvVacio = findViewById(R.id.tvVacioEncomienda)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuEncomienda)

        rvEncomiendas.layoutManager = LinearLayoutManager(this)

        adapter = EncomiendaAdapter(
            lista = lista,
            onActualizar = { seleccionada ->
                val intent = Intent(this, AgregarEncomienda::class.java)
                intent.putExtra("idEncomienda", seleccionada.id)
                intent.putExtra("destinatarioId", seleccionada.destinatarioId)
                intent.putExtra("tipoEquipajeId", seleccionada.tipoEquipajeId)
                intent.putExtra("peso", seleccionada.peso)
                intent.putExtra("valorDeclarado", seleccionada.valorDeclarado)
                intent.putExtra("tarifaCalculada", seleccionada.tarifaCalculada)
                intent.putExtra("fechaRegistro", seleccionada.fechaRegistro)
                startActivity(intent)
            },
            onEliminar = { seleccionada -> confirmarEliminar(seleccionada) },
            onPdf = { seleccionada -> generarComprobante(seleccionada) }
        )
        rvEncomiendas.adapter = adapter

        btnVolver.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        cargarEncomiendas()
    }

    private fun mostrarCarga(estado: Boolean) {
        ocupado = estado
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
    }

    private fun cargarEncomiendas() {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga(true)
        db.collection("encomiendas")
            .whereEqualTo("usuarioId", Sesion.uid())
            .get()
            .addOnCompleteListener { task ->
                // Si hay una subida a Drive en curso, el indicador de carga se mantiene
                mostrarCarga(subiendoDrive)
                if (task.isSuccessful) {
                    lista.clear()
                    for (doc in task.result) {
                        lista.add(doc.toObject(Encomienda::class.java).copy(id = doc.id))
                    }
                    lista.sortByDescending { it.fechaRegistro }
                    adapter.notifyDataSetChanged()
                    tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                    Eventos.registrar(this, "listado_consultado", "encomienda")
                } else {
                    task.exception?.let { FirebaseCrashlytics.getInstance().recordException(it) }
                    Toast.makeText(this, "No se pudieron cargar las encomiendas", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun generarComprobante(encomienda: Encomienda) {
        try {
            val archivo = ComprobantePdf.generar(this, encomienda)
            AlertDialog.Builder(this)
                .setTitle("Comprobante generado")
                .setMessage("¿Qué quieres hacer con el PDF?")
                .setPositiveButton("Abrir") { _, _ -> ComprobantePdf.abrir(this, archivo) }
                .setNeutralButton("Compartir") { _, _ -> ComprobantePdf.compartir(this, archivo) }
                .setNegativeButton("Subir a Drive") { _, _ -> subirComprobanteADrive(archivo) }
                .show()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            Toast.makeText(this, "No se pudo generar el comprobante", Toast.LENGTH_SHORT).show()
        }
    }

    // Paso 1: pedir permiso de Drive (solo drive.file: archivos creados por esta app)
    private fun subirComprobanteADrive(archivo: File) {
        if (ocupado || subiendoDrive) return
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }
        archivoPendiente = archivo
        subiendoDrive = true
        mostrarCarga(true)

        val peticion = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.file")))
            .build()

        Identity.getAuthorizationClient(this).authorize(peticion)
            .addOnSuccessListener { resultado ->
                if (resultado.hasResolution()) {
                    // Primera vez: Google muestra la pantalla para permitir el acceso
                    val pendiente = resultado.pendingIntent
                    if (pendiente != null) {
                        pedirPermisoDrive.launch(IntentSenderRequest.Builder(pendiente.intentSender).build())
                    } else {
                        terminarSubida()
                    }
                } else {
                    val token = resultado.accessToken
                    if (token != null) {
                        subirADrive(token, archivo)
                    } else {
                        terminarSubida()
                        Toast.makeText(this, "No se obtuvo el permiso de Drive", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                terminarSubida()
                FirebaseCrashlytics.getInstance().recordException(e)
                Log.e("DRIVE", "authorize falló", e)
                Toast.makeText(this, "No se pudo pedir el permiso de Drive", Toast.LENGTH_LONG).show()
            }
    }

    // Paso 2: subir el PDF. La red no puede usarse en el hilo principal, por eso va en un Thread
    private fun subirADrive(token: String, archivo: File) {
        Thread {
            try {
                DriveHelper.subirComprobante(token, archivo)
                runOnUiThread {
                    terminarSubida()
                    Toast.makeText(this, "Comprobante subido a la carpeta GestionEquipaje de tu Drive", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Log.e("DRIVE", "subida falló", e)
                runOnUiThread {
                    terminarSubida()
                    Toast.makeText(this, "No se pudo subir el comprobante a Drive", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun terminarSubida() {
        subiendoDrive = false
        mostrarCarga(false)
    }

    private fun confirmarEliminar(encomienda: Encomienda) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar encomienda")
            .setMessage("¿Seguro que quieres eliminar esta encomienda?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarEncomienda(encomienda.id) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarEncomienda(id: String) {
        if (id.isEmpty()) {
            Toast.makeText(this, "El registro no tiene un id válido", Toast.LENGTH_SHORT).show()
            return
        }
        if (ocupado) return
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarCarga(true)
        db.collection("encomiendas").document(id).delete()
            .addOnSuccessListener {
                Eventos.registrar(this, "registro_eliminado", "encomienda")
                Toast.makeText(this, "Encomienda eliminada", Toast.LENGTH_SHORT).show()
                cargarEncomiendas()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo eliminar la encomienda", Toast.LENGTH_SHORT).show()
            }
    }
}