package com.negocio.inventarioollas.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.negocio.inventarioollas.models.Venta
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator(private val context: Context) {

    private val pageWidth = 595  // A4 width
    private val pageHeight = 842 // A4 height
    private val margin = 50f

    fun generarPDF(venta: Venta, tipoDocumento: String = "boleta"): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPosition = margin

            // Paint para títulos
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                isFakeBoldText = true
            }

            // Paint para subtítulos
            val subtitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                isFakeBoldText = true
            }

            // Paint para texto normal
            val normalPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
            }

            // Paint para líneas
            val linePaint = Paint().apply {
                color = Color.GRAY
                strokeWidth = 1f
            }

            // Encabezado
            val titulo = when (tipoDocumento) {
                "factura" -> "FACTURA"
                "nota_pedido" -> "NOTA DE PEDIDO"
                else -> "BOLETA DE VENTA"
            }

            canvas.drawText(titulo, margin, yPosition, titlePaint)
            yPosition += 40f

            // Información del negocio
            canvas.drawText("INVENTARIO OLLAS", margin, yPosition, subtitlePaint)
            yPosition += 25f
            canvas.drawText("RUC: 20123456789", margin, yPosition, normalPaint)
            yPosition += 20f
            canvas.drawText("Dirección: Av. Principal 123, Arequipa", margin, yPosition, normalPaint)
            yPosition += 20f
            canvas.drawText("Teléfono: (054) 123-4567", margin, yPosition, normalPaint)
            yPosition += 30f

            // Línea separadora
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 20f

            // Información de la venta
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fecha = dateFormat.format(Date(venta.fecha))

            canvas.drawText("Fecha: $fecha", margin, yPosition, normalPaint)
            yPosition += 20f
            canvas.drawText("Vendedor: ${venta.vendedorNombre}", margin, yPosition, normalPaint)
            yPosition += 30f

            // Datos del cliente
            canvas.drawText("DATOS DEL CLIENTE", margin, yPosition, subtitlePaint)
            yPosition += 25f
            canvas.drawText("Cliente: ${venta.clienteNombre}", margin, yPosition, normalPaint)
            yPosition += 20f

            if (venta.clienteDni.isNotBlank()) {
                canvas.drawText("DNI: ${venta.clienteDni}", margin, yPosition, normalPaint)
                yPosition += 20f
            }

            if (venta.clienteRuc.isNotBlank()) {
                canvas.drawText("RUC: ${venta.clienteRuc}", margin, yPosition, normalPaint)
                yPosition += 20f
            }

            canvas.drawText("Teléfono: ${venta.clienteTelefono}", margin, yPosition, normalPaint)
            yPosition += 20f

            if (venta.clienteDireccion.isNotBlank()) {
                canvas.drawText("Dirección: ${venta.clienteDireccion}", margin, yPosition, normalPaint)
                yPosition += 20f
            }

            yPosition += 10f

            // Línea separadora
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 20f

            // Encabezado de tabla
            canvas.drawText("DETALLE DE PRODUCTOS", margin, yPosition, subtitlePaint)
            yPosition += 25f

            canvas.drawText("Producto", margin, yPosition, normalPaint)
            canvas.drawText("Cant.", 300f, yPosition, normalPaint)
            canvas.drawText("P. Unit.", 370f, yPosition, normalPaint)
            canvas.drawText("Subtotal", 470f, yPosition, normalPaint)
            yPosition += 5f

            // Línea debajo de encabezado
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 15f

            // Productos
            venta.productos.values.forEach { item ->
                canvas.drawText(item.nombre, margin, yPosition, normalPaint)
                canvas.drawText(item.cantidad.toString(), 310f, yPosition, normalPaint)
                canvas.drawText("S/ ${String.format("%.2f", item.precioUnitario)}", 370f, yPosition, normalPaint)
                canvas.drawText("S/ ${String.format("%.2f", item.subtotal)}", 470f, yPosition, normalPaint)
                yPosition += 20f
            }

            yPosition += 10f

            // Línea antes del total
            canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
            yPosition += 25f

            // Total
            val totalPaint = Paint().apply {
                color = Color.BLACK
                textSize = 18f
                isFakeBoldText = true
            }

            canvas.drawText("TOTAL:", 370f, yPosition, totalPaint)
            canvas.drawText("S/ ${String.format("%.2f", venta.total)}", 470f, yPosition, totalPaint)

            yPosition += 40f

            // Pie de página
            canvas.drawText("¡Gracias por su compra!", margin, yPosition, normalPaint)

            // Finalizar página
            pdfDocument.finishPage(page)

            // Guardar archivo
            val fileName = "venta_${venta.clienteNombre.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            // Crear directorio si no existe
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            pdfDocument.close()

            // Mostrar mensaje de éxito
            Toast.makeText(context, "PDF generado: ${file.absolutePath}", Toast.LENGTH_LONG).show()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }
}
