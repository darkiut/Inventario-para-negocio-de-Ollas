package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.launch

data class ProductoState(
    val productos: List<Producto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProductoViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    var state by mutableStateOf(ProductoState())
        private set

    // Campos para agregar producto
    var nombre by mutableStateOf("")
        private set
    var codigo by mutableStateOf("")
        private set
    var stock by mutableStateOf("")
        private set
    var precioUnitario by mutableStateOf("")
        private set
    var categoria by mutableStateOf("")
        private set
    var descripcion by mutableStateOf("")
        private set

    // No cargar productos automáticamente en init
    // Se cargarán solo después de iniciar sesión

    fun onNombreChange(value: String) { nombre = value }
    fun onCodigoChange(value: String) { codigo = value }
    fun onStockChange(value: String) { stock = value }
    fun onPrecioChange(value: String) { precioUnitario = value }
    fun onCategoriaChange(value: String) { categoria = value }
    fun onDescripcionChange(value: String) { descripcion = value }

    // Función para inicializar productos manualmente
    fun inicializarProductos() {
        if (state.productos.isEmpty()) {
            cargarProductos()
        }
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            try {
                repository.obtenerProductos().collect { productos ->
                    state = state.copy(productos = productos, isLoading = false)
                }
            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    error = "Error al cargar productos: ${e.message}"
                )
            }
        }
    }

    fun agregarProducto(onSuccess: () -> Unit) {
        // Validaciones
        if (nombre.isBlank()) {
            state = state.copy(error = "El nombre es requerido")
            return
        }

        if (codigo.isBlank()) {
            state = state.copy(error = "El código es requerido")
            return
        }

        val stockInt = stock.toIntOrNull()
        if (stockInt == null || stockInt < 0) {
            state = state.copy(error = "Stock inválido")
            return
        }

        val precioDouble = precioUnitario.toDoubleOrNull()
        if (precioDouble == null || precioDouble <= 0) {
            state = state.copy(error = "Precio inválido")
            return
        }

        state = state.copy(isLoading = true, error = null)

        val producto = Producto(
            nombre = nombre,
            codigo = codigo,
            stock = stockInt,
            precioUnitario = precioDouble,
            categoria = categoria,
            descripcion = descripcion
        )

        viewModelScope.launch {
            val result = repository.agregarProducto(producto)

            result.onSuccess {
                state = state.copy(
                    isLoading = false,
                    successMessage = "Producto agregado correctamente"
                )
                limpiarCampos()
                onSuccess()
            }.onFailure { error ->
                state = state.copy(
                    isLoading = false,
                    error = error.message ?: "Error al agregar producto"
                )
            }
        }
    }


    fun limpiarCampos() {
        nombre = ""
        codigo = ""
        stock = ""
        precioUnitario = ""
        categoria = ""
        descripcion = ""
    }

    fun limpiarMensajes() {
        state = state.copy(error = null, successMessage = null)
    }

    fun actualizarStock(productoId: String, nuevoStock: Int) {
        viewModelScope.launch {
            repository.actualizarStock(productoId, nuevoStock)
        }
    }
    fun aumentarStock(productoId: String, cantidadAAgregar: Int) {
        viewModelScope.launch {
            // Obtener el producto actual
            val producto = state.productos.find { it.id == productoId }
            if (producto != null) {
                val nuevoStock = producto.stock + cantidadAAgregar
                repository.actualizarStock(productoId, nuevoStock)
            }
        }
    }


}
