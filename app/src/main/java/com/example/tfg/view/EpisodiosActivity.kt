package com.example.tfg.view

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.R
import com.example.tfg.databinding.ActivityEpisodiosBinding
import com.example.tfg.model.adapter.EpisodiosAdapter
import com.example.tfg.model.adapter.EpisodiosAdapter.Companion.onClickListener
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.Episodio
import com.example.tfg.utils.ColorManager
import com.example.tfg.utils.getRetrofit
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Activity que muestra la lista de episodios de una temporada especifica de una serie.
 *
 * Esta clase implementa un sistema optimizado de carga de datos que incluye:
 * - Carga asincrona de episodios desde la API de TMDb
 * - Sistema de cache para evitar recargas innecesarias
 * - Precarga de imagenes en segundo plano
 * - Manejo de estados de carga mediante Flow y Channels
 * - Fallback entre idiomas (español e ingles)
 * - Optimizaciones de rendimiento para una experiencia fluida
 *
 * La activity recibe los datos de la temporada a traves del Intent y muestra:
 * - Informacion de la temporada (nombre, descripcion, imagen)
 * - Lista de episodios en un RecyclerView
 * - Pantalla de carga mientras se obtienen los datos
 *
 */
class EpisodiosActivity : AppCompatActivity() {

    /** ViewBinding para acceso eficiente a las vistas del layout */
    private lateinit var b: ActivityEpisodiosBinding

    /** Adaptador del RecyclerView que gestiona la lista de episodios */
    private lateinit var episodiosAdapter: EpisodiosAdapter

    /** Instancia de la API para realizar peticiones a TMDb */
    private val api: APImedia = getRetrofit().create(APImedia::class.java)

    /** Handler para operaciones en el hilo principal */
    private val handler = Handler(Looper.getMainLooper())

    // Pool de hilos para operaciones en background - Optimiza el rendimiento separando tareas pesadas
    /** Dispatcher para operaciones de carga de datos en segundo plano (3 hilos) */
    private val backgroundDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

    /** Dispatcher especializado para carga de imagenes (2 hilos) */
    private val imageLoadingDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    // Cache para evitar recargas innecesarias - Mejora la experiencia del usuario
    /** Cache thread-safe que almacena episodios por clave "serieId_temporadaId" */
    private val episodiosCache = ConcurrentHashMap<String, List<Episodio>>()

    // Canal para comunicacion entre corrutinas - Patron productor-consumidor
    /** Canal para comunicar estados de carga entre corrutinas de forma asincrona */
    private val loadingChannel = Channel<LoadingState>(Channel.UNLIMITED)

    // Datos recibidos del Intent
    /** ID de la serie obtenido del Intent */
    private var idSerie: Int = -1
    /** ID de la temporada obtenido del Intent */
    private var idTemporada: Int = -1
    /** Nombre de la temporada */
    private var tempName: String = ""
    /** Descripcion/resumen de la temporada */
    private var tempOverview: String = ""
    /** URL de la imagen de portada principal */
    private var ivPortada: String = ""
    /** URL alternativa de la imagen de portada */
    private var ivPortadaAux: String = ""
    /** URL de la imagen de portada para episodios */
    private var ivEpisodioPortadaAux: String = ""

    // Flags de control de estado
    /** Flag que indica si la activity ha sido destruida */
    private var isActivityDestroyed = false
    /** Flag que indica si la carga ha completado */
    private var isLoadingComplete = false

    // Jobs para controlar las corrutinas - Permite cancelacion y manejo de ciclo de vida
    /** Job para controlar la animacion de carga */
    private var loadingJob: Job? = null
    /** Job para controlar la carga de datos */
    private var dataLoadingJob: Job? = null
    /** Job para controlar la precarga de imagenes */
    private var imagePreloadingJob: Job? = null
    /** Job para controlar la configuracion de UI */
    private var uiSetupJob: Job? = null

    /**
     * Sealed class que representa los diferentes estados de carga de la aplicacion.
     * Utiliza el patron State para manejar de forma segura los diferentes estados.
     */
    sealed class LoadingState {
        /** Estado de carga inicial */
        object Loading : LoadingState()

        /** Estado cuando los datos han sido cargados exitosamente */
        data class DataLoaded(val episodes: List<Episodio>) : LoadingState()

        /** Estado de error con mensaje descriptivo */
        data class Error(val message: String) : LoadingState()

        /** Estado final cuando tod.o ha terminado */
        object Complete : LoadingState()
    }

    /**
     * Objeto companion que contiene constantes de configuracion para optimizar el rendimiento.
     */
    companion object {
        /** Delay reducido para mayor velocidad de respuesta (ms) */
        private const val LOADING_DELAY = 200L
        /** Duracion de la animacion de desvanecimiento de salida (ms) */
        private const val FADE_OUT_DURATION = 200L
        /** Duracion de la animacion de desvanecimiento de entrada (ms) */
        private const val FADE_IN_DURATION = 300L
        /** Delay antes de iniciar la animacion de entrada (ms) */
        private const val FADE_IN_DELAY = 50L
        /** Maximo numero de peticiones concurrentes para imagenes */
        private const val MAX_CONCURRENT_REQUESTS = 2
    }

    /**
     * Metodo onCreate optimizado que inicializa la activity de forma asincrona.
     *
     * Implementa un patron de inicializacion en paralelo donde las tareas criticas
     * se ejecutan simultaneamente para reducir el tiempo de carga total.
     *
     * @param savedInstanceState Estado guardado de la activity (si existe)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificacion temprana de estado para evitar operaciones innecesarias
        if (isActivityDestroyed) return

        // Configuracion basica de la activity
        b = ActivityEpisodiosBinding.inflate(layoutInflater)
        enableEdgeToEdge() // Habilita diseño edge-to-edge
        setContentView(b.root)

        // Configuracion de window insets para diseño moderno
        ViewCompat.setOnApplyWindowInsetsListener(b.fondoMain) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Validacion de datos del Intent - Si falla, termina la activity
        if (!initializeFromIntent()) return

        // Mostrar pantalla de carga inmediatamente
        setupLoadingScreen()

        // Inicializar componentes y UI en paralelo para mejor rendimiento
        lifecycleScope.launch {
            // Lanzar tareas criticas en paralelo usando async
            val setupJobs = listOf(
                async(Dispatchers.Main.immediate) { initializeComponents() }, // UI en hilo principal
                async(Dispatchers.Default) { preloadUIColors() }, // Colores en hilo por defecto
                async(backgroundDispatcher) { checkCacheAndLoadData() } // Datos en background
            )

            // Esperar solo a las tareas criticas (UI y colores)
            setupJobs.take(2).awaitAll()

            // Configurar UI y monitor solo si la activity sigue activa
            if (!isActivityDestroyed) {
                setupUI()
                startLoadingStateMonitor()
            }
        }
    }

    /**
     * Inicializa los datos desde el Intent de forma segura y eficiente.
     *
     * Extrae todos los datos necesarios del Intent y valida que sean correctos.
     * Si algun dato critico falta o es invalido, muestra un error y retorna false.
     *
     * @return true si la inicializacion fue exitosa, false en caso contrario
     */
    private fun initializeFromIntent(): Boolean {
        return try {
            // Extraccion segura de datos del Intent
            idSerie = intent.getIntExtra("SERIE", -1)
            idTemporada = intent.getIntExtra("TEMPORADA", -1)
            tempName = intent.getStringExtra("TEMP_NAME") ?: ""
            tempOverview = intent.getStringExtra("TEMP_RESUMEN") ?: ""
            ivPortada = intent.getStringExtra("PORTADA") ?: ""
            ivPortadaAux = intent.getStringExtra("PORTADAEXTRA") ?: ""
            ivEpisodioPortadaAux = intent.getStringExtra("PORTADAEPISODIO") ?: ""

            // Validacion de datos criticos
            if (idSerie == -1 || idTemporada == -1) {
                showError("IDs de serie o temporada no validos")
                false
            } else true
        } catch (e: Exception) {
            // Manejo de errores durante la extraccion
            showError("Error inicializando datos: ${e.message}")
            false
        }
    }

    /**
     * Precarga los colores de UI en un hilo de background para evitar bloqueos.
     *
     * Accede a las propiedades del ColorManager en un hilo separado para que
     * esten listas cuando se configure la UI, evitando calculos en el hilo principal.
     */
    private suspend fun preloadUIColors() = withContext(Dispatchers.Default) {
        // Forzar calculo de colores en background thread
        ColorManager.darkerColor // Color mas oscuro para fondos
        ColorManager.averageColor // Color promedio para contenedores
        ColorManager.isDark // Flag para determinar si usar tema oscuro
    }

    /**
     * Verifica si existen datos en cache y decide si cargar datos frescos.
     *
     * Implementa una estrategia de cache simple que mejora significativamente
     * el rendimiento al evitar peticiones de red innecesarias.
     */
    private suspend fun checkCacheAndLoadData() = withContext(backgroundDispatcher) {
        val cacheKey = "${idSerie}_${idTemporada}" // Clave unica para esta temporada
        val cachedEpisodes = episodiosCache[cacheKey]

        if (cachedEpisodes != null && cachedEpisodes.isNotEmpty()) {
            // Usar datos en cache - Respuesta inmediata
            loadingChannel.send(LoadingState.DataLoaded(cachedEpisodes))
        } else {
            // Cargar datos frescos desde la API
            cargarEpisodios()
        }
    }

    /**
     * Monitor de estados de carga usando Flow para una comunicacion reactiva.
     *
     * Utiliza el patron Observer con Flow para reaccionar a los cambios de estado
     * de forma asincrona y thread-safe.
     */
    private fun startLoadingStateMonitor() {
        lifecycleScope.launch {
            loadingChannel.receiveAsFlow() // Convierte el Channel en Flow
                .flowOn(backgroundDispatcher) // Procesa en background
                .collect { state -> // Reacciona a cada estado
                    when (state) {
                        is LoadingState.Loading -> {
                            // Mantener pantalla de carga activa
                        }
                        is LoadingState.DataLoaded -> {
                            handleDataLoaded(state.episodes)
                        }
                        is LoadingState.Error -> {
                            showError(state.message)
                        }
                        is LoadingState.Complete -> {
                            onDataLoaded()
                        }
                    }
                }
        }
    }

    /**
     * Maneja los datos cargados de forma eficiente actualizando el adaptador y precargando imagenes.
     *
     * @param episodes Lista de episodios cargados desde la API o cache
     */
    private suspend fun handleDataLoaded(episodes: List<Episodio>) {
        if (isActivityDestroyed) return

        // Actualizar adaptador en hilo principal para modificar UI
        withContext(Dispatchers.Main.immediate) {
            if (!isActivityDestroyed) {
                // Llenar el adaptador con los episodios obtenidos
                EpisodiosAdapter.fetchEpisodios(this@EpisodiosActivity, episodiosAdapter, episodes)

                // Iniciar precarga de imagenes en background para mejor experiencia
                preloadImages(episodes)

                // Notificar que la carga ha completado
                loadingChannel.send(LoadingState.Complete)
            }
        }
    }

    /**
     * Precarga imagenes de episodios de forma asincrona para mejorar la experiencia de usuario.
     *
     * Divide los episodios en chunks para evitar sobrecargar la red y procesa
     * las imagenes en paralelo con un limite de concurrencia.
     *
     * @param episodes Lista de episodios cuyas imagenes se precargaran
     */
    private fun preloadImages(episodes: List<Episodio>) {
        imagePreloadingJob = lifecycleScope.launch(imageLoadingDispatcher) {
            // Dividir episodios en chunks para procesamiento eficiente
            episodes.chunked(MAX_CONCURRENT_REQUESTS).forEach { chunk ->
                if (isActivityDestroyed) return@launch

                // Procesar cada chunk en paralelo
                val preloadJobs = chunk.map { episode ->
                    async {
                        try {
                            // Precargar solo si hay imagen valida
                            if (!episode.still_path.isNullOrBlank()) {
                                preloadSingleImage("https://image.tmdb.org/t/p/w500${episode.still_path}")
                            }
                        } catch (e: Exception) {
                            // Error silencioso para precarga - no afecta funcionalidad principal
                        }
                    }
                }
                preloadJobs.awaitAll() // Esperar a que termine el chunk
                delay(50) // Pequeña pausa para no saturar la red
            }
        }
    }

    /**
     * Precarga una imagen individual usando Picasso.
     *
     * Descarga la imagen y la almacena en cache sin mostrarla,
     * mejorando la velocidad cuando se necesite mostrar posteriormente.
     *
     * @param url URL de la imagen a precargar
     */
    private suspend fun preloadSingleImage(url: String) = withContext(imageLoadingDispatcher) {
        try {
            Picasso.get()
                .load(url)
                .fetch() // Solo descarga, no muestra - para cache
        } catch (e: Exception) {
            // Error silencioso - la precarga es opcional
        }
    }

    /**
     * Configura la pantalla de carga inicial de forma rapida.
     *
     * Oculta el contenido principal y muestra la pantalla de carga
     * con la configuracion de transparencias adecuada.
     */
    private fun setupLoadingScreen() {
        if (isActivityDestroyed) return

        // Ocultar contenido principal
        b.main.visibility = View.INVISIBLE
        b.main.alpha = 0f

        // Mostrar pantalla de carga
        b.loadingContainer.visibility = View.VISIBLE
        b.loadingContainer.alpha = 1f
        b.pbCargar.visibility = View.VISIBLE
    }

    /**
     * Inicializa el RecyclerView y su adaptador de forma optimizada.
     *
     * Configura el adaptador con el callback de click y aplica optimizaciones
     * de rendimiento al RecyclerView para una experiencia mas fluida.
     */
    private fun initializeComponents() {
        if (isActivityDestroyed) return

        try {
            // Crear adaptador con callback de click
            episodiosAdapter = EpisodiosAdapter(
                onItemClick = { episodio ->
                    // Verificar estado antes de procesar click
                    if (!isActivityDestroyed && !isFinishing) {
                        onClickListener(this, episodio)
                    }
                },
                ivPortadaAux = ivEpisodioPortadaAux // Imagen alternativa para episodios
            )

            // Configurar RecyclerView con optimizaciones
            if (!isActivityDestroyed && isContextValid()) {
                b.rvEpisodios.apply {
                    layoutManager = LinearLayoutManager(
                        this@EpisodiosActivity,
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    adapter = episodiosAdapter

                    // Optimizaciones de rendimiento
                    setHasFixedSize(true) // Tamaño fijo mejora rendimiento
                    itemAnimator = null // Desactivar animaciones para mejor rendimiento
                    setItemViewCacheSize(10) // Cache mas grande para scroll fluido
                }
            }
        } catch (e: Exception) {
            if (!isActivityDestroyed) {
                showError("Error inicializando RecyclerView: ${e.message}")
            }
        }
    }

    /**
     * Configura la interfaz de usuario con colores, textos e imagenes.
     *
     * Aplica el esquema de colores calculado, configura los textos de la temporada
     * y carga la imagen de portada con fallback en caso de error.
     */
    private fun setupUI() {
        if (isActivityDestroyed) return

        uiSetupJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
            try {
                // Configurar colores de fondo basados en la imagen dominante
                b.fondoMain.setBackgroundColor(ColorManager.darkerColor)
                setupContainerBackground()

                // Determinar color de texto segun el tema
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK

                // Configurar nombre de la temporada
                b.tvNombreTemporada.setTextColor(textColor)
                b.tvNombreTemporada.text = tempName

                // Configurar descripcion con fallback si esta vacia
                b.tvOverview.setTextColor(textColor)
                b.tvOverview.text = tempOverview.takeIf { it.isNotEmpty() }
                    ?: "No tenemos una sinopsis en español e ingles."

                // Cargar imagen de portada con sistema de fallback
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/w500${ivPortada}")
                    .placeholder(R.drawable.media_carga) // Imagen mientras carga
                    .transform(RoundedCornersTransformation(16, 0)) // Esquinas redondeadas
                    .fit()
                    .centerCrop()
                    .into(b.ivPortada, object : Callback {
                        override fun onSuccess() {
                            // Imagen principal cargada correctamente
                        }
                        override fun onError(e: Exception?) {
                            // Fallback a imagen alternativa
                            Picasso.get()
                                .load("https://image.tmdb.org/t/p/w500$ivPortadaAux")
                                .error(R.drawable.media_carga) // Imagen por defecto si tambien falla
                                .placeholder(R.drawable.media_carga)
                                .transform(RoundedCornersTransformation(16, 0))
                                .fit()
                                .centerCrop()
                                .into(b.ivPortada)
                        }
                    })

            } catch (e: Exception) {
                if (!isActivityDestroyed) {
                    showError("Error configurando UI: ${e.message}")
                }
            }
        }
    }

    /**
     * Carga la imagen de portada de forma optimizada en un hilo separado.
     *
     * Metodo auxiliar que maneja la carga de imagen con sistema de fallback
     * en un dispatcher especializado para operaciones de imagen.
     */
    private fun loadPortadaImage() {
        lifecycleScope.launch(imageLoadingDispatcher) {
            try {
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/w500${ivPortada}")
                    .placeholder(R.drawable.media_carga)
                    .transform(RoundedCornersTransformation(16, 0))
                    .fit()
                    .centerCrop()
                    .into(b.ivPortada, object : Callback {
                        override fun onSuccess() {
                            // Carga exitosa
                        }
                        override fun onError(e: Exception?) {
                            // Intentar con imagen alternativa
                            Picasso.get()
                                .load("https://image.tmdb.org/t/p/w500$ivPortadaAux")
                                .error(R.drawable.media_carga)
                                .placeholder(R.drawable.media_carga)
                                .transform(RoundedCornersTransformation(16, 0))
                                .fit()
                                .centerCrop()
                                .into(b.ivPortada)
                        }
                    })
            } catch (e: Exception) {
                // Error silencioso para carga de imagen
            }
        }
    }

    /**
     * Configura el fondo del contenedor de temporada con gradiente y bordes.
     *
     * Crea un drawable con esquinas redondeadas y borde que se adapta
     * al tema (claro u oscuro) basado en el color dominante de la imagen.
     */
    private fun setupContainerBackground() {
        val containerBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(ColorManager.averageColor) // Color de fondo adaptativo
            cornerRadius = 24f // Esquinas redondeadas modernas

            // Borde adaptativo segun el tema
            if (ColorManager.isDark) {
                setStroke(2, Color.argb(30, 255, 255, 255)) // Borde blanco semitransparente
            } else {
                setStroke(2, Color.argb(30, 0, 0, 0)) // Borde negro semitransparente
            }
        }
        b.containerTemporada.background = containerBackground
    }

    /**
     * Carga episodios desde la API con manejo optimizado de hilos y cache.
     *
     * Ejecuta la peticion en un hilo de background y maneja los resultados
     * a traves del sistema de estados, incluyendo cache para futuras consultas.
     */
    private fun cargarEpisodios() {
        if (isActivityDestroyed) return

        dataLoadingJob = lifecycleScope.launch(backgroundDispatcher) {
            try {
                if (isActivityDestroyed) return@launch

                // Obtener episodios con sistema de fallback entre idiomas
                val episodios = getEpisodiosTemporadaConFallback(idSerie, idTemporada)
                val cacheKey = "${idSerie}_${idTemporada}"

                if (!isActivityDestroyed && episodios.isNotEmpty()) {
                    // Guardar en cache para futuras consultas
                    episodiosCache[cacheKey] = episodios
                    loadingChannel.send(LoadingState.DataLoaded(episodios))
                } else if (!isActivityDestroyed) {
                    loadingChannel.send(LoadingState.Error("No se encontraron episodios"))
                }

            } catch (e: Exception) {
                if (!isActivityDestroyed) {
                    loadingChannel.send(LoadingState.Error("Error cargando episodios: ${e.message}"))
                }
            }
        }
    }

    /**
     * Obtiene episodios de una temporada con sistema de fallback entre idiomas.
     *
     * Realiza peticiones paralelas en español e ingles, y combina los datos
     * para obtener la informacion mas completa posible, priorizando español
     * pero completando con ingles cuando sea necesario.
     *
     * @param serieId ID de la serie
     * @param temporadaId ID de la temporada
     * @return Lista de episodios con datos combinados
     */
    private suspend fun getEpisodiosTemporadaConFallback(serieId: Int, temporadaId: Int): List<Episodio> {
        return withContext(backgroundDispatcher) {
            try {
                // Lanzar ambas peticiones en paralelo para reducir tiempo total
                val deferredES = async { api.getTvSeason(serieId, temporadaId, "es-ES") }
                val deferredEN = async { api.getTvSeason(serieId, temporadaId, "en-US") }

                // Esperar respuesta en español (prioritaria)
                val responseES = deferredES.await()
                val episodiosES = responseES.episodes ?: emptyList()

                // Verificar si necesitamos completar con datos en ingles
                val needsEnglishData = episodiosES.any { episodio ->
                    episodio.name.isNullOrBlank() || // Sin nombre
                            episodio.overview.isNullOrBlank() || // Sin descripcion
                            episodio.still_path.isNullOrBlank() || // Sin imagen
                            episodio.runtime == null || // Sin duracion
                            episodio.runtime == 0 // Duracion invalida
                }

                if (needsEnglishData) {
                    try {
                        // Esperar respuesta en ingles y combinar datos
                        val responseEN = deferredEN.await()
                        val episodiosEN = responseEN.episodes ?: emptyList()
                        val episodiosENMap = episodiosEN.associateBy { it.episode_number } // Map para busqueda rapida

                        // Combinar datos de forma eficiente
                        episodiosES.map { episodioES ->
                            val episodioEN = episodiosENMap[episodioES.episode_number]
                            combineEpisodeData(episodioES, episodioEN)
                        }
                    } catch (e: Exception) {
                        // Si falla la peticion en ingles, usar solo los datos en español
                        episodiosES
                    }
                } else {
                    // Los datos en español estan completos
                    episodiosES
                }

            } catch (e: Exception) {
                println("Error en EpisodiosActivity: Error en la llamada a la API")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Combina datos de episodios de español e ingles priorizando español.
     *
     * Toma los datos del episodio en español y los completa con datos
     * del episodio en ingles solo cuando los campos estan vacios o son invalidos.
     *
     * @param episodioES Episodio con datos en español (prioritario)
     * @param episodioEN Episodio con datos en ingles (fallback)
     * @return Episodio con datos combinados
     */
    private fun combineEpisodeData(episodioES: Episodio, episodioEN: Episodio?): Episodio {
        return Episodio(
            air_date = episodioES.air_date, // Fecha siempre la misma
            episode_number = episodioES.episode_number, // Numero siempre el mismo
            // Usar nombre en español si existe, sino ingles
            name = episodioES.name?.takeIf { it.isNotBlank() } ?: episodioEN?.name,
            // Usar descripcion en español si existe, sino ingles
            overview = episodioES.overview?.takeIf { it.isNotBlank() } ?: episodioEN?.overview,
            // Usar duracion valida (>0) en español, sino ingles
            runtime = episodioES.runtime?.takeIf { it > 0 } ?: episodioEN?.runtime,
            // Usar imagen en español si existe, sino ingles
            still_path = episodioES.still_path?.takeIf { it.isNotBlank() } ?: episodioEN?.still_path,
            // Usar calificacion de español, sino ingles
            vote_average = episodioES.vote_average ?: episodioEN?.vote_average
        )
    }

    /**
     * Callback que se ejecuta cuando los datos terminan de cargar completamente.
     *
     * Gestiona la transicion final de la pantalla de carga al contenido
     * con un pequeño delay para mejorar la percepcion de fluidez.
     */
    private fun onDataLoaded() {
        if (isLoadingComplete || isActivityDestroyed) return

        loadingJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
            try {
                // Pequeño delay para que se sienta natural la transicion
                delay(LOADING_DELAY)

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

    /**
     * Oculta la pantalla de carga y muestra el contenido principal con animaciones fluidas.
     *
     * Realiza una transicion suave entre la pantalla de carga y el contenido principal
     * utilizando animaciones de desvanecimiento. La funcion incluye multiples verificaciones
     * de estado para evitar ejecuciones duplicadas o en contextos invalidos.
     *
     * El proceso de transicion:
     * 1. Verifica que no se haya completado ya la carga
     * 2. Anima la desaparicion del contenedor de carga
     * 3. Oculta los elementos de carga cuando termina la animacion
     * 4. Muestra y anima la aparicion del contenido principal
     *
     * Las animaciones estan optimizadas para ofrecer una experiencia visual suave
     * con duraciones y delays configurables mediante constantes.
     */
    private fun hideLoadingAndShowContent() {
        if (isLoadingComplete || isActivityDestroyed || isFinishing) return
        isLoadingComplete = true

        // Animaciones mas rapidas y fluidas
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

        b.main.visibility = View.VISIBLE
        b.main.animate()
            .alpha(1f)
            .setDuration(FADE_IN_DURATION)
            .setStartDelay(FADE_IN_DELAY)
            .start()
    }

    /**
     * Verifica si el contexto de la activity sigue siendo valido para operaciones seguras.
     *
     * Esta funcion es crucial para evitar crashes y errores en operaciones asincronas.
     * Comprueba multiples estados de la activity para determinar si es seguro continuar
     * con operaciones que modifican la UI o acceden a recursos.
     *
     * Estados verificados:
     * - isDestroyed: Si la activity ha sido destruida por el sistema
     * - isFinishing: Si la activity esta en proceso de finalizacion
     * - isActivityDestroyed: Flag interno personalizado de control
     *
     * @return true si el contexto es valido y seguro para usar, false en caso contrario
     */
    private fun isContextValid(): Boolean {
        return !isDestroyed && !isFinishing && !isActivityDestroyed
    }

    /**
     * Muestra un mensaje de error de forma segura y finaliza la activity.
     *
     * Metodo optimizado para manejo de errores que garantiza una ejecucion segura
     * en el hilo principal. Incluye multiples verificaciones de estado para evitar
     * operaciones en contextos invalidos.
     *
     * Proceso de manejo de error:
     * 1. Verifica que la activity no este destruida
     * 2. Ejecuta en el hilo principal con prioridad inmediata
     * 3. Oculta elementos de carga
     * 4. Imprime el error para debugging
     * 5. Finaliza la activity de forma controlada
     *
     * @param message Mensaje descriptivo del error que se ha producido
     */
    private fun showError(message: String) {
        if (isActivityDestroyed) return

        lifecycleScope.launch(Dispatchers.Main.immediate) {
            if (!isActivityDestroyed && !isFinishing) {
                println("Error en EpisodiosActivity: $message")

                b.loadingContainer.visibility = View.GONE
                b.pbCargar.visibility = View.GONE

                finish()
            }
        }
    }

    /**
     * Callback del ciclo de vida que se ejecuta cuando la activity pasa a segundo plano.
     *
     * Limpia las animaciones en curso para evitar comportamientos inesperados
     * cuando la activity se pausa. Esto es importante para:
     * - Evitar animaciones ejecutandose en background
     * - Liberar recursos de animacion innecesarios
     * - Prevenir inconsistencias visuales al volver a primer plano
     *
     * Sobrescribe el metodo de la clase padre para agregar funcionalidad especifica
     * mientras mantiene el comportamiento estandar del ciclo de vida.
     */
    override fun onPause() {
        super.onPause()
        b.loadingContainer.clearAnimation()
        b.main.clearAnimation()
    }

    /**
     * Callback del ciclo de vida que limpia todos los recursos cuando la activity se destruye.
     *
     * Realiza una limpieza exhaustiva y ordenada de todos los recursos utilizados
     * por la activity para prevenir memory leaks y garantizar un cierre limpio.
     * Este metodo es critico para la estabilidad de la aplicacion.
     *
     * Recursos limpiados:
     * - Cancela todos los jobs de corrutinas activos
     * - Cierra channels de comunicacion
     * - Libera dispatchers personalizados
     * - Limpia callbacks del handler
     * - Limpia adaptadores inicializados
     * - Detiene animaciones en curso
     * - Establece flags de control de estado
     *
     * La limpieza se realiza en un orden especifico para evitar dependencias
     * y garantizar que todos los recursos se liberen correctamente.
     *
     * Sobrescribe el metodo de la clase padre manteniendo la llamada super
     * al final para preservar el comportamiento estandar del ciclo de vida.
     */
    override fun onDestroy() {
        isActivityDestroyed = true

        // Cancelar todas las operaciones
        loadingJob?.cancel()
        dataLoadingJob?.cancel()
        imagePreloadingJob?.cancel()
        uiSetupJob?.cancel()

        handler.removeCallbacksAndMessages(null)
        loadingChannel.close()

        // Limpiar dispatchers
        backgroundDispatcher.close()
        imageLoadingDispatcher.close()

        if (::episodiosAdapter.isInitialized) {
            episodiosAdapter.cleanup()
        }

        b.loadingContainer.clearAnimation()
        b.main.clearAnimation()

        super.onDestroy()
    }
}