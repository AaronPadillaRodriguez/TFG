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
 * Clase que maneja la visualizacion de contenido multimedia popular.
 * Permite filtrar entre diferentes categorias mediante un Spinner.
 */
class Popular(private val context: Context, private val b: ActivityMainBinding) {
    /**
     * Adaptador para el RecyclerView que muestra los elementos multimedia populares.
     */
    private lateinit var mediaAdapter: MediaAdapter

    /**
     * Categoría actualmente seleccionada para filtrar el contenido.
     * Por defecto, se establece en "RETRANSMISION".
     */
    private var currentCategory = OpcionesSpinner.RETRANSMISION.opcion

    /**
     * Inicializa el componente y comienza la carga de datos.
     * Configura el Spinner y RecyclerView en un hilo secundario.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarContenido()
        }
    }

    /**
     * Configura el Spinner con opciones de filtrado.
     * Establece el listener para detectar cambios de seleccion.
     */
    private fun initSpinner() {
        val adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.Popular))

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Popular.spPopular.adapter = adapter

        b.Popular.spPopular.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Se ejecuta cuando se selecciona un nuevo elemento en el spinner.
             * Actualiza la categoría actual y recarga el contenido según la selección.
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
                    0 -> OpcionesSpinner.RETRANSMISION.opcion
                    1 -> OpcionesSpinner.EN_TELEVISION.opcion
                    2 -> OpcionesSpinner.EN_ALQUILER.opcion
                    3 -> OpcionesSpinner.EN_CINES.opcion
                    else -> currentCategory
                }
                cargarContenido()
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
        val recyclerView = b.Popular.rvPopular

        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)
        }

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
        recyclerView.smoothScrollToPosition(0)
    }

    /**
     * Carga el contenido segun la categoria seleccionada.
     * Realiza diferentes llamadas a la API dependiendo de la opcion elegida.
     */
    private fun cargarContenido() {
        when (currentCategory) {
            OpcionesSpinner.EN_CINES.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getPeliculasEnCine() },true)
            OpcionesSpinner.RETRANSMISION.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getEnRetransmision() }, true)
            OpcionesSpinner.EN_ALQUILER.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getPeliculasEnAlquiler() }, true)
            OpcionesSpinner.EN_TELEVISION.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getEnTelevision() }, true)
        }
    }
}
