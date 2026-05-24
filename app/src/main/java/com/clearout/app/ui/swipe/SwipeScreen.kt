package com.clearout.app.ui.swipe

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.clearout.app.domain.model.Photo
import com.clearout.app.domain.model.SwipeAction
import com.clearout.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    viewModel: SwipeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResults: (photosDeleted: Int, bytesFreed: Long) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // ActivityResult Launcher for MediaStore deletion confirmations
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedPhotos = viewModel.getToDeleteList()
            val totalBytes = deletedPhotos.sumOf { it.sizeBytes }
            viewModel.onDeleteConfirmed()
            onNavigateToResults(deletedPhotos.size, totalBytes)
        } else {
            viewModel.onDeleteCancelled()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PURGE GALLERY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = ClearoutText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = ClearoutText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClearoutInk,
                    titleContentColor = ClearoutText
                )
            )
        },
        containerColor = ClearoutInk
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            when (val state = uiState) {
                is SwipeUiState.Loading -> {
                    CircularProgressIndicator(
                        color = ClearoutOrange,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is SwipeUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = DeleteRed,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadGallery() },
                            colors = ButtonDefaults.buttonColors(containerColor = ClearoutOrange)
                        ) {
                            Text("Retry")
                        }
                    }
                }

                is SwipeUiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Gallery fully swiped!",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = ClearoutText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You processed all loaded photos in this session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ClearoutMuted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        val deleteList = viewModel.getToDeleteList()
                        if (deleteList.isNotEmpty()) {
                            val totalBytes = deleteList.sumOf { it.sizeBytes }
                            val mbStr = android.text.format.Formatter.formatShortFileSize(context, totalBytes)

                            Button(
                                onClick = {
                                    val sender = viewModel.buildDeleteIntentSender()
                                    if (sender != null) {
                                        val request = IntentSenderRequest.Builder(sender).build()
                                        deleteLauncher.launch(request)
                                    } else {
                                        // Fallback/Direct Deletion for older APIs
                                        viewModel.onDeleteConfirmed()
                                        onNavigateToResults(deleteList.size, totalBytes)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeleteRed),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Confirm Delete (${deleteList.size} items, $mbStr)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Button(
                                onClick = { onNavigateToResults(0, 0L) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ClearoutOrange),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "View Dashboard Summary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                is SwipeUiState.Ready -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "REMAINING",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ClearoutMuted,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.remainingCount} Photos",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ClearoutText,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "MARKED DELETE",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DeleteRed,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.toDeleteCount} Selected",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeleteRed,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Cards Stack Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 1. Next Card in stack
                            state.nextPhoto?.let { nextPhoto ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .fillMaxHeight(0.95f)
                                        .scale(0.94f)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(ClearoutPaper)
                                        .border(1.dp, ClearoutBorder, RoundedCornerShape(24.dp))
                                ) {
                                    AsyncImage(
                                        model = nextPhoto.uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                    )
                                }
                            }

                            // 2. Main Active Card
                            SwipeableCard(
                                photo = state.currentPhoto,
                                onSwiped = { action ->
                                    viewModel.onSwipe(action)
                                }
                            )
                        }

                        // Bottom Control Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Undo button
                            Button(
                                onClick = { viewModel.undo() },
                                enabled = viewModel.isUndoAvailable(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ClearoutPaper,
                                    disabledContainerColor = ClearoutPaper.copy(alpha = 0.2f),
                                    contentColor = ClearoutText,
                                    disabledContentColor = ClearoutMuted.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .padding(horizontal = 8.dp)
                                    .border(
                                        1.dp,
                                        if (viewModel.isUndoAvailable()) ClearoutBorder else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Text(
                                    text = "↩ UNDO",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Deletion checkout button
                            Button(
                                onClick = {
                                    val list = viewModel.getToDeleteList()
                                    if (list.isNotEmpty()) {
                                        val sender = viewModel.buildDeleteIntentSender()
                                        if (sender != null) {
                                            val request = IntentSenderRequest.Builder(sender).build()
                                            deleteLauncher.launch(request)
                                        } else {
                                            viewModel.onDeleteConfirmed()
                                            val size = list.size
                                            val bytes = list.sumOf { it.sizeBytes }
                                            onNavigateToResults(size, bytes)
                                        }
                                    } else {
                                        onNavigateToResults(0, 0L)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ClearoutOrange
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "CHECKOUT 🧹",
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeableCard(
    photo: Photo,
    onSwiped: (SwipeAction) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val vibrator = context.getSystemService(Vibrator::class.java)

    val offsetX = remember(photo.id) { Animatable(0f) }
    val offsetY = remember(photo.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val maxSwipeDistance = 400f

    // Calculate rotation during swipe based on coordinates
    val rotationZ = (offsetX.value / 25f)

    // Calculate dynamic overlay colors opacity
    val deltaRatio = (abs(offsetX.value) / maxSwipeDistance).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                this.rotationZ = rotationZ
                cameraDistance = 8 * density
            }
            .clip(RoundedCornerShape(24.dp))
            .background(ClearoutPaper)
            .border(1.dp, ClearoutBorder, RoundedCornerShape(24.dp))
            .pointerInput(photo.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }

                        // Provide subtle ticking haptics when crossing thresholds
                        if (abs(offsetX.value) in 240f..255f) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (offsetX.value > 250f) {
                                // Swiped right: KEEP
                                // Stronger confirmation vibration
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
                                }
                                offsetX.animateTo(800f, spring(stiffness = Spring.StiffnessMedium))
                                onSwiped(SwipeAction.KEEP)
                            } else if (offsetX.value < -250f) {
                                // Swiped left: DELETE
                                // Stronger confirmation vibration
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    vibrator?.vibrate(VibrationEffect.createOneShot(25L, VibrationEffect.DEFAULT_AMPLITUDE))
                                }
                                offsetX.animateTo(-800f, spring(stiffness = Spring.StiffnessMedium))
                                onSwiped(SwipeAction.DELETE)
                            } else {
                                // Snapback in parallel coroutines to avoid sequential blocking
                                launch {
                                    offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                }
                                launch {
                                    offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            // Snapback instantly in parallel if gesture was hijacked or cancelled
                            launch {
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            }
                            launch {
                                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            }
                        }
                    }
                )
            }
    ) {
        // Render photo downsampled using Coil memory size constraints
        AsyncImage(
            model = photo.uri,
            contentDescription = "Swipe Card",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // DELETE overlay (turns Red when swiping left)
        if (offsetX.value < 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeleteRed.copy(alpha = deltaRatio * 0.45f))
            )
        }

        // KEEP overlay (turns Green when swiping right)
        if (offsetX.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(KeepGreen.copy(alpha = deltaRatio * 0.45f))
            )
        }

        // Stamp Text Labels ("DELETE" / "KEEP")
        if (deltaRatio > 0.15f) {
            val isDelete = offsetX.value < 0f
            Box(
                modifier = Modifier
                    .align(if (isDelete) Alignment.TopEnd else Alignment.TopStart)
                    .padding(24.dp)
                    .border(3.dp, if (isDelete) DeleteRed else KeepGreen, RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isDelete) "DELETE" else "KEEP",
                    color = if (isDelete) DeleteRed else KeepGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
