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
 * Adaptador para mostrar una lista de actores/personas en un RecyclerView.
 * Utiliza Picasso para cargar imagenes y maneja eventos de clic.
 *
 * @param onItemClick Funcion que se ejecuta al hacer clic en un elemento.
 */
class PeopleAdapter(private val onItemClick: (People) -> Unit) : RecyclerView.Adapter<PeopleAdapter.PeopleViewHolder>() {

    private val items = mutableListOf<People>()
    private var isCleanedUp = false

    /**
     * Actualiza la lista de elementos.
     *
     * @param newItems Nueva lista de personas a mostrar.
     */
    fun submitList(newItems: List<People>) {
        if (isCleanedUp) return

        try {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = if (isCleanedUp) 0 else items.size

    /**
     * Crea un nuevo ViewHolder para los elementos de la lista.
     *
     * @param parent El ViewGroup en el que se inflará la vista
     * @param viewType El tipo de vista (no utilizado en esta implementación)
     * @return Un nuevo PeopleViewHolder que contiene la vista del elemento
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeopleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_people, parent, false)
        return PeopleViewHolder(view)
    }

    /**
     * Vincula los datos del elemento en la posición especificada al ViewHolder.
     *
     * @param holder El PeopleViewHolder al que vincular los datos
     * @param position La posición del elemento en la lista
     */
    override fun onBindViewHolder(holder: PeopleViewHolder, position: Int) {
        if (isCleanedUp || position >= items.size) return

        try {
            holder.bind(items[position], onItemClick, isCleanedUp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Limpia el adaptador y libera recursos.
     */
    fun cleanup() {
        isCleanedUp = true
        items.clear()
        notifyDataSetChanged()
    }

    /**
     * Companion object con metodos estaticos para:
     * - Cargar datos de personas
     * - Manejar eventos de clic
     * - Validar contexto
     */
    companion object {
        /**
         * Carga datos de personas y actualiza el adaptador.
         *
         * @param context Contexto de la aplicacion.
         * @param peopleAdapter Adaptador a actualizar.
         * @param response Lista de personas a mostrar.
         */
        fun fetchPeople(context: Context, peopleAdapter: PeopleAdapter, response: List<People>) {
            // Verificar que el contexto sea válido
            if (!isContextValid(context) || peopleAdapter.isCleanedUp) return

            try {
                if (context is LifecycleOwner) {
                    context.lifecycleScope.launch {
                        if (!isContextValid(context) || peopleAdapter.isCleanedUp) return@launch

                        try {
                            if (response.isNullOrEmpty()) {
                                if (isContextValid(context)) {
                                    println("No hay contenido disponible.")
                                }
                            } else {
                                peopleAdapter.submitList(response)
                            }
                        } catch (e: Exception) {
                            if (isContextValid(context)) {
                                println("Error: ${e.message}")
                            }
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Maneja el clic en un elemento persona.
         *
         * @param context Contexto.
         * @param people Persona seleccionada.
         */
        fun onClickListener(context: Context, people: People) {
            if (!isContextValid(context)) return

            try {
                println("Seleccionaste: ${people.name} y su id es ${people.id}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Verifica si el contexto sigue siendo válido
         */
        private fun isContextValid(context: Context): Boolean {
            return when (context) {
                is DetallesActivity -> !context.isDestroyed && !context.isFinishing
                else -> true
            }
        }
    }

    /**
     * ViewHolder para mostrar elementos individuales de personas.
     */
    class PeopleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val b = ItemPeopleBinding.bind(itemView)
        private var picassoTarget: com.squareup.picasso.Target? = null

        /**
         * Enlaza datos de una persona a las vistas.
         *
         * @param people Persona a mostrar.
         * @param onItemClick Funcion al hacer clic.
         * @param isCleanedUp Flag de limpieza.
         */
        fun bind(people: People, onItemClick: (People) -> Unit, isCleanedUp: Boolean) {
            if (isCleanedUp) return

            try {
                // Limpiar el target anterior si existe
                cleanupPicassoTarget()

                // Configurar colores y fondos
                b.main.setBackgroundColor(ColorManager.averageColor)
                b.fondoMain.setCardBackgroundColor(ColorManager.averageColor)

                // Cargar imagen de forma segura
                Picasso.get()
                    .load("https://image.tmdb.org/t/p/original${people.profile_path}")
                    .resize(150, 225)
                    .placeholder(R.drawable.media_carga)
                    .error(R.drawable.media_carga)
                    .transform(RoundedCornersTransformation(12, 0))
                    .into(b.ivFoto)

                // Configurar colores de texto
                val textColor = if (ColorManager.isDark) Color.WHITE else Color.BLACK
                b.tvNombre.setTextColor(textColor)
                b.tvCharacter.setTextColor(textColor)

                // Configurar textos
                b.tvNombre.text = people.name?.takeIf { it.isNotEmpty() } ?: "Nombre no disponible"

                // Configurar personaje/rol
                when {
                    !people.character.isNullOrEmpty() -> {
                        b.tvCharacter.text = people.character
                    }
                    !people.roles.isNullOrEmpty() -> {
                        val rolesParts = people.roles.toString().split(";")
                        b.tvCharacter.text = if (rolesParts.size > 1) rolesParts[1] else people.roles.toString()
                    }
                    else -> {
                        b.tvCharacter.text = "Rol no disponible"
                    }
                }

                // Configurar episodios (solo para TV shows)
                b.tvCantEpisodios.visibility = if (!people.roles.isNullOrEmpty() && people.total_episode_count != null && people.total_episode_count > 0) {
                    b.tvCantEpisodios.setTextColor(textColor)
                    b.tvCantEpisodios.text = "${people.total_episode_count} episodios"
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Configurar listener de clic de forma segura
                itemView.setOnClickListener {
                    if (!isCleanedUp) {
                        try {
                            onItemClick(people)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Limpia el target de Picasso de forma segura
         */
        private fun cleanupPicassoTarget() {
            picassoTarget?.let {
                try {
                    Picasso.get().cancelRequest(it)
                } catch (e: Exception) {
                    // Error silencioso
                }
                picassoTarget = null
            }
        }
    }
}