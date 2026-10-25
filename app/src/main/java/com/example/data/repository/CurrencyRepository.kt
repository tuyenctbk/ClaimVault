package com.example.data.repository

import com.example.data.remote.FrankfurterApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CurrencyConversionResult(
    val originalAmount: Double,
    val fromCurrency: String,
    val targetAmount: Double,
    val targetCurrency: String,
    val rate: Double,
    val date: String,
    val isOfflineFallback: Boolean = false
)

class CurrencyRepository(
    private val apiService: FrankfurterApiService = FrankfurterApiService.create()
) {

    // Fallback static conversion rates against USD (updated baseline)
    private val fallbackRatesToUsd = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 152.5,
        "CAD" to 1.38,
        "AUD" to 1.51,
        "CHF" to 0.86,
        "CNY" to 7.12,
        "INR" to 84.1,
        "VND" to 25350.0,
        "SGD" to 1.32,
        "KRW" to 1380.0,
        "BRL" to 5.68
    )

    fun normalizeCurrencyCode(raw: String): String {
        val trimmed = raw.trim().uppercase(Locale.US)
        return when (trimmed) {
            "$", "USD", "US$" -> "USD"
            "€", "EUR" -> "EUR"
            "£", "GBP" -> "GBP"
            "¥", "JPY", "JP¥" -> "JPY"
            "CA$", "CAD" -> "CAD"
            "A$", "AUD" -> "AUD"
            "CHF" -> "CHF"
            "CNY", "RMB", "元" -> "CNY"
            "INR", "₹" -> "INR"
            "VND", "₫" -> "VND"
            "S$", "SGD" -> "SGD"
            "KRW", "₩" -> "KRW"
            "BRL", "R$" -> "BRL"
            else -> if (trimmed.length == 3) trimmed else "USD"
        }
    }

    suspend fun convertCurrency(
        amount: Double,
        fromCurrencyRaw: String,
        targetCurrencyRaw: String,
        dateEpochMs: Long = System.currentTimeMillis()
    ): Result<CurrencyConversionResult> = withContext(Dispatchers.IO) {
        val fromCode = normalizeCurrencyCode(fromCurrencyRaw)
        val targetCode = normalizeCurrencyCode(targetCurrencyRaw)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = dateFormat.format(Date(dateEpochMs))

        if (fromCode == targetCode) {
            return@withContext Result.success(
                CurrencyConversionResult(
                    originalAmount = amount,
                    fromCurrency = fromCode,
                    targetAmount = amount,
                    targetCurrency = targetCode,
                    rate = 1.0,
                    date = dateStr,
                    isOfflineFallback = false
                )
            )
        }

        try {
            // Call Frankfurter historical endpoint (completely free, no API key needed)
            val response = apiService.getHistoricalRates(date = dateStr, base = fromCode, symbols = targetCode)
            if (response.isSuccessful) {
                val body = response.body()
                val rate = body?.rates?.get(targetCode)
                if (rate != null) {
                    val converted = amount * rate
                    return@withContext Result.success(
                        CurrencyConversionResult(
                            originalAmount = amount,
                            fromCurrency = fromCode,
                            targetAmount = converted,
                            targetCurrency = targetCode,
                            rate = rate,
                            date = body.date,
                            isOfflineFallback = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Network failure or unsupported symbol on API side: use offline calculation fallback
        }

        // Offline / Fallback conversion logic
        val rateFromUsd = fallbackRatesToUsd[fromCode] ?: 1.0
        val rateTargetUsd = fallbackRatesToUsd[targetCode] ?: 1.0

        // Calculate rate: amountInUsd = amount / rateFromUsd; converted = amountInUsd * rateTargetUsd
        val calculatedRate = rateTargetUsd / rateFromUsd
        val convertedAmount = amount * calculatedRate

        return@withContext Result.success(
            CurrencyConversionResult(
                originalAmount = amount,
                fromCurrency = fromCode,
                targetAmount = convertedAmount,
                targetCurrency = targetCode,
                rate = calculatedRate,
                date = "$dateStr (Offline Estimate)",
                isOfflineFallback = true
            )
        )
    }

    companion object {
        val SUPPORTED_CURRENCIES = listOf(
            "USD" to "$ (US Dollar)",
            "EUR" to "€ (Euro)",
            "GBP" to "£ (British Pound)",
            "JPY" to "¥ (Japanese Yen)",
            "CAD" to "CA$ (Canadian Dollar)",
            "AUD" to "A$ (Australian Dollar)",
            "CHF" to "CHF (Swiss Franc)",
            "CNY" to "¥ (Chinese Yuan)",
            "INR" to "₹ (Indian Rupee)",
            "VND" to "₫ (Vietnamese Dong)",
            "SGD" to "S$ (Singapore Dollar)",
            "KRW" to "₩ (South Korean Won)",
            "BRL" to "R$ (Brazilian Real)"
        )
    }
}
