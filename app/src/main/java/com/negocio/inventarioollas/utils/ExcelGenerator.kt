package com.negocio.inventarioollas.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.negocio.inventarioollas.models.Venta
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ExcelGenerator(private val context: Context) {

    fun generarReporteMensual(ventas: List<Venta>) {
        try {
            val fileName = "Reporte_Ventas_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)

            // Usamos .use para asegurar que el archivo se cierre y guarde correctamente
            file.outputStream().use { fileOut ->

                // 1. BOM para UTF-8 (Vital para tildes y Ñ en Excel)
                fileOut.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                // 2. Cabecera (Agregamos Hora y Productos)
                val header = "Fecha,Hora,Tipo Doc.,Cliente,DNI/RUC,Vendedor,Productos,Total (S/)\n"
                fileOut.write(header.toByteArray())

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                var granTotal = 0.0

                ventas.sortedBy { it.fecha }.forEach { venta ->
                    val line = StringBuilder()

                    // Columna 1: Fecha
                    line.append("${dateFormat.format(Date(venta.fecha))},")

                    // Columna 2: Hora
                    line.append("${timeFormat.format(Date(venta.fecha))},")

                    // Datos básicos (Sanitizados por si tienen comas)
                    line.append("${sanitizar(venta.tipoDocumento)},")
                    line.append("${sanitizar(venta.clienteNombre)},")

                    // Truco del apóstrofe para que Excel trate el DNI como texto (no borra ceros iniciales)
                    line.append("'${venta.clienteDni},")

                    line.append("${sanitizar(venta.vendedorNombre)},")

                    // Columna NUEVA: Resumen de qué se llevó
                    // Genera algo como: "2x Olla Grande | 1x Cucharón"
                    val detalleProductos = venta.productos.values.joinToString(" | ") {
                        "${it.cantidad}x ${it.nombre}"
                    }
                    line.append("${sanitizar(detalleProductos)},")

                    // Total (Formato inglés con punto para CSV estándar: 120.50)
                    line.append(String.format(Locale.US, "%.2f", venta.total))
                    line.append("\n")

                    fileOut.write(line.toString().toByteArray())
                    granTotal += venta.total
                }

                // 3. Fila de Total Final (Ajustamos las comas para que caiga bajo la columna Total)
                val footer = ",,,,,,,TOTAL GENERAL:,${String.format(Locale.US, "%.2f", granTotal)}\n"
                fileOut.write(footer.toByteArray())
            }

            // 4. Compartir
            compartirExcel(file)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Función auxiliar para limpiar textos:
    // Si un texto tiene comas o comillas, Excel se rompe si no hacemos esto.
    private fun sanitizar(texto: String): String {
        val textoLimpio = texto.replace("\"", "\"\"") // Escapar comillas dobles internas
        return "\"$textoLimpio\"" // Envolver todo en comillas
    }

    private fun compartirExcel(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val intentChooser = Intent.createChooser(intent, "Compartir Reporte Excel (CSV)")
        intentChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intentChooser)
    }
}