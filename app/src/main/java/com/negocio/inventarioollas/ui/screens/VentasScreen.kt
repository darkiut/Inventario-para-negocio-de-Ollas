package com.negocio.inventarioollas.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.negocio.inventarioollas.models.ItemVenta
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Usuario
import com.negocio.inventarioollas.utils.PdfGenerator
import com.negocio.inventarioollas.viewmodels.VentaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    usuario: Usuario,
    onNavigateBack: () -> Unit,
    ventaViewModel: VentaViewModel = viewModel()
) {
    val state = ventaViewModel.state
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (state.ventaExitosa != null) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50)) },
            title = { Text("¡Venta Exitosa!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("La venta se registró correctamente.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¿Deseas enviar el comprobante?", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pdfGenerator = PdfGenerator(context)
                        // PASAMOS LA VENTA Y LOS DATOS DEL NEGOCIO
                        pdfGenerator.generarYEnviarPdf(state.ventaExitosa, state.datosNegocio)
                        ventaViewModel.limpiarVentaExitosa()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar WhatsApp")
                }
            },
            dismissButton = { TextButton(onClick = { ventaViewModel.limpiarVentaExitosa() }) { Text("Cerrar") } }
        )
    }

    LaunchedEffect(state.error) { state.error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Venta") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total a Pagar:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("S/ ${String.format("%.2f", state.total)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showConfirmDialog = true }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = state.carrito.isNotEmpty() && !state.isLoading) {
                        if (state.isLoading) CircularProgressIndicator(color = Color.White) else { Icon(Icons.Default.ShoppingCart, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Continuar") }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (state.carrito.isNotEmpty()) {
                    item { Text("Carrito de Compras (${state.carrito.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                    items(state.carrito.values.toList()) { item -> ItemCarritoCard(item = item, onRemove = { ventaViewModel.quitarProductoDelCarrito(item.productoId) }) }
                    item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                }
                item { Text("Productos Disponibles", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                if (state.productosDisponibles.isEmpty()) item { Text("No hay productos con stock disponible.") }
                else items(state.productosDisponibles) { producto -> ProductoVentaCard(producto = producto, onAddToCart = { cantidad -> ventaViewModel.agregarProductoAlCarrito(producto, cantidad) }) }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Finalizar Venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tipo de Documento:", fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Boleta", "Factura", "Nota").forEach { tipo ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = state.tipoDocumento == tipo, onClick = { ventaViewModel.cambiarTipoDocumento(tipo) })
                                Text(tipo, fontSize = 14.sp)
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(value = state.clienteNombre, onValueChange = { ventaViewModel.actualizarDatosCliente(it, state.clienteTelefono, state.clienteDni) }, label = { Text("Nombre Cliente *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.clienteDni, onValueChange = { ventaViewModel.actualizarDatosCliente(state.clienteNombre, state.clienteTelefono, it) }, label = { Text(if (state.tipoDocumento == "Factura") "RUC *" else "DNI (Opcional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.clienteTelefono, onValueChange = { ventaViewModel.actualizarDatosCliente(state.clienteNombre, it, state.clienteDni) }, label = { Text("Teléfono (Opcional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { showConfirmDialog = false; ventaViewModel.registrarVenta(usuario.id, usuario.nombre) }, enabled = state.clienteNombre.isNotBlank() && (state.tipoDocumento != "Factura" || state.clienteDni.isNotBlank())) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }
}

// (Mantén aquí las funciones ProductoVentaCard e ItemCarritoCard que ya tenías)
@Composable
fun ProductoVentaCard(producto: Producto, onAddToCart: (Int) -> Unit) {
    var cantidad by remember { mutableStateOf("1") }
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(producto.nombre, fontWeight = FontWeight.Bold)
                Text("Stock: ${producto.stock}", fontSize = 12.sp, color = Color.Gray)
                Text("S/ ${producto.precioUnitario}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = cantidad, onValueChange = { if (it.all { char -> char.isDigit() }) cantidad = it }, modifier = Modifier.width(60.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textStyle = LocalTextStyle.current.copy(fontSize = 14.sp))
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { val cantInt = cantidad.toIntOrNull() ?: 1; if (cantInt > 0) onAddToCart(cantInt); cantidad = "1" }, colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Icon(Icons.Default.Add, contentDescription = "Agregar") }
            }
        }
    }
}

@Composable
fun ItemCarritoCard(item: ItemVenta, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, fontWeight = FontWeight.Medium)
                Text("${item.cantidad} x S/ ${item.precioUnitario}", fontSize = 12.sp, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("S/ ${String.format("%.2f", item.subtotal)}", fontWeight = FontWeight.Bold)
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}