package Model
import java.time.LocalDate
import java.util.UUID

data class Encomienda(
    val id: String = UUID.randomUUID().toString(),
    val clienteId: String,
    val destinatarioId: String,
    val tipoEquipajeId: String,
    val peso: Double,
    val valorDeclarado: Double,
    val tarifaCalculada: Double,
    val fechaRegistro: String = LocalDate.now().toString()
)
