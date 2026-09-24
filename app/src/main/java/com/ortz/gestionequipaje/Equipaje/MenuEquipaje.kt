package com.ortz.gestionequipaje.Equipaje

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.ortz.gestionequipaje.MenuPrincipal
import com.ortz.gestionequipaje.R

class MenuEquipaje : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_equipaje)
        val cvAgregarEquipaje = findViewById<CardView>(R.id.cvAgregarEquipaje)
        val cvVerEquipaje = findViewById<CardView>(R.id.cvVerEquipaje)
        val cvVolverMenuPrincipal = findViewById<CardView>(R.id.cvVolverMenuPrincipal)


        val intentAgregarEquipaje = Intent(this, AgregarEquipaje::class.java)
        val intentVerEquipaje = Intent(this, VerEquipaje::class.java)

        cvAgregarEquipaje.setOnClickListener {
            startActivity(intentAgregarEquipaje)
        }

        cvVerEquipaje.setOnClickListener {
            startActivity(intentVerEquipaje)
        }

        cvVolverMenuPrincipal.setOnClickListener {
            finish()
        }

    }
}