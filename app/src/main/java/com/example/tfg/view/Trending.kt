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

class Trending(private val context: Context, private val b: ActivityMainBinding) {
    private lateinit var mediaAdapter: MediaAdapter
    private var currentCategory = OpcionesSpinner.HOY.texto

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarTendencias()
        }
    }

    private fun initSpinner() {
        val adapter = ArrayAdapter(context,
                                    android.R.layout.simple_spinner_item,
                                    context.resources.getStringArray(R.array.Tendencias))

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Tendencias.spTendencias.adapter = adapter

        b.Tendencias.spTendencias.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initRecyclerView() {
        val recyclerView = b.Tendencias.rvTendencias
        recyclerView.smoothScrollToPosition(0)

        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)
        }

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
    }

    private fun cargarTendencias() {
        fetchMedia(context, mediaAdapter) { api -> api.getTrendingAll("all", currentCategory) }
        b.Tendencias.rvTendencias.smoothScrollToPosition(0)
    }

}