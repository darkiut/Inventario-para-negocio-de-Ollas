package com.negocio.inventarioollas.repository

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.negocio.inventarioollas.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

object FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance().apply {
            setPersistenceEnabled(true)
        }
    }

    private val usuariosRef = database.getReference("usuarios")
    private val productosRef = database.getReference("productos")
    private val ventasRef = database.getReference("ventas")
    private val configRef = database.getReference("configuracion")

    // ==================== AUTENTICACIÓN ====================

    suspend fun registrarUsuario(email: String, password: String, nombre: String, rol: String): Result<Usuario> {
        return try {
            withTimeout(10.seconds) {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("Error al obtener ID de usuario")

                val usuario = Usuario(
                    id = userId,
                    nombre = nombre,
                    email = email,
                    rol = rol
                )

                usuariosRef.child(userId).setValue(usuario).await()
                Result.success(usuario)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(email: String, password: String): Result<Usuario> {
        return try {
            withTimeout(10.seconds) {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val userId = result.user?.uid ?: throw Exception("Error al obtener ID de usuario")
                obtenerUsuarioPorId(userId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerUsuarioPorId(userId: String): Result<Usuario> {
        return try {
            withTimeout(10.seconds) {
                val snapshot = usuariosRef.child(userId).get().await()
                val usuario = snapshot.getValue(Usuario::class.java)

                if (usuario != null) {
                    Result.success(usuario)
                } else {
                    Result.failure(Exception("Usuario no encontrado en la base de datos"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        auth.signOut()
    }

    fun obtenerUsuarioActual(): String? {
        return auth.currentUser?.uid
    }

    // ==================== PRODUCTOS ====================

    suspend fun agregarProducto(producto: Producto): Result<String> {
        return try {
            withTimeout(10.seconds) {
                val productoId = productosRef.push().key ?: throw Exception("Error al generar ID")
                producto.id = productoId
                productosRef.child(productoId).setValue(producto).await()
                Result.success(productoId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarProducto(producto: Producto): Result<Unit> {
        return try {
            withTimeout(10.seconds) {
                productosRef.child(producto.id).setValue(producto).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerProductos(): Flow<List<Producto>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productos = mutableListOf<Producto>()
                for (child in snapshot.children) {
                    child.getValue(Producto::class.java)?.let { productos.add(it) }
                }
                trySend(productos)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        productosRef.addValueEventListener(listener)
        awaitClose { productosRef.removeEventListener(listener) }
    }

    suspend fun aumentarStock(productoId: String, cantidad: Int): Result<Unit> {
        return try {
            withTimeout(10.seconds) {
                val ref = productosRef.child(productoId).child("stock")
                val snapshot = ref.get().await()
                val stockActual = snapshot.getValue(Int::class.java) ?: 0
                ref.setValue(stockActual + cantidad).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarProducto(productoId: String): Result<Unit> {
        return try {
            withTimeout(10.seconds) {
                productosRef.child(productoId).removeValue().await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== VENTAS ====================

    suspend fun registrarVenta(venta: Venta): Result<String> {
        return try {
            withTimeout(15.seconds) {
                val ventaId = ventasRef.push().key ?: throw Exception("Error al generar ID")
                venta.id = ventaId

                // Registrar venta
                ventasRef.child(ventaId).setValue(venta).await()

                // Descontar stock de cada producto
                venta.productos.values.forEach { item ->
                    val productoRef = productosRef.child(item.productoId).child("stock")
                    val snapshot = productoRef.get().await()
                    val stockActual = snapshot.getValue(Int::class.java) ?: 0
                    val nuevoStock = (stockActual - item.cantidad).coerceAtLeast(0)
                    productoRef.setValue(nuevoStock).await()
                }

                Result.success(ventaId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun obtenerVentas(): Flow<List<Venta>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ventas = mutableListOf<Venta>()
                for (child in snapshot.children) {
                    child.getValue(Venta::class.java)?.let { ventas.add(it) }
                }
                trySend(ventas.sortedByDescending { it.fecha })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ventasRef.addValueEventListener(listener)
        awaitClose { ventasRef.removeEventListener(listener) }
    }

    fun obtenerVentasPorVendedor(vendedorId: String): Flow<List<Venta>> = callbackFlow {
        val query = ventasRef.orderByChild("vendedorId").equalTo(vendedorId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ventas = mutableListOf<Venta>()
                for (child in snapshot.children) {
                    child.getValue(Venta::class.java)?.let { ventas.add(it) }
                }
                trySend(ventas.sortedByDescending { it.fecha })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    // ==================== CONFIGURACIÓN NEGOCIO ====================

    suspend fun guardarDatosNegocio(datos: DatosNegocio): Result<Unit> {
        return try {
            withTimeout(10.seconds) {
                configRef.setValue(datos).await()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerDatosNegocio(): Result<DatosNegocio> {
        return try {
            withTimeout(10.seconds) {
                val snapshot = configRef.get().await()
                val datos = snapshot.getValue(DatosNegocio::class.java) ?: DatosNegocio()
                Result.success(datos)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ELIMINAR Y OBTENER VENTAS ====================

    // ✅ CORREGIDO - Eliminar venta y reponer stock
    suspend fun eliminarVenta(ventaId: String): Result<Unit> {
        return try {
            withTimeout(15.seconds) {
                suspendCancellableCoroutine { continuation ->
                    // Usar ventasRef en lugar de database.child()
                    val ventaRef = ventasRef.child(ventaId)

                    // Primero obtenemos la venta para saber qué productos reponer
                    ventaRef.get().addOnSuccessListener { snapshot ->
                        val venta = snapshot.getValue(Venta::class.java)

                        if (venta == null) {
                            continuation.resume(Result.failure(Exception("Venta no encontrada")))
                            return@addOnSuccessListener
                        }

                        // Reponer stock de cada producto
                        val reposiciones = mutableListOf<Task<Void>>()

                        venta.productos.values.forEach { item ->
                            val productoRef = productosRef.child(item.productoId)
                            val reposicion = productoRef.get().continueWithTask { task ->
                                if (task.isSuccessful) {
                                    val producto = task.result.getValue(Producto::class.java)
                                    if (producto != null) {
                                        val nuevoStock = producto.stock + item.cantidad
                                        productoRef.child("stock").setValue(nuevoStock)
                                    } else {
                                        Tasks.forResult(null)
                                    }
                                } else {
                                    Tasks.forResult(null)
                                }
                            }
                            @Suppress("UNCHECKED_CAST")
                            reposiciones.add(reposicion as Task<Void>)
                        }

                        // Cuando todas las reposiciones terminen, eliminamos la venta
                        Tasks.whenAll(reposiciones).addOnCompleteListener {
                            ventaRef.removeValue().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    continuation.resume(Result.success(Unit))
                                } else {
                                    continuation.resume(
                                        Result.failure(
                                            deleteTask.exception ?: Exception("Error al eliminar venta")
                                        )
                                    )
                                }
                            }
                        }
                    }.addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ CORREGIDO - Obtener detalles de una venta específica
    suspend fun obtenerVenta(ventaId: String): Result<Venta> {
        return try {
            withTimeout(10.seconds) {
                suspendCancellableCoroutine { continuation ->
                    // Usar ventasRef en lugar de database.child()
                    ventasRef.child(ventaId).get()
                        .addOnSuccessListener { snapshot ->
                            val venta = snapshot.getValue(Venta::class.java)
                            if (venta != null) {
                                continuation.resume(Result.success(venta))
                            } else {
                                continuation.resume(Result.failure(Exception("Venta no encontrada")))
                            }
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
