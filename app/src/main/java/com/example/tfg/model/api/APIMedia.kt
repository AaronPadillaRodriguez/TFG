package com.example.tfg.model.api

import com.example.tfg.BuildConfig
import com.example.tfg.model.dataclass.ApiResponse
import com.example.tfg.model.dataclass.Credits
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.Seasons
import com.example.tfg.model.dataclass.TvShow
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Token de autorizacion para la API de TMDB (The Movie Database).
 * Se obtiene del archivo BuildConfig y se valida que no este vacio.
 *
 * @throws IllegalStateException si la API_KEY_TMDB no esta configurada en local.properties
 */
private val apiKey = if (BuildConfig.API_KEY_TMDB.isNotBlank()) {
    "Bearer ${BuildConfig.API_KEY_TMDB}" // Formato Bearer requerido por TMDB API
} else {
    throw IllegalStateException("API_KEY_TMDB is not set in local.properties")
}

/**
 * Interfaz que define los endpoints de la API de TMDB (The Movie Database) utilizados
 * en la aplicacion para obtener informacion sobre peliculas y series de television.
 *
 * Esta interfaz esta diseñada para trabajar con Retrofit y proporciona acceso a:
 * - Contenido trending (tendencias)
 * - Contenido popular en diferentes categorias
 * - Contenido gratuito
 * - Detalles especificos de peliculas y series
 * - Informacion de temporadas y episodios
 *
 * Todos los metodos son suspendidos para su uso con Coroutines.
 */
interface APImedia {

    //-------------------Utilizado en Trending-------------------

    /**
     * Obtiene el contenido en tendencia de TMDB segun el tipo y periodo de tiempo especificado.
     *
     * @param tipo Tipo de contenido ("movie", "tv", "person" o "all")
     * @param tiempo Periodo de tiempo ("day" para diario, "week" para semanal)
     * @param page Numero de pagina para paginacion (por defecto 1)
     * @param language Idioma de respuesta en formato ISO 639-1 (por defecto "es-ES")
     * @param token Token de autorizacion Bearer para autenticacion
     * @return [ApiResponse] con la lista de contenido trending
     */
    @GET("trending/{tipo}/{tiempo}")
    suspend fun getTrendingAll(
        @Path("tipo") tipo: String,
        @Path("tiempo") tiempo: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //-------------------Utilizado en Popular-------------------

    /**
     * Obtiene las series de television que estan actualmente en retransmision.
     * Incluye series que han emitido episodios en los ultimos 7 dias.
     *
     * @param page Numero de pagina para paginacion (por defecto 1)
     * @param language Idioma de respuesta (por defecto "es-ES")
     * @param token Token de autorizacion Bearer
     * @return [ApiResponse] con las series en retransmision
     */
    @GET("tv/on_the_air")
    suspend fun getEnRetransmision(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    /**
     * Obtiene las series de television que se emiten hoy.
     * Muestra series con episodios programados para el dia actual.
     *
     * @param page Numero de pagina para paginacion (por defecto 1)
     * @param language Idioma de respuesta (por defecto "es-ES")
     * @param token Token de autorizacion Bearer
     * @return [ApiResponse] con las series que se emiten hoy
     */
    @GET("tv/airing_today")
    suspend fun getEnTelevision(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    /**
     * Obtiene peliculas disponibles para alquilar en la region especificada.
     * Utiliza el sistema de filtrado de TMDB para monetizacion.
     *
     * @param monetizationType Tipo de monetizacion ("rent" para alquiler)
     * @param watchRegion Region de visualizacion en formato ISO 3166-1 (por defecto "ES" para España)
     * @param page Numero de pagina para paginacion (por defecto 1)
     * @param language Idioma de respuesta (por defecto "es-ES")
     * @param token Token de autorizacion Bearer
     * @return [ApiResponse] con las peliculas disponibles para alquilar
     */
    @GET("discover/movie")
    suspend fun getPeliculasEnAlquiler(
        @Query("with_watch_monetization_types") monetizationType: String = "rent",
        @Query("watch_region") watchRegion: String = "ES",
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    /**
     * Obtiene las peliculas que estan actualmente en cartelera en cines.
     * Filtrado por region para mostrar solo las disponibles en el pais especificado.
     *
     * @param page Numero de pagina para paginacion (por defecto 1)
     * @param region Codigo de region ISO 3166-1 (por defecto "ES" para España)
     * @param language Idioma de respuesta (por defecto "es-ES")
     * @param token Token de autorizacion Bearer
     * @return [ApiResponse] con las peliculas actualmente en cines
     */
    @GET("movie/now_playing")
    suspend fun getPeliculasEnCine(
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ES",
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //-------------------Utilizado en Gratis-------------------

    /**
     * Obtiene contenido gratuito (peliculas o series) disponible en la region especificada.
     * Utiliza el endpoint discover para filtrar por tipo de monetizacion gratuita.
     *
     * @param tipo Tipo de contenido ("movie" para peliculas, "tv" para series)
     * @param page Numero de pagina para paginacion (por defecto 1)
     * @param language Idioma de respuesta (por defecto "es-ES")
     * @param watchRegion Region de visualizacion (por defecto "ES")
     * @param monetizationType Tipo de monetizacion ("free" para contenido gratuito)
     * @param token Token de autorizacion Bearer
     * @return [ApiResponse] con el contenido gratuito disponible
     */
    @GET("discover/{tipo}")
    suspend fun getGratis(
        @Path("tipo") tipo: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Query("watch_region") watchRegion: String = "ES",
        @Query("with_watch_monetization_types") monetizationType: String = "free",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //-------------------Utilizado en Detalles-------------------

    /**
     * Obtiene los detalles completos de una pelicula especifica.
     * Incluye informacion detallada como sinopsis, fecha de estreno, generos, etc.
     *
     * @param movieId ID unico de la pelicula en TMDB
     * @param language Idioma para la respuesta
     * @param token Token de autorizacion Bearer
     * @return [Pelicula] objeto con toda la informacion detallada de la pelicula
     */
    @GET("movie/{media_id}")
    suspend fun getMovieDetails(
        @Path("media_id") movieId: Int,
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): Pelicula

    /**
     * Obtiene los detalles completos de una serie de television especifica.
     * Incluye informacion como sinopsis, fechas de emision, numero de temporadas, etc.
     *
     * @param tvId ID unico de la serie en TMDB
     * @param language Idioma para la respuesta
     * @param token Token de autorizacion Bearer
     * @return [TvShow] objeto con toda la informacion detallada de la serie
     */
    @GET("tv/{media_id}")
    suspend fun getTvDetails(
        @Path("media_id") tvId: Int,
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): TvShow

    /**
     * Obtiene los creditos (reparto y equipo) de una pelicula especifica.
     * Incluye actores, directores, productores y demas personal involucrado.
     *
     * @param tvId ID de la pelicula (nota: parametro mal nombrado, deberia ser movieId)
     * @param language Idioma para los nombres y biografias
     * @param token Token de autorizacion Bearer
     * @return [Credits] objeto con informacion del reparto y equipo tecnico
     */
    @GET("movie/{media_id}/credits")
    suspend fun getMoviePeople(
        @Path("media_id") tvId: Int, // Nota: nombre confuso, es para peliculas
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): Credits

    /**
     * Obtiene los creditos agregados de una serie de television.
     * Utiliza aggregate_credits que combina informacion de todas las temporadas.
     *
     * @param tvId ID unico de la serie en TMDB
     * @param language Idioma para los nombres y biografias
     * @param token Token de autorizacion Bearer
     * @return [Credits] objeto con informacion agregada del reparto y equipo
     */
    @GET("tv/{media_id}/aggregate_credits")
    suspend fun getTvPeople(
        @Path("media_id") tvId: Int,
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): Credits

    //-------------------Utilizado en Episodios-------------------

    /**
     * Obtiene informacion detallada de una temporada especifica de una serie.
     * Incluye lista de episodios, fechas de emision y informacion general de la temporada.
     *
     * @param tvId ID unico de la serie en TMDB
     * @param seasonNum Numero de la temporada (comenzando desde 1, o 0 para especiales)
     * @param language Idioma para titulos, sinopsis y descripciones
     * @param token Token de autorizacion Bearer
     * @return [Seasons] objeto con informacion completa de la temporada y sus episodios
     */
    @GET("tv/{series_id}/season/{season_number}")
    suspend fun getTvSeason(
        @Path("series_id") tvId: Int,
        @Path("season_number") seasonNum: Int,
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): Seasons
}