package com.example.tfg.model.dataclass

/**
 * Representa una serie de television obtenida de la API de TMDB.
 *
 * Esta clase implementa MediaItem y añade propiedades especificas para series de TV
 * como temporadas, paises de origen y fechas de estreno. Se utiliza para mostrar
 * informacion detallada de series televisivas en la aplicacion.
 *
 * @property id Identificador unico de la serie en TMDB.
 * @property name Nombre de la serie localizado segun el idioma de la aplicacion.
 *               Puede ser nulo si no hay traduccion disponible.
 * @property original_name Nombre original de la serie en su idioma nativo.
 *                        Puede ser nulo en casos excepcionales.
 * @property backdrop_path Ruta relativa a la imagen de fondo de alta resolucion.
 * @property poster_path Ruta relativa al poster oficial de la serie.
 * @property adult Indica si la serie tiene clasificacion para adultos.
 * @property original_language Codigo ISO del idioma original de la serie.
 * @property popularity indice de popularidad actual en TMDB.
 * @property genres Lista de generos televisivos asociados.
 * @property tagline Eslogan promocional de la serie.
 * @property overview Sinopsis oficial de la serie.
 * @property first_air_date Fecha de estreno del primer episodio en formato ISO (YYYY-MM-DD).
 *                         Puede ser nula para series sin fecha confirmada.
 * @property vote_average Puntuacion media de usuarios (0.0-10.0).
 * @property vote_count Numero total de votos recibidos.
 * @property seasons Lista completa de temporadas que componen la serie.
 *                  Puede ser nula si no se solicita informacion detallada.
 * @property origin_country Lista de codigos ISO de paises donde se produjo la serie.
 *                         Puede ser nula si la informacion no esta disponible.
 * @property media_type Siempre "tv" para instancias de esta clase.
 *
 * @see MediaItem
 * @see Seasons
 * @see Genero
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