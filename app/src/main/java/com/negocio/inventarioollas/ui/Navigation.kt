package com.negocio.inventarioollas.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.negocio.inventarioollas.ui.screens.*
import com.negocio.inventarioollas.viewmodels.AuthViewModel
import com.negocio.inventarioollas.viewmodels.ProductoViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val productoViewModel: ProductoViewModel = viewModel()
    val usuario = authViewModel.state.usuario

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { usuarioLogueado ->
                    if (usuarioLogueado.rol == "dueno") {
                        navController.navigate("home_dueno") { popUpTo("login") { inclusive = true } }
                    } else {
                        navController.navigate("home_vendedor") { popUpTo("login") { inclusive = true } }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.popBackStack() }
            )
        }

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
                    onNavigateToLogin = { navController.navigate("login") { popUpTo(0) } },
                    onNavigateToEditarProducto = { navController.navigate("editar_producto") }
                )
            }
        }

        composable("home_dueno") {
            if (usuario == null) {
                navController.navigate("login") { popUpTo(0) }
            } else {
                HomeDuenoScreen(
                    usuario = usuario,
                    authViewModel = authViewModel,
                    productoViewModel = productoViewModel,
                    onNavigateToProductos = { navController.navigate("agregar_producto") },
                    onNavigateToLogin = { navController.navigate("login") { popUpTo(0) } },
                    onNavigateToEditarProducto = { navController.navigate("editar_producto") },
                    onNavigateToConfig = { navController.navigate("configuracion") } // <--- NUEVA RUTA CONECTADA
                )
            }
        }

        composable("agregar_producto") {
            AgregarProductoScreen(onNavigateBack = { navController.popBackStack() }, productoViewModel = productoViewModel)
        }

        composable("editar_producto") {
            EditarProductoScreen(onNavigateBack = { navController.popBackStack() }, productoViewModel = productoViewModel)
        }

        composable("ventas") {
            if (usuario != null) {
                VentasScreen(usuario = usuario, onNavigateBack = { navController.popBackStack() })
            }
        }

        // NUEVA PANTALLA
        composable("configuracion") {
            ConfiguracionScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}