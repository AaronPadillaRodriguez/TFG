package com.example.tfg.view

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.model.adapter.TemporadasAdapter.Companion.fetchPeople
import com.example.tfg.model.adapter.TemporadasAdapter.Companion.onClickListener
import com.example.tfg.model.adapter.TemporadasAdapter
import com.example.tfg.model.dataclass.Seasons
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.dpToPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Clase que maneja la visualizacion de temporadas en la pantalla de detalles.
 * Se encarga de cargar y mostrar la lista de temporadas de una serie usando un RecyclerView.
 */
class DetalleTemporadas(private val context: Context,
                        private val b: ActivityDetallesBinding,
                        private val temporadas: List<Seasons>,
                        private val onLoadedCallback: (() -> Unit)? = null) {

    private lateinit var temporadaAdapter: TemporadasAdapter
    private var isCleanedUp = false

    // Jobs para controlar las corrutinas
    private var initJob: Job? = null
    private var loadDataJob: Job? = null

    init {
        initializeComponent()
    }

    /**
     * Inicializa el componente y comienza la carga de datos.
     */
    private fun initializeComponent() {
        // Verificar que el contexto sea un LifecycleOwner
        if (context !is LifecycleOwner) {
            // Log o manejo del error sin usar return
            println("Error en DetallePeople: El contexto debe ser un LifecycleOwner")
            // Marcar como limpiado para evitar operaciones futuras
            isCleanedUp = true
            return
        }

        initJob = context.lifecycleScope.launch {
            try {
                if (isCleanedUp) return@launch

                // Inicializar el RecyclerView con un adaptador vacío primero
                initRecyclerView()

                if (isCleanedUp) return@launch

                // Luego cargar los datos
                cargarTemporadas()

                if (isCleanedUp) return@launch

                // Configuraciones de UI que no dependen de los datos
                setupUI()

                onLoadedCallback?.invoke()
            } catch (e: Exception) {
                if (!isCleanedUp) {
                    println("Error en DetallePeople: Error durante la inicialización")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Configura el RecyclerView para mostrar las temporadas.
     * Crea el adaptador y establece el layout manager horizontal.
     */
    private fun initRecyclerView() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Crear el adaptador vacío primero (sin datos)
            temporadaAdapter = TemporadasAdapter { item ->
                if (!isCleanedUp && isContextValid()) {
                    onClickListener(context, item)
                }
            }

            // Configurar el RecyclerView en el hilo principal
            // Como ya estamos en una corrutina del lifecycleScope, podemos acceder directamente a la UI
            if (!isCleanedUp && isContextValid()) {
                val recyclerView = b.DetalleTemporadas.rvTemporadas
                recyclerView.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                recyclerView.adapter = temporadaAdapter
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetallePeople: Error inicializando RecyclerView")
                e.printStackTrace()
            }
        }
    }

    /**
     * Carga los datos de las temporadas en el adaptador.
     */
    private fun cargarTemporadas() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Ya estamos en el contexto de lifecycleScope, no necesitamos lanzar otra corrutina
            if (!isCleanedUp && isContextValid()) {

                if (!isCleanedUp && isContextValid()) {
                    fetchPeople(context, temporadaAdapter, temporadas)
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
     * Aplica los estilos y colores a la interfaz.
     * Usa los colores del ColorManager para mantener consistencia visual.
     */
    private fun setupUI() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Ya estamos en el contexto correcto, podemos modificar la UI directamente
            if (!isCleanedUp && isContextValid()) {
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK

                b.DetalleTemporadas.fondoTemporadas.setBackgroundColor(ColorManager.averageColor)
                b.DetalleTemporadas.main.setBackgroundColor(ColorManager.darkerColor)
                b.DetalleTemporadas.tvTemporadas.setTextColor(textColor)

                val roundedDrawable = GradientDrawable().apply {
                    setColor(ColorManager.averageColor)
                    cornerRadius = 12.dpToPx(context).toFloat()
                }
                b.DetalleTemporadas.fondoTemporadas.background = roundedDrawable
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                println("Error en DetallePeople: Error configurando UI")
                e.printStackTrace()
            }
        }
    }

    /**
     * Verifica si el contexto sigue siendo valido.
     * @return true si el contexto es valido, false en caso contrario.
     */
    private fun isContextValid(): Boolean {
        return when (context) {
            is DetallesActivity -> !context.isDestroyed && !context.isFinishing
            else -> !isCleanedUp
        }
    }

    /**
     * Libera recursos y cancela operaciones pendientes.
     * Debe llamarse cuando la clase ya no se use.
     */
    fun cleanup() {
        isCleanedUp = true

        // Cancelar todas las corrutinas
        initJob?.cancel()
        loadDataJob?.cancel()

        // Limpiar el adaptador si existe
        if (::temporadaAdapter.isInitialized) {
            temporadaAdapter.cleanup()
        }

        // Resetear jobs
        initJob = null
        loadDataJob = null
    }
}