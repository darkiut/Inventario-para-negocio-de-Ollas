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
import java.util.*

data class DuenoState(
    val ventas: List<Venta> = emptyList(),
    val productos: List<Producto> = emptyList(),
    val ventasFiltradas: List<Venta> = emptyList(),
    val ventasDelDia: List<Venta> = emptyList(), // NUEVO
    val totalVentas: Double = 0.0,
    val totalVentasHoy: Double = 0.0,
    val diasConVentas: List<String> = emptyList(), // NUEVO - Lista de fechas con ventas
    val fechaSeleccionada: String? = null, // NUEVO - Fecha seleccionada para filtrar
    val isLoading: Boolean = true,
    val ventasEliminadas: Int = 0 // NUEVO - Contador de ventas eliminadas
)

class DuenoViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    var state by mutableStateOf(DuenoState())
        private set

    var filtroVendedor by mutableStateOf<String?>(null)
        private set

    fun cargarDatos() {
        cargarVentas()
        cargarProductos()
        eliminarVentasAntiguas()
    }

    private fun cargarVentas() {
        viewModelScope.launch {
            repository.obtenerVentasUltimoMes().collect { ventas ->
                val ahora = Calendar.getInstance()
                val hoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val ventasHoy = ventas.filter { venta ->
                    val ventaCalendar = Calendar.getInstance().apply {
                        timeInMillis = venta.fecha
                    }
                    ventaCalendar.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                            ventaCalendar.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)
                }

                val totalHoy = ventasHoy.sumOf { it.total }
                val totalGeneral = ventas.sumOf { it.total }

                // Obtener días únicos con ventas
                val diasConVentas = ventas.map { venta ->
                    val cal = Calendar.getInstance().apply { timeInMillis = venta.fecha }
                    "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
                }.distinct().sorted()

                state = state.copy(
                    ventas = ventas,
                    ventasFiltradas = ventas,
                    ventasDelDia = ventasHoy,
                    totalVentas = totalGeneral,
                    totalVentasHoy = totalHoy,
                    diasConVentas = diasConVentas,
                    isLoading = false
                )

                filtroVendedor?.let { filtrarPorVendedor(it) }
            }
        }
    }

    private fun cargarProductos() {
        viewModelScope.launch {
            repository.obtenerProductos().collect { productos ->
                state = state.copy(productos = productos)
            }
        }
    }

    private fun eliminarVentasAntiguas() {
        viewModelScope.launch {
            val result = repository.eliminarVentasAntiguas()
            result.onSuccess { cantidad ->
                state = state.copy(ventasEliminadas = cantidad)
                if (cantidad > 0) {
                    android.util.Log.d("DuenoViewModel", "Se eliminaron $cantidad ventas antiguas")
                }
            }
        }
    }

    fun filtrarPorVendedor(vendedorId: String?) {
        filtroVendedor = vendedorId

        val ventasFiltradas = if (vendedorId == null) {
            state.ventas
        } else {
            state.ventas.filter { it.vendedorId == vendedorId }
        }

        state = state.copy(ventasFiltradas = ventasFiltradas)
    }

    // NUEVA FUNCIÓN - Filtrar por fecha específica
    fun filtrarPorFecha(fecha: String?) {
        state = state.copy(fechaSeleccionada = fecha)

        if (fecha == null) {
            // Mostrar todas las ventas
            state = state.copy(ventasFiltradas = state.ventas)
        } else {
            // Filtrar por fecha específica
            val ventasDelDia = state.ventas.filter { venta ->
                val cal = Calendar.getInstance().apply { timeInMillis = venta.fecha }
                val ventaFecha = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}"
                ventaFecha == fecha
            }
            state = state.copy(ventasFiltradas = ventasDelDia)
        }
    }

    fun obtenerVendedoresUnicos(): List<Pair<String, String>> {
        return state.ventas
            .map { it.vendedorId to it.vendedorNombre }
            .distinct()
    }

    fun aumentarStock(productoId: String, cantidadAAgregar: Int) {
        viewModelScope.launch {
            val producto = state.productos.find { it.id == productoId }
            if (producto != null) {
                val nuevoStock = producto.stock + cantidadAAgregar
                repository.actualizarStock(productoId, nuevoStock)
            }
        }
    }
}
