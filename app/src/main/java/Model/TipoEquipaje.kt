package Model
import java.util.UUID

data class TipoEquipaje(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val pesoMaximo: Double,
    val tarifaBase: Double
)
