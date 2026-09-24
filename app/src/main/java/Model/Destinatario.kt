package Model
import java.util.UUID

data class Destinatario(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val direccion: String,
    val telefono: String
)
