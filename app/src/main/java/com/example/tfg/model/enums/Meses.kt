package com.example.tfg.model.enums

/**
 * Enumeracion de los meses del año con utilidades de conversion y localizacion.
 *
 * Esta enumeracion proporciona una representacion completa de los meses del año
 * con multiples formatos de presentacion, facilitando su uso en interfaces de usuario,
 * filtros de fecha y operaciones de conversion. Especialmente util para mostrar
 * fechas localizadas en español y para operaciones de parsing de fechas.
 *
 * @property numMes Numero ordinal del mes en el calendario gregoriano (1-12).
 * @property abreviatura Forma abreviada del mes con 3 letras para interfaces compactas.
 * @property completo Nombre completo del mes para uso en interfaces detalladas.
 */
enum class Meses (val numMes: Int, val abreviatura: String, val completo: String) {
    ENERO(1, "Ene", "Enero"),
    FEBRERO(2, "Feb", "Febrero"),
    MARZO(3, "Mar", "Marzo"),
    ABRIL(4, "Abr", "Abril"),
    MAYO(5, "May", "Mayo"),
    JUNIO(6, "Jun", "Junio"),
    JULIO(7, "Jul", "Julio"),
    AGOSTO(8, "Ago", "Agosto"),
    SEPTIEMBRE(9, "Sep", "Septiembre"),
    OCTUBRE(10, "Oct", "Octubre"),
    NOVIEMBRE(11, "Nov", "Noviembre"),
    DICIEMBRE(12, "Dic", "Diciembre");

    companion object {
        /**
         * Obtiene la instancia del mes correspondiente a un numero dado.
         *
         * Metodo de conveniencia para convertir numeros de mes (como los obtenidos
         * de fechas ISO o Calendar) a la representacion enum correspondiente.
         * util para parsear fechas de la API de TMDB y mostrarlas localizadas.
         *
         * @param numMes Numero del mes en el rango 1-12 (enero=1, diciembre=12).
         * @return La instancia de [Meses] correspondiente al numero dado,
         *         o null si el numero esta fuera del rango valido.
         *
         * @throws IllegalArgumentException implicitamente si numMes < 1 || numMes > 12
         */
        fun fromNumero(numMes: Int): Meses? {
            return entries.find { it.numMes == numMes }
        }
    }
}