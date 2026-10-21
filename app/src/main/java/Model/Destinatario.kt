package Model

data class Destinatario(
    val id: String = "",
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val email: String,
    val comuna: String
)
