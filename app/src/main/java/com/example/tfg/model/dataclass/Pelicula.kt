package com.example.tfg.model.dataclass

/**
 * Datos de una película.
 *
 * @property id ID único de TMDB
 * @property title Título localizado
 * @property original_title Título original
 * @property backdrop_path Ruta del fondo (nullable)
 * @property poster_path Ruta del póster (nullable)
 * @property adult Si es para adultos
 * @property original_language Idioma original
 * @property popularity Puntuación de popularidad
 * @property genres Lista de géneros
 * @property tagline Frase destacada (nullable)
 * @property overview Sinopsis (nullable)
 * @property runtime Duración en minutos (nullable)
 * @property release_date Fecha de estreno (nullable)
 * @property video Si tiene video asociado (nullable)
 * @property vote_average Puntuación media (0-10)
 * @property vote_count Total de votos
 * @property media_type Siempre "movie"
 */
data class Pelicula(
    override val id: Int,
    val title: String?,
    val original_title: String?,
    override val backdrop_path: String?,
    override val poster_path: String?,
    override val adult: Boolean,
    override val original_language: String,
    override val popularity: Double?,
    override val genres: List<Genero>,
    override val tagline: String?,
    override val overview: String?,
    val runtime: Int?,
    val release_date: String?,
    val video: Boolean?,
    override val vote_average: Double?,
    override val vote_count: Int?,
    override var media_type: String?
) : MediaItem()
