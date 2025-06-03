package com.example.tfg.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfg.R
import com.example.tfg.databinding.ActivityMainBinding
import com.example.tfg.model.adapter.MediaAdapter
import com.example.tfg.model.adapter.MediaAdapter.Companion.fetchMedia
import com.example.tfg.model.adapter.MediaAdapter.Companion.onClickListener
import com.example.tfg.model.enums.OpcionesSpinner
import com.example.tfg.utils.ViewUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase controladora para la gestion de contenido multimedia gratuito en la aplicacion.
 *
 * Esta clase se encarga de mostrar y gestionar una seccion dedicada al contenido gratuito,
 * incluyendo peliculas y series de television. Proporciona funcionalidad para filtrar
 * el contenido por categorias mediante un spinner y muestra los resultados en un
 * RecyclerView con desplazamiento horizontal.
 *
 * La clase maneja la carga asincrona de datos y la configuracion de la interfaz de usuario,
 * aplicando estilos personalizados y fondos redondeados a los componentes visuales.
 *
 * @param context El contexto de Android necesario para acceder a recursos y servicios del sistema
 * @param b El binding de la actividad principal que contiene las referencias a las vistas
 *
 * @constructor Inicializa automaticamente los componentes UI y carga los datos iniciales
 * utilizando corrutinas para operaciones asíncronas.
 */
class Gratis(private val context: Context, private val b: ActivityMainBinding) {

    /**
     * Adaptador para el RecyclerView que gestiona la visualizacion de elementos multimedia gratuitos.
     *
     * Se inicializa de forma tardia (lateinit) ya que su configuracion requiere que otros
     * componentes esten previamente configurados.
     */
    private lateinit var mediaAdapter: MediaAdapter

    /**
     * Categoria actualmente seleccionada para filtrar el contenido gratuito.
     *
     * Determina que tipo de contenido se muestra en el RecyclerView.
     * Por defecto, se establece en peliculas gratuitas para proporcionar una
     * experiencia inicial coherente al usuario.
     */
    private var currentCategory = OpcionesSpinner.GRATIS_PELICULAS.texto

    /**
     * Bloque de inicializacion que configura todos los componentes de la interfaz.
     *
     * Se ejecuta automaticamente al crear una instancia de la clase.
     * Utiliza una corrutina en el dispatcher IO para evitar bloquear el hilo principal
     * durante las operaciones de configuracion y carga inicial de datos.
     */
    init {
        CoroutineScope(Dispatchers.IO).launch {
            initSpinner()      // Configura el selector de categorias
            initRecyclerView() // Configura la lista de contenido
            cargarGratis()     // Carga los datos iniciales
        }
    }

    /**
     * Configura el Spinner con las opciones de filtrado disponibles.
     *
     * Este metodo se encarga de:
     * - Crear un adaptador personalizado para el spinner con estilos especificos
     * - Aplicar fondos redondeados y colores personalizados
     * - Configurar el listener para detectar cambios de seleccion
     *
     * El spinner permite al usuario alternar entre diferentes categorias de contenido
     * gratuito (peliculas y television).
     */
    private fun initSpinner() {
        // Crear adaptador personalizado con estilos aplicados
        val adapter = object : ArrayAdapter<String>(
            context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.Gratis) // Obtiene opciones desde recursos
        ) {
            /**
             * Personaliza la vista del elemento seleccionado en el spinner.
             * Aplica color negro y texto en negrita para mejorar la legibilidad.
             */
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK) // Mejora contraste
                textView.typeface = Typeface.DEFAULT_BOLD // Enfatiza la seleccion
                return view
            }

            /**
             * Personaliza la vista de los elementos en el menu desplegable.
             * Mantiene consistencia visual con el elemento seleccionado.
             */
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(Color.BLACK) // Consistencia visual
                textView.typeface = Typeface.DEFAULT_BOLD // Legibilidad mejorada
                return view
            }
        }

        // Crear y aplicar fondo redondeado personalizado
        val background = ViewUtils.createRoundedBackground(
            context = context,
            cornerRadiusDp = 8f,      // Bordes suavemente redondeados
            color = "#b0aa6b"         // Color dorado/beige corporativo
        )

        // Aplicar estilos al spinner
        b.Gratis.spGratis.background = background
        b.Gratis.spGratis.setPopupBackgroundDrawable(background) // Unifica diseño del popup

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.Gratis.spGratis.adapter = adapter

        // Configurar listener para cambios de seleccion
        b.Gratis.spGratis.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            /**
             * Maneja la seleccion de una nueva categoria en el spinner.
             *
             * Actualiza la categoria actual y recarga el contenido correspondiente.
             * Implementa una verificacion para evitar recargas innecesarias cuando
             * se selecciona la misma categoria.
             *
             * @param parent El AdapterView donde se realizo la seleccion
             * @param view La vista dentro del AdapterView que fue seleccionada
             * @param position La posicion del elemento seleccionado (0: peliculas, 1: TV)
             * @param id El ID de fila del elemento seleccionado
             */
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // Mapear posicion a categoria correspondiente
                val newCategory = when (position) {
                    0 -> OpcionesSpinner.GRATIS_PELICULAS.texto    // Peliculas gratuitas
                    1 -> OpcionesSpinner.GRATIS_TELEVISION.texto   // Series/TV gratuitas
                    else -> currentCategory // Fallback de seguridad
                }

                // Solo recargar si la categoria ha cambiado realmente
                if(currentCategory != newCategory) {
                    currentCategory = newCategory
                    cargarGratis() // Recargar contenido con nueva categoria
                }

                // Forzar que el spinner mantenga su fondo personalizado
                val background = ViewUtils.createRoundedBackground(
                    context = context,
                    cornerRadiusDp = 8f,
                    color = "#b0aa6b"
                )
                b.Gratis.spGratis.background = background
            }

            /**
             * Se ejecuta cuando no hay ninguna seleccion en el spinner.
             *
             * Metodo requerido por la interfaz pero sin implementacion especifica,
             * ya que el spinner siempre tendra una opcion seleccionada por defecto.
             *
             * @param parent El AdapterView que no contiene ninguna seleccion
             */
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No se requiere accion - siempre habra una seleccion
            }
        }
    }

    /**
     * Inicializa y configura el RecyclerView para mostrar el contenido multimedia.
     *
     * Este metodo se encarga de:
     * - Aplicar estilos visuales personalizados (fondo redondeado)
     * - Configurar el adaptador con funcionalidad de click
     * - Establecer un layout manager horizontal para desplazamiento lateral
     * - Posicionar la vista en el primer elemento
     *
     * El RecyclerView utiliza desplazamiento horizontal para optimizar el espacio
     * de pantalla y proporcionar una experiencia de navegacion fluida.
     */
    private fun initRecyclerView() {
        val recyclerView = b.Gratis.rvGratis

        // Crear fondo redondeado con mayor radio para el contenedor principal
        val background = ViewUtils.createRoundedBackground(
            context = context,
            cornerRadiusDp = 16f,     // Bordes mas redondeados para el contenedor
            color = "#b0aa6b"         // Mantiene consistencia con el spinner
        )

        recyclerView.background = background

        // Configurar adaptador con callback de click personalizado
        // CORREGIDO: Se elimina la línea que forzaba el scroll a posición 0 al hacer clic
        mediaAdapter = MediaAdapter { item ->
            onClickListener(context, item)  // Solo maneja la accion al hacer click
        }

        // Configurar layout horizontal para desplazamiento lateral
        recyclerView.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.HORIZONTAL, // Desplazamiento horizontal
            false // Sin orden inverso
        )
        recyclerView.adapter = mediaAdapter
        recyclerView.smoothScrollToPosition(0) // Solo posicionar en el primer elemento al inicializar
    }

    /**
     * Carga el contenido gratuito segun la categoria actualmente seleccionada.
     *
     * Utiliza el metodo fetchMedia del MediaAdapter para realizar la peticion a la API
     * de forma asincrona. Los datos se obtienen y muestran de forma aleatoria para
     * proporcionar variedad en cada carga.
     *
     * Una vez completada la carga, automaticamente posiciona la vista en el primer
     * elemento para garantizar una experiencia de usuario consistente cuando cambia
     * la categoria del spinner.
     *
     * @see MediaAdapter.fetchMedia para detalles sobre la implementacion de la carga de datos
     */
    private fun cargarGratis() {
        fetchMedia(
            mediaAdapter,                           // Adaptador donde mostrar los datos
            { api -> api.getGratis(currentCategory) }, // Lambda que define la llamada a la API
            shuffle = true                          // Aleatorizar resultados para variedad
        ) {
            // Callback ejecutado tras completar la carga
            // Solo vuelve al inicio cuando se carga nuevo contenido (cambio de categoria)
            b.Gratis.rvGratis.smoothScrollToPosition(0)
        }
    }
}