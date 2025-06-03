package com.example.tfg.model.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.example.tfg.utils.expandImage
import com.example.tfg.utils.formatDate
import com.example.tfg.utils.getProgressDrawableRes
import com.example.tfg.utils.resetImage
import com.example.tfg.view.DetallesActivity
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Adaptador personalizado para mostrar una lista de elementos multimedia (peliculas y series de TV)
 * en un RecyclerView. Implementa el patron ViewHolder y utiliza DiffUtil para optimizar las
 * actualizaciones de la lista.
 *
 * Esta clase hereda de ListAdapter que proporciona funcionalidades avanzadas como:
 * - Comparacion automatica de elementos para actualizaciones eficientes
 * - Animaciones suaves al actualizar la lista
 * - Manejo optimizado de memoria
 *
 * Caracteristicas principales:
 * - Carga de imagenes con Picasso y transformaciones de esquinas redondeadas
 * - Efectos visuales de presion en los elementos
 * - Navegacion a pantalla de detalles mediante intents
 * - Procesamiento asincrono de datos con corrutinas
 * - Integracion con API de TMDB (The Movie Database)
 *
 * @param onItemClick Funcion callback que se ejecuta cuando el usuario hace clic en un elemento.
 *                    Recibe como parametro el MediaItem seleccionado.
 *
 * @see ListAdapter
 * @see MediaItem
 * @see MediaViewHolder
 */
class MediaAdapter(private val onItemClick: (MediaItem) -> Unit) :
    ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    /**
     * Implementacion de DiffUtil.ItemCallback para comparar elementos MediaItem de manera eficiente.
     *
     * DiffUtil es una utilidad que calcula la diferencia entre dos listas y genera las operaciones
     * de actualizacion minimas necesarias. Esto mejora significativamente el rendimiento del
     * RecyclerView al evitar refrescos innecesarios de elementos que no han cambiado.
     *
     * Esta clase define dos metodos de comparacion:
     * - areItemsTheSame: Compara si dos elementos representan la misma entidad
     * - areContentsTheSame: Compara si el contenido de dos elementos es identico
     */
    class MediaDiffCallback : DiffUtil.ItemCallback<MediaItem>() {

        /**
         * Determina si dos elementos representan la misma entidad comparando sus IDs unicos.
         *
         * Este metodo se utiliza para identificar elementos que pueden haber cambiado de posicion
         * en la lista pero que siguen siendo la misma entidad logica.
         *
         * @param oldItem El elemento de la lista anterior
         * @param newItem El elemento de la lista nueva
         * @return true si ambos elementos representan la misma entidad (mismo ID), false en caso contrario
         */
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Determina si el contenido de dos elementos es completamente identico.
         *
         * Este metodo se llama solo si areItemsTheSame() devuelve true. Se utiliza para
         * determinar si es necesario actualizar la vista del elemento o si puede mantenerse
         * como esta.
         *
         * @param oldItem El elemento de la lista anterior
         * @param newItem El elemento de la lista nueva
         * @return true si el contenido es identico, false si hay diferencias que requieren actualizacion
         */
        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
            return oldItem == newItem
        }
    }

    /**
     * Crea una nueva instancia de MediaViewHolder inflando el layout del elemento.
     *
     * Este metodo es llamado por el RecyclerView cuando necesita crear un nuevo ViewHolder
     * para mostrar un elemento. Se ejecuta unicamente cuando no hay ViewHolders reciclables
     * disponibles.
     *
     * @param parent El ViewGroup padre en el que se insertara la nueva vista (RecyclerView)
     * @param viewType El tipo de vista del nuevo elemento (no utilizado en esta implementacion
     *                 ya que todos los elementos son del mismo tipo)
     * @return Una nueva instancia de MediaViewHolder que contiene la vista inflada del elemento
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        // Inflar el layout del elemento desde el archivo XML
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view, onItemClick)
    }

    /**
     * Vincula los datos del elemento en la posicion especificada al ViewHolder correspondiente.
     *
     * Este metodo es llamado por el RecyclerView para mostrar los datos en la posicion
     * especificada. Se ejecuta tanto para nuevos ViewHolders como para ViewHolders reciclados.
     *
     * El metodo delega la logica de vinculacion al metodo bind() del ViewHolder para mantener
     * la separacion de responsabilidades.
     *
     * @param holder El MediaViewHolder al que se vincularan los datos
     * @param position La posicion del elemento en la lista de datos (0-indexada)
     */
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        // Obtener el elemento en la posicion especificada y vincularlo al ViewHolder
        holder.bind(getItem(position))
    }

    /**
     * Objeto companion que contiene metodos estaticos y configuraciones compartidas.
     *
     * Este companion object encapsula:
     * - La configuracion de Retrofit para comunicacion con la API
     * - Metodos utilitarios para obtener datos de la API
     * - Logica de manejo de clics que requiere navegacion entre actividades
     *
     * Al usar lazy initialization, garantizamos que Retrofit se configure solo una vez
     * y se reutilice en toda la aplicacion, mejorando el rendimiento.
     */
    companion object {

        /**
         * Instancia de Retrofit configurada para comunicarse con la API de TMDB.
         *
         * Utiliza lazy initialization para crear la instancia solo cuando se necesite
         * por primera vez, optimizando el tiempo de inicio de la aplicacion.
         *
         * Configuracion incluida:
         * - URL base de la API de TMDB
         * - Convertidor Gson con adaptador personalizado para MediaItem
         */
        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.themoviedb.org/3/") // URL base de la API de TMDB
                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            // Registrar adaptador personalizado para deserializar MediaItem
                            .registerTypeAdapter(MediaItem::class.java, MediaItemTypeAdapter())
                            .create()
                    )
                )
                .build()
        }

        /**
         * Instancia de la interfaz APImedia creada por Retrofit.
         *
         * Esta instancia permite realizar llamadas a los endpoints definidos en APImedia
         * de manera tipada y segura. Tambien utiliza lazy initialization para optimizar
         * el rendimiento.
         */
        private val api: APImedia by lazy {
            retrofit.create(APImedia::class.java)
        }

        /**
         * Obtiene datos multimedia de la API de TMDB y actualiza el adaptador con los resultados.
         *
         * Este metodo maneja toda la logica asincrona de red y actualizacion de UI:
         * - Ejecuta la llamada a la API en un hilo de E/S
         * - Procesa los resultados (incluyendo mezcla opcional)
         * - Actualiza la UI en el hilo principal
         * - Maneja errores de red de forma robusta
         *
         * Utiliza SupervisorJob para que los errores en una operacion no cancelen
         * otras operaciones concurrentes.
         *
         * @param mediaAdapter El adaptador que se actualizara con los nuevos datos
         * @param apiCall Funcion suspendida que define que endpoint de la API llamar.
         *                Debe devolver un ApiResponse con la lista de elementos multimedia
         * @param shuffle Si es true, los resultados se mezclan aleatoriamente antes de mostrarlos.
         *                util para mostrar contenido variado en listas recomendadas
         * @param onDataLoaded Callback opcional que se ejecuta cuando los datos se han cargado
         *                     exitosamente. util para ocultar indicadores de carga
         */
        fun fetchMedia(
            mediaAdapter: MediaAdapter,
            apiCall: suspend (APImedia) -> ApiResponse,
            shuffle: Boolean = false,
            onDataLoaded: (() -> Unit)? = null
        ) {
            // Usar SupervisorJob para manejar errores sin afectar otras corrutinas
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    // Ejecutar llamada a la API en hilo de E/S para no bloquear la UI
                    val response = apiCall(api)

                    // Procesar los datos tambien en hilo secundario para mejor rendimiento
                    val processedResults = if (response.results.isNullOrEmpty()) {
                        emptyList<MediaItem>() // Lista vacia si no hay resultados
                    } else {
                        // Aplicar mezcla si se solicita, util para contenido aleatorio
                        if (shuffle) response.results.shuffled() else response.results
                    }

                    // Cambiar al hilo principal solo para actualizar la UI
                    withContext(Dispatchers.Main) {
                        if (processedResults.isEmpty()) {
                            println("No hay contenido disponible.") // Log para debugging
                        } else {
                            // Actualizar la lista del adaptador (activa animaciones automaticas)
                            mediaAdapter.submitList(processedResults)
                            // Notificar que los datos se han cargado exitosamente
                            onDataLoaded?.invoke()
                        }
                    }
                } catch (e: Exception) {
                    // Manejar errores en el hilo principal para poder mostrar mensajes al usuario
                    withContext(Dispatchers.Main) {
                        println("Error: ${e.message}") // En produccion, usar sistema de logging apropiado
                    }
                }
            }
        }

        /**
         * Maneja el clic en un elemento multimedia iniciando la actividad de detalles.
         *
         * Este metodo:
         * - Determina el tipo de media (pelicula o serie) del elemento seleccionado
         * - Extrae el titulo apropiado segun el tipo de contenido
         * - Crea un Intent con los datos necesarios para la pantalla de detalles
         * - Inicia la nueva actividad
         *
         * Utiliza corrutinas para procesar los datos en un hilo secundario y solo
         * actualizar la UI (iniciar la actividad) en el hilo principal.
         *
         * @param context El contexto de la aplicacion necesario para crear el Intent
         *                y iniciar la nueva actividad
         * @param mediaItem El elemento multimedia seleccionado que contiene la informacion
         *                  a mostrar en la pantalla de detalles
         */
        fun onClickListener(context: Context, mediaItem: MediaItem) {
            // Procesar datos en hilo secundario para no bloquear la UI
            CoroutineScope(Dispatchers.Default).launch {
                // Crear Intent para navegar a la pantalla de detalles
                val intent = Intent(context, DetallesActivity::class.java)

                // Determinar el tipo de media basado en la instancia del objeto
                val mediaType = when (mediaItem) {
                    is Pelicula -> mediaItem.media_type ?: "movie" // Valor por defecto si es null
                    is TvShow -> mediaItem.media_type ?: "tv"
                }

                // Extraer el titulo apropiado segun el tipo de contenido
                val title = when (mediaItem) {
                    is Pelicula -> mediaItem.title // Las peliculas usan 'title'
                    is TvShow -> mediaItem.name    // Las series usan 'name'
                }

                // Agregar datos al Intent para que esten disponibles en DetallesActivity
                intent.putExtra("ID", mediaItem.id)     // ID unico del elemento
                intent.putExtra("TYPE", mediaType)      // Tipo para determinar endpoints de API

                // Cambiar al hilo principal solo para operaciones de UI
                withContext(Dispatchers.Main) {
                    context.startActivity(intent) // Iniciar la nueva actividad
                    // Log util para debugging y seguimiento de interacciones
                    println("Seleccionaste: $title y su id es ${mediaItem.id}")
                }
            }
        }
    }

    /**
     * ViewHolder personalizado que representa un elemento multimedia individual en el RecyclerView.
     *
     * Esta clase es responsable de:
     * - Mantener las referencias a las vistas del elemento
     * - Vincular los datos del MediaItem a las vistas correspondientes
     * - Manejar las interacciones del usuario (toques, clics)
     * - Aplicar efectos visuales como animaciones de presion
     * - Cargar imagenes de forma asincrona
     *
     * El ViewHolder implementa el patron de reutilizacion de vistas del RecyclerView,
     * donde las vistas se reciclan para mostrar diferentes datos conforme el usuario
     * hace scroll, optimizando el uso de memoria.
     *
     * @param itemView La vista raiz del elemento, inflada desde item_media.xml
     * @param onItemClick Funcion callback que se ejecuta cuando el usuario hace clic en el elemento
     */
    class MediaViewHolder(
        itemView: View,
        private val onItemClick: (MediaItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // Binding generado automaticamente para acceder a las vistas del layout
        private val b = ItemMediaBinding.bind(itemView)

        // Flags para controlar el estado de las animaciones y eventos tactiles
        private var isTouchCancelled = false // Indica si el toque fue cancelado
        private var isAnimating = false      // Indica si hay una animacion en curso

        /**
         * Vincula los datos del elemento multimedia a las vistas del ViewHolder.
         *
         * Este metodo es el punto de entrada principal para configurar la vista con
         * nuevos datos. Se ejecuta cada vez que el ViewHolder se reutiliza para
         * mostrar un elemento diferente.
         *
         * Responsabilidades del metodo:
         * - Resetear el estado visual del elemento
         * - Configurar los listeners de eventos tactiles
         * - Iniciar el proceso asincrono de carga de datos
         *
         * @param mediaItem El elemento multimedia que se mostrara en esta vista
         */
        fun bind(mediaItem: MediaItem) {
            // Resetear el estado del elemento para nueva vinculacion
            isTouchCancelled = false
            isAnimating = false
            b.ivPortada.scaleX = 1f // Resetear escala horizontal de la imagen
            b.ivPortada.scaleY = 1f // Resetear escala vertical de la imagen

            // Configurar el listener de eventos tactiles para efectos visuales interactivos
            itemView.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Programar la animacion de expansion con un pequeño retraso
                        // Esto evita animaciones innecesarias en toques muy rapidos
                        v.postDelayed({
                            if (!isTouchCancelled) {
                                expandImage(b.ivPortada, b.clNota) // Expandir imagen y calificacion
                            }
                        }, 50) // Retraso de 50ms para mejorar la respuesta tactil
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        resetImage(b.ivPortada) // Restaurar imagen a tamaño normal
                        if (!isTouchCancelled) {
                            v.performClick() // Disparar evento de clic estandar
                            onItemClick(mediaItem) // Ejecutar callback personalizado
                        }
                        true
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        // Resetear completamente el estado si el toque es cancelado
                        isTouchCancelled = false
                        isAnimating = false
                        b.ivPortada.scaleX = 1f
                        b.ivPortada.scaleY = 1f
                        true
                    }

                    else -> false // No manejar otros tipos de eventos
                }
            }

            // Iniciar el proceso asincrono de carga y vinculacion de datos
            bindDataAsync(mediaItem)
        }

        /**
         * Procesa los datos del elemento multimedia de forma asincrona para no bloquear la UI.
         *
         * Este metodo realiza todas las operaciones de procesamiento de datos en un hilo
         * secundario y luego actualiza la UI en el hilo principal. Esto incluye:
         * - Calculo del porcentaje de calificacion
         * - Determinacion del drawable apropiado para la barra de progreso
         * - Extraccion y formateo de titulos y fechas
         *
         * El procesamiento asincrono es especialmente importante cuando se manejan
         * grandes cantidades de elementos o operaciones costosas como formateo de fechas.
         *
         * @param mediaItem El elemento multimedia cuyos datos se procesaran
         */
        private fun bindDataAsync(mediaItem: MediaItem) {
            // Usar corrutina para procesar datos en hilo secundario
            CoroutineScope(Dispatchers.Default).launch {
                // Calcular porcentaje de calificacion (vote_average esta en escala 0-10)
                val porcentaje = (mediaItem.vote_average?.times(10))?.toInt() ?: 0

                // Obtener el drawable apropiado basado en el porcentaje de calificacion
                val drawableRes = getProgressDrawableRes(porcentaje)

                // Extraer nombre y fecha segun el tipo de contenido multimedia
                val (nombre, fecha) = when (mediaItem) {
                    is Pelicula -> Pair(
                        mediaItem.title, // Las peliculas usan el campo 'title'
                        mediaItem.release_date?.let { formatDate(it) } // Fecha de estreno
                    )
                    is TvShow -> Pair(
                        mediaItem.name, // Las series usan el campo 'name'
                        mediaItem.first_air_date?.let { formatDate(it) } // Fecha de primera emision
                    )
                }

                // Cambiar al hilo principal para actualizar la UI
                withContext(Dispatchers.Main) {
                    updateUI(mediaItem, nombre, fecha, porcentaje, drawableRes)
                }
            }
        }

        /**
         * Actualiza todos los elementos de la interfaz de usuario con los datos procesados.
         *
         * Este metodo debe ejecutarse en el hilo principal ya que modifica elementos
         * de la UI. Se encarga de:
         * - Cargar la imagen del poster usando Picasso
         * - Actualizar los textos de titulo y fecha
         * - Configurar la barra de progreso de calificacion
         * - Aplicar el drawable de progreso apropiado
         *
         * @param mediaItem El elemento multimedia original
         * @param nombre El titulo formateado del contenido
         * @param fecha La fecha formateada de estreno/emision
         * @param porcentaje El porcentaje de calificacion (0-100)
         * @param drawableRes ID del recurso drawable para la barra de progreso
         */
        private fun updateUI(
            mediaItem: MediaItem,
            nombre: String?,
            fecha: String?,
            porcentaje: Int,
            drawableRes: Int
        ) {
            // Cargar imagen del poster usando Picasso con configuracion optimizada
            Picasso.get()
                .load("https://image.tmdb.org/t/p/w500${mediaItem.poster_path}") // URL completa de la imagen
                .fit() // Ajustar imagen al tamaño de la vista
                .centerInside() // Centrar imagen manteniendo proporciones
                .placeholder(R.drawable.media_carga) // Imagen mientras carga
                .error(R.drawable.media_carga) // Imagen si falla la carga
                .transform(RoundedCornersTransformation(16, 0)) // Esquinas redondeadas
                .into(b.ivPortada) // Vista destino

            // Actualizar elementos de texto con los datos procesados
            b.tvNombre.text = nombre // Titulo de la pelicula/serie
            b.tvFechaEstreno.text = fecha // Fecha de estreno/primera emision
            b.tvNota.text = "$porcentaje%" // Porcentaje de calificacion

            // Configurar la barra de progreso circular
            b.pbBarra.progress = porcentaje // Progreso principal
            b.pbBarra.secondaryProgress = porcentaje // Progreso secundario (para efectos visuales)

            // Aplicar el drawable personalizado basado en la calificacion
            val drawable = ContextCompat.getDrawable(itemView.context, drawableRes)
            b.pbBarra.progressDrawable = drawable
            // El level se multiplica por 100 para el rango correcto del drawable
            b.pbBarra.progressDrawable?.level = porcentaje * 100
        }
    }
}