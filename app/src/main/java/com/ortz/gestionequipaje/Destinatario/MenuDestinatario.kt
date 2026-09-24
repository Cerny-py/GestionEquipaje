package com.ortz.gestionequipaje.Destinatario

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.ortz.gestionequipaje.Destinatario.AgregarDestinatario
import com.ortz.gestionequipaje.Destinatario.VerDestinatario
import com.ortz.gestionequipaje.Equipaje.AgregarEquipaje
import com.ortz.gestionequipaje.Equipaje.VerEquipaje
import com.ortz.gestionequipaje.MenuPrincipal
import com.ortz.gestionequipaje.R

class MenuDestinatario : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_destinatarios)

        val cvAgregarDestinatario = findViewById<CardView>(R.id.cvAgregarDestinatario)
        val cvVerDestinatario = findViewById<CardView>(R.id.cvVerDestinatario)
        val cvVolverMenuPrincipal = findViewById<CardView>(R.id.cvVolverMenuPrincipal)

        val intentAgregarDestinatario = Intent(this, AgregarDestinatario::class.java)
        val intentVerDestinatario = Intent(this, VerDestinatario::class.java)

        cvAgregarDestinatario.setOnClickListener {
            startActivity(intentAgregarDestinatario)
        }
        cvVerDestinatario.setOnClickListener {
            startActivity(intentVerDestinatario)
        }
        cvVolverMenuPrincipal.setOnClickListener {
            finish()
        }
    }
}