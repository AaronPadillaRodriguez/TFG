package com.example.tfg.model.adapter

import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.google.gson.*
import java.lang.reflect.Type

class MediaItemTypeAdapter : JsonDeserializer<MediaItem> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): MediaItem {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("El JSON es nulo o no es un objeto válido")
        }

        val jsonObject = json.asJsonObject

        val mediaType = if (jsonObject.has("media_type") && !jsonObject.get("media_type").isJsonNull) {
            jsonObject.get("media_type").asString
        } else {
            when {
                jsonObject.has("title") -> "movie"
                jsonObject.has("name") -> "tv"
                else -> "unknown"
            }
        }

        return when (mediaType) {
            "movie" -> context?.deserialize<Pelicula>(json, Pelicula::class.java)
                ?: throw JsonParseException("No se pudo deserializar Pelicula")
            "tv" -> context?.deserialize<TvShow>(json, TvShow::class.java)
                ?: throw JsonParseException("No se pudo deserializar TvShow")
            else -> throw JsonParseException("Tipo desconocido: $mediaType")
        }
    }
}
