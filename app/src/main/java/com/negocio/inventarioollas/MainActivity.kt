package com.negocio.inventarioollas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.negocio.inventarioollas.ui.screens.*
import com.negocio.inventarioollas.ui.theme.InventarioOllasTheme
import com.negocio.inventarioollas.viewmodels.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InventarioOllasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Instanciamos los ViewModels
    val authViewModel: AuthViewModel = viewModel()
    val productoViewModel: ProductoViewModel = viewModel()

    // Accedemos al usuario a través del state
    val usuario = authViewModel.state.usuario

    NavHost(navController = navController, startDestination = "login") {

        // 1. Pantalla de Login
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { usuarioLogueado ->
                    if (usuarioLogueado.rol == "dueno") {
                        navController.navigate("home_dueno") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("home_vendedor") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // 2. Pantalla de Registro (CORREGIDA)
        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.popBackStack() } // <--- ESTA ES LA QUE FALTABA
            )
        }

        // 3. Pantalla Home Vendedor
        composable("home_vendedor") {
            if (usuario == null) {
                navController.navigate("login") { popUpTo(0) }
            } else {
                HomeVendedorScreen(
                    usuario = usuario,
                    authViewModel = authViewModel,
                    productoViewModel = productoViewModel,
                    onNavigateToAgregarProducto = { navController.navigate("agregar_producto") },
                    onNavigateToVentas = { navController.navigate("ventas") },
                    onNavigateToLogin = {
                        navController.navigate("login") { popUpTo(0) }
                    },
                    onNavigateToEditarProducto = {
                        navController.navigate("editar_producto")
                    }
                )
            }
        }

        // 4. Pantalla Home Dueño
        composable("home_dueno") {
            if (usuario == null) {
                navController.navigate("login") { popUpTo(0) }
            } else {
                HomeDuenoScreen(
                    usuario = usuario,
                    authViewModel = authViewModel,
                    productoViewModel = productoViewModel,
                    onNavigateToProductos = { navController.navigate("agregar_producto") },
                    onNavigateToLogin = {
                        navController.navigate("login") { popUpTo(0) }
                    },
                    onNavigateToEditarProducto = {
                        navController.navigate("editar_producto")
                    },
                    onNavigateToConfig = {
                        navController.navigate("configuracion") // <--- ESTA TAMBIÉN ES IMPORTANTE
                    }
                )
            }
        }

        // 5. Agregar Producto
        composable("agregar_producto") {
            AgregarProductoScreen(
                onNavigateBack = { navController.popBackStack() },
                productoViewModel = productoViewModel
            )
        }

        // 6. Editar Producto
        composable("editar_producto") {
            EditarProductoScreen(
                onNavigateBack = { navController.popBackStack() },
                productoViewModel = productoViewModel
            )
        }

        // 7. Ventas
        composable("ventas") {
            if (usuario != null) {
                VentasScreen(
                    usuario = usuario,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        // 8. Configuración
        composable("configuracion") {
            ConfiguracionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}