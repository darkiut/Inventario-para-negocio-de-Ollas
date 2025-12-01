package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.DatosNegocio
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.launch

class ConfiguracionViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    var nombre by mutableStateOf("")
    var ruc by mutableStateOf("")
    var direccion by mutableStateOf("")
    var telefono by mutableStateOf("")
    var mensajeFinal by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var mensajeExito by mutableStateOf<String?>(null)

    init {
        cargarDatos()
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            isLoading = true
            repository.obtenerDatosNegocio().onSuccess { datos ->
                nombre = datos.nombre
                ruc = datos.ruc
                direccion = datos.direccion
                telefono = datos.telefono
                mensajeFinal = datos.mensajeFinal
            }
            isLoading = false
        }
    }

    fun guardarCambios() {
        viewModelScope.launch {
            isLoading = true
            val datos = DatosNegocio(nombre, ruc, direccion, telefono, mensajeFinal)
            repository.guardarDatosNegocio(datos).onSuccess {
                mensajeExito = "Datos guardados correctamente"
            }
            isLoading = false
        }
    }

    fun limpiarMensaje() { mensajeExito = null }
}