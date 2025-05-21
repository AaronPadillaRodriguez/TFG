package com.example.tfg.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg.databinding.ActivityDetallesBinding
import com.example.tfg.model.adapter.MediaAdapter.Companion.getRetrofit
import com.example.tfg.model.api.APImedia
import com.example.tfg.model.dataclass.Pelicula
import com.example.tfg.model.dataclass.TvShow
import com.example.tfg.utils.fetchWithLanguageFallback

class DetallesActivity : AppCompatActivity() {
    private lateinit var b: ActivityDetallesBinding
    private val api: APImedia = getRetrofit().create(APImedia::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDetallesBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val id = intent.getIntExtra("ID", -1)
        val tipo = intent.getStringExtra("TYPE")

        try {
            DetalleSuperior(this, b, id, tipo.toString())
        } catch (e: Exception) {
            finish()
        }
    }
}