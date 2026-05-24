package com.clearout.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ClearOut branded splash screen.
 *
 * Rendered immediately after the system-level SplashScreen API 1-frame window.
 * Plays a scale+fade entrance of the brand mark, then fades out before navigating.
 *
 * Brand guidelines sourced from clearout-logo.html:
 *   - Background: Ink Black #1A1410
 *   - Icon bg: Clearout Orange #FF5C1A (rounded square)
 *   - Icon mark: Storage bar + swipe arrow in cream white
 *   - Wordmark: "ClearOut" (Clear = white, out = orange)
 *   - Tagline: "Swipe. Free. Done." in orange
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {
    // Phase 1: entrance scale + fade in
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }
    // Phase 2: tagline fade in (delayed)
    val taglineAlpha = remember { Animatable(0f) }
    // Phase 3: full screen exit fade
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Entrance: icon pops in
        launch {
            scale.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        alpha.animateTo(1f, animationSpec = tween(400))

        // Tagline fades in after icon settles
        delay(300)
        taglineAlpha.animateTo(1f, animationSpec = tween(500))

        // Hold for readability
        delay(800)

        // Exit: fade entire screen to black before navigation
        screenAlpha.animateTo(0f, animationSpec = tween(400))
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = screenAlpha.value }
            .background(Color(0xFF1A1410)), // Ink Black
        contentAlignment = Alignment.Center
    ) {
        // Subtle radial glow behind the icon
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x33FF5C1A), // 20% orange
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // ── Brand Mark: Orange rounded square with SVG icon inside ──
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawBrandMark()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Wordmark: "Clear" white + "out" orange ──
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "Clear",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "out",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF5C1A),
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tagline: delayed fade in ──
            Text(
                text = "Swipe. Free. Done.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFFF5C1A),
                letterSpacing = 3.sp,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}

/**
 * Draws the brand icon mark directly on Canvas, pixel-perfect to clearout-logo.html Panel B SVG:
 *
 *   Orange rounded square (#FF5C1A) background
 *   Glass shimmer overlay (135deg linear gradient top-left triangle)
 *   Storage bar: full dimmed track + used bright white portion
 *   Curved swipe arrow from bar midpoint curving upward
 *   Arrowhead at tip
 *   White dot accent at arrow tip
 */
private fun DrawScope.drawBrandMark() {
    val w = size.width
    val h = size.height
    val cornerRadius = w * 0.22f // ~28dp equivalent

    // ── Orange rounded square background ──
    drawRoundRect(
        color = Color(0xFFFF5C1A),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
    )

    // ── Glass shimmer: top-left triangle at 15% white ──
    val shimmerPath = Path().apply {
        moveTo(0f, 0f)
        lineTo(w * 0.75f, 0f)
        lineTo(0f, h * 0.6f)
        close()
    }
    drawPath(shimmerPath, color = Color(0x26FFFFFF))

    // ── Dark overlay behind icon elements (matches HTML rx32 rect at 15% black) ──
    drawRoundRect(
        color = Color(0x26000000),
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
    )

    // ── Storage bar positions (scaled from HTML 140x140 → canvas size) ──
    val barX = w * 0.157f        // x=22/140
    val barY = h * 0.414f        // y=58/140
    val barW = w * 0.686f        // width=96/140
    val barH = h * 0.071f        // height=10/140
    val barRx = barH / 2f

    // Full track (dimmed)
    drawRoundRect(
        color = Color(0x26FFFFFF),
        topLeft = Offset(barX, barY),
        size = Size(barW, barH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRx)
    )

    // Used portion (bright — 60/96 of full width)
    val usedW = barW * (60f / 96f)
    drawRoundRect(
        color = Color(0xE6FFFFFF),
        topLeft = Offset(barX, barY),
        size = Size(usedW, barH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRx)
    )

    // ── Swipe curved arrow: M82 63 Q100 63 108 48 (scaled) ──
    val arrowPath = Path().apply {
        moveTo(w * 0.586f, barY + barH / 2)   // M82/140 * w
        quadraticBezierTo(
            w * 0.714f, barY + barH / 2,       // Q100/140
            w * 0.771f, h * 0.343f             // Q108/140, y48/140
        )
    }
    drawPath(
        path = arrowPath,
        color = Color.White,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // ── Arrowhead: M102 42 L108 48 L114 44 ──
    val headPath = Path().apply {
        moveTo(w * 0.729f, h * 0.300f)   // M102/140, y42/140
        lineTo(w * 0.771f, h * 0.343f)   // L108/140, y48/140
        lineTo(w * 0.814f, h * 0.314f)   // L114/140, y44/140
    }
    drawPath(
        path = headPath,
        color = Color.White,
        style = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // ── Dot accent at arrow tip: cx109 cy47 r4 ──
    drawCircle(
        color = Color.White,
        radius = w * 0.030f,
        center = Offset(w * 0.771f, h * 0.336f)
    )
}
