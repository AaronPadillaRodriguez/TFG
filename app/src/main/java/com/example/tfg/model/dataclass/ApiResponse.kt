package com.example.tfg.model.dataclass

/**
 * Respuesta estandar de la API TMDB con paginacion.
 *
 * @property page Pagina actual de resultados.
 * @property results Lista de elementos multimedia (puede ser nula).
 * @property total_pages Total de paginas disponibles.
 * @property total_results Total de elementos coincidentes.
 */
data class ApiResponse(
    val page: Int,
    val results: List<MediaItem>?,
    val total_pages: Int,
    val total_results: Int
)