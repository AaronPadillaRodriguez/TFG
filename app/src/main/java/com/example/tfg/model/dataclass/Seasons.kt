package com.example.tfg.model.dataclass

/**
 * Informacion de una temporada de serie TV.
 *
 * @property air_date Fecha de emision.
 * @property episode_count Cantidad de episodios.
 * @property id ID unico.
 * @property name Nombre de la temporada.
 * @property overview Sinopsis.
 * @property poster_path Ruta del poster.
 * @property season_number Numero de temporada.
 * @property vote_average Puntuacion media.
 */
data class Seasons(
    val air_date: String?,
    val episode_count: Int,
    val id: Int,
    val name: String?,
    val overview: String?,
    val poster_path: String?,
    val season_number: Int,
    val vote_average: Double?
)
