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
 * Clase que maneja la visualizacion de contenido multimedia gratuito.
 * Permite filtrar entre peliculas y programas de TV gratuitos mediante un Spinner.
 */
class Gratis(private val context: Context, private val b: ActivityMainBinding) {
    /**
     * Adaptador para el RecyclerView que muestra los elementos multimedia gratuitos.
     */
    private lateinit var mediaAdapter: MediaAdapter

    /**
     * Categoria actualmente seleccionada para filtrar el contenido gratuito.
     * Por defecto, se establece en peliculas gratuitas.
     */
    private var currentCategory = OpcionesSpinner.GRATIS_PELICULAS.texto

    /**
     * Inicializa el componente y comienza la carga de datos.
     * Configura el Spinner y RecyclerView en un hilo secundario.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarGratis()
        }
    }

    /**
     * Configura el Spinner con opciones de filtrado.
     * Establece el listener para detectar cambios de seleccion.
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
             * Actualiza la categoria actual y recarga el contenido gratuito.
             *
             * @param parent El AdapterView donde se realizo la seleccion.
             * @param view La vista dentro del AdapterView que fue seleccionada.
             * @param position La posicion del elemento seleccionado en el adaptador.
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
             * Se ejecuta cuando no hay ninguna seleccion en el spinner.
             * Metodo vacio ya que siempre habra una opcion seleccionada.
             *
             * @param parent El AdapterView que ahora no contiene ninguna seleccion.
             */
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Inicializa el RecyclerView para mostrar el contenido.
     * Configura un layout horizontal y el adaptador de medios.
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
     * Carga el contenido gratuito segun la categoria seleccionada.
     * Usa la API para obtener los datos y los muestra aleatoriamente.
     */
    private fun cargarGratis() {
        fetchMedia(context, mediaAdapter, { api -> api.getGratis(currentCategory) }, shuffle = true)
    }
}