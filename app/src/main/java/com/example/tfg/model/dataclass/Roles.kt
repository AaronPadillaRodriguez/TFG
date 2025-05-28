package com.example.tfg.model.dataclass

/**
 * Rol especifico de un actor.
 *
 * @property credit_id ID del credito.
 * @property character Nombre del personaje.
 * @property episode_count Episodios participados.
 */
data class Roles(
    val credit_id: String,
    val character: String,
    val episode_count: Int
) {
    override fun toString(): String {
        return "$credit_id;$character;$episode_count"
    }
}


