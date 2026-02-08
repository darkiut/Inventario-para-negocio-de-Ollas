package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ProductoState(
    val productos: List<Producto> = emptyList(),
    val productosFiltrados: List<Producto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProductoViewModel : ViewModel() {
    // ✅ Ahora usa el singleton
    private val repository = FirebaseRepository

    var state by mutableStateOf(ProductoState())
        private set

    var searchQuery by mutableStateOf("")
    private var searchJob: Job? = null // ✅ Para debounce

    // Variables del formulario
    var nombre by mutableStateOf("")
    var codigo by mutableStateOf("")
    var stock by mutableStateOf("")
    var precioUnitario by mutableStateOf("")
    var categoria by mutableStateOf("")
    var descripcion by mutableStateOf("")

    var productoAEditar: Producto? = null

    fun inicializarProductos() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            repository.obtenerProductos().collect { lista ->
                state = state.copy(
                    productos = lista,
                    productosFiltrados = lista,
                    isLoading = false
                )
                if (searchQuery.isNotEmpty()) onSearchQueryChange(searchQuery)
            }
        }
    }

    // ✅ BÚSQUEDA CON DEBOUNCE (espera 300ms antes de buscar)
    fun onSearchQueryChange(query: String) {
        searchQuery = query
        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(300) // Esperar 300ms

            if (query.isBlank()) {
                state = state.copy(productosFiltrados = state.productos)
            } else {
                val filtrados = state.productos.filter {
                    it.nombre.contains(query, ignoreCase = true) ||
                            it.codigo.contains(query, ignoreCase = true) ||
                            it.categoria.contains(query, ignoreCase = true)
                }
                state = state.copy(productosFiltrados = filtrados)
            }
        }
    }

    fun seleccionarProductoParaEditar(producto: Producto) {
        productoAEditar = producto
        nombre = producto.nombre
        codigo = producto.codigo
        stock = producto.stock.toString()
        precioUnitario = producto.precioUnitario.toString()
        categoria = producto.categoria
        descripcion = producto.descripcion
    }

    fun limpiarFormulario() {
        productoAEditar = null
        nombre = ""
        codigo = ""
        stock = ""
        precioUnitario = ""
        categoria = ""
        descripcion = ""
        state = state.copy(error = null, successMessage = null)
    }

    fun guardarProducto(onSuccess: () -> Unit) {
        val nombreVal = nombre.trim()
        val codigoVal = codigo.trim()
        val stockVal = stock.toIntOrNull()
        val precioVal = precioUnitario.toDoubleOrNull()

        // ✅ VALIDACIONES MEJORADAS
        if (nombreVal.isEmpty()) {
            state = state.copy(error = "El nombre es obligatorio")
            return
        }

        if (codigoVal.isEmpty()) {
            state = state.copy(error = "El código es obligatorio")
            return
        }

        if (stockVal == null || stockVal < 0) {
            state = state.copy(error = "Stock inválido (debe ser un número positivo)")
            return
        }

        if (precioVal == null || precioVal <= 0) {
            state = state.copy(error = "Precio inválido (debe ser mayor a 0)")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            val resultado = if (productoAEditar == null) {
                val nuevoProducto = Producto(
                    nombre = nombreVal,
                    codigo = codigoVal,
                    stock = stockVal,
                    precioUnitario = precioVal,
                    categoria = categoria,
                    descripcion = descripcion
                )
                repository.agregarProducto(nuevoProducto)
            } else {
                val productoActualizado = productoAEditar!!.copy(
                    nombre = nombreVal,
                    codigo = codigoVal,
                    stock = stockVal,
                    precioUnitario = precioVal,
                    categoria = categoria,
                    descripcion = descripcion
                )
                repository.actualizarProducto(productoActualizado)
            }

            if (resultado.isSuccess) {
                state = state.copy(
                    isLoading = false,
                    successMessage = "Producto guardado correctamente"
                )
                limpiarFormulario()
                onSuccess()
            } else {
                state = state.copy(
                    isLoading = false,
                    error = "Error: ${resultado.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun aumentarStock(productoId: String, cantidad: Int) {
        viewModelScope.launch {
            repository.aumentarStock(productoId, cantidad)
        }
    }

    fun eliminarProducto(productoId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.eliminarProducto(productoId)
            if (result.isSuccess) onSuccess()
        }
    }

    fun cancelarEdicion() {
        limpiarFormulario()
    }
}
