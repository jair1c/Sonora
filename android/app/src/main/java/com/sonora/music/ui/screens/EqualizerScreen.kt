package com.sonora.music.ui.screens

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.service.SonoraAudioPlayer



@Composable
fun EqualizerModal(
    isOpen: Boolean,
    onClose: () -> Unit,
    audioPlayer: SonoraAudioPlayer,
    sonoraPrefs: SonoraPreferences,
    isDark: Boolean
) {
    if (!isOpen) return

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgCard = if (isGlass) (if (isDark) Color(0xF610141D) else Color(0xF8F0F4F8)) else (if (isDark) Color(0xFF161513) else Color(0xFFF5F2EA))
    val borderCol = themeColors.borderCol
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val subCardBg = themeColors.subCardBg

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

    var bassBoostLevel by remember { mutableFloatStateOf(sonoraPrefs.getBassBoost().toFloat()) }
    var preAmpLevel by remember { mutableFloatStateOf(sonoraPrefs.getPreAmpGain()) }
    var autoVolume by remember { mutableStateOf(sonoraPrefs.isAutoVolumeLeveling()) }

    val activePillBg = if (isDark) Color.White else Color(0xFF121212)
    val activePillText = if (isDark) Color.Black else Color.White

    androidx.activity.compose.BackHandler(onBack = onClose)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            val hazeState = com.sonora.music.ui.theme.LocalHazeState.current ?: remember { dev.chrisbanes.haze.HazeState() }
            val modalShape = RoundedCornerShape(28.dp)
            val modalGlassStyle = dev.chrisbanes.haze.HazeStyle(
                blurRadius = 26.dp,
                tint = if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg.copy(alpha = 0.40f) else com.sonora.music.ui.theme.SonoraGlassLightBg.copy(alpha = 0.25f),
                noiseFactor = 0.04f
            )
            val modalGlareBorder = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
                    Color.White.copy(alpha = 0.10f)
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(modalShape)
                    .then(
                        if (isGlass) {
                            Modifier
                                .hazeChild(state = hazeState, shape = modalShape, style = modalGlassStyle)
                                .background(if (isDark) Color(0xEB141D2B) else Color(0xF2FFFFFF))
                                .border(1.2.dp, modalGlareBorder, modalShape)
                        } else {
                            Modifier
                                .background(bgCard)
                                .border(1.dp, borderCol, modalShape)
                        }
                    )
                    .clickable(enabled = false) {}
                    .padding(22.dp)
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Ecualizador",
                                tint = if (isDark) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Ecualizador de Audio",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Calibra el sonido a tu gusto",
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

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Presets
                    Column {
                        Text(
                            text = "PRESETS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(presets) { idx, presetName ->
                                val isSelected = selectedPreset == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) (if (isDark) Color.White else Color(0xFF121212)) else subCardBg)
                                        .border(1.dp, if (isSelected) Color.Transparent else borderCol, RoundedCornerShape(16.dp))
                                        .clickable {
                                            selectedPreset = idx
                                            sonoraPrefs.setEqualizerPreset(idx)
                                            eq.usePreset(idx.toShort())
                                            for (i in 0 until bandCount) {
                                                bandLevels[i] = eq.getBandLevel(i.toShort()).toFloat()
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = presetName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) (if (isDark) Color.Black else Color.White) else textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 5-Band Sliders Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BANDAS DE FRECUENCIA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        for (i in 0 until bandCount) {
                            val freqHz = eq.getBandFrequency(i.toShort())
                            val freqLabel = if (freqHz >= 1000) "${freqHz / 1000} kHz" else "$freqHz Hz"
                            val currentVal = bandLevels.getOrNull(i) ?: 0f

                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(freqLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                    Text("${(currentVal / 100).toInt()} dB", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                                }
                                Slider(
                                    value = currentVal,
                                    onValueChange = { newVal ->
                                        bandLevels[i] = newVal
                                        eq.setBandLevel(i.toShort(), newVal.toInt().toShort())
                                    },
                                    valueRange = minRange.toFloat()..maxRange.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = textPrimary,
                                        activeTrackColor = textPrimary,
                                        inactiveTrackColor = borderCol
                                    )
                                )
                            }
                        }
                    }

                    // Bass Boost Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BASS BOOST (REFUERZO DE GRAVES)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = bassBoostLevel,
                            onValueChange = { newVal ->
                                bassBoostLevel = newVal
                                sonoraPrefs.setBassBoost(newVal.toInt())
                                eq.setBassBoost(newVal.toInt().toShort())
                            },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = textPrimary,
                                activeTrackColor = textPrimary,
                                inactiveTrackColor = borderCol
                            )
                        )
                    }

                    // Pre-Amp Gain Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PRE-AMPLIFICADOR DIGITAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            }
                            Text(
                                text = "${if (preAmpLevel > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1f", preAmpLevel)} dB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (preAmpLevel != 0f) textPrimary else textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = preAmpLevel,
                            onValueChange = { newVal ->
                                preAmpLevel = newVal
                                sonoraPrefs.setPreAmpGain(newVal)
                                eq.setPreAmp(newVal)
                            },
                            valueRange = -12f..12f,
                            steps = 23,
                            colors = SliderDefaults.colors(
                                thumbColor = textPrimary,
                                activeTrackColor = textPrimary,
                                inactiveTrackColor = borderCol
                            )
                        )
                    }
                }

                // 5. NORMALIZADOR AUTOMÁTICO DE VOLUMEN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(subCardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                        .clickable {
                            val nextState = !autoVolume
                            autoVolume = nextState
                            sonoraPrefs.setAutoVolumeLeveling(nextState)
                            eq.setAutoVolumeLeveling(nextState)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Normalizador de Volumen",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Nivela el volumen de pistas bajas",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                    Switch(
                        checked = autoVolume,
                        onCheckedChange = { checked ->
                            autoVolume = checked
                            sonoraPrefs.setAutoVolumeLeveling(checked)
                            eq.setAutoVolumeLeveling(checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = activePillText,
                            checkedTrackColor = activePillBg,
                            uncheckedThumbColor = textSecondary,
                            uncheckedTrackColor = borderCol
                        )
                    )
                }
            }
        }
    }

// Backward-compatible alias
@Composable
fun EqualizerScreen(
    audioPlayer: SonoraAudioPlayer,
    sonoraPrefs: SonoraPreferences,
    isDark: Boolean,
    onBack: () -> Unit
) {
    EqualizerModal(
        isOpen = true,
        onClose = onBack,
        audioPlayer = audioPlayer,
        sonoraPrefs = sonoraPrefs,
        isDark = isDark
    )

}
