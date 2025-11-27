package com.negocio.inventarioollas.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.negocio.inventarioollas.models.Usuario
import com.negocio.inventarioollas.models.Producto
import com.negocio.inventarioollas.models.Venta
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar  // ⬅️ AGREGAR ESTE IMPORT


class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    // Referencias a las colecciones
    private val usuariosRef = database.getReference("usuarios")
    private val productosRef = database.getReference("productos")
    private val ventasRef = database.getReference("ventas")
    private val movimientosRef = database.getReference("movimientos_inventario")

    // ==================== AUTENTICACIÓN ====================

    suspend fun registrarUsuario(email: String, password: String, nombre: String, rol: String): Result<Usuario> {
        return try {
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(email: String, password: String): Result<Usuario> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val userId = result.user?.uid ?: throw Exception("Error al obtener ID de usuario")

            val snapshot = usuariosRef.child(userId).get().await()
            val usuario = snapshot.getValue(Usuario::class.java)
                ?: throw Exception("Usuario no encontrado")

            Result.success(usuario)
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
            val productoId = productosRef.push().key ?: throw Exception("Error al generar ID")
            producto.id = productoId
            productosRef.child(productoId).setValue(producto).await()
            Result.success(productoId)
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

    suspend fun actualizarStock(productoId: String, nuevoStock: Int): Result<Unit> {
        return try {
            productosRef.child(productoId).child("stock").setValue(nuevoStock).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== VENTAS ====================

    suspend fun registrarVenta(venta: Venta): Result<String> {
        return try {
            val ventaId = ventasRef.push().key ?: throw Exception("Error al generar ID")
            venta.id = ventaId
            ventasRef.child(ventaId).setValue(venta).await()
            Result.success(ventaId)
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

    suspend fun obtenerUsuarioPorId(userId: String): Result<Usuario> {
        return try {
            val snapshot = usuariosRef.child(userId).get().await()
            val usuario = snapshot.getValue(Usuario::class.java)
                ?: throw Exception("Usuario no encontrado")
            Result.success(usuario)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    // Eliminar ventas más antiguas de 30 días
    suspend fun eliminarVentasAntiguas(): Result<Int> {
        return try {
            val treintaDiasAtras = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val snapshot = ventasRef.get().await()
            var eliminadas = 0

            snapshot.children.forEach { ventaSnapshot ->
                val fecha = ventaSnapshot.child("fecha").getValue(Long::class.java) ?: 0L
                if (fecha < treintaDiasAtras) {
                    ventaSnapshot.ref.removeValue().await()
                    eliminadas++
                }
            }

            Result.success(eliminadas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener ventas de los últimos 30 días
    fun obtenerVentasUltimoMes(): Flow<List<Venta>> = callbackFlow {
        val treintaDiasAtras = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ventas = mutableListOf<Venta>()

                snapshot.children.forEach { ventaSnapshot ->
                    val venta = ventaSnapshot.getValue(Venta::class.java)
                    if (venta != null && venta.fecha >= treintaDiasAtras) {
                        venta.id = ventaSnapshot.key ?: ""
                        ventas.add(venta)
                    }
                }

                // Ordenar por fecha descendente (más reciente primero)
                ventas.sortByDescending { it.fecha }
                trySend(ventas)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ventasRef.addValueEventListener(listener)
        awaitClose { ventasRef.removeEventListener(listener) }
    }


}
