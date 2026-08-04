package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class OcrResult(
    val merchant: String?,
    val totalAmount: Double?,
    val purchaseDateEpoch: Long?,
    val currency: String,
    val rawText: String
)

object OcrHelper {

    suspend fun saveAndEnhanceReceiptImage(
        context: Context,
        inputUri: Uri,
        applyFilter: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }

        val fileName = "receipt_${System.currentTimeMillis()}.jpg"
        val outputFile = File(receiptsDir, fileName)

        val inputStream = context.contentResolver.openInputStream(inputUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (originalBitmap == null) {
            throw IllegalArgumentException("Could not decode image from URI")
        }

        val processedBitmap = if (applyFilter) {
            enhanceReceiptBitmap(originalBitmap)
        } else {
            scaleBitmapIfNeeded(originalBitmap, 1200)
        }

        FileOutputStream(outputFile).use { out ->
            // Compress to JPEG at 80% quality -> usually < 250KB
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        outputFile.absolutePath
    }

    /**
     * Bumps contrast and sharpens faint receipt ink while reducing file size.
     */
    fun enhanceReceiptBitmap(src: Bitmap): Bitmap {
        val scaled = scaleBitmapIfNeeded(src, 1200)
        val width = scaled.width
        val height = scaled.height

        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()

        // High contrast grayscale filter matrix
        val cm = ColorMatrix(
            floatArrayOf(
                1.5f, 0f, 0f, 0f, -20f,
                0f, 1.5f, 0f, 0f, -20f,
                0f, 0f, 1.5f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return dest
    }

    private fun scaleBitmapIfNeeded(src: Bitmap, maxDimension: Int): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= maxDimension && height <= maxDimension) {
            return src
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
    }

    /**
     * Local pattern extractor for simulated / smart fallback OCR text parsing with currency auto-detection and locale fallback.
     */
    fun parseReceiptText(text: String, context: Context? = null): OcrResult {
        var merchant: String? = null
        var totalAmount: Double? = null
        var dateEpoch: Long? = null
        var detectedCurrency: String? = null

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // Common Merchants
        val knownMerchants = listOf(
            "Best Buy", "Apple Store", "Amazon", "Target", "Walmart",
            "Home Depot", "Costco", "Bed Bath & Beyond", "Dyson", "Sony Store",
            "IKEA", "Micro Center", "B&H Photo", "REI", "Nordstrom", "Sephora"
        )

        for (m in knownMerchants) {
            if (text.contains(m, ignoreCase = true)) {
                merchant = m
                break
            }
        }

        if (merchant == null && lines.isNotEmpty()) {
            merchant = lines.firstOrNull { it.length in 3..25 && !it.contains("receipt", ignoreCase = true) }
        }

        // Auto-detect currency symbol or code
        val currencyMap = mapOf(
            "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "INR" to "₹",
            "CAD" to "CA$", "AUD" to "AU$", "CHF" to "CHF", "CNY" to "¥", "BRL" to "R$",
            "SEK" to "kr", "NOK" to "kr", "DKK" to "kr", "ZAR" to "R"
        )
        for ((code, sym) in currencyMap) {
            if (text.contains(code, ignoreCase = true) || text.contains(sym)) {
                detectedCurrency = sym
                break
            }
        }

        if (detectedCurrency == null) {
            if (text.contains("$")) detectedCurrency = "$"
            else if (text.contains("€")) detectedCurrency = "€"
            else if (text.contains("£")) detectedCurrency = "£"
            else if (text.contains("¥")) detectedCurrency = "¥"
            else if (text.contains("₹")) detectedCurrency = "₹"
            else if (text.contains("kr", ignoreCase = true)) detectedCurrency = "kr"
        }

        // Default to device's locale setting if not found in text
        if (detectedCurrency == null) {
            detectedCurrency = try {
                val locale = if (context != null) {
                    context.resources.configuration.locales[0]
                } else {
                    Locale.getDefault()
                }
                val curr = java.util.Currency.getInstance(locale)
                curr?.symbol ?: "$"
            } catch (e: Exception) {
                "$"
            }
        }

        // Price regex (e.g. $199.99, TOTAL: 199.99)
        val pricePattern = Pattern.compile("(?:TOTAL|AMT|AMOUNT|BAL)?\\s*[$€£¥₹]?\\s*(\\d{1,4}\\.\\d{2})", Pattern.CASE_INSENSITIVE)
        val priceMatcher = pricePattern.matcher(text)
        var maxFoundPrice = 0.0

        while (priceMatcher.find()) {
            val pStr = priceMatcher.group(1)
            pStr?.toDoubleOrNull()?.let { price ->
                if (price > maxFoundPrice) {
                    maxFoundPrice = price
                }
            }
        }
        if (maxFoundPrice > 0.0) {
            totalAmount = maxFoundPrice
        }

        // Date regex (e.g., 08/15/2024, 2024-08-15)
        val datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        val dateMatcher = datePattern.matcher(text)
        if (dateMatcher.find()) {
            val dateStr = dateMatcher.group(1)
            dateStr?.let {
                dateEpoch = parseDateToEpoch(it)
            }
        }

        return OcrResult(
            merchant = merchant,
            totalAmount = totalAmount,
            purchaseDateEpoch = dateEpoch,
            currency = detectedCurrency,
            rawText = text
        )
    }

    private fun parseDateToEpoch(dateStr: String): Long? {
        val formats = listOf(
            "MM/dd/yyyy", "MM-dd-yyyy", "yyyy-MM-dd", "MM/dd/yy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (e: Exception) {
                // continue
            }
        }
        return null
    }
}
