package com.example.tfg.view

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.utils.OnDetailsLoadedListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Actividad que muestra los detalles de una pelicula o serie.
 * Maneja la carga de tres componentes principales: superior, actores y temporadas.
 * Implementa un sistema de carga con animaciones y manejo de errores.
 */
class DetallesActivity : AppCompatActivity(), OnDetailsLoadedListener {
    private lateinit var b: ActivityDetallesBinding
    private val handler = Handler(Looper.getMainLooper())
    private var detallePeople: DetallePeople? = null
    private var detalleSuperior: DetalleSuperior? = null
    private var detalleTemporadas: DetalleTemporadas? = null
    private var isLoadingComplete = false
    private var isActivityDestroyed = false
    private lateinit var tipo: String

    // Control de carga de componentes
    private var isDetalleSuperiorLoaded = false
    private var isDetallePeopleLoaded = false
    private var isDetalleTemporadasLoaded = false

    // Jobs para controlar las corrutinas
    private var loadingJob: Job? = null

    companion object {
        private const val LOADING_DELAY = 300L
        private const val FADE_OUT_DURATION = 250L
        private const val FADE_IN_DURATION = 400L
        private const val FADE_IN_DELAY = 100L
    }

    /**
     * Configura la UI inicial y comienza la carga de datos.
     * @param savedInstanceState Bundle con el estado guardado.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si la actividad ya fue destruida antes de continuar
        if (isActivityDestroyed) return

        b = ActivityDetallesBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(b.root)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val id = intent.getIntExtra("ID", -1)
        tipo = intent.getStringExtra("TYPE").toString()

        if(tipo == "movie") {
            isDetalleTemporadasLoaded = true
            b.DetalleTemporadas.main.visibility = View.GONE
        }
        // Validar que los datos necesarios estén presentes
        if (id == -1 || tipo.isNullOrEmpty()) {
            showError("Datos inválidos")
            return
        }

        // Inicializar la pantalla de carga
        setupLoadingScreen()

        try {
            // Inicializar DetalleSuperior que cargará los colores primero
            detalleSuperior = DetalleSuperior(this, b, id, tipo, this)
        } catch (e: Exception) {
            if (!isActivityDestroyed) {
                showError("Error al cargar los detalles: ${e.message}")
            }
        }
    }

    /**
     * Configura la pantalla de carga mostrando un ProgressBar.
     */
    private fun setupLoadingScreen() {
        if (isActivityDestroyed) return

        // Asegurar que el contenido principal esté oculto
        b.main.visibility = View.INVISIBLE
        b.main.alpha = 0f

        // Mostrar el container de loading
        b.loadingContainer.visibility = View.VISIBLE
        b.loadingContainer.alpha = 1f

        // Iniciar el ProgressBar
        b.pbCargar.visibility = View.VISIBLE
    }

    /**
     * Callback que se ejecuta cuando DetalleSuperior termina de cargar.
     * Inicia la carga de los componentes restantes.
     */
    override fun onDetailsLoaded() {
        if (isLoadingComplete || isActivityDestroyed) return

        val id = intent.getIntExtra("ID", -1)
        val tipo = intent.getStringExtra("TYPE")

        try {
            // Marcar DetalleSuperior como cargado
            isDetalleSuperiorLoaded = true

            // Inicializar DetallePeople y DetalleTemporadas
            initializeRemainingComponents(id, tipo!!)

        } catch (e: Exception) {
            if (!isActivityDestroyed) {
                showError("Error al cargar la información adicional: ${e.message}")
            }
        }
    }

    /**
     * Inicializa los componentes DetallePeople y DetalleTemporadas.
     * @param id ID de la media.
     * @param tipo Tipo de media ("movie" o "tv").
     */
    private fun initializeRemainingComponents(id: Int, tipo: String) {
        try {
            // Usar lifecycleScope para dar tiempo a que los colores se establezcan
            lifecycleScope.launch {
                // Pequeño delay para asegurar que ColorManager esté actualizado
                delay(100L)

                if (!isActivityDestroyed) {
                    // Inicializar DetallePeople con callback
                    detallePeople = DetallePeople(this@DetallesActivity, b, tipo, id) {
                        onDetallePeopleLoaded()
                    }

                    // Inicializar DetalleTemporadas con callback
                    detalleTemporadas = DetalleTemporadas(this@DetallesActivity, b,detalleSuperior?.getTvShowList() ?: emptyList()) {
                        onDetalleTemporadasLoaded()
                    }
                }
            }

        } catch (e: Exception) {
            if (!isActivityDestroyed) {
                showError("Error al inicializar componentes: ${e.message}")
            }
        }
    }

    /**
     * Callback cuando DetallePeople termina de cargar.
     * Verifica si todos los componentes estan listos.
     */
    private fun onDetallePeopleLoaded() {
        if (isActivityDestroyed) return

        isDetallePeopleLoaded = true
        checkIfAllComponentsLoaded()
    }

    /**
     * Callback cuando DetalleTemporadas termina de cargar.
     * Verifica si todos los componentes estan listos.
     */
    private fun onDetalleTemporadasLoaded() {
        if (isActivityDestroyed) return

        isDetalleTemporadasLoaded = true
        checkIfAllComponentsLoaded()
    }

    /**
     * Verifica si todos los componentes han terminado de cargar.
     * Si es asi, oculta la pantalla de carga.
     */
    private fun checkIfAllComponentsLoaded() {
        if (isActivityDestroyed || isLoadingComplete) return

        // Solo proceder si todos los componentes están cargados
        if (isDetalleSuperiorLoaded && isDetallePeopleLoaded && isDetalleTemporadasLoaded) {
            // Usar lifecycleScope para gestionar la corrutina
            loadingJob = lifecycleScope.launch {
                try {
                    // Pequeño delay para asegurar que tod.o esté cargado antes de mostrar
                    delay(LOADING_DELAY)

                    // Verificar si la actividad sigue activa antes de continuar
                    if (!isActivityDestroyed && !isFinishing) {
                        hideLoadingAndShowContent()
                    }
                } catch (e: Exception) {
                    if (!isActivityDestroyed) {
                        showError("Error durante la carga final: ${e.message}")
                    }
                }
            }
        }
    }

    /**
     * Oculta la pantalla de carga y muestra el contenido con animaciones.
     */
    private fun hideLoadingAndShowContent() {
        if (isLoadingComplete || isActivityDestroyed || isFinishing) return
        isLoadingComplete = true

        runOnUiThread {
            if (isActivityDestroyed || isFinishing) return@runOnUiThread

            // Animar la desaparición del loading
            b.loadingContainer.animate()
                .alpha(0f)
                .setDuration(FADE_OUT_DURATION)
                .withEndAction {
                    if (!isActivityDestroyed && !isFinishing) {
                        b.loadingContainer.visibility = View.GONE
                        b.pbCargar.visibility = View.GONE
                    }
                }
                .start()

            // Animar la aparición del contenido principal
            b.main.visibility = View.VISIBLE
            b.main.animate()
                .alpha(1f)
                .setDuration(FADE_IN_DURATION)
                .setStartDelay(FADE_IN_DELAY) // Pequeño delay para que termine la animación de loading
                .start()
        }
    }

    /**
     * Muestra un mensaje de error y cierra la actividad.
     * @param message Mensaje de error a mostrar.
     */
    private fun showError(message: String) {
        if (isActivityDestroyed) return

        runOnUiThread {
            if (!isActivityDestroyed && !isFinishing) {
                println(message)
                // Ocultar loading en caso de error
                b.loadingContainer.visibility = View.GONE
                b.pbCargar.visibility = View.GONE

                finish()
            }
        }
    }

    /**
     * Realiza limpieza al destruir la actividad.
     * Cancela operaciones pendientes y libera recursos.
     */
    override fun onDestroy() {
        isActivityDestroyed = true

        // Cancelar todas las operaciones pendientes
        loadingJob?.cancel()
        handler.removeCallbacksAndMessages(null)

        // Limpiar referencias
        detalleSuperior?.cleanup()
        detallePeople?.cleanup()
        detalleTemporadas?.cleanup()

        detallePeople = null
        detalleSuperior = null
        detalleTemporadas = null

        super.onDestroy()
    }

    /**
     * Limpieza adicional al pausar la actividad.
     * Cancela animaciones en curso.
     */
    override fun onPause() {
        super.onPause()
        // Cancelar animaciones si la actividad se pausa
        b.loadingContainer.clearAnimation()
        b.main.clearAnimation()
    }
}