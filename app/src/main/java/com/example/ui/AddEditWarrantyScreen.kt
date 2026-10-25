package com.example.ui

import android.app.DatePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.repository.CurrencyRepository
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.util.AppLanguage
import com.example.util.localize
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWarrantyScreen(
    viewModel: ClaimVaultViewModel,
    editingItemId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCameraScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val allWarranties by viewModel.allWarranties.collectAsStateWithLifecycle()
    val isScanningOcr by viewModel.isScanningOcr.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()

    val isLookingUpBarcode by viewModel.isLookingUpBarcode.collectAsStateWithLifecycle()
    val barcodeSearchResult by viewModel.barcodeSearchResult.collectAsStateWithLifecycle()
    val isConvertingCurrency by viewModel.isConvertingCurrency.collectAsStateWithLifecycle()
    val currencyConversionState by viewModel.currencyConversionState.collectAsStateWithLifecycle()

    val editingItem = remember(editingItemId, allWarranties) {
        if (editingItemId != null && editingItemId > 0L) {
            allWarranties.find { it.id == editingItemId }
        } else null
    }

    var title by remember { mutableStateOf(editingItem?.title ?: "") }
    var merchant by remember { mutableStateOf(editingItem?.merchant ?: "") }
    var priceText by remember { mutableStateOf(editingItem?.price?.toString() ?: "") }
    var currency by remember { mutableStateOf(editingItem?.currency ?: try { java.util.Currency.getInstance(Locale.getDefault())?.symbol ?: "$" } catch(e: Exception) { "$" }) }
    var category by remember { mutableStateOf(editingItem?.category ?: "Electronics") }
    var warrantyDurationMonths by remember { mutableIntStateOf(editingItem?.warrantyDurationMonths ?: 12) }
    var serialNumber by remember { mutableStateOf(editingItem?.serialNumber ?: "") }
    var modelNumber by remember { mutableStateOf(editingItem?.modelNumber ?: "") }
    var notes by remember { mutableStateOf(editingItem?.notes ?: "") }
    var tagsInput by remember { mutableStateOf(editingItem?.tags ?: "") }
    var purchaseDateEpoch by remember { mutableLongStateOf(editingItem?.purchaseDateEpoch ?: System.currentTimeMillis()) }
    var receiptImagePath by remember { mutableStateOf<String?>(editingItem?.receiptImagePath) }

    var ocrDetectedText by remember { mutableStateOf<String?>(null) }

    // Barcode Lookup state
    var barcodeInput by remember { mutableStateOf("") }

    // Foreign Currency Converter Dialog State
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // Check if returned from CameraScanScreen
    LaunchedEffect(Unit) {
        if (viewModel.scannedReceiptPath != null) {
            receiptImagePath = viewModel.scannedReceiptPath
            if (!viewModel.scannedMerchant.isNullOrBlank() && merchant.isBlank()) {
                merchant = viewModel.scannedMerchant!!
            }
            if (viewModel.scannedAmount != null && viewModel.scannedAmount!! > 0.0 && priceText.isBlank()) {
                priceText = viewModel.scannedAmount.toString()
            }
            if (viewModel.scannedDateEpoch != null && viewModel.scannedDateEpoch!! > 0L) {
                purchaseDateEpoch = viewModel.scannedDateEpoch!!
            }
            if (viewModel.scannedCurrency != null && !viewModel.scannedCurrency.isNullOrBlank()) {
                currency = viewModel.scannedCurrency!!
            }
            if (!viewModel.scannedCategory.isNullOrBlank()) {
                category = viewModel.scannedCategory!!
            }
            ocrDetectedText = "CameraX ML Kit OCR: ${viewModel.scannedMerchant ?: "Receipt"}, ${currency}${viewModel.scannedAmount ?: 0.0} (${viewModel.scannedCategory ?: "Electronics"})"

            viewModel.scannedReceiptPath = null
            viewModel.scannedMerchant = null
            viewModel.scannedAmount = null
            viewModel.scannedDateEpoch = null
            viewModel.scannedCurrency = null
            viewModel.scannedCategory = null
        }
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.scanReceiptImage(context, it) { path, ocrResult ->
                if (path != null) {
                    receiptImagePath = path
                }
                if (ocrResult != null) {
                    ocrDetectedText = "Detected: ${ocrResult.merchant ?: "Receipt"}, ${ocrResult.currency}${ocrResult.totalAmount ?: "0.00"} (${ocrResult.suggestedCategory})"
                    if (merchant.isBlank() && !ocrResult.merchant.isNullOrBlank()) {
                        merchant = ocrResult.merchant
                    }
                    if (priceText.isBlank() && ocrResult.totalAmount != null) {
                        priceText = ocrResult.totalAmount.toString()
                    }
                    if (ocrResult.purchaseDateEpoch != null) {
                        purchaseDateEpoch = ocrResult.purchaseDateEpoch
                    }
                    currency = ocrResult.currency
                    category = ocrResult.suggestedCategory
                }
            }
        }
    }

    // Date Picker Dialog
    val calendar = Calendar.getInstance().apply { timeInMillis = purchaseDateEpoch }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            purchaseDateEpoch = cal.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingItem != null) "Edit Warranty".localize(appLanguage) else "Quick Add Receipt".localize(appLanguage),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back".localize(appLanguage)
                        )
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

            // Step 1: Receipt Photo Capture Card
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (receiptImagePath != null && File(receiptImagePath!!).exists()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(File(receiptImagePath!!)),
                                contentDescription = "Receipt Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Compressed < 300KB".localize(appLanguage),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = EmeraldDark,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Snap or Select Receipt Photo".localize(appLanguage),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldDark
                                )
                                Text(
                                    text = "Auto-OCR extracts merchant & date instantly".localize(appLanguage),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToCameraScan() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("camera_scan_receipt_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan CameraX".localize(appLanguage))
                        }

                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_receipt_photo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Gallery".localize(appLanguage))
                        }
                    }

                    if (isScanningOcr) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Running local OCR text enhancement...",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark
                            )
                        }
                    }

                    if (ocrDetectedText != null) {
                        Surface(
                            color = EmeraldLightContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ocrDetectedText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 100% FREE OPEN BARCODE LOOKUP CARD (UPC/EAN)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("barcode_lookup_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "UPC / EAN Barcode Auto-Fill".localize(appLanguage),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(color = EmeraldLightContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "100% Free Open API".localize(appLanguage),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { barcodeInput = it },
                            label = { Text("Product Barcode".localize(appLanguage)) },
                            placeholder = { Text("e.g. 012345678905") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_barcode"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                        )

                        Button(
                            onClick = {
                                viewModel.lookupBarcode(barcodeInput) { prod ->
                                    if (prod != null) {
                                        if (title.isBlank() || title.startsWith("Item UPC")) title = prod.title
                                        if (merchant.isBlank() || merchant == "Store") merchant = prod.brand
                                        category = prod.category
                                        if (modelNumber.isBlank()) modelNumber = prod.modelNumber
                                    }
                                }
                            },
                            enabled = barcodeInput.isNotBlank() && !isLookingUpBarcode,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                            modifier = Modifier.testTag("barcode_lookup_button")
                        ) {
                            if (isLookingUpBarcode) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Lookup".localize(appLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (barcodeSearchResult != null) {
                        Surface(
                            color = EmeraldLightContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-filled: ${barcodeSearchResult!!.brand} ${barcodeSearchResult!!.title}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Step 2: Product & Merchant Info
            Text(
                text = "Product Information".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Product Title *".localize(appLanguage)) },
                placeholder = { Text("e.g., Sony WH-1000XM5, LG OLED TV".localize(appLanguage)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_product_title"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Store / Merchant *".localize(appLanguage)) },
                    placeholder = { Text("e.g. Best Buy".localize(appLanguage)) },
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("input_merchant"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text("Curr".localize(appLanguage)) },
                    placeholder = { Text("$") },
                    modifier = Modifier
                        .width(75.dp)
                        .testTag("input_currency"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price".localize(appLanguage)) },
                    placeholder = { Text("399.99") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("input_price"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )
            }

            // FRANKFURTER API CURRENCY CONVERTER ACTION BUTTON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showCurrencyDialog = true },
                    modifier = Modifier.testTag("open_currency_converter_button")
                ) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = EmeraldDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Bought abroad? Convert currency (Frankfurter API)".localize(appLanguage),
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Purchase Date Picker
            OutlinedTextField(
                value = dateFormat.format(Date(purchaseDateEpoch)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Purchase Date".localize(appLanguage)) },
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date".localize(appLanguage))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() }
                    .testTag("purchase_date_picker"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
            )

            // Category Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category".localize(appLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (ocrDetectedText != null || barcodeSearchResult != null) {
                    Surface(
                        color = EmeraldLightContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Auto-Suggested",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            CategoryDropdown(
                selectedCategory = category,
                appLanguage = appLanguage,
                onCategorySelected = { category = it }
            )

            // Quick Category Chips
            val quickCategories = listOf("Electronics", "Appliances", "Home & Kitchen", "Fashion", "Automotive", "Tools", "Furniture")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(quickCategories) { cat ->
                    val isSelected = category.equals(cat, ignoreCase = true)
                    Surface(
                        modifier = Modifier.clickable { category = cat },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "${com.example.util.getCategoryEmoji(cat)} ${cat.localize(appLanguage)}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Warranty Duration Selector
            Text(
                text = "Warranty Duration".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    12 to "1 Year".localize(appLanguage),
                    24 to "2 Years".localize(appLanguage),
                    36 to "3 Years".localize(appLanguage),
                    -1 to "Lifetime".localize(appLanguage)
                ).forEach { (months, label) ->
                    val isSelected = warrantyDurationMonths == months
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { warrantyDurationMonths = months }
                            .testTag("duration_chip_$months"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Serial & Model Numbers
            Text(
                text = "Claim Proof Identifiers (Optional)".localize(appLanguage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = modelNumber,
                    onValueChange = { modelNumber = it },
                    label = { Text("Model Number".localize(appLanguage)) },
                    placeholder = { Text("WH1000XM5/B") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_model_number"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )

                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Serial Number".localize(appLanguage)) },
                    placeholder = { Text("SN-982140") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_serial_number"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & Service History".localize(appLanguage)) },
                placeholder = { Text("Coverage details, store receipt notes, repair history...".localize(appLanguage)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .testTag("input_notes"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Custom Tags (comma separated)".localize(appLanguage)) },
                    placeholder = { Text("e.g. Tax, Business, Priority".localize(appLanguage)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_tags"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )

                val suggestions = listOf("Tax", "Business", "Personal", "Priority", "Warranty", "Gift")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(suggestions) { sug ->
                        Surface(
                            modifier = Modifier.clickable {
                                val currentTags = tagsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                if (!currentTags.contains(sug)) {
                                    tagsInput = if (tagsInput.isBlank()) sug else "$tagsInput, $sug"
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "+ $sug",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val price = priceText.toDoubleOrNull() ?: 0.0
                        viewModel.saveWarranty(
                            id = editingItem?.id ?: 0L,
                            title = title.trim(),
                            merchant = if (merchant.isBlank()) "Store" else merchant.trim(),
                            category = category,
                            price = price,
                            purchaseDateEpoch = purchaseDateEpoch,
                            warrantyDurationMonths = warrantyDurationMonths,
                            serialNumber = serialNumber.trim(),
                            modelNumber = modelNumber.trim(),
                            receiptImagePath = receiptImagePath,
                            notes = notes.trim(),
                            reminder30Days = editingItem?.reminder30Days ?: true,
                            reminder7Days = editingItem?.reminder7Days ?: true,
                            reminder1Day = editingItem?.reminder1Day ?: true,
                            tags = tagsInput.trim(),
                            currency = currency.trim().ifBlank { "$" }
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_warranty_button"),
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (editingItem != null) "UPDATE VAULT ITEM".localize(appLanguage) else "SAVE TO CLAIMVAULT".localize(appLanguage),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Frankfurter API Currency Converter Dialog
        if (showCurrencyDialog) {
            CurrencyConverterDialog(
                purchaseDateEpoch = purchaseDateEpoch,
                currentPrice = priceText.toDoubleOrNull() ?: 0.0,
                currentCurrency = currency,
                appLanguage = appLanguage,
                isConverting = isConvertingCurrency,
                conversionResult = currencyConversionState,
                onConvert = { amt, fromCode, targetCode ->
                    viewModel.convertCurrency(amt, fromCode, targetCode, purchaseDateEpoch)
                },
                onApply = { convertedAmt, targetCode ->
                    priceText = String.format(Locale.US, "%.2f", convertedAmt)
                    currency = targetCode
                    showCurrencyDialog = false
                    viewModel.clearCurrencyConversion()
                },
                onDismiss = {
                    showCurrencyDialog = false
                    viewModel.clearCurrencyConversion()
                }
            )
        }
    }
}
}

@Composable
fun CurrencyConverterDialog(
    purchaseDateEpoch: Long,
    currentPrice: Double,
    currentCurrency: String,
    appLanguage: AppLanguage,
    isConverting: Boolean,
    conversionResult: com.example.data.repository.CurrencyConversionResult?,
    onConvert: (Double, String, String) -> Unit,
    onApply: (Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf(if (currentPrice > 0.0) currentPrice.toString() else "100.00") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var targetCurrency by remember { mutableStateOf("EUR") }

    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(purchaseDateEpoch))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("currency_converter_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CurrencyExchange, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Frankfurter Currency Converter".localize(appLanguage),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Convert foreign receipt price on purchase date ($dateStr) using 100% free European Central Bank rates.".localize(appLanguage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount".localize(appLanguage)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("foreign_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fromCurrency,
                        onValueChange = { fromCurrency = it.uppercase(Locale.US) },
                        label = { Text("From (e.g. USD)".localize(appLanguage)) },
                        modifier = Modifier.weight(1f).testTag("from_currency_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                    )

                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = EmeraldPrimary)

                    OutlinedTextField(
                        value = targetCurrency,
                        onValueChange = { targetCurrency = it.uppercase(Locale.US) },
                        label = { Text("To (e.g. EUR)".localize(appLanguage)) },
                        modifier = Modifier.weight(1f).testTag("target_currency_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
                    )
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        onConvert(amt, fromCurrency, targetCurrency)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("fetch_exchange_rate_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                    enabled = amountText.toDoubleOrNull() != null && !isConverting
                ) {
                    if (isConverting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Text("Calculate Rate via Frankfurter API".localize(appLanguage), fontWeight = FontWeight.Bold)
                    }
                }

                if (conversionResult != null) {
                    Surface(
                        color = EmeraldLightContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "${conversionResult.originalAmount} ${conversionResult.fromCurrency} = ${String.format(Locale.US, "%.2f", conversionResult.targetAmount)} ${conversionResult.targetCurrency}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark
                            )
                            Text(
                                text = "Rate: 1 ${conversionResult.fromCurrency} = ${conversionResult.rate} ${conversionResult.targetCurrency} (${conversionResult.date})",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDark
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onApply(conversionResult.targetAmount, conversionResult.targetCurrency) },
                                modifier = Modifier.fillMaxWidth().testTag("apply_converted_rate_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark, contentColor = Color.White)
                            ) {
                                Text("APPLY TO RECEIPT PRICE".localize(appLanguage), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel".localize(appLanguage))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
    selectedCategory: String,
    appLanguage: AppLanguage,
    onCategorySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val categories = listOf("Electronics", "Appliances", "Home & Kitchen", "Fashion", "Clothing", "Automotive", "Tools", "Furniture", "Other")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedCategory.localize(appLanguage),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("category_dropdown"),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldPrimary)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.localize(appLanguage)) },
                    onClick = {
                        onCategorySelected(cat)
                        expanded = false
                    }
                )
            }
        }
    }
}
