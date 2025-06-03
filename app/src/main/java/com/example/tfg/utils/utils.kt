package com.example.tfg.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.tfg.R
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.People
import com.example.tfg.model.dataclass.Roles
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.model.enums.Meses
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utilidades generales para la aplicacion de gestion de medios audiovisuales.
 *
 * Este archivo contiene funciones auxiliares para el manejo de:
 * - Configuracion de red y API (Retrofit)
 * - Procesamiento de imagenes y colores
 * - Gestion de idiomas con fallback
 * - Formateo de datos y fechas
 * - Animaciones de UI
 *
 * Desarrollado como parte del Trabajo Final de Grado (TFG) para una aplicacion
 * de catalogo de peliculas y series usando la API de The Movie Database (TMDB).
 */

// ==================== CONFIGURACIoN DE RED ====================

/**
 * Crea y retorna una instancia de Retrofit configurada para la API de TMDB.
 *
 * Esta funcion centraliza la configuracion de Retrofit para todas las peticiones
 * HTTP a The Movie Database API. Utiliza Gson como convertidor JSON para
 * deserializar automaticamente las respuestas.
 *
 * @return Instancia de [Retrofit] configurada con la URL base de TMDB y Gson
 */
fun getRetrofit(): Retrofit =
    Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/") // URL base oficial de TMDB API v3
        .addConverterFactory(GsonConverterFactory.create()) // Convertidor JSON automatico
        .build()

/**
 * Realiza una peticion HTTP con sistema de fallback automatico entre idiomas.
 *
 * Esta funcion generica implementa un patron de fallback que primero intenta
 * obtener datos en el idioma preferido (español por defecto) y, si la respuesta
 * no es valida segun el validador proporcionado, automaticamente reintenta
 * en el idioma alternativo (ingles por defecto).
 *
 * Es especialmente util para la API de TMDB, donde algunos contenidos pueden
 * tener informacion incompleta en ciertos idiomas.
 *
 * @param T Tipo generico de la respuesta esperada
 * @param primaryLanguage Codigo de idioma preferido en formato ISO 639-1 (default: "es-ES")
 * @param fallbackLanguage Codigo de idioma alternativo en formato ISO 639-1 (default: "en-US")
 * @param fetchFunction Funcion suspendida que ejecuta la peticion HTTP, recibe el codigo de idioma
 * @param validateResponse Funcion que valida si la respuesta es valida (default: siempre true)
 * @return Respuesta del tipo T en el idioma que haya proporcionado datos validos
 * @throws Exception Si ambas peticiones (primaria y fallback) fallan
 */
suspend fun <T> fetchWithLanguageFallback(
    primaryLanguage: String = "es-ES",
    fallbackLanguage: String = "en-US",
    fetchFunction: suspend (String) -> T,
    validateResponse: (T) -> Boolean = { true }
): T {
    // Intento inicial con idioma preferido
    val primaryResponse = fetchFunction(primaryLanguage)

    // Validacion de la respuesta primaria
    if (validateResponse(primaryResponse)) {
        return primaryResponse
    }

    // Fallback automatico al idioma alternativo
    return fetchFunction(fallbackLanguage)
}

// ==================== PROCESAMIENTO DE IMaGENES ====================

/**
 * Calcula el color promedio dominante de una imagen bitmap.
 *
 * Analiza todos los pixeles de la imagen (redimensionada a 50x50 para optimizacion)
 * y calcula el color promedio RGB, excluyendo colores extremos (muy blancos o muy negros)
 * que podrian distorsionar el resultado final.
 *
 * Este color se utiliza tipicamente para crear temas dinamicos en la UI,
 * adaptando los colores de fondo segun la imagen del poster.
 *
 * @param bitmap Imagen de la cual extraer el color dominante
 * @return Color promedio en formato entero ARGB. Retorna [Color.GRAY] si no se encuentran colores validos
 *
 * @see ColorManager.updateFromBitmap para uso practico
 */
fun calcularColorPromedio(bitmap: Bitmap): Int {
    // Redimensionar para mejorar rendimiento (50x50 = 2500 pixeles vs potencialmente millones)
    val scaled = Bitmap.createScaledBitmap(bitmap, 50, 50, true)

    var rSum = 0L // Suma de componentes rojos
    var gSum = 0L // Suma de componentes verdes
    var bSum = 0L // Suma de componentes azules
    var count = 0L // Contador de pixeles validos

    // Analisis pixel por pixel
    for (x in 0 until scaled.width) {
        for (y in 0 until scaled.height) {
            val color = scaled.getPixel(x, y)

            // Extraccion de componentes RGB
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            // Filtro de colores extremos: suma RGB entre 30 y 735
            // Esto excluye negros puros (0,0,0 = suma 0) y blancos puros (255,255,255 = suma 765)
            if ((r + g + b) in 30..735) {
                rSum += r
                gSum += g
                bSum += b
                count++
            }
        }
    }

    // Proteccion contra division por cero
    if (count == 0L) return Color.GRAY

    // Calculo del promedio por componente
    val rAvg = (rSum / count).toInt()
    val gAvg = (gSum / count).toInt()
    val bAvg = (bSum / count).toInt()

    return Color.rgb(rAvg, gAvg, bAvg)
}

// ==================== UTILIDADES DE UI ====================

/**
 * Selecciona el recurso drawable apropiado para barras de progreso circulares
 * basandose en el porcentaje de puntuacion.
 *
 * Implementa un sistema de colores semaforico para representar visualmente
 * la calidad de puntuaciones (ej: puntuaciones IMDB, Rotten Tomatoes).
 *
 * @param porcentaje Valor numerico entre 0-100 representando la puntuacion
 * @return ID del recurso drawable correspondiente:
 *         - Verde: 70-100% (excelente)
 *         - Amarillo: 30-69% (regular)
 *         - Rojo: 1-29% (malo)
 *         - Gris: 0% (sin puntuacion)
 */
fun getProgressDrawableRes(porcentaje: Int): Int {
    return when {
        porcentaje == 0 -> R.drawable.circular_progress_null        // Sin datos
        porcentaje >= 70 -> R.drawable.circular_progress_green      // Puntuacion alta
        porcentaje in 30..69 -> R.drawable.circular_progress_yellow // Puntuacion media
        else -> R.drawable.circular_progress_red                    // Puntuacion baja
    }
}

/**
 * Determina si un color es considerado oscuro basandose en su luminancia percibida.
 *
 * Utiliza la formula de luminancia ponderada que tiene en cuenta la sensibilidad
 * del ojo humano a diferentes longitudes de onda (el verde se percibe mas brillante
 * que el rojo, y el rojo mas que el azul).
 *
 * Esta funcion es util para decidir automaticamente si usar texto claro u oscuro
 * sobre un fondo de color determinado.
 *
 * @receiver Int Color en formato ARGB
 * @return true si el color es oscuro (luminancia < 0.5), false si es claro
 */
fun Int.isDarkColor(): Boolean {
    val darknessThreshold = 0.5 // Umbral de oscuridad (50%)

    // Extraccion de componentes RGB
    val red = Color.red(this)
    val green = Color.green(this)
    val blue = Color.blue(this)

    // Formula de luminancia ponderada segun sensibilidad del ojo humano
    // Coeficientes estandar: R=0.299, G=0.587, B=0.114
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255

    return luminance < darknessThreshold
}

// ==================== GESTIoN DE DATOS CON FALLBACK ====================

/**
 * Obtiene detalles completos de una pelicula con sistema de fallback multiidioma.
 *
 * Implementa una estrategia sofisticada para garantizar datos completos:
 * 1. Intenta obtener todos los datos en español
 * 2. Detecta campos faltantes o vacios
 * 3. Complementa automaticamente con datos en ingles
 * 4. Combina ambas respuestas priorizando español cuando esta disponible
 *
 * @param movieId Identificador unico de la pelicula en TMDB
 * @param api Instancia del servicio API configurado
 * @return [Pelicula] con datos completos combinados de ambos idiomas
 * @throws Exception Si fallan ambas peticiones (español e ingles)
 */
suspend fun getMovieDetailsConFallback(movieId: Int, api: APImedia): Pelicula {
    // Primera tentativa: obtener datos en español
    val peliculaEspaniol = try {
        api.getMovieDetails(movieId, "es-ES").apply {
            // Garantizar campo media_type para compatibilidad
            media_type = media_type ?: "movie"
        }
    } catch (e: Exception) {
        null // Continua si falla la peticion en español
    }

    // Analisis de completitud de datos en español
    val faltanDatos = peliculaEspaniol?.let {
        it.title.isNullOrEmpty() || it.overview.isNullOrEmpty() ||
                it.tagline.isNullOrEmpty() || it.poster_path.isNullOrEmpty() ||
                it.backdrop_path.isNullOrEmpty()
    } ?: true // Si peliculaEspaniol es null, definitivamente faltan datos

    // Sistema de complemento con datos en ingles
    if (faltanDatos) {
        try {
            val peliculaIngles = api.getMovieDetails(movieId, "en-US").apply {
                media_type = media_type ?: "movie"
            }

            // Si no hay datos en español, retornar directamente los de ingles
            if (peliculaEspaniol == null) {
                return peliculaIngles
            }

            // Combinacion inteligente: español preferido, ingles como complemento
            return peliculaEspaniol.copy(
                title = peliculaEspaniol.title?.ifEmpty { peliculaIngles.title ?: "" },
                overview = peliculaEspaniol.overview?.ifEmpty { peliculaIngles.overview ?: "" },
                tagline = peliculaEspaniol.tagline?.ifEmpty { peliculaIngles.tagline ?: "" },
            )
        } catch (e: Exception) {
            Log.w("DetalleSuperior", "Error al obtener detalles en ingles: ${e.message}")
            // Retornar datos parciales en español o propagar excepcion
            return peliculaEspaniol ?: throw e
        }
    }

    // Si los datos en español estan completos, usarlos directamente
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
 * Obtiene detalles completos de una serie de TV con sistema de fallback multiidioma.
 *
 * Funciona de manera similar a [getMovieDetailsConFallback] pero adaptado para series.
 * Maneja los campos especificos de TV como 'name' en lugar de 'title'.
 *
 * @param tvId Identificador unico de la serie en TMDB
 * @param api Instancia del servicio API configurado
 * @return [TvShow] con datos completos combinados de ambos idiomas
 * @throws Exception Si fallan ambas peticiones (español e ingles)
 *
 * @see getMovieDetailsConFallback para logica similar aplicada a peliculas
 */
suspend fun getTvDetailsConFallback(tvId: Int, api: APImedia): TvShow {
    // Primera tentativa: obtener datos en español
    val tvShowEspaniol = try {
        api.getTvDetails(tvId, "es-ES").apply {
            media_type = media_type ?: "tv" // Tipo especifico para series
        }
    } catch (e: Exception) {
        null
    }

    // Verificacion de campos especificos de series TV
    val faltanDatos = tvShowEspaniol?.let {
        it.name.isNullOrEmpty() || it.overview.isNullOrEmpty() ||
                it.tagline.isNullOrEmpty() || it.poster_path.isNullOrEmpty() ||
                it.backdrop_path.isNullOrEmpty()
    } ?: true

    // Sistema de complemento con ingles
    if (faltanDatos) {
        try {
            val tvShowIngles = api.getTvDetails(tvId, "en-US").apply {
                media_type = media_type ?: "tv"
            }

            if (tvShowEspaniol == null) {
                return tvShowIngles
            }

            // Combinacion especifica para series (usando 'name' en lugar de 'title')
            return tvShowEspaniol.copy(
                name = tvShowEspaniol.name?.ifEmpty() { tvShowIngles.name ?: "" },
                overview = tvShowEspaniol.overview?.ifEmpty { tvShowIngles.overview ?: "" },
                tagline = tvShowEspaniol.tagline?.ifEmpty { tvShowIngles.tagline ?: "" },
            )
        } catch (e: Exception) {
            Log.w("DetalleSuperior", "Error al obtener detalles en ingles: ${e.message}")
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
 * Obtiene el reparto (cast) de una pelicula con fallback de idioma.
 *
 * Maneja la obtencion de informacion de actores y sus personajes, garantizando
 * que se muestren nombres de personajes traducidos cuando esten disponibles,
 * pero complementando con informacion en ingles cuando sea necesario.
 *
 * @param mediaId Identificador unico de la pelicula en TMDB
 * @param api Instancia del servicio API configurado
 * @return Lista de [People] representando el reparto con informacion combinada
 */
suspend fun getMoviePeopleConFallback(mediaId: Int, api: APImedia): List<People> {
    return try {
        // Obtener creditos en español primero
        val creditosEs = api.getMoviePeople(mediaId, "es-ES")

        // Verificar calidad de los datos obtenidos
        val faltanDatos = creditosEs.cast.isEmpty() ||
                creditosEs.cast.any { it.name.isNullOrEmpty() || it.character.isNullOrEmpty() }

        if (!faltanDatos) {
            return creditosEs.cast
        }

        // Complementar con datos en ingles si es necesario
        try {
            val creditosEn = api.getMoviePeople(mediaId, "en-US")
            // Funcion helper para combinar listas de creditos
            mergeCreditos(creditosEs.cast, creditosEn.cast)
        } catch (e: Exception) {
            Log.w("CreditosManager", "Error al obtener creditos en ingles: ${e.message}")
            creditosEs.cast // Usar datos parciales en español
        }
    } catch (e: Exception) {
        // Fallback completo a ingles si español falla totalmente
        try {
            api.getMoviePeople(mediaId, "en-US").cast
        } catch (e: Exception) {
            Log.e("CreditosManager", "Error al obtener creditos: ${e.message}")
            emptyList() // Lista vacia como ultimo recurso
        }
    }
}

/**
 * Obtiene el reparto (cast) de una serie de TV con fallback de idioma.
 *
 * Similar a [getMoviePeopleConFallback] pero adaptado para series, incluyendo
 * validaciones especificas como el conteo de episodios en los roles.
 *
 * @param mediaId Identificador unico de la serie en TMDB
 * @param api Instancia del servicio API configurado
 * @return Lista de [People] representando el reparto con informacion combinada
 *
 * @see getMoviePeopleConFallback para funcionalidad similar en peliculas
 */
suspend fun getTvPeopleConFallback(mediaId: Int, api: APImedia): List<People> {
    return try {
        val creditosEs = api.getTvPeople(mediaId, "es-ES")

        // Validacion especifica para series TV (incluye conteo de episodios)
        val faltanDatos = creditosEs.cast.isEmpty() ||
                creditosEs.cast.any {
                    it.name.isNullOrEmpty() || it.character.isNullOrEmpty()
                            || it.roles?.any { rol -> rol.episode_count == 0 } ?: true
                }

        if (!faltanDatos) {
            return creditosEs.cast
        }

        try {
            val creditosEn = api.getTvPeople(mediaId, "en-US")
            mergeCreditos(creditosEs.cast, creditosEn.cast)
        } catch (e: Exception) {
            Log.w("CreditosManager", "Error al obtener creditos en ingles: ${e.message}")
            creditosEs.cast
        }
    } catch (e: Exception) {
        try {
            api.getTvPeople(mediaId, "en-US").cast
        } catch (e: Exception) {
            Log.e("CreditosManager", "Error al obtener creditos: ${e.message}")
            emptyList()
        }
    }
}

/**
 * Combina listas de creditos de reparto en diferentes idiomas de forma inteligente.
 *
 * Implementa una estrategia de fusion que:
 * 1. Prioriza informacion en español cuando esta disponible
 * 2. Complementa campos vacios con informacion en ingles
 * 3. Añade actores que solo aparecen en la lista en ingles
 * 4. Mantiene la integridad de los datos combinados
 *
 * @param castEs Lista de actores con informacion en español
 * @param castEn Lista de actores con informacion en ingles
 * @return Lista combinada y optimizada de [People]
 */
private fun mergeCreditos(castEs: List<People>, castEn: List<People>): List<People> {
    // Crear mapa indexado por ID para busquedas O(1)
    val mapaEn = castEn.associateBy { it.id }

    // Combinar datos existentes en español con complementos en ingles
    val creditosCombinados = castEs.map { personaEs ->
        val personaEn = mapaEn[personaEs.id]

        // Combinacion campo por campo, priorizando español
        personaEs.copy(
            name = personaEs.name?.ifEmpty { personaEn?.name ?: "" },
            character = personaEs.character?.ifEmpty { personaEn?.character ?: "" },
            profile_path = personaEs.profile_path.ifEmpty { personaEn?.profile_path ?: "" },
            roles = personaEs.roles?.let { mergeRoles(it, personaEn?.roles ?: emptyList()) }
        )
    }

    // Añadir actores que solo estan presentes en la lista en ingles
    val actoresExclusivosEn = castEn.filterNot { en ->
        castEs.any { es -> es.id == en.id }
    }

    return creditosCombinados + actoresExclusivosEn
}

/**
 * Combina listas de roles de actores en diferentes idiomas.
 *
 * Especifico para series de TV, mantiene la informacion de roles en español
 * pero actualiza datos numericos como el conteo de episodios desde la fuente
 * en ingles cuando sea mas precisa.
 *
 * @param rolesEs Lista de roles con informacion en español
 * @param rolesEn Lista de roles con informacion en ingles
 * @return Lista combinada de [Roles] con informacion optimizada
 */
private fun mergeRoles(rolesEs: List<Roles>, rolesEn: List<Roles>): List<Roles> {
    // Indexar por credit_id para busquedas eficientes
    val mapaEn = rolesEn.associateBy { it.credit_id }

    // Actualizar roles existentes con datos complementarios
    val rolesActualizados = rolesEs.map { rolEs ->
        val rolEn = mapaEn[rolEs.credit_id]

        rolEs.copy(
            // Usar conteo de episodios en ingles si es mas preciso
            episode_count = rolEn?.episode_count ?: rolEs.episode_count
        )
    }

    // Añadir roles que solo existen en ingles
    val rolesExclusivosEn = rolesEn.filterNot { en ->
        rolesEs.any { es -> es.credit_id == en.credit_id }
    }

    return rolesActualizados + rolesExclusivosEn
}

// ==================== UTILIDADES DE CONVERSIoN ====================

/**
 * Convierte una medida en density-independent pixels (dp) a pixeles fisicos.
 *
 * Los dp son unidades independientes de la densidad que garantizan que los
 * elementos UI mantengan el mismo tamaño fisico en diferentes dispositivos,
 * independientemente de la densidad de pantalla.
 *
 * @receiver Int Valor en dp a convertir
 * @param context Contexto necesario para acceder a las metricas de pantalla
 * @return Valor equivalente en pixeles fisicos para el dispositivo actual
 *
 */
fun Int.dpToPx(context: Context): Int {
    // Obtener factor de conversion basado en densidad de pantalla
    return (this * context.resources.displayMetrics.density).toInt()
}

// ==================== GESTIoN DE COLORES DINaMICOS ====================

/**
 * Manager singleton para gestion de colores predominantes en la interfaz de usuario.
 *
 * Proporciona un sistema centralizado para:
 * - Calcular y almacenar colores dominantes de imagenes
 * - Generar variaciones de color (mas oscuro/claro)
 * - Determinar automaticamente si usar texto claro u oscuro
 * - Mantener consistencia visual en toda la aplicacion
 *
 * Se utiliza tipicamente para crear temas dinamicos basados en posters de peliculas/series.
 *
 * @property averageColor Color promedio extraido de la imagen actual
 * @property darkerColor Version mas oscura del color promedio (80% de luminancia)
 * @property isDark Flag que indica si el color promedio es considerado oscuro
 */
object ColorManager {
    /** Color promedio actual, inicializado como transparente */
    var averageColor: Int = Color.TRANSPARENT

    /** Version oscurecida del color promedio para gradientes/sombras */
    var darkerColor: Int = Color.TRANSPARENT

    /** Flag indicando si el color actual es oscuro (para seleccion de texto) */
    var isDark: Boolean = false

    /**
     * Actualiza todos los colores gestionados basandose en una imagen bitmap.
     *
     * Proceso de actualizacion:
     * 1. Calcula el color promedio de la imagen
     * 2. Determina si es un color oscuro o claro
     * 3. Genera una version mas oscura para efectos visuales
     *
     * @param bitmap Imagen fuente para extraer colores (tipicamente poster de pelicula/serie)
     */
    fun updateFromBitmap(bitmap: Bitmap) {
        // Extraer color dominante de la imagen
        averageColor = calcularColorPromedio(bitmap)

        // Determinar luminancia para seleccion automatica de texto
        isDark = averageColor.isDarkColor()

        // Generar variacion mas oscura usando HSV
        val hsv = FloatArray(3)
        Color.colorToHSV(averageColor, hsv) // Convertir a Hue-Saturation-Value
        hsv[2] *= 0.8f // Reducir Value (luminancia) al 80%
        darkerColor = Color.HSVToColor(hsv) // Reconvertir a RGB
    }
}

// ==================== INTERFACES DE CALLBACK ====================

/**
 * Interfaz de callback para notificar cuando se han cargado completamente los detalles.
 *
 * Permite implementar un patron observer para coordinar actualizaciones de UI
 * una vez que se han obtenido y procesado todos los datos necesarios de la API.
 *
 * Tipicamente utilizada para:
 * - Ocultar indicadores de carga
 * - Activar animaciones de entrada
 * - Inicializar elementos UI dependientes de datos
 */
interface OnDetailsLoadedListener {
    /**
     * Llamado cuando todos los detalles han sido cargados y procesados.
     *
     * Este metodo se ejecuta en el hilo principal y es seguro para
     * realizar actualizaciones de UI directamente.
     */
    fun onDetailsLoaded()
}

// ==================== FORMATEO DE DATOS ====================

/**
 * Formatea una fecha en formato ISO (yyyy-MM-dd) a formato legible en español.
 *
 * Convierte fechas del formato estandar de API a un formato amigable para el usuario,
 * utilizando nombres de meses en español completos.
 *
 * @param fechaOriginal Fecha en formato ISO 8601 (ejemplo: "2023-12-25")
 * @return Fecha formateada en español (ejemplo: "25 de diciembre de 2023")
 *         o la fecha original si ocurre algun error en el procesamiento
 *
 */
fun formatDate(fechaOriginal: String): String {
    return try {
        // Parser para formato ISO estandar
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val fecha = LocalDate.parse(fechaOriginal, dateFormatter)

        // Extraccion de componentes de fecha
        val dia = fecha.dayOfMonth
        val mes = Meses.fromNumero(fecha.monthValue) // Enum personalizado para meses en español
        val anio = fecha.year

        // Construccion de fecha formateada
        if (mes != null) {
            "$dia de ${mes.completo} de $anio"
        } else {
            fechaOriginal // Fallback si hay problema con el enum de meses
        }
    } catch (e: Exception) {
        fechaOriginal // Fallback en caso de error
    }
}

/**
 * Expande una imagen con animacion de escala y muestra una nota asociada.
 *
 * Esta funcion aplica una animacion de escalado suave a un ImageView, aumentando
 * ligeramente su tamaño para crear un efecto visual de expansion. Simultaneamente,
 * hace visible un ConstraintLayout que contiene informacion adicional (nota).
 *
 * @param imageView La vista de imagen que se va a expandir con la animacion
 * @param nota El ConstraintLayout que contiene la nota a mostrar
 *
 * @see resetImage Para revertir los cambios de escala aplicados
 */
fun expandImage(imageView: ImageView, nota: ConstraintLayout) {
    // Aplica animacion de escalado con factores ligeramente superiores a 1.0
    imageView.animate()
        .scaleX(1.05f) // Escala horizontal al 105%
        .scaleY(1.05f) // Escala vertical al 105%
        .setDuration(100) // Duracion de la animacion en milisegundos
        .start()

    // Hace visible la nota asociada a la imagen
    nota.visibility = View.VISIBLE
}

/**
 * Restaura una imagen a su estado original, cancelando animaciones y resetando transformaciones.
 *
 * Esta funcion cancela cualquier animacion en curso en el ImageView y restaura todas
 * las propiedades de transformacion (escala, traslacion) a sus valores por defecto.
 * Es util para limpiar el estado visual despues de aplicar efectos de animacion.
 *
 * @param imageView La vista de imagen que se va a restaurar a su estado original
 *
 * @see expandImage Para aplicar efectos de expansion que esta funcion puede revertir
 */
fun resetImage(imageView: ImageView) {
    imageView.animate().cancel() // Cancela cualquier animacion pendiente o en ejecucion

    // Restaura la escala a tamaño normal (100%)
    imageView.scaleX = 1f
    imageView.scaleY = 1f

    // Elimina cualquier desplazamiento aplicado
    imageView.translationX = 0f
    imageView.translationY = 0f

    // Fuerza el redibujado de la vista para aplicar los cambios inmediatamente
    imageView.invalidate()
}

/**
 * Utilidades para la manipulacion y creacion de elementos visuales en Android.
 *
 * Este objeto singleton proporciona funciones de utilidad para crear y modificar
 * elementos de interfaz de usuario, especialmente relacionados con la apariencia
 * visual y los fondos de las vistas.
 */
object ViewUtils {

    /**
     * Crea un fondo redondeado con un color solido y dimensiones minimas especificadas.
     *
     * Esta funcion genera un Drawable personalizado con esquinas redondeadas que puede
     * ser aplicado como fondo a cualquier vista de Android. Permite especificar el radio
     * de las esquinas, el color de fondo y las dimensiones minimas del drawable.
     *
     * @param context Contexto de la aplicacion necesario para acceder a los recursos del sistema
     * @param cornerRadiusDp Radio de las esquinas redondeadas expresado en density-independent pixels (dp)
     * @param color Color del fondo en formato hexadecimal (ej: "#FF5722", "#BLUE")
     * @param minWidth Ancho minimo del drawable en pixeles (por defecto: 100px)
     * @param minHeight Alto minimo del drawable en pixeles (por defecto: 48px)
     *
     * @return Un [Drawable] de tipo [GradientDrawable] configurado con las especificaciones dadas
     *
     * @throws IllegalArgumentException Si el formato del color no es valido
     */
    fun createRoundedBackground(
        context: Context,
        cornerRadiusDp: Float,
        color: String,
        minWidth: Int = 100,  // Ancho minimo en pixeles
        minHeight: Int = 48    // Alto minimo en pixeles
    ): Drawable {
        // Convierte el valor en dp a pixeles segun la densidad de la pantalla
        val radius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            cornerRadiusDp,
            context.resources.displayMetrics
        )

        // Parsea el color desde formato hexadecimal a entero
        val colorInt = Color.parseColor(color)

        // Crea y configura el GradientDrawable con las especificaciones
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE // Define la forma como rectangulo
            setColor(colorInt) // Aplica el color de fondo
            cornerRadius = radius // Establece el radio de las esquinas redondeadas
            // Establece las dimensiones minimas del drawable
            setBounds(0, 0, minWidth, minHeight)
        }
    }
}