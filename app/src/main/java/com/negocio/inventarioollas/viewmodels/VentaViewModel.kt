package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.DatosNegocio
import com.negocio.inventarioollas.models.ItemVenta
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Venta
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class VentaState(
    val productosDisponibles: List<Producto> = emptyList(),
    val productosFiltrados: List<Producto> = emptyList(), // ✅ NUEVO
    val carrito: MutableMap<String, ItemVenta> = mutableMapOf(),
    val total: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val clienteNombre: String = "",
    val clienteTelefono: String = "",
    val clienteDni: String = "",
    val tipoDocumento: String = "Boleta",
    val ventaExitosa: Venta? = null,
    val datosNegocio: DatosNegocio = DatosNegocio(),
    val categorias: List<String> = emptyList() // ✅ NUEVO
)

class VentaViewModel : ViewModel() {
    private val repository = FirebaseRepository

    var state by mutableStateOf(VentaState())
        private set

    // ✅ NUEVO - Variables de búsqueda y filtro
    var searchQuery by mutableStateOf("")
    var categoriaSeleccionada by mutableStateOf<String?>(null)
    private var searchJob: Job? = null

    init {
        cargarProductos()
        cargarDatosNegocio()
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            repository.obtenerProductos().collect { productos ->
                val productosConStock = productos.filter { it.stock > 0 }

                // ✅ NUEVO - Extraer categorías únicas
                val categoriasUnicas = productosConStock
                    .map { it.categoria }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()

                state = state.copy(
                    productosDisponibles = productosConStock,
                    productosFiltrados = productosConStock, // ✅ Inicialmente mostrar todos
                    categorias = categoriasUnicas,
                    isLoading = false
                )
            }
        }
    }

    // ✅ NUEVO - Búsqueda con debounce
    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(300)
            aplicarFiltros()
        }
    }

    // ✅ NUEVO - Filtrar por categoría
    fun filtrarPorCategoria(categoria: String?) {
        categoriaSeleccionada = categoria
        aplicarFiltros()
    }

    // ✅ NUEVO - Aplicar ambos filtros
    private fun aplicarFiltros() {
        var productosFiltrados = state.productosDisponibles

        // Filtro por categoría
        if (categoriaSeleccionada != null) {
            productosFiltrados = productosFiltrados.filter {
                it.categoria.equals(categoriaSeleccionada, ignoreCase = true)
            }
        }

        // Filtro por búsqueda
        if (searchQuery.isNotBlank()) {
            productosFiltrados = productosFiltrados.filter {
                it.nombre.contains(searchQuery, ignoreCase = true) ||
                        it.codigo.contains(searchQuery, ignoreCase = true) ||
                        it.categoria.contains(searchQuery, ignoreCase = true)
            }
        }

        state = state.copy(productosFiltrados = productosFiltrados)
    }

    private fun cargarDatosNegocio() {
        viewModelScope.launch {
            repository.obtenerDatosNegocio().onSuccess { datos ->
                state = state.copy(datosNegocio = datos)
            }
        }
    }

    fun agregarProductoAlCarrito(producto: Producto, cantidad: Int) {
        if (cantidad <= 0) {
            state = state.copy(error = "La cantidad debe ser mayor a 0")
            return
        }

        val carritoActual = state.carrito.toMutableMap()
        val itemExistente = carritoActual[producto.id]
        val cantidadActual = itemExistente?.cantidad ?: 0
        val cantidadTotal = cantidadActual + cantidad

        if (cantidadTotal > producto.stock) {
            state = state.copy(error = "Stock insuficiente. Solo quedan ${producto.stock} unidades")
            return
        }

        if (itemExistente != null) {
            carritoActual[producto.id] = itemExistente.copy(
                cantidad = cantidadTotal,
                subtotal = cantidadTotal * producto.precioUnitario
            )
        } else {
            carritoActual[producto.id] = ItemVenta(
                productoId = producto.id,
                nombre = producto.nombre,
                cantidad = cantidad,
                precioUnitario = producto.precioUnitario,
                subtotal = cantidad * producto.precioUnitario
            )
        }

        actualizarEstadoCarrito(carritoActual)
    }

    fun quitarProductoDelCarrito(productoId: String) {
        val carritoActual = state.carrito.toMutableMap()
        carritoActual.remove(productoId)
        actualizarEstadoCarrito(carritoActual)
    }

    fun limpiarCarrito() {
        actualizarEstadoCarrito(mutableMapOf())
        state = state.copy(
            clienteNombre = "",
            clienteTelefono = "",
            clienteDni = "",
            error = null,
            successMessage = null
        )
    }

    fun limpiarVentaExitosa() {
        state = state.copy(ventaExitosa = null)
    }

    private fun actualizarEstadoCarrito(carrito: MutableMap<String, ItemVenta>) {
        val nuevoTotal = carrito.values.sumOf { it.subtotal }
        state = state.copy(carrito = carrito, total = nuevoTotal, error = null)
    }

    fun actualizarDatosCliente(nombre: String, telefono: String, dni: String) {
        state = state.copy(
            clienteNombre = nombre,
            clienteTelefono = telefono,
            clienteDni = dni
        )
    }

    fun cambiarTipoDocumento(tipo: String) {
        state = state.copy(tipoDocumento = tipo)
    }

    fun registrarVenta(vendedorId: String, vendedorNombre: String) {
        if (state.carrito.isEmpty()) {
            state = state.copy(error = "El carrito está vacío")
            return
        }

        if (state.clienteNombre.isBlank()) {
            state = state.copy(error = "Ingresa el nombre del cliente")
            return
        }

        val productosInsuficientes = mutableListOf<String>()
        state.carrito.values.forEach { item ->
            val producto = state.productosDisponibles.find { it.id == item.productoId }
            if (producto == null || producto.stock < item.cantidad) {
                productosInsuficientes.add(item.nombre)
            }
        }

        if (productosInsuficientes.isNotEmpty()) {
            state = state.copy(
                error = "Stock insuficiente para: ${productosInsuficientes.joinToString(", ")}"
            )
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            val venta = Venta(
                vendedorId = vendedorId,
                vendedorNombre = vendedorNombre,
                total = state.total,
                clienteNombre = state.clienteNombre,
                clienteTelefono = state.clienteTelefono,
                clienteDni = state.clienteDni,
                tipoDocumento = state.tipoDocumento,
                productos = state.carrito
            )

            repository.registrarVenta(venta)
                .onSuccess { ventaId ->
                    state = state.copy(
                        isLoading = false,
                        successMessage = "Venta registrada con éxito",
                        ventaExitosa = venta.copy(id = ventaId)
                    )
                    limpiarCarrito()
                }
                .onFailure { e ->
                    state = state.copy(
                        isLoading = false,
                        error = "Error al registrar venta: ${e.message}"
                    )
                }
        }
    }
}
