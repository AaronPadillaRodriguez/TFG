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
 * Clase encargada de mostrar contenido multimedia gratuito según la categoría seleccionada.
 * Permite filtrar entre películas gratuitas y programas de televisión gratuitos.
 *
 * @property context Contexto de la aplicación necesario para acceder a recursos y servicios.
 * @property b Binding de la actividad principal que contiene las referencias a las vistas.
 */
class Gratis(private val context: Context, private val b: ActivityMainBinding) {
    /**
     * Adaptador para el RecyclerView que muestra los elementos multimedia gratuitos.
     */
    private lateinit var mediaAdapter: MediaAdapter

    /**
     * Categoría actualmente seleccionada para filtrar el contenido gratuito.
     * Por defecto, se establece en películas gratuitas.
     */
    private var currentCategory = OpcionesSpinner.GRATIS_PELICULAS.texto

    /**
     * Bloque de inicialización que se ejecuta al crear una instancia de la clase.
     * Lanza una corrutina en un hilo de IO para inicializar las vistas y cargar los datos.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarGratis()
        }
    }

    /**
     * Inicializa el spinner con las opciones de categorías de contenido gratuito
     * y configura su listener para detectar cambios de selección.
     */
    private fun initSpinner() {
        val adapter = ArrayAdapter(context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.Gratis))

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Gratis.spGratis.adapter = adapter

        b.Gratis.spGratis.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Se ejecuta cuando se selecciona un nuevo elemento en el spinner.
             * Actualiza la categoría actual y recarga el contenido gratuito.
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
                    0 -> OpcionesSpinner.GRATIS_PELICULAS.texto
                    1 -> OpcionesSpinner.GRATIS_TELEVISION.texto
                    else -> currentCategory
                }
                cargarGratis()
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
     * Inicializa el RecyclerView que muestra los elementos multimedia gratuitos.
     * Configura el adaptador con un listener para manejar clics en los elementos.
     */
    private fun initRecyclerView() {
        val recyclerView = b.Gratis.rvGratis

        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)
        }

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
        recyclerView.smoothScrollToPosition(0)
    }

    /**
     * Carga el contenido multimedia gratuito según la categoría seleccionada.
     * Utiliza la función fetchMedia para obtener los datos de la API.
     */
    private fun cargarGratis() {
        fetchMedia(context, mediaAdapter, { api -> api.getGratis(currentCategory) }, shuffle = true)
    }
}