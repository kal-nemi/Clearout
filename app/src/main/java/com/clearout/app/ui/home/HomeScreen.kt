package com.clearout.app.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api

import android.text.format.Formatter
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clearout.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartClearing: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val stats = uiState.gamification

    // State for overflow menu and reset confirmation dialog
    var showMenu by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Trigger reload whenever entering screen
    LaunchedEffect(Unit) {
        viewModel.loadStorageAndGalleryMetadata()
    }

    // Confirmation dialog for destructive Reset Stats action
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Stats?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will permanently clear your streak, XP, and total photos cleared. " +
                    "Your photos will NOT be affected. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetStats()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = ClearoutOrange, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = ClearoutPaper,
            titleContentColor = ClearoutText,
            textContentColor = ClearoutMuted
        )
    }

    // Infinite float animation for flame glow
    val infiniteTransition = rememberInfiniteTransition(label = "streak_glow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearoutInk)
    ) {
        // Subtle orange gradient blur in bottom center
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ClearoutOrangeMuted, Color.Transparent),
                        radius = 450f
                    )
                )
                .align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Bar: Level, Sync button, Overflow Menu ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LEVEL ${((stats?.xpTotal ?: 0) / 300) + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClearoutOrange,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stats?.levelName ?: "Hoarder",
                        style = MaterialTheme.typography.titleMedium,
                        color = ClearoutText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync button: re-reads storage numbers from device. Does NOT reset stats.
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip { Text("Refresh storage data") }
                        },
                        state = rememberTooltipState()
                    ) {
                        IconButton(
                            onClick = { viewModel.loadStorageAndGalleryMetadata() },
                            modifier = Modifier.background(ClearoutPaper, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh storage data",
                                tint = ClearoutText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Overflow menu: contains the dangerous Reset Stats action
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.background(ClearoutPaper, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = ClearoutText
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Reset Stats",
                                        color = ClearoutOrange,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showResetDialog = true
                                }
                            )
                        }
                    }
                }
            }


            // Circular Storage Progress Ring
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val ratio = if (uiState.totalStorageBytes > 0) {
                    uiState.usedStorageBytes.toFloat() / uiState.totalStorageBytes.toFloat()
                } else 0.75f

                // Animate progress arc on load
                val animatedRatio = remember { Animatable(0f) }
                LaunchedEffect(ratio) {
                    animatedRatio.animateTo(
                        targetValue = ratio,
                        animationSpec = tween(1500, easing = FastOutSlowInEasing)
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Empty background ring
                    drawCircle(
                        color = ClearoutBorder,
                        radius = size.minDimension / 2,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active storage ratio arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(ClearoutOrange, KeepGreen, ClearoutOrange)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedRatio.value,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(animatedRatio.value * 100).toInt()}%",
                        style = MaterialTheme.typography.displayLarge,
                        color = ClearoutText,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Used Space",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClearoutMuted
                    )
                    Text(
                        text = Formatter.formatShortFileSize(context, uiState.usedStorageBytes),
                        style = MaterialTheme.typography.titleMedium,
                        color = ClearoutOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Streak Fire Flame Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClearoutPaper)
                    .border(1.dp, ClearoutBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Cleaning Streak",
                            style = MaterialTheme.typography.titleMedium,
                            color = ClearoutText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if ((stats?.streak ?: 0) > 0) {
                                "Keep the fire alive! Clean photos daily."
                            } else {
                                "Start a cleaning habit. Day 1 is waiting!"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ClearoutMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Flame representation
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(
                                elevation = if ((stats?.streak ?: 0) > 0) 12.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = ClearoutOrange
                            )
                            .background(
                                if ((stats?.streak ?: 0) > 0) ClearoutOrange else ClearoutBorder,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 24.sp
                            )
                            Text(
                                text = "${stats?.streak ?: 0}d",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Primary Call to Action Button
            Button(
                onClick = onStartClearing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = ClearoutOrange),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClearoutOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "START CLEARING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "🧹",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
