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
 * Actividad principal para mostrar los detalles completos de una pelicula o serie de TV.
 *
 * Esta actividad gestiona la carga secuencial y asincrona de tres componentes principales:
 * - DetalleSuperior: Informacion basica y colores de la UI
 * - DetallePeople: Reparto y equipo tecnico
 * - DetalleTemporadas: Temporadas y episodios (solo para series)
 *
 * Caracteristicas principales:
 * - Carga secuencial con DetalleSuperior cargandose primero para establecer los colores
 * - Sistema de callbacks para coordinar la carga de componentes
 * - Pantalla de carga con animaciones fluidas
 * - Manejo robusto de errores y estados de la actividad
 * - Diferenciacion automatica entre peliculas y series
 *
 * @property b Binding de la actividad para acceso a las vistas
 * @property handler Handler principal para operaciones en el hilo UI
 * @property detallePeople Componente que maneja la informacion del reparto
 * @property detalleSuperior Componente que maneja la informacion principal y colores
 * @property detalleTemporadas Componente que maneja las temporadas (solo series)
 * @property isLoadingComplete Flag que indica si la carga ha terminado completamente
 * @property isActivityDestroyed Flag para prevenir operaciones despues de onDestroy()
 * @property tipo Tipo de contenido: "movie" para peliculas, "tv" para series
 */
class DetallesActivity : AppCompatActivity(), OnDetailsLoadedListener {

    /** Binding para acceso directo a las vistas del layout */
    private lateinit var b: ActivityDetallesBinding

    /** Handler para ejecutar operaciones en el hilo principal */
    private val handler = Handler(Looper.getMainLooper())

    /** Componente encargado de mostrar informacion del reparto y equipo */
    private var detallePeople: DetallePeople? = null

    /** Componente encargado de mostrar informacion principal y configurar colores */
    private var detalleSuperior: DetalleSuperior? = null

    /** Componente encargado de mostrar temporadas y episodios (solo series) */
    private var detalleTemporadas: DetalleTemporadas? = null

    /** Indica si todos los componentes han terminado de cargar */
    private var isLoadingComplete = false

    /** Previene operaciones despues de que la actividad sea destruida */
    private var isActivityDestroyed = false

    /** Tipo de contenido: "movie" o "tv" */
    private lateinit var tipo: String

    // Control de carga de componentes - Flags para tracking individual
    /** Indica si DetalleSuperior ha terminado de cargar */
    private var isDetalleSuperiorLoaded = false

    /** Indica si DetallePeople ha terminado de cargar */
    private var isDetallePeopleLoaded = false

    /** Indica si DetalleTemporadas ha terminado de cargar */
    private var isDetalleTemporadasLoaded = false

    /** Job para controlar la corrutina de carga final */
    private var loadingJob: Job? = null

    companion object {
        /** Tiempo de espera antes de ocultar la pantalla de carga */
        private const val LOADING_DELAY = 300L

        /** Duracion de la animacion de desaparicion del loading */
        private const val FADE_OUT_DURATION = 250L

        /** Duracion de la animacion de aparicion del contenido */
        private const val FADE_IN_DURATION = 400L

        /** Retraso antes de iniciar la animacion de aparicion */
        private const val FADE_IN_DELAY = 100L
    }

    /**
     * Metodo principal del ciclo de vida que inicializa la actividad.
     *
     * Proceso de inicializacion:
     * 1. Configura el binding y la UI basica
     * 2. Extrae parametros del Intent (ID y tipo)
     * 3. Valida los datos recibidos
     * 4. Configura la pantalla de carga
     * 5. Inicia DetalleSuperior (primer componente)
     *
     * @param savedInstanceState Bundle con el estado guardado de la actividad
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificacion temprana para prevenir operaciones innecesarias
        if (isActivityDestroyed) return

        // Configuracion inicial de la UI
        b = ActivityDetallesBinding.inflate(layoutInflater)
        enableEdgeToEdge() // Habilita el diseño edge-to-edge
        setContentView(b.root)

        // Ocultar el titulo de la ActionBar para diseño personalizado
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configurar padding para barras del sistema (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Extraccion de parametros del Intent
        val id = intent.getIntExtra("ID", -1)
        tipo = intent.getStringExtra("TYPE").toString()

        // Para peliculas, DetalleTemporadas no es necesario
        if(tipo == "movie") {
            isDetalleTemporadasLoaded = true // Marcar como "cargado" para que no bloquee
            b.DetalleTemporadas.main.visibility = View.GONE // Ocultar la vista
        }

        // Validacion de datos criticos
        if (id == -1 || tipo.isNullOrEmpty()) {
            showError("Datos invalidos")
            return
        }

        // Configurar la pantalla de carga inicial
        setupLoadingScreen()

        try {
            // DetalleSuperior se carga primero para establecer los colores de la UI
            detalleSuperior = DetalleSuperior(this, b, id, tipo, this)
        } catch (e: Exception) {
            if (!isActivityDestroyed) {
                showError("Error al cargar los detalles: ${e.message}")
            }
        }
    }

    /**
     * Configura la pantalla de carga inicial mostrando un ProgressBar.
     *
     * Esta funcion prepara la UI para el estado de carga:
     * - Oculta el contenido principal
     * - Muestra el container de loading
     * - Activa el ProgressBar
     */
    private fun setupLoadingScreen() {
        if (isActivityDestroyed) return

        // Ocultar completamente el contenido principal
        b.main.visibility = View.INVISIBLE
        b.main.alpha = 0f

        // Mostrar el contenedor de carga
        b.loadingContainer.visibility = View.VISIBLE
        b.loadingContainer.alpha = 1f

        // Activar el indicador de progreso circular
        b.pbCargar.visibility = View.VISIBLE
    }

    /**
     * Callback del interface OnDetailsLoadedListener.
     * Se ejecuta cuando DetalleSuperior completa su carga inicial.
     *
     * Este metodo marca el inicio de la segunda fase de carga, donde se
     * inicializan DetallePeople y DetalleTemporadas en paralelo.
     * DetalleSuperior debe cargarse primero porque establece los colores
     * que utilizaran los demas componentes.
     */
    override fun onDetailsLoaded() {
        if (isLoadingComplete || isActivityDestroyed) return

        // Re-obtener parametros para la siguiente fase
        val id = intent.getIntExtra("ID", -1)
        val tipo = intent.getStringExtra("TYPE")

        try {
            // Marcar la primera fase como completada
            isDetalleSuperiorLoaded = true

            // Iniciar la carga de los componentes restantes
            initializeRemainingComponents(id, tipo!!)

        } catch (e: Exception) {
            if (!isActivityDestroyed) {
                showError("Error al cargar la informacion adicional: ${e.message}")
            }
        }
    }

    /**
     * Inicializa DetallePeople y DetalleTemporadas despues de que DetalleSuperior este listo.
     *
     * Este metodo utiliza una corrutina para dar tiempo a que ColorManager se actualice
     * con los colores obtenidos de DetalleSuperior antes de inicializar los componentes
     * que dependen de esos colores.
     *
     * @param id ID unico del contenido multimedia
     * @param tipo Tipo de contenido ("movie" para peliculas, "tv" para series)
     */
    private fun initializeRemainingComponents(id: Int, tipo: String) {
        try {
            // Usar el scope del ciclo de vida para gestion automatica de corrutinas
            lifecycleScope.launch {
                // Pequeño delay para asegurar que ColorManager este actualizado
                // Esto es crucial para que los componentes tengan acceso a los colores correctos
                delay(100L)

                if (!isActivityDestroyed) {
                    // Inicializar componente de reparto con callback de finalizacion
                    detallePeople = DetallePeople(this@DetallesActivity, b, tipo, id) {
                        onDetallePeopleLoaded()
                    }

                    // Solo para series: inicializar componente de temporadas
                    if(tipo == "tv") {
                        // Pasar la referencia del TvShow obtenido de DetalleSuperior
                        detalleTemporadas = DetalleTemporadas(
                            this@DetallesActivity,
                            id,
                            b,
                            detalleSuperior?.getTvShow() // Datos de la serie ya cargados
                        ) {
                            onDetalleTemporadasLoaded()
                        }
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
     * Callback ejecutado cuando DetallePeople completa su carga.
     *
     * Marca el componente como cargado y verifica si todos los componentes
     * necesarios estan listos para mostrar la interfaz completa.
     */
    private fun onDetallePeopleLoaded() {
        if (isActivityDestroyed) return

        isDetallePeopleLoaded = true
        checkIfAllComponentsLoaded() // Verificar si podemos mostrar el contenido
    }

    /**
     * Callback ejecutado cuando DetalleTemporadas completa su carga.
     *
     * Marca el componente como cargado y verifica si todos los componentes
     * necesarios estan listos para mostrar la interfaz completa.
     */
    private fun onDetalleTemporadasLoaded() {
        if (isActivityDestroyed) return

        isDetalleTemporadasLoaded = true
        checkIfAllComponentsLoaded() // Verificar si podemos mostrar el contenido
    }

    /**
     * Verifica si todos los componentes necesarios han terminado de cargar.
     *
     * Solo cuando todos los componentes requeridos esten listos se procede
     * a ocultar la pantalla de carga y mostrar el contenido con animaciones.
     *
     * La verificacion incluye:
     * - DetalleSuperior (siempre requerido)
     * - DetallePeople (siempre requerido)
     * - DetalleTemporadas (solo para series, se marca como cargado para peliculas)
     */
    private fun checkIfAllComponentsLoaded() {
        if (isActivityDestroyed || isLoadingComplete) return

        // Verificar que todos los componentes necesarios esten cargados
        if (isDetalleSuperiorLoaded && isDetallePeopleLoaded && isDetalleTemporadasLoaded) {
            // Usar lifecycleScope para gestion automatica del Job
            loadingJob = lifecycleScope.launch {
                try {
                    // Delay para asegurar que tod.o este completamente renderizado
                    delay(LOADING_DELAY)

                    // Verificacion final antes de mostrar el contenido
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
     * Oculta la pantalla de carga y muestra el contenido principal con animaciones fluidas.
     *
     * Proceso de transicion:
     * 1. Anima la desaparicion del loading container (fade out)
     * 2. Una vez completada, oculta los elementos de carga
     * 3. Anima la aparicion del contenido principal (fade in)
     *
     * Las animaciones estan sincronizadas para crear una transicion suave.
     */
    private fun hideLoadingAndShowContent() {
        if (isLoadingComplete || isActivityDestroyed || isFinishing) return
        isLoadingComplete = true // Prevenir ejecuciones multiples

        runOnUiThread {
            // Verificacion adicional por seguridad
            if (isActivityDestroyed || isFinishing) return@runOnUiThread

            // Fase 1: Animacion de desaparicion del loading
            b.loadingContainer.animate()
                .alpha(0f)
                .setDuration(FADE_OUT_DURATION)
                .withEndAction {
                    // Callback ejecutado al completar la animacion de fade out
                    if (!isActivityDestroyed && !isFinishing) {
                        // Ocultar completamente los elementos de carga
                        b.loadingContainer.visibility = View.GONE
                        b.pbCargar.visibility = View.GONE
                    }
                }
                .start()

            // Fase 2: Animacion de aparicion del contenido principal
            b.main.visibility = View.VISIBLE
            b.main.animate()
                .alpha(1f)
                .setDuration(FADE_IN_DURATION)
                .setStartDelay(FADE_IN_DELAY) // Delay para sincronizar con fade out
                .start()
        }
    }

    /**
     * Muestra un mensaje de error en consola y cierra la actividad.
     *
     * Este metodo se ejecuta cuando ocurre un error critico que impide
     * la carga correcta de los datos. Limpia la UI de loading y termina
     * la actividad de forma controlada.
     *
     * @param message Mensaje descriptivo del error ocurrido
     */
    private fun showError(message: String) {
        if (isActivityDestroyed) return

        runOnUiThread {
            if (!isActivityDestroyed && !isFinishing) {
                println(message) // Log del error para debugging

                // Limpiar UI de loading en caso de error
                b.loadingContainer.visibility = View.GONE
                b.pbCargar.visibility = View.GONE

                // Cerrar la actividad de forma controlada
                finish()
            }
        }
    }

    /**
     * Metodo del ciclo de vida para limpieza de recursos al destruir la actividad.
     *
     * Realiza una limpieza exhaustiva para prevenir memory leaks:
     * - Cancela operaciones asincronas pendientes
     * - Limpia handlers y callbacks
     * - Invoca metodos de limpieza de componentes
     * - Libera referencias de objetos
     *
     * Es crucial que este metodo se ejecute para evitar problemas de memoria.
     */
    override fun onDestroy() {
        isActivityDestroyed = true // Marcar como destruida inmediatamente

        // Cancelar operaciones asincronas pendientes
        loadingJob?.cancel()
        handler.removeCallbacksAndMessages(null) // Limpiar todos los mensajes pendientes

        // Invocar limpieza especifica de cada componente
        detalleSuperior?.cleanup()
        detallePeople?.cleanup()
        detalleTemporadas?.cleanup()

        // Liberar referencias para permitir garbage collection
        detallePeople = null
        detalleSuperior = null
        detalleTemporadas = null

        super.onDestroy()
    }

    /**
     * Metodo del ciclo de vida ejecutado al pausar la actividad.
     *
     * Cancela las animaciones en curso para evitar inconsistencias visuales
     * cuando la actividad no esta visible. Esto es especialmente importante
     * para las animaciones de transicion entre loading y contenido.
     */
    override fun onPause() {
        super.onPause()
        // Cancelar animaciones activas para evitar problemas al reanudar
        b.loadingContainer.clearAnimation()
        b.main.clearAnimation()
    }
}