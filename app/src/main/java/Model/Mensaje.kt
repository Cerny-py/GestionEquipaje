package Model

import com.google.firebase.Timestamp

data class Mensaje(
    val id: String = "",
    val texto: String = "",
    val emisor: String = "",
    val usuarioId: String = "",
    val fecha: Timestamp? = null
)