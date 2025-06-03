package com.example.tfg.model.adapter

import android.content.Context
import android.content.Intent
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
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.getProgressDrawableRes
import com.example.tfg.view.DetallesActivity
import com.example.tfg.view.EpisodiosActivity
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.launch

/**
 * Adaptador para RecyclerView que gestiona la visualizacion de temporadas de series de TV.
 *
 * Esta clase se encarga de:
 * - Mostrar una lista de temporadas con informacion detallada (imagen, nombre, fecha, episodios, valoracion)
 * - Gestionar la carga de imagenes mediante Picasso con transformaciones de esquinas redondeadas
 * - Aplicar colores dinamicos basados en el ColorManager de la aplicacion
 * - Manejar la navegacion hacia la vista de episodios cuando se selecciona una temporada
 * - Prevenir memory leaks mediante limpieza de recursos
 *
 * @param onItemClick Funcion callback que se ejecuta cuando el usuario hace clic en una temporada
 * @param tvShow Objeto TvShow que contiene informacion de la serie padre (utilizado como fallback para imagenes)
 *
 * @see RecyclerView.Adapter
 * @see Seasons
 * @see TvShow
 */
class TemporadasAdapter(
    private val onItemClick: (Seasons) -> Unit,
    private var tvShow: TvShow
) : RecyclerView.Adapter<TemporadasAdapter.TemporadasViewHolder>() {

    /** Lista mutable que almacena las temporadas a mostrar */
    private val items = mutableListOf<Seasons>()

    /** Flag que indica si el adaptador ha sido limpiado para prevenir operaciones en contextos invalidos */
    private var isCleanedUp = false

    /**
     * Actualiza la lista completa de temporadas que se mostraran en el RecyclerView.
     *
     * Este metodo reemplaza completamente la lista anterior con una nueva lista de temporadas.
     * Incluye validacion para evitar operaciones si el adaptador ya ha sido limpiado.
     *
     * @param newItems Nueva lista de temporadas a mostrar. Puede estar vacia pero no debe ser null
     *
     * @throws Exception Si ocurre un error durante la actualizacion de la lista
     */
    fun submitList(newItems: List<Seasons>) {
        // Validar que el adaptador no haya sido limpiado
        if (isCleanedUp) return

        try {
            items.clear() // Limpiar lista anterior
            items.addAll(newItems) // Agregar nuevos elementos
            notifyDataSetChanged() // Notificar cambios al RecyclerView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Devuelve el numero total de elementos en la lista.
     *
     * @return Numero de temporadas si el adaptador esta activo, 0 si ha sido limpiado
     */
    override fun getItemCount(): Int = if (isCleanedUp) 0 else items.size

    /**
     * Crea una nueva instancia de ViewHolder inflando el layout correspondiente.
     *
     * @param parent ViewGroup padre donde se inflara la vista
     * @param viewType Tipo de vista (no utilizado en este caso)
     * @return Nueva instancia de TemporadasViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemporadasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_temporada, parent, false)
        return TemporadasViewHolder(view)
    }

    /**
     * Vincula los datos de una temporada especifica con su ViewHolder correspondiente.
     *
     * Incluye validaciones para prevenir errores cuando el adaptador ha sido limpiado
     * o cuando la posicion esta fuera de rango.
     *
     * @param holder ViewHolder que mostrara los datos
     * @param position Posicion del elemento en la lista
     */
    override fun onBindViewHolder(holder: TemporadasViewHolder, position: Int) {
        // Validaciones de seguridad
        if (isCleanedUp || position >= items.size) return

        try {
            holder.bind(items[position], onItemClick, isCleanedUp, tvShow)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Limpia el adaptador liberando recursos y marcandolo como inactivo.
     *
     * Este metodo es crucial para prevenir memory leaks, especialmente importante
     * cuando se usa con Fragments o Activities que pueden ser destruidos.
     */
    fun cleanup() {
        isCleanedUp = true
        items.clear() // Limpiar lista de elementos
        notifyDataSetChanged() // Notificar cambios para refrescar la vista
    }

    /**
     * Objeto companion que contiene metodos estaticos de utilidad para el adaptador.
     *
     * Estos metodos proporcionan funcionalidades compartidas como:
     * - Carga de datos de temporadas
     * - Manejo de eventos de clic
     * - Validacion de contextos
     */
    companion object {

        /**
         * Carga una lista de temporadas en el adaptador de forma asincrona y segura.
         *
         * Este metodo maneja la carga de datos utilizando corrutinas cuando el contexto
         * implementa LifecycleOwner, asegurando que las operaciones se realicen en el
         * contexto adecuado del ciclo de vida.
         *
         * @param context Contexto de la aplicacion, preferiblemente un LifecycleOwner
         * @param temporadasAdapter Instancia del adaptador a actualizar
         * @param response Lista de temporadas obtenida de la API o base de datos
         *
         * @see LifecycleOwner
         * @see kotlinx.coroutines.launch
         */
        fun fetchTemporada(context: Context, temporadasAdapter: TemporadasAdapter, response: List<Seasons>) {
            // Verificar validez del contexto y estado del adaptador
            if (!isContextValid(context) || temporadasAdapter.isCleanedUp) return

            try {
                // Si el contexto implementa LifecycleOwner, usar corrutinas
                if (context is LifecycleOwner) {
                    context.lifecycleScope.launch {
                        // Doble validacion dentro de la corrutina
                        if (!isContextValid(context) || temporadasAdapter.isCleanedUp) return@launch

                        try {
                            if (response.isNullOrEmpty()) {
                                // Log cuando no hay contenido disponible
                                if (isContextValid(context)) {
                                    println("No hay contenido disponible.")
                                }
                            } else {
                                // Actualizar adaptador con los datos recibidos
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
         * Maneja el evento de clic en una temporada, navegando a la actividad de episodios.
         *
         * Este metodo crea un Intent con todos los datos necesarios para mostrar los episodios
         * de la temporada seleccionada, incluyendo informacion de respaldo para imagenes.
         *
         * @param context Contexto desde el cual se iniciara la nueva actividad
         * @param temporada Objeto Seasons de la temporada seleccionada
         * @param idShow ID unico de la serie de TV
         * @param tvShow Objeto TvShow con informacion adicional de la serie
         *
         * @see EpisodiosActivity
         * @see Intent
         */
        fun onClickListener(context: Context, temporada: Seasons, idShow: Int, tvShow: TvShow) {
            if (!isContextValid(context)) return

            try {
                val intent = Intent(context, EpisodiosActivity::class.java).apply {
                    // Datos principales de la temporada
                    putExtra("SERIE", idShow)
                    putExtra("TEMPORADA", temporada.season_number)
                    putExtra("TEMP_NAME", temporada.name)
                    putExtra("TEMP_RESUMEN", temporada.overview)

                    // URLs de imagenes (principal y de respaldo)
                    putExtra("PORTADA", temporada.poster_path)
                    putExtra("PORTADAEXTRA", tvShow.poster_path) // Imagen de respaldo de la serie
                    putExtra("PORTADAEPISODIO", tvShow.backdrop_path) // Imagen de fondo para episodios
                }

                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Verifica si un contexto sigue siendo valido para operaciones de UI.
         *
         * Esta validacion es especialmente importante para Activities que pueden
         * haber sido destruidas o estar en proceso de finalizacion.
         *
         * @param context Contexto a validar
         * @return true si el contexto es valido para operaciones de UI, false en caso contrario
         *
         * @see DetallesActivity
         */
        private fun isContextValid(context: Context): Boolean {
            return when (context) {
                is DetallesActivity -> !context.isDestroyed && !context.isFinishing
                else -> true // Para otros tipos de contexto, asumir que son validos
            }
        }
    }

    /**
     * ViewHolder que representa cada elemento individual de temporada en el RecyclerView.
     *
     * Esta clase interna maneja:
     * - La vinculacion de datos con las vistas mediante View Binding
     * - La carga de imagenes con Picasso y manejo de errores
     * - La aplicacion de colores dinamicos basados en el ColorManager
     * - La configuracion de barras de progreso para mostrar valoraciones
     * - La limpieza de recursos de Picasso para prevenir memory leaks
     *
     * @param itemView Vista raiz del elemento de lista
     *
     * @see RecyclerView.ViewHolder
     * @see ItemTemporadaBinding
     */
    class TemporadasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /** Binding para acceder a las vistas del layout de forma segura */
        private val b = ItemTemporadaBinding.bind(itemView)

        /** Target de Picasso para poder cancelar requests si es necesario */
        private var picassoTarget: com.squareup.picasso.Target? = null

        /**
         * Vincula los datos de una temporada con las vistas del ViewHolder.
         *
         * Este metodo configura:
         * - Colores de fondo y texto basados en ColorManager
         * - Carga de imagen principal con fallback a la imagen de la serie
         * - Textos informativos (nombre, fecha, episodios, sinopsis)
         * - Barra de progreso para mostrar la valoracion
         * - Listener de clic para navegacion
         *
         * @param temporada Datos de la temporada a mostrar
         * @param onItemClick Funcion callback para el evento de clic
         * @param isCleanedUp Flag que indica si el adaptador ha sido limpiado
         * @param tv Objeto TvShow usado como fallback para datos faltantes
         *
         * @see ColorManager
         * @see Picasso
         * @see RoundedCornersTransformation
         */
        fun bind(temporada: Seasons, onItemClick: (Seasons) -> Unit, isCleanedUp: Boolean, tv: TvShow) {
            if (isCleanedUp) return

            try {
                // Limpiar cualquier request anterior de Picasso
                cleanupPicassoTarget()

                // Aplicar colores de tema dinamicos
                b.main.setBackgroundColor(ColorManager.averageColor)
                b.fondoMain.setCardBackgroundColor(ColorManager.averageColor)

                // Cargar imagen de la temporada con transformacion de esquinas redondeadas
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/w500${temporada.poster_path}")
                    .placeholder(R.drawable.media_carga) // Imagen mientras carga
                    .transform(RoundedCornersTransformation(16, 0)) // Esquinas redondeadas
                    .into(b.ivPortada, object : Callback {
                        override fun onSuccess() {
                            // Imagen cargada exitosamente
                        }

                        override fun onError(e: Exception?) {
                            // Si falla, intentar cargar imagen de la serie como fallback
                            Picasso.get()
                                .load("https://image.tmdb.org/t/p/w500${tv.poster_path}")
                                .error(R.drawable.media_carga) // Imagen por defecto si todo falla
                                .placeholder(R.drawable.media_carga)
                                .transform(RoundedCornersTransformation(16, 0))
                                .into(b.ivPortada)
                        }
                    })

                // Configurar colores de texto segun el tema (claro/oscuro)
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK
                b.tvNombre.setTextColor(textColor)
                b.tvFechaEpisodios.setTextColor(textColor)
                b.tvOverview.setTextColor(textColor)

                // Configurar barra de progreso para mostrar valoracion
                val porcentaje = (temporada.vote_average?.times(10))?.toInt() ?: 0 // Convertir de 0-10 a 0-100
                val drawable = ContextCompat.getDrawable(itemView.context, getProgressDrawableRes(porcentaje))

                b.pbNota.progress = porcentaje
                b.pbNota.progressDrawable = drawable
                b.pbNota.progressDrawable?.level = porcentaje * 100 // Para animaciones
                b.pbNota.secondaryProgress = porcentaje

                // Configurar textos informativos
                b.tvNombre.text = temporada.name ?: "Nombre no disponible"
                b.tvFechaEpisodios.text = "${temporada.air_date ?: "Desconocida"} • ${temporada.episode_count} episodios"
                b.tvOverview.text = temporada.overview?.takeIf { it.isNotEmpty() }
                    ?: "No tenemos una sinopsis en español e ingles." // Texto por defecto
                b.tvNota.text = "$porcentaje%" // Mostrar porcentaje de valoracion

                // Configurar listener de clic con validacion de seguridad
                itemView.setOnClickListener {
                    if (!isCleanedUp) {
                        try {
                            onItemClick(temporada) // Ejecutar callback
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
         * Limpia de forma segura cualquier request activo de Picasso.
         *
         * Este metodo es importante para prevenir memory leaks y crashes
         * cuando el ViewHolder es reciclado o la vista es destruida.
         *
         * @see Picasso.cancelRequest
         */
        private fun cleanupPicassoTarget() {
            picassoTarget?.let {
                try {
                    Picasso.get().cancelRequest(it) // Cancelar request activo
                } catch (e: Exception) {
                    // Error silencioso - no es critico si falla la cancelacion
                }
                picassoTarget = null // Limpiar referencia
            }
        }
    }
}