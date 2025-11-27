package com.negocio.inventarioollas.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.negocio.inventarioollas.models.Usuario
import com.negocio.inventarioollas.repository.FirebaseRepository
import kotlinx.coroutines.launch


data class AuthState(
    val isLoading: Boolean = false,
    val usuario: Usuario? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    var authState by mutableStateOf(AuthState())
        private set

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var nombre by mutableStateOf("")
        private set

    var emailError by mutableStateOf<String?>(null)
        private set

    var passwordError by mutableStateOf<String?>(null)
        private set

    fun onEmailChange(newEmail: String) {
        email = newEmail
        emailError = null
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
        passwordError = null
    }

    fun onNombreChange(newNombre: String) {
        nombre = newNombre
    }

    private fun validateEmail(): Boolean {
        return when {
            email.isBlank() -> {
                emailError = "El correo es requerido"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailError = "Formato de correo inválido"
                false
            }
            else -> {
                emailError = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        return when {
            password.isBlank() -> {
                passwordError = "La contraseña es requerida"
                false
            }
            password.length < 6 -> {
                passwordError = "La contraseña debe tener al menos 6 caracteres"
                false
            }
            else -> {
                passwordError = null
                true
            }
        }
    }

    fun iniciarSesion(onSuccess: (Usuario) -> Unit) {
        if (!validateEmail() || !validatePassword()) return

        authState = authState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repository.iniciarSesion(email, password)

            result.onSuccess { usuario ->
                authState = authState.copy(
                    isLoading = false,
                    usuario = usuario,
                    isAuthenticated = true
                )
                onSuccess(usuario)
            }.onFailure { error ->
                authState = authState.copy(
                    isLoading = false,
                    error = error.message ?: "Error al iniciar sesión"
                )
            }
        }
    }

    fun registrarUsuario(rol: String, onSuccess: (Usuario) -> Unit) {
        if (!validateEmail() || !validatePassword() || nombre.isBlank()) {
            authState = authState.copy(error = "Por favor completa todos los campos")
            return
        }

        authState = authState.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repository.registrarUsuario(email, password, nombre, rol)

            result.onSuccess { usuario ->
                authState = authState.copy(
                    isLoading = false,
                    usuario = usuario,
                    isAuthenticated = true
                )
                onSuccess(usuario)
            }.onFailure { error ->
                authState = authState.copy(
                    isLoading = false,
                    error = error.message ?: "Error al registrar usuario"
                )
            }
        }
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        authState = AuthState()
        email = ""
        password = ""
        nombre = ""
    }

    fun limpiarError() {
        authState = authState.copy(error = null)
    }

    fun cargarUsuarioActual() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            authState = authState.copy(isLoading = true)

            viewModelScope.launch {
                val result = repository.obtenerUsuarioPorId(currentUser.uid)
                result.onSuccess { usuario ->
                    authState = authState.copy(
                        isLoading = false,
                        usuario = usuario,
                        isAuthenticated = true
                    )
                }.onFailure {
                    authState = authState.copy(isLoading = false)
                }
            }
        }
    }

}
