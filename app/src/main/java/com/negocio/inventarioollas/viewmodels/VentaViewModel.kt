package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.ItemVenta
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Venta
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.launch

data class VentaState(
    val productos: List<Producto> = emptyList(),
    val carrito: Map<String, ItemVenta> = emptyMap(),
    val total: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val ventaRegistrada: Boolean = false
)

class VentaViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    var state by mutableStateOf(VentaState())
        private set

    // Datos del cliente
    // Variables de estado - UNA SOLA VEZ cada una
    var clienteNombre by mutableStateOf("")
        private set
    var clienteTelefono by mutableStateOf("")
        private set
    var clienteDni by mutableStateOf("")
        private set
    var clienteRuc by mutableStateOf("")
        private set
    var clienteDireccion by mutableStateOf("")
        private set
    var tipoDocumento by mutableStateOf("boleta")
        private set

    // Funciones onChange - UNA SOLA VEZ cada una
    fun onClienteNombreChange(value: String) { clienteNombre = value }
    fun onClienteTelefonoChange(value: String) { clienteTelefono = value }
    fun onClienteDniChange(value: String) { clienteDni = value }
    fun onClienteRucChange(value: String) { clienteRuc = value }
    fun onClienteDireccionChange(value: String) { clienteDireccion = value }
    fun onTipoDocumentoChange(value: String) { tipoDocumento = value }


    init {
        cargarProductos()
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            try {
                repository.obtenerProductos().collect { productos ->
                    state = state.copy(productos = productos)
                }
            } catch (e: Exception) {
                state = state.copy(error = "Error al cargar productos: ${e.message}")
            }
        }
    }


    fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        if (cantidad <= 0 || cantidad > producto.stock) {
            state = state.copy(error = "Cantidad inválida o stock insuficiente")
            return
        }

        val carritoActual = state.carrito.toMutableMap()
        val itemExistente = carritoActual[producto.id]

        val nuevaCantidad = if (itemExistente != null) {
            itemExistente.cantidad + cantidad
        } else {
            cantidad
        }

        if (nuevaCantidad > producto.stock) {
            state = state.copy(error = "No hay suficiente stock disponible")
            return
        }

        val item = ItemVenta(
            productoId = producto.id,
            nombre = producto.nombre,
            cantidad = nuevaCantidad,
            precioUnitario = producto.precioUnitario,
            subtotal = nuevaCantidad * producto.precioUnitario
        )

        carritoActual[producto.id] = item

        val nuevoTotal = carritoActual.values.sumOf { it.subtotal }

        state = state.copy(
            carrito = carritoActual,
            total = nuevoTotal,
            error = null
        )
    }

    fun actualizarCantidad(productoId: String, nuevaCantidad: Int) {
        if (nuevaCantidad <= 0) {
            eliminarDelCarrito(productoId)
            return
        }

        val producto = state.productos.find { it.id == productoId } ?: return

        if (nuevaCantidad > producto.stock) {
            state = state.copy(error = "Stock insuficiente")
            return
        }

        val carritoActual = state.carrito.toMutableMap()
        val item = carritoActual[productoId] ?: return

        val itemActualizado = item.copy(
            cantidad = nuevaCantidad,
            subtotal = nuevaCantidad * item.precioUnitario
        )

        carritoActual[productoId] = itemActualizado

        val nuevoTotal = carritoActual.values.sumOf { it.subtotal }

        state = state.copy(
            carrito = carritoActual,
            total = nuevoTotal,
            error = null
        )
    }

    fun eliminarDelCarrito(productoId: String) {
        val carritoActual = state.carrito.toMutableMap()
        carritoActual.remove(productoId)

        val nuevoTotal = carritoActual.values.sumOf { it.subtotal }

        state = state.copy(
            carrito = carritoActual,
            total = nuevoTotal
        )
    }

    fun registrarVenta(vendedorId: String, vendedorNombre: String, onSuccess: (Venta) -> Unit) {
        if (state.carrito.isEmpty()) {
            state = state.copy(error = "El carrito está vacío")
            return
        }

        if (clienteNombre.isBlank()) {
            state = state.copy(error = "El nombre del cliente es requerido")
            return
        }

        if (clienteTelefono.isBlank()) {
            state = state.copy(error = "El teléfono del cliente es requerido")
            return
        }

        // Validaciones específicas para factura
        if (tipoDocumento == "factura") {
            if (clienteRuc.isBlank()) {
                state = state.copy(error = "El RUC es requerido para factura")
                return
            }
            if (clienteDireccion.isBlank()) {
                state = state.copy(error = "La dirección es requerida para factura")
                return
            }
        }

        state = state.copy(isLoading = true, error = null)

        val totalVenta = state.total

        val venta = Venta(
            vendedorId = vendedorId,
            vendedorNombre = vendedorNombre,
            total = totalVenta,
            tipoDocumento = tipoDocumento,
            clienteNombre = clienteNombre,
            clienteTelefono = clienteTelefono,
            clienteDni = clienteDni,
            clienteRuc = clienteRuc,
            clienteDireccion = clienteDireccion,
            productos = state.carrito
        )

        viewModelScope.launch {
            val result = repository.registrarVenta(venta)

            result.onSuccess { ventaId ->
                state.carrito.forEach { (productoId, item) ->
                    val producto = state.productos.find { it.id == productoId }
                    if (producto != null) {
                        val nuevoStock = producto.stock - item.cantidad
                        repository.actualizarStock(productoId, nuevoStock)
                    }
                }

                venta.id = ventaId

                state = state.copy(
                    isLoading = false,
                    ventaRegistrada = true,
                    total = totalVenta
                )

                onSuccess(venta)
            }.onFailure { error ->
                state = state.copy(
                    isLoading = false,
                    error = error.message ?: "Error al registrar la venta"
                )
            }
        }
    }



    fun limpiarVenta() {
        state = VentaState()
        clienteNombre = ""
        clienteTelefono = ""
        clienteDni = ""
        clienteRuc = ""
        clienteDireccion = ""
        tipoDocumento = "boleta"
        cargarProductos()
    }


    fun resetVentaRegistrada() {
        state = state.copy(ventaRegistrada = false)
    }

    fun limpiarError() {
        state = state.copy(error = null)
    }
}
