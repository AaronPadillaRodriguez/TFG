package com.example.tfg.model.dataclass

/**
 * Representacion en cadena del genero.
 *
 * Sobrescribe el metodo toString() para devolver directamente el nombre
 * del genero, facilitando su uso en spinners, listas y otros componentes
 * de la interfaz de usuario que requieren una representacion textual.
 *
 * @return El nombre del genero como String.
 */
data class Genero(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name // Facilita el uso en spinners y listas de UI
}
