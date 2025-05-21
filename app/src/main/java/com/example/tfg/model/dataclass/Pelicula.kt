package com.example.tfg.model.dataclass


/**
 *
 * Esta clase implementa la clase base sellada MediaItem y define propiedades
 * específicas de películas además de las heredadas de la clase base.
 * Cada instancia contiene información detallada sobre una película individual,
 * como su título, sinopsis, fecha de lanzamiento, etc.
 *
 * @property id Identificador único de la película en la API TMDB
 * @property title El título localizado (según la configuración regional) de la película
 * @property original_title El título original de la película en su idioma de producción
 * @property overview Descripción o sinopsis de la película
 * @property backdrop_path Ruta relativa a la imagen de fondo, puede ser nula
 * @property poster_path Ruta relativa al póster de la película, puede ser nula
 * @property adult Indica si la película está clasificada para adultos (true) o no (false)
 * @property original_language El código ISO 639-1 del idioma original de la película
 * @property genres Lista de géneros asociados con la película
 * @property popularity Valor numérico que representa la popularidad de la película
 * @property release_date Fecha de estreno de la película en formato ISO 8601 (YYYY-MM-DD)
 * @property video Indica si hay videos disponibles para esta película
 * @property vote_average Puntuación media de valoraciones de usuarios (0-10)
 * @property vote_count Número total de valoraciones recibidas
 * @property media_type Tipo de medio, siempre "movie" para películas
 */
data class Pelicula(
    override val id: Int,
    val title: String,
    val original_title: String,
    override val backdrop_path: String?,
    override val poster_path: String?,
    override val adult: Boolean,
    override val original_language: String,
    override val popularity: Double,
    override val genres: List<Genero>,
    override val tagline: String,
    override val overview: String,
    val runtime: Int,
    val release_date: String,
    val video: Boolean,
    override val vote_average: Double,
    override val vote_count: Int,
    override var media_type: String
) : MediaItem()
