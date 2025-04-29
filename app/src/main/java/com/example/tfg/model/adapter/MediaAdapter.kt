package com.example.tfg.model.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.databinding.ItemMediaBinding
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.ApiResponse
import com.example.tfg.model.dataclass.MediaItem
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.model.enums.Meses
import com.example.tfg.utils.porcentaje
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MediaAdapter(private val onItemClick: (MediaItem) -> Unit) : androidx.recyclerview.widget.ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(MediaItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    companion object {
        fun fetchMedia(context: Context, mediaAdapter: MediaAdapter, apiCall: suspend (APImedia) -> ApiResponse) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val api = getRetrofit().create(APImedia::class.java)
                    val response = apiCall(api)

                    withContext(Dispatchers.Main) {
                        if (response.results.isEmpty()) {
                            Toast.makeText(context, "No hay contenido disponible.", Toast.LENGTH_SHORT).show()
                        } else {
                            mediaAdapter.submitList(response.results)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        println("Error: ${e.message}")
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

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

        fun onClickListener(context: Context, mediaItem: MediaItem) {
            when (mediaItem) {
                is Pelicula -> {
                    Toast.makeText(context, "Seleccionaste: ${mediaItem.title}", Toast.LENGTH_SHORT).show()
                }
                is TvShow -> {
                    Toast.makeText(context, "Seleccionaste: ${mediaItem.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val b = ItemMediaBinding.bind(itemView)

        fun bind(mediaItem: MediaItem, onItemClick: (MediaItem) -> Unit) {
            porcentaje(mediaItem, b, itemView)
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}")
                .resize(150, 225)
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

    class MediaItemDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }
}
