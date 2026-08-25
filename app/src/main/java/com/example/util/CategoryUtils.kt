package com.example.util

import java.util.Locale

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.US)) {
        "electronics" -> "📱"
        "appliances" -> "🔌"
        "home & kitchen" -> "🏠"
        "fashion" -> "👟"
        "clothing" -> "👕"
        "automotive" -> "🚗"
        "tools" -> "🛠️"
        "furniture" -> "🛋️"
        else -> "🧾"
    }
}
