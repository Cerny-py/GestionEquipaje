package Model

data class TipoEquipaje(
    val id: String = "",
    val nombre: String = "",
    val pesoMaximo: Double = 0.0,
    val tarifaBase: Double = 0.0,
    val descripcion: String = "",
    val requiereEmbalajeEspecial: Boolean = false,
    val usuarioId: String = ""
)
