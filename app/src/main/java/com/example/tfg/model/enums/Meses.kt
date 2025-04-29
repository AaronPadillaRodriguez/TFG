package com.example.tfg.model.enums

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
        fun fromNumero(numMes: Int): Meses? {
            return entries.find { it.numMes == numMes }
        }
    }
}