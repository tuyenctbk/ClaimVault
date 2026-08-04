package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.WarrantyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfClaimExporter {

    suspend fun generateClaimPdf(context: Context, item: WarrantyItem): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 22f
            isFakeBoldText = true
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 14f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 12f
        }
        val boldBodyPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            isFakeBoldText = true
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
        }

        // Draw Header Banner
        canvas.drawRect(0f, 0f, 595f, 90f, headerBgPaint)

        canvas.drawText("ClaimVault Warranty Proof Packet", 30f, 40f, titlePaint)
        canvas.drawText("Stored 100% Locally • Offical Proof-of-Purchase Bundle", 30f, 65f, subTitlePaint)

        var yPos = 120f

        // Draw Details Box
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val purchaseStr = dateFormat.format(Date(item.purchaseDateEpoch))
        val expiryStr = if (item.warrantyDurationMonths == -1) "Lifetime Warranty" else dateFormat.format(Date(item.expiryDateEpoch))

        val now = System.currentTimeMillis()
        val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / 86400000L).toInt()

        val statusText = when {
            item.warrantyDurationMonths == -1 -> "LIFETIME WARRANTY"
            daysLeft < 0 -> "EXPIRED ($daysLeft days ago)"
            daysLeft <= 30 -> "EXPIRING SOON ($daysLeft Days Remaining)"
            else -> "ACTIVE ($daysLeft Days Remaining)"
        }

        val statusColor = when {
            daysLeft < 0 -> Color.parseColor("#DC2626")
            daysLeft <= 30 -> Color.parseColor("#D97706")
            else -> Color.parseColor("#059669")
        }

        val statusPaint = Paint().apply {
            color = statusColor
            textSize = 13f
            isFakeBoldText = true
        }

        // Section: Item Metadata
        canvas.drawText("PRODUCT INFORMATION", 30f, yPos, boldBodyPaint)
        yPos += 8f
        canvas.drawLine(30f, yPos, 565f, yPos, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        yPos += 24f

        canvas.drawText("Item Title:", 30f, yPos, boldBodyPaint)
        canvas.drawText(item.title, 140f, yPos, bodyPaint)

        canvas.drawText("Merchant:", 320f, yPos, boldBodyPaint)
        canvas.drawText(item.merchant, 410f, yPos, bodyPaint)
        yPos += 22f

        canvas.drawText("Category:", 30f, yPos, boldBodyPaint)
        canvas.drawText(item.category, 140f, yPos, bodyPaint)

        canvas.drawText("Purchase Price:", 320f, yPos, boldBodyPaint)
        canvas.drawText("${item.currency}${String.format(Locale.US, "%.2f", item.price)}", 410f, yPos, bodyPaint)
        yPos += 22f

        canvas.drawText("Purchase Date:", 30f, yPos, boldBodyPaint)
        canvas.drawText(purchaseStr, 140f, yPos, bodyPaint)

        canvas.drawText("Expiry Date:", 320f, yPos, boldBodyPaint)
        canvas.drawText(expiryStr, 410f, yPos, bodyPaint)
        yPos += 22f

        canvas.drawText("Model Number:", 30f, yPos, boldBodyPaint)
        canvas.drawText(if (item.modelNumber.isNotBlank()) item.modelNumber else "N/A", 140f, yPos, bodyPaint)

        canvas.drawText("Serial Number:", 320f, yPos, boldBodyPaint)
        canvas.drawText(if (item.serialNumber.isNotBlank()) item.serialNumber else "N/A", 410f, yPos, bodyPaint)
        yPos += 22f

        canvas.drawText("Claim Status:", 30f, yPos, boldBodyPaint)
        canvas.drawText(statusText, 140f, yPos, statusPaint)
        yPos += 30f

        if (item.notes.isNotBlank()) {
            canvas.drawText("NOTES & REPAIR HISTORY", 30f, yPos, boldBodyPaint)
            yPos += 8f
            canvas.drawLine(30f, yPos, 565f, yPos, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
            yPos += 20f

            canvas.drawText(item.notes, 30f, yPos, bodyPaint)
            yPos += 35f
        }

        // Section: Attached Receipt
        canvas.drawText("RECEIPT PROOF OF PURCHASE", 30f, yPos, boldBodyPaint)
        yPos += 8f
        canvas.drawLine(30f, yPos, 565f, yPos, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
        yPos += 20f

        if (!item.receiptImagePath.isNullOrBlank() && File(item.receiptImagePath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(item.receiptImagePath)
                if (bitmap != null) {
                    val availableWidth = 535f
                    val availableHeight = 812f - yPos
                    val scale = Math.min(availableWidth / bitmap.width, availableHeight / bitmap.height)
                    val drawWidth = (bitmap.width * scale).toInt()
                    val drawHeight = (bitmap.height * scale).toInt()

                    val destRect = Rect(30, yPos.toInt(), 30 + drawWidth, yPos.toInt() + drawHeight)
                    canvas.drawBitmap(bitmap, null, destRect, null)
                }
            } catch (e: Exception) {
                canvas.drawText("[Receipt image attached: ${item.receiptImagePath}]", 30f, yPos, bodyPaint)
            }
        } else {
            canvas.drawText("[No physical receipt photo attached - Digital Claim Packet Record]", 30f, yPos, bodyPaint)
        }

        pdfDocument.finishPage(page)

        val claimsDir = File(context.cacheDir, "claims")
        if (!claimsDir.exists()) claimsDir.mkdirs()

        val pdfFile = File(claimsDir, "Claim_${item.title.replace(" ", "_")}.pdf")
        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        pdfFile
    }

    fun shareClaimPdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Warranty Claim Packet - ${pdfFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Attached is the official proof of purchase and warranty claim packet generated by ClaimVault.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Export Warranty Claim Package")
        context.startActivity(chooser)
    }
}
