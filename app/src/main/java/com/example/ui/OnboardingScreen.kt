package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoubleArrow
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ClaimVaultLogo
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLightContainer
import com.example.ui.theme.EmeraldPrimary

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val primaryIcon: ImageVector,
    val colorAccent: Color,
    val illustrationType: IllustrationType
)

enum class IllustrationType {
    VAULT_SECURE,
    OCR_SCAN,
    NOTIFY_PDF
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPageIndex by remember { mutableIntStateOf(0) }

    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Welcome to ClaimVault",
                subtitle = "100% Offline & Private",
                description = "Tired of paper receipts fading or warranties disappearing? Store, track, and protect everything in one fully local app. Zero cloud databases, zero ads, zero mandatory tracking.",
                primaryIcon = Icons.Default.Security,
                colorAccent = EmeraldPrimary,
                illustrationType = IllustrationType.VAULT_SECURE
            ),
            OnboardingPage(
                title = "Instant OCR Scan",
                subtitle = "Frictionless Auto-Fill",
                description = "Simply snap a photo of any purchase receipt. Our on-device offline scanner automatically detects the merchant name, purchase date, and total cost to populate your vault in seconds.",
                primaryIcon = Icons.Default.CameraAlt,
                colorAccent = Color(0xFFD97706), // Amber
                illustrationType = IllustrationType.OCR_SCAN
            ),
            OnboardingPage(
                title = "Expiry Alerts & PDF Export",
                subtitle = "Be Claim-Ready Instantly",
                description = "Get notified with gentle push reminders before your warranties run out. Generate fully packaged PDF claims featuring your item details and high-resolution receipt in just one click.",
                primaryIcon = Icons.Default.NotificationsActive,
                colorAccent = Color(0xFF0284C7), // Sky Blue
                illustrationType = IllustrationType.NOTIFY_PDF
            )
        )
    }

    val currentPage = pages[currentPageIndex]

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App branding header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                ClaimVaultLogo(sizeDp = 32)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ClaimVault",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Main sliding layout content (constrained to look premium and centered on wider screens)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentPageIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { width -> width } + fadeIn() with
                                    slideOutHorizontally { width -> -width } + fadeOut()
                        } else {
                            slideInHorizontally { width -> -width } + fadeIn() with
                                    slideOutHorizontally { width -> width } + fadeOut()
                        }
                    },
                    label = "onboarding_animation"
                ) { targetPage ->
                    val page = pages[targetPage]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // High-fidelity illustration panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OnboardingIllustration(
                                type = page.illustrationType,
                                accentColor = page.colorAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Subtitle badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(page.colorAccent.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = page.primaryIcon,
                                    contentDescription = null,
                                    tint = page.colorAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = page.subtitle,
                                    color = page.colorAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main title
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Narrative description
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Bottom Navigation & Controls (staggered & adaptive)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                // Indicator dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    pages.forEachIndexed { index, _ ->
                        val isSelected = index == currentPageIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (isSelected) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) currentPage.colorAccent else MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.2f
                                    )
                                )
                        )
                    }
                }

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button or space backer
                    if (currentPageIndex < pages.lastIndex) {
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier.testTag("onboarding_skip_button")
                        ) {
                            Text(
                                "Skip",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                    }

                    // Next / Get Started button
                    Button(
                        onClick = {
                            if (currentPageIndex < pages.lastIndex) {
                                currentPageIndex++
                            } else {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentPage.colorAccent,
                            contentColor = if (currentPage.colorAccent == EmeraldPrimary) Color.Black else Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("onboarding_next_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (currentPageIndex == pages.lastIndex) "Get Started" else "Next",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = if (currentPageIndex == pages.lastIndex) Icons.Default.DoubleArrow else Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A beautiful, rich vector drawing system for the Onboarding illustrations
 * to keep the app lightweight and completely robust across screens.
 */
@Composable
fun OnboardingIllustration(
    type: IllustrationType,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.value == 0xFF0F172A.toULong()

    Card(
        modifier = modifier
            .size(width = 280.dp, height = 180.dp)
            .clip(RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (type) {
                IllustrationType.VAULT_SECURE -> {
                    // Vault Vector Drawing
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2
                        val radius = 40.dp.toPx()

                        // Outer ring of vault door
                        drawCircle(
                            color = accentColor.copy(alpha = 0.2f),
                            radius = radius + 12.dp.toPx(),
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Main circular vault door
                        drawCircle(
                            color = accentColor,
                            radius = radius,
                            style = Stroke(width = 6.dp.toPx())
                        )

                        // Center lock spindle
                        drawCircle(
                            color = accentColor,
                            radius = 12.dp.toPx()
                        )

                        // Radial locking spokes
                        val spokeLength = 32.dp.toPx()
                        for (i in 0 until 4) {
                            val angle = (i * Math.PI / 2).toFloat()
                            val startX = centerX + (12.dp.toPx() * Math.cos(angle.toDouble())).toFloat()
                            val startY = centerY + (12.dp.toPx() * Math.sin(angle.toDouble())).toFloat()
                            val endX = centerX + (spokeLength * Math.cos(angle.toDouble())).toFloat()
                            val endY = centerY + (spokeLength * Math.sin(angle.toDouble())).toFloat()

                            drawLine(
                                color = accentColor,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 5.dp.toPx()
                            )

                            // Handle knobs at the ends
                            drawCircle(
                                color = accentColor,
                                radius = 6.dp.toPx(),
                                center = Offset(endX, endY)
                            )
                        }
                    }
                }

                IllustrationType.OCR_SCAN -> {
                    // Scanner & Camera HUD Vector
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val width = size.width
                        val height = size.height

                        // Draw a dashed mockup receipt/document
                        val docLeft = 25.dp.toPx()
                        val docTop = 15.dp.toPx()
                        val docWidth = 70.dp.toPx()
                        val docHeight = 90.dp.toPx()

                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.15f),
                            topLeft = Offset(docLeft, docTop),
                            size = androidx.compose.ui.geometry.Size(docWidth, docHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )

                        // Content text lines
                        val linePadding = 12.dp.toPx()
                        val totalLines = 5
                        for (i in 0 until totalLines) {
                            val lineY = docTop + 16.dp.toPx() + (i * 14.dp.toPx())
                            val lineWidth = if (i == 0) docWidth * 0.4f else if (i == totalLines - 1) docWidth * 0.3f else docWidth * 0.7f
                            drawLine(
                                color = accentColor.copy(alpha = 0.3f),
                                start = Offset(docLeft + linePadding, lineY),
                                end = Offset(docLeft + linePadding + lineWidth, lineY),
                                strokeWidth = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                            )
                        }

                        // Scanning laser beam line (red or accent)
                        val laserY = height / 2 + 5.dp.toPx()
                        drawLine(
                            color = accentColor,
                            start = Offset(10.dp.toPx(), laserY),
                            end = Offset(width - 10.dp.toPx(), laserY),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Laser glow rect
                        drawRect(
                            color = accentColor.copy(alpha = 0.1f),
                            topLeft = Offset(15.dp.toPx(), laserY - 10.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(width - 30.dp.toPx(), 20.dp.toPx())
                        )
                    }
                }

                IllustrationType.NOTIFY_PDF -> {
                    // Notifications + PDF Vector Drawing
                    Canvas(modifier = Modifier.size(120.dp)) {
                        val centerX = size.width / 2
                        val centerY = size.height / 2

                        // Draw a beautifully layered PDF Document sheet backer
                        val pdfWidth = 60.dp.toPx()
                        val pdfHeight = 76.dp.toPx()
                        drawRoundRect(
                            color = accentColor.copy(alpha = 0.2f),
                            topLeft = Offset(centerX - pdfWidth / 2 - 10.dp.toPx(), centerY - pdfHeight / 2 + 8.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(pdfWidth, pdfHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                        )

                        // Draw main PDF Document sheet
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(centerX - pdfWidth / 2, centerY - pdfHeight / 2),
                            size = androidx.compose.ui.geometry.Size(pdfWidth, pdfHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // PDF Red Ribbon Badge style icon
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(centerX - 18.dp.toPx(), centerY - 14.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(12.dp.toPx(), 16.dp.toPx())
                        )

                        // PDF lines
                        drawLine(
                            color = accentColor.copy(alpha = 0.4f),
                            start = Offset(centerX - 18.dp.toPx(), centerY + 10.dp.toPx()),
                            end = Offset(centerX + 18.dp.toPx(), centerY + 10.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )
                        drawLine(
                            color = accentColor.copy(alpha = 0.4f),
                            start = Offset(centerX - 18.dp.toPx(), centerY + 18.dp.toPx()),
                            end = Offset(centerX + 10.dp.toPx(), centerY + 18.dp.toPx()),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Overlapping notification bell
                        drawCircle(
                            color = Color(0xFFF59E0B), // Yellow/Amber notification bell glow
                            center = Offset(centerX + 32.dp.toPx(), centerY - 20.dp.toPx()),
                            radius = 16.dp.toPx()
                        )

                        // Draw small notification exclamation mark inside bell circle
                        val bellX = centerX + 32.dp.toPx()
                        val bellY = centerY - 20.dp.toPx()
                        drawLine(
                            color = Color.White,
                            start = Offset(bellX, bellY - 6.dp.toPx()),
                            end = Offset(bellX, bellY + 1.dp.toPx()),
                            strokeWidth = 2.5f.dp.toPx()
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.5f.dp.toPx(),
                            center = Offset(bellX, bellY + 5.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
