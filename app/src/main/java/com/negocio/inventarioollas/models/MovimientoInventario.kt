package com.negocio.inventarioollas.models

data class MovimientoInventario(
    var id: String = "",
    var productoId: String = "",
    var productoNombre: String = "",
    var tipo: String = "entrada", // "entrada" o "salida"
    var cantidad: Int = 0,
    var usuarioId: String = "",
    var usuarioNombre: String = "",
    var fecha: Long = System.currentTimeMillis(),
    var motivo: String = "",
    var referencia: String = "" // ID de venta si es salida por venta
) {
    constructor() : this("", "", "", "entrada", 0, "", "", System.currentTimeMillis(), "", "")
}
