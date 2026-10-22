package com.ortz.gestionequipaje.data

import Model.Destinatario
import Model.TipoEquipaje
import Model.Encomienda
import Model.EstadoEncomienda
import com.google.firebase.firestore.FirebaseFirestore

object DataStore {

    val destinatarios = mutableListOf<Destinatario>()
    val tiposEquipaje = mutableListOf<TipoEquipaje>()
    val encomiendas = mutableListOf<Encomienda>()

    // crud destinatario
    fun agregarDestinatario(destinatario: Destinatario) {
        destinatarios.add(destinatario)
    }

    fun obtenerDestinatario(id: String): Destinatario? =
        destinatarios.find { it.id == id }

    fun actualizarDestinatario(destinatario: Destinatario) {
        val index = destinatarios.indexOfFirst { it.id == destinatario.id }
        if (index != -1) destinatarios[index] = destinatario
    }

    fun eliminarDestinatario(id: String) {
        destinatarios.removeAll { it.id == id }
    }

    // crud equipaje
    fun agregarTipoEquipaje(tipo: TipoEquipaje) {
        tiposEquipaje.add(tipo)
    }

    fun obtenerTipoEquipaje(id: String): TipoEquipaje? =
        tiposEquipaje.find { it.id == id }

    fun actualizarTipoEquipaje(tipo: TipoEquipaje) {
        val index = tiposEquipaje.indexOfFirst { it.id == tipo.id }
        if (index != -1) tiposEquipaje[index] = tipo
    }

    fun eliminarTipoEquipaje(id: String) {
        tiposEquipaje.removeAll { it.id == id }
    }

    // crud encomiendas
    fun agregarEncomienda(encomienda: Encomienda) {
        encomiendas.add(encomienda)
    }

    fun obtenerEncomienda(id: String): Encomienda? =
        encomiendas.find { it.id == id }

    fun actualizarEncomienda(encomienda: Encomienda) {
        val index = encomiendas.indexOfFirst { it.id == encomienda.id }
        if (index != -1) encomiendas[index] = encomienda
    }

    fun eliminarEncomienda(id: String) {
        encomiendas.removeAll { it.id == id }
    }
    fun cargarCatalogos() {
        val uid = Sesion.uid()
        if (uid.isEmpty()) return
        val db = FirebaseFirestore.getInstance()

        db.collection("destinatarios").whereEqualTo("usuarioId", uid).get()
            .addOnSuccessListener { resultado ->
                destinatarios.clear()
                for (doc in resultado) {
                    destinatarios.add(
                        Destinatario(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "",
                            direccion = doc.getString("direccion") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            email = doc.getString("email") ?: "",
                            comuna = doc.getString("comuna") ?: ""
                        )
                    )
                }
            }

        db.collection("tiposEquipaje").whereEqualTo("usuarioId", uid).get()
            .addOnSuccessListener { resultado ->
                tiposEquipaje.clear()
                for (doc in resultado) {
                    tiposEquipaje.add(doc.toObject(TipoEquipaje::class.java).copy(id = doc.id))
                }
            }
    }
}