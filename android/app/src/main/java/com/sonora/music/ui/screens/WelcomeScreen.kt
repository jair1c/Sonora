package com.sonora.music.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.music.ui.components.Organic8PetalShape

@Composable
fun WelcomeScreen(
    isDark: Boolean,
    onStart: () -> Unit
) {
    val bgColor = if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA)
    val textPrimary = if (isDark) Color(0xFFF5F2EA) else Color(0xFF121212)
    val textSecondary = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)
    val buttonBg = if (isDark) Color.White else Color(0xFF121212)
    val buttonText = if (isDark) Color.Black else Color.White
    val lineStroke = if (isDark) Color(0xFF262420) else Color(0xFFDED8CD)
    val borderCol = if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_float")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Organic Curved Thread Lines Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Line 1: Left to right meandering
            val path1 = Path().apply {
                moveTo(w * 0.12f, 0f)
                cubicTo(
                    w * 0.12f, h * 0.25f,
                    w * 0.45f, h * 0.35f,
                    w * 0.25f, h * 0.65f
                )
                cubicTo(
                    w * 0.15f, h * 0.8f,
                    w * 0.30f, h * 0.9f,
                    w * 0.50f, h
                )
            }
            drawPath(path = path1, color = lineStroke, style = Stroke(width = 1.5f))

            // Line 2: Center-Right connection
            val path2 = Path().apply {
                moveTo(w * 0.78f, h * 0.22f)
                cubicTo(
                    w * 0.95f, h * 0.38f,
                    w * 0.80f, h * 0.48f,
                    w * 0.82f, h * 0.68f
                )
            }
            drawPath(path = path2, color = lineStroke, style = Stroke(width = 1.5f))
        }

        // Floating Organic Portrait Bubbles
        Box(modifier = Modifier.fillMaxSize()) {
            // Bubble 1 (Left Middle)
            Box(
                modifier = Modifier
                    .offset(x = 10.dp, y = (180 + floatAnim).dp)
                    .size(130.dp)
                    .clip(Organic8PetalShape(8, 0.08f))
                    .border(2.dp, borderCol, Organic8PetalShape(8, 0.08f))
                    .background(if (isDark) Color(0xFF1E1D1A) else Color(0xFFE5DFC9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=400&auto=format&fit=crop",
                    contentDescription = "Artist 1",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bubble 2 (Top Right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-10).dp, y = (130 - floatAnim).dp)
                    .size(140.dp)
                    .clip(Organic8PetalShape(8, 0.09f))
                    .border(2.dp, borderCol, Organic8PetalShape(8, 0.09f))
                    .background(if (isDark) Color(0xFF1E1D1A) else Color(0xFFE5DFC9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=400&auto=format&fit=crop",
                    contentDescription = "Artist 2",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bubble 3 (Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-15).dp, y = (110 + floatAnim * 0.8f).dp)
                    .size(125.dp)
                    .clip(Organic8PetalShape(8, 0.08f))
                    .border(2.dp, borderCol, Organic8PetalShape(8, 0.08f))
                    .background(if (isDark) Color(0xFF1E1D1A) else Color(0xFFE5DFC9)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=400&auto=format&fit=crop",
                    contentDescription = "Artist 3",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Main UI Overlay Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Sonora + Saltar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sonora",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = textPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isDark) Color(0xFF1E1D1A) else Color(0xFFEAE5DA))
                        .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                        .clickable { onStart() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Saltar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            // Bottom Typography & Action
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // "Eleva Cada Momento con la Música"
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                            append("Eleva ")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                            append("Cada\n")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                            append("Momento con la ")
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                            append("Música")
                        }
                    },
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Sumérgete en un mundo donde cada ritmo realza tu estado de ánimo y cada melodía cuenta tu historia local.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Big Button: Explorar Mi Música
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonText
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Explorar Mi Música",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✨ Reproducción 100% Offline de Alta Fidelidad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }
            }
        }
    }
}
