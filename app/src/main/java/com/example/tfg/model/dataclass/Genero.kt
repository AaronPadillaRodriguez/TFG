package com.example.tfg.model.dataclass

data class Genero(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name
}
