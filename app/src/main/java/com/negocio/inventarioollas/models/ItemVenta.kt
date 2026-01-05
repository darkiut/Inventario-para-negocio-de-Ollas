package com.negocio.inventarioollas.models

data class ItemVenta(
    var productoId: String = "",
    var nombre: String = "",
    var cantidad: Int = 0,
    var precio: Double = 0.0,
    var subtotal: Double = 0.0
) {
    constructor() : this("", "", 0, 0.0, 0.0)

    // Método para calcular subtotal
    fun calcularSubtotal(): Double {
        return cantidad * precio
    }
}
