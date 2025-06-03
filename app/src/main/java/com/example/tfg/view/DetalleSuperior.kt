package com.example.tfg.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.example.tfg.R
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.Seasons
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.OnDetailsLoadedListener
import com.example.tfg.utils.calcularColorPromedio
import com.example.tfg.utils.getMovieDetailsConFallback
import com.example.tfg.utils.getRetrofit
import com.example.tfg.utils.getTvDetailsConFallback
import com.example.tfg.utils.isDarkColor
import com.example.tfg.utils.dpToPx
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Controlador para la seccion superior de la pantalla de detalles de medios audiovisuales.
 *
 * Esta clase se encarga de gestionar la presentacion de informacion basica de peliculas y series de TV,
 * incluyendo la carga asincrona de datos desde la API de TMDB, el procesamiento de imagenes,
 * la extraccion de colores dinamicos para tematizacion, y la configuracion de la interfaz de usuario.
 *
 * Caracteristicas principales:
 * - Carga de datos desde API con manejo de errores y fallbacks
 * - Extraccion automatica de paleta de colores desde el poster
 * - Aplicacion de temas dinamicos basados en colores dominantes
 * - Formateo inteligente de texto con autoajuste de tamaño
 * - Gestion de memoria y recursos para evitar memory leaks
 * - Soporte para notificaciones de carga completada
 *
 * @param context Contexto de la aplicacion, debe implementar [LifecycleOwner] para operaciones asincronas seguras
 * @param b Binding de la actividad principal que contiene las vistas a modificar
 * @param id Identificador unico del medio en TMDB (pelicula o serie)
 * @param type Tipo de medio: "movie" para peliculas o "tv" para series de television
 * @param onDetailsLoadedListener Listener opcional para notificar cuando la carga de detalles se complete
 *
 * @see MediaItem
 * @see Pelicula
 * @see TvShow
 * @see ColorManager
 */
class DetalleSuperior(private val context: Context,
                      private val b: ActivityDetallesBinding,
                      private val id: Int,
                      private val type: String,
                      private val onDetailsLoadedListener: OnDetailsLoadedListener? = null) {

    /** Instancia de la API para realizar llamadas a TMDB */
    val api: APImedia = getRetrofit().create(APImedia::class.java)

    /** Target de Picasso para manejar la carga de imagenes y extraccion de colores */
    private var picassoTarget: com.squareup.picasso.Target? = null

    /** Flag que indica si los datos basicos del medio han sido cargados */
    private var isDataLoaded = false

    /** Flag que indica si los colores dinamicos han sido procesados y aplicados */
    private var isColorsLoaded = false

    /** Flag que indica si la clase ha sido limpiada y no debe realizar mas operaciones */
    private var isCleanedUp = false

    /** Job de corrutina principal para controlar la carga asincrona de datos */
    private var mainJob: Job? = null

    /** Almacena los datos de la serie cuando esta disponible, usado para acceso posterior */
    private var tvShowData: TvShow? = null

    init {
        llamarContenido() // Inicia automaticamente la carga de contenido
    }

    /**
     * Inicia el proceso de carga asincrona de los datos del medio desde la API.
     *
     * Este metodo es el punto de entrada principal para obtener informacion del medio.
     * Realiza las siguientes operaciones:
     * 1. Valida que el contexto sea compatible con operaciones asincronas
     * 2. Determina el tipo de medio (pelicula o serie) y llama al endpoint correspondiente
     * 3. Procesa la respuesta y actualiza la interfaz de usuario
     * 4. Maneja errores y estados de carga
     *
     * Utiliza corrutinas para operaciones de red y garantiza que las actualizaciones
     * de UI se realicen en el hilo principal.
     *
     * @throws IllegalStateException si el contexto no implementa [LifecycleOwner]
     */
    private fun llamarContenido() {
        // Verificar que el contexto sea un LifecycleOwner para operaciones asincronas seguras
        if (context !is LifecycleOwner) {
            handleError("Contexto invalido para operaciones asincronas")
            return
        }

        mainJob = context.lifecycleScope.launch {
            try {
                if (isCleanedUp) return@launch // Verificar si ya se ha limpiado

                // Determinar el tipo de medio y llamar al metodo correspondiente
                val mediaItem: MediaItem = when (type.lowercase()) {
                    "movie" -> getMovieDetailsConFallback(id, api) // Carga detalles de pelicula con fallback
                    "tv" -> getTvDetailsConFallback(id, api) // Carga detalles de serie con fallback
                    else -> {
                        handleError("Tipo de media no valido: $type")
                        return@launch
                    }
                }

                if (isCleanedUp) return@launch

                // Cambiar al hilo principal para actualizar la UI de forma segura
                context.lifecycleScope.launch {
                    if (!isCleanedUp && isContextValid()) {
                        mostrarDetalles(mediaItem)
                        // Guardar datos de serie para acceso posterior si es necesario
                        if (mediaItem is TvShow) {
                            tvShowData = mediaItem
                        }
                        isDataLoaded = true
                        checkIfLoadingComplete() // Verificar si la carga esta completa
                    }
                }
            } catch (e: Exception) {
                if (!isCleanedUp) {
                    handleError("Error cargando detalles: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Configura y muestra los detalles del medio en la interfaz de usuario.
     *
     * Este metodo es responsable de:
     * - Cargar imagenes (poster y fondo) con transformaciones y placeholders
     * - Configurar textos con autoajuste de tamaño
     * - Mostrar informacion especifica segun el tipo de medio
     * - Aplicar formateo dinamico para titulos largos
     * - Iniciar el proceso de extraccion de colores del poster
     *
     * @param mediaItem Objeto que contiene todos los datos del medio a mostrar
     *
     * @see Pelicula Para detalles especificos de peliculas
     * @see TvShow Para detalles especificos de series de TV
     */
    private fun mostrarDetalles(mediaItem: MediaItem) {
        if (isCleanedUp || !isContextValid()) return

        val binding = b.DetalleSuperior

        try {
            // Configurar un fondo temporal transparente mientras se cargan los colores
            binding.main.setBackgroundColor(Color.TRANSPARENT)

            // Cargar poster con esquinas redondeadas y manejo de errores
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
                .placeholder(R.drawable.media_carga) // Imagen mientras carga
                .error(R.drawable.media_carga) // Imagen si falla la carga
                .fit()
                .centerInside()
                .transform(RoundedCornersTransformation(16, 0)) // Esquinas redondeadas
                .into(binding.ivPoster)

            // Cargar imagen de fondo con mayor resolucion
            Picasso.get()
                .load("https://image.tmdb.org/t/p/original${mediaItem.backdrop_path}")
                .placeholder(R.drawable.imagen_carga)
                .error(R.drawable.imagen_carga)
                .fit()
                .centerInside()
                .transform(RoundedCornersTransformation(16, 0))
                .into(binding.ivFondo)

            // Mostrar valoracion como porcentaje con indicador visual
            mostrarPorcentaje(mediaItem, binding)

            // Extraer y formatear fecha de estreno segun el tipo de medio
            val fechaEstreno = when (mediaItem) {
                is Pelicula -> mediaItem.release_date?.let { formatDate(it) }
                is TvShow -> mediaItem.first_air_date?.let { formatDate(it) }
            }

            // Configurar titulo segun el tipo de medio
            binding.tvTitulo.text = when (mediaItem) {
                is Pelicula -> mediaItem.title
                is TvShow -> mediaItem.name
            }

            // Mostrar solo el año entre parentesis
            binding.tvFechaAnio.text = "(${fechaEstreno?.takeLast(4)})"

            // Ajustar dinamicamente la posicion del año si el titulo es muy largo
            binding.tvTitulo.post {
                if (isCleanedUp || !isContextValid()) return@post

                val maxWidthPx = 320.dpToPx(context) // Ancho maximo permitido
                if (binding.tvTitulo.width > maxWidthPx) {
                    // Mover el año debajo del titulo si es necesario
                    val params = binding.tvFechaAnio.layoutParams as ConstraintLayout.LayoutParams
                    params.topToBottom = R.id.tvTitulo
                    params.startToEnd = ConstraintLayout.LayoutParams.UNSET
                    params.topMargin = 4.dpToPx(context)
                    binding.tvFechaAnio.layoutParams = params
                }
            }

            // Configurar autoajuste del tamaño del titulo (12sp-26sp)
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.tvTitulo,
                12, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )

            // Mostrar tagline solo si existe y no esta vacio
            val hasValidTagline = mediaItem.tagline?.trim().isNullOrEmpty().not()

            binding.tvTagline.visibility = if (hasValidTagline) {
                binding.tvTagline.text = mediaItem.tagline?.trim()
                View.VISIBLE
            } else {
                // Ajustar margen superior si no hay tagline
                binding.tvVistaGeneral.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 10.dpToPx(context)
                }
                View.GONE
            }

            // Autoajuste para el tagline
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.tvTagline, 12, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )

            // Configurar datos especificos segun el tipo de medio
            val datos = when (mediaItem) {
                is Pelicula -> {
                    // Para peliculas: mostrar duracion formateada
                    val runtime = if (mediaItem.runtime!! > 0)
                        "${mediaItem.runtime.div(60)}h ${mediaItem.runtime.rem(60)}m" // Convertir a horas y minutos
                    else "Duracion desconocida"
                    "$fechaEstreno · $runtime\n${mediaItem.genres.toString().replace("[", "").replace("]", "")}"
                }
                is TvShow -> {
                    // Para series: solo fecha y generos
                    "$fechaEstreno\n${mediaItem.genres.toString().replace("[", "").replace("]", "")}"
                }
            }
            binding.tvDatos.text = datos

            // Configurar sinopsis con texto por defecto si no esta disponible
            binding.tvResumen.text = mediaItem.overview ?: "Sin descripcion disponible"

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.tvResumen, 10, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )

            // Limpiar el Target anterior para evitar memory leaks
            cleanupPicassoTarget()

            // Crear nuevo target para extraccion de colores del poster
            picassoTarget = object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    if (isCleanedUp || !isContextValid()) return

                    if (bitmap != null) {
                        processBitmapColors(bitmap, binding) // Procesar colores dinamicos
                    } else {
                        useDefaultColors(binding) // Usar colores por defecto
                    }
                    isColorsLoaded = true
                    checkIfLoadingComplete()
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    if (isCleanedUp || !isContextValid()) return

                    useDefaultColors(binding)
                    isColorsLoaded = true
                    checkIfLoadingComplete()
                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    // Opcional: mostrar placeholder durante la carga
                }
            }

            // Iniciar carga de imagen para extraccion de colores
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
                .into(picassoTarget!!)

        } catch (e: Exception) {
            if (!isCleanedUp) {
                handleError("Error configurando la vista: ${e.message}")
            }
        }
    }

    /**
     * Procesa los colores dominantes de un bitmap para aplicar tematizacion dinamica.
     *
     * Utiliza la biblioteca Palette de Android para extraer colores representativos
     * del poster y aplicarlos como tema visual de la interfaz. Este proceso incluye:
     *
     * 1. Calculo del color promedio de la imagen
     * 2. Determinacion si el color es claro u oscuro para ajustar contraste
     * 3. Aplicacion de colores de texto apropiados
     * 4. Creacion de variantes mas oscuras para fondos
     * 5. Generacion de gradientes dinamicos
     * 6. Actualizacion del ColorManager global
     *
     * @param bitmap Imagen del poster de la cual extraer los colores dominantes
     * @param binding Binding de la vista donde aplicar los colores extraidos
     *
     * @see Palette Biblioteca de Android para extraccion de colores
     * @see ColorManager Gestor global de colores de la aplicacion
     */
    private fun processBitmapColors(bitmap: Bitmap, binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        if (isCleanedUp || !isContextValid()) return

        try {
            Palette.from(bitmap).generate {
                if (isCleanedUp || !isContextValid()) return@generate

                // Calcular color promedio de toda la imagen
                val averageColor = calcularColorPromedio(bitmap)
                binding.main.setBackgroundColor(averageColor)

                // Determinar si el color es oscuro para ajustar el contraste del texto
                val isDark = averageColor.isDarkColor()
                val textColor = if (isDark) Color.WHITE else Color.BLACK

                // Aplicar el color de texto apropiado a todos los TextView
                binding.tvTitulo.setTextColor(textColor)
                binding.tvFechaAnio.setTextColor(textColor)
                binding.tvTagline.setTextColor(textColor)
                binding.tvDatos.setTextColor(textColor)
                binding.tvVistaGeneral.setTextColor(textColor)
                binding.tvResumen.setTextColor(textColor)

                // Crear una variante mas oscura del color para el fondo principal
                val hsv = FloatArray(3)
                Color.colorToHSV(averageColor, hsv)
                hsv[2] *= 0.8f // Reduce el brillo en un 20%
                val darkerColor = Color.HSVToColor(hsv)
                b.main.setBackgroundColor(darkerColor) // Aplicar al fondo principal

                // Crear gradiente dinamico de izquierda a derecha
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(averageColor, Color.TRANSPARENT) // De color solido a transparente
                ).apply {
                    cornerRadius = 0f
                    gradientType = GradientDrawable.LINEAR_GRADIENT
                }

                binding.bottomGradient.background = gradientDrawable

                // Actualizar el gestor global de colores para otras partes de la app
                ColorManager.updateFromBitmap(bitmap)

                // Pequeño delay para asegurar que todas las operaciones se completen
                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isCleanedUp && isContextValid()) {
                        isColorsLoaded = true
                        checkIfLoadingComplete()
                    }
                }, 50L)
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                useDefaultColors(binding) // Fallback a colores por defecto en caso de error
            }
        }
    }

    /**
     * Aplica una paleta de colores por defecto cuando no se pueden extraer del poster.
     *
     * Este metodo sirve como fallback cuando:
     * - La imagen del poster no se puede cargar
     * - Falla la extraccion de colores
     * - Ocurre una excepcion durante el procesamiento
     *
     * Configura un tema neutro con:
     * - Fondo blanco como color base
     * - Texto negro para maximo contraste
     * - Variante gris mas oscura para el fondo principal
     * - Gradiente por defecto
     * - Actualizacion del ColorManager con valores seguros
     *
     * @param binding Binding de la vista donde aplicar los colores por defecto
     */
    private fun useDefaultColors(binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Color blanco como base por defecto
            val defaultColor = ContextCompat.getColor(context, R.color.white)
            binding.main.setBackgroundColor(defaultColor)

            // Aplicar texto negro para maximo contraste
            val textColor = Color.BLACK
            binding.tvTitulo.setTextColor(textColor)
            binding.tvFechaAnio.setTextColor(textColor)
            binding.tvTagline.setTextColor(textColor)
            binding.tvDatos.setTextColor(textColor)
            binding.tvVistaGeneral.setTextColor(textColor)
            binding.tvResumen.setTextColor(textColor)

            // Crear version mas oscura del color por defecto
            val hsv = FloatArray(3)
            Color.colorToHSV(defaultColor, hsv)
            hsv[2] *= 0.8f // Reducir brillo
            val darkerColor = Color.HSVToColor(hsv)
            b.main.setBackgroundColor(darkerColor)

            // Gradiente por defecto
            val defaultGradient = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(defaultColor, Color.TRANSPARENT)
            )
            binding.bottomGradient.background = defaultGradient

            // Actualizar ColorManager con valores por defecto seguros
            ColorManager.averageColor = defaultColor
            ColorManager.darkerColor = darkerColor
            ColorManager.isDark = false
        } catch (e: Exception) {
            // Error silencioso si no se pueden aplicar colores por defecto
        }
    }

    /**
     * Verifica si tanto los datos como los colores han terminado de cargar completamente.
     *
     * Este metodo actua como sincronizador para dos procesos asincronos independientes:
     * 1. Carga de datos desde la API ([isDataLoaded])
     * 2. Procesamiento de colores desde la imagen ([isColorsLoaded])
     *
     * Solo cuando ambos procesos han terminado exitosamente, se notifica al listener
     * que la carga esta completa, permitiendo que otras partes de la aplicacion
     * procedan con operaciones que dependan de esta informacion.
     *
     * @see OnDetailsLoadedListener.onDetailsLoaded
     */
    private fun checkIfLoadingComplete() {
        if (isCleanedUp || !isContextValid()) return

        if (isDataLoaded && isColorsLoaded) {
            onDetailsLoadedListener?.onDetailsLoaded() // Notificar carga completa
        }
    }

    /**
     * Maneja errores durante el proceso de carga de datos y actualizacion de la interfaz.
     *
     * Proporciona un mecanismo centralizado para:
     * - Mostrar mensajes de error al usuario
     * - Registrar errores para debugging
     * - Notificar estados de error a los listeners
     * - Mantener la estabilidad de la aplicacion
     *
     * @param message Mensaje descriptivo del error ocurrido
     */
    private fun handleError(message: String) {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Mostrar error al usuario solo si el contexto es valido
            if (context is LifecycleOwner) {
                context.lifecycleScope.launch {
                    if (isContextValid()) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        notifyLoadingError() // Notificar error y aplicar fallbacks
                    }
                }
            }
        } catch (e: Exception) {
            // Error silencioso si no se puede mostrar el Toast
            notifyLoadingError()
        }
    }

    /**
     * Notifica un error de carga y aplica configuraciones por defecto para mantener la estabilidad.
     *
     * Este metodo se ejecuta cuando ocurre un error irrecuperable durante la carga.
     * Configura el ColorManager con valores seguros y notifica al listener que
     * la "carga" esta completa (aunque haya fallado) para evitar que la aplicacion
     * se quede esperando indefinidamente.
     */
    private fun notifyLoadingError() {
        if (isCleanedUp) return

        try {
            // Configurar ColorManager con valores por defecto seguros
            ColorManager.averageColor = ContextCompat.getColor(context, R.color.white)
            ColorManager.darkerColor = ContextCompat.getColor(context, android.R.color.darker_gray)
            ColorManager.isDark = false

            // Notificar que la carga esta "completa" aunque haya fallado
            onDetailsLoadedListener?.onDetailsLoaded()
        } catch (e: Exception) {
            // Error silencioso para evitar crashes en cascada
        }
    }

    /**
     * Configura y muestra la valoracion del medio como porcentaje con indicador visual.
     *
     * Convierte la puntuacion de TMDB (0.0-10.0) a porcentaje (0-100) y:
     * - Muestra el porcentaje como texto
     * - Configura un ProgressBar circular con el valor correspondiente
     * - Aplica colores diferentes segun el rango de puntuacion:
     *   * Verde: 70% o mas (excelente)
     *   * Amarillo: 30-69% (regular)
     *   * Rojo: menos de 30% (malo)
     *   * Gris: 0% (sin puntuacion)
     *
     * @param mediaItem Objeto que contiene la valoracion del medio
     * @param binding Binding donde mostrar la valoracion visual
     */
    private fun mostrarPorcentaje(mediaItem: MediaItem, binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Convertir puntuacion de 0-10 a porcentaje 0-100
            val porcentaje = (mediaItem.vote_average?.times(10))?.toInt() ?: 0
            binding.tvNota.text = "$porcentaje%"

            // Configurar progreso del ProgressBar circular
            binding.pbNota.progress = porcentaje
            binding.pbNota.secondaryProgress = porcentaje

            // Seleccionar drawable segun el rango de puntuacion
            val drawableRes = when {
                porcentaje == 0 -> R.drawable.circular_progress_null // Sin puntuacion
                porcentaje >= 70 -> R.drawable.circular_progress_green // Excelente
                porcentaje in 30..69 -> R.drawable.circular_progress_yellow // Regular
                else -> R.drawable.circular_progress_red // Malo
            }

            // Aplicar el drawable y configurar el nivel de progreso
            val drawable = ContextCompat.getDrawable(context, drawableRes)
            binding.pbNota.progressDrawable = drawable
            binding.pbNota.progressDrawable.level = porcentaje * 100 // Escalar para el drawable
            binding.pbNota.invalidate() // Forzar redibujado
        } catch (e: Exception) {
            // Error silencioso si no se puede mostrar la valoracion
        }
    }

    /**
     * Convierte una fecha del formato ISO (yyyy-MM-dd) al formato local (dd/MM/yyyy).
     *
     * Utiliza las APIs modernas de Java Time para parsing y formateo seguro de fechas.
     * Proporciona mensajes descriptivos en caso de fechas invalidas o vacias.
     *
     * @param fechaOriginal Cadena con la fecha en formato "yyyy-MM-dd" (formato TMDB)
     * @return Cadena con la fecha formateada como "dd/MM/yyyy" o mensaje de error
     *
     * @see DateTimeFormatter Para patrones de formateo de fechas
     * @see LocalDate Para manipulacion de fechas sin zona horaria
     */
    private fun formatDate(fechaOriginal: String): String {
        if (fechaOriginal.isBlank()) return "Fecha no disponible"

        try {
            val formatterEntrada = DateTimeFormatter.ofPattern("yyyy-MM-dd") // Formato de entrada TMDB
            val fecha = LocalDate.parse(fechaOriginal, formatterEntrada)

            // Extraer componentes individuales para formato personalizado
            val dia = fecha.dayOfMonth
            val mes = fecha.monthValue
            val anio = fecha.year

            return "$dia/$mes/$anio" // Formato local dd/MM/yyyy
        } catch (e: Exception) {
            return "Fecha no valida" // Fallback para fechas malformadas
        }
    }

    /**
     * Verifica si el contexto de la aplicacion sigue siendo valido para operaciones UI.
     *
     * Previene crashes y memory leaks verificando que:
     * - La actividad no ha sido destruida
     * - La actividad no esta en proceso de finalizacion
     * - El contexto es accesible para operaciones de UI
     *
     * @return true si el contexto es valido y seguro para usar, false en caso contrario
     */
    private fun isContextValid(): Boolean {
        return when (context) {
            is DetallesActivity -> !context.isDestroyed && !context.isFinishing
            else -> true // Para otros tipos de contexto, asumir valido
        }
    }

    /**
     * Limpia el target de Picasso para evitar memory leaks y referencias colgantes.
     *
     * Cancela cualquier peticion de imagen pendiente y libera la referencia al target.
     * Es crucial para evitar que Picasso mantenga referencias a vistas destruidas.
     */
    private fun cleanupPicassoTarget() {
        picassoTarget?.let {
            try {
                Picasso.get().cancelRequest(it) // Cancelar peticion pendiente
            } catch (e: Exception) {
                // Error silencioso si ya fue cancelada
            }
            picassoTarget = null // Liberar referencia
        }
    }

    /**
     * Libera todos los recursos y cancela operaciones pendientes.
     *
     * Metodo de limpieza que debe llamarse obligatoriamente cuando la clase
     * ya no se use (por ejemplo, en onDestroy de la Activity). Realiza:
     *
     * - Marca la clase como limpiada para evitar operaciones posteriores
     * - Cancela el job principal de corrutinas
     * - Limpia el target de Picasso
     * - Resetea todos los flags de estado
     *
     * **Importante**: Despues de llamar a este metodo, la instancia no debe usarse mas.
     */
    fun cleanup() {
        isCleanedUp = true

        // Cancelar job principal de corrutinas
        mainJob?.cancel()

        // Limpiar Picasso target para evitar memory leaks
        cleanupPicassoTarget()

        // Resetear flags de estado
        isDataLoaded = false
        isColorsLoaded = false
    }

    /**
     * Obtiene los datos de la serie de television cargados previamente desde la API.
     *
     * Este metodo proporciona acceso a la informacion completa de la serie de TV que fue
     * cargada durante el proceso de inicializacion de la clase. Los datos solo estaran
     * disponibles si:
     * - El tipo de medio es "tv" (serie de television)
     * - La carga desde la API se completo exitosamente
     * - No se ha llamado al metodo [cleanup]
     *
     * @return [TvShow] con todos los datos de la serie si esta disponible,
     *         o `null` si:
     *         - El medio no es una serie de TV
     *         - Los datos aun no se han cargado
     *         - Ocurrio un error durante la carga
     *         - La clase fue limpiada con [cleanup]
     *
     * @see TvShow Estructura de datos que contiene toda la informacion de la serie
     * @see Seasons Para acceder a informacion especifica de temporadas
     * @see llamarContenido Metodo que inicia la carga de datos desde la API
     *
     */
    fun getTvShow(): TvShow? {
        return tvShowData
    }
}