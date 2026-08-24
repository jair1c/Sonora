package com.sonora.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.theme.SonoraObsidianCard
import com.sonora.music.ui.theme.SonoraObsidianDark
import com.sonora.music.ui.theme.SonoraPaperBeige
import com.sonora.music.ui.theme.SonoraPaperCard

@Composable
fun EqualizerScreen(
    audioPlayer: SonoraAudioPlayer,
    sonoraPrefs: SonoraPreferences,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) SonoraObsidianDark else SonoraPaperBeige
    val cardBg = if (isDark) SonoraObsidianCard else SonoraPaperCard
    val textColor = if (isDark) Color.White else Color(0xFF121212)
    val subtextColor = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)

    val eq = audioPlayer.equalizerManager
    val presets = remember { eq.getPresets() }
    var selectedPreset by remember { mutableIntStateOf(sonoraPrefs.getEqualizerPreset().coerceIn(0, (presets.size - 1).coerceAtLeast(0))) }

    val bandCount = remember { eq.getBandCount() }
    val (minRange, maxRange) = remember { eq.getBandLevelRange() }

    val bandLevels = remember {
        mutableStateListOf<Float>().apply {
            for (i in 0 until bandCount) {
                add(eq.getBandLevel(i.toShort()).toFloat())
            }
        }
    }

    var bassBoostLevel by remember { mutableStateOf(sonoraPrefs.getBassBoost().toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = textColor)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ECUALIZADOR DE AUDIO",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Presets Chips
        Text(
            text = "PRESETS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = subtextColor
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(presets) { idx, presetName ->
                val isSelected = selectedPreset == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) textColor else cardBg)
                        .clickable {
                            selectedPreset = idx
                            sonoraPrefs.setEqualizerPreset(idx)
                            eq.usePreset(idx.toShort())
                            for (i in 0 until bandCount) {
                                bandLevels[i] = eq.getBandLevel(i.toShort()).toFloat()
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = presetName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) bgColor else textColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5-Band Sliders Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BANDAS DE FRECUENCIA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Spacer(modifier = Modifier.height(12.dp))

                for (i in 0 until bandCount) {
                    val freqHz = eq.getBandFrequency(i.toShort())
                    val freqLabel = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"
                    val currentVal = bandLevels.getOrNull(i) ?: 0f

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(freqLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subtextColor)
                            Text("${(currentVal / 100).toInt()} dB", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor)
                        }
                        Slider(
                            value = currentVal,
                            onValueChange = { newVal ->
                                bandLevels[i] = newVal
                                eq.setBandLevel(i.toShort(), newVal.toInt().toShort())
                            },
                            valueRange = minRange.toFloat()..maxRange.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = textColor,
                                activeTrackColor = textColor,
                                inactiveTrackColor = if (isDark) Color(0xFF2C2A26) else Color(0xFFD5CFC2)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bass Boost Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBg)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BASS BOOST (REFUERZO DE GRAVES)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = bassBoostLevel,
                    onValueChange = { newVal ->
                        bassBoostLevel = newVal
                        sonoraPrefs.setBassBoost(newVal.toInt())
                        eq.setBassBoost(newVal.toInt().toShort())
                    },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = textColor,
                        activeTrackColor = textColor,
                        inactiveTrackColor = if (isDark) Color(0xFF2C2A26) else Color(0xFFD5CFC2)
                    )
                )
            }
        }
    }
}
