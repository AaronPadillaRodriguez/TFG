package com.example.tfg.model.dataclass

/**
 * Clase base sellada para elementos multimedia de TMDB.
 *
 * Define la estructura comun para todos los tipos de contenido multimedia
 * (peliculas y series de TV). Al ser una clase sellada, garantiza que solo
 * las subclases especificas (Pelicula, TvShow) puedan heredar de ella,
 * proporcionando seguridad de tipos en operaciones polimorficas.
 *
 * @property id Identificador unico del elemento multimedia en TMDB.
 * @property backdrop_path Ruta relativa a la imagen de fondo de alta resolucion.
 *                        Puede ser nula si no esta disponible.
 * @property poster_path Ruta relativa al poster oficial del contenido.
 *                      Puede ser nula si no esta disponible.
 * @property adult Indica si el contenido esta clasificado para adultos unicamente.
 * @property original_language Codigo ISO 639-1 del idioma original (ej: "en", "es").
 * @property genres Lista de generos asociados al contenido multimedia.
 * @property tagline Frase promocional o eslogan del contenido. Puede ser nula.
 * @property overview Sinopsis o descripcion general del contenido. Puede ser nula.
 * @property popularity indice de popularidad calculado por TMDB basado en visualizaciones
 *                     y interacciones recientes.
 * @property vote_average Puntuacion media de los usuarios en escala 0-10.
 *                       Puede ser nula si no hay valoraciones suficientes.
 * @property vote_count Numero total de valoraciones recibidas.
 *                     Puede ser nulo para contenido sin valoraciones.
 * @property media_type Tipo de contenido multimedia: "movie" para peliculas, "tv" para series.
 *
 * @see Pelicula
 * @see TvShow
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