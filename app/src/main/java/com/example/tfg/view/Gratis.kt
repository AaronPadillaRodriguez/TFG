package com.example.tfg.view

import android.content.Context
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
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

class Gratis(private val context: Context, private val b: ActivityMainBinding){
    private lateinit var mediaAdapter: MediaAdapter
    private var currentCategory = OpcionesSpinner.GRATIS_PELICULAS.texto

    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()
            initRecyclerView()
            cargarGratis()
        }
    }

    private fun initSpinner() {
        val adapter = ArrayAdapter(context,
                                    android.R.layout.simple_spinner_item,
                                    context.resources.getStringArray(R.array.Gratis))

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Gratis.spGratis.adapter = adapter

        b.Gratis.spGratis.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initRecyclerView() {
        val recyclerView = b.Gratis.rvGratis
        recyclerView.smoothScrollToPosition(0)

        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)
        }

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = mediaAdapter
    }

    private fun cargarGratis() {
        fetchMedia(context, mediaAdapter) { api -> api.getGratis(currentCategory) }
    }
}