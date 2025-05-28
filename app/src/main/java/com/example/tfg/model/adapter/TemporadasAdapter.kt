package com.example.tfg.model.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.databinding.ItemTemporadaBinding
import com.example.tfg.model.dataclass.Seasons
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.getProgressDrawableRes
import com.example.tfg.view.DetallesActivity
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.launch

/**
 * Adaptador para mostrar una lista de temporadas de series en un RecyclerView.
 * Utiliza Picasso para cargar imagenes y muestra informacion como nombre, fecha y valoracion.
 *
 * @param onItemClick Funcion que se ejecuta al hacer clic en una temporada.
 */
class TemporadasAdapter (private val onItemClick: (Seasons) -> Unit) : RecyclerView.Adapter<TemporadasAdapter.TemporadasViewHolder>() {

    private val items = mutableListOf<Seasons>()
    private var isCleanedUp = false

    /**
     * Actualiza la lista de temporadas.
     *
     * @param newItems Nueva lista de temporadas a mostrar.
     */
    fun submitList(newItems: List<Seasons>) {
        if (isCleanedUp) return

        try {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = if (isCleanedUp) 0 else items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemporadasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_temporada, parent, false)
        return TemporadasViewHolder(view)
    }

    override fun onBindViewHolder(holder: TemporadasViewHolder, position: Int) {
        if (isCleanedUp || position >= items.size) return

        try {
            holder.bind(items[position], onItemClick, isCleanedUp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Limpia el adaptador y libera recursos.
     */
    fun cleanup() {
        isCleanedUp = true
        items.clear()
        notifyDataSetChanged()
    }

    /**
     * Companion object con metodos estaticos para:
     * - Cargar datos de temporadas
     * - Manejar eventos de clic
     * - Validar contexto
     */
    companion object {
        /**
         * Carga datos de temporadas y actualiza el adaptador.
         *
         * @param context Contexto de la aplicacion.
         * @param temporadasAdapter Adaptador a actualizar.
         * @param response Lista de temporadas a mostrar.
         */
        fun fetchPeople(context: Context, temporadasAdapter: TemporadasAdapter, response: List<Seasons>) {
            // Verificar que el contexto sea válido
            if (!isContextValid(context) || temporadasAdapter.isCleanedUp) return

            try {
                if (context is LifecycleOwner) {
                    context.lifecycleScope.launch {
                        if (!isContextValid(context) || temporadasAdapter.isCleanedUp) return@launch

                        try {
                            if (response.isNullOrEmpty()) {
                                if (isContextValid(context)) {
                                    println("No hay contenido disponible.")
                                }
                            } else {
                                temporadasAdapter.submitList(response)
                            }
                        } catch (e: Exception) {
                            if (isContextValid(context)) {
                                println("Error: ${e.message}")
                            }
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Maneja el clic en una temporada.
         *
         * @param context Contexto.
         * @param temporada Temporada seleccionada.
         */
        fun onClickListener(context: Context, temporada: Seasons) {
            if (!isContextValid(context)) return

            try {
                println("Seleccionaste: ${temporada.name} y su id es ${temporada.id}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Verifica si el contexto sigue siendo válido
        private fun isContextValid(context: Context): Boolean {
            return when (context) {
                is DetallesActivity -> !context.isDestroyed && !context.isFinishing
                else -> true
            }
        }
    }

    /**
     * ViewHolder para mostrar elementos individuales de temporadas.
     */
    class TemporadasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val b = ItemTemporadaBinding.bind(itemView)
        private var picassoTarget: com.squareup.picasso.Target? = null

        /**
         * Enlaza datos de una temporada a las vistas.
         *
         * @param temporada Temporada a mostrar.
         * @param onItemClick Funcion al hacer clic.
         * @param isCleanedUp Flag de limpieza.
         */
        fun bind(temporada: Seasons, onItemClick: (Seasons) -> Unit, isCleanedUp: Boolean) {
            if (isCleanedUp) return

            try {
                // Limpiar el target anterior si existe
                cleanupPicassoTarget()

                // Configurar colores y fondos
                b.main.setBackgroundColor(ColorManager.averageColor)
                b.fondoMain.setCardBackgroundColor(ColorManager.averageColor)

                // Cargar imagen de forma segura
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/w500${temporada.poster_path}")
                    .fit()
                    .centerInside()
                    .placeholder(R.drawable.media_carga)
                    .error(R.drawable.media_carga)
                    .transform(RoundedCornersTransformation(12, 0))
                    .into(b.ivPortada)

                // Configurar colores de texto
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK
                b.tvNombre.setTextColor(textColor)
                b.tvFechaEpisodios.setTextColor(textColor)
                b.tvOverview.setTextColor(textColor)

                val porcentaje = (temporada.vote_average?.times(10))?.toInt() ?: 0
                val drawable = ContextCompat.getDrawable(itemView.context, getProgressDrawableRes(porcentaje))

                b.pbNota.progress = porcentaje
                b.pbNota.progressDrawable = drawable
                b.pbNota.progressDrawable?.level = porcentaje * 100
                b.pbNota.secondaryProgress = porcentaje

                // Configurar textos
                b.tvNombre.text = temporada.name ?: "Nombre no disponible"
                b.tvFechaEpisodios.text = "${temporada.air_date} • ${temporada.episode_count} episodios"
                b.tvOverview.text = temporada.overview?.takeIf { it.isNotEmpty() } ?: "No tenemos una sinopsis en español e ingles."
                b.tvNota.text = "$porcentaje%"

                // Configurar listener de clic de forma segura
                itemView.setOnClickListener {
                    if (!isCleanedUp) {
                        try {
                            onItemClick(temporada)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Limpia el target de Picasso de forma segura
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
    }
}