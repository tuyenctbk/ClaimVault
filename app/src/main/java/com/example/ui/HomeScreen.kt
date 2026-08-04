package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.WarrantyItem
import com.example.ui.theme.AmberUrgent
import com.example.ui.theme.AmberUrgentContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedAlertContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.components.ClaimVaultHeroBanner
import com.example.ui.components.ClaimVaultLogo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ClaimVaultViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val filteredList by viewModel.filteredWarranties.collectAsStateWithLifecycle()
    val stats by viewModel.vaultStats.collectAsStateWithLifecycle()
    val allWarranties by viewModel.allWarranties.collectAsStateWithLifecycle()

    val now = System.currentTimeMillis()
    val dayMs = 86400000L

    // Expiring soon items (<= 30 days)
    val expiringItems = allWarranties.filter { item ->
        val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / dayMs).toInt()
        daysLeft in 0..30
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "ClaimVault Logo",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ClaimVault",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Privacy"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("SCAN NEW RECEIPT", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.Black,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
                    .testTag("scan_new_receipt_fab")
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 600.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats & Hero Banner
                item {
                    ClaimVaultHeroBanner(
                        stats = stats,
                        onAddClick = onNavigateToAdd,
                        onScanClick = onNavigateToAdd,
                        onShareClick = { viewModel.smartPromptManager.triggerShareDialog() }
                    )
                }

                // EXPIRING SOON HIGHLIGHT SECTION (Urgency-First)
                if (expiringItems.isNotEmpty()) {
                    item {
                        Text(
                            text = "EXPIRING SOON (${expiringItems.size} Item${if (expiringItems.size > 1) "s" else ""})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    if (isWide) {
                        val chunkedExpiring = expiringItems.chunked(2)
                        items(chunkedExpiring) { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExpiringHighlightCard(
                                        item = pair[0],
                                        onExportClaim = { viewModel.exportClaimPdf(context, pair[0]) },
                                        onClick = { onNavigateToDetail(pair[0].id) }
                                    )
                                }
                                if (pair.size > 1) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ExpiringHighlightCard(
                                            item = pair[1],
                                            onExportClaim = { viewModel.exportClaimPdf(context, pair[1]) },
                                            onClick = { onNavigateToDetail(pair[1].id) }
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        items(expiringItems, key = { "expiring_${it.id}" }) { item ->
                            ExpiringHighlightCard(
                                item = item,
                                onExportClaim = { viewModel.exportClaimPdf(context, item) },
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }
                }

            // Search Bar & Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "MY VAULT (${stats.totalCount} Warranties)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input"),
                        placeholder = { Text("Search by merchant, product, S/N...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary
                        )
                    )

                    // Category Filter Chips
                    val categories = listOf("All", "Electronics", "Home & Kitchen", "Fashion", "Automotive", "Tools", "Other")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.selectedCategory.value = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    // Filter Status Chips ("All", "Expiring Soon", "Active", "Expired")
                    val statuses = listOf("All", "Expiring Soon", "Active", "Expired")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(statuses) { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { viewModel.selectedStatus.value = st },
                                label = { Text(st) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Main Vault List
            if (filteredList.isEmpty()) {
                item {
                    EmptyVaultState(
                        isFiltered = searchQuery.isNotEmpty() || selectedCategory != "All" || selectedStatus != "All",
                        onResetFilters = {
                            viewModel.searchQuery.value = ""
                            viewModel.selectedCategory.value = "All"
                            viewModel.selectedStatus.value = "All"
                        }
                    )
                }
            } else {
                if (isWide) {
                    val chunkedList = filteredList.chunked(2)
                    items(chunkedList) { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                WarrantyVaultCard(
                                    item = pair[0],
                                    onClick = { onNavigateToDetail(pair[0].id) }
                                )
                            }
                            if (pair.size > 1) {
                                Box(modifier = Modifier.weight(1f)) {
                                    WarrantyVaultCard(
                                        item = pair[1],
                                        onClick = { onNavigateToDetail(pair[1].id) }
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(filteredList, key = { it.id }) { item ->
                        WarrantyVaultCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp)) // Fab padding
            }
        }
    }
}
}

@Composable
fun VaultHeaderStatsCard(stats: VaultStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL VALUE PROTECTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", stats.totalProtectedValue)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                }

                // 100% On-Device Badge
                Surface(
                    color = EmeraldLightContainer,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "100% On-Device",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatSubItem(label = "Active Warranties", value = "${stats.activeCount}")
                StatSubItem(label = "Expiring Soon", value = "${stats.expiringCount}", isUrgent = stats.expiringCount > 0)
                StatSubItem(label = "Total Items", value = "${stats.totalCount}")
            }
        }
    }
}

@Composable
fun StatSubItem(label: String, value: String, isUrgent: Boolean = false) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isUrgent) AmberUrgent else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ExpiringHighlightCard(
    item: WarrantyItem,
    onExportClaim: () -> Unit,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / 86400000L).toInt()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val expiryDateStr = dateFormat.format(Date(item.expiryDateEpoch))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("expiring_item_card_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = AmberUrgentContainer
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AmberUrgent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getCategoryEmoji(item.category),
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = AmberUrgent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "⏳ $daysLeft Days Left",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = "Expires $expiryDateStr • Purchased at ${item.merchant}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )

            Button(
                onClick = onExportClaim,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_claim_button_${item.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXPORT CLAIM PACKET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun WarrantyVaultCard(
    item: WarrantyItem,
    onClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val dayMs = 86400000L
    val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / dayMs).toInt()

    val (badgeBg, badgeFg, statusText) = when {
        item.warrantyDurationMonths == -1 -> Triple(EmeraldLightContainer, EmeraldDark, "Lifetime")
        daysLeft < 0 -> Triple(RedAlertContainer, RedAlert, "Expired")
        daysLeft <= 30 -> Triple(AmberUrgentContainer, AmberUrgent, "$daysLeft Days Left")
        else -> {
            val monthsLeft = daysLeft / 30
            if (monthsLeft >= 12) {
                Triple(EmeraldLightContainer, EmeraldDark, "${monthsLeft / 12} Yrs Left")
            } else {
                Triple(EmeraldLightContainer, EmeraldDark, "$monthsLeft Mos Left")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("warranty_card_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getCategoryEmoji(item.category),
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.merchant} • ${item.currency}${String.format(Locale.US, "%.2f", item.price)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg
                )
            }
        }
    }
}

@Composable
fun EmptyVaultState(isFiltered: Boolean, onResetFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = if (isFiltered) "No warranties match your filters" else "Your Vault is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isFiltered) "Try adjusting your search query or category filters." else "Snap a receipt or tap + to protect your first purchase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (isFiltered) {
            OutlinedButton(onClick = onResetFilters) {
                Text("Reset Filters")
            }
        }
    }
}

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
