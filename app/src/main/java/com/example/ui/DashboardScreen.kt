package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AmberUrgentContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.util.AppLanguage
import com.example.util.localize
import com.example.ui.theme.AmberUrgent
import com.example.util.getCategoryEmoji
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ClaimVaultViewModel,
    onNavigateBack: () -> Unit
) {
    val allWarranties by viewModel.allWarranties.collectAsStateWithLifecycle()
    val stats by viewModel.vaultStats.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    val totalSpending = allWarranties.sumOf { it.price }

    // Category breakdown
    val categoryMap = allWarranties.groupBy { it.category.ifBlank { "Other" } }
    val categoryColors = listOf(
        EmeraldPrimary,
        Color(0xFF3B82F6), // Blue
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6), // Purple
        Color(0xFF10B981)  // Teal
    )

    // Monthly spending (last 6 months)
    val monthlyData = mutableMapOf<String, Double>()
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    val monthKeys = mutableListOf<String>()
    val monthLabels = mutableListOf<String>()
    for (i in 5 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -i)
        val key = monthKeyFormat.format(cal.time)
        val label = dateFormat.format(cal.time).substring(0, 3)
        monthKeys.add(key)
        monthLabels.add(label)
        monthlyData[key] = 0.0
    }

    allWarranties.forEach { item ->
        val itemCal = Calendar.getInstance().apply { timeInMillis = item.purchaseDateEpoch }
        val key = monthKeyFormat.format(itemCal.time)
        if (monthlyData.containsKey(key)) {
            monthlyData[key] = (monthlyData[key] ?: 0.0) + item.price
        }
    }

    val monthlyValues = monthKeys.map { monthlyData[it] ?: 0.0 }
    val maxMonthlyValue = (monthlyValues.maxOrNull() ?: 1.0).let { if (it == 0.0) 1.0 else it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Analytics & Dashboard".localize(appLanguage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".localize(appLanguage))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SUMMARY CARDS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_spending"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Protected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$${String.format(Locale.US, "%.2f", totalSpending)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_active_items"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Active Items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${stats.activeCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // EXPIRING & EXPIRED STATS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_expiring_soon"),
                        colors = CardDefaults.cardColors(containerColor = AmberUrgentContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberUrgent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Expiring Soon".localize(appLanguage), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AmberUrgent)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${stats.expiringCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AmberUrgent)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_total_items"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Vault Items".localize(appLanguage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${stats.totalCount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // MONTHLY SPENDING BAR CHART CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bar_chart_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Monthly Spending Trends".localize(appLanguage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Purchase spending across the last 6 months".localize(appLanguage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Bar Chart Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 8.dp)
                                .testTag("monthly_bar_chart"),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barColor = EmeraldPrimary
                            val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            val bottomOffsetPx = with(density) { 30.dp.toPx() }
                            val strokeWidthPx = with(density) { 1.dp.toPx() }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = size.width
                                val height = size.height - bottomOffsetPx
                                val barWidth = (width / monthlyValues.size) * 0.5f
                                val spacing = (width / monthlyValues.size)

                                for (i in 0..3) {
                                    val y = (height / 3) * i
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(width, y),
                                        strokeWidth = strokeWidthPx
                                    )
                                }

                                monthlyValues.forEachIndexed { index, value ->
                                    val barHeight = (value / maxMonthlyValue).toFloat() * height
                                    val left = index * spacing + (spacing - barWidth) / 2
                                    val top = height - barHeight

                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                                    )
                                }
                            }
                        }

                        // X-Axis Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            monthLabels.forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // CIRCULAR / DONUT CHART FOR CATEGORY BREAKDOWN
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donut_chart_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DonutLarge, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Items by Category".localize(appLanguage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        if (allWarranties.isEmpty()) {
                            Text("No warranty items in vault yet.".localize(appLanguage), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        } else {
                            val totalItemsCount = allWarranties.size.toFloat()
                            val categoryEntries = categoryMap.entries.toList()
                            val strokeWidthPx = with(density) { 28.dp.toPx() }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Circular Donut Chart Canvas
                                Box(
                                    modifier = Modifier
                                        .size(160.dp)
                                        .testTag("category_donut_chart"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val canvasSize = size.minDimension
                                        val radius = (canvasSize - strokeWidthPx) / 2
                                        val center = Offset(canvasSize / 2, canvasSize / 2)

                                        var startAngle = -90f
                                        categoryEntries.forEachIndexed { index, entry ->
                                            val count = entry.value.size
                                            val sweepAngle = (count / totalItemsCount) * 360f
                                            val color = categoryColors[index % categoryColors.size]

                                            drawArc(
                                                color = color,
                                                startAngle = startAngle,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                topLeft = Offset(center.x - radius, center.y - radius),
                                                size = Size(radius * 2, radius * 2),
                                                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Butt)
                                            )
                                            startAngle += sweepAngle
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${allWarranties.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        Text("Items".localize(appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Legend
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categoryEntries.forEachIndexed { index, entry ->
                                        val catName = entry.key
                                        val count = entry.value.size
                                        val percentage = (count / totalItemsCount) * 100
                                        val color = categoryColors[index % categoryColors.size]
                                        val emoji = getCategoryEmoji(catName)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "$emoji " + catName.localize(appLanguage),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "$count (${String.format(Locale.US, "%.0f", percentage)}%)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
