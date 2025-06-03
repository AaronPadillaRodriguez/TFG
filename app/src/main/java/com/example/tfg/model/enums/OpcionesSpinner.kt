package com.example.tfg.model.enums

/**
 * Opciones predefinidas para componentes Spinner en la interfaz de usuario.
 *
 * Esta enumeracion centraliza las opciones de filtrado disponibles en la aplicacion,
 * combinando el texto mostrado al usuario con los valores requeridos por la API de TMDB.
 * Facilita la configuracion de spinners de filtrado y garantiza consistencia entre
 * la UI y las llamadas a la API.
 *
 * @property texto Texto mostrado en la interfaz de usuario o valor usado en parametros de API.
 *                Para algunas opciones es el valor literal que se envia a TMDB.
 * @property opcion Identificador numerico unico para uso interno y logica de seleccion.
 *                 Permite identificar la opcion seleccionada independientemente del texto.
 *
 */
enum class OpcionesSpinner (val texto: String, val opcion: Int) {
    HOY("day", 0),
    ESTA_SEMANA("week", 1),
    RETRANSMISION("Retransmision", 2),
    EN_TELEVISION("En television", 3),
    EN_ALQUILER("En alquiler", 4),
    EN_CINES("En cines", 5),
    GRATIS_PELICULAS("movie", 6),
    GRATIS_TELEVISION("tv", 7),
}