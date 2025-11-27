package com.negocio.inventarioollas.models

data class Producto(
    var id: String = "",
    var nombre: String = "",
    var codigo: String = "",
    var stock: Int = 0,
    var precioUnitario: Double = 0.0,
    var categoria: String = "",
    var descripcion: String = "",
    var fechaIngreso: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", 0, 0.0, "", "", System.currentTimeMillis())
}
