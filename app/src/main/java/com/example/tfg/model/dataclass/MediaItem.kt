package com.example.tfg.model.dataclass

data class ApiResponse(
    val page: Int,
    val results: List<MediaItem>,
    val total_pages: Int,
    val total_results: Int
)

sealed class MediaItem {
    abstract val id: Int
    abstract val backdrop_path: String?
    abstract val poster_path: String?
    abstract val adult: Boolean
    abstract val original_language: String
    abstract val genre_ids: List<Int>
    abstract val popularity: Double
    abstract val vote_average: Double
    abstract val vote_count: Int
    abstract val media_type: String
}