package com.example.tfg.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.tfg.R
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.People
import com.example.tfg.model.dataclass.Roles
import com.example.tfg.model.dataclass.TvShow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Utilidades generales para la aplicación.
 * Incluye funciones para manejo de colores, red, conversiones y más.
 */

/**
 * Crea y retorna una instancia de Retrofit configurada para la API de TMDB.
 *
 * @return Instancia de Retrofit configurada
 */
fun getRetrofit(): Retrofit =
    Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

/**
 * Realiza una petición con fallback a otro idioma si la respuesta primaria no es válida.
 *
 * @param primaryLanguage Idioma preferido (default: "es-ES")
 * @param fallbackLanguage Idioma alternativo (default: "en-US")
 * @param fetchFunction Función que realiza la petición
 * @param validateResponse Función que valida la respuesta
 * @return Respuesta en el idioma preferido o alternativo
 */
suspend fun <T> fetchWithLanguageFallback(primaryLanguage: String = "es-ES",
                                          fallbackLanguage: String = "en-US",
                                          fetchFunction: suspend (String) -> T,
                                          validateResponse: (T) -> Boolean = { true } ): T {
    val primaryResponse = fetchFunction(primaryLanguage)
    if (validateResponse(primaryResponse)) {
        return primaryResponse
    }
    return fetchFunction(fallbackLanguage)
}

/**
 * Calcula el color promedio de un Bitmap, ignorando blancos/negros extremos.
 *
 * @param bitmap Imagen para analizar
 * @return Color promedio en formato Int
 */
fun calcularColorPromedio(bitmap: Bitmap): Int {
    val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)

    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L

    for (x in 0 until scaled.width) {
        for (y in 0 until scaled.height) {
            val color = scaled.getPixel(x, y)

            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            // Ignora blancos/negros extremos
            if ((r + g + b) in 30..735) {
                rSum += r
                gSum += g
                bSum += b
                count++
            }
        }
    }

    if (count == 0L) return Color.GRAY

    val rAvg = (rSum / count).toInt()
    val gAvg = (gSum / count).toInt()
    val bAvg = (bSum / count).toInt()

    return Color.rgb(rAvg, gAvg, bAvg)
}

/**
 * Obtiene el recurso drawable adecuado para una barra de progreso según el porcentaje.
 *
 * @param porcentaje Valor entre 0-100
 * @return ID del recurso drawable
 */
fun getProgressDrawableRes(porcentaje: Int): Int {
    return when {
        porcentaje == 0 -> R.drawable.circular_progress_null
        porcentaje >= 70 -> R.drawable.circular_progress_green
        porcentaje in 30..69 -> R.drawable.circular_progress_yellow
        else -> R.drawable.circular_progress_red
    }
}

/**
 * Determina si un color es considerado oscuro según su luminancia.
 *
 * @return true si el color es oscuro, false en caso contrario
 */
fun Int.isDarkColor(): Boolean {
    val darknessThreshold = 0.5 // Umbral para considerar "oscuro" (ajústalo si es necesario)
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
    return luminance < darknessThreshold
}

/**
 * Obtiene detalles de película con fallback de idioma.
 * Primero intenta en español, luego en inglés si faltan datos.
 *
 * @param movieId ID de la película
 * @param api Instancia de la API
 * @return Detalles de la película combinados
 */
suspend fun getMovieDetailsConFallback(movieId: Int, api: APImedia): Pelicula {
    // Primero intentamos obtener todos los datos en español
    val peliculaEspaniol = try {
        api.getMovieDetails(movieId, "es-ES").apply {
            // Asegurarnos que los campos requeridos no sean null
            media_type = media_type ?: "movie"
        }
    } catch (e: Exception) {
        null
    }

    // Comprobamos si faltan datos importantes en español
    val faltanDatos = peliculaEspaniol?.let {
        it.title.isNullOrEmpty() || it.overview.isNullOrEmpty() ||
                it.tagline.isNullOrEmpty() || it.poster_path.isNullOrEmpty() ||
                it.backdrop_path.isNullOrEmpty()
    } ?: true

    // Si faltan datos, intentamos completar con ingles
    if (faltanDatos) {
        try {
            val peliculaIngles = api.getMovieDetails(movieId, "en-US").apply {
                // Asegurarnos que los campos requeridos no sean null
                media_type = media_type ?: "movie"
            }

            // Si no teniamos datos en español, devolvemos directamente los datos en ingles
            if (peliculaEspaniol == null) {
                return peliculaIngles
            }

            // Combinamos los datos, dando preferencia a los datos en español cuando existen
            return peliculaEspaniol.copy(
                title = peliculaEspaniol.title?.ifEmpty { peliculaIngles.title ?: "" },
                overview = peliculaEspaniol.overview?.ifEmpty { peliculaIngles.overview ?: "" },
                tagline = peliculaEspaniol.tagline?.ifEmpty { peliculaIngles.tagline ?: "" },
            )
        } catch (e: Exception) {
            Log.w("DetalleSuperior", "Error al obtener detalles en ingles: ${e.message}")
            // Si falla el ingles, devolvemos lo que tenemos en español o lanzamos excepcion
            return peliculaEspaniol ?: throw e
        }
    }

    return peliculaEspaniol ?: fetchWithLanguageFallback(
        fetchFunction = { language ->
            api.getMovieDetails(movieId, language).apply {
                media_type = media_type ?: "movie"
            }
        },
        validateResponse = { pelicula ->
            !pelicula.title.isNullOrEmpty() && !pelicula.overview.isNullOrEmpty()
        }
    )
}

/**
 * Obtiene detalles de serie TV con fallback de idioma.
 * Primero intenta en español, luego en inglés si faltan datos.
 *
 * @param tvId ID de la serie
 * @param api Instancia de la API
 * @return Detalles de la serie combinados
 */
suspend fun getTvDetailsConFallback(tvId: Int, api: APImedia): TvShow {
    // Primero intentamos obtener todos los datos en español
    val tvShowEspaniol = try {
        api.getTvDetails(tvId, "es-ES").apply {
            // Asegurarnos que los campos requeridos no sean null
            media_type = media_type ?: "tv"
        }
    } catch (e: Exception) {
        null
    }

    // Comprobamos si faltan datos importantes en español
    val faltanDatos = tvShowEspaniol?.let {
        it.name.isNullOrEmpty() || it.overview.isNullOrEmpty() ||
                it.tagline.isNullOrEmpty() || it.poster_path.isNullOrEmpty() ||
                it.backdrop_path.isNullOrEmpty()
    } ?: true

    // Si faltan datos, intentamos completar con ingles
    if (faltanDatos) {
        try {
            val tvShowIngles = api.getTvDetails(tvId, "en-US").apply {
                // Asegurarnos que los campos requeridos no sean null
                media_type = media_type ?: "tv"
            }

            // Si no teniamos datos en español, devolvemos directamente los datos en ingles
            if (tvShowEspaniol == null) {
                return tvShowIngles
            }

            // Combinamos los datos, dando preferencia a los datos en español cuando existen
            return tvShowEspaniol.copy(
                name = tvShowEspaniol.name?.ifEmpty() { tvShowIngles.name ?: "" },
                overview = tvShowEspaniol.overview?.ifEmpty { tvShowIngles.overview ?: "" },
                tagline = tvShowEspaniol.tagline?.ifEmpty { tvShowIngles.tagline ?: "" },
            )
        } catch (e: Exception) {
            Log.w("DetalleSuperior", "Error al obtener detalles en ingles: ${e.message}")
            // Si falla el ingles, devolvemos lo que tenemos en español o lanzamos excepcion
            return tvShowEspaniol ?: throw e
        }
    }

    return tvShowEspaniol ?: fetchWithLanguageFallback(
        fetchFunction = { language ->
            api.getTvDetails(tvId, language).apply {
                media_type = media_type ?: "tv"
            }
        },
        validateResponse = { tvShow ->
            !tvShow.name.isNullOrEmpty() && !tvShow.overview.isNullOrEmpty()
        }
    )
}

/**
 * Obtiene el reparto de película con fallback de idioma.
 *
 * @param mediaId ID del medio
 * @param api Instancia de la API
 * @return Lista de actores combinada
 */
suspend fun getMoviePeopleConFallback(mediaId: Int, api: APImedia): List<People> {
    return try {
        // Primero intentamos en español
        val creditosEs = api.getMoviePeople(mediaId, "es-ES")

        // Verificamos si hay datos importantes faltantes
        val faltanDatos = creditosEs.cast.isEmpty() ||
                creditosEs.cast.any { it.name.isNullOrEmpty() || it.character.isNullOrEmpty() }

        if (!faltanDatos) {
            return creditosEs.cast
        }

        // Si faltan datos, intentamos en inglés
        try {
            val creditosEn = api.getMoviePeople(mediaId, "en-US")

            // Combinamos los resultados
            mergeCreditos(creditosEs.cast, creditosEn.cast)
        } catch (e: Exception) {
            Log.w("CreditosManager", "Error al obtener créditos en inglés: ${e.message}")
            creditosEs.cast // Devolvemos lo que tenemos en español
        }
    } catch (e: Exception) {
        // Si falla completamente en español, intentamos en inglés
        try {
            api.getMoviePeople(mediaId, "en-US").cast
        } catch (e: Exception) {
            Log.e("CreditosManager", "Error al obtener créditos: ${e.message}")
            emptyList() // Devolvemos lista vacía si toddo falla
        }
    }
}

/**
 * Obtiene el reparto de serie TV con fallback de idioma.
 *
 * @param mediaId ID del medio
 * @param api Instancia de la API
 * @return Lista de actores combinada
 */
suspend fun getTvPeopleConFallback(mediaId: Int, api: APImedia): List<People> {
    return try {
        // Primero intentamos en español
        val creditosEs = api.getTvPeople(mediaId, "es-ES")

        // Verificamos si hay datos importantes faltantes
        val faltanDatos = creditosEs.cast.isEmpty() ||
                creditosEs.cast.any { it.name.isNullOrEmpty() || it.character.isNullOrEmpty()
                        || it.roles?.any { rol -> rol.episode_count == 0 } ?: true
                }

        if (!faltanDatos) {
            return creditosEs.cast
        }

        // Si faltan datos, intentamos en inglés
        try {
            val creditosEn = api.getMoviePeople(mediaId, "en-US")

            // Combinamos los resultados
            mergeCreditos(creditosEs.cast, creditosEn.cast)
        } catch (e: Exception) {
            Log.w("CreditosManager", "Error al obtener créditos en inglés: ${e.message}")
            creditosEs.cast // Devolvemos lo que tenemos en español
        }
    } catch (e: Exception) {
        // Si falla completamente en español, intentamos en inglés
        try {
            api.getMoviePeople(mediaId, "en-US").cast
        } catch (e: Exception) {
            Log.e("CreditosManager", "Error al obtener créditos: ${e.message}")
            emptyList() // Devolvemos lista vacía si toddo falla
        }
    }
}

/**
 * Combina listas de créditos en diferentes idiomas.
 * Prioriza los datos en español pero complementa con inglés cuando faltan.
 *
 * @param castEs Lista de actores en español
 * @param castEn Lista de actores en inglés
 * @return Lista combinada y optimizada
 */
private fun mergeCreditos(castEs: List<People>, castEn: List<People>): List<People> {
    // Creamos un mapa por ID para acceso rápido
    val mapaEn = castEn.associateBy { it.id }

    return castEs.map { personaEs ->
        val personaEn = mapaEn[personaEs.id]

        // Combinamos los datos principales
        val personaCombinada = personaEs.copy(
            name = personaEs.name?.ifEmpty { personaEn?.name ?: "" },
            character = personaEs.character?.ifEmpty { personaEn?.character ?: "" },
            profile_path = personaEs.profile_path.ifEmpty { personaEn?.profile_path ?: "" },
            roles = personaEs.roles?.let { mergeRoles(it, personaEn?.roles ?: emptyList()) }
        )

        personaCombinada
    } + castEn.filterNot { en -> castEs.any { es -> es.id == en.id } } // Añadimos actores que solo están en inglés
}

/**
 * Combina listas de roles de actores en diferentes idiomas.
 * Mantiene la información en español pero actualiza conteo de episodios si es necesario.
 *
 * @param rolesEs Lista de roles en español
 * @param rolesEn Lista de roles en inglés
 * @return Lista combinada de roles
 */
private fun mergeRoles(rolesEs: List<Roles>, rolesEn: List<Roles>): List<Roles> {
    // Creamos un mapa por credit_id para acceso rápido
    val mapaEn = rolesEn.associateBy { it.credit_id }

    return rolesEs.map { rolEs ->
        val rolEn = mapaEn[rolEs.credit_id]

        rolEs.copy(
            episode_count = rolEn?.episode_count ?: rolEs.episode_count
        )
    } + rolesEn.filterNot { en -> rolesEs.any { es -> es.credit_id == en.credit_id } }
}


/**
 * Convierte dp a píxeles según la densidad de pantalla.
 *
 * @param context Contexto para acceder a recursos
 * @return Valor en píxeles
 */
fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

/**
 * Manager para colores predominientes en la UI.
 * Almacena y calcula colores basados en imágenes.
 */
object ColorManager {
    var averageColor: Int = Color.TRANSPARENT
    var darkerColor: Int = Color.TRANSPARENT
    var isDark: Boolean = false

    /**
     * Actualiza los colores basados en un Bitmap.
     *
     * @param bitmap Imagen para calcular colores
     */
    fun updateFromBitmap(bitmap: Bitmap) {
        averageColor = calcularColorPromedio(bitmap)
        isDark = averageColor.isDarkColor()

        val hsv = FloatArray(3)
        Color.colorToHSV(averageColor, hsv)
        hsv[2] *= 0.8f
        darkerColor = Color.HSVToColor(hsv)
    }
}

/**
 * Interfaz para notificar cuando se cargan detalles.
 */
interface OnDetailsLoadedListener {
    fun onDetailsLoaded()
}

// Funciones privadas omitidas en la documentación pública
