package com.ortz.gestionequipaje.Encomienda

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ortz.gestionequipaje.Destinatario.AgregarDestinatario
import com.ortz.gestionequipaje.Destinatario.VerDestinatario
import com.ortz.gestionequipaje.MenuPrincipal
import com.ortz.gestionequipaje.R

class MenuEncomienda : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_encomienda)


        val cvAgregarEncomienda = findViewById<CardView>(R.id.cvAgregarEncomienda)
        val cvVerEncomienda = findViewById<CardView>(R.id.cvVerEncomiendas)
        val cvVolverMenuPrincipal = findViewById<CardView>(R.id.cvVolverMenuPrincipal)


        val intentAgregarEncomienda = Intent(this, AgregarEncomienda::class.java)
        val intentVerEncomienda = Intent(this, VerEncomienda::class.java)

        cvVolverMenuPrincipal.setOnClickListener {
            finish()
        }
        cvAgregarEncomienda.setOnClickListener {
            startActivity(intentAgregarEncomienda)
        }
        cvVerEncomienda.setOnClickListener {
            startActivity(intentVerEncomienda)
        }

    }
}