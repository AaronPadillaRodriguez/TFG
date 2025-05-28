package com.example.tfg.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg.databinding.ActivityMainBinding

/**
 * Actividad principal que sirve como punto de entrada de la aplicacion.
 * Configura la interfaz y gestiona las diferentes secciones de contenido multimedia.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    /**
     * Configura la actividad al crearse.
     * Inicializa Firebase, establece el layout y configura las secciones de contenido.
     * @param savedInstanceState Estado previo de la actividad (si existe).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuracion inicial de la UI
        b = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(b.root)

        //supportActionBar?.setDisplayShowTitleEnabled(false)

        // Ajustes de la barra de sistema
        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializacion de secciones de contenido
        Trending(this, b)
        Popular(this, b)
        Gratis(this, b)
    }
}