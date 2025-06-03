package com.example.tfg.model.dataclass

/**
 * Representa un episodio individual de una serie de television.
 *
 * Esta clase encapsula toda la informacion relevante de un episodio especifico,
 * incluyendo metadatos, duracion y estado de visualizacion en la interfaz de usuario.
 *
 * @property air_date Fecha de emision original del episodio en formato ISO (YYYY-MM-DD).
 * @property episode_number Numero del episodio dentro de la temporada (empezando desde 1).
 * @property name Titulo del episodio. Puede ser nulo si no esta disponible.
 * @property overview Sinopsis o descripcion del episodio. Puede ser nulo si no esta disponible.
 * @property runtime Duracion del episodio en minutos. Puede ser nulo si no esta especificado.
 * @property still_path Ruta relativa a la imagen de vista previa del episodio en TMDB.
 *                     Puede ser nulo si no hay imagen disponible.
 * @property vote_average Puntuacion promedio del episodio (0.0 - 10.0).
 *                       Puede ser nulo si no hay suficientes valoraciones.
 * @property isExpanded Estado de expansion en la interfaz de usuario para mostrar/ocultar detalles.
 *                     Por defecto es false (contraido).
 *
 */
data class Episodio(
    val air_date: String,
    val episode_number: Int,
    val name: String?,
    val overview: String?,
    val runtime: Int?,
    val still_path: String?,
    val vote_average: Double?,
    var isExpanded: Boolean = false
)
