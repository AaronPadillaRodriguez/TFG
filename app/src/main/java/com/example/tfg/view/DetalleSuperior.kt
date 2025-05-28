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
 * Clase que maneja la seccion superior de la pantalla de detalles.
 * Se encarga de cargar y mostrar la informacion basica de la media (pelicula o serie),
 * incluyendo imagenes, titulo, fecha, generos, sinopsis y valoracion.
 * Tambien extrae y aplica colores dinamicos basados en el poster.
 */
class DetalleSuperior(private val context: Context,
                      private val b: ActivityDetallesBinding,
                      private val id: Int,
                      private val type: String,
                      private val onDetailsLoadedListener: OnDetailsLoadedListener? = null) {

    val api: APImedia = getRetrofit().create(APImedia::class.java)
    private var picassoTarget: com.squareup.picasso.Target? = null
    private var isDataLoaded = false
    private var isColorsLoaded = false
    private var isCleanedUp = false

    // Job para controlar la corrutina principal
    private var mainJob: Job? = null

    // Variable para almacenar el TvShow cuando esté disponible
    private var tvShowData: TvShow? = null

    init {
        llamarContenido()
    }

    /**
     * Inicia la carga de los datos de la media.
     * Carga los detalles desde la API y procesa la respuesta.
     */
    private fun llamarContenido() {
        // Verificar que el contexto sea un LifecycleOwner
        if (context !is LifecycleOwner) {
            handleError("Contexto inválido para operaciones asíncronas")
            return
        }

        mainJob = context.lifecycleScope.launch {
            try {
                if (isCleanedUp) return@launch

                val mediaItem: MediaItem = when (type.lowercase()) {
                    "movie" -> getMovieDetailsConFallback(id, api)
                    "tv" -> getTvDetailsConFallback(id, api)
                    else -> {
                        handleError("Tipo de media no válido: $type")
                        return@launch
                    }
                }

                if (isCleanedUp) return@launch

                // Cambiar al hilo principal para actualizar la UI
                context.lifecycleScope.launch {
                    if (!isCleanedUp && isContextValid()) {
                        mostrarDetalles(mediaItem)
                        if (mediaItem is TvShow) {
                            tvShowData = mediaItem
                        }
                        isDataLoaded = true
                        checkIfLoadingComplete()
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
     * Muestra los detalles de la media en la interfaz.
     * @param mediaItem Objeto con los datos de la media a mostrar.
     */
    private fun mostrarDetalles(mediaItem: MediaItem) {
        if (isCleanedUp || !isContextValid()) return

        val binding = b.DetalleSuperior

        try {
            // Configurar un fondo temporal
            binding.main.setBackgroundColor(Color.TRANSPARENT)

            // Cargar imágenes y configurar otros datos primero
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
                .placeholder(R.drawable.media_carga)
                .error(R.drawable.media_carga)
                .fit()
                .centerInside()
                .transform(RoundedCornersTransformation(16, 0))
                .into(binding.ivPoster)

            Picasso.get()
                .load("https://image.tmdb.org/t/p/original${mediaItem.backdrop_path}")
                .placeholder(R.drawable.imagen_carga)
                .error(R.drawable.imagen_carga)
                .fit()
                .centerInside()
                .transform(RoundedCornersTransformation(16, 0))
                .into(binding.ivFondo)

            // Mostrar porcentaje de valoración
            mostrarPorcentaje(mediaItem, binding)

            // Configurar textos según el tipo (Película o TV)
            val fechaEstreno = when (mediaItem) {
                is Pelicula -> mediaItem.release_date?.let { formatDate(it) }
                is TvShow -> mediaItem.first_air_date?.let { formatDate(it) }
            }

            // Asignar título y año
            binding.tvTitulo.text = when (mediaItem) {
                is Pelicula -> mediaItem.title
                is TvShow -> mediaItem.name
            }

            binding.tvFechaAnio.text = "(${fechaEstreno?.takeLast(4)})"

            // Ajustar dinámicamente la posición del año
            binding.tvTitulo.post {
                if (isCleanedUp || !isContextValid()) return@post

                val maxWidthPx = 320.dpToPx(context)
                if (binding.tvTitulo.width > maxWidthPx) {
                    val params = binding.tvFechaAnio.layoutParams as ConstraintLayout.LayoutParams
                    params.topToBottom = R.id.tvTitulo
                    params.startToEnd = ConstraintLayout.LayoutParams.UNSET
                    params.topMargin = 4.dpToPx(context)
                    binding.tvFechaAnio.layoutParams = params
                }
            }

            // Configurar autoajuste del título
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.tvTitulo,
                12, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )

            val hasValidTagline = mediaItem.tagline?.trim().isNullOrEmpty().not()

            binding.tvTagline.visibility = if (hasValidTagline) {
                binding.tvTagline.text = mediaItem.tagline?.trim()
                View.VISIBLE
            } else {
                binding.tvVistaGeneral.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = 10.dpToPx(context)
                }
                View.GONE
            }

            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                binding.tvTagline, 12, 26, 1, TypedValue.COMPLEX_UNIT_SP
            )

            // Configurar datos adicionales
            val datos = when (mediaItem) {
                is Pelicula -> {
                    val runtime = if (mediaItem.runtime!! > 0)
                        "${mediaItem.runtime.div(60)}h ${mediaItem.runtime.rem(60)}m"
                    else "Duración desconocida"
                    "$fechaEstreno · $runtime\n${mediaItem.genres.toString().replace("[", "").replace("]", "")}"
                }
                is TvShow -> {
                    "$fechaEstreno\n${mediaItem.genres.toString().replace("[", "").replace("]", "")}"
                }
            }
            binding.tvDatos.text = datos

            // Configurar resumen
            binding.tvResumen.text = mediaItem.overview ?: "Sin descripción disponible"

            // Limpiar el Target anterior si existe
            cleanupPicassoTarget()

            // Cargar el color de fondo
            picassoTarget = object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    if (isCleanedUp || !isContextValid()) return

                    if (bitmap != null) {
                        processBitmapColors(bitmap, binding)
                    } else {
                        useDefaultColors(binding)
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
                    // Opcional: mostrar placeholder
                }
            }

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
     * Procesa los colores de un bitmap para aplicar un tema dinamico.
     * Extrae colores dominantes y ajusta la interfaz en consecuencia.
     * @param bitmap Imagen de la que extraer los colores.
     * @param binding Binding de la vista donde aplicar los colores.
     */
    private fun processBitmapColors(bitmap: Bitmap, binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        if (isCleanedUp || !isContextValid()) return

        try {
            Palette.from(bitmap).generate {
                if (isCleanedUp || !isContextValid()) return@generate

                val averageColor = calcularColorPromedio(bitmap)
                binding.main.setBackgroundColor(averageColor)

                // Determinar si el color es oscuro
                val isDark = averageColor.isDarkColor()
                val textColor = if (isDark) Color.WHITE else Color.BLACK

                // Aplicar el color de texto a todos los TextView
                binding.tvTitulo.setTextColor(textColor)
                binding.tvFechaAnio.setTextColor(textColor)
                binding.tvTagline.setTextColor(textColor)
                binding.tvDatos.setTextColor(textColor)
                binding.tvVistaGeneral.setTextColor(textColor)
                binding.tvResumen.setTextColor(textColor)

                // Crear color más oscuro para el fondo principal
                val hsv = FloatArray(3)
                Color.colorToHSV(averageColor, hsv)
                hsv[2] *= 0.8f // Reduce el brillo en un 20%
                val darkerColor = Color.HSVToColor(hsv)
                b.main.setBackgroundColor(darkerColor)

                // Crear gradiente dinámico
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    intArrayOf(averageColor, Color.TRANSPARENT)
                ).apply {
                    cornerRadius = 0f
                    gradientType = GradientDrawable.LINEAR_GRADIENT
                }

                binding.bottomGradient.background = gradientDrawable

                // Actualizar ColorManager
                ColorManager.updateFromBitmap(bitmap)

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!isCleanedUp && isContextValid()) {
                        isColorsLoaded = true
                        checkIfLoadingComplete()
                    }
                }, 50L)
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                useDefaultColors(binding)
            }
        }
    }

    /**
     * Aplica colores por defecto cuando no se pueden extraer del poster.
     * @param binding Binding de la vista donde aplicar los colores.
     */
    private fun useDefaultColors(binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        if (isCleanedUp || !isContextValid()) return

        try {
            val defaultColor = ContextCompat.getColor(context, R.color.white)
            binding.main.setBackgroundColor(defaultColor)

            // Aplicar colores de texto por defecto
            val textColor = Color.BLACK
            binding.tvTitulo.setTextColor(textColor)
            binding.tvFechaAnio.setTextColor(textColor)
            binding.tvTagline.setTextColor(textColor)
            binding.tvDatos.setTextColor(textColor)
            binding.tvVistaGeneral.setTextColor(textColor)
            binding.tvResumen.setTextColor(textColor)

            // Crear color más oscuro
            val hsv = FloatArray(3)
            Color.colorToHSV(defaultColor, hsv)
            hsv[2] *= 0.8f
            val darkerColor = Color.HSVToColor(hsv)
            b.main.setBackgroundColor(darkerColor)

            // Gradiente por defecto
            val defaultGradient = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(defaultColor, Color.TRANSPARENT)
            )
            binding.bottomGradient.background = defaultGradient

            // Actualizar ColorManager con colores por defecto
            ColorManager.averageColor = defaultColor
            ColorManager.darkerColor = darkerColor
            ColorManager.isDark = false
        } catch (e: Exception) {
            // Error silencioso si no se pueden aplicar colores por defecto
        }
    }

    /**
     * Verifica si tanto los datos como los colores han terminado de cargar.
     * Si es asi, notifica al listener.
     */
    private fun checkIfLoadingComplete() {
        if (isCleanedUp || !isContextValid()) return

        if (isDataLoaded && isColorsLoaded) {
            onDetailsLoadedListener?.onDetailsLoaded()
        }
    }

    /**
     * Maneja errores durante la carga de datos.
     * @param message Mensaje de error a mostrar.
     */
    private fun handleError(message: String) {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Usar contexto de actividad si está disponible
            if (context is LifecycleOwner) {
                context.lifecycleScope.launch {
                    if (isContextValid()) {
                        println(message)
                        notifyLoadingError()
                    }
                }
            }
        } catch (e: Exception) {
            // Error silencioso
            notifyLoadingError()
        }
    }

    /**
     * Notifica un error de carga y aplica colores por defecto.
     */
    private fun notifyLoadingError() {
        if (isCleanedUp) return

        try {
            // Usar colores por defecto en caso de error
            ColorManager.averageColor = ContextCompat.getColor(context, R.color.white)
            ColorManager.darkerColor = ContextCompat.getColor(context, android.R.color.darker_gray)
            ColorManager.isDark = false

            // Notificar que la carga está "completa" aunque haya fallado
            onDetailsLoadedListener?.onDetailsLoaded()
        } catch (e: Exception) {
            // Error silencioso
        }
    }

    /**
     * Muestra la valoracion de la media como porcentaje.
     * @param mediaItem Objeto con los datos de la media.
     * @param binding Binding de la vista donde mostrar la valoracion.
     */
    private fun mostrarPorcentaje(mediaItem: MediaItem, binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        if (isCleanedUp || !isContextValid()) return

        try {
            val porcentaje = (mediaItem.vote_average?.times(10))?.toInt() ?: 0
            binding.tvNota.text = "$porcentaje%"

            binding.pbNota.progress = porcentaje
            binding.pbNota.secondaryProgress = porcentaje

            val drawableRes = when {
                porcentaje == 0 -> R.drawable.circular_progress_null
                porcentaje >= 70 -> R.drawable.circular_progress_green
                porcentaje in 30..69 -> R.drawable.circular_progress_yellow
                else -> R.drawable.circular_progress_red
            }

            val drawable = ContextCompat.getDrawable(context, drawableRes)
            binding.pbNota.progressDrawable = drawable
            binding.pbNota.progressDrawable.level = porcentaje * 100
            binding.pbNota.invalidate()
        } catch (e: Exception) {
            // Error silencioso
        }
    }

    /**
     * Formatea una fecha de "yyyy-MM-dd" a "dd/MM/yyyy".
     * @param fechaOriginal Cadena con la fecha original.
     * @return Cadena con la fecha formateada.
     */
    private fun formatDate(fechaOriginal: String): String {
        if (fechaOriginal.isBlank()) return "Fecha no disponible"

        try {
            val formatterEntrada = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fecha = LocalDate.parse(fechaOriginal, formatterEntrada)
            val dia = fecha.dayOfMonth
            val mes = fecha.monthValue
            val anio = fecha.year

            return "$dia/$mes/$anio"
        } catch (e: Exception) {
            return "Fecha no válida"
        }
    }

    /**
     * Verifica si el contexto sigue siendo valido.
     * @return true si el contexto es valido, false en caso contrario.
     */
    private fun isContextValid(): Boolean {
        return when (context) {
            is DetallesActivity -> !context.isDestroyed && !context.isFinishing
            else -> true
        }
    }

    /**
     * Limpia el target de Picasso para evitar memory leaks.
     */
    private fun cleanupPicassoTarget() {
        picassoTarget?.let {
            try {
                Picasso.get().cancelRequest(it)
            } catch (e: Exception) {
                // Error silencioso
            }
            picassoTarget = null
        }
    }

    /**
     * Libera recursos y cancela operaciones pendientes.
     * Debe llamarse cuando la clase ya no se use.
     */
    fun cleanup() {
        isCleanedUp = true

        // Cancelar job principal
        mainJob?.cancel()

        // Limpiar Picasso target
        cleanupPicassoTarget()

        // Resetear flags
        isDataLoaded = false
        isColorsLoaded = false
    }

    /**
     * Obtiene la lista de temporadas de una serie.
     * @return Lista de temporadas o lista vacia si no es una serie.
     */
    fun getTvShowList(): List<Seasons> {
        return tvShowData?.seasons ?: emptyList()
    }
}