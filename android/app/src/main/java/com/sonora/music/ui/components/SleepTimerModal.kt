package com.sonora.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SleepTimerModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    currentMinutesRemaining: Int?,
    onSetTimer: (Int?) -> Unit,
    isDark: Boolean
) {
    if (!isOpen) return

    val bgCard = if (isDark) Color(0xFF161513) else Color(0xFFF5F2EA)
    val borderCol = if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)
    val textPrimary = if (isDark) Color(0xFFF5F2EA) else Color(0xFF121212)
    val textSecondary = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)
    val subCardBg = if (isDark) Color(0xFF1F1D1A) else Color(0xFFEAE5DA)
    val activePillBg = if (isDark) Color.White else Color(0xFF121212)
    val activePillText = if (isDark) Color.Black else Color.White

    val timerOptions = listOf(
        Pair("Desactivado", null),
        Pair("15 minutos", 15),
        Pair("30 minutos", 30),
        Pair("45 minutos", 45),
        Pair("60 minutos", 60),
        Pair("Al terminar canción", -1)
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(bgCard)
                    .border(1.dp, borderCol, RoundedCornerShape(28.dp))
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
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = "Temporizador",
                                tint = if (isDark) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Temporizador",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = if (currentMinutesRemaining != null && currentMinutesRemaining > 0)
                                    "Apaga en ~$currentMinutesRemaining min"
                                else
                                    "Pausa la música al dormir",
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

                // Options list
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timerOptions.forEach { (label, minutes) ->
                        val isSelected = (minutes == null && currentMinutesRemaining == null) ||
                                (minutes != null && minutes == currentMinutesRemaining)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) activePillBg else subCardBg)
                                .border(1.dp, if (isSelected) Color.Transparent else borderCol, RoundedCornerShape(14.dp))
                                .clickable {
                                    onSetTimer(minutes)
                                    onClose()
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activePillText else textPrimary
                            )

                            if (isSelected) {
                                Text(
                                    text = "ACTIVO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDark) Color(0xFF121212) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
