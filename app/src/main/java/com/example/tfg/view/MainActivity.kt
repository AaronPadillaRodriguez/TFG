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

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
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

        setupToolbarButtons()

        Trending(this, b)
        Popular(this, b)
        Gratis(this, b)

        val db = Firebase.firestore

        val user = hashMapOf(
            "email" to "paco@gmail.com",
            "username" to "paco",
            "password" to 1234,
        )


        //Introducir datos
        val idPersonalizado = "usuario001"
        db.collection("users")
            .document(idPersonalizado)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot added with ID: $idPersonalizado")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }


        //leer datos
        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }
    }

    private fun setupToolbarButtons() {
        b.btMenu.setOnClickListener {
            // Implementar acción del menú
        }

        b.btSesion.setOnClickListener {
            // Implementar acción del perfil
        }

        b.btBuscar.setOnClickListener {
            // Implementar acción de búsqueda
        }
    }
}