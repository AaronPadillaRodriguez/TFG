package com.example.tfg.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.R
import com.example.tfg.databinding.ActivityMainBinding
import com.example.tfg.model.adapter.MediaAdapter
import com.example.tfg.model.adapter.MediaAdapter.Companion.fetchMedia
import com.example.tfg.model.adapter.MediaAdapter.Companion.onClickListener
import com.example.tfg.model.enums.OpcionesSpinner
import com.example.tfg.utils.ViewUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase que gestiona la visualizacion y funcionalidad del contenido multimedia popular.
 *
 * Esta clase se encarga de:
 * - Mostrar contenido multimedia popular mediante un RecyclerView horizontal
 * - Permitir filtrar el contenido por diferentes categorias (En cines, En retransmision, etc.)
 * - Gestionar la interfaz de usuario del spinner de seleccion
 * - Realizar llamadas a la API para obtener datos segun la categoria seleccionada
 *
 * La clase implementa el patron de inicializacion diferida y utiliza corrutinas
 * para operaciones asincronas, garantizando una experiencia de usuario fluida.
 *
 * @property context El contexto de la aplicacion necesario para operaciones de UI
 * @property b El binding de la actividad principal que contiene las vistas
 *
 * @constructor Inicializa automaticamente los componentes UI y carga los datos iniciales
 * utilizando corrutinas para operaciones asincronas.
 */
class Popular(private val context: Context, private val b: ActivityMainBinding) {

    /**
     * Adaptador para el RecyclerView que gestiona la presentacion de elementos multimedia populares.
     *
     * Se inicializa de forma diferida (lateinit) ya que su configuracion requiere
     * que otros componentes esten listos primero.
     */
    private lateinit var mediaAdapter: MediaAdapter

    /**
     * Categoria actualmente seleccionada para filtrar el contenido multimedia.
     *
     * Almacena la opcion del enum OpcionesSpinner que determina que tipo de contenido
     * se muestra en el RecyclerView. Por defecto se establece en "RETRANSMISION"
     * para mostrar contenido en streaming al iniciar la aplicacion.
     */
    private var currentCategory = OpcionesSpinner.RETRANSMISION.opcion

    /**
     * Bloque de inicializacion que configura todos los componentes de la interfaz.
     *
     * Utiliza un CoroutineScope con Dispatchers.IO para realizar las operaciones
     * de configuracion en un hilo secundario, evitando bloquear el hilo principal
     * de la UI durante la inicializacion.
     *
     * Orden de inicializacion:
     * 1. Configuracion del Spinner de filtros
     * 2. Configuracion del RecyclerView
     * 3. Carga inicial del contenido
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarContenido()
        }
    }

    /**
     * Configura el Spinner con las opciones de filtrado disponibles.
     *
     * Este metodo:
     * - Crea un ArrayAdapter personalizado para el Spinner
     * - Aplica estilos personalizados (color negro y negrita) a los elementos
     * - Establece un fondo redondeado con color personalizado
     * - Configura el listener para detectar cambios de seleccion
     *
     * El Spinner permite al usuario filtrar entre diferentes categorias de contenido
     * como peliculas en cines, en retransmision, en alquiler, etc.
     */
    private fun initSpinner() {
        // Crear adaptador personalizado para controlar la apariencia del Spinner
        val adapter = object : ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.Popular) // Array de opciones desde resources
        ) {
            /**
             * Personaliza la vista del elemento seleccionado en el Spinner.
             * Aplica color negro y negrita al texto para mejorar la legibilidad.
             */
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK) // Color negro para contraste
                textView.typeface = Typeface.DEFAULT_BOLD // Negrita para destacar
                return view
            }

            /**
             * Personaliza la vista de los elementos en el dropdown del Spinner.
             * Mantiene consistencia visual con el elemento seleccionado.
             */
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK) // Consistencia de color
                textView.typeface = Typeface.DEFAULT_BOLD // Consistencia de estilo
                return view
            }
        }

        // Crear fondo redondeado personalizado para el Spinner
        val background = ViewUtils.createRoundedBackground(
            context = context,
            cornerRadiusDp = 8f, // Esquinas ligeramente redondeadas
            color = "#b0aa6b" // Color dorado/verde personalizado
        )

        // Aplicar el fondo tanto al Spinner como a su popup
        b.Popular.spPopular.background = background
        b.Popular.spPopular.setPopupBackgroundDrawable(background)

        // Configurar el layout del dropdown
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Popular.spPopular.adapter = adapter

        // Configurar listener para cambios de seleccion
        b.Popular.spPopular.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Maneja la seleccion de un nuevo elemento en el spinner.
             *
             * Cuando el usuario selecciona una nueva categoria:
             * 1. Mapea la posicion seleccionada a la categoria correspondiente
             * 2. Verifica si la categoria ha cambiado realmente
             * 3. Actualiza la categoria actual y recarga el contenido
             *
             * @param parent El AdapterView donde se realizo la seleccion
             * @param view La vista del elemento seleccionado
             * @param position La posicion del elemento en el adaptador (0-3)
             * @param id El ID de fila del elemento seleccionado
             */
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Mapear posicion del spinner a categoria correspondiente
                val newCategory = when (position) {
                    0 -> OpcionesSpinner.RETRANSMISION.opcion    // Contenido en streaming
                    1 -> OpcionesSpinner.EN_TELEVISION.opcion    // Contenido en TV
                    2 -> OpcionesSpinner.EN_ALQUILER.opcion      // Contenido de alquiler
                    3 -> OpcionesSpinner.EN_CINES.opcion         // Contenido en cines
                    else -> currentCategory // Mantener categoria actual si posicion invalida
                }

                // Solo recargar contenido si la categoria realmente cambio
                if(currentCategory != newCategory) {
                    currentCategory = newCategory
                    cargarContenido() // Llamada para actualizar el contenido mostrado
                }

                // Forzar que el spinner mantenga su fondo personalizado
                val background = ViewUtils.createRoundedBackground(
                    context = context,
                    cornerRadiusDp = 8f,
                    color = "#b0aa6b"
                )
                b.Popular.spPopular.background = background
            }

            /**
             * Maneja el caso cuando no hay ninguna seleccion en el spinner.
             *
             * Este metodo permanece vacio ya que el Spinner siempre tendra
             * una opcion seleccionada por defecto.
             *
             * @param parent El AdapterView que no contiene seleccion
             */
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Metodo vacio - siempre habra una seleccion por defecto
            }
        }
    }

    /**
     * Inicializa y configura el RecyclerView para mostrar el contenido multimedia.
     *
     * Este metodo:
     * - Aplica un fondo redondeado personalizado al RecyclerView
     * - Configura el MediaAdapter con su callback de click
     * - Establece un LinearLayoutManager horizontal para scroll lateral
     * - Posiciona el scroll al inicio
     *
     * El RecyclerView utiliza scroll horizontal para permitir al usuario
     * navegar facilmente entre multiples elementos multimedia.
     */
    private fun initRecyclerView() {
        val recyclerView = b.Popular.rvPopular

        // Crear fondo redondeado para el RecyclerView
        val background = ViewUtils.createRoundedBackground(
            context = context,
            cornerRadiusDp = 16f, // Esquinas mas redondeadas que el Spinner
            color = "#b0aa6b" // Mismo color que el Spinner para consistencia
        )

        recyclerView.background = background

        // Inicializar adaptador con callback para clicks en elementos
        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item) // Manejar click en elemento multimedia
        }

        // Configurar layout horizontal para scroll lateral
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
        recyclerView.smoothScrollToPosition(0) // Posicionar al inicio
    }

    /**
     * Carga el contenido multimedia segun la categoria actualmente seleccionada.
     *
     * Este metodo determina que llamada a la API realizar basandose en la categoria
     * actual y utiliza el MediaAdapter para gestionar la obtencion y presentacion
     * de los datos.
     *
     * Categorias disponibles:
     * - EN_CINES: Peliculas actualmente en cartelera
     * - RETRANSMISION: Contenido disponible en streaming
     * - EN_ALQUILER: Contenido disponible para alquilar
     * - EN_TELEVISION: Contenido transmitido en television
     *
     * Cada llamada incluye un callback que reposiciona el RecyclerView al inicio
     * una vez que los datos han sido cargados exitosamente.
     */
    private fun cargarContenido() {
        // Callback para reposicionar el RecyclerView al inicio tras cargar datos
        val callback = { b.Popular.rvPopular.smoothScrollToPosition(0) }

        // Determinar que metodo de la API llamar segun la categoria seleccionada
        when (currentCategory) {
            OpcionesSpinner.EN_CINES.opcion -> {
                // Cargar peliculas actualmente en cartelera de cines
                fetchMedia(mediaAdapter, { api -> api.getPeliculasEnCine() }, true, callback)
            }
            OpcionesSpinner.RETRANSMISION.opcion -> {
                // Cargar contenido disponible en plataformas de streaming
                fetchMedia(mediaAdapter, { api -> api.getEnRetransmision() }, true, callback)
            }
            OpcionesSpinner.EN_ALQUILER.opcion -> {
                // Cargar contenido disponible para alquiler digital
                fetchMedia(mediaAdapter, { api -> api.getPeliculasEnAlquiler() }, true, callback)
            }
            OpcionesSpinner.EN_TELEVISION.opcion -> {
                // Cargar contenido transmitido en canales de television
                fetchMedia(mediaAdapter, { api -> api.getEnTelevision() }, true, callback)
            }
        }
    }
}