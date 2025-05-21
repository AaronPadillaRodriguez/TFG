package com.example.tfg.model.dataclass

/**
 *
 * Esta clase de datos hereda de [MediaItem] y contiene información específica sobre programas
 * de televisión obtenidos generalmente desde una API externa.
 *
 * @property id Identificador único del programa de TV.
 * @property name Nombre del programa en el idioma configurado en la aplicación.
 * @property original_name Nombre original del programa en su idioma de origen.
 * @property overview Descripción general o sinopsis del programa.
 * @property backdrop_path Ruta relativa a la imagen de fondo, puede ser nula si no está disponible.
 * @property poster_path Ruta relativa al póster del programa, puede ser nula si no está disponible.
 * @property adult Indica si el programa está clasificado para adultos.
 * @property original_language Código ISO del idioma original del programa.
 * @property genres Lista de géneros a los que pertenece el programa.
 * @property popularity Puntuación de popularidad del programa.
 * @property first_air_date Fecha de primera emisión en formato de cadena (generalmente YYYY-MM-DD).
 * @property vote_average Promedio de votos o calificación del programa.
 * @property vote_count Número total de votos recibidos.
 * @property origin_country Lista de códigos de países de origen del programa.
 * @property media_type Tipo de medio, generalmente "tv" para esta clase.
 */
data class TvShow(
    override val id: Int,
    val name: String,
    val original_name: String,
    override val backdrop_path: String?,
    override val poster_path: String?,
    override val adult: Boolean,
    override val original_language: String,
    override val popularity: Double,
    override val genres: List<Genero>,
    override val tagline: String,
    override val overview: String,
    val first_air_date: String,
    override val vote_average: Double,
    override val vote_count: Int,
    val origin_country: List<String>,
    override var media_type: String
) : MediaItem()