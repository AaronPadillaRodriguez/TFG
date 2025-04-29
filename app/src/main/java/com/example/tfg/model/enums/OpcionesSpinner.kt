package com.example.tfg.model.enums

enum class OpcionesSpinner (val texto: String, val opcion: Int) {
    HOY("day", 0),
    ESTA_SEMANA("week", 1),
    RETRANSMISION("Retransmisión", 2),
    EN_TELEVISION("En televisión", 3),
    EN_ALQUILER("En alquiler", 4),
    EN_CINES("En cines", 5),
    GRATIS_PELICULAS("movie", 6),
    GRATIS_TELEVISION("tv", 7)
}