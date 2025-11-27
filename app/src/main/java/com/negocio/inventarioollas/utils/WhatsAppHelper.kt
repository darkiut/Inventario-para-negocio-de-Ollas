package com.negocio.inventarioollas.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object WhatsAppHelper {

    fun enviarPDFPorWhatsApp(
        context: Context,
        file: File,
        numeroTelefono: String,
        mensaje: String = "Gracias por su compra"
    ) {
        try {
            // Limpiar el número de teléfono
            val numeroLimpio = numeroTelefono.replace("+", "")
                .replace(" ", "")
                .replace("-", "")

            // Obtener URI del archivo usando FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Intent para compartir
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, mensaje)
                putExtra("jid", "$numeroLimpio@s.whatsapp.net")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla WhatsApp, intentar con cualquier app
            compartirPDF(context, file, mensaje)
        }
    }

    private fun compartirPDF(context: Context, file: File, mensaje: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, mensaje)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Compartir PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
