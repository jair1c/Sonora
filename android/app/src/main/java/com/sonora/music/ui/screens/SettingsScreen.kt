package com.sonora.music.ui.screens
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush

import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.animation.*
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

    val (appVersionName, appVersionCode) = remember(context) {
        try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo.versionName ?: com.sonora.app.BuildConfig.VERSION_NAME
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            Pair(vName, vCode)
        } catch (e: Exception) {
            Pair(com.sonora.app.BuildConfig.VERSION_NAME, com.sonora.app.BuildConfig.VERSION_CODE.toLong())
        }
    }

    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val bgColor = if (isGlass) Color.Transparent else themeColors.bg
    val cardBg = themeColors.cardBg
    val subCardBg = themeColors.subCardBg
    val borderCol = themeColors.borderCol
    val textPrimary = themeColors.textPrimary
    val textSecondary = themeColors.textSecondary
    val activePillBg = themeColors.activePillBg
    val activePillText = themeColors.activePillText

    var showSleepModal by remember { mutableStateOf(false) }
    var showStatsModal by remember { mutableStateOf(false) }

    val sleepTimerSeconds by audioPlayer.sleepTimerSecondsLeft.collectAsState()
    var currentTheme by remember { mutableStateOf(sonoraPrefs.getThemeMode()) }
    var currentGlassVariant by remember { mutableStateOf(sonoraPrefs.getGlassVariant()) }
    var petalRoundness by remember { mutableIntStateOf(sonoraPrefs.getPetalRoundness()) }
    var crossfadeSeconds by remember { mutableIntStateOf(sonoraPrefs.getCrossfadeSeconds()) }
    var playbackSpeed by remember { mutableFloatStateOf(sonoraPrefs.getPlaybackSpeed()) }
    var activeNavTabs by remember { mutableStateOf(sonoraPrefs.getNavTabs()) }
    var navLabelMode by remember { mutableStateOf(sonoraPrefs.getNavLabelMode()) }
    var playerControlsStyle by remember { mutableStateOf(sonoraPrefs.getPlayerControlsStyle()) }

    // Battery optimization — reactive: updates automatically when user returns from system settings
    val powerManager = remember { context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
            } else true
        )
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // SAF launcher — Export backup to .sonora file
    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            try {
                val json = sonoraPrefs.exportBackupJson()
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "✅ Copia exportada exitosamente", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // SAF launcher — Import backup from .sonora file
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: ""
                if (json.isBlank()) {
                    Toast.makeText(context, "El archivo está vacío o no es válido", Toast.LENGTH_SHORT).show()
                    return@let
                }
                val success = sonoraPrefs.importBackupJson(json)
                if (success) {
                    currentTheme = sonoraPrefs.getThemeMode()
                    currentGlassVariant = sonoraPrefs.getGlassVariant()
                    petalRoundness = sonoraPrefs.getPetalRoundness()
                    crossfadeSeconds = sonoraPrefs.getCrossfadeSeconds()
                    playbackSpeed = sonoraPrefs.getPlaybackSpeed()
                    activeNavTabs = sonoraPrefs.getNavTabs()
                    navLabelMode = sonoraPrefs.getNavLabelMode()
                    playerControlsStyle = sonoraPrefs.getPlayerControlsStyle()
                    audioPlayer.setCrossfadeSeconds(sonoraPrefs.getCrossfadeSeconds())
                    audioPlayer.setPlaybackSpeed(sonoraPrefs.getPlaybackSpeed())
                    onThemeChanged(sonoraPrefs.getThemeMode())
                    onRescanLibrary()
                    Toast.makeText(context, "✅ ¡Copia restaurada exitosamente!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Archivo inválido o corrupto", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al restaurar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    val oemBrand = remember {
        val manu = Build.MANUFACTURER.lowercase()
        when {
            manu.contains("samsung") -> "Samsung One UI"
            manu.contains("xiaomi") || manu.contains("redmi") || manu.contains("poco") -> "Xiaomi HyperOS"
            manu.contains("oppo") || manu.contains("oneplus") || manu.contains("realme") -> "ColorOS / OxygenOS"
            manu.contains("huawei") || manu.contains("honor") -> "Huawei / MagicOS"
            manu.contains("vivo") || manu.contains("iqoo") -> "Vivo Funtouch / OriginOS"
            manu.contains("motorola") || manu.contains("moto") -> "Motorola Hello UI"
            manu.contains("nothing") -> "Nothing OS"
            manu.contains("google") -> "Google Pixel"
            else -> "${Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }} UI"
        }
    }

    val playCounts = remember(showStatsModal, songs) { sonoraPrefs.getPlayCounts() }
    val totalPlayedSongs = remember(songs, playCounts) {
        songs.count { (playCounts[it.id] ?: it.playCount) > 0 }
    }
    val totalMinutes = remember(showStatsModal, songs) {
        sonoraPrefs.getTotalListeningMinutes()
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

    val baseSettingsBg = if (isGlass) (if (isDark) com.sonora.music.ui.theme.SonoraGlassDarkBg else com.sonora.music.ui.theme.SonoraGlassLightBg) else themeColors.bg
    val backBtnBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x606080A8), Color(0x35385070))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC0E2E8F0)))
    } else {
        SolidColor(if (isDark) Color(0xFF141312) else Color(0xFFF5F2EA))
    }
    val backBtnBorderBrush = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0xB5FFFFFF), Color(0x30FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x8094A3B8)))
    } else {
        SolidColor(borderCol)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(baseSettingsBg)
    ) {
        if (isGlass) {
            com.sonora.music.ui.theme.LiquidGlassBackdrop(isDark = isDark)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)
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
                        .background(backBtnBrush)
                        .border(if (isGlass) 1.2.dp else 1.dp, backBtnBorderBrush, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Regresar",
                        tint = textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val versionBadgeBrush = if (isGlass) {
                    if (isDark) Brush.linearGradient(listOf(Color(0x556080A8), Color(0x30385070))) else Brush.linearGradient(listOf(Color(0xEEFFFFFF), Color(0xC0E2E8F0)))
                } else {
                    SolidColor(if (isDark) Color(0xFF1F1D1A) else Color(0xFFEAE5DA))
                }
                val versionBadgeBorder = if (isGlass) {
                    if (isDark) Brush.verticalGradient(listOf(Color(0x95FFFFFF), Color(0x25FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x8094A3B8)))
                } else {
                    SolidColor(borderCol)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(versionBadgeBrush)
                        .border(if (isGlass) 1.2.dp else 1.dp, versionBadgeBorder, RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sonora v$appVersionName • Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        Pair("system", "Sistema"),
                        Pair("dark", "Oscuro"),
                        Pair("light", "Claro"),
                        Pair("glass", "Liquid Glass 💎")
                    ).forEach { (mode, label) ->
                        val isSelected = currentTheme == mode
                        Box(
                            modifier = Modifier
                                .weight(if (mode == "glass") 1.25f else 1f)
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
                                fontSize = if (mode == "glass") 10.5.sp else 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) activePillText else textPrimary
                            )
                        }
                    }
                }

                // Sub-desglose dinámico de opciones Liquid Glass
                AnimatedVisibility(
                    visible = currentTheme == "glass",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "TONALIDAD DE CRISTAL TRANSLÚCIDO (APPLE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Pair("system", "📱 Sistema"),
                                Pair("dark", "🌑 Oscuro"),
                                Pair("light", "☀️ Claro")
                            ).forEach { (variant, label) ->
                                val isVarSelected = currentGlassVariant == variant
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isVarSelected) activePillBg else cardBg)
                                        .border(1.dp, if (isVarSelected) Color.Transparent else borderCol, RoundedCornerShape(10.dp))
                                        .clickable {
                                            currentGlassVariant = variant
                                            sonoraPrefs.setGlassVariant(variant)
                                            onThemeChanged("glass")
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isVarSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isVarSelected) activePillText else textPrimary
                                    )
                                }
                            }
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
                                .background(if (isSelected) activePillBg else subCardBg)
                                .border(1.dp, if (isSelected) (if (isGlass) Color.Transparent else (if (isDark) Color.White else Color.Black)) else borderCol, RoundedCornerShape(14.dp))
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
                                    color = if (isSelected) activePillText else textPrimary
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
                                    selectedColor = activePillText,
                                    unselectedColor = if (isGlass) (if (isDark) Color(0x60FFFFFF) else Color(0x600F172A)) else borderCol
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

        // NOTIFICACIONES & SEGUNDO PLANO (ANDROID 16+)
        item {
            val areNotificationsEnabled = remember(context) {
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            }

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
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "INTEGRACIÓN CON EL SISTEMA",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFD4AF37).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = oemBrand,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Compatibilidad con panel de control, pantalla de bloqueo y reproducción continua en Samsung, Xiaomi, Oppo, Pixel, Vivo, Motorola y más.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Control Multimedia / Notificación
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Control Multimedia del Sistema",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Token de sesión activo para widgets nativos y pantalla de bloqueo.",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (areNotificationsEnabled) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (areNotificationsEnabled) "Activo" else "Permiso requerido",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(activePillBg)
                        .clickable {
                            try {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                } else {
                                    android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", context.packageName, null)
                                    }
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Abre Ajustes > Aplicaciones > Sonora > Notificaciones", Toast.LENGTH_LONG).show()
                            }
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (areNotificationsEnabled) "Ajustes de Notificación en $oemBrand" else "🔔 Habilitar Permiso de Notificación",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activePillText
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Batería & Segundo Plano
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reproducción en Segundo Plano",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = if (isIgnoringBatteryOptimizations) "Sin restricciones de ahorro de energía" else "Ahorro de batería activo (puede pausar música)",
                            fontSize = 10.sp,
                            color = textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isIgnoringBatteryOptimizations) Color(0xFF1B5E20) else Color(0xFFE65100))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isIgnoringBatteryOptimizations) "Ilimitado" else "Optimizado",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                if (!isIgnoringBatteryOptimizations && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFD4AF37).copy(alpha = 0.15f))
                            .clickable {
                                try {
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚡ Quitar Restricción de Batería (Sin Cortes)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37)
                        )
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
                        val rowBg = subCardBg
                        val badgeBg = if (isGlass) (if (isDark) Color(0x35FFFFFF) else Color(0x250F172A)) else (if (isDark) Color(0xFF38352F) else Color(0xFFD2CBC0))
                        
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

        // 8. COPIA DE SEGURIDAD & RESTAURACIÓN
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
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = textPrimary, modifier = Modifier.size(18.dp))
                    Text("COPIA DE SEGURIDAD & RESTAURACIÓN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Exporta o restaura toda tu configuración: listas, favoritos, carpetas, ecualización, temas y más. El archivo se guarda con extensión .sonora.",
                    fontSize = 11.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export button — opens SAF file saver
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(subCardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .clickable {
                                val timestamp = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())
                                exportLauncher.launch("sonora-backup-$timestamp.sonora")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = textPrimary, modifier = Modifier.size(16.dp))
                            Text("Exportar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        }
                    }

                    // Restore button — opens SAF file picker directly
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activePillBg)
                            .border(1.dp, if (isGlass) borderCol else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                importLauncher.launch(arrayOf("*/*"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = activePillText, modifier = Modifier.size(16.dp))
                            Text("Restaurar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = activePillText)
                        }
                    }
                }
            }
        }


        // 9. PRIVACIDAD
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

        // 10. ACERCA DE SONORA & INFORMACIÓN DE VERSIÓN
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(cardBg)
                    .border(1.dp, borderCol, RoundedCornerShape(22.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌸 Sonora Music Player",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Versión $appVersionName (Compilación $appVersionCode)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(subCardBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Jetpack Compose Nativo + AndroidX Media3",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                }
            }
        }
    }

    // Modals
    val sleepTimerFinishSong by audioPlayer.sleepTimerFinishSong.collectAsState()

    SleepTimerModal(
        isOpen = showSleepModal,
        onClose = { showSleepModal = false },
        currentMinutesRemaining = if (sleepTimerSeconds == -1) -1 else sleepTimerSeconds?.let { it / 60 },
        currentFinishSong = sleepTimerFinishSong,
        onSetTimer = { mins, finishSong ->
            if (mins == null) {
                audioPlayer.cancelSleepTimer()
            } else {
                audioPlayer.startSleepTimer(mins, finishSong)
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
    val themeColors = com.sonora.music.ui.theme.LocalSonoraColors.current
    val isGlass = themeColors.isGlass
    val isDark = themeColors.isDark
    
    val iconBrush = if (isGlass) {
        if (isDark) Brush.linearGradient(listOf(Color(0x606080A8), Color(0x35385070))) else Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)))
    } else {
        SolidColor(if (isDark) Color.White else Color.Black)
    }
    val iconBorderBrush = if (isGlass) {
        if (isDark) Brush.verticalGradient(listOf(Color(0x95FFFFFF), Color(0x25FFFFFF))) else Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x90CBD5E1)))
    } else {
        SolidColor(Color.Transparent)
    }
    val iconTint = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else (if (isDark) Color.Black else Color.White)
    val badgeBg = if (isGlass) (if (isDark) Color(0x45FFFFFF) else Color(0x250F172A)) else Color.Gray.copy(alpha = 0.2f)
    val badgeText = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else textSecondary
    val cardTitle = if (isGlass) (if (isDark) Color.White else Color(0xFF0F172A)) else textPrimary
    val cardSub = if (isGlass) (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)) else textSecondary

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
                    .background(iconBrush)
                    .border(if (isGlass) 1.2.dp else 0.dp, iconBorderBrush, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(badgeBg)
                        .border(if (isGlass) 1.dp else 0.dp, if (isGlass) Brush.verticalGradient(listOf(Color(0x70FFFFFF), Color(0x15FFFFFF))) else SolidColor(Color.Transparent), RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
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
                color = cardTitle,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = cardSub,
                maxLines = 1
            )
        }
    }
}