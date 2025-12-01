package com.negocio.inventarioollas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Usuario
import com.negocio.inventarioollas.models.Venta
import com.negocio.inventarioollas.utils.ExcelGenerator
import com.negocio.inventarioollas.viewmodels.AuthViewModel
import com.negocio.inventarioollas.viewmodels.DuenoViewModel
import com.negocio.inventarioollas.viewmodels.ProductoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDuenoScreen(
    usuario: Usuario,
    authViewModel: AuthViewModel,
    duenoViewModel: DuenoViewModel = viewModel(),
    productoViewModel: ProductoViewModel = viewModel(),
    onNavigateToProductos: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEditarProducto: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    val state = duenoViewModel.state
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showFiltroDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Obtenemos el contexto necesario para generar el Excel
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        duenoViewModel.cargarDatos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Panel del Dueño")
                        Text(
                            text = "Dueño: ${usuario.nombre}",
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
                            text = { Text("Configurar Negocio") },
                            onClick = {
                                showMenu = false
                                onNavigateToConfig()
                            },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
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
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = onNavigateToProductos,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Ventas Hoy",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            "S/ ${String.format("%.2f", state.totalVentasHoy)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Total General",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "S/ ${String.format("%.2f", state.totalVentas)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ventas") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Inventario") }
                )
            }

            when (selectedTab) {
                0 -> {
                    // TAB VENTAS
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (duenoViewModel.state.fechaSeleccionada == null)
                                        "Todas las ventas (30 días)"
                                    else
                                        "Ventas del ${duenoViewModel.state.fechaSeleccionada}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (duenoViewModel.state.ventasEliminadas > 0) {
                                    Text(
                                        "${duenoViewModel.state.ventasEliminadas} ventas antiguas eliminadas",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        // BOTONES DE ACCIÓN (Excel y Filtros)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // BOTÓN EXCEL (NUEVO)
                            Button(
                                onClick = {
                                    val excelGenerator = ExcelGenerator(context)
                                    // Exportamos lo que se está viendo en pantalla (filtrado)
                                    excelGenerator.generarReporteMensual(state.ventasFiltradas)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6F42)) // Verde Excel
                            ) {
                                Icon(Icons.Default.List, contentDescription = null) // Icono de lista
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Excel")
                            }

                            // Botón Filtrar
                            Button(
                                onClick = { showFiltroDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Filtrar")
                            }

                            // Botón Limpiar Filtro (X)
                            if (duenoViewModel.state.fechaSeleccionada != null) {
                                OutlinedButton(onClick = { duenoViewModel.filtrarPorFecha(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filtro de vendedor activo
                        if (duenoViewModel.filtroVendedor != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = true,
                                    onClick = { duenoViewModel.filtrarPorVendedor(null) },
                                    label = { Text("Vendedor activo") },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Lista
                        if (state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else if (state.ventasFiltradas.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
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
                                        if (duenoViewModel.state.fechaSeleccionada != null)
                                            "No hay ventas el ${duenoViewModel.state.fechaSeleccionada}"
                                        else
                                            "No hay ventas registradas",
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.ventasFiltradas) { venta ->
                                    VentaCard(venta = venta)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB INVENTARIO
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = duenoViewModel.searchQuery,
                            onValueChange = { duenoViewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            placeholder = { Text("Buscar producto...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (duenoViewModel.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { duenoViewModel.onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            singleLine = true
                        )

                        if (state.productosFiltrados.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (duenoViewModel.searchQuery.isEmpty()) "No hay productos registrados"
                                    else "No se encontraron resultados",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.productosFiltrados) { producto ->
                                    ProductoCard(
                                        producto = producto,
                                        onAumentarStock = { pid, cant ->
                                            duenoViewModel.aumentarStock(pid, cant)
                                        },
                                        onEditarProducto = { p ->
                                            scope.launch {
                                                delay(300)
                                                productoViewModel.seleccionarProductoParaEditar(p)
                                                onNavigateToEditarProducto()
                                            }
                                        },
                                        onEliminarProducto = { pid ->
                                            productoViewModel.eliminarProducto(pid) {}
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFiltroDialog) {
        AlertDialog(
            onDismissRequest = { showFiltroDialog = false },
            title = { Text("Seleccionar Día") },
            text = {
                LazyColumn {
                    items(duenoViewModel.state.diasConVentas) { fecha ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                duenoViewModel.filtrarPorFecha(fecha)
                                showFiltroDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(fecha)
                            }
                        }
                    }

                    if (duenoViewModel.state.diasConVentas.isEmpty()) {
                        item {
                            Text(
                                "No hay ventas recientes",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFiltroDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun VentaCard(venta: Venta) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val fecha = dateFormat.format(Date(venta.fecha))

    Card(
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, Color(0xFF4CAF50)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = venta.clienteNombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Vendedor: ${venta.vendedorNombre}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "S/ ${String.format("%.2f", venta.total)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = fecha, fontSize = 12.sp, color = Color.Gray)

            if (venta.clienteTelefono.isNotBlank()) {
                Text(
                    text = "Tel: ${venta.clienteTelefono}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("Productos:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            venta.productos.values.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• ${item.nombre} x${item.cantidad}", fontSize = 12.sp)
                    Text(
                        "S/ ${String.format("%.2f", item.subtotal)}",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}