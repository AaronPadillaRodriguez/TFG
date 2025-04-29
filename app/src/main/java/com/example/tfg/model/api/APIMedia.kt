package com.example.tfg.model.api

import com.example.tfg.model.dataclass.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

private const val apiKey = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIwOGQ3Nzg3YmFjODVhNWIzZjMwZjA2ZDYzMTlkNTI2NCIsIm5iZiI6MTc0MTYyNDA0Mi4zMjgsInN1YiI6IjY3Y2YxMmVhNzVjOWYxYmQxMmUzMWJlMiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.oPC9xpGiBVpxK4S0DJLL0pDj9rAZbokxxgNFLA3l_uA"

interface APImedia {

    /**
     * --------------- Se puede mejorar ---------------
     */

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
}