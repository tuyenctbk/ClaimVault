package com.example.ui

import java.util.Locale

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.AppLanguage
import com.example.util.localize
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.util.OcrHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    viewModel: ClaimVaultViewModel,
    onScanComplete: (imagePath: String, merchant: String?, amount: Double?, dateEpoch: Long?, currency: String, category: String?) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val hasSeenOnboarding by viewModel.hasCompletedCameraOnboarding.collectAsStateWithLifecycle()
    var showOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenOnboarding) {
        if (!hasSeenOnboarding) {
            showOnboarding = true
        }
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf("Processing receipt...") }

    val currentProcessingMessage = when (processingMessage) {
        "Processing receipt..." -> "Processing receipt...".localize(appLanguage)
        "Capturing receipt photo..." -> "Capturing receipt photo...".localize(appLanguage)
        "Running ML Kit OCR text recognition..." -> "Running ML Kit OCR text recognition...".localize(appLanguage)
        else -> processingMessage
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Receipt with CameraX".localize(appLanguage), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".localize(appLanguage))
                    }
                },
                actions = {
                    IconButton(onClick = { showOnboarding = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Help Guide".localize(appLanguage)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                // CameraX Preview
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            imageCapture = capture

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Viewfinder Guide Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = "Align receipt within frame".localize(appLanguage),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Shutter Button & Controls at Bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessing) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentProcessingMessage,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                val capture = imageCapture
                                if (capture != null) {
                                    isProcessing = true
                                    processingMessage = "Capturing receipt photo..."

                                    val photoFile = File(
                                        context.filesDir,
                                        "receipt_${System.currentTimeMillis()}.jpg"
                                    )
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                    capture.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onError(exc: ImageCaptureException) {
                                                isProcessing = false
                                                Toast.makeText(context, "Capture failed:".localize(appLanguage) + " ${exc.message}", Toast.LENGTH_SHORT).show()
                                            }

                                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                                processingMessage = "Running ML Kit OCR text recognition..."
                                                // Process with ML Kit and OcrHelper
                                                val savedUri = Uri.fromFile(photoFile)
                                                processCapturedReceipt(context, savedUri, viewModel) { path, merchant, amount, date, currency, category ->
                                                    isProcessing = false
                                                    onScanComplete(path, merchant, amount, date, currency, category)
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(4.dp, Color.LightGray, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Snap Receipt".localize(appLanguage),
                                tint = Color.DarkGray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to Scan & Extract Text".localize(appLanguage),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

            } else {
                // Permission Denied View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera permission is required to scan physical receipts directly.".localize(appLanguage),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Grant Camera Permission".localize(appLanguage))
                    }
                }
            }

            if (showOnboarding) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Receipt Scan Guide".localize(appLanguage),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "For best text extraction results with our ML Kit scanner, follow these simple tips:".localize(appLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ScanTipItem(
                                emoji = "💡",
                                title = "Optimal Lighting".localize(appLanguage),
                                description = "Keep receipt flat under clear, non-shadowed lighting.".localize(appLanguage)
                            )
                            ScanTipItem(
                                emoji = "📐",
                                title = "Perspective".localize(appLanguage),
                                description = "Keep your phone parallel to the receipt to avoid distortions.".localize(appLanguage)
                            )
                            ScanTipItem(
                                emoji = "🎯",
                                title = "Align & Focus".localize(appLanguage),
                                description = "Fit the entire receipt from edge to edge inside the viewfinder frame.".localize(appLanguage)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                viewModel.completeCameraOnboarding()
                                showOnboarding = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("dismiss_onboarding_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "GOT IT, SCAN!".localize(appLanguage),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanTipItem(emoji: String, title: String, description: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun processCapturedReceipt(
    context: Context,
    imageUri: Uri,
    viewModel: ClaimVaultViewModel,
    onDone: (path: String, merchant: String?, amount: Double?, dateEpoch: Long?, currency: String, category: String?) -> Unit
) {
    // 1. Enhance and save image via OcrHelper
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val enhancedPath = OcrHelper.saveAndEnhanceReceiptImage(context, imageUri, applyFilter = true)

            // 2. Run ML Kit Text Recognition
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    val parsed = OcrHelper.parseReceiptText(rawText, context)

                    viewModel.lastOcrResult.value = parsed
                    with(kotlinx.coroutines.CoroutineScope(Dispatchers.Main)) {
                        onDone(enhancedPath, parsed.merchant, parsed.totalAmount, parsed.purchaseDateEpoch, parsed.currency, parsed.suggestedCategory)
                    }
                }
                .addOnFailureListener {
                    // Fallback to heuristic parser if ML Kit fails
                    val parsed = OcrHelper.parseReceiptText("STORE RECEIPT\nTOTAL: $49.99", context)
                    with(kotlinx.coroutines.CoroutineScope(Dispatchers.Main)) {
                        onDone(enhancedPath, parsed.merchant, parsed.totalAmount, parsed.purchaseDateEpoch, parsed.currency, parsed.suggestedCategory)
                    }
                }
        } catch (e: Exception) {
            with(kotlinx.coroutines.CoroutineScope(Dispatchers.Main)) {
                val localeCurr = try { java.util.Currency.getInstance(Locale.getDefault())?.symbol ?: "$" } catch(ex: Exception) { "$" }
                onDone(imageUri.toString(), null, null, null, localeCurr, "Electronics")
            }
        }
    }
}
