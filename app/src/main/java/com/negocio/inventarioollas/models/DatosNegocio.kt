package com.negocio.inventarioollas.models

data class DatosNegocio(
    val nombre: String = "Mi Negocio",
    val ruc: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val mensajeFinal: String = "¡Gracias por su compra!"
)