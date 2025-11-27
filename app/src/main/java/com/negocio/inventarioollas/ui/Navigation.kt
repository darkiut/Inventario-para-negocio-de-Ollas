package com.negocio.inventarioollas.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.negocio.inventarioollas.ui.screens.*
import com.negocio.inventarioollas.viewmodels.AuthViewModel
import com.negocio.inventarioollas.viewmodels.ProductoViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object HomeVendedor : Screen("home_vendedor")
    object HomeDueno : Screen("home_dueno")
    object AgregarProducto : Screen("agregar_producto")
    object Ventas : Screen("ventas")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // ViewModels compartidos a nivel de Navigation
    val authViewModel: AuthViewModel = viewModel()
    val productoViewModel: ProductoViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = { usuario ->
                    val destination = if (usuario.rol == "dueno") {
                        Screen.HomeDueno.route
                    } else {
                        Screen.HomeVendedor.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = { usuario ->
                    val destination = if (usuario.rol == "dueno") {
                        Screen.HomeDueno.route
                    } else {
                        Screen.HomeVendedor.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.HomeVendedor.route) {
            val usuario = authViewModel.authState.usuario
            if (usuario != null) {
                HomeVendedorScreen(
                    usuario = usuario,
                    authViewModel = authViewModel,
                    productoViewModel = productoViewModel,
                    onNavigateToAgregarProducto = {
                        navController.navigate(Screen.AgregarProducto.route)
                    },
                    onNavigateToVentas = {
                        navController.navigate(Screen.Ventas.route)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.HomeDueno.route) {
            val usuario = authViewModel.authState.usuario
            if (usuario != null) {
                HomeDuenoScreen(
                    usuario = usuario,
                    authViewModel = authViewModel,
                    onNavigateToProductos = {
                        navController.navigate(Screen.AgregarProducto.route)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.AgregarProducto.route) {
            AgregarProductoScreen(
                productoViewModel = productoViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Ventas.route) {
            val usuario = authViewModel.authState.usuario
            if (usuario != null) {
                VentasScreen(
                    usuario = usuario,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onVentaRegistrada = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
