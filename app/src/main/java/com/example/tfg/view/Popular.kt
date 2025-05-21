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
 * Clase encargada de mostrar contenido multimedia popular según diferentes categorías.
 * Permite al usuario filtrar el contenido por opciones como retransmisión, televisión, alquiler o cines.
 *
 * @property context Contexto de la aplicación necesario para el acceso a recursos y servicios.
 * @property b Binding de la actividad principal que contiene las referencias a las vistas.
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
     * Bloque de inicialización que se ejecuta al crear una instancia de la clase.
     * Lanza una corrutina en un hilo de IO para inicializar las vistas y cargar los datos.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarContenido()
        }
    }

    /**
     * Inicializa el spinner con las opciones de categorías de contenido popular
     * y configura su listener para detectar cambios de selección.
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
     * Inicializa el RecyclerView que muestra los elementos multimedia populares.
     * Configura el adaptador con un listener para manejar clics en los elementos.
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
     * Carga el contenido multimedia según la categoría seleccionada.
     * Utiliza diferentes llamadas a la API dependiendo de la opción elegida.
     */
    private fun cargarContenido() {
        when (currentCategory) {
            OpcionesSpinner.EN_CINES.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getPeliculasEnCine() }, shuffle = true)
            OpcionesSpinner.RETRANSMISION.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getEnRetransmision() }, shuffle = true)
            OpcionesSpinner.EN_ALQUILER.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getPeliculasEnAlquiler() }, shuffle = true)
            OpcionesSpinner.EN_TELEVISION.opcion -> fetchMedia(context, mediaAdapter, { api -> api.getEnTelevision() }, shuffle = true)
        }
    }
}
