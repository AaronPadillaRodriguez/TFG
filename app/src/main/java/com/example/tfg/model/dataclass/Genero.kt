package com.example.tfg.model.dataclass

/**
 * Representa un genero cinematografico/televisivo.
 *
 * @property id ID unico del genero.
 * @property name Nombre del genero.
 */
data class Genero(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name
}
