package com.example.tfg.model.dataclass

/**
 * Respuesta estandar de la API TMDB con paginacion.
 *
 * Esta clase representa la estructura comun de respuesta que devuelve la API de TMDB
 * para consultas que retornan multiples elementos con paginacion. Se utiliza para
 * encapsular tanto los datos solicitados como la informacion de navegacion entre paginas.
 *
 * @property page Pagina actual de resultados (empezando desde 1).
 * @property results Lista de elementos multimedia obtenidos en la consulta actual.
 *                  Puede ser nula si no hay resultados o si ocurre un error.
 * @property total_pages Numero total de paginas disponibles para la consulta realizada.
 * @property total_results Numero total de elementos que coinciden con los criterios de busqueda.
 *
 * @see MediaItem
 */
data class ApiResponse(
    val page: Int,
    val results: List<MediaItem>?,
    val total_pages: Int,
    val total_results: Int
)