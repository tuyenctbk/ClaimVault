package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.AddEditWarrantyScreen
import com.example.ui.CameraScanScreen
import com.example.ui.ClaimVaultViewModel
import com.example.ui.DashboardScreen
import com.example.ui.HomeScreen
import com.example.ui.OnboardingScreen
import com.example.ui.SettingsScreen
import com.example.ui.WarrantyDetailScreen
import com.example.ui.components.RateAppDialog
import com.example.ui.components.ShareAppDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FirebaseManager
import com.example.util.ThemeMode

import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldPrimary
import com.example.util.BiometricAuthManager

class MainActivity : FragmentActivity() {

    private val viewModel: ClaimVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enqueue background WorkManager task to check for 30-day warranty expiries
        com.example.notification.WarrantyExpiryWorker.enqueuePeriodicExpiryCheck(applicationContext)
        com.example.notification.DailyExpiryCheckReceiver.scheduleNextDailyCheck(applicationContext)

        val targetItemId = intent.getLongExtra("target_item_id", -1L)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()
            val showRateDialog by viewModel.smartPromptManager.showRateDialog.collectAsStateWithLifecycle()
            val showShareDialog by viewModel.smartPromptManager.showShareDialog.collectAsStateWithLifecycle()

            var isBiometricAuthenticated by remember { mutableStateOf(false) }

            LaunchedEffect(isBiometricEnabled) {
                if (isBiometricEnabled && !isBiometricAuthenticated) {
                    BiometricAuthManager.promptBiometricAuth(
                        activity = this@MainActivity,
                        title = "Unlock ClaimVault",
                        subtitle = "Authenticate with Fingerprint or Face ID to view receipts",
                        onSuccess = {
                            isBiometricAuthenticated = true
                        },
                        onError = { errorMsg ->
                            Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isBiometricEnabled && !isBiometricAuthenticated) {
                        // Locked Biometric Overlay Screen
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Vault Locked",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "ClaimVault is Locked",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Biometric security is enabled to protect sensitive purchase receipts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        BiometricAuthManager.promptBiometricAuth(
                                            activity = this@MainActivity,
                                            title = "Unlock ClaimVault",
                                            subtitle = "Authenticate with Fingerprint or Face ID",
                                            onSuccess = { isBiometricAuthenticated = true },
                                            onError = { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
                                ) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Authenticate with Fingerprint / Face ID", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        val navController = rememberNavController()

                    LaunchedEffect(viewModel.messageEvent) {
                        viewModel.messageEvent.collect { msg ->
                            msg?.let {
                                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                                viewModel.clearMessage()
                            }
                        }
                    }

                    LaunchedEffect(targetItemId) {
                        if (targetItemId > 0L) {
                            navController.navigate("detail/$targetItemId")
                        }
                    }

                    // Smart Dialog Triggers
                    if (showRateDialog) {
                        RateAppDialog(
                            onDismiss = { viewModel.smartPromptManager.dismissRateDialog(neverShowAgain = false) },
                            onNeverShowAgain = { viewModel.smartPromptManager.dismissRateDialog(neverShowAgain = true) },
                            onRateSuccess = { rating, feedback ->
                                viewModel.smartPromptManager.markRated()
                                FirebaseManager.logEvent("user_rated_app", mapOf("rating" to rating, "feedback" to feedback))
                                Toast.makeText(this@MainActivity, "Thank you for rating ClaimVault!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (showShareDialog) {
                        ShareAppDialog(
                            onDismiss = { viewModel.smartPromptManager.dismissShareDialog() },
                            onShare = {
                                viewModel.smartPromptManager.shareApp()
                            }
                        )
                    }

                    val startDest = if (hasCompletedOnboarding) "home" else "onboarding"

                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = viewModel,
                                onComplete = {
                                    viewModel.completeOnboarding()
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { navController.navigate("add_edit") },
                                onNavigateToDetail = { id -> navController.navigate("detail/$id") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToDashboard = { navController.navigate("dashboard") }
                            )
                        }

                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "add_edit?itemId={itemId}",
                            arguments = listOf(
                                navArgument("itemId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
                            AddEditWarrantyScreen(
                                viewModel = viewModel,
                                editingItemId = if (itemId > 0L) itemId else null,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToCameraScan = { navController.navigate("camera_scan") }
                            )
                        }

                        composable("camera_scan") {
                            CameraScanScreen(
                                viewModel = viewModel,
                                onScanComplete = { path, merchant, amount, dateEpoch, currency, category ->
                                    viewModel.scannedReceiptPath = path
                                    viewModel.scannedMerchant = merchant
                                    viewModel.scannedAmount = amount
                                    viewModel.scannedDateEpoch = dateEpoch
                                    viewModel.scannedCurrency = currency
                                    viewModel.scannedCategory = category
                                    navController.popBackStack()
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "detail/{itemId}",
                            arguments = listOf(
                                navArgument("itemId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->
                            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
                            WarrantyDetailScreen(
                                viewModel = viewModel,
                                itemId = itemId,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToEdit = { id -> navController.navigate("add_edit?itemId=$id") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
