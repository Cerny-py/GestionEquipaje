package com.ortz.gestionequipaje.Encomienda

import Model.Mensaje
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Sesion

class ChatEncomienda : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val mensajes = mutableListOf<Mensaje>()

    private var idEncomienda = ""
    private var modoRepartidor = false
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
    private lateinit var llControles: LinearLayout
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
        val swRepartidor = findViewById<SwitchCompat>(R.id.swRepartidor)
        val btnEnTransito = findViewById<Button>(R.id.btnEnTransito)
        val btnEntregada = findViewById<Button>(R.id.btnEntregada)
        val btnVolver = findViewById<Button>(R.id.btnVolverChat)
        tvEstado = findViewById(R.id.tvEstadoChat)
        tvSinMensajes = findViewById(R.id.tvSinMensajes)
        tvChatCerrado = findViewById(R.id.tvChatCerrado)
        llControles = findViewById(R.id.llControlesRepartidor)
        rvMensajes = findViewById(R.id.rvMensajes)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviarMensaje)

        tvTitulo.text = "Chat: ${intent.getStringExtra("destinatarioNombre") ?: "encomienda"}"

        val manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        rvMensajes.layoutManager = manager
        adapter = MensajeAdapter(mensajes)
        rvMensajes.adapter = adapter

        swRepartidor.setOnCheckedChangeListener { _, activado ->
            modoRepartidor = activado
            adapter.miRol = if (activado) "repartidor" else "cliente"
            adapter.notifyDataSetChanged()
            llControles.visibility = if (activado) View.VISIBLE else View.GONE
            revisarCalificacion()
        }

        btnEnviar.setOnClickListener { enviarMensaje() }
        btnEnTransito.setOnClickListener { cambiarEstado("en_transito") }
        btnEntregada.setOnClickListener { cambiarEstado("entregado") }
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
                calificacionActual = (doc.getLong("calificacion") ?: 0L).toInt()
                entregada = estado.equals("entregado", ignoreCase = true)

                tvEstado.text = "Estado: ${estado.uppercase()}"
                tvChatCerrado.visibility = if (entregada) View.VISIBLE else View.GONE
                etMensaje.isEnabled = !entregada
                btnEnviar.isEnabled = !entregada && !enviando

                revisarCalificacion()
            }
    }

    private fun enviarMensaje() {
        if (enviando || entregada) return

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
            "emisor" to (if (modoRepartidor) "repartidor" else "cliente"),
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
        btnEnviar.isEnabled = !entregada
    }

    private fun cambiarEstado(nuevoEstado: String) {
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("encomiendas").document(idEncomienda).update("estado", nuevoEstado)
            .addOnSuccessListener {
                Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "No se pudo cambiar el estado", Toast.LENGTH_SHORT).show()
            }
    }

    private fun revisarCalificacion() {
        if (entregada && calificacionActual == 0 && !modoRepartidor && !dialogoMostrado) {
            dialogoMostrado = true
            mostrarCalificacion()
        }
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