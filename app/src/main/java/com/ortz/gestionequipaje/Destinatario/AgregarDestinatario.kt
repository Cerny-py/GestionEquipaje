package com.ortz.gestionequipaje.Destinatario

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.ortz.gestionequipaje.R
import com.ortz.gestionequipaje.data.Eventos
import com.ortz.gestionequipaje.data.Red
import com.ortz.gestionequipaje.data.Sesion

class AgregarDestinatario : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var idEditar: String? = null
    private var editandoMascara = false
    private var guardando = false

    private lateinit var edNombre: EditText
    private lateinit var edDireccion: EditText
    private lateinit var edTelefono: EditText
    private lateinit var edEmail: EditText
    private lateinit var edComuna: EditText
    private lateinit var btnGuardar: Button
    private lateinit var progreso: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agregar_destinatario)

        edNombre = findViewById(R.id.edNombreDestinatario)
        edDireccion = findViewById(R.id.edDireccionDestinatario)
        edTelefono = findViewById(R.id.edTelefonoDestinatario)
        edEmail = findViewById(R.id.edEmailDestinatario)
        edComuna = findViewById(R.id.edComunaDestinatario)
        btnGuardar = findViewById(R.id.btnGuardarDestinatario)
        progreso = findViewById(R.id.progresoDestinatario)
        val btnVolver = findViewById<Button>(R.id.btnVolverMenuDestinatario)
        val tvTitulo = findViewById<TextView>(R.id.txtTituloLogIn)

        edTelefono.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (editandoMascara) return
                editandoMascara = true

                val soloNumeros = s.toString().replace(Regex("[^\\d]"), "")
                val formateado = formatearTelefono(soloNumeros)

                s?.replace(0, s.length, formateado)
                editandoMascara = false
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        idEditar = intent.getStringExtra("idDestinatario")

        if (idEditar != null) {
            edNombre.setText(intent.getStringExtra("nombre"))
            edDireccion.setText(intent.getStringExtra("direccion"))
            edTelefono.setText(intent.getStringExtra("telefono"))
            edEmail.setText(intent.getStringExtra("email"))
            edComuna.setText(intent.getStringExtra("comuna"))
            btnGuardar.text = "Actualizar"
            tvTitulo.text = "Editar destinatario"
        }

        btnGuardar.setOnClickListener { guardarDestinatario() }
        btnVolver.setOnClickListener { finish() }
    }

    private fun formatearTelefono(numeros: String): String {
        var digitos = numeros
        if (!digitos.startsWith("56")) {
            digitos = "56$digitos"
        }
        digitos = digitos.take(11)

        val sb = StringBuilder("+")
        for (i in digitos.indices) {
            sb.append(digitos[i])
            when (i) {
                1 -> sb.append(" ")
                2 -> sb.append(" ")
                6 -> sb.append(" ")
            }
        }
        return sb.toString().trim()
    }

    private fun mostrarCarga(estado: Boolean) {
        guardando = estado
        progreso.visibility = if (estado) View.VISIBLE else View.GONE
        btnGuardar.isEnabled = !estado
    }

    private fun guardarDestinatario() {
        if (guardando) return

        val nombre = edNombre.text.toString().trim()
        val direccion = edDireccion.text.toString().trim()
        val telefono = edTelefono.text.toString().trim()
        val email = edEmail.text.toString().trim()
        val comuna = edComuna.text.toString().trim()

        if (nombre.isEmpty()) {
            edNombre.error = "Ingresa el nombre"
            edNombre.requestFocus()
            return
        }
        if (direccion.isEmpty()) {
            edDireccion.error = "Ingresa la dirección"
            edDireccion.requestFocus()
            return
        }
        if (telefono.filter { it.isDigit() }.length != 11) {
            edTelefono.error = "Ingresa un teléfono válido (+56 9 1234 5678)"
            edTelefono.requestFocus()
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edEmail.error = "Ingresa un correo válido (nombre@correo.cl)"
            edEmail.requestFocus()
            return
        }
        if (comuna.isEmpty()) {
            edComuna.error = "Ingresa la comuna"
            edComuna.requestFocus()
            return
        }

        val usuarioId = Sesion.uid()
        if (usuarioId.isEmpty()) {
            Toast.makeText(this, "No hay sesión de Firebase activa, reintenta", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Red.hayInternet(this)) {
            Toast.makeText(this, "Sin conexión a internet", Toast.LENGTH_SHORT).show()
            return
        }

        val destinatario = hashMapOf(
            "nombre" to nombre,
            "direccion" to direccion,
            "telefono" to telefono,
            "email" to email,
            "comuna" to comuna,
            "usuarioId" to usuarioId
        )

        mostrarCarga(true)

        val id = idEditar
        if (id == null) {
            db.collection("destinatarios").add(destinatario)
                .addOnSuccessListener {
                    Eventos.registrar(this, "registro_creado", "destinatario")
                    Toast.makeText(this, "Destinatario registrado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e -> errorAlGuardar(e, "registrar") }
        } else {
            db.collection("destinatarios").document(id).set(destinatario)
                .addOnSuccessListener {
                    Eventos.registrar(this, "registro_actualizado", "destinatario")
                    Toast.makeText(this, "Destinatario actualizado correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e -> errorAlGuardar(e, "actualizar") }
        }
    }

    private fun errorAlGuardar(e: Exception, accion: String) {
        mostrarCarga(false)
        FirebaseCrashlytics.getInstance().recordException(e)
        Toast.makeText(this, "No se pudo $accion el destinatario, intenta nuevamente", Toast.LENGTH_LONG).show()
    }
}