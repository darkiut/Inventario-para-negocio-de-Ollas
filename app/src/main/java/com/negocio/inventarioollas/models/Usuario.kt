package com.negocio.inventarioollas.models

data class Usuario(
    var id: String = "",
    var nombre: String = "",
    var email: String = "",
    var rol: String = "vendedor", // "vendedor" o "dueno"
    var activo: Boolean = true,
    var fechaRegistro: Long = System.currentTimeMillis()
) {
    // Constructor vacío requerido por Firebase
    constructor() : this("", "", "", "vendedor", true, System.currentTimeMillis())
}
