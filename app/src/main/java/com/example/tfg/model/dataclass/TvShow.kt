package com.example.tfg.model.dataclass

/**
 * Datos de una serie de TV.
 *
 * @property id ID único de TMDB
 * @property name Título localizado
 * @property original_name Título original
 * @property backdrop_path Ruta del fondo (nullable)
 * @property poster_path Ruta del póster (nullable)
 * @property adult Si es para adultos
 * @property original_language Idioma original
 * @property popularity Puntuación de popularidad
 * @property genres Lista de géneros
 * @property tagline Frase destacada (nullable)
 * @property overview Sinopsis (nullable)
 * @property first_air_date Fecha de estreno (nullable)
 * @property vote_average Puntuación media (0-10)
 * @property vote_count Total de votos
 * @property seasons Lista de temporadas (nullable)
 * @property origin_country Países de origen (nullable)
 * @property media_type Siempre "tv"
 */
data class TvShow(
    override val id: Int,
    val name: String?,
    val original_name: String?,
    override val backdrop_path: String?,
    override val poster_path: String?,
    override val adult: Boolean,
    override val original_language: String,
    override val popularity: Double?,
    override val genres: List<Genero>,
    override val tagline: String?,
    override val overview: String?,
    val first_air_date: String?,
    override val vote_average: Double?,
    override val vote_count: Int?,
    val seasons: List<Seasons>?,
    val origin_country: List<String>?,
    override var media_type: String?
) : MediaItem()