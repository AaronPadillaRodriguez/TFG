package com.example.tfg.model.enums

/**
 * Enumeracion de meses del año con utilidades de conversion.
 *
 * @property numMes Numero del mes (1-12)
 * @property abreviatura Abreviatura de 3 letras (ej: "Ene")
 * @property completo Nombre completo del mes
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
         * Obtiene el mes correspondiente a un numero.
         *
         * @param numMes Numero del mes (1-12)
         * @return Mes correspondiente o null si no existe
         */
        fun fromNumero(numMes: Int): Meses? {
            return entries.find { it.numMes == numMes }
        }
    }
}