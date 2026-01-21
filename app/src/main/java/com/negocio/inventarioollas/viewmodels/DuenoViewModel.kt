package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Venta
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DuenoState(
    val productos: List<Producto> = emptyList(),
    val productosFiltrados: List<Producto> = emptyList(), // LISTA PARA MOSTRAR
    val ventas: List<Venta> = emptyList(),
    val ventasFiltradas: List<Venta> = emptyList(),
    val totalVentasHoy: Double = 0.0,
    val totalVentas: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val ventasEliminadas: Int = 0,
    val diasConVentas: List<String> = emptyList(),
    val fechaSeleccionada: String? = null
)

class DuenoViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    var state by mutableStateOf(DuenoState())
        private set

    var filtroVendedor: String? = null
    var filtroFecha: String? = null

    // Variable para el buscador del Dueño
    var searchQuery by mutableStateOf("")

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)

            // 1. Cargar Productos
            launch {
                repository.obtenerProductos().collect { productos ->
                    state = state.copy(
                        productos = productos,
                        productosFiltrados = productos // Inicialmente todo
                    )
                    if (searchQuery.isNotEmpty()) onSearchQueryChange(searchQuery)
                }
            }

            // 2. Cargar Ventas
            launch {
                repository.obtenerVentas().collect { ventas ->
                    procesarVentas(ventas)
                }
            }
        }
    }

    // --- FUNCIÓN DE BÚSQUEDA DEL DUEÑO ---
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

    private fun procesarVentas(todasLasVentas: List<Venta>) {
        val fechaLimite = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
        val ventasRecientes = todasLasVentas.filter { it.fecha >= fechaLimite }
        val ventasAntiguasCount = todasLasVentas.size - ventasRecientes.size

        var totalHoy = 0.0
        var totalGeneral = 0.0
        val diasSet = sortedSetOf<String>(reverseOrder())
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val hoyStr = sdf.format(Date())

        ventasRecientes.forEach { venta ->
            totalGeneral += venta.total
            val fechaVenta = sdf.format(Date(venta.fecha))
            diasSet.add(fechaVenta)
            if (fechaVenta == hoyStr) totalHoy += venta.total
        }

        if (filtroFecha == null) filtroFecha = hoyStr

        state = state.copy(
            ventas = ventasRecientes,
            totalVentas = totalGeneral,
            totalVentasHoy = totalHoy,
            ventasEliminadas = ventasAntiguasCount,
            diasConVentas = diasSet.toList(),
            isLoading = false
        )
        aplicarFiltros()
    }

    fun filtrarPorFecha(fecha: String?) {
        filtroFecha = fecha
        aplicarFiltros()
    }

    fun filtrarPorVendedor(vendedorId: String?) {
        filtroVendedor = vendedorId
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        var lista = state.ventas
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        if (filtroFecha != null) {
            lista = lista.filter { sdf.format(Date(it.fecha)) == filtroFecha }
        }
        if (filtroVendedor != null) {
            lista = lista.filter { it.vendedorId == filtroVendedor }
        }

        state = state.copy(ventasFiltradas = lista, fechaSeleccionada = filtroFecha)
    }

    fun aumentarStock(productoId: String, cantidad: Int) {
        viewModelScope.launch { repository.aumentarStock(productoId, cantidad) }
    }

    fun anularVenta(ventaId: String) {
        viewModelScope.launch {
            repository.actualizarEstadoVenta(ventaId, "CANCELADO")
        }
    }

    fun actualizarVenta(venta: Venta) {
        viewModelScope.launch {
            repository.actualizarVentaProductos(venta.id, venta.productos, venta.total)
        }
    }
}