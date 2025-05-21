package com.example.tfg.model.api

import com.example.tfg.BuildConfig
import com.example.tfg.model.dataclass.ApiResponse
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

private val apiKey = if (BuildConfig.API_KEY_TMDB.isNotBlank()) {
    "Bearer ${BuildConfig.API_KEY_TMDB}"
} else {
    throw IllegalStateException("API_KEY_TMDB is not set in local.properties")
}

interface APImedia {

    //-------------------Utilizado en Trending-------------------
    //Trending
    @GET("trending/{tipo}/{tiempo}")
    suspend fun getTrendingAll(
        @Path("tipo") tipo: String,
        @Path("tiempo") tiempo: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse


    //-------------------Utilizado en Popular-------------------
    //En retransmision
    @GET("tv/on_the_air")
    suspend fun getEnRetransmision(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //En television
    @GET("tv/airing_today")
    suspend fun getEnTelevision(
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //Alquiler
    @GET("discover/movie")
    suspend fun getPeliculasEnAlquiler(
        @Query("with_watch_monetization_types") monetizationType: String = "rent",
        @Query("watch_region") watchRegion: String = "ES",
        @Query("page") page: Int = 1,
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //En cine
    @GET("movie/now_playing")
    suspend fun getPeliculasEnCine(
        @Query("page") page: Int = 1,
        @Query("region") region: String = "ES",
        @Query("language") language: String = "es-ES",
        @Header("Authorization") token: String = apiKey
    ): ApiResponse

    //-------------------Utilizado en Gratis-------------------
    //Gratis
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
    @GET("movie/{media_id}")
    suspend fun getMovieDetails(
        @Path("media_id") movieId: Int,
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): Pelicula

    @GET("tv/{media_id}")
    suspend fun getTvDetails(
        @Path("media_id") tvId: Int,
        @Query("language") language: String,
        @Header("Authorization") token: String = apiKey
    ): TvShow
}