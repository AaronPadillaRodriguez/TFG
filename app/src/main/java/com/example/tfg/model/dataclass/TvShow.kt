package com.example.tfg.model.dataclass

data class TvShow(
    override val id: Int,
    val name: String,
    val original_name: String,
    val overview: String,
    override val backdrop_path: String?,
    override val poster_path: String?,
    override val adult: Boolean,
    override val original_language: String,
    override val genre_ids: List<Int>,
    override val popularity: Double,
    val first_air_date: String,
    override val vote_average: Double,
    override val vote_count: Int,
    val origin_country: List<String>,
    override val media_type: String
) : MediaItem()