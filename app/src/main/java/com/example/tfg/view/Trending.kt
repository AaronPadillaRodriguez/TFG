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
 * Clase que maneja la visualización de contenido multimedia en tendencia.
 * Permite filtrar entre tendencias del día o de la semana.
 */
class Trending(private val context: Context, private val b: ActivityMainBinding) {

    // Adaptador para el RecyclerView que muestra los elementos multimedia en tendencia.
    private lateinit var mediaAdapter: MediaAdapter

    //Categoría temporal actualmente seleccionada ("day" para hoy o "week" para esta semana).
    private var currentCategory = OpcionesSpinner.HOY.texto

    /**
     * Inicializa el componente y comienza la carga de datos.
     * Configura el Spinner y RecyclerView en un hilo secundario.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarTendencias()
        }
    }

    /**
     * Configura el Spinner con opciones de filtrado temporal (Hoy/Esta semana).
     * Establece el listener para detectar cambios de selección.
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
     * Inicializa el RecyclerView para mostrar el contenido.
     * Configura un layout horizontal y el adaptador de medios.
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
     * Carga las tendencias según la categoría temporal seleccionada.
     * Realiza la llamada a la API para obtener los contenidos populares.
     */
    private fun cargarTendencias() {
        fetchMedia(context, mediaAdapter, { api -> api.getTrendingAll("all", currentCategory) })
    }
}