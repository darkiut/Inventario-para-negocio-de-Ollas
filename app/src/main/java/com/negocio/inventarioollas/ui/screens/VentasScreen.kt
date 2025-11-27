package com.negocio.inventarioollas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.negocio.inventarioollas.models.Venta
import com.negocio.inventarioollas.utils.PdfGenerator
import com.negocio.inventarioollas.utils.WhatsAppHelper
import com.negocio.inventarioollas.viewmodels.VentaViewModel
import androidx.compose.foundation.clickable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Divider


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    usuario: Usuario,
    ventaViewModel: VentaViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onVentaRegistrada: () -> Unit
) {
    val state = ventaViewModel.state
    val context = LocalContext.current
    var showProductoDialog by remember { mutableStateOf(false) }
    var showClienteDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var ventaCompletada by remember { mutableStateOf<Venta?>(null) }

    LaunchedEffect(state.ventaRegistrada) {
        if (state.ventaRegistrada) {
            showSuccessDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Venta") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Carrito de compras
            if (state.carrito.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Carrito vacío",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.carrito.values.toList()) { item ->
                        CarritoItemCard(
                            item = item,
                            onCantidadChange = { nuevaCantidad ->
                                ventaViewModel.actualizarCantidad(item.productoId, nuevaCantidad)
                            },
                            onEliminar = {
                                ventaViewModel.eliminarDelCarrito(item.productoId)
                            }
                        )
                    }
                }
            }

            // Total y botones
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL:",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "S/ ${String.format("%.2f", state.total)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showProductoDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Producto")
                        }

                        Button(
                            onClick = { showClienteDialog = true },
                            enabled = state.carrito.isNotEmpty() && !state.isLoading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Finalizar")
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para agregar productos
    if (showProductoDialog) {
        AgregarProductoDialog(
            productos = state.productos,
            onDismiss = { showProductoDialog = false },
            onAgregar = { producto, cantidad ->
                ventaViewModel.agregarAlCarrito(producto, cantidad)
                showProductoDialog = false
            }
        )
    }

    // Diálogo para datos del cliente
    if (showClienteDialog) {
        DatosClienteDialog(
            ventaViewModel = ventaViewModel,
            usuario = usuario,
            onDismiss = { showClienteDialog = false },
            onConfirmar = {
                ventaViewModel.registrarVenta(usuario.id, usuario.nombre) { venta ->
                    ventaCompletada = venta
                    showClienteDialog = false
                }
            }
        )
    }

    // Mostrar errores
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = { ventaViewModel.limpiarError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { ventaViewModel.limpiarError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Diálogo de éxito con opciones de PDF y WhatsApp
    if (showSuccessDialog && ventaCompletada != null) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        "¡Venta Registrada!",
                        color = Color(0xFF4CAF50)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        "La venta se registró correctamente.",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "S/ ${String.format("%.2f", state.total)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "¿Desea enviar el comprobante al cliente?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ventaCompletada?.let { venta ->
                                val pdfGenerator = PdfGenerator(context)
                                val pdfFile = pdfGenerator.generarPDF(venta, ventaViewModel.tipoDocumento)

                                if (pdfFile != null) {
                                    WhatsAppHelper.enviarPDFPorWhatsApp(
                                        context,
                                        pdfFile,
                                        ventaViewModel.clienteTelefono,
                                        "¡Gracias por su compra! Adjunto encontrará su ${ventaViewModel.tipoDocumento}."
                                    )
                                }
                            }
                            showSuccessDialog = false
                            ventaViewModel.resetVentaRegistrada()
                            ventaViewModel.limpiarVenta()
                            ventaCompletada = null
                            onVentaRegistrada()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366)
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ENVIAR POR WHATSAPP", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            showSuccessDialog = false
                            ventaViewModel.resetVentaRegistrada()
                            ventaViewModel.limpiarVenta()
                            ventaCompletada = null
                            onVentaRegistrada()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OMITIR")
                    }
                }
            }
        )
    }
}

@Composable
fun CarritoItemCard(
    item: ItemVenta,
    onCantidadChange: (Int) -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "S/ ${String.format("%.2f", item.precioUnitario)} c/u",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subtotal: S/ ${String.format("%.2f", item.subtotal)}",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onCantidadChange(item.cantidad - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = item.cantidad.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                IconButton(
                    onClick = { onCantidadChange(item.cantidad + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar")
                }

                IconButton(
                    onClick = onEliminar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AgregarProductoDialog(
    productos: List<Producto>,
    onDismiss: () -> Unit,
    onAgregar: (Producto, Int) -> Unit
) {
    var selectedProducto by remember { mutableStateOf<Producto?>(null) }
    var cantidad by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Producto") },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productos) { producto ->
                        val stockColor = when {
                            producto.stock >= 30 -> Color(0xFF4CAF50)
                            producto.stock in 11..29 -> Color(0xFFFF9800)
                            producto.stock in 1..10 -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedProducto = producto },
                            border = if (selectedProducto?.id == producto.id) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, stockColor)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(producto.nombre, fontWeight = FontWeight.Bold)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Stock: ${producto.stock}",
                                            fontSize = 12.sp,
                                            color = stockColor,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    "S/ ${String.format("%.2f", producto.precioUnitario)}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                selectedProducto?.let { producto ->
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { cantidad = it },
                        label = { Text("Cantidad (Máx: ${producto.stock})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedProducto?.let { producto ->
                        val cant = cantidad.toIntOrNull() ?: 1
                        onAgregar(producto, cant)
                    }
                },
                enabled = selectedProducto != null
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DatosClienteDialog(
    ventaViewModel: VentaViewModel,
    usuario: Usuario,
    onDismiss: () -> Unit,
    onConfirmar: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Opciones de tipo de documento
    val tiposDocumento = listOf(
        "boleta" to "Boleta",
        "factura" to "Factura",
        "nota_pedido" to "Nota de Pedido"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datos del Cliente") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de tipo de documento
                Text(
                    "Tipo de Documento *",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                tiposDocumento.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ventaViewModel.onTipoDocumentoChange(value) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ventaViewModel.tipoDocumento == value,
                            onClick = { ventaViewModel.onTipoDocumentoChange(value) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ventaViewModel.clienteNombre,
                    onValueChange = { ventaViewModel.onClienteNombreChange(it) },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ventaViewModel.clienteTelefono,
                    onValueChange = { ventaViewModel.onClienteTelefonoChange(it) },
                    label = { Text("Teléfono/WhatsApp *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mostrar campo DNI solo para Boleta y Nota de Pedido
                if (ventaViewModel.tipoDocumento != "factura") {
                    OutlinedTextField(
                        value = ventaViewModel.clienteDni,
                        onValueChange = { ventaViewModel.onClienteDniChange(it) },
                        label = { Text("DNI ${if (ventaViewModel.tipoDocumento == "factura") "*" else ""}") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mostrar campo RUC solo para Factura
                if (ventaViewModel.tipoDocumento == "factura") {
                    OutlinedTextField(
                        value = ventaViewModel.clienteRuc,
                        onValueChange = { ventaViewModel.onClienteRucChange(it) },
                        label = { Text("RUC *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("20123456789") }
                    )
                }

                OutlinedTextField(
                    value = ventaViewModel.clienteDireccion,
                    onValueChange = { ventaViewModel.onClienteDireccionChange(it) },
                    label = { Text("Dirección ${if (ventaViewModel.tipoDocumento == "factura") "*" else ""}") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Nota informativa
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        when (ventaViewModel.tipoDocumento) {
                            "factura" -> "Para factura se requiere: RUC y Dirección"
                            "boleta" -> "Para boleta se puede agregar DNI (opcional)"
                            else -> "Nota de pedido: documento sin valor tributario"
                        },
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmar) {
                Text("Confirmar Venta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

