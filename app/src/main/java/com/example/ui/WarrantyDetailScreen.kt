package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.AmberUrgent
import com.example.ui.theme.AmberUrgentContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedAlertContainer
import com.example.util.getCategoryEmoji
import com.example.util.AppLanguage
import com.example.util.localize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantyDetailScreen(
    viewModel: ClaimVaultViewModel,
    itemId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val itemState by viewModel.getWarrantyById(itemId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isImageZoomed by remember { mutableStateOf(false) }

    val item = itemState ?: return

    val now = System.currentTimeMillis()
    val dayMs = 86400000L
    val daysLeft = if (item.warrantyDurationMonths == -1) 9999 else ((item.expiryDateEpoch - now) / dayMs).toInt()

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    val purchaseStr = dateFormat.format(Date(item.purchaseDateEpoch))
    val expiryStr = if (item.warrantyDurationMonths == -1) "Lifetime".localize(appLanguage) else dateFormat.format(Date(item.expiryDateEpoch))

    val (statusBg, statusFg, statusTitle) = when {
        item.warrantyDurationMonths == -1 -> Triple(EmeraldLightContainer, EmeraldDark, "LIFETIME COVERAGE".localize(appLanguage))
        daysLeft < 0 -> Triple(RedAlertContainer, RedAlert, "EXPIRED COVERAGE".localize(appLanguage))
        daysLeft <= 30 -> Triple(AmberUrgentContainer, AmberUrgent, "EXPIRING SOON".localize(appLanguage) + " ($daysLeft " + "Days Remaining".localize(appLanguage) + ")")
        else -> Triple(EmeraldLightContainer, EmeraldDark, "ACTIVE COVERAGE".localize(appLanguage) + " ($daysLeft " + "Days Remaining".localize(appLanguage) + ")")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".localize(appLanguage))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(item.id) },
                        modifier = Modifier.testTag("edit_warranty_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit".localize(appLanguage))
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_warranty_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete".localize(appLanguage), tint = RedAlert)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Urgency & Status Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = statusBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusFg)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(getCategoryEmoji(item.category), fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusFg
                            )
                        }
                    }

                    if (item.warrantyDurationMonths != -1 && daysLeft >= 0) {
                        val totalDurationDays = (item.warrantyDurationMonths * 30).coerceAtLeast(1)
                        val progress = (daysLeft.toFloat() / totalDurationDays.toFloat()).coerceIn(0f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = statusFg,
                                trackColor = statusFg.copy(alpha = 0.2f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Purchased".localize(appLanguage) + " $purchaseStr", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
                                Text("Expires".localize(appLanguage) + " $expiryStr", style = MaterialTheme.typography.labelSmall, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ONE-TAP CLAIM EXPORT BUTTON
                    Button(
                        onClick = { viewModel.exportClaimPdf(context, item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("export_claim_pdf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EXPORT CLAIM PACKET (PDF / IMAGE BUNDLE)".localize(appLanguage), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Product Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("PRODUCT & CLAIM DETAILS".localize(appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)

                    DetailRow(label = "Store / Merchant".localize(appLanguage), value = item.merchant)
                    DetailRow(label = "Purchase Price".localize(appLanguage), value = "${item.currency}${String.format(Locale.US, "%.2f", item.price)}")
                    DetailRow(label = "Category".localize(appLanguage), value = item.category.localize(appLanguage))
                    DetailRow(
                        label = "Warranty Duration".localize(appLanguage),
                        value = if (item.warrantyDurationMonths == -1) "Lifetime".localize(appLanguage) else "${item.warrantyDurationMonths} " + "Months".localize(appLanguage)
                    )

                    // Model & Serial Number with One-Tap Copy
                    if (item.modelNumber.isNotBlank()) {
                        CopyableDetailRow(
                            context = context,
                            label = "Model Number".localize(appLanguage),
                            value = item.modelNumber,
                            appLanguage = appLanguage
                        )
                    }

                    if (item.serialNumber.isNotBlank()) {
                        CopyableDetailRow(
                            context = context,
                            label = "Serial Number".localize(appLanguage),
                            value = item.serialNumber,
                            appLanguage = appLanguage
                        )
                    }
                }
            }

            // Custom Tags Card
            if (item.tags.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("CUSTOM TAGS".localize(appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🏷️ $tag",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Pre-Expiry Checkup Alerts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pre-Expiry Checkup Alerts".localize(appLanguage), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Schedules local push reminders so you can inspect appliances/electronics for defects before coverage ends.".localize(appLanguage),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    ReminderRow(
                        title = "30 Days Before Expiry".localize(appLanguage),
                        enabled = item.reminder30Days,
                        onToggle = { enabled ->
                            viewModel.saveWarranty(
                                id = item.id,
                                title = item.title,
                                merchant = item.merchant,
                                category = item.category,
                                price = item.price,
                                purchaseDateEpoch = item.purchaseDateEpoch,
                                warrantyDurationMonths = item.warrantyDurationMonths,
                                serialNumber = item.serialNumber,
                                modelNumber = item.modelNumber,
                                receiptImagePath = item.receiptImagePath,
                                notes = item.notes,
                                reminder30Days = enabled,
                                reminder7Days = item.reminder7Days,
                                reminder1Day = item.reminder1Day,
                                tags = item.tags
                            )
                        }
                    )

                    ReminderRow(
                        title = "7 Days Before Expiry".localize(appLanguage),
                        enabled = item.reminder7Days,
                        onToggle = { enabled ->
                            viewModel.saveWarranty(
                                id = item.id,
                                title = item.title,
                                merchant = item.merchant,
                                category = item.category,
                                price = item.price,
                                purchaseDateEpoch = item.purchaseDateEpoch,
                                warrantyDurationMonths = item.warrantyDurationMonths,
                                serialNumber = item.serialNumber,
                                modelNumber = item.modelNumber,
                                receiptImagePath = item.receiptImagePath,
                                notes = item.notes,
                                reminder30Days = item.reminder30Days,
                                reminder7Days = enabled,
                                reminder1Day = item.reminder1Day,
                                tags = item.tags
                            )
                        }
                    )

                    ReminderRow(
                        title = "1 Day Before Expiry".localize(appLanguage),
                        enabled = item.reminder1Day,
                        onToggle = { enabled ->
                            viewModel.saveWarranty(
                                id = item.id,
                                title = item.title,
                                merchant = item.merchant,
                                category = item.category,
                                price = item.price,
                                purchaseDateEpoch = item.purchaseDateEpoch,
                                warrantyDurationMonths = item.warrantyDurationMonths,
                                serialNumber = item.serialNumber,
                                modelNumber = item.modelNumber,
                                receiptImagePath = item.receiptImagePath,
                                notes = item.notes,
                                reminder30Days = item.reminder30Days,
                                reminder7Days = item.reminder7Days,
                                reminder1Day = enabled,
                                tags = item.tags
                            )
                        }
                    )
                }
            }

            // Receipt Image Card
            if (!item.receiptImagePath.isNullOrBlank() && File(item.receiptImagePath).exists()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("PROOF OF PURCHASE RECEIPT".localize(appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { isImageZoomed = true }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(item.receiptImagePath)),
                                contentDescription = "Receipt Image".localize(appLanguage),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = "Tap image to view full size".localize(appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Notes / Repair Log
            if (item.notes.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("NOTES & REPAIR HISTORY".localize(appLanguage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                        Text(text = item.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Warranty?".localize(appLanguage)) },
            text = { Text("Are you sure you want to remove".localize(appLanguage) + " ${item.title} " + "from your ClaimVault? This action cannot be undone.".localize(appLanguage)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteWarranty(item)
                        onNavigateBack()
                    }
                ) {
                    Text("Delete".localize(appLanguage), color = RedAlert, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel".localize(appLanguage))
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CopyableDetailRow(context: Context, label: String, value: String, appLanguage: AppLanguage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }

        IconButton(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(label, value)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "$label " + "copied!".localize(appLanguage), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("copy_${label.lowercase(Locale.US).replace(" ", "_")}")
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy".localize(appLanguage) + " $label", modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
        }
    }
}

@Composable
fun ReminderRow(title: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
        )
    }
}
