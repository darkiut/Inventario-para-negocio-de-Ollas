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
    val productosFiltrados: List<Producto> = emptyList(), // LISTA PARA MOSTRAR
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProductoViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    var state by mutableStateOf(ProductoState())
        private set

    // Variable para el texto del buscador
    var searchQuery by mutableStateOf("")

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
                    productosFiltrados = lista, // Al inicio mostramos todo
                    isLoading = false
                )
                // Si había una búsqueda activa, volvemos a filtrar
                if (searchQuery.isNotEmpty()) onSearchQueryChange(searchQuery)
            }
        }
    }

    // --- NUEVA FUNCIÓN DE BÚSQUEDA ---
    fun onSearchQueryChange(query: String) {
        searchQuery = query
        if (query.isBlank()) {
            state = state.copy(productosFiltrados = state.productos)
        } else {
            val filtrados = state.productos.filter {
                it.nombre.contains(query, ignoreCase = true) ||
                        it.codigo.contains(query, ignoreCase = true)
            }
            state = state.copy(productosFiltrados = filtrados)
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

        if (nombreVal.isEmpty() || codigoVal.isEmpty() || stockVal == null || precioVal == null) {
            state = state.copy(error = "Por favor completa todos los campos correctamente")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)

            val resultado = if (productoAEditar == null) {
                val nuevoProducto = Producto(
                    nombre = nombreVal, codigo = codigoVal, stock = stockVal,
                    precioUnitario = precioVal, categoria = categoria, descripcion = descripcion
                )
                repository.agregarProducto(nuevoProducto)
            } else {
                val productoActualizado = productoAEditar!!.copy(
                    nombre = nombreVal, codigo = codigoVal, stock = stockVal,
                    precioUnitario = precioVal, categoria = categoria, descripcion = descripcion
                )
                repository.actualizarProducto(productoActualizado)
            }

            if (resultado.isSuccess) {
                state = state.copy(isLoading = false, successMessage = "Producto guardado correctamente")
                limpiarFormulario()
                onSuccess()
            } else {
                state = state.copy(isLoading = false, error = "Error: ${resultado.exceptionOrNull()?.message}")
            }
        }
    }

    fun aumentarStock(productoId: String, cantidad: Int) {
        viewModelScope.launch { repository.aumentarStock(productoId, cantidad) }
    }

    fun eliminarProducto(productoId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.eliminarProducto(productoId)
            if (result.isSuccess) onSuccess()
        }
    }

    fun cancelarEdicion() { limpiarFormulario() }
}