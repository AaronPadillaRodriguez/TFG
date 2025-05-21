package com.example.tfg.model.dataclass

/**
 *
 * Esta clase encapsula la estructura de respuesta estándar de la API TMDB, que incluye
 * información de paginación (página actual, total de páginas), conteo de resultados
 * y la lista de elementos multimedia (películas o programas de TV).
 *
 * @property page El número de página actual de los resultados
 * @property results La lista de elementos multimedia (películas o programas de TV)
 * @property total_pages El número total de páginas disponibles para la consulta
 * @property total_results El número total de elementos multimedia que coinciden con la consulta
 */
data class ApiResponse(
    val page: Int,
    val results: List<MediaItem>?,
    val total_pages: Int,
    val total_results: Int
)

/**
 *
 * Esta clase abstracta define una estructura común para todos los tipos de medios
 * (películas y programas de TV) que pueden ser obtenidos de la API TMDB.
 * Al ser una clase sellada, solo permite subclases específicas (Pelicula y TvShow)
 * definidas dentro del mismo archivo o módulo.
 *
 * @property id Identificador único del elemento multimedia en la API TMDB
 * @property backdrop_path Ruta relativa a la imagen de fondo, puede ser nula si no está disponible
 * @property poster_path Ruta relativa al póster del elemento multimedia, puede ser nula
 * @property adult Indica si el contenido es para adultos (true) o no (false)
 * @property original_language El código ISO 639-1 del idioma original del contenido
 * @property genre_ids Lista de IDs de géneros asociados con el elemento multimedia
 * @property popularity Valor numérico que representa la popularidad del elemento
 * @property vote_average Puntuación media de valoraciones de usuarios (0-10)
 * @property vote_count Número total de valoraciones recibidas
 * @property media_type Tipo de medio ("movie" para películas, "tv" para programas de TV)
 */
sealed class MediaItem {
    abstract val id: Int
    abstract val backdrop_path: String?
    abstract val poster_path: String?
    abstract val adult: Boolean
    abstract val original_language: String
    abstract val genres: List<Genero>
    abstract val tagline: String
    abstract val overview: String
    abstract val popularity: Double
    abstract val vote_average: Double
    abstract val vote_count: Int
    abstract val media_type: String
}