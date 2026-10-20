package com.ortz.gestionequipaje

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val edNombreLogIn = findViewById<EditText>(R.id.edNombreLogIn)
        val edContraseñaLogIn = findViewById<EditText>(R.id.edContraseñaLogIn)
        val btnIniciarSesion = findViewById<Button>(R.id.btnInicioSesion)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        edContraseñaLogIn.setOnTouchListener { _, event ->
            val DRAWABLE_END = 2
            val drawableEnd = edContraseñaLogIn.compoundDrawables[DRAWABLE_END]

            if (drawableEnd != null) {
                val iconWidth = drawableEnd.bounds.width()
                val tocandoIcono = event.rawX >= (edContraseñaLogIn.right - iconWidth - edContraseñaLogIn.paddingEnd)

                if (tocandoIcono) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            edContraseñaLogIn.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                            edContraseñaLogIn.setSelection(edContraseñaLogIn.text.length)
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            edContraseñaLogIn.inputType =
                                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                            edContraseñaLogIn.setSelection(edContraseñaLogIn.text.length)
                            return@setOnTouchListener true
                        }
                    }
                }
            }
            false
        }

        // Login por RAM (se mantiene), pero asegurando una sesión de Firebase por debajo
        btnIniciarSesion.setOnClickListener {
            if (edNombreLogIn.text.toString() == "Franco" && edContraseñaLogIn.text.toString() == "123") {
                Toast.makeText(this, "Inicio de sesion correcto", Toast.LENGTH_LONG).show()
                if (auth.currentUser != null) {
                    irAlMenu(cerrar = false)
                } else {
                    btnIniciarSesion.isEnabled = false
                    auth.signInAnonymously()
                        .addOnFailureListener {
                            Toast.makeText(this, "Sin conexión a la nube: los datos no se guardarán", Toast.LENGTH_LONG).show()
                        }
                        .addOnCompleteListener {
                            btnIniciarSesion.isEnabled = true
                            irAlMenu(cerrar = false)
                        }
                }
            } else {
                Toast.makeText(this, "Error, datos invalidos", Toast.LENGTH_LONG).show()
                edNombreLogIn.text.clear()
                edContraseñaLogIn.text.clear()
            }
        }

        btnGoogle.setOnClickListener { loginConGoogle(btnGoogle) }
    }

    override fun onStart() {
        super.onStart()
        val usuario = auth.currentUser
        if (usuario != null && !usuario.isAnonymous) irAlMenu(cerrar = true)
    }

    private fun loginConGoogle(boton: Button) {
        val opcion = GetSignInWithGoogleOption.Builder(getString(R.string.default_web_client_id)).build()
        val pedido = GetCredentialRequest.Builder().addCredentialOption(opcion).build()

        boton.isEnabled = false
        lifecycleScope.launch {
            try {
                val cred = CredentialManager.create(this@MainActivity)
                    .getCredential(this@MainActivity, pedido).credential
                val token = GoogleIdTokenCredential.createFrom((cred as CustomCredential).data).idToken
                auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .addOnSuccessListener { irAlMenu(cerrar = true) }
                    .addOnFailureListener {
                        boton.isEnabled = true
                        Toast.makeText(this@MainActivity, "Falló la autenticación. Revise su conexión", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                boton.isEnabled = true
                Toast.makeText(this@MainActivity, "No se pudo iniciar sesión con Google", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun irAlMenu(cerrar: Boolean) {
        startActivity(Intent(this, MenuPrincipal::class.java))
        if (cerrar) finish()
    }
}