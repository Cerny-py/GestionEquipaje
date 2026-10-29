package Model

data class Encomienda(
    val id: String = "",
    val destinatarioId: String = "",
    val destinatarioNombre: String = "",
    val tipoEquipajeId: String = "",
    val tipoEquipajeNombre: String = "",
    val peso: Double = 0.0,
    val valorDeclarado: Double = 0.0,
    val tarifaCalculada: Double = 0.0,
    val fechaRegistro: String = "",
    val usuarioId: String = ""
)