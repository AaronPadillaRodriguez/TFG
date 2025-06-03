package com.example.tfg.model.adapter

import androidx.core.content.ContextCompat
import com.example.tfg.utils.formatDate
import com.example.tfg.utils.getProgressDrawableRes
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.databinding.ItemEpisodioBinding
import com.example.tfg.model.dataclass.Episodio
import com.example.tfg.utils.ColorManager
import com.example.tfg.view.EpisodiosActivity
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.launch

/**
 * Adaptador personalizado para mostrar una lista de episodios en un RecyclerView.
 *
 * Esta clase gestiona la presentacion de episodios de series/peliculas, incluyendo:
 * - Carga de imagenes mediante Picasso con transformaciones de esquinas redondeadas
 * - Manejo de colores dinamicos basados en ColorManager
 * - Expansion/colapso de descripciones largas
 * - Gestion del ciclo de vida para evitar memory leaks
 * - Barras de progreso para mostrar calificaciones
 *
 * @param onItemClick Funcion lambda que se ejecuta cuando el usuario hace clic en un episodio.
 *                    Recibe el episodio seleccionado como parametro.
 * @param ivPortadaAux URL de imagen auxiliar utilizada como fallback cuando la imagen
 *                     principal del episodio no se puede cargar.
 */
class EpisodiosAdapter(
    private val onItemClick: (Episodio) -> Unit,
    private val ivPortadaAux: String
) : RecyclerView.Adapter<EpisodiosAdapter.EpisodioViewHolder>() {

    /** Lista mutable que contiene los episodios a mostrar en el RecyclerView */
    private val items = mutableListOf<Episodio>()

    /** Flag que indica si el adaptador ha sido limpiado para evitar operaciones en objetos destruidos */
    private var isCleanedUp = false

    /**
     * Actualiza la lista completa de episodios que se muestran en el RecyclerView.
     *
     * Este metodo reemplaza completamente los datos existentes con la nueva lista proporcionada.
     * Utiliza notifyDataSetChanged() para actualizar toda la vista, lo cual es apropiado
     * cuando se cargan datos por primera vez o se hace un refresh completo.
     *
     * @param newItems Nueva lista de episodios a mostrar. Puede estar vacia.
     *
     * @see notifyDataSetChanged
     */
    fun submitList(newItems: List<Episodio>) {
        // Verificar si el adaptador fue limpiado antes de proceder
        if (isCleanedUp) return

        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged() // Notificar cambios a toda la lista
    }

    /**
     * Retorna el numero total de elementos en la lista.
     *
     * @return Numero de episodios si el adaptador esta activo, 0 si fue limpiado.
     */
    override fun getItemCount(): Int = if (isCleanedUp) 0 else items.size

    /**
     * Crea una nueva instancia de ViewHolder para mostrar un elemento episodio.
     *
     * Este metodo infla el layout item_episodio.xml y crea el ViewHolder correspondiente.
     * Se llama automaticamente por el RecyclerView cuando necesita crear nuevas vistas.
     *
     * @param parent ViewGroup padre al cual se añadira la nueva vista.
     * @param viewType Tipo de vista (no utilizado en este adaptador).
     * @return Nueva instancia de EpisodioViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodioViewHolder {
        // Inflar el layout del item usando LayoutInflater
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episodio, parent, false)
        return EpisodioViewHolder(view)
    }

    /**
     * Vincula los datos de un episodio especifico con un ViewHolder.
     *
     * Este metodo se llama para cada elemento visible en el RecyclerView y es responsable
     * de mostrar los datos del episodio en la posicion especificada.
     *
     * @param holder ViewHolder que contiene las vistas a actualizar.
     * @param position Posicion del elemento en la lista (indice basado en 0).
     */
    override fun onBindViewHolder(holder: EpisodioViewHolder, position: Int) {
        // Verificaciones de seguridad antes de proceder
        if (isCleanedUp || position >= items.size) return

        try {
            // Delegar la vinculacion al ViewHolder
            holder.bind(items[position], onItemClick, isCleanedUp, ivPortadaAux)
        } catch (e: Exception) {
            e.printStackTrace() // Log del error para debugging
        }
    }

    /**
     * Limpia el adaptador y libera todos los recursos asociados.
     *
     * Este metodo debe llamarse cuando la actividad/fragmento se destruye para:
     * - Evitar memory leaks
     * - Cancelar operaciones pendientes
     * - Marcar el adaptador como no utilizable
     *
     * Es especialmente importante para adaptadores que manejan imagenes y corrutinas.
     */
    fun cleanup() {
        isCleanedUp = true
        items.clear()
        notifyDataSetChanged() // Notificar que no hay mas datos
    }

    /**
     * Objeto companion que contiene metodos estaticos para operaciones relacionadas con episodios.
     *
     * Estos metodos son independientes de instancias especificas del adaptador y pueden
     * ser utilizados desde cualquier parte de la aplicacion.
     */
    companion object {

        /**
         * Carga una lista de episodios y actualiza el adaptador proporcionado.
         *
         * Este metodo maneja la carga asincrona de datos de episodios utilizando corrutinas
         * para evitar bloquear el hilo principal de la UI. Incluye validaciones de contexto
         * para prevenir crashes cuando la actividad se destruye durante la operacion.
         *
         * @param context Contexto de la aplicacion, preferiblemente una Activity que implemente LifecycleOwner.
         * @param episodiosAdapter Instancia del adaptador que se actualizara con los nuevos datos.
         * @param response Lista de episodios obtenida desde la API o base de datos local.
         *
         * @throws Exception Si ocurre un error durante la carga de datos.
         *
         * @see LifecycleOwner
         * @see kotlinx.coroutines.launch
         */
        fun fetchEpisodios(context: Context, episodiosAdapter: EpisodiosAdapter, response: List<Episodio>) {
            // Verificar validez del contexto y estado del adaptador
            if (!isContextValid(context) || episodiosAdapter.isCleanedUp) return

            try {
                // Solo proceder si el contexto implementa LifecycleOwner (Activities/Fragments)
                if (context is LifecycleOwner) {
                    // Lanzar corrutina en el scope del ciclo de vida
                    context.lifecycleScope.launch {
                        // Verificacion adicional dentro de la corrutina
                        if (!isContextValid(context) || episodiosAdapter.isCleanedUp) return@launch

                        try {
                            if (response.isNullOrEmpty()) {
                                // Log cuando no hay episodios disponibles
                                if (isContextValid(context)) {
                                    println("No hay episodios disponibles.")
                                }
                            } else {
                                // Actualizar adaptador con los nuevos datos
                                episodiosAdapter.submitList(response)
                            }
                        } catch (e: Exception) {
                            // Manejo de errores durante la actualizacion
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
         * Maneja el evento de clic en un elemento episodio especifico.
         *
         * Metodo de utilidad para procesar la seleccion de episodios. Actualmente
         * solo registra la seleccion, pero puede extenderse para navegar a pantallas
         * de detalle o reproducir contenido.
         *
         * @param context Contexto actual de la aplicacion.
         * @param episodio Objeto episodio que fue seleccionado por el usuario.
         *
         * @see Episodio
         */
        fun onClickListener(context: Context, episodio: Episodio) {
            // Verificar que el contexto sea valido antes de proceder
            if (!isContextValid(context)) return

            try {
                // Log de la seleccion para debugging/tracking
                println("Seleccionaste el episodio: ${episodio.name} (Episodio ${episodio.episode_number})")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Verifica si un contexto dado sigue siendo valido y utilizable.
         *
         * Esta verificacion es crucial para evitar crashes cuando se intenta usar
         * contextos de actividades que han sido destruidas o estan en proceso de destruccion.
         *
         * @param context Contexto a verificar.
         * @return true si el contexto es valido y puede ser utilizado, false en caso contrario.
         *
         * @see EpisodiosActivity.isDestroyed
         * @see EpisodiosActivity.isFinishing
         */
        private fun isContextValid(context: Context): Boolean {
            return when (context) {
                // Verificacion especifica para EpisodiosActivity
                is EpisodiosActivity -> !context.isDestroyed && !context.isFinishing
                // Para otros tipos de contexto, asumir que son validos
                else -> true
            }
        }
    }

    /**
     * ViewHolder interno que representa y maneja la vista de un elemento episodio individual.
     *
     * Esta clase es responsable de:
     * - Vincular datos del episodio con elementos de la UI
     * - Manejar la carga de imagenes con Picasso
     * - Gestionar la expansion/colapso de descripciones
     * - Aplicar temas de color dinamicos
     * - Configurar barras de progreso para calificaciones
     * - Limpiar recursos al reutilizar la vista
     *
     * @param itemView Vista raiz del elemento que sera gestionada por este ViewHolder.
     *
     * @see ItemEpisodioBinding
     * @see Picasso
     */
    inner class EpisodioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /** Binding que proporciona acceso directo a todas las vistas del layout */
        val b = ItemEpisodioBinding.bind(itemView)

        /** Target de Picasso para manejar la carga de imagenes de forma segura */
        private var picassoTarget: com.squareup.picasso.Target? = null

        /**
         * Vincula los datos de un episodio especifico con las vistas del ViewHolder.
         *
         * Este metodo configura todos los elementos visuales del item:
         * - Imagen de portada con fallback
         * - Informacion textual (titulo, fecha, duracion)
         * - Barra de progreso con calificacion
         * - Descripcion expandible/colapsable
         * - Colores dinamicos basados en el tema
         * - Listeners para interaccion del usuario
         *
         * @param episodio Objeto episodio con los datos a mostrar.
         * @param onItemClick Funcion callback para manejar clics en el item completo.
         * @param isCleanedUp Flag que indica si el adaptador fue limpiado.
         * @param ivPortadaAux URL de imagen de respaldo en caso de error de carga.
         */
        fun bind(episodio: Episodio, onItemClick: (Episodio) -> Unit, isCleanedUp: Boolean, ivPortadaAux: String) {
            // Salir inmediatamente si el adaptador fue limpiado
            if (isCleanedUp) return

            try {
                // Limpiar recursos de carga de imagenes anteriores
                cleanupPicassoTarget()

                // Configurar colores de fondo basados en el tema actual
                b.main.setBackgroundColor(ColorManager.averageColor)
                b.fondoMain.setCardBackgroundColor(ColorManager.averageColor)

                // Cargar imagen principal del episodio con Picasso
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/w500${episodio.still_path}") // URL base de TMDB
                    .placeholder(R.drawable.imagen_carga) // Imagen mientras carga
                    .transform(RoundedCornersTransformation(16, 0)) // Esquinas redondeadas
                    .into(b.ivPortada, object : Callback {
                        override fun onSuccess() {
                            // Imagen cargada exitosamente
                        }
                        override fun onError(e: Exception?) {
                            // Cargar imagen de respaldo si falla la principal
                            Picasso.get()
                                .load("https://image.tmdb.org/t/p/w500${ivPortadaAux}")
                                .error(R.drawable.imagen_carga) // Imagen final si todo falla
                                .placeholder(R.drawable.imagen_carga)
                                .transform(RoundedCornersTransformation(16, 0))
                                .into(b.ivPortada)
                        }
                    })

                // Configurar colores de texto segun el tema (claro/oscuro)
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK
                b.tvNumEpisodio.setTextColor(textColor)
                b.tvNombre.setTextColor(textColor)
                b.tvDatos.setTextColor(textColor)
                b.tvOverview.setTextColor(textColor)
                b.tvExpandCollapse.setTextColor(textColor)

                // Configurar barra de progreso para mostrar calificacion
                val porcentaje = (episodio.vote_average?.times(10))?.toInt() ?: 0 // Convertir a porcentaje
                val drawable = ContextCompat.getDrawable(itemView.context, getProgressDrawableRes(porcentaje))
                b.pbNota.progress = porcentaje
                b.pbNota.progressDrawable = drawable
                b.pbNota.progressDrawable?.level = porcentaje * 100 // Para animaciones
                b.pbNota.secondaryProgress = porcentaje

                // Configurar textos informativos del episodio
                b.tvNota.text = "$porcentaje%" // Mostrar porcentaje de calificacion
                b.tvNumEpisodio.text = "${episodio.episode_number}"
                // Usar nombre del episodio o fallback generico
                b.tvNombre.text = episodio.name?.takeIf { it.isNotEmpty() } ?: "Episodio ${episodio.episode_number}"
                // Combinar fecha de emision y duracion
                val datos = "${formatDate(episodio.air_date)} • ${episodio.runtime} min"
                b.tvDatos.text = datos.ifEmpty { "Informacion no disponible" }

                // Configurar descripcion expandible
                b.tvExpandCollapse.visibility = View.GONE
                b.tvExpandCollapse.text = "Ver mas"
                b.tvOverview.text = episodio.overview?.takeIf { it.isNotEmpty() } ?: "Sin descripcion disponible"
                b.tvOverview.maxLines = if (episodio.isExpanded) Int.MAX_VALUE else 6

                /**
                 * Actualiza la visibilidad y texto del boton "Ver mas/menos" segun el estado actual.
                 *
                 * Reglas de visibilidad:
                 * - Siempre visible cuando esta expandido (mostrando "Ver menos")
                 * - Visible cuando no esta expandido Y el texto tiene 6+ lineas Y esta truncado
                 * - Oculto en cualquier otro caso
                 */
                fun updateExpandCollapseVisibility() {
                    if (episodio.isExpanded) {
                        b.tvExpandCollapse.text = "Ver menos"
                        b.tvExpandCollapse.visibility = View.VISIBLE
                    } else {
                        val layout = b.tvOverview.layout ?: return
                        val lines = b.tvOverview.lineCount
                        val isTruncated = layout.getEllipsisCount(lines - 1) > 0

                        b.tvExpandCollapse.visibility = if (lines >= 6 && isTruncated) View.VISIBLE else View.GONE
                    }
                }

                /**
                 * Listener para manejar la expansion/colapso de la descripcion.
                 *
                 * Validaciones importantes:
                 * 1. Solo actua si el layout esta disponible
                 * 2. Solo permite expandir si el texto esta truncado (necesita expansion)
                 * 3. Siempre permite colapsar si esta expandido
                 */
                val clickListener = View.OnClickListener {
                    val layout = b.tvOverview.layout ?: return@OnClickListener
                    val lines = b.tvOverview.lineCount
                    val isTruncated = layout.getEllipsisCount(lines - 1) > 0

                    // Solo procesar clic si:
                    // - Ya esta expandido (puede colapsar)
                    // - O necesita expansion (texto truncado)
                    if (episodio.isExpanded || (lines >= 6 && isTruncated)) {
                        episodio.isExpanded = !episodio.isExpanded
                        b.tvOverview.maxLines = if (episodio.isExpanded) Int.MAX_VALUE else 6

                        // Actualizar visibilidad despues del cambio
                        updateExpandCollapseVisibility()

                        // Forzar nueva medicion del layout
                        b.tvOverview.post {
                            b.tvOverview.requestLayout()
                            // Verificar visibilidad nuevamente despues del re-layout
                            updateExpandCollapseVisibility()
                        }
                    }
                }

                // Asignar listeners tanto al texto como al boton
                b.tvOverview.setOnClickListener(clickListener)
                b.tvExpandCollapse.setOnClickListener(clickListener)

                // Observador inicial para configurar la visibilidad correcta al crear la vista
                b.tvOverview.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        b.tvOverview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        updateExpandCollapseVisibility()
                    }
                })

                // Configurar listener de clic para tod.o el item
                itemView.setOnClickListener {
                    if (!isCleanedUp) {
                        try {
                            onItemClick(episodio) // Ejecutar callback proporcionado
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace() // Log de errores para debugging
            }
        }

        /**
         * Limpia de forma segura el target de Picasso para evitar memory leaks.
         *
         * Este metodo cancela cualquier solicitud de carga de imagen pendiente
         * y libera la referencia al target para permitir la recoleccion de basura.
         * Es especialmente importante cuando las vistas son reutilizadas por el RecyclerView.
         *
         * @see Picasso.cancelRequest
         */
        private fun cleanupPicassoTarget() {
            picassoTarget?.let {
                try {
                    Picasso.get().cancelRequest(it) // Cancelar solicitud pendiente
                } catch (e: Exception) {
                    // Error silencioso - no es critico si falla la cancelacion
                }
                picassoTarget = null // Liberar referencia
            }
        }
    }
}