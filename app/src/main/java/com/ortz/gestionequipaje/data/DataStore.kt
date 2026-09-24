package com.ortz.gestionequipaje.data

import Model.Destinatario
import Model.TipoEquipaje
import Model.Encomienda
import Model.EstadoEncomienda

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
}