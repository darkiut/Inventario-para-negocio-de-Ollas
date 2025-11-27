package com.negocio.inventarioollas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Usuario
import com.negocio.inventarioollas.viewmodels.AuthViewModel
import com.negocio.inventarioollas.viewmodels.ProductoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeVendedorScreen(
    usuario: Usuario,
    authViewModel: AuthViewModel,
    productoViewModel: ProductoViewModel = viewModel(),
    onNavigateToAgregarProducto: () -> Unit,
    onNavigateToVentas: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val productoState = productoViewModel.state
    var showMenu by remember { mutableStateOf(false) }

    // Cargar productos al entrar
    LaunchedEffect(Unit) {
        productoViewModel.inicializarProductos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventario Ollas")
                        Text(
                            text = "Vendedor: ${usuario.nombre}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menú")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                authViewModel.cerrarSesion()
                                onNavigateToLogin()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAgregarProducto,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Botones de acciones principales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToVentas,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Nueva Venta")
                }
            }

            // Lista de productos
            if (productoState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (productoState.productos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No hay productos registrados",
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Presiona + para agregar uno",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Productos en Inventario",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(productoState.productos) { producto ->
                        ProductoCard(
                            producto = producto,
                            onAumentarStock = { productoId, cantidad ->
                                productoViewModel.aumentarStock(productoId, cantidad)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductoCard(
    producto: Producto,
    onAumentarStock: ((String, Int) -> Unit)? = null
) {
    val borderColor = when {
        producto.stock >= 30 -> Color(0xFF4CAF50)
        producto.stock in 11..29 -> Color(0xFFFF9800)
        producto.stock in 1..10 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    val stockColor = when {
        producto.stock >= 30 -> Color(0xFF4CAF50)
        producto.stock in 11..29 -> Color(0xFFFF9800)
        producto.stock in 1..10 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    var showDialogoCantidad by remember { mutableStateOf(false) }
    var cantidadAAgregar by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Código: ${producto.codigo}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (producto.categoria.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Categoría: ${producto.categoria}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "S/ ${String.format("%.2f", producto.precioUnitario)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = stockColor.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = stockColor
                            )
                            Text(
                                text = "Stock: ${producto.stock}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = stockColor
                            )
                        }
                    }

                    Text(
                        text = when {
                            producto.stock >= 30 -> "✓ Disponible"
                            producto.stock in 11..29 -> "⚠ Stock Medio"
                            producto.stock in 1..10 -> "⚠ Stock Bajo"
                            else -> "✗ Agotado"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = stockColor
                    )
                }

                if (onAumentarStock != null) {
                    IconButton(
                        onClick = { showDialogoCantidad = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Aumentar stock",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showDialogoCantidad) {
        AlertDialog(
            onDismissRequest = {
                showDialogoCantidad = false
                cantidadAAgregar = ""
            },
            title = { Text("Aumentar Stock") },
            text = {
                Column {
                    Text("Producto: ${producto.nombre}")
                    Text("Stock actual: ${producto.stock}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = cantidadAAgregar,
                        onValueChange = { cantidadAAgregar = it },
                        label = { Text("Cantidad a agregar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (cantidadAAgregar.toIntOrNull() != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Nuevo stock: ${producto.stock + (cantidadAAgregar.toIntOrNull() ?: 0)}",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cantidad = cantidadAAgregar.toIntOrNull()
                        if (cantidad != null && cantidad > 0) {
                            onAumentarStock?.invoke(producto.id, cantidad)
                            showDialogoCantidad = false
                            cantidadAAgregar = ""
                        }
                    },
                    enabled = (cantidadAAgregar.toIntOrNull() ?: 0) > 0
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialogoCantidad = false
                        cantidadAAgregar = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
