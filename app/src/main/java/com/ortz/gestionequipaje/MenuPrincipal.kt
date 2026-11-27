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
import com.ortz.gestionequipaje.Admin.AdminRepartidores
import com.ortz.gestionequipaje.Admin.EntregasActivity
import com.ortz.gestionequipaje.data.Rol

class MenuPrincipal : AppCompatActivity() {
    private val pedirPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) Toast.makeText(this, "Sin permiso no verás las notificaciones", Toast.LENGTH_LONG).show()
    }

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
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
        findViewById<CardView>(R.id.cvRepartidores).setOnClickListener {
            startActivity(Intent(this, AdminRepartidores::class.java))
        }
        findViewById<CardView>(R.id.cvEntregas).setOnClickListener {
            startActivity(Intent(this, EntregasActivity::class.java))
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
    override fun onResume() {
        super.onResume()
        Rol.detectar { rol ->
            if (isFinishing) return@detectar

            val esAdmin = rol == Rol.ADMIN
            val esRepartidor = rol == Rol.REPARTIDOR
            val visibilidadCliente = if (esRepartidor) View.GONE else View.VISIBLE

            findViewById<CardView>(R.id.cvTipoEquipaje).visibility = visibilidadCliente
            findViewById<CardView>(R.id.cvDestinatario).visibility = visibilidadCliente
            findViewById<CardView>(R.id.cvEncomienda).visibility = visibilidadCliente
            findViewById<CardView>(R.id.cvRepartidores).visibility =
                if (esAdmin) View.VISIBLE else View.GONE
            findViewById<CardView>(R.id.cvEntregas).visibility =
                if (esAdmin || esRepartidor) View.VISIBLE else View.GONE
        }
    }
}