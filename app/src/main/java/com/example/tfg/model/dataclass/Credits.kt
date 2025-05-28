package com.example.tfg.model.dataclass

/**
 * Creditos de una produccion (reparto principal).
 *
 * @property id ID de la produccion.
 * @property cast Lista de actores/equipo tecnico.
 */
data class Credits(
    val id: Int,
    val cast: List<People>
)
