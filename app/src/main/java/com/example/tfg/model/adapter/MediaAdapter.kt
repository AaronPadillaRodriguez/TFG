package com.example.tfg.model.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.BuildConfig
import com.example.tfg.R
import com.example.tfg.databinding.ItemMediaBinding
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.ApiResponse
import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.model.enums.Meses
import com.example.tfg.utils.fetchWithLanguageFallback
import com.example.tfg.view.DetallesActivity
import com.google.gson.GsonBuilder
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


/**
 * Adaptador para mostrar elementos de medios (películas y series) en un RecyclerView.
 *
 * Este adaptador maneja la visualización de elementos de tipo MediaItem, que pueden
 * ser películas (Pelicula) o programas de TV (TvShow). Utiliza un patrón de diseño
 * ListAdapter para un manejo eficiente de los cambios en la lista mediante DiffUtil.
 *
 * @property onItemClick Función lambda que se ejecuta cuando se hace clic en un elemento
 *                      Recibe como parámetro el MediaItem seleccionado
 */
class MediaAdapter(private val onItemClick: (MediaItem) -> Unit) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private val items = mutableListOf<MediaItem>()

    fun submitList(newItems: List<MediaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size
    /**
     * Crea un nuevo ViewHolder para los elementos de la lista.
     *
     * @param parent El ViewGroup en el que se inflará la vista
     * @param viewType El tipo de vista (no utilizado en esta implementación)
     * @return Un nuevo MediaViewHolder que contiene la vista del elemento
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    /**
     * Vincula los datos del elemento en la posición especificada al ViewHolder.
     *
     * @param holder El ViewHolder al que vincular los datos
     * @param position La posición del elemento en la lista
     */
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    companion object {
        /**
         * Obtiene los datos de medios de la API TMDB y los establece en el adaptador.
         *
         * Esta función utiliza corrutinas para realizar la llamada a la API en un hilo secundario,
         * y actualiza la UI en el hilo principal una vez que se reciben los datos.
         *
         * @param context El contexto de la aplicación utilizado para mostrar mensajes Toast
         * @param mediaAdapter El adaptador de medios que se actualizará con los resultados
         * @param apiCall Función suspendida que realiza la llamada específica a la API
         */
        fun fetchMedia(context: Context, mediaAdapter: MediaAdapter, apiCall: suspend (APImedia) -> ApiResponse, shuffle: Boolean = false) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val api = getRetrofit().create(APImedia::class.java)
                    val response = apiCall(api)

                    withContext(Dispatchers.Main) {
                        if (response.results.isNullOrEmpty()) {
                            Toast.makeText(context, "No hay contenido disponible.", Toast.LENGTH_SHORT).show()
                        } else {
                            mediaAdapter.submitList(
                                if (shuffle) response.results.shuffled()
                                else response.results
                            )
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        /**
         * Construye y devuelve una instancia configurada de Retrofit.
         *
         * Configura Retrofit con la URL base de la API TMDB y un convertidor GSON
         * personalizado que maneja la deserialización de objetos MediaItem.
         *
         * @return Una instancia configurada de Retrofit
         */
        fun getRetrofit(): Retrofit =
            Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/")
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .registerTypeAdapter(MediaItem::class.java, MediaItemTypeAdapter())
                            .create()
                    )
                )
                .build()

        /**
         * Maneja el evento de clic en un elemento MediaItem.
         *
         * Muestra un Toast con el título del elemento seleccionado, diferenciando
         * entre películas y programas de TV.
         *
         * @param context El contexto de la aplicación para mostrar el Toast
         * @param mediaItem El elemento de medios seleccionado
         */
        fun onClickListener(context: Context, mediaItem: MediaItem) {
            val intent = Intent(context, DetallesActivity::class.java)

            val mediaType = when (mediaItem) {
                is Pelicula -> mediaItem.media_type ?: "movie"
                is TvShow -> mediaItem.media_type ?: "tv"
                else -> ""
            }

            intent.putExtra("ID", mediaItem.id)
            intent.putExtra("TYPE", mediaType)
            context.startActivity(intent)

            val title = when (mediaItem) {
                is Pelicula -> mediaItem.title
                is TvShow -> mediaItem.name
                else -> ""
            }

            println("Seleccionaste: $title y su id es ${mediaItem.id}")
            Toast.makeText(context, "Seleccionaste: $title y su id es ${mediaItem.id}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ViewHolder para elementos MediaItem que mantiene referencias a las vistas
     * necesarias para mostrar información de películas y series.
     *
     * @property itemView La vista de elemento individual
     */
    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val b = ItemMediaBinding.bind(itemView)

        /**
         * Vincula los datos de un MediaItem a las vistas correspondientes.
         *
         * Configura la visualización de la portada, el título, la fecha y el porcentaje de valoración,
         * y establece un listener para manejar los eventos de clic.
         *
         * @param mediaItem El elemento de medios a mostrar
         * @param onItemClick La función a llamar cuando se hace clic en este elemento
         */
        fun bind(mediaItem: MediaItem, onItemClick: (MediaItem) -> Unit) {
            porcentaje(mediaItem, itemView)
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
                .resize(150, 225)
                .transform(RoundedCornersTransformation(16, 0))
                .into(b.ivPortada)

            when (mediaItem) {
                is Pelicula -> {
                    b.tvNombre.text = mediaItem.title
                    b.tvFechaEstreno.text = formatDate(mediaItem.release_date)
                }
                is TvShow -> {
                    b.tvNombre.text = mediaItem.name
                    b.tvFechaEstreno.text = formatDate(mediaItem.first_air_date)
                }
            }

            itemView.setOnClickListener { onItemClick(mediaItem) }
        }

        private fun porcentaje(mediaItem: MediaItem, itemView: View) {
            val porcentaje = (mediaItem.vote_average * 10).toInt()
            b.tvNota.text = "$porcentaje%"

            b.pbNota.progress = porcentaje
            b.pbNota.secondaryProgress = porcentaje

            val drawableRes = when {
                porcentaje == 0 -> R.drawable.circular_progress_null
                porcentaje >= 70 -> R.drawable.circular_progress_green
                porcentaje in 30..69 -> R.drawable.circular_progress_yellow
                else -> R.drawable.circular_progress_red
            }

            val drawable = ContextCompat.getDrawable(itemView.context, drawableRes)
            b.pbNota.progressDrawable = drawable

            b.pbNota.progressDrawable.level = porcentaje * 100
            b.pbNota.invalidate()
        }

        /**
         * Formatea una fecha en formato ISO (yyyy-MM-dd) a un formato más legible (dd mmm yyyy).
         *
         * @param fechaOriginal La fecha en formato ISO (yyyy-MM-dd)
         * @return La fecha formateada como "día mes año" (ej. "12 ene 2023")
         * @throws IllegalArgumentException Si el mes no es válido
         */
        private fun formatDate(fechaOriginal: String): String {
            val formatterEntrada = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val fecha = LocalDate.parse(fechaOriginal, formatterEntrada)
            val dia = fecha.dayOfMonth
            val mes = Meses.fromNumero(fecha.monthValue)
            val anio = fecha.year

            return if (mes != null) {
                "$dia ${mes.abreviatura.lowercase()} $anio"
            } else {
                throw IllegalArgumentException("El mes no es válido.")
            }
        }
    }
}
