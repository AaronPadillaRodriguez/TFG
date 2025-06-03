package com.example.tfg.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.model.adapter.TemporadasAdapter.Companion.fetchTemporada
import com.example.tfg.model.adapter.TemporadasAdapter.Companion.onClickListener
import com.example.tfg.model.adapter.TemporadasAdapter
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.dpToPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Clase responsable de gestionar la visualizacion y comportamiento del componente de temporadas
 * en la pantalla de detalles de una serie de television.
 *
 * Esta clase se encarga de:
 * - Inicializar y configurar un RecyclerView horizontal para mostrar las temporadas
 * - Cargar los datos de las temporadas de forma asincrona usando corrutinas
 * - Aplicar el tema visual consistente usando ColorManager
 * - Manejar el ciclo de vida del componente y la limpieza de recursos
 * - Gestionar los eventos de click en las temporadas
 *
 * La clase implementa un patron de inicializacion segura que verifica la validez del contexto
 * y maneja adecuadamente los estados de limpieza para evitar memory leaks y crashes.
 *
 * @param context El contexto de la aplicacion, debe implementar LifecycleOwner para el manejo de corrutinas
 * @param idShow ID unico de la serie de television
 * @param b Binding de la actividad que contiene las vistas del componente de temporadas
 * @param temporadas Objeto TvShow que contiene la informacion de la serie y sus temporadas
 * @param onLoadedCallback Callback opcional que se ejecuta cuando el componente ha terminado de cargar
 *
 */
class DetalleTemporadas(private val context: Context,
                        private val idShow: Int,
                        private val b: ActivityDetallesBinding,
                        private val temporadas: TvShow?,
                        private val onLoadedCallback: (() -> Unit)? = null) {

    /**
     * Adaptador del RecyclerView que gestiona la lista de temporadas.
     * Se inicializa de forma tardia para asegurar que el contexto es valido.
     */
    private lateinit var temporadaAdapter: TemporadasAdapter

    /**
     * Flag que indica si el componente ha sido limpiado y no debe realizar mas operaciones.
     * Previene operaciones sobre recursos ya liberados.
     */
    private var isCleanedUp = false

    /**
     * Job de corrutina para controlar el proceso de inicializacion.
     * Permite cancelar la inicializacion si es necesario.
     */
    private var initJob: Job? = null

    /**
     * Job de corrutina para controlar la carga de datos.
     * Permite cancelar la carga de datos si es necesario.
     */
    private var loadDataJob: Job? = null

    init {
        initializeComponent()
    }

    /**
     * Inicializa el componente de temporadas de forma asincrona.
     *
     * Este metodo coordina tod.o el proceso de inicializacion:
     * 1. Verifica que el contexto sea valido (LifecycleOwner)
     * 2. Inicializa el RecyclerView con un adaptador vacio
     * 3. Carga los datos de las temporadas
     * 4. Aplica los estilos de UI
     * 5. Ejecuta el callback de finalizacion
     *
     * Utiliza el lifecycleScope del contexto para asegurar que las operaciones
     * se cancelen automaticamente cuando el ciclo de vida termine.
     *
     * @throws Exception Si ocurre algun error durante la inicializacion
     */
    private fun initializeComponent() {
        // Verificar que el contexto sea un LifecycleOwner para poder usar lifecycleScope
        if (context !is LifecycleOwner) {
            println("Error en DetalleTemporadas: El contexto debe ser un LifecycleOwner")
            // Marcar como limpiado para evitar operaciones futuras
            isCleanedUp = true
            return
        }

        // Lanzar la inicializacion en el scope del ciclo de vida
        initJob = context.lifecycleScope.launch {
            try {
                // Verificar si ya se ha limpiado antes de cada operacion
                if (isCleanedUp) return@launch

                // Paso 1: Inicializar el RecyclerView con un adaptador vacio primero
                initRecyclerView()

                if (isCleanedUp) return@launch

                // Paso 2: Cargar los datos de las temporadas
                cargarTemporadas()

                if (isCleanedUp) return@launch

                // Paso 3: Aplicar configuraciones de UI (colores, estilos)
                setupUI()

                // Paso 4: Notificar que la carga ha terminado
                onLoadedCallback?.invoke()
            } catch (e: Exception) {
                if (!isCleanedUp) {
                    println("Error en DetalleTemporadas: Error durante la inicializacion")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Configura e inicializa el RecyclerView para mostrar las temporadas.
     *
     * Crea un RecyclerView horizontal con las siguientes caracteristicas:
     * - Layout horizontal para desplazamiento lateral
     * - Adaptador personalizado para temporadas con manejo de clicks
     * - Configuracion segura con verificacion de estado
     *
     * El adaptador se crea inicialmente vacio y se poblara posteriormente
     * con los datos cargados de forma asincrona.
     *
     * @throws Exception Si hay problemas al configurar el RecyclerView
     */
    private fun initRecyclerView() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Crear el adaptador con el callback de click y referencia al TvShow
            temporadaAdapter = TemporadasAdapter(
                onItemClick = { season ->
                    // Verificar validez antes de manejar el click
                    if (!isCleanedUp && isContextValid()) {
                        // Delegar el manejo del click al companion object del adaptador
                        temporadas?.let { onClickListener(context, season, it.id, temporadas) }
                    }
                },
                tvShow = temporadas!! // El !! es seguro aqui porque se valida en la inicializacion
            )

            // Configurar el RecyclerView - ya estamos en el hilo principal via lifecycleScope
            if (!isCleanedUp && isContextValid()) {
                val recyclerView = b.DetalleTemporadas.rvTemporadas

                // Configurar layout manager horizontal
                recyclerView.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL, // Desplazamiento horizontal
                    false // No invertir el orden
                )

                // Asignar el adaptador al RecyclerView
                recyclerView.adapter = temporadaAdapter
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetalleTemporadas: Error inicializando RecyclerView")
                e.printStackTrace()
            }
        }
    }

    /**
     * Carga los datos de las temporadas en el adaptador del RecyclerView.
     *
     * Utiliza el metodo fetchTemporada del companion object del adaptador
     * para cargar y procesar los datos de las temporadas. La carga se realiza
     * de forma asincrona y pobla el adaptador con la informacion obtenida.
     *
     * Este metodo debe ejecutarse despues de initRecyclerView() para asegurar
     * que el adaptador este inicializado.
     *
     * @throws Exception Si hay problemas durante la carga de datos
     */
    private fun cargarTemporadas() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Ya estamos en el contexto de lifecycleScope, no necesitamos otra corrutina
            if (!isCleanedUp && isContextValid()) {
                // Obtener la lista de temporadas del TvShow, usar lista vacia si es null
                val seasonsList = temporadas?.seasons ?: emptyList()

                // Delegar la carga al metodo companion del adaptador
                fetchTemporada(context, temporadaAdapter, seasonsList)
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetalleTemporadas: Error cargando datos de temporadas")
                e.printStackTrace()
            }
        }
    }

    /**
     * Aplica los estilos visuales y colores al componente de temporadas.
     *
     * Configura:
     * - Colores de fondo usando ColorManager para consistencia visual
     * - Color del texto basado en el tema (claro/oscuro)
     * - Bordes redondeados para el contenedor de temporadas
     * - Aplicacion de tema coherente con el resto de la aplicacion
     *
     * Utiliza ColorManager para obtener colores que complementen la paleta
     * de colores extraida de la imagen principal de la serie.
     *
     * @throws Exception Si hay problemas aplicando los estilos
     */
    private fun setupUI() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Ya estamos en el contexto correcto del hilo principal
            if (!isCleanedUp && isContextValid()) {
                // Determinar color del texto segun el tema
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK

                // Aplicar colores de fondo
                b.DetalleTemporadas.fondoTemporadas.setBackgroundColor(ColorManager.averageColor)
                b.DetalleTemporadas.main.setBackgroundColor(ColorManager.darkerColor)
                b.DetalleTemporadas.tvTemporadas.setTextColor(textColor)

                // Crear drawable con bordes redondeados para el contenedor
                val roundedDrawable = GradientDrawable().apply {
                    setColor(ColorManager.averageColor) // Color base del ColorManager
                    cornerRadius = 12.dpToPx(context).toFloat() // 12dp convertido a pixeles
                }

                // Aplicar el drawable con bordes redondeados
                b.DetalleTemporadas.fondoTemporadas.background = roundedDrawable
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetalleTemporadas: Error configurando UI")
                e.printStackTrace()
            }
        }
    }

    /**
     * Verifica si el contexto proporcionado sigue siendo valido para operaciones.
     *
     * Realiza diferentes validaciones segun el tipo de contexto:
     * - Para DetallesActivity: verifica que no este destruida ni finalizando
     * - Para otros contextos: verifica el estado de limpieza interno
     *
     * Esta validacion es crucial para evitar operaciones sobre contextos
     * invalidos que podrian causar crashes o memory leaks.
     *
     * @return true si el contexto es valido para operaciones, false en caso contrario
     */
    private fun isContextValid(): Boolean {
        return when (context) {
            // Si es una DetallesActivity, verificar su estado de ciclo de vida
            is DetallesActivity -> !context.isDestroyed && !context.isFinishing
            // Para otros tipos de contexto, usar el flag interno
            else -> !isCleanedUp
        }
    }

    /**
     * Libera todos los recursos utilizados por el componente y cancela operaciones pendientes.
     *
     * Este metodo debe llamarse cuando el componente ya no se necesite para:
     * - Cancelar todas las corrutinas activas
     * - Limpiar el adaptador del RecyclerView
     * - Liberar referencias para permitir garbage collection
     * - Marcar el componente como limpiado para prevenir operaciones futuras
     *
     * Es especialmente importante llamar este metodo en el onDestroy() de la Activity
     * o cuando el fragmento se desasocie para evitar memory leaks.
     *
     * El metodo es seguro de llamar multiples veces.
     */
    fun cleanup() {
        // Marcar como limpiado para prevenir nuevas operaciones
        isCleanedUp = true

        // Cancelar todas las corrutinas activas
        initJob?.cancel()
        loadDataJob?.cancel()

        // Limpiar el adaptador si fue inicializado
        if (::temporadaAdapter.isInitialized) {
            temporadaAdapter.cleanup()
        }

        // Resetear las referencias de jobs para permitir garbage collection
        initJob = null
        loadDataJob = null
    }
}