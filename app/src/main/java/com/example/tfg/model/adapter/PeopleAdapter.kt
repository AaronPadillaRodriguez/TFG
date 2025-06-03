package com.example.tfg.model.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.tfg.R
import com.example.tfg.databinding.ItemPeopleBinding
import com.example.tfg.model.dataclass.People
import com.example.tfg.utils.ColorManager
import com.example.tfg.view.DetallesActivity
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.coroutines.launch

/**
 * Adaptador personalizado para RecyclerView que gestiona la visualizacion de una lista de personas/actores.
 *
 * Esta clase implementa el patron ViewHolder para optimizar el rendimiento del RecyclerView,
 * utilizando ViewBinding para el enlace de vistas y Picasso para la carga asincrona de imagenes.
 * Incluye gestion de recursos y ciclo de vida para evitar memory leaks.
 *
 * **Funcionalidades principales:**
 * - Visualizacion de informacion de actores (nombre, personaje, foto de perfil)
 * - Carga asincrona de imagenes con placeholder y manejo de errores
 * - Aplicacion de colores dinamicos basados en ColorManager
 * - Gestion segura de clics y navegacion
 * - Limpieza automatica de recursos para prevenir memory leaks
 *
 * @param onItemClick Funcion lambda que se ejecuta cuando el usuario hace clic en un elemento.
 *                    Recibe como parametro el objeto [People] seleccionado.
 */
class PeopleAdapter(private val onItemClick: (People) -> Unit) : RecyclerView.Adapter<PeopleAdapter.PeopleViewHolder>() {

    /**
     * Lista mutable que almacena los elementos de personas a mostrar en el RecyclerView.
     * Se utiliza MutableList para permitir operaciones de adicion y eliminacion eficientes.
     */
    private val items = mutableListOf<People>()

    /**
     * Flag que indica si el adaptador ha sido limpiado para evitar operaciones sobre recursos liberados.
     * Importante para prevenir crashes cuando la actividad/fragmento es destruido.
     */
    private var isCleanedUp = false

    /**
     * Actualiza la lista completa de elementos mostrados en el RecyclerView.
     *
     * Este metodo reemplaza todos los elementos existentes con una nueva lista,
     * notificando al RecyclerView para que actualice la interfaz de usuario.
     * Incluye validacion del estado de limpieza para evitar operaciones sobre recursos liberados.
     *
     * @param newItems Nueva lista de objetos [People] a mostrar. Puede estar vacia.
     *
     * **Proceso interno:**
     * 1. Verifica que el adaptador no haya sido limpiado
     * 2. Limpia la lista actual de elementos
     * 3. Añade todos los nuevos elementos
     * 4. Notifica al RecyclerView del cambio completo de datos
     *
     * @throws Exception Si ocurre un error durante la actualizacion, se imprime el stack trace
     */
    fun submitList(newItems: List<People>) {
        if (isCleanedUp) return // Previene operaciones sobre adaptador limpiado

        try {
            items.clear() // Limpia elementos existentes
            items.addAll(newItems) // Añade nuevos elementos
            notifyDataSetChanged() // Notifica cambios al RecyclerView
        } catch (e: Exception) {
            e.printStackTrace() // Log de errores para debugging
        }
    }

    /**
     * Retorna el numero total de elementos en la lista.
     *
     * @return Numero de elementos si el adaptador esta activo, 0 si ha sido limpiado.
     *         Esto previene que el RecyclerView intente acceder a elementos inexistentes.
     */
    override fun getItemCount(): Int = if (isCleanedUp) 0 else items.size

    /**
     * Crea un nuevo ViewHolder para mostrar un elemento de la lista.
     *
     * Este metodo es llamado por el RecyclerView cuando necesita un nuevo ViewHolder.
     * Infla el layout del elemento y crea una nueva instancia de PeopleViewHolder.
     *
     * @param parent El ViewGroup contenedor donde se añadira la nueva vista
     * @param viewType Tipo de vista (no utilizado en esta implementacion, todos los elementos son iguales)
     * @return Nueva instancia de [PeopleViewHolder] que contiene la vista inflada
     *
     * **Proceso:**
     * 1. Obtiene el LayoutInflater del contexto del parent
     * 2. Infla el layout item_people.xml
     * 3. Crea y retorna un nuevo PeopleViewHolder con la vista inflada
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_people, parent, false)
        return PeopleViewHolder(view)
    }

    /**
     * Vincula los datos de una persona especifica a un ViewHolder existente.
     *
     * Este metodo es llamado por el RecyclerView para mostrar los datos en una posicion especifica.
     * Incluye validaciones de seguridad para evitar crashes por indices fuera de rango.
     *
     * @param holder El [PeopleViewHolder] que mostrara los datos
     * @param position La posicion del elemento en la lista (indice base 0)
     *
     * **Validaciones de seguridad:**
     * - Verifica que el adaptador no haya sido limpiado
     * - Comprueba que la posicion este dentro del rango valido
     * - Maneja excepciones para evitar crashes de la aplicacion
     *
     * @throws Exception Si ocurre un error durante el binding, se imprime el stack trace
     */
    override fun onBindViewHolder(holder: PeopleViewHolder, position: Int) {
        if (isCleanedUp || position >= items.size) return // Validaciones de seguridad

        try {
            // Delega el binding al ViewHolder con validacion de limpieza
            holder.bind(items[position], onItemClick, isCleanedUp)
        } catch (e: Exception) {
            e.printStackTrace() // Log para debugging
        }
    }

    /**
     * Limpia todos los recursos del adaptador y previene operaciones futuras.
     *
     * Este metodo debe ser llamado cuando la actividad/fragmento se destruye
     * para liberar memoria y prevenir memory leaks. Una vez llamado, el adaptador
     * no puede ser reutilizado.
     *
     * **Proceso de limpieza:**
     * 1. Marca el adaptador como limpiado
     * 2. Limpia la lista de elementos
     * 3. Notifica al RecyclerView para actualizar la UI
     *
     * @see isCleanedUp
     */
    fun cleanup() {
        isCleanedUp = true // Marca como limpiado
        items.clear() // Libera referencias a objetos People
        notifyDataSetChanged() // Actualiza UI para mostrar lista vacia
    }

    /**
     * Objeto companion que contiene metodos utilitarios estaticos para operaciones
     * relacionadas con el adaptador de personas.
     *
     * Estos metodos proporcionan funcionalidades auxiliares como carga de datos,
     * manejo de eventos y validaciones de contexto, manteniendo la separacion
     * de responsabilidades del adaptador principal.
     */
    companion object {
        /**
         * Carga y actualiza la lista de personas en el adaptador de forma asincrona.
         *
         * Este metodo maneja la carga de datos de personas utilizando corrutinas
         * para operaciones asincronas, con validaciones de contexto y ciclo de vida
         * para evitar crashes cuando la actividad se destruye.
         *
         * @param context Contexto de la aplicacion, preferiblemente una instancia de [LifecycleOwner]
         * @param peopleAdapter Instancia del adaptador a actualizar con los nuevos datos
         * @param response Lista de objetos [People] obtenida de la API o base de datos
         *
         * **Proceso de carga:**
         * 1. Valida que el contexto sea valido y el adaptador no este limpiado
         * 2. Si el contexto es LifecycleOwner, ejecuta en el alcance del ciclo de vida
         * 3. Valida nuevamente antes de procesar los datos
         * 4. Actualiza el adaptador o muestra mensaje si no hay datos
         *
         * **Manejo de errores:**
         * - Validaciones multiples de estado del contexto
         * - Try-catch para prevenir crashes
         * - Logging de errores para debugging
         *
         * @throws Exception Los errores se capturan y se registran, no se propagan
         */
        fun fetchPeople(context: Context, peopleAdapter: PeopleAdapter, response: List<People>) {
            // Verificacion inicial de contexto valido y adaptador no limpiado
            if (!isContextValid(context) || peopleAdapter.isCleanedUp) return

            try {
                // Si el contexto soporta ciclo de vida, usar corrutinas
                if (context is LifecycleOwner) {
                    context.lifecycleScope.launch {
                        // Re-validacion en el contexto de la corrutina
                        if (!isContextValid(context) || peopleAdapter.isCleanedUp) return@launch

                        try {
                            // Procesamiento de la respuesta
                            if (response.isNullOrEmpty()) {
                                // Validacion antes de mostrar mensaje
                                if (isContextValid(context)) {
                                    println("No hay contenido disponible.") // Log para debugging
                                }
                            } else {
                                // Actualizacion del adaptador con datos validos
                                peopleAdapter.submitList(response)
                            }
                        } catch (e: Exception) {
                            // Manejo de errores durante el procesamiento
                            if (isContextValid(context)) {
                                println("Error: ${e.message}")
                            }
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log de errores del nivel superior
            }
        }

        /**
         * Maneja el evento de clic en un elemento de persona de la lista.
         *
         * Este metodo se ejecuta cuando el usuario toca un elemento del RecyclerView.
         * Incluye validacion de contexto para evitar operaciones sobre contextos invalidos.
         *
         * @param context Contexto actual de la aplicacion
         * @param people Objeto [People] correspondiente al elemento seleccionado
         *
         * - Registro en consola de la seleccion
         * - Validacion de contexto antes de ejecutar acciones
         *
         * @throws Exception Los errores se capturan y registran sin propagarse
         */
        fun onClickListener(context: Context, people: People) {
            if (!isContextValid(context)) return // Validacion de contexto

            try {
                // Log de la seleccion para debugging y seguimiento
                println("Seleccionaste: ${people.name} y su id es ${people.id}")
            } catch (e: Exception) {
                e.printStackTrace() // Manejo de errores
            }
        }

        /**
         * Verifica si el contexto proporcionado sigue siendo valido para operaciones.
         *
         * Esta validacion es crucial para evitar crashes cuando se intenta realizar
         * operaciones sobre actividades que han sido destruidas o estan en proceso
         * de finalizacion.
         *
         * @param context Contexto a validar
         * @return `true` si el contexto es valido y seguro para usar, `false` en caso contrario
         *
         * **Validaciones especificas:**
         * - Para [DetallesActivity]: verifica que no este destruida ni finalizando
         * - Para otros contextos: asume que son validos (contexto de aplicacion, etc.)
         *
         * **Casos de uso:**
         * - Antes de operaciones asincronas
         * - Antes de actualizar UI
         * - Antes de navegar entre pantallas
         */
        private fun isContextValid(context: Context): Boolean {
            return when (context) {
                // Validacion especifica para DetallesActivity
                is DetallesActivity -> !context.isDestroyed && !context.isFinishing
                // Otros contextos se consideran validos por defecto
                else -> true
            }
        }
    }

    /**
     * ViewHolder personalizado que gestiona la vista individual de cada elemento de persona.
     *
     * Esta clase maneja la vinculacion de datos a las vistas, carga de imagenes,
     * aplicacion de estilos dinamicos y gestion de eventos para cada elemento
     * de la lista de personas.
     *
     * **Responsabilidades:**
     * - Binding de datos de [People] a las vistas del layout
     * - Carga y gestion de imagenes de perfil con Picasso
     * - Aplicacion de colores dinamicos basados en [ColorManager]
     * - Configuracion de listeners de eventos
     * - Limpieza de recursos para prevenir memory leaks
     *
     * @param itemView Vista raiz del elemento inflada desde item_people.xml
     */
    class PeopleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /**
         * Instancia de ViewBinding para acceso tipado y seguro a las vistas del layout.
         * Utiliza ItemPeopleBinding generado automaticamente desde item_people.xml
         */
        private val b = ItemPeopleBinding.bind(itemView)

        /**
         * Referencia al target de Picasso para gestion manual de solicitudes de imagen.
         * Permite cancelar solicitudes pendientes para evitar memory leaks cuando
         * el ViewHolder es reciclado.
         */
        private var picassoTarget: com.squareup.picasso.Target? = null

        /**
         * Vincula los datos de una persona a las vistas del ViewHolder.
         *
         * Este metodo configura todos los aspectos visuales del elemento:
         * colores, textos, imagenes y eventos. Incluye multiples validaciones
         * y manejo de casos edge para garantizar una experiencia robusta.
         *
         * @param people Objeto [People] con los datos a mostrar
         * @param onItemClick Funcion lambda para manejar clics en el elemento
         * @param isCleanedUp Flag que indica si el adaptador ha sido limpiado
         *
         * **Proceso de binding:**
         * 1. Validacion de estado de limpieza
         * 2. Limpieza de recursos previos (imagenes de Picasso)
         * 3. Configuracion de colores de fondo basados en ColorManager
         * 4. Carga asincrona de imagen de perfil con placeholder y manejo de errores
         * 5. Configuracion de colores de texto segun el tema (claro/oscuro)
         * 6. Asignacion de textos con fallbacks para datos faltantes
         * 7. Configuracion de informacion especifica de TV shows (episodios)
         * 8. Configuracion del listener de clic con validaciones de seguridad
         *
         * **Manejo de datos faltantes:**
         * - Nombre: "Nombre no disponible" si esta vacio o nulo
         * - Personaje/Rol: Prioriza 'character', luego 'roles', finalmente fallback
         * - Imagen: Placeholder durante carga, imagen de error si falla
         *
         * @throws Exception Todos los errores se capturan y registran sin propagarse
         */
        fun bind(people: People, onItemClick: (People) -> Unit, isCleanedUp: Boolean) {
            if (isCleanedUp) return // Previene operaciones sobre adaptador limpiado

            try {
                // Limpieza de recursos previos para evitar memory leaks
                cleanupPicassoTarget()

                // Configuracion de colores de fondo basados en ColorManager
                b.main.setBackgroundColor(ColorManager.averageColor) // Fondo principal
                b.fondoMain.setCardBackgroundColor(ColorManager.averageColor) // Fondo de la tarjeta

                // Carga de imagen de perfil con Picasso
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/original${people.profile_path}") // URL completa de TMDB
                    .resize(150, 225) // Redimension para optimizar memoria
                    .placeholder(R.drawable.media_carga) // Imagen durante la carga
                    .error(R.drawable.media_carga) // Imagen si falla la carga
                    .transform(RoundedCornersTransformation(12, 0)) // Esquinas redondeadas
                    .into(b.ivFoto) // Vista de destino

                // Configuracion de colores de texto segun tema
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK
                b.tvNombre.setTextColor(textColor) // Color del nombre
                b.tvCharacter.setTextColor(textColor) // Color del personaje/rol

                // Configuracion del nombre con fallback
                b.tvNombre.text = people.name?.takeIf { it.isNotEmpty() } ?: "Nombre no disponible"

                // Configuracion del personaje/rol con logica de prioridad
                when {
                    // Prioridad 1: Campo 'character' (para peliculas)
                    !people.character.isNullOrEmpty() -> {
                        b.tvCharacter.text = people.character
                    }
                    // Prioridad 2: Campo 'roles' (para series TV)
                    !people.roles.isNullOrEmpty() -> {
                        // Los roles pueden venir en formato "tipo;nombre" - extraemos el nombre
                        val rolesParts = people.roles.toString().split(";")
                        b.tvCharacter.text = if (rolesParts.size > 1) rolesParts[1] else people.roles.toString()
                    }
                    // Fallback: texto por defecto
                    else -> {
                        b.tvCharacter.text = "Rol no disponible"
                    }
                }

                // Configuracion de informacion de episodios (solo para series TV)
                b.tvCantEpisodios.visibility = if (!people.roles.isNullOrEmpty() &&
                    people.total_episode_count != null &&
                    people.total_episode_count > 0) {
                    // Mostrar contador de episodios para series
                    b.tvCantEpisodios.setTextColor(textColor)
                    b.tvCantEpisodios.text = "${people.total_episode_count} episodios"
                    View.VISIBLE
                } else {
                    // Ocultar para peliculas o cuando no hay datos
                    View.GONE
                }

                // Configuracion del listener de clic con validaciones de seguridad
                itemView.setOnClickListener {
                    if (!isCleanedUp) { // Validacion de estado de limpieza
                        try {
                            onItemClick(people) // Ejecutar callback proporcionado
                        } catch (e: Exception) {
                            e.printStackTrace() // Log de errores en callback
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace() // Log de errores generales del binding
            }
        }

        /**
         * Limpia de forma segura el target de Picasso para prevenir memory leaks.
         *
         * Este metodo cancela cualquier solicitud de imagen pendiente y libera
         * la referencia al target. Es importante llamarlo antes de reutilizar
         * el ViewHolder o cuando el adaptador se limpia.
         *
         * **Proceso de limpieza:**
         * 1. Verifica que existe un target activo
         * 2. Cancela la solicitud de imagen pendiente
         * 3. Libera la referencia al target
         * 4. Maneja errores silenciosamente para evitar crashes
         *
         * **Casos de uso:**
         * - Antes de cargar una nueva imagen en el mismo ViewHolder
         * - Durante la limpieza del adaptador
         * - Cuando el ViewHolder es reciclado por el RecyclerView
         */
        private fun cleanupPicassoTarget() {
            picassoTarget?.let { target ->
                try {
                    // Cancelar solicitud de imagen pendiente
                    Picasso.get().cancelRequest(target)
                } catch (e: Exception) {
                    // Error silencioso - no es critico si falla la cancelacion
                }
                // Liberar referencia al target
                picassoTarget = null
            }
        }
    }
}