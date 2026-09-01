package com.sonora.music.ui.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.music.data.local.SonoraPreferences
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

@Composable
fun CreatePlaylistModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    sonoraPrefs: SonoraPreferences,
    isDark: Boolean,
    onPlaylistCreated: () -> Unit = {}
) {
    if (!isOpen) return

    val context = LocalContext.current
    var playlistName by remember { mutableStateOf("") }

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val borderCol = themeColors.borderCol

    val hazeState = com.sonora.music.ui.theme.LocalHazeState.current ?: remember { dev.chrisbanes.haze.HazeState() }
    val modalShape = RoundedCornerShape(28.dp)
    val modalGlassStyle = HazeStyle(
        blurRadius = 26.dp,
        tint = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.45f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.25f),
        noiseFactor = 0.04f
    )
    val modalGlareBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
            Color.White.copy(alpha = 0.10f)
        )
    )

    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.60f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(modalShape)
                .then(
                    if (isGlass) {
                        Modifier
                            .hazeChild(state = hazeState, shape = modalShape, style = modalGlassStyle)
                            .background(if (isDark) Color(0xEB141D2B) else Color(0xD8FFFFFF))
                            .border(1.2.dp, modalGlareBorder, modalShape)
                    } else {
                        Modifier
                            .background(if (isDark) Color(0xFF161513) else Color(0xFFF5F2EA))
                            .border(1.dp, borderCol, modalShape)
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consumes clicks inside the card
                )
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Circular Icon Badge with Liquid Glass
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(if (isGlass) (if (isDark) Color(0x30FFFFFF) else Color(0x75FFFFFF)) else (if (isDark) Color(0xFF22201C) else Color(0xFFE6E1D5)))
                        .border(1.dp, if (isGlass) (if (isDark) Color(0x50FFFFFF) else Color(0xB5FFFFFF)) else borderCol, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NUEVA LISTA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Crea una lista personalizada para tus canciones",
                        fontSize = 11.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Luxury Glass TextField
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("Nombre de la lista...", color = textSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isGlass) (if (isDark) Color(0x25FFFFFF) else Color(0x70FFFFFF)) else (if (isDark) Color(0xFF1E1C19) else Color(0xFFEBE6DC)),
                        unfocusedContainerColor = if (isGlass) (if (isDark) Color(0x18FFFFFF) else Color(0x50FFFFFF)) else (if (isDark) Color(0xFF1E1C19) else Color(0xFFEBE6DC)),
                        focusedBorderColor = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else textPrimary,
                        unfocusedBorderColor = if (isGlass) (if (isDark) Color(0x40FFFFFF) else Color(0x90CBD5E1)) else borderCol,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        cursorColor = textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Action Buttons Row with Glass / Glare styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isGlass) (if (isDark) Color(0x25FFFFFF) else Color(0x70FFFFFF)) else (if (isDark) Color(0xFF1E1C19) else Color(0xFFE6E1D5)))
                            .border(1.dp, if (isGlass) (if (isDark) Color(0x45FFFFFF) else Color(0xB5FFFFFF)) else borderCol, RoundedCornerShape(100.dp))
                            .clickable {
                                playlistName = ""
                                onClose()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancelar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    // Create Button with High-Contrast Gradient Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                                if (isGlass) {
                                    if (isDark) Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1)))
                                    else Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF334155)))
                                } else {
                                    SolidColor(if (isDark) Color.White else Color(0xFF121212))
                                }
                            )
                            .clickable {
                                if (playlistName.isNotBlank()) {
                                    sonoraPrefs.createCustomPlaylist(playlistName.trim())
                                    playlistName = ""
                                    onPlaylistCreated()
                                    onClose()
                                    Toast.makeText(context, "Lista creada con éxito", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Crear Lista",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
