package com.example.tfg.model.dataclass

/**
 * Representa una pelicula obtenida de la API de TMDB.
 *
 * Esta clase implementa MediaItem y añade propiedades especificas para peliculas
 * como duracion, fecha de estreno y titulos localizados. Se utiliza para mostrar
 * informacion detallada de peliculas en la aplicacion.
 *
 * @property id Identificador unico de la pelicula en TMDB.
 * @property title Titulo de la pelicula localizado segun el idioma de la aplicacion.
 *               Puede ser nulo si no hay traduccion disponible.
 * @property original_title Titulo original de la pelicula en su idioma nativo.
 *                         Puede ser nulo en casos excepcionales.
 * @property backdrop_path Ruta relativa a la imagen de fondo de alta resolucion.
 * @property poster_path Ruta relativa al poster oficial de la pelicula.
 * @property adult Indica si la pelicula tiene clasificacion para adultos.
 * @property original_language Codigo ISO del idioma original de la pelicula.
 * @property popularity indice de popularidad actual en TMDB.
 * @property genres Lista de generos cinematograficos asociados.
 * @property tagline Eslogan promocional de la pelicula.
 * @property overview Sinopsis oficial de la pelicula.
 * @property runtime Duracion total de la pelicula en minutos.
 *                  Puede ser nulo si no esta especificado.
 * @property release_date Fecha de estreno en formato ISO (YYYY-MM-DD).
 *                       Puede ser nula para peliculas sin fecha confirmada.
 * @property video Indica si la pelicula tiene contenido de video asociado en TMDB.
 *                Puede ser nulo si la informacion no esta disponible.
 * @property vote_average Puntuacion media de usuarios (0.0-10.0).
 * @property vote_count Numero total de votos recibidos.
 * @property media_type Siempre "movie" para instancias de esta clase.
 *
 * @see MediaItem
 * @see Genero
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
