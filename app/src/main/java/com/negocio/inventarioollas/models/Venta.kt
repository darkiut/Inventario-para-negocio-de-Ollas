package com.negocio.inventarioollas.models

data class Venta(
    var id: String = "",
    var vendedorId: String = "",
    var vendedorNombre: String = "",
    var fecha: Long = System.currentTimeMillis(),
    var total: Double = 0.0,
    var tipoDocumento: String = "boleta", // "boleta", "factura", "nota_pedido"
    var numeroDocumento: String = "",
    var clienteNombre: String = "",
    var clienteTelefono: String = "",
    var clienteDni: String = "",
    var clienteRuc: String = "",
    var clienteDireccion: String = "",
    var productos: Map<String, ItemVenta> = emptyMap(),
    var estado: String = "COMPLETADO" // "COMPLETADO", "CANCELADO"
) {
    constructor() : this(
        "", "", "", System.currentTimeMillis(), 0.0,
        "boleta", "", "", "", "", "", "", emptyMap(), "COMPLETADO"
    )

    // Método para calcular el total
    fun calcularTotal(): Double {
        return productos.values.sumOf { it.subtotal }
    }
}
