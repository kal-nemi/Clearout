package com.clearout.app.ui.results

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.clearout.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ResultsScreen(
    photosDeleted: Int,
    bytesFreed: Long,
    onBackToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1. Storage Size Formatting
    val formattedSpace = Formatter.formatShortFileSize(context, bytesFreed)

    // 2. CountUp Animation for Deleted Photos
    val animatedCount = remember { Animatable(0f) }
    LaunchedEffect(photosDeleted) {
        animatedCount.animateTo(
            targetValue = photosDeleted.toFloat(),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    // 3. Comparative Metric calculations
    val mb = bytesFreed / 1_048_576.0
    val equivalentText = when {
        mb < 100 -> "${(mb / 4).toInt()} high-quality photos 🌅"
        mb < 1000 -> "${(mb / 3.5).toInt()} MP3 songs worth of space 🎵"
        else -> "${(mb / 1024 * 60).toInt()} minutes of Full HD video 🎬"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ClearoutInk)
    ) {
        // High fidelity orange background accent glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(ClearoutOrangeMuted, Color.Transparent),
                        radius = 500f
                    )
                )
                .align(Alignment.TopCenter)
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
            // Screen Title
            Text(
                text = "PURGE COMPLETE",
                style = MaterialTheme.typography.labelMedium,
                color = ClearoutOrange,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 16.dp)
            )

            // Giant Stat Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = "${animatedCount.value.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = ClearoutText,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Photos Cleared",
                    style = MaterialTheme.typography.titleMedium,
                    color = ClearoutMuted,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "FREED $formattedSpace",
                    style = MaterialTheme.typography.displayMedium,
                    color = ClearoutOrange,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Comparative Card explaining space freed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ClearoutPaper)
                    .border(1.dp, ClearoutBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "THAT'S EQUIVALENT TO",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClearoutMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = equivalentText,
                        style = MaterialTheme.typography.titleMedium,
                        color = ClearoutText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // CTA Bottom buttons (Share summary & Return dashboard)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share button
                Button(
                    onClick = {
                        scope.launch {
                            val uri = generateAndCacheShareCard(context, photosDeleted, formattedSpace, equivalentText)
                            if (uri != null) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "I just cleared $photosDeleted photos and freed $formattedSpace with ClearOut! 🧹 Declutter your gallery locally! Get the app: https://play.google.com/store/apps/details?id=com.clearout.app"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Summary"))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ClearoutOrange),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "SHARE VIRAL STATS 📲",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Return to dashboard button
                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, ClearoutBorder, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = ClearoutPaper),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Return to Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = ClearoutText
                    )
                }
            }
        }
    }
}

// Background PNG Generation Logic
private suspend fun generateAndCacheShareCard(
    context: Context,
    deletedCount: Int,
    freedSpace: String,
    equivalentText: String
): Uri? = withContext(Dispatchers.IO) {
    try {
        val width = 800
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw deep background ink color
        val bgPaint = Paint().apply {
            color = 0xFF0C0C0F.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw inner rounded border card
        val borderPaint = Paint().apply {
            color = 0xFF26262E.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(24f, 24f, width - 24f, height - 24f, 24f, 24f, borderPaint)

        // Draw header text
        val titlePaint = Paint().apply {
            color = 0xFFFF5C1A.toInt() // energetic orange
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("CLEAROUT SUMMARY", 60f, 80f, titlePaint)

        // Draw giant photo metric count (Number on its own line to prevent clipping)
        val countPaint = Paint().apply {
            color = 0xFFF0EEE8.toInt()
            textSize = 100f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("$deletedCount", 60f, 170f, countPaint)

        // Description Label on the next line
        val labelPaint = Paint().apply {
            color = 0xFF8B8890.toInt() // Ash grey muted
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("PHOTOS PURGED FROM GALLERY", 60f, 210f, labelPaint)

        // Draw freed storage metric
        val storagePaint = Paint().apply {
            color = 0xFFFF5C1A.toInt() // Clearout Orange
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("FREED $freedSpace OF STORAGE SPACE", 60f, 280f, storagePaint)

        // Draw comparison text block
        val compTitlePaint = Paint().apply {
            color = 0xFF8B8890.toInt()
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("THAT'S EQUIVALENT TO:", 60f, 350f, compTitlePaint)

        val compBodyPaint = Paint().apply {
            color = 0xFFF0EEE8.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        // Clean comparison emoji strings
        canvas.drawText(equivalentText, 60f, 395f, compBodyPaint)

        // Draw footer watermark brand
        val watermarkPaint = Paint().apply {
            color = 0xFF8B8890.toInt()
            textSize = 14f
            isAntiAlias = true
        }
        canvas.drawText("Make space for what matters. Locally cleaned.", 60f, 445f, watermarkPaint)

        // Draw Play Store Deep Link Watermark inside the card
        val linkPaint = Paint().apply {
            color = 0xFFFF5C1A.toInt()
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Get ClearOut: play.google.com/store/apps/details?id=com.clearout.app", 60f, 470f, linkPaint)

        // Save Bitmap to Cache directory
        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs()
        val file = File(cachePath, "clearout_summary.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        // Get FileProvider Uri
        FileProvider.getUriForFile(context, "com.clearout.app.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
