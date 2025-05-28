package com.example.tfg.model.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.databinding.ItemMediaBinding
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.ApiResponse
import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.model.enums.Meses
import com.example.tfg.utils.getProgressDrawableRes
import com.example.tfg.view.DetallesActivity
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Adaptador para mostrar una lista de elementos multimedia (peliculas o series) en un RecyclerView.
 * Utiliza DiffUtil para actualizaciones eficientes y Picasso para cargar imagenes.
 *
 * @param onItemClick Funcion que se ejecuta al hacer clic en un elemento.
 */
class MediaAdapter(private val onItemClick: (MediaItem) -> Unit) :
    ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    /**
     * Callback para comparar elementos MediaItem y determinar cambios.
     */
    class MediaDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }

    // Metodos requeridos por RecyclerView.Adapter
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view, onItemClick)
    }

    // Metodos requeridos por RecyclerView.Adapter
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * Companion object que contiene metodos estaticos para:
     * - Configuracion de Retrofit
     * - Obtencion de datos de la API
     * - Manejo de clics en elementos
     */
    companion object {
        // Retrofit y API configurados como lazy para inicializacion unica
        private val retrofit: Retrofit by lazy {
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
        }

        private val api: APImedia by lazy {
            retrofit.create(APImedia::class.java)
        }

        /**
         * Obtiene datos multimedia de la API y los actualiza en el adaptador.
         *
         * @param context Contexto de la aplicacion.
         * @param mediaAdapter Adaptador a actualizar.
         * @param apiCall Funcion que realiza la llamada a la API.
         * @param shuffle Si es true, mezcla los resultados.
         */
        fun fetchMedia(
            context: Context,
            mediaAdapter: MediaAdapter,
            apiCall: suspend (APImedia) -> ApiResponse,
            shuffle: Boolean = false
        ) {
            // Usar un SupervisorJob para manejar mejor los errores
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    // Toda la lógica de red en IO dispatcher
                    val response = apiCall(api)

                    // Procesamiento de datos también en hilo secundario
                    val processedResults = if (response.results.isNullOrEmpty()) {
                        emptyList()
                    } else {
                        if (shuffle) response.results.shuffled() else response.results
                    }

                    // Solo cambiar al hilo principal para actualizar la UI
                    withContext(Dispatchers.Main) {
                        if (processedResults.isEmpty()) {
                            println("No hay contenido disponible.")
                        } else {
                            // submitList es más eficiente que notifyDataSetChanged
                            mediaAdapter.submitList(processedResults)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        println("Error: ${e.message}")
                    }
                }
            }
        }

        /**
         * Maneja el clic en un elemento multimedia.
         *
         * @param context Contexto de la aplicacion.
         * @param mediaItem Elemento multimedia seleccionado.
         */
        fun onClickListener(context: Context, mediaItem: MediaItem) {
            // Procesar datos en hilo secundario
            CoroutineScope(Dispatchers.Default).launch {
                val intent = Intent(context, DetallesActivity::class.java)

                val mediaType = when (mediaItem) {
                    is Pelicula -> mediaItem.media_type ?: "movie"
                    is TvShow -> mediaItem.media_type ?: "tv"
                }

                val title = when (mediaItem) {
                    is Pelicula -> mediaItem.title
                    is TvShow -> mediaItem.name
                }

                intent.putExtra("ID", mediaItem.id)
                intent.putExtra("TYPE", mediaType)

                // Solo actualizar UI en hilo principal
                withContext(Dispatchers.Main) {
                    context.startActivity(intent)
                    println("Seleccionaste: $title y su id es ${mediaItem.id}")
                }
            }
        }
    }

    /**
     * ViewHolder que muestra un elemento multimedia individual.
     */
    class MediaViewHolder(
        itemView: View,
        private val onItemClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val b = ItemMediaBinding.bind(itemView)

        // Cache para formatter para evitar crearlo cada vez
        companion object {
            private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }

        /**
         * Enlaza los datos del elemento multimedia a las vistas.
         *
         * @param mediaItem Elemento multimedia a mostrar.
         */
        fun bind(mediaItem: MediaItem) {
            // Configurar click listener una sola vez
            itemView.setOnClickListener { onItemClick(mediaItem) }

            // Procesar datos de manera asíncrona
            bindDataAsync(mediaItem)
        }

        // Metodos privados para manejo de datos y UI
        private fun bindDataAsync(mediaItem: MediaItem) {
            // Usar un scope ligado al ViewHolder
            CoroutineScope(Dispatchers.Default).launch {
                // Preparar todos los datos en hilo secundario
                val porcentaje = (mediaItem.vote_average?.times(10))?.toInt() ?: 0
                val drawableRes = getProgressDrawableRes(porcentaje)

                val (nombre, fecha) = when (mediaItem) {
                    is Pelicula -> Pair(
                        mediaItem.title,
                        mediaItem.release_date?.let { formatDate(it) }
                    )
                    is TvShow -> Pair(
                        mediaItem.name,
                        mediaItem.first_air_date?.let { formatDate(it) }
                    )
                }

                // Actualizar UI en hilo principal solo una vez con todos los datos
                withContext(Dispatchers.Main) {
                    updateUI(mediaItem, nombre, fecha, porcentaje, drawableRes)
                }
            }
        }

        // Metodos privados para manejo de datos y UI
        private fun updateUI(
            mediaItem: MediaItem,
            nombre: String?,
            fecha: String?,
            porcentaje: Int,
            drawableRes: Int
        ) {
            // Cargar imagen (Picasso maneja esto de manera asíncrona internamente)
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
                .fit()
                .centerInside()
                .placeholder(R.drawable.media_carga)
                .error(R.drawable.media_carga)
                .transform(RoundedCornersTransformation(16, 0))
                .into(b.ivPortada)

            // Actualizar textos
            b.tvNombre.text = nombre
            b.tvFechaEstreno.text = fecha
            b.tvNota.text = "$porcentaje%"

            // Configurar progress bar
            b.pbNota.progress = porcentaje
            b.pbNota.secondaryProgress = porcentaje

            val drawable = ContextCompat.getDrawable(itemView.context, drawableRes)
            b.pbNota.progressDrawable = drawable
            b.pbNota.progressDrawable?.level = porcentaje * 100
        }

        // Metodos privados para manejo de datos y UI
        private fun formatDate(fechaOriginal: String): String {
            return try {
                val fecha = LocalDate.parse(fechaOriginal, dateFormatter)
                val dia = fecha.dayOfMonth
                val mes = Meses.fromNumero(fecha.monthValue)
                val anio = fecha.year

                if (mes != null) {
                    "$dia ${mes.abreviatura.lowercase()} $anio"
                } else {
                    fechaOriginal // Fallback a fecha original si hay problema
                }
            } catch (e: Exception) {
                fechaOriginal // Fallback en caso de error
            }
        }
    }
}