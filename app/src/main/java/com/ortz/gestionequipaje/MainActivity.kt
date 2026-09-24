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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val edNombreLogIn = findViewById<EditText>(R.id.edNombreLogIn)
        val edContraseñaLogIn = findViewById<EditText>(R.id.edContraseñaLogIn)
        val btnIniciarSesion = findViewById<Button>(R.id.btnInicioSesion)

        val intentPrincipal = Intent(this, MenuPrincipal::class.java)

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

        btnIniciarSesion.setOnClickListener {
            if (edNombreLogIn.text.toString().equals("Franco") && edContraseñaLogIn.text.toString().equals("123")) {
                Toast.makeText(this, "Inicio de sesion correcto", Toast.LENGTH_LONG).show()
                startActivity(intentPrincipal)
            } else {
                Toast.makeText(this, "Error, datos invalidos", Toast.LENGTH_LONG).show()
                edNombreLogIn.text.clear()
                edContraseñaLogIn.text.clear()
                return@setOnClickListener
            }
        }
    }
}