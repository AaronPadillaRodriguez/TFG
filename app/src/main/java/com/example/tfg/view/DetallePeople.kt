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
 * Clase que maneja la visualizacion y carga de datos de actores y actrices (People)
 * en la pantalla de detalles.
 *
 * @property context Contexto de la aplicacion, debe ser un LifecycleOwner.
 * @property b Binding de la actividad de detalles.
 * @property type Tipo de media ("movie" o "tv").
 * @property id ID de la media.
 * @property onLoadedCallback Callback opcional que se ejecuta al terminar la carga.
 */
class DetallePeople(private val context: Context,
                    private val b: ActivityDetallesBinding,
                    private val type: String,
                    private val id: Int,
                    private val onLoadedCallback: (() -> Unit)? = null) {


    private lateinit var peopleAdapter: PeopleAdapter
    private var isCleanedUp = false

    private var initJob: Job? = null
    private var loadDataJob: Job? = null

    private val api: APImedia = getRetrofit().create(APImedia::class.java)

    init {
        initializeComponent()
    }

    /**
     * Inicializa el componente y carga los datos.
     */
    private fun initializeComponent() {
        // Verificar que el contexto sea un LifecycleOwner
        if (context !is LifecycleOwner) {
            println("Error en DetallePeople: El contexto debe ser un LifecycleOwner")
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
                cargarPeople()

                if (isCleanedUp) return@launch

                // Configuraciones de UI que no dependen de los datos
                setupUI()

                onLoadedCallback?.invoke()
            } catch (e: Exception) {
                if (!isCleanedUp) {
                    handleError("Error durante la inicialización", e)
                }
            }
        }
    }

    /**
     * Configura el RecyclerView para mostrar la lista de actores.
     */
    private fun initRecyclerView() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Crear el adaptador vacío primero (sin datos)
            peopleAdapter = PeopleAdapter { item ->
                if (!isCleanedUp && isContextValid()) {
                    onClickListener(context, item)
                }
            }

            // Configurar el RecyclerView en el hilo principal
            // Como ya estamos en una corrutina del lifecycleScope, podemos acceder directamente a la UI
            if (!isCleanedUp && isContextValid()) {
                val recyclerView = b.DetallePeople.rvPeople
                recyclerView.layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                recyclerView.adapter = peopleAdapter
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                handleError("Error inicializando RecyclerView", e)
            }
        }
    }

    /**
     * Carga los datos de actores segun el tipo de media (pelicula o serie).
     */
    private suspend fun cargarPeople() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Ya estamos en el contexto de lifecycleScope, no necesitamos lanzar otra corrutina
            if (!isCleanedUp && isContextValid()) {
                when (type.lowercase()) {
                    "movie" -> {
                        val moviePeople = getMoviePeopleConFallback(id, api)
                        if (!isCleanedUp && isContextValid()) {
                            fetchPeople(context, peopleAdapter, moviePeople)
                        }
                    }
                    "tv" -> {
                        val tvPeople = getTvPeopleConFallback(id, api)
                        if (!isCleanedUp && isContextValid()) {
                            fetchPeople(context, peopleAdapter, tvPeople)
                        }
                    }
                    else -> {
                        handleError("Tipo de media no válido: $type", null)
                    }
                }
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                handleError("Error cargando datos de people", e)
            }
        }
    }

    /**
     * Aplica estilos y colores a la interfaz.
     */
    private fun setupUI() {
        if (isCleanedUp || !isContextValid()) return

        try {
            // Ya estamos en el contexto correcto, podemos modificar la UI directamente
            if (!isCleanedUp && isContextValid()) {
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK

                b.DetallePeople.tvReparto.setTextColor(textColor)
                b.DetallePeople.main.setBackgroundColor(ColorManager.darkerColor)

                val roundedDrawable = GradientDrawable().apply {
                    setColor(ColorManager.averageColor)
                    cornerRadius = 12.dpToPx(context).toFloat()
                }
                b.DetallePeople.fondoReparto.background = roundedDrawable
            }
        } catch (e: Exception) {
            if (!isCleanedUp) {
                handleError("Error configurando UI", e)
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
     * Maneja errores durante la ejecucion.
     * @param message Mensaje de error.
     * @param exception Excepcion opcional relacionada.
     */
    private fun handleError(message: String, exception: Exception?) {
        println("Error en DetallePeople: $message")
        exception?.printStackTrace()
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
        if (::peopleAdapter.isInitialized) {
            peopleAdapter.cleanup()
        }

        // Resetear jobs
        initJob = null
        loadDataJob = null
    }
}