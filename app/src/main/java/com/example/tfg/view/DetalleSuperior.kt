package com.example.tfg.view

import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.TextViewCompat
import com.example.tfg.R
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.utils.fetchWithLanguageFallback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DetalleSuperior(private val context: Context,
                      private val b: ActivityDetallesBinding,
                      private val id: Int,
                      private val type: String) {

    val api: APImedia = getRetrofit().create(APImedia::class.java)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            llamarContenido()
        }
    }

    private fun llamarContenido() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaItem: MediaItem = when (type.lowercase()) {
                    "movie" -> getMovieDetailsWithFallback(id)
                    "tv" -> getTvDetailsWithFallback(id)
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Tipo de media no válido: $type", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }

                withContext(Dispatchers.Main) {
                    mostrarDetalles(mediaItem)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    println("Error obteniendo detalles: ${e.message}")
                    e.printStackTrace()
                    Toast.makeText(context, "Error cargando detalles: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun getMovieDetailsWithFallback(movieId: Int): Pelicula {
        // Primero intentamos obtener todos los datos en español
        val peliculaEspañol = try {
            api.getMovieDetails(movieId, "es-ES").apply {
                // Asegurarnos que los campos requeridos no sean null
                media_type = media_type ?: "movie"
            }
        } catch (e: Exception) {
            null
        }

        // Comprobamos si faltan datos importantes en español
        val faltanDatos = peliculaEspañol?.let {
            it.title.isNullOrEmpty() || it.overview.isNullOrEmpty() ||
                    it.tagline.isNullOrEmpty() || it.poster_path.isNullOrEmpty() ||
                    it.backdrop_path.isNullOrEmpty()
        } ?: true

        // Si faltan datos, intentamos completar con inglés
        if (faltanDatos) {
            try {
                val peliculaIngles = api.getMovieDetails(movieId, "en-US").apply {
                    // Asegurarnos que los campos requeridos no sean null
                    media_type = media_type ?: "movie"
                }

                // Si no teníamos datos en español, devolvemos directamente los datos en inglés
                if (peliculaEspañol == null) {
                    return peliculaIngles
                }

                // Combinamos los datos, dando preferencia a los datos en español cuando existen
                return peliculaEspañol.copy(
                    title = peliculaEspañol.title.ifEmpty { peliculaIngles.title ?: "" },
                    overview = peliculaEspañol.overview.ifEmpty { peliculaIngles.overview ?: "" },
                    tagline = peliculaEspañol.tagline.ifEmpty { peliculaIngles.tagline ?: "" },
                    poster_path = peliculaEspañol.poster_path ?: peliculaIngles.poster_path,
                    backdrop_path = peliculaEspañol.backdrop_path ?: peliculaIngles.backdrop_path
                )
            } catch (e: Exception) {
                Log.w("DetalleSuperior", "Error al obtener detalles en inglés: ${e.message}")
                // Si falla el inglés, devolvemos lo que tenemos en español o lanzamos excepción
                return peliculaEspañol ?: throw e
            }
        }

        return peliculaEspañol ?: fetchWithLanguageFallback(
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

    private suspend fun getTvDetailsWithFallback(tvId: Int): TvShow {
        // Primero intentamos obtener todos los datos en español
        val tvShowEspañol = try {
            api.getTvDetails(tvId, "es-ES").apply {
                // Asegurarnos que los campos requeridos no sean null
                media_type = media_type ?: "tv"
            }
        } catch (e: Exception) {
            null
        }

        // Comprobamos si faltan datos importantes en español
        val faltanDatos = tvShowEspañol?.let {
            it.name.isNullOrEmpty() || it.overview.isNullOrEmpty() ||
                    it.tagline.isNullOrEmpty() || it.poster_path.isNullOrEmpty() ||
                    it.backdrop_path.isNullOrEmpty()
        } ?: true

        // Si faltan datos, intentamos completar con inglés
        if (faltanDatos) {
            try {
                val tvShowIngles = api.getTvDetails(tvId, "en-US").apply {
                    // Asegurarnos que los campos requeridos no sean null
                    media_type = media_type ?: "tv"
                }

                // Si no teníamos datos en español, devolvemos directamente los datos en inglés
                if (tvShowEspañol == null) {
                    return tvShowIngles
                }

                // Combinamos los datos, dando preferencia a los datos en español cuando existen
                return tvShowEspañol.copy(
                    name = tvShowEspañol.name.ifEmpty { tvShowIngles.name ?: "" },
                    overview = tvShowEspañol.overview.ifEmpty { tvShowIngles.overview ?: "" },
                    tagline = tvShowEspañol.tagline.ifEmpty { tvShowIngles.tagline ?: "" },
                    poster_path = tvShowEspañol.poster_path ?: tvShowIngles.poster_path,
                    backdrop_path = tvShowEspañol.backdrop_path ?: tvShowIngles.backdrop_path
                )
            } catch (e: Exception) {
                Log.w("DetalleSuperior", "Error al obtener detalles en inglés: ${e.message}")
                // Si falla el inglés, devolvemos lo que tenemos en español o lanzamos excepción
                return tvShowEspañol ?: throw e
            }
        }

        return tvShowEspañol ?: fetchWithLanguageFallback(
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

    private fun mostrarDetalles(mediaItem: MediaItem) {
        val binding = b.DetalleSuperior

        // Cargar imágenes
        Picasso.get()
            .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
            .resize(150, 225)
            .transform(RoundedCornersTransformation(16, 0))
            .into(binding.ivPoster)

        Picasso.get()
            .load("https://image.tmdb.org/t/p/w500${mediaItem.backdrop_path}")
            .resize(327, 256)
            .transform(RoundedCornersTransformation(16, 0))
            .into(binding.ivFondo)

        // Mostrar porcentaje de valoración
        mostrarPorcentaje(mediaItem, binding)

        // Configurar textos según el tipo (Película o TV)
        val fechaEstreno = when (mediaItem) {
            is Pelicula -> formatDate(mediaItem.release_date)
            is TvShow -> formatDate(mediaItem.first_air_date)
            else -> ""
        }

        // Asignar título y año
        binding.tvTitulo.text = when (mediaItem) {
            is Pelicula -> mediaItem.title
            is TvShow -> mediaItem.name
        }

        binding.tvFechaAnio.text = "(${fechaEstreno.takeLast(4)})"

        // Ajustar dinámicamente la posición del año (abajo o derecha)
        binding.tvTitulo.post {
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
            12,  // Tamaño mínimo (sp)
            26,  // Tamaño inicial (sp)
            1,   // Paso de reducción
            TypedValue.COMPLEX_UNIT_SP
        )

        val hasValidTagline = mediaItem.tagline?.trim().isNullOrEmpty().not()

        binding.tvTagline.visibility = if (hasValidTagline) {
            binding.tvTagline.text = mediaItem.tagline?.trim() // Asigna el texto (sin espacios extras)
            View.VISIBLE
        } else {
            binding.tvVistaGeneral.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = 10.dpToPx(context) // Convierte 10dp a píxeles
            }
            View.GONE
        }

        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            binding.tvTagline,
            12,
            26,
            1,
            TypedValue.COMPLEX_UNIT_SP
        )

        // Configurar datos adicionales (runtime, géneros, etc.)
        val datos = when (mediaItem) {
            is Pelicula -> {
                val runtime = if (mediaItem.runtime > 0) "${mediaItem.runtime / 60}h ${mediaItem.runtime % 60}m"
                else "Duración desconocida"
                "$fechaEstreno · $runtime\n${
                    mediaItem.genres.toString().replace("[", "")
                        .replace("]", "")
                }"
            }
            is TvShow -> {
                "$fechaEstreno\n${mediaItem.genres.toString().replace("[", "")
                    .replace("]", "")}"
            }
            else -> ""
        }
        binding.tvDatos.text = datos

        // Configurar resumen
        binding.tvResumen.text = mediaItem.overview ?: "Sin descripción disponible"
    }

    // Extensión para convertir dp a píxeles
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun mostrarPorcentaje(mediaItem: MediaItem, binding: com.example.tfg.databinding.ItemDetalleSuperiorBinding) {
        val porcentaje = (mediaItem.vote_average * 10).toInt()
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
    }

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

    private fun getRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}