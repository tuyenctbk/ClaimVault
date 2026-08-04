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
import com.example.ui.ClaimVaultViewModel
import com.example.ui.HomeScreen
import com.example.ui.OnboardingScreen
import com.example.ui.SettingsScreen
import com.example.ui.WarrantyDetailScreen
import com.example.ui.components.RateAppDialog
import com.example.ui.components.ShareAppDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.FirebaseManager
import com.example.util.ThemeMode

class MainActivity : ComponentActivity() {

    private val viewModel: ClaimVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val targetItemId = intent.getLongExtra("target_item_id", -1L)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
            val showRateDialog by viewModel.smartPromptManager.showRateDialog.collectAsStateWithLifecycle()
            val showShareDialog by viewModel.smartPromptManager.showShareDialog.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                                onNavigateToSettings = { navController.navigate("settings") }
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
