package com.sonora.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun PlaybackSpeedModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentSpeed: Float,
    currentPitch: Float,
    onApply: (speed: Float, pitch: Float) -> Unit,
    isDark: Boolean
) {
    if (!isOpen) return

    var speedState by remember(isOpen, currentSpeed) { mutableStateOf(currentSpeed) }
    var pitchState by remember(isOpen, currentPitch) { mutableStateOf(currentPitch) }

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val hazeState = com.sonora.music.ui.theme.LocalHazeState.current ?: remember { dev.chrisbanes.haze.HazeState() }
    val modalShape = RoundedCornerShape(28.dp)
    val modalGlassStyle = dev.chrisbanes.haze.HazeStyle(
        blurRadius = 28.dp,
        tint = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.35f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.20f),
        noiseFactor = 0.05f
    )
    val modalGlareBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.50f else 0.90f),
            Color.White.copy(alpha = 0.10f)
        )
    )
    val bgCard = themeColors.cardBg
    val borderCol = themeColors.borderCol
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val subCardBg = themeColors.subCardBg
    val activePillBg = themeColors.activePillBg
    val activePillText = themeColors.activePillText

    val speedPresets = listOf(0.5f, 0.75f, 0.9f, 1.0f, 1.1f, 1.25f, 1.5f, 2.0f)

    androidx.activity.compose.BackHandler(onBack = onClose)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(modalShape)
                    .then(
                        if (isGlass) {
                            Modifier
                                .hazeChild(state = hazeState, shape = modalShape, style = modalGlassStyle)
                                .background(if (isDark) Color(0xDC141D2B) else Color(0xEAFFFFFF))
                                .border(1.2.dp, modalGlareBorder, modalShape)
                        } else {
                            Modifier
                                .background(bgCard)
                                .border(1.dp, borderCol, modalShape)
                        }
                    )
                    .clickable(enabled = false) {}
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color.White else Color(0xFF121212)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Velocidad",
                                tint = if (isDark) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Velocidad y Tono",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Ajuste de tempo y pitch en vivo",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .border(1.dp, borderCol, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Speed label & value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Velocidad:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2fx", speedState),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (speedState != 1.0f) Color(0xFF10B981) else textPrimary
                    )
                }

                Slider(
                    value = speedState,
                    onValueChange = {
                        speedState = it
                        onApply(speedState, pitchState)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 29,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isDark) Color.White else Color(0xFF121212),
                        activeTrackColor = if (isDark) Color.White else Color(0xFF121212),
                        inactiveTrackColor = borderCol
                    )
                )

                // Speed Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    speedPresets.take(4).forEach { p ->
                        val isSel = kotlin.math.abs(speedState - p) < 0.03f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activePillBg else subCardBg)
                                .border(1.dp, if (isSel) Color.Transparent else borderCol, RoundedCornerShape(8.dp))
                                .clickable {
                                    speedState = p
                                    onApply(speedState, pitchState)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${p}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) activePillText else textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    speedPresets.takeLast(4).forEach { p ->
                        val isSel = kotlin.math.abs(speedState - p) < 0.03f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) activePillBg else subCardBg)
                                .border(1.dp, if (isSel) Color.Transparent else borderCol, RoundedCornerShape(8.dp))
                                .clickable {
                                    speedState = p
                                    onApply(speedState, pitchState)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${p}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) activePillText else textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Pitch label & value
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tono Musical (Pitch):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2fx", pitchState),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (pitchState != 1.0f) Color(0xFF10B981) else textPrimary
                    )
                }

                Slider(
                    value = pitchState,
                    onValueChange = {
                        pitchState = it
                        onApply(speedState, pitchState)
                    },
                    valueRange = 0.5f..1.5f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isDark) Color.White else Color(0xFF121212),
                        activeTrackColor = if (isDark) Color.White else Color(0xFF121212),
                        inactiveTrackColor = borderCol
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Effects (Normal, Slowed, Nightcore)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Normal
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .clickable {
                                speedState = 1.0f
                                pitchState = 1.0f
                                onApply(1.0f, 1.0f)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Original (1.0x)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    // Slowed
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .clickable {
                                speedState = 0.85f
                                pitchState = 0.85f
                                onApply(0.85f, 0.85f)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌙 Slowed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    // Nightcore
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .clickable {
                                speedState = 1.25f
                                pitchState = 1.25f
                                onApply(1.25f, 1.25f)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Nightcore",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            }
        }
    }
