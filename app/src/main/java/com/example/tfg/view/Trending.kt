package com.example.tfg.view

import android.content.Context
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.R
import com.example.tfg.databinding.ActivityMainBinding
import com.example.tfg.model.adapter.MediaAdapter
import com.example.tfg.model.adapter.MediaAdapter.Companion.fetchMedia
import com.example.tfg.model.adapter.MediaAdapter.Companion.onClickListener
import com.example.tfg.model.enums.OpcionesSpinner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase encargada de mostrar contenido multimedia en tendencia según diferentes categorías temporales.
 *
 * @property context Contexto de la aplicación necesario para acceder a recursos y servicios.
 * @property b Binding de la actividad principal que contiene las referencias a las vistas.
 */
class Trending(private val context: Context, private val b: ActivityMainBinding) {

    // Adaptador para el RecyclerView que muestra los elementos multimedia en tendencia.
    private lateinit var mediaAdapter: MediaAdapter

    //Categoría temporal actualmente seleccionada ("day" para hoy o "week" para esta semana).
    private var currentCategory = OpcionesSpinner.HOY.texto

    /**
     * Bloque de inicialización que se ejecuta al crear una instancia de la clase.
     * Lanza una corrutina en un hilo para inicializar las vistas y cargar los datos.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarTendencias()
        }
    }

    /**
     * Inicializa el spinner con las opciones de categorías temporales (Hoy/Esta semana)
     * y configura su listener para detectar cambios de selección.
     */
    private fun initSpinner() {
        val adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.Tendencias))

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Tendencias.spTendencias.adapter = adapter

        b.Tendencias.spTendencias.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Se ejecuta cuando se selecciona un nuevo elemento en el spinner.
             * Actualiza la categoría actual y recarga las tendencias.
             *
             * @param parent El AdapterView donde se realizó la selección.
             * @param view La vista dentro del AdapterView que fue seleccionada.
             * @param position La posición del elemento seleccionado en el adaptador.
             * @param id El ID de fila del elemento seleccionado.
             */
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                currentCategory = when (position) {
                    0 -> OpcionesSpinner.HOY.texto
                    1 -> OpcionesSpinner.ESTA_SEMANA.texto
                    else -> currentCategory
                }
                cargarTendencias()
            }

            /**
             * Se ejecuta cuando no hay ninguna selección en el spinner.
             * Método vacío ya que siempre habrá una opción seleccionada.
             *
             * @param parent El AdapterView que ahora no contiene ninguna selección.
             */
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Inicializa el RecyclerView que muestra los elementos multimedia en tendencia.
     * Configura el adaptador con un listener para manejar clics en los elementos.
     */
    private fun initRecyclerView() {
        val recyclerView = b.Tendencias.rvTendencias

        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)
        }

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
        recyclerView.smoothScrollToPosition(0)
    }

    /**
     * Carga los elementos multimedia en tendencia según la categoría seleccionada.
     * Utiliza la función fetchMedia para obtener los datos de la API.
     */
    private fun cargarTendencias() {
        fetchMedia(context, mediaAdapter, { api -> api.getTrendingAll("all", currentCategory) })
    }
}