package com.ortz.gestionequipaje

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.ortz.gestionequipaje.Destinatario.MenuDestinatario
import com.ortz.gestionequipaje.Encomienda.MenuEncomienda
import com.ortz.gestionequipaje.Equipaje.MenuEquipaje

class MenuPrincipal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)

        val cvTipoEquipaje = findViewById<CardView>(R.id.cvTipoEquipaje)
        val cvDestinatario = findViewById<CardView>(R.id.cvDestinatario)
        val cvEncomienda = findViewById<CardView>(R.id.cvEncomienda)
        val cvCerrarSesion = findViewById<CardView>(R.id.cvCerrarSesion)



        val intentEquipaje = Intent(this, MenuEquipaje::class.java)
        val intentDestinatario = Intent(this, MenuDestinatario::class.java)
        val intentEncomienda = Intent(this, MenuEncomienda::class.java)


        cvTipoEquipaje.setOnClickListener {
            startActivity(intentEquipaje)
        }
        cvDestinatario.setOnClickListener {
            startActivity(intentDestinatario)
        }
        cvEncomienda.setOnClickListener {
            startActivity(intentEncomienda)
        }
        cvCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesion")
                .setMessage("¿Deseas cerrar sesion?")
                .setPositiveButton("Cerrar sesion") { _, _ ->
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

    }
}