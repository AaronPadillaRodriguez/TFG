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
 * Clase responsable de gestionar la visualizacion y funcionalidad de la seccion de contenido
 * multimedia en tendencia dentro de la aplicacion.
 *
 * Esta clase implementa un sistema de filtrado temporal que permite a los usuarios
 * alternar entre las tendencias del dia actual y las tendencias de la semana completa.
 * Utiliza un RecyclerView horizontal para mostrar los elementos multimedia y un Spinner
 * para la seleccion de categorias temporales.
 *
 * @param context El contexto de la aplicacion necesario para operaciones de UI y recursos.
 * @param b El binding de la actividad principal que contiene las referencias a las vistas.
 *
 * @constructor Inicializa automaticamente los componentes UI y carga los datos iniciales
 * utilizando corrutinas para operaciones asincronas.
 *
 */
class Trending(private val context: Context, private val b: ActivityMainBinding) {

    /**
     * Adaptador personalizado para el RecyclerView que gestiona la visualizacion
     * de elementos multimedia en tendencia. Maneja la presentacion de datos
     * y las interacciones del usuario con cada elemento.
     */
    private lateinit var mediaAdapter: MediaAdapter

    /**
     * Variable que almacena la categoria temporal actualmente seleccionada.
     * Valores posibles:
     * - "day": Para mostrar tendencias del dia actual
     * - "week": Para mostrar tendencias de la semana completa
     *
     * Por defecto se inicializa con las tendencias del dia.
     */
    private var currentCategory = OpcionesSpinner.HOY.texto

    /**
     * Bloque de inicializacion que se ejecuta al crear una instancia de la clase.
     * Configura todos los componentes UI necesarios y carga los datos iniciales
     * de forma asincrona para evitar bloquear el hilo principal.
     *
     * Utiliza el Dispatcher.IO para operaciones de red y configuracion pesada.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()        // Configura el selector de categorias temporales
            initRecyclerView()   // Inicializa la lista horizontal de contenido
            cargarTendencias()   // Realiza la primera carga de datos desde la API
        }
    }

    /**
     * Inicializa y configura el Spinner que permite al usuario seleccionar
     * entre diferentes periodos temporales para filtrar las tendencias.
     *
     * Este metodo:
     * - Crea un ArrayAdapter personalizado con estilo visual especifico
     * - Configura el fondo redondeado del Spinner
     * - Establece el listener para detectar cambios de seleccion
     * - Aplica formato de texto (color negro y negrita) tanto para la vista
     *   principal como para el menu desplegable
     *
     * El Spinner utiliza los valores definidos en el array de recursos 'Tendencias'
     * y mapea las posiciones a valores del enum OpcionesSpinner.
     */
    private fun initSpinner() {
        // Crear adaptador personalizado para el Spinner con formato especifico
        val adapter = object : ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.Tendencias) // Array de opciones desde resources
        ) {
            /**
             * Personaliza la vista principal del Spinner (elemento seleccionado visible).
             * Aplica color negro y negrita al texto para mejor legibilidad.
             */
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK) // Texto negro para contraste
                textView.typeface = Typeface.DEFAULT_BOLD // Aplicar negrita
                return view
            }

            /**
             * Personaliza las vistas del menu desplegable del Spinner.
             * Mantiene consistencia visual con la vista principal.
             */
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK) // Consistencia de color
                textView.typeface = Typeface.DEFAULT_BOLD // Consistencia de formato
                return view
            }
        }

        // Crear fondo redondeado personalizado para el Spinner
        val background = ViewUtils.createRoundedBackground(
            context = context,
            cornerRadiusDp = 8f,        // Bordes redondeados sutiles
            color = "#b0aa6b"           // Color de marca de la aplicacion
        )

        // Aplicar el fondo tanto al Spinner como a su menu desplegable
        b.Tendencias.spTendencias.background = background
        b.Tendencias.spTendencias.setPopupBackgroundDrawable(background)

        // Configurar el layout del menu desplegable
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Tendencias.spTendencias.adapter = adapter

        // Configurar el listener para detectar cambios de seleccion
        b.Tendencias.spTendencias.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Callback ejecutado cuando el usuario selecciona un elemento diferente en el Spinner.
             *
             * Realiza las siguientes operaciones:
             * 1. Mapea la posicion seleccionada a la categoria correspondiente
             * 2. Verifica si hay un cambio real en la seleccion para evitar recargas innecesarias
             * 3. Actualiza la categoria actual y recarga los datos si es necesario
             *
             * @param parent El AdapterView donde se realizo la seleccion
             * @param view La vista del elemento seleccionado
             * @param position Posicion del elemento en el adaptador (0=Hoy, 1=Esta semana)
             * @param id ID unico del elemento seleccionado
             */
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Mapear posicion a categoria de API
                val newCategory = when (position) {
                    0 -> OpcionesSpinner.HOY.texto        // "day" para tendencias diarias
                    1 -> OpcionesSpinner.ESTA_SEMANA.texto // "week" para tendencias semanales
                    else -> currentCategory                 // Mantener categoria actual si posicion invalida
                }

                // Solo recargar si realmente cambio la categoria (optimizacion)
                if(currentCategory != newCategory) {
                    currentCategory = newCategory
                    cargarTendencias() // Cargar nuevos datos desde la API
                }

                // Forzar que el spinner mantenga su fondo personalizado
                val background = ViewUtils.createRoundedBackground(
                    context = context,
                    cornerRadiusDp = 8f,
                    color = "#b0aa6b"
                )
                b.Tendencias.spTendencias.background = background
            }

            /**
             * Callback ejecutado cuando no hay ninguna seleccion en el Spinner.
             *
             * En este caso particular, el metodo permanece vacio ya que el Spinner
             * siempre tendra una opcion seleccionada por defecto y no se permite
             * estados sin seleccion en la UI de tendencias.
             *
             * @param parent El AdapterView que perdio la seleccion
             */
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Metodo intencionalmente vacio - siempre habra una seleccion valida
            }
        }
    }

    /**
     * Configura e inicializa el RecyclerView que muestra el contenido multimedia
     * en formato de lista horizontal desplazable.
     *
     * Este metodo:
     * - Aplica un fondo redondeado personalizado al RecyclerView
     * - Inicializa el MediaAdapter con callback de click
     * - Configura un LinearLayoutManager horizontal
     * - Establece scroll automatico a la primera posicion
     *
     * El RecyclerView se configura especificamente para desplazamiento horizontal
     * para proporcionar una experiencia de navegacion tipo carrusel.
     */
    private fun initRecyclerView() {
        val recyclerView = b.Tendencias.rvTendencias

        // Crear fondo redondeado para el contenedor del RecyclerView
        val background = ViewUtils.createRoundedBackground(
            context = context,
            cornerRadiusDp = 16f,       // Bordes mas redondeados que el Spinner
            color = "#b0aa6b"           // Color consistente con el tema de la app
        )

        recyclerView.background = background

        // Inicializar adaptador con lambda para manejar clicks en elementos
        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)  // Delegar manejo de click al adaptador
        }

        // Configurar layout manager para desplazamiento horizontal
        recyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL,  // Orientacion horizontal tipo carrusel
            false               // No invertir el orden de elementos
        )

        recyclerView.adapter = mediaAdapter

        // Posicionar automaticamente en el primer elemento
        recyclerView.smoothScrollToPosition(0)
    }

    /**
     * Ejecuta la carga de datos de tendencias desde la API externa.
     *
     * Utiliza el metodo fetchMedia del MediaAdapter para realizar una llamada
     * asincrona a la API de TMDb (The Movie Database) y obtener contenido
     * multimedia popular segun la categoria temporal seleccionada.
     *
     * Parametros de la API:
     * - "all": Tipo de contenido (peliculas y series)
     * - currentCategory: Periodo temporal ("day" o "week")
     *
     * Despues de cargar los datos, automaticamente posiciona el RecyclerView
     * en el primer elemento para consistencia visual.
     *
     * @see MediaAdapter.fetchMedia para detalles de implementacion de la llamada API
     */
    private fun cargarTendencias() {
        fetchMedia(
            mediaAdapter,  // Adaptador que recibira y mostrara los datos
            { api -> api.getTrendingAll("all", currentCategory) }  // Lambda que define la llamada API especifica
        ) {
            // Callback ejecutado despues de cargar los datos exitosamente
            b.Tendencias.rvTendencias.smoothScrollToPosition(0)  // Resetear posicion de scroll
        }
    }
}