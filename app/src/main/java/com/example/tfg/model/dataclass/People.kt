package com.example.tfg.model.dataclass

/**
 * Representa informacion de una persona en el ambito cinematografico.
 *
 * Esta clase almacena datos de actores, directores y otros profesionales
 * del cine y television obtenidos de TMDB. Incluye informacion personal,
 * profesional y los roles especificos que han interpretado.
 *
 * @property id Identificador unico de la persona en la base de datos de TMDB.
 * @property adult Indica si la persona esta asociada principalmente con contenido para adultos.
 * @property gender Codigo numerico del genero de la persona:
 *                 - 0: No especificado o no revelado
 *                 - 1: Femenino
 *                 - 2: Masculino
 *                 - 3: No binario (en casos especificos)
 * @property name Nombre artistico o profesional actual de la persona.
 *               Puede ser nulo si no esta disponible.
 * @property original_name Nombre original o real de la persona, sin traducciones.
 * @property popularity indice de popularidad actual basado en busquedas y menciones recientes.
 * @property profile_path Ruta relativa a la fotografia oficial de perfil.
 *                       Usado para construir la URL completa de la imagen.
 * @property roles Lista detallada de roles interpretados por la persona en diferentes producciones.
 *                Puede ser nula si no se solicita informacion detallada de roles.
 * @property character Nombre del personaje principal que interpreta en el contexto actual.
 *                    Se usa cuando la persona aparece en los creditos de una produccion especifica.
 * @property total_episode_count Numero total de episodios en los que ha participado.
 *                              Solo relevante para series de TV, puede ser nulo.
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
