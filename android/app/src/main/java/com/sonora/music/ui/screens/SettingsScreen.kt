package com.sonora.music.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.music.data.local.SonoraPreferences
import com.sonora.music.data.model.Song
import com.sonora.music.service.SonoraAudioPlayer
import com.sonora.music.ui.components.SleepTimerModal
import com.sonora.music.ui.components.StatsModal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    sonoraPrefs: SonoraPreferences,
    audioPlayer: SonoraAudioPlayer,
    songs: List<Song>,
    isDark: Boolean,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRescanLibrary: () -> Unit,
    onThemeChanged: (String) -> Unit
) {
    val context = LocalContext.current

    val bgColor = if (isDark) Color(0xFF0F0E0D) else Color(0xFFF5F2EA)
    val cardBg = if (isDark) Color(0xFF161513) else Color(0xFFEAE5DA)
    val subCardBg = if (isDark) Color(0xFF1F1D1A) else Color(0xFFECE7DC)
    val borderCol = if (isDark) Color(0xFF2A2824) else Color(0xFFDED8CD)
    val textPrimary = if (isDark) Color(0xFFF5F2EA) else Color(0xFF121212)
    val textSecondary = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)
    val activePillBg = if (isDark) Color.White else Color(0xFF121212)
    val activePillText = if (isDark) Color.Black else Color.White

    var showSleepModal by remember { mutableStateOf(false) }
    var showStatsModal by remember { mutableStateOf(false) }

    val sleepTimerSeconds by audioPlayer.sleepTimerSecondsLeft.collectAsState()
    var currentTheme by remember { mutableStateOf(sonoraPrefs.getThemeMode()) }
    var petalRoundness by remember { mutableIntStateOf(sonoraPrefs.getPetalRoundness()) }
    var crossfadeSeconds by remember { mutableIntStateOf(sonoraPrefs.getCrossfadeSeconds()) }
    var playbackSpeed by remember { mutableFloatStateOf(sonoraPrefs.getPlaybackSpeed()) }
    var activeNavTabs by remember { mutableStateOf(sonoraPrefs.getNavTabs()) }
    var navLabelMode by remember { mutableStateOf(sonoraPrefs.getNavLabelMode()) }
    var playerControlsStyle by remember { mutableStateOf(sonoraPrefs.getPlayerControlsStyle()) }

    val playCounts = remember(showStatsModal, songs) { sonoraPrefs.getPlayCounts() }
    val totalPlayedSongs = remember(songs, playCounts) {
        songs.count { (playCounts[it.id] ?: it.playCount) > 0 }
    }
    val totalMinutes = remember(songs, playCounts) {
        val recordedMins = sonoraPrefs.getTotalListeningMinutes()
        if (recordedMins > 0) recordedMins
        else songs.sumOf { s -> ((playCounts[s.id] ?: s.playCount) * (s.durationMs / 1000L / 60L)).toInt() }
    }

    val allAvailableTabs = listOf(
        Pair("biblioteca", "Biblioteca"),
        Pair("canciones", "Canciones"),
        Pair("albumes", "Álbumes"),
        Pair("artistas", "Artistas"),
        Pair("listas", "Listas ♡"),
        Pair("carpetas", "Carpetas"),
        Pair("reproductor", "Reproductor"),
        Pair("ajustes", "Ajustes")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        // 1. Top Bar & Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
                        .border(1.dp, borderCol, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isDark) Color(0xFF1F1D1A) else Color(0xFFEAE5DA))
                        .border(1.dp, borderCol, RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sonora v2.1 • Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "CENTRO DE CONTROL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Ajustes &\nHerramientas",
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                color = textPrimary
            )
        }

        // 2. 2x2 Quick Action Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Equalizer Card
                    QuickActionCard(
                        icon = Icons.Default.Tune,
                        badge = "10 Bandas",
                        title = "Ecualizador Gráfico",
                        subtitle = "Graves, perfiles acústicos",
                        cardBg = cardBg,
                        borderCol = borderCol,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenEqualizer
                    )

                    // Sleep Timer Card
                    val sleepSub = if (sleepTimerSeconds != null) {
                        "${sleepTimerSeconds!! / 60}m restantes"
                    } else {
                        "Pausa al dormir"
                    }
                    QuickActionCard(
                        icon = Icons.Default.NightlightRound,
                        badge = if (sleepTimerSeconds != null) "Activo" else null,
                        title = "Temporizador de Apagado",
                        subtitle = sleepSub,
                        cardBg = cardBg,
                        borderCol = borderCol,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { showSleepModal = true }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // SonoraStats Card
                    QuickActionCard(
                        icon = Icons.Default.BarChart,
                        badge = "$totalPlayedSongs canciones",
                        title = "sonoraStats",
                        subtitle = "$totalMinutes min escuchados",
                        cardBg = cardBg,
                        borderCol = borderCol,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = { showStatsModal = true }
                    )

                    // Rescan Card
                    QuickActionCard(
                        icon = Icons.Default.Refresh,
                        badge = "${songs.size} Pistas",
                        title = "Re-escanear",
                        subtitle = "Actualizar biblioteca",
                        cardBg = cardBg,
                        borderCol = borderCol,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onRescanLibrary()
                            Toast.makeText(context, "Biblioteca re-escaneada", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // 3. APARIENCIA & TEMA
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "APARIENCIA & TEMA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona la paleta visual para tu experiencia auditiva.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("system", "Sistema"),
                        Pair("dark", "Oscuro"),
                        Pair("light", "Claro")
                    ).forEach { (mode, label) ->
                        val isSelected = currentTheme == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) activePillBg else subCardBg)
                                .border(1.dp, if (isSelected) Color.Transparent else borderCol, RoundedCornerShape(12.dp))
                                .clickable {
                                    currentTheme = mode
                                    sonoraPrefs.setThemeMode(mode)
                                    onThemeChanged(mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activePillText else textPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. ESTILO DE CONTROLES DE REPRODUCCIÓN (5 Opciones de Lujo)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ESTILO DE BOTONES DE REPRODUCCIÓN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Elige la estética y disposición de los botones en la pantalla de reproducción completa.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("dock", "Cápsula Flotante Studio", "Barra horizontal continua con efecto dock y botón Play 72dp"),
                        Triple("circles", "Geometría Suiza (Círculos)", "Esferas independientes de alta relojería con anillos de luz"),
                        Triple("organic", "Orgánico Esculpido (Flor)", "Botón central con 8 pétalos florales y gotas laterales"),
                        Triple("squircle", "Audiófilo Hi-Fi (Squircle)", "Cuadrado redondeado geométrico y líneas minimalistas"),
                        Triple("waveform", "Dynamic Action Pill (Mini-Onda)", "Píldora interactiva con ecualizador animado en vivo")
                    ).forEach { (id, title, subtitle) ->
                        val isSelected = playerControlsStyle == id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) (if (isDark) Color(0xFF262420) else Color(0xFFDFD9CE)) else subCardBg)
                                .border(1.dp, if (isSelected) (if (isDark) Color.White else Color.Black) else borderCol, RoundedCornerShape(14.dp))
                                .clickable {
                                    playerControlsStyle = id
                                    sonoraPrefs.setPlayerControlsStyle(id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textPrimary
                                )
                                Text(
                                    text = subtitle,
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    playerControlsStyle = id
                                    sonoraPrefs.setPlayerControlsStyle(id)
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = if (isDark) Color.White else Color.Black,
                                    unselectedColor = borderCol
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. REDONDEZ DE LOS PÉTALOS
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterVintage,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "REDONDEZ DE LOS PÉTALOS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(subCardBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$petalRoundness%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modifica la curvatura y profundidad de los 8 pétalos de la carátula y el contorno del reproductor.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Slider(
                    value = petalRoundness.toFloat(),
                    onValueChange = {
                        petalRoundness = it.toInt()
                        sonoraPrefs.setPetalRoundness(petalRoundness)
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = activePillBg,
                        activeTrackColor = textPrimary,
                        inactiveTrackColor = borderCol
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0% (Círculo)", fontSize = 10.sp, color = textSecondary)
                    Text("50% (Suave)", fontSize = 10.sp, color = textSecondary)
                    Text("100% (Pétalos marcados)", fontSize = 10.sp, color = textSecondary)
                }
            }
        }

        // 5. MOTOR DE AUDIO & MEZCLA
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "MOTOR DE AUDIO & MEZCLA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Crossfade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fundido Cruzado (Crossfade)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(subCardBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "La siguiente canción comenzará a sonar gradualmente antes de terminar la actual.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair(0, "Off"),
                        Pair(2, "2s"),
                        Pair(4, "4s"),
                        Pair(6, "6s"),
                        Pair(8, "8s"),
                        Pair(10, "10s")
                    ).forEach { (sec, label) ->
                        val isSel = crossfadeSeconds == sec
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) activePillBg else subCardBg)
                                .border(1.dp, if (isSel) Color.Transparent else borderCol, RoundedCornerShape(10.dp))
                                .clickable {
                                    crossfadeSeconds = sec
                                    sonoraPrefs.setCrossfadeSeconds(sec)
                                    audioPlayer.setCrossfadeSeconds(sec)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) activePillText else textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Playback Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Velocidad de Reproducción",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(subCardBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0.8f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { sp ->
                        val isSel = playbackSpeed == sp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) activePillBg else subCardBg)
                                .border(1.dp, if (isSel) Color.Transparent else borderCol, RoundedCornerShape(10.dp))
                                .clickable {
                                    playbackSpeed = sp
                                    sonoraPrefs.setPlaybackSpeed(sp)
                                    audioPlayer.setPlaybackSpeed(sp)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${sp}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) activePillText else textPrimary
                            )
                        }
                    }
                }
            }
        }

        // 6. PERSONALIZAR BARRA INFERIOR
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "PERSONALIZAR BARRA INFERIOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                    }

                    Text(
                        text = "Restablecer",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.clickable {
                            activeNavTabs = listOf("canciones", "listas", "ajustes")
                            sonoraPrefs.setNavTabs(activeNavTabs)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecciona qué accesos directos mostrar en la barra inferior y reorganiza su orden.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Chips Selector
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allAvailableTabs.forEach { (tabId, label) ->
                        val isSelected = activeNavTabs.contains(tabId)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (isSelected) activePillBg else subCardBg)
                                .border(1.dp, if (isSelected) Color.Transparent else borderCol, RoundedCornerShape(100.dp))
                                .clickable {
                                    val newTabs = activeNavTabs.toMutableList()
                                    if (isSelected) {
                                        if (newTabs.size > 2) newTabs.remove(tabId)
                                    } else {
                                        if (newTabs.size < 5) newTabs.add(tabId)
                                    }
                                    activeNavTabs = newTabs
                                    sonoraPrefs.setNavTabs(newTabs)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isSelected) "✓ $label" else "+$label",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activePillText else textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Orden actual de pestañas (de izquierda a derecha):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeNavTabs.forEachIndexed { index, tabId ->
                        val tabName = allAvailableTabs.firstOrNull { it.first == tabId }?.second ?: tabId
                        val rowBg = if (isDark) Color(0xFF262420) else Color(0xFFE3DDD1)
                        val badgeBg = if (isDark) Color(0xFF38352F) else Color(0xFFD2CBC0)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(rowBg)
                                .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(badgeBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = textPrimary
                                    )
                                }
                                Text(
                                    text = tabName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(badgeBg)
                                            .clickable {
                                                val list = activeNavTabs.toMutableList()
                                                val temp = list[index]
                                                list[index] = list[index - 1]
                                                list[index - 1] = temp
                                                activeNavTabs = list
                                                sonoraPrefs.setNavTabs(list)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", tint = textPrimary, modifier = Modifier.size(15.dp))
                                    }
                                }
                                if (index < activeNavTabs.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(badgeBg)
                                            .clickable {
                                                val list = activeNavTabs.toMutableList()
                                                val temp = list[index]
                                                list[index] = list[index + 1]
                                                list[index + 1] = temp
                                                activeNavTabs = list
                                                sonoraPrefs.setNavTabs(list)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar", tint = textPrimary, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Visualización de Etiquetas de Texto:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("active_only", "Solo Activa"),
                        Pair("always", "Siempre"),
                        Pair("never", "Solo Iconos")
                    ).forEach { (mode, label) ->
                        val isSel = navLabelMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) activePillBg else subCardBg)
                                .border(1.dp, if (isSel) Color.Transparent else borderCol, RoundedCornerShape(10.dp))
                                .clickable {
                                    navLabelMode = mode
                                    sonoraPrefs.setNavLabelMode(mode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) activePillText else textPrimary
                            )
                        }
                    }
                }
            }
        }

        // 7. ALMACENAMIENTO & CACHÉ
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = textPrimary, modifier = Modifier.size(18.dp))
                    Text("ALMACENAMIENTO & CACHÉ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Libera memoria borrando carátulas e imágenes en caché de artistas locales.", fontSize = 11.sp, color = textSecondary)

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF1E1D1A) else Color.White)
                        .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        .clickable {
                            Toast.makeText(context, "Caché de carátulas liberada con éxito", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                        Text("Limpiar Caché de Carátulas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                }
            }
        }

        // 8. PRIVACIDAD
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(subCardBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = textPrimary, modifier = Modifier.size(20.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("100% Privado y Sin Rastreo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("Toda la música y metadatos se procesan localmente en tu dispositivo.", fontSize = 10.sp, color = textSecondary)
                }
            }
        }
    }

    // Modals
    SleepTimerModal(
        isOpen = showSleepModal,
        onClose = { showSleepModal = false },
        currentMinutesRemaining = sleepTimerSeconds?.let { it / 60 },
        onSetTimer = { mins ->
            if (mins == null) {
                audioPlayer.cancelSleepTimer()
            } else if (mins > 0) {
                audioPlayer.startSleepTimer(mins)
            }
        },
        isDark = isDark
    )

    StatsModal(
        isOpen = showStatsModal,
        onClose = { showStatsModal = false },
        songs = songs,
        isDark = isDark,
        onPlaySong = { song ->
            audioPlayer.playSong(song, songs)
        }
    )
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String?,
    title: String,
    subtitle: String,
    cardBg: Color,
    borderCol: Color,
    textPrimary: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Gray.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = textSecondary,
                maxLines = 1
            )
        }
    }
}
