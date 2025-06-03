package com.example.tfg.model.dataclass

/**
 * Creditos de una produccion cinematografica o televisiva.
 *
 * Esta clase almacena la informacion del reparto y equipo tecnico asociado
 * a una pelicula o serie especifica. Se utiliza para mostrar informacion
 * detallada sobre las personas involucradas en la produccion.
 *
 * @property id Identificador unico de la produccion en TMDB.
 * @property cast Lista de personas que participaron en la produccion,
 *               incluyendo actores, directores y equipo tecnico principal.
 *
 * @see People
 */
data class Credits(
    val id: Int,
    val cast: List<People>
)
