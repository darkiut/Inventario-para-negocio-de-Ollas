package com.negocio.inventarioollas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.negocio.inventarioollas.viewmodels.ProductoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarProductoScreen(
    productoViewModel: ProductoViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state = productoViewModel.state
    val scrollState = rememberScrollState()

    // Manejar navegación automática después de agregar producto
    state.successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(800) // Esperar 800ms
            productoViewModel.limpiarMensajes()
            productoViewModel.limpiarCampos()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nombre
                OutlinedTextField(
                    value = productoViewModel.nombre,
                    onValueChange = { productoViewModel.onNombreChange(it) },
                    label = { Text("Nombre del producto *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading
                )

                // Código
                OutlinedTextField(
                    value = productoViewModel.codigo,
                    onValueChange = { productoViewModel.onCodigoChange(it) },
                    label = { Text("Código *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading
                )

                // Categoría
                OutlinedTextField(
                    value = productoViewModel.categoria,
                    onValueChange = { productoViewModel.onCategoriaChange(it) },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading
                )

                // Stock
                OutlinedTextField(
                    value = productoViewModel.stock,
                    onValueChange = { productoViewModel.onStockChange(it) },
                    label = { Text("Stock *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading
                )

                // Precio
                OutlinedTextField(
                    value = productoViewModel.precioUnitario,
                    onValueChange = { productoViewModel.onPrecioChange(it) },
                    label = { Text("Precio unitario (S/) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isLoading
                )

                // Descripción
                OutlinedTextField(
                    value = productoViewModel.descripcion,
                    onValueChange = { productoViewModel.onDescripcionChange(it) },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Agregar
                Button(
                    onClick = {
                        productoViewModel.agregarProducto {
                            // onSuccess callback - no hace nada aquí
                            // La navegación se maneja en el LaunchedEffect
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("AGREGAR PRODUCTO")
                }

                Text(
                    "* Campos obligatorios",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mensaje de éxito (Toast-like)
            state.successMessage?.let { message ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack, // Puedes cambiar por CheckCircle si lo tienes
                                contentDescription = null,
                                tint = Color.White
                            )
                            Text(
                                text = message,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Mensaje de error
            state.error?.let { error ->
                AlertDialog(
                    onDismissRequest = { productoViewModel.limpiarMensajes() },
                    title = { Text("Error") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { productoViewModel.limpiarMensajes() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}
