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
import android.content.pm.ApplicationInfo
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import android.Manifest
import android.os.Build
import android.util.Log
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.ortz.gestionequipaje.data.Notificaciones
import com.ortz.gestionequipaje.data.DataStore

class MenuPrincipal : AppCompatActivity() {
    private val pedirPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) Toast.makeText(this, "Sin permiso no verás las notificaciones", Toast.LENGTH_LONG).show()
    }
    override fun onResume() {
        super.onResume()
        DataStore.cargarCatalogos()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)


        val cvTipoEquipaje = findViewById<CardView>(R.id.cvTipoEquipaje)
        val cvDestinatario = findViewById<CardView>(R.id.cvDestinatario)
        val cvEncomienda = findViewById<CardView>(R.id.cvEncomienda)
        val cvCerrarSesion = findViewById<CardView>(R.id.cvCerrarSesion)
        val cvDiagnostico = findViewById<CardView>(R.id.cvDiagnostico)

        val esDiagnostico = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!esDiagnostico) {
            cvDiagnostico.visibility = View.GONE
        }
        cvDiagnostico.setOnClickListener {
            startActivity(Intent(this, DiagnosticoActivity::class.java))
        }



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
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        Notificaciones.crearCanal(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM_TOKEN", token)
        }


    }
}