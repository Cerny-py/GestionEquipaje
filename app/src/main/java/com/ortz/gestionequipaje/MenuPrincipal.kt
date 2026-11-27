package com.ortz.gestionequipaje

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.ortz.gestionequipaje.Admin.AdminRepartidores
import com.ortz.gestionequipaje.Admin.EntregasActivity
import com.ortz.gestionequipaje.Destinatario.MenuDestinatario
import com.ortz.gestionequipaje.Encomienda.MenuEncomienda
import com.ortz.gestionequipaje.Equipaje.MenuEquipaje
import com.ortz.gestionequipaje.data.Notificaciones
import com.ortz.gestionequipaje.data.Rol

class MenuPrincipal : AppCompatActivity() {

    private lateinit var gridMenu: GridLayout
    private lateinit var cvTipoEquipaje: CardView
    private lateinit var cvDestinatario: CardView
    private lateinit var cvEncomienda: CardView
    private lateinit var cvRepartidores: CardView
    private lateinit var cvEntregas: CardView
    private lateinit var cvCerrarSesion: CardView

    private val pedirPermiso = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (!concedido) Toast.makeText(this, "Sin permiso no verás las notificaciones", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)

        cvTipoEquipaje = findViewById(R.id.cvTipoEquipaje)
        cvDestinatario = findViewById(R.id.cvDestinatario)
        cvEncomienda = findViewById(R.id.cvEncomienda)
        cvRepartidores = findViewById(R.id.cvRepartidores)
        cvEntregas = findViewById(R.id.cvEntregas)
        cvCerrarSesion = findViewById(R.id.cvCerrarSesion)
        gridMenu = cvTipoEquipaje.parent as GridLayout

        cvTipoEquipaje.setOnClickListener {
            startActivity(Intent(this, MenuEquipaje::class.java))
        }
        cvDestinatario.setOnClickListener {
            startActivity(Intent(this, MenuDestinatario::class.java))
        }
        cvEncomienda.setOnClickListener {
            startActivity(Intent(this, MenuEncomienda::class.java))
        }
        cvRepartidores.setOnClickListener {
            startActivity(Intent(this, AdminRepartidores::class.java))
        }
        cvEntregas.setOnClickListener {
            startActivity(Intent(this, EntregasActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        Rol.detectar { rol ->
            if (isFinishing || isDestroyed) return@detectar

            val esAdmin = rol == Rol.ADMIN
            val esRepartidor = rol == Rol.REPARTIDOR
            val visibilidadCliente = if (esRepartidor) View.GONE else View.VISIBLE

            cvTipoEquipaje.visibility = visibilidadCliente
            cvDestinatario.visibility = visibilidadCliente
            cvEncomienda.visibility = visibilidadCliente
            cvRepartidores.visibility = if (esAdmin) View.VISIBLE else View.GONE
            cvEntregas.visibility = if (esAdmin || esRepartidor) View.VISIBLE else View.GONE

            val tarjetas = listOf(
                cvTipoEquipaje,
                cvDestinatario,
                cvEncomienda,
                cvRepartidores,
                cvEntregas,
                cvCerrarSesion
            )

            val densidad = resources.displayMetrics.density
            val margen = (12 * densidad).toInt()
            val alto = (150 * densidad).toInt()

            gridMenu.removeAllViews()
            for (tarjeta in tarjetas) {
                if (tarjeta.visibility == View.VISIBLE) {
                    val params = GridLayout.LayoutParams(
                        GridLayout.spec(GridLayout.UNDEFINED),
                        GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    ).apply {
                        width = 0
                        height = alto
                        setMargins(margen, margen, margen, margen)
                    }
                    gridMenu.addView(tarjeta, params)
                }
            }
        }
    }
}