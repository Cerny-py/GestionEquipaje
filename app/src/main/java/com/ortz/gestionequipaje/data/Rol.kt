package com.ortz.gestionequipaje.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object Rol {

    const val ADMIN = "admin"
    const val REPARTIDOR = "repartidor"
    const val CLIENTE = "cliente"

    var actual = CLIENTE
        private set

    fun email(): String {
        return FirebaseAuth.getInstance().currentUser?.email?.lowercase() ?: ""
    }

    fun detectar(alTerminar: (String) -> Unit) {
        val correo = email()
        if (correo.isEmpty()) {
            actual = CLIENTE
            alTerminar(actual)
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("administradores").document(correo).get()
            .addOnSuccessListener { admin ->
                if (admin.exists()) {
                    actual = ADMIN
                    alTerminar(actual)
                } else {
                    db.collection("repartidores").document(correo).get()
                        .addOnSuccessListener { repartidor ->
                            val activo = repartidor.exists() && repartidor.getBoolean("activo") == true
                            actual = if (activo) REPARTIDOR else CLIENTE
                            alTerminar(actual)
                        }
                        .addOnFailureListener {
                            actual = CLIENTE
                            alTerminar(actual)
                        }
                }
            }
            .addOnFailureListener {
                actual = CLIENTE
                alTerminar(actual)
            }
    }
}