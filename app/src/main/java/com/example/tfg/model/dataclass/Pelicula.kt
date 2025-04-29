package com.example.tfg.model.dataclass

import com.google.gson.annotations.SerializedName

data class Pelicula(
    override val id: Int,
    val title: String,
    val original_title: String,
    val overview: String,
    override val backdrop_path: String?,
    override val poster_path: String?,
    override val adult: Boolean,
    override val original_language: String,
    override val genre_ids: List<Int>,
    override val popularity: Double,
    val release_date: String,
    val video: Boolean,
    override val vote_average: Double,
    override val vote_count: Int,
    override val media_type: String
) : MediaItem()
