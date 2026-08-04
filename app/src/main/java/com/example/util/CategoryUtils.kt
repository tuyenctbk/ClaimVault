package com.example.util

import java.util.Locale

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.US)) {
        "electronics" -> "📱"
        "home & kitchen" -> "🏠"
        "fashion" -> "👟"
        "automotive" -> "🚗"
        "tools" -> "🛠️"
        else -> "🧾"
    }
}
