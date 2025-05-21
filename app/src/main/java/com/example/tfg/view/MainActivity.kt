package com.example.tfg.view

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tfg.databinding.ActivityMainBinding
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp

/**
 * Actividad principal de la aplicación que muestra diferentes secciones de contenido multimedia
 * y maneja la configuración inicial de Firebase y la interfaz de usuario.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    /**
     * Método llamado cuando se crea la actividad.
     * Inicializa Firebase, configura la interfaz de usuario y realiza operaciones en Firestore.
     *
     * @param savedInstanceState Si la actividad está siendo reinicializada después de
     * haber sido cerrada anteriormente, este Bundle contiene los datos que suministró
     * más recientemente en onSaveInstanceState(Bundle). De lo contrario, es nulo.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //FirebaseApp.initializeApp(this)

        b = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)

        ViewCompat.setOnApplyWindowInsetsListener(b.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configura los botones de la toolbar
        setupToolbarButtons()

        // Inicializa las secciones de contenido multimedia
        Trending(this, b)
        Popular(this, b)
        Gratis(this, b)

//        // Obtiene una instancia de Firestore
//        val db = Firebase.firestore
//
//        // Define un usuario para ser guardado en Firestore
//        val user = hashMapOf(
//            "email" to "paco@gmail.com",
//            "username" to "paco",
//            "password" to 1234,
//        )
//
//        // Introduce datos de usuario en Firestore con un ID personalizado
//        val idPersonalizado = "usuario001"
//        db.collection("users")
//            .document(idPersonalizado)
//            .set(user)
//            .addOnSuccessListener {
//                Log.d(TAG, "DocumentSnapshot added with ID: $idPersonalizado")
//            }
//            .addOnFailureListener { e ->
//                Log.w(TAG, "Error adding document", e)
//            }
//
//        // Lee todos los documentos de la colección "users" y los muestra en el log
//        db.collection("users")
//            .get()
//            .addOnSuccessListener { result ->
//                for (document in result) {
//                    Log.d(TAG, "${document.id} => ${document.data}")
//                }
//            }
//            .addOnFailureListener { exception ->
//                Log.w(TAG, "Error getting documents.", exception)
//            }
    }

    /**
     * Configura los listeners para los botones de la toolbar.
     * Define las acciones a realizar cuando los botones de menú, sesión y búsqueda son pulsados.
     */
    private fun setupToolbarButtons() {
        // Configura el listener para el botón de menú
        b.btMenu.setOnClickListener {
            // Implementar acción del menú
        }

        // Configura el listener para el botón de sesión/perfil
        b.btSesion.setOnClickListener {
            // Implementar acción del perfil
        }

        // Configura el listener para el botón de búsqueda
        b.btBuscar.setOnClickListener {
            // Implementar acción de búsqueda
        }
    }
}