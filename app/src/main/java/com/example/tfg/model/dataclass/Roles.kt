package com.example.tfg.model.dataclass

/**
 * Representa un rol especifico interpretado por un actor en una produccion.
 *
 * Esta clase almacena informacion detallada sobre la participacion de un actor
 * en una produccion especifica, incluyendo el personaje interpretado y su
 * participacion en episodios (para series de TV).
 *
 * @property credit_id Identificador unico del credito en TMDB.
 *                    Se utiliza para referenciar esta participacion especifica.
 * @property character Nombre del personaje interpretado por el actor.
 *                    Puede incluir multiples personajes separados por " / ".
 * @property episode_count Numero de episodios en los que participa el actor
 *                        interpretando este personaje especifico.
 *
 * @see People
 */
data class Roles(
    val credit_id: String,
    val character: String,
    val episode_count: Int
) {
    /**
     * Representacion en cadena del rol para serializacion o depuracion.
     *
     * Genera una cadena con formato "credit_id;character;episode_count"
     * util para logging, debugging o almacenamiento en formato de texto.
     * Los campos se separan con punto y coma para evitar conflictos con
     * nombres de personajes que puedan contener comas.
     *
     * @return String formateado con los datos del rol separados por punto y coma.
     */
    override fun toString(): String {
        return "$credit_id;$character;$episode_count" // Formato para serializacion/debug
    }
}


