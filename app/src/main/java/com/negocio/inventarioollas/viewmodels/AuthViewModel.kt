package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.negocio.inventarioollas.models.Usuario
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.launch

// Estado de Autenticación
data class AuthState(
    val usuario: Usuario? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {
    // ✅ Ahora usa el singleton (sin paréntesis)
    private val repository = FirebaseRepository

    var state by mutableStateOf(AuthState())
        private set

    val usuarioActual: Usuario?
        get() = state.usuario

    init {
        verificarSesion()
    }

    private fun verificarSesion() {
        val uid = repository.obtenerUsuarioActual()
        if (uid != null) {
            viewModelScope.launch {
                state = state.copy(isLoading = true)
                repository.obtenerUsuarioPorId(uid)
                    .onSuccess { usuario ->
                        state = state.copy(
                            usuario = usuario,
                            isAuthenticated = true,
                            isLoading = false
                        )
                    }
                    .onFailure {
                        state = state.copy(isLoading = false)
                    }
            }
        }
    }

    fun iniciarSesion(email: String, password: String, onSuccess: (Usuario) -> Unit) {
        // ✅ VALIDACIONES MEJORADAS
        if (email.isBlank() || password.isBlank()) {
            state = state.copy(error = "Llena todos los campos")
            return
        }

        if (!email.contains("@") || !email.contains(".")) {
            state = state.copy(error = "Email inválido")
            return
        }

        if (password.length < 6) {
            state = state.copy(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val result = repository.iniciarSesion(email, password)

            result.onSuccess { usuario ->
                state = state.copy(
                    usuario = usuario,
                    isAuthenticated = true,
                    isLoading = false
                )
                onSuccess(usuario)
            }.onFailure { e ->
                state = state.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun registrarUsuario(nombre: String, email: String, password: String, rol: String, onSuccess: () -> Unit) {
        // ✅ VALIDACIONES MEJORADAS
        if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
            state = state.copy(error = "Llena todos los campos")
            return
        }

        if (!email.contains("@") || !email.contains(".")) {
            state = state.copy(error = "Email inválido")
            return
        }

        if (password.length < 8) {
            state = state.copy(error = "La contraseña debe tener al menos 8 caracteres")
            return
        }

        if (!password.any { it.isDigit() }) {
            state = state.copy(error = "La contraseña debe contener al menos un número")
            return
        }

        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            val result = repository.registrarUsuario(email, password, nombre, rol)

            result.onSuccess {
                state = state.copy(isLoading = false)
                onSuccess()
            }.onFailure { e ->
                state = state.copy(
                    isLoading = false,
                    error = "Error al registrar: ${e.message}"
                )
            }
        }
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        state = AuthState()
    }

    fun limpiarErrores() {
        state = state.copy(error = null)
    }
}
