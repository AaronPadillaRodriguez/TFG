package com.example.tfg.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tfg.databinding.ActivityMainBinding

/**
 * Actividad principal que sirve como punto de entrada de la aplicacion.
 *
 * Esta clase gestiona la interfaz principal de la aplicacion, configurando y coordinando
 * las diferentes secciones de contenido multimedia (Trending, Popular y Gratis).
 * Implementa persistencia de estado para mantener las selecciones de los usuarios
 * durante rotaciones de pantalla y cambios de configuracion.

 */
class MainActivity : AppCompatActivity() {

    /** Binding para acceder a las vistas del layout de forma type-safe */
    private lateinit var b: ActivityMainBinding

    /** Instancia que gestiona la seccion de contenido en tendencia */
    private var trending: Trending? = null

    /** Instancia que gestiona la seccion de contenido popular */
    private var popular: Popular? = null

    /** Instancia que gestiona la seccion de contenido gratuito */
    private var gratis: Gratis? = null

    // Variables para persistir el estado de los Spinners entre cambios de configuracion
    /** Posicion seleccionada en el spinner de tendencias */
    private var trendingSpinnerPos = 0

    /** Posicion seleccionada en el spinner de contenido popular */
    private var popularSpinnerPos = 0

    /** Posicion seleccionada en el spinner de contenido gratuito */
    private var gratisSpinnerPos = 0

    /**
     * Metodo llamado cuando se crea la actividad.
     *
     * Configura el binding de la vista, restaura el estado guardado si existe,
     * e inicializa las tres secciones principales de contenido multimedia.
     *
     * @param savedInstanceState Bundle que contiene el estado previamente guardado
     *                          de la actividad, o null si es la primera vez que se crea
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Infla el layout usando View Binding para acceso type-safe a las vistas
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Restaura las posiciones de los spinners si la actividad se esta recreando
        if (savedInstanceState != null) {
            trendingSpinnerPos = savedInstanceState.getInt("trendingPos", 0)
            popularSpinnerPos = savedInstanceState.getInt("popularPos", 0)
            gratisSpinnerPos = savedInstanceState.getInt("gratisPos", 0)
        }

        // Inicializa la seccion de tendencias y restaura su estado
        trending = Trending(this, b).apply {
            // Restaura la seleccion previa del spinner
            b.Tendencias.spTendencias.setSelection(trendingSpinnerPos)
        }

        // Inicializa la seccion de contenido popular y restaura su estado
        popular = Popular(this, b).apply {
            // Restaura la seleccion previa del spinner
            b.Popular.spPopular.setSelection(popularSpinnerPos)
        }

        // Inicializa la seccion de contenido gratuito y restaura su estado
        gratis = Gratis(this, b).apply {
            // Restaura la seleccion previa del spinner
            b.Gratis.spGratis.setSelection(gratisSpinnerPos)
        }
    }

    /**
     * Guarda el estado actual de la actividad antes de ser destruida.
     *
     * Este metodo es llamado por el sistema antes de que la actividad sea destruida
     * debido a cambios de configuracion (como rotacion de pantalla). Preserva las
     * posiciones actuales de todos los spinners para poder restaurarlas posteriormente.
     *
     * @param outState Bundle donde se guardan los datos de estado que se quieren preservar
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Guarda las posiciones actuales de cada spinner para preservar la seleccion del usuario
        outState.putInt("trendingPos", b.Tendencias.spTendencias.selectedItemPosition)
        outState.putInt("popularPos", b.Popular.spPopular.selectedItemPosition)
        outState.putInt("gratisPos", b.Gratis.spGratis.selectedItemPosition)
    }
}