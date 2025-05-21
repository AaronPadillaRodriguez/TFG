package com.example.tfg.model.adapter

import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.google.gson.*
import java.lang.reflect.Type

/**
 * Adaptador de tipo personalizado para deserializar objetos JSON en instancias de MediaItem.
 *
 * Esta clase se encarga de convertir los datos JSON recibidos de la API TMDB en objetos
 * del tipo correspondiente (Pelicula o TvShow), que son subclases de MediaItem.
 * La determinación del tipo se realiza basándose en el campo "media_type" del JSON,
 * o en la presencia de campos específicos cuando ese campo no está disponible.
 */
class MediaItemTypeAdapter : JsonDeserializer<MediaItem> {

    /**
     * Deserializa un elemento JSON en un objeto MediaItem apropiado.
     *
     * El método analiza el JSON entrante y decide si debe convertirlo en un objeto
     * Pelicula o TvShow basándose en el valor del campo "media_type" o en la estructura
     * del objeto JSON si ese campo no está presente.
     *
     * @param json El elemento JSON a deserializar
     * @param typeOfT El tipo de objeto que se está deserializando (no utilizado en esta implementación)
     * @param context El contexto de deserialización que proporciona métodos para deserializar
     * @return Una instancia de MediaItem, ya sea Pelicula o TvShow
     * @throws JsonParseException Si el JSON es nulo, no es un objeto válido,
     *                            o no se puede determinar o deserializar el tipo de medio
     */
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MediaItem {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("El JSON es nulo o no es un objeto válido")
        }

        val jsonObject = json.asJsonObject

        // Determina el tipo de medio basado en el campo "media_type" o en la estructura del objeto
        val mediaType = if (jsonObject.has("media_type") && !jsonObject.get("media_type").isJsonNull) {
            jsonObject.get("media_type").asString
        } else {
            when {
                jsonObject.has("title") -> "movie"  // Las películas tienen un campo "title"
                jsonObject.has("name") -> "tv"      // Los programas de TV tienen un campo "name"
                else -> "unknown"
            }
        }

        // Deserializa el JSON al tipo apropiado según el valor de mediaType
        return when (mediaType) {
            "movie" -> context?.deserialize<Pelicula>(json, Pelicula::class.java)
                ?: throw JsonParseException("No se pudo deserializar Pelicula")
            "tv" -> context?.deserialize<TvShow>(json, TvShow::class.java)
                ?: throw JsonParseException("No se pudo deserializar TvShow")
            else -> throw JsonParseException("Tipo desconocido: $mediaType")
        }
    }
}