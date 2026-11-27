package com.ortz.gestionequipaje.Encomienda

import Model.Mensaje
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Rol
import com.ortz.gestionequipaje.data.Sesion

class ChatEncomienda : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val mensajes = mutableListOf<Mensaje>()

    private var idEncomienda = ""
    private var emisor = ""
    private var enviando = false
    private var entregada = false
    private var calificacionActual = 0
    private var dialogoMostrado = false
    private var registroMensajes: ListenerRegistration? = null
    private var registroEncomienda: ListenerRegistration? = null

    private lateinit var adapter: MensajeAdapter
    private lateinit var rvMensajes: RecyclerView
    private lateinit var tvEstado: TextView
    private lateinit var tvSinMensajes: TextView
    private lateinit var tvChatCerrado: TextView
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_encomienda)

        idEncomienda = intent.getStringExtra("idEncomienda") ?: ""
        if (idEncomienda.isEmpty()) {
            Toast.makeText(this, "No se pudo abrir el chat de esta encomienda", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvTitulo = findViewById<TextView>(R.id.tvTituloChat)
        val btnVolver = findViewById<Button>(R.id.btnVolverChat)
        tvEstado = findViewById(R.id.tvEstadoChat)
        tvSinMensajes = findViewById(R.id.tvSinMensajes)
        tvChatCerrado = findViewById(R.id.tvChatCerrado)
        rvMensajes = findViewById(R.id.rvMensajes)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviarMensaje)

        tvTitulo.text = "Chat: ${intent.getStringExtra("destinatarioNombre") ?: "encomienda"}"

        val manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        rvMensajes.layoutManager = manager
        adapter = MensajeAdapter(mensajes)
        adapter.miUid = Sesion.uid()
        rvMensajes.adapter = adapter

        etMensaje.isEnabled = false
        btnEnviar.isEnabled = false

        btnEnviar.setOnClickListener { enviarMensaje() }
        btnVolver.setOnClickListener { finish() }
    }

    override fun onStart() {
        super.onStart()
        escucharEncomienda()
        escucharMensajes()
    }

    override fun onStop() {
        super.onStop()
        registroEncomienda?.remove()
        registroMensajes?.remove()
    }

    private fun escucharMensajes() {
        registroMensajes = db.collection("encomiendas").document(idEncomienda)
            .collection("mensajes")
            .orderBy("fecha")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    FirebaseCrashlytics.getInstance().recordException(error)
                    Toast.makeText(this, "No se pudieron cargar los mensajes", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                mensajes.clear()
                for (doc in snapshot.documents) {
                    val mensaje = doc.toObject(
                        Mensaje::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (mensaje != null) {
                        mensajes.add(mensaje.copy(id = doc.id))
                    }
                }
                adapter.notifyDataSetChanged()
                tvSinMensajes.visibility = if (mensajes.isEmpty()) View.VISIBLE else View.GONE
                if (mensajes.isNotEmpty()) {
                    rvMensajes.scrollToPosition(mensajes.size - 1)
                }
            }
    }

    private fun escucharEncomienda() {
        registroEncomienda = db.collection("encomiendas").document(idEncomienda)
            .addSnapshotListener { doc, error ->
                if (error != null) {
                    FirebaseCrashlytics.getInstance().recordException(error)
                    return@addSnapshotListener
                }
                if (doc == null || !doc.exists()) return@addSnapshotListener

                val estado = doc.getString("estado") ?: "registrada"
                val duenoId = doc.getString("usuarioId") ?: ""
                val repartidorEmail = doc.getString("repartidorEmail") ?: ""
                val repartidorNombre = doc.getString("repartidorNombre") ?: ""
                calificacionActual = (doc.getLong("calificacion") ?: 0L).toInt()
                entregada = estado.equals("entregado", ignoreCase = true)

                emisor = when {
                    duenoId == Sesion.uid() -> "cliente"
                    Rol.actual == Rol.REPARTIDOR && repartidorEmail == Rol.email() -> "repartidor"
                    Rol.actual == Rol.ADMIN -> "admin"
                    else -> ""
                }

                val quien = if (repartidorNombre.isEmpty()) "sin asignar" else repartidorNombre
                tvEstado.text = "Estado: ${estado.uppercase()}  |  Repartidor: $quien"
                tvChatCerrado.visibility = if (entregada) View.VISIBLE else View.GONE

                val puedeEscribir = emisor.isNotEmpty() && !entregada
                etMensaje.isEnabled = puedeEscribir
                btnEnviar.isEnabled = puedeEscribir && !enviando

                if (entregada && calificacionActual == 0 && emisor == "cliente" && !dialogoMostrado) {
                    dialogoMostrado = true
                    mostrarCalificacion()
                }
            }
    }

    private fun enviarMensaje() {
        if (enviando || entregada || emisor.isEmpty()) return

        val texto = etMensaje.text.toString().trim()
        if (texto.isEmpty()) {
            etMensaje.error = "Escribe un mensaje"
            return
        }
        if (texto.length > 500) {
            etMensaje.error = "Máximo 500 caracteres"
            return
        }

        val usuarioId = Sesion.uid()
        if (usuarioId.isEmpty()) {
            Toast.makeText(this, "No hay sesión de Firebase activa", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        enviando = true
        btnEnviar.isEnabled = false

        val mensaje = hashMapOf<String, Any>(
            "texto" to texto,
            "emisor" to emisor,
            "usuarioId" to usuarioId,
            "fecha" to FieldValue.serverTimestamp()
        )

        db.collection("encomiendas").document(idEncomienda)
            .collection("mensajes").add(mensaje)
            .addOnSuccessListener {
                etMensaje.setText("")
                terminarEnvio()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show()
                terminarEnvio()
            }
    }

    private fun terminarEnvio() {
        enviando = false
        btnEnviar.isEnabled = emisor.isNotEmpty() && !entregada
    }

    private fun mostrarCalificacion() {
        if (isFinishing) return

        val vista = layoutInflater.inflate(R.layout.dialog_calificacion, null)
        val barra = vista.findViewById<RatingBar>(R.id.ratingCalificacion)

        AlertDialog.Builder(this)
            .setTitle("Encomienda entregada")
            .setMessage("¿Cómo fue la atención del repartidor?")
            .setView(vista)
            .setPositiveButton("Enviar") { _, _ -> guardarCalificacion(barra.rating.toInt()) }
            .setNegativeButton("Ahora no", null)
            .show()
    }

    private fun guardarCalificacion(nota: Int) {
        if (nota < 1) {
            Toast.makeText(this, "Elige al menos 1 estrella", Toast.LENGTH_SHORT).show()
            mostrarCalificacion()
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("encomiendas").document(idEncomienda).update("calificacion", nota)
            .addOnSuccessListener {
                Toast.makeText(this, "Gracias por tu calificación", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo guardar la calificación", Toast.LENGTH_SHORT).show()
            }
    }
}