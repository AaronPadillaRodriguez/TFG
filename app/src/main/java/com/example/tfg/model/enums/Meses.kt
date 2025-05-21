package com.example.tfg.model.enums

/**
 * Enumeración que representa los meses del año.
 *
 * Esta clase enum proporciona una representación estructurada de los doce meses del año
 * con sus respectivos números ordinales, abreviaturas y nombres completos.
 *
 * @property numMes Número ordinal del mes (1-12).
 * @property abreviatura Representación abreviada del nombre del mes (3 caracteres).
 * @property completo Nombre completo del mes en español.
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
         * Obtiene un mes a partir de su número ordinal.
         *
         * @param numMes El número del mes (1-12) que se quiere obtener.
         * @return El valor del enum [Meses] correspondiente al número proporcionado,
         * o null si no existe un mes con ese número.
         */
        fun fromNumero(numMes: Int): Meses? {
            return entries.find { it.numMes == numMes }
        }
    }
}