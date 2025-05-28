package com.example.tfg.model.dataclass

/**
 * Clase base sellada para elementos multimedia.
 * Implementada por Pelicula y TvShow.
 *
 * @property id Identificador único del elemento
 * @property backdrop_path Ruta de la imagen de fondo (nullable)
 * @property poster_path Ruta del póster (nullable)
 * @property adult Si es contenido para adultos
 * @property original_language Idioma original (código ISO)
 * @property genres Lista de géneros asociados
 * @property tagline Frase destacada (nullable)
 * @property overview Sinopsis (nullable)
 * @property popularity Puntuación de popularidad
 * @property vote_average Puntuación media (0-10)
 * @property vote_count Total de votos
 * @property media_type Tipo de contenido ("movie" o "tv")
 */
sealed class MediaItem {
    abstract val id: Int
    abstract val backdrop_path: String?
    abstract val poster_path: String?
    abstract val adult: Boolean
    abstract val original_language: String
    abstract val genres: List<Genero>
    abstract val tagline: String?
    abstract val overview: String?
    abstract val popularity: Double?
    abstract val vote_average: Double?
    abstract val vote_count: Int?
    abstract val media_type: String?
}