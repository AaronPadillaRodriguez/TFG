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

class Popular(private val context: Context, private val b: ActivityMainBinding) {
    private lateinit var mediaAdapter: MediaAdapter
    private var currentCategory = OpcionesSpinner.RETRANSMISION.opcion

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarContenido()
        }
    }

    private fun initSpinner() {
        val adapter = ArrayAdapter(context,
                                    android.R.layout.simple_spinner_item,
                                    context.resources.getStringArray(R.array.Popular))

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Popular.spPopular.adapter = adapter

        b.Popular.spPopular.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun initRecyclerView() {
        val recyclerView = b.Popular.rvPopular
        recyclerView.smoothScrollToPosition(0)

        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)
        }

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
    }

    private fun cargarContenido() {
        when (currentCategory) {
            OpcionesSpinner.EN_CINES.opcion -> fetchMedia(context, mediaAdapter) { api -> api.getPeliculasEnCine() }
            OpcionesSpinner.RETRANSMISION.opcion -> fetchMedia(context, mediaAdapter) { api -> api.getEnRetransmision() }
            OpcionesSpinner.EN_ALQUILER.opcion -> fetchMedia(context, mediaAdapter) { api -> api.getPeliculasEnAlquiler() }
            OpcionesSpinner.EN_TELEVISION.opcion -> fetchMedia(context, mediaAdapter) { api -> api.getEnTelevision() }
        }
    }
}
