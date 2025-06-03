package com.example.tfg.model.dataclass

/**
 * Representa una temporada de una serie de television.
 *
 * Esta clase encapsula toda la informacion relevante sobre una temporada especifica,
 * incluyendo sus episodios, fechas de emision y metadatos. Se utiliza para organizar
 * y mostrar el contenido de series de TV de manera estructurada.
 *
 * @property air_date Fecha de emision del primer episodio de la temporada en formato ISO (YYYY-MM-DD).
 *                   Puede ser nula si no esta confirmada o disponible.
 * @property episodes Lista completa de episodios que componen esta temporada.
 *                   Puede ser nula si no se ha solicitado informacion detallada de episodios.
 * @property episode_count Numero total de episodios confirmados para esta temporada.
 *                        Siempre disponible incluso si la lista de episodios es nula.
 * @property id Identificador unico de la temporada en TMDB.
 * @property name Nombre oficial de la temporada (ej: "Temporada 1", "Parte Final").
 *               Puede ser nulo si no hay un nombre especifico asignado.
 * @property overview Sinopsis o descripcion general de la temporada.
 *                   Puede ser nula si no esta disponible.
 * @property poster_path Ruta relativa al poster oficial de la temporada.
 *                      Puede ser nula si no hay imagen especifica disponible.
 * @property season_number Numero ordinal de la temporada (empezando desde 1).
 *                        Las temporadas especiales pueden usar el numero 0.
 * @property vote_average Puntuacion media de la temporada basada en valoraciones de usuarios (0.0-10.0).
 *                       Puede ser nula si no hay suficientes valoraciones.
 *
 * @see Episodio
 * @see TvShow
 */
data class Seasons(
    val air_date: String?,
    val episodes: List<Episodio>?,
    val episode_count: Int,
    val id: Int,
    val name: String?,
    val overview: String?,
    val poster_path: String?,
    val season_number: Int,
    val vote_average: Double?
)
