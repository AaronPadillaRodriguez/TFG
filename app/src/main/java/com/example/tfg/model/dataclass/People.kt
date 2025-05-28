package com.example.tfg.model.dataclass

/**
 * Informacion de un actor/persona.
 *
 * @property id ID unico.
 * @property adult Si es contenido adulto.
 * @property gender Genero (0=No especificado, 1=F, 2=M).
 * @property name Nombre actual.
 * @property original_name Nombre original.
 * @property popularity Indice de popularidad.
 * @property profile_path Ruta de la foto.
 * @property roles Lista de roles interpretados.
 * @property character Personaje principal.
 * @property total_episode_count Total de episodios.
 */
data class People(
    val id: Int,
    val adult: Boolean,
    val gender: Int,
    val name: String?,
    val original_name: String,
    val popularity: Double,
    val profile_path: String,
    val roles: List<Roles>?,
    val character: String?,
    val total_episode_count: Int?
)
