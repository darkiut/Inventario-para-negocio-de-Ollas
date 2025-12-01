package com.negocio.inventarioollas.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.negocio.inventarioollas.models.DatosNegocio
import com.negocio.inventarioollas.models.Venta
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfGenerator(private val context: Context) {

    fun generarYEnviarPdf(venta: Venta, datosNegocio: DatosNegocio) {
        try {
            val nombreArchivo = "Venta_${venta.id}.pdf"
            val file = File(context.cacheDir, nombreArchivo)
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // ENCABEZADO
            document.add(Paragraph(venta.tipoDocumento.uppercase())
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(20f))

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            document.add(Paragraph("Fecha: ${dateFormat.format(Date(venta.fecha))}")
                .setTextAlignment(TextAlignment.RIGHT))

            // DATOS DEL NEGOCIO (DINÁMICOS)
            document.add(Paragraph(datosNegocio.nombre.uppercase()).setBold().setFontSize(14f))
            document.add(Paragraph(datosNegocio.direccion))
            if (datosNegocio.ruc.isNotBlank()) document.add(Paragraph("RUC: ${datosNegocio.ruc}"))
            if (datosNegocio.telefono.isNotBlank()) document.add(Paragraph("Tel: ${datosNegocio.telefono}"))
            document.add(Paragraph("Vendedor: ${venta.vendedorNombre}"))

            document.add(Paragraph("\nDatos del Cliente:"))
            document.add(Paragraph("Nombre: ${venta.clienteNombre}"))
            if (venta.clienteDni.isNotBlank()) document.add(Paragraph("DNI/RUC: ${venta.clienteDni}"))
            if (venta.clienteTelefono.isNotBlank()) document.add(Paragraph("Teléfono: ${venta.clienteTelefono}"))

            document.add(Paragraph("\n"))

            // TABLA
            val table = Table(UnitValue.createPercentArray(floatArrayOf(4f, 2f, 2f)))
            table.setWidth(UnitValue.createPercentValue(100f))
            table.addHeaderCell(Cell().add(Paragraph("Producto").setBold()))
            table.addHeaderCell(Cell().add(Paragraph("Cant.").setBold()))
            table.addHeaderCell(Cell().add(Paragraph("Total").setBold()))

            venta.productos.values.forEach { item ->
                table.addCell(Paragraph(item.nombre))
                table.addCell(Paragraph("${item.cantidad} x S/ ${item.precioUnitario}"))
                table.addCell(Paragraph("S/ ${String.format("%.2f", item.subtotal)}"))
            }
            document.add(table)

            // TOTAL
            document.add(Paragraph("\nTOTAL A PAGAR: S/ ${String.format("%.2f", venta.total)}")
                .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(16f))

            // MENSAJE FINAL
            document.add(Paragraph("\n${datosNegocio.mensajeFinal}")
                .setTextAlignment(TextAlignment.CENTER).setItalic())

            document.close()
            compartirPdf(file)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun compartirPdf(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Compartir Comprobante"))
        }
    }
}