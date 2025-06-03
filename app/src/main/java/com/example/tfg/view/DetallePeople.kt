package com.example.tfg.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.model.adapter.PeopleAdapter
import com.example.tfg.model.adapter.PeopleAdapter.Companion.fetchPeople
import com.example.tfg.model.adapter.PeopleAdapter.Companion.onClickListener
import com.example.tfg.model.api.APImedia
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.dpToPx
import com.example.tfg.utils.getMoviePeopleConFallback
import com.example.tfg.utils.getRetrofit
import com.example.tfg.utils.getTvPeopleConFallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Clase responsable de la gestion y visualizacion del reparto (actores/actrices) en la pantalla de detalles
 * de peliculas y series de TV. Maneja la carga asincrona de datos, la configuracion del RecyclerView
 * y la aplicacion de estilos dinamicos basados en el tema de la aplicacion.
 *
 * Esta clase implementa un patron de inicializacion asincrona con manejo de ciclo de vida para evitar
 * memory leaks y crashes por contextos invalidos.
 *
 * @property context Contexto de la aplicacion que debe implementar LifecycleOwner para el manejo
 *                   correcto del ciclo de vida de las corrutinas
 * @property b Binding de la actividad de detalles que proporciona acceso a las vistas de la UI
 * @property type Tipo de contenido multimedia ("movie" para peliculas o "tv" para series)
 * @property id Identificador unico del contenido multimedia en la API de TMDB
 * @property onLoadedCallback Callback opcional que se ejecuta cuando la carga de datos ha finalizado,
 *                           util para coordinar con otros componentes de la UI
 */
class DetallePeople(private val context: Context,
                    private val b: ActivityDetallesBinding,
                    private val type: String,
                    private val id: Int,
                    private val onLoadedCallback: (() -> Unit)? = null) {

    /**
     * Adaptador del RecyclerView que maneja la lista de personas (actores/actrices)
     * Se inicializa de forma lazy para evitar problemas de concurrencia
     */
    private lateinit var peopleAdapter: PeopleAdapter

    /**
     * Flag que indica si los recursos han sido liberados para evitar operaciones
     * en un contexto ya destruido
     */
    private var isCleanedUp = false

    /**
     * Job de la corrutina de inicializacion, permite cancelar la operacion si es necesario
     */
    private var initJob: Job? = null

    /**
     * Job de la corrutina de carga de datos, permite cancelar la operacion si es necesario
     */
    private var loadDataJob: Job? = null

    /**
     * Instancia de la API para realizar peticiones HTTP a los servicios de TMDB
     */
    private val api: APImedia = getRetrofit().create(APImedia::class.java)

    init {
        initializeComponent()
    }

    /**
     * Inicializa el componente de forma asincrona siguiendo un patron de inicializacion segura.
     *
     * El proceso de inicializacion se divide en tres fases:
     * 1. Configuracion del RecyclerView con adaptador vacio
     * 2. Carga asincrona de datos del reparto desde la API
     * 3. Aplicacion de estilos y configuraciones de UI
     *
     * Cada fase incluye verificaciones de estado para evitar operaciones en contextos invalidos.
     *
     * @throws Exception Si el contexto no implementa LifecycleOwner o si ocurre un error durante la inicializacion
     */
    private fun initializeComponent() {
        // Verificar que el contexto sea un LifecycleOwner para el manejo seguro de corrutinas
        if (context !is LifecycleOwner) {
            println("Error en DetallePeople: El contexto debe ser un LifecycleOwner")
            isCleanedUp = true
            return
        }

        // Lanzar la inicializacion en el scope del ciclo de vida del contexto
        initJob = context.lifecycleScope.launch {
            try {
                // Verificacion de estado antes de cada operacion critica
                if (isCleanedUp) return@launch

                // Fase 1: Inicializar el RecyclerView con un adaptador vacio
                initRecyclerView()

                if (isCleanedUp) return@launch

                // Fase 2: Cargar los datos del reparto desde la API
                cargarPeople()

                if (isCleanedUp) return@launch

                // Fase 3: Aplicar configuraciones de UI y estilos
                setupUI()

                // Notificar que la carga ha terminado
                onLoadedCallback?.invoke()
            } catch (e: Exception) {
                if (!isCleanedUp) {
                    println("Error en DetallePeople: Error durante la inicializacion")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Configura el RecyclerView para mostrar la lista horizontal de actores y actrices.
     *
     * Inicializa el adaptador con un callback para manejar los clics en los elementos
     * y configura un LinearLayoutManager horizontal para la disposicion de los elementos.
     *
     * @throws Exception Si ocurre un error durante la configuracion del RecyclerView
     */
    private fun initRecyclerView() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Crear el adaptador con lambda para manejar clics en elementos del reparto
            peopleAdapter = PeopleAdapter { item ->
                // Verificar estado antes de procesar el clic
                if (!isCleanedUp && isContextValid()) {
                    onClickListener(context, item) // Navegar a detalles de la persona
                }
            }

            // Configurar el RecyclerView en el hilo principal
            if (!isCleanedUp && isContextValid()) {
                val recyclerView = b.DetallePeople.rvPeople

                // Configurar layout horizontal para mostrar elementos en fila
                recyclerView.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

                // Asignar el adaptador al RecyclerView
                recyclerView.adapter = peopleAdapter
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetallePeople: Error inicializando RecyclerView")
                e.printStackTrace()
            }
        }
    }

    /**
     * Carga los datos del reparto desde la API segun el tipo de contenido multimedia.
     *
     * Utiliza diferentes endpoints de la API dependiendo de si el contenido es una pelicula
     * o una serie de TV. Los datos se cargan de forma asincrona y se pasan al adaptador
     * una vez obtenidos.
     *
     * @throws Exception Si ocurre un error durante la carga de datos desde la API
     */
    private suspend fun cargarPeople() {
        if (isCleanedUp || !isContextValid()) return

        try {
            if (!isCleanedUp && isContextValid()) {
                when (type.lowercase()) {
                    "movie" -> {
                        // Obtener reparto de pelicula con fallback en caso de error
                        val moviePeople = getMoviePeopleConFallback(id, api)
                        if (!isCleanedUp && isContextValid()) {
                            // Poblar el adaptador con los datos obtenidos
                            fetchPeople(context, peopleAdapter, moviePeople)
                        }
                    }
                    "tv" -> {
                        // Obtener reparto de serie de TV con fallback en caso de error
                        val tvPeople = getTvPeopleConFallback(id, api)
                        if (!isCleanedUp && isContextValid()) {
                            // Poblar el adaptador con los datos obtenidos
                            fetchPeople(context, peopleAdapter, tvPeople)
                        }
                    }
                    else -> {
                        println("Error en DetallePeople: Tipo de media no valido: $type")
                    }
                }
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetallePeople: Error cargando datos de people")
                e.printStackTrace()
            }
        }
    }

    /**
     * Aplica estilos dinamicos y configuraciones de UI basados en el tema actual de la aplicacion.
     *
     * Configura los colores del texto y fondo segun si el tema es oscuro o claro,
     * y aplica un fondo redondeado al contenedor del reparto utilizando un GradientDrawable.
     *
     * @throws Exception Si ocurre un error durante la aplicacion de estilos
     */
    private fun setupUI() {
        if (isCleanedUp || !isContextValid()) return

        try {
            if (!isCleanedUp && isContextValid()) {
                // Determinar color del texto segun el tema (blanco para oscuro, negro para claro)
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK

                // Aplicar colores dinamicos a los elementos de UI
                b.DetallePeople.tvReparto.setTextColor(textColor)
                b.DetallePeople.main.setBackgroundColor(ColorManager.darkerColor)

                // Crear y aplicar fondo redondeado al contenedor del reparto
                val roundedDrawable = GradientDrawable().apply {
                    setColor(ColorManager.averageColor) // Color base del tema
                    cornerRadius = 12.dpToPx(context).toFloat() // Bordes redondeados de 12dp
                }
                b.DetallePeople.fondoReparto.background = roundedDrawable
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetallePeople: Error configurando UI")
                e.printStackTrace()
            }
        }
    }

    /**
     * Verifica si el contexto actual sigue siendo valido para realizar operaciones de UI.
     *
     * Para actividades, verifica que no esten destruidas o en proceso de finalizacion.
     * Para otros tipos de contexto, se basa en el flag de cleanup.
     *
     * @return true si el contexto es valido y se pueden realizar operaciones seguras,
     *         false si el contexto esta invalido y se deben evitar operaciones de UI
     */
    private fun isContextValid(): Boolean {
        return when (context) {
            // Para DetallesActivity, verificar estado especifico de la actividad
            is DetallesActivity -> !context.isDestroyed && !context.isFinishing
            // Para otros contextos, usar el flag general de cleanup
            else -> !isCleanedUp
        }
    }

    /**
     * Libera todos los recursos utilizados por la clase y cancela operaciones pendientes.
     *
     * Este metodo debe llamarse obligatoriamente cuando la clase ya no se vaya a utilizar
     * para evitar memory leaks y crashes por contextos invalidos. Cancela todas las
     * corrutinas activas, limpia el adaptador y resetea las referencias.
     *
     * Es especialmente importante llamar este metodo en el onDestroy() de la actividad
     * o cuando se navegue fuera de la pantalla de detalles.
     */
    fun cleanup() {
        // Marcar como limpio para prevenir nuevas operaciones
        isCleanedUp = true

        // Cancelar todas las corrutinas activas
        initJob?.cancel()
        loadDataJob?.cancel()

        // Limpiar el adaptador si ha sido inicializado
        if (::peopleAdapter.isInitialized) {
            peopleAdapter.cleanup()
        }

        // Resetear referencias de jobs para liberar memoria
        initJob = null
        loadDataJob = null
    }
}