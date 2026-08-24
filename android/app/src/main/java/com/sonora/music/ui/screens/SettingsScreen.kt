package com.sonora.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
fun SettingsScreen(
    sonoraPrefs: SonoraPreferences,
    audioPlayer: SonoraAudioPlayer,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onThemeChanged: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) SonoraObsidianDark else SonoraPaperBeige
    val cardBg = if (isDark) SonoraObsidianCard else SonoraPaperCard
    val textColor = if (isDark) Color.White else Color(0xFF121212)
    val subtextColor = if (isDark) Color(0xFF8A857B) else Color(0xFF75726B)
    val context = LocalContext.current

    var showSleepTimerModal by remember { mutableStateOf(false) }
    var showImportModal by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    val sleepTimerSeconds by audioPlayer.sleepTimerSecondsLeft.collectAsState()
    var currentTheme by remember { mutableStateOf(sonoraPrefs.getThemeMode()) }

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
                text = "AJUSTES",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Audio & Equalizer Section
            item {
                SectionTitle("AUDIO & SONIDO", subtextColor)
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.GraphicEq,
                    title = "Ecualizador de Audio",
                    subtitle = "Presets de sonido y refuerzo de graves",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    onClick = onOpenEqualizer
                )
            }

            item {
                val sleepSubtitle = if (sleepTimerSeconds != null) {
                    val m = sleepTimerSeconds!! / 60
                    val s = sleepTimerSeconds!! % 60
                    "Activo: %02d:%02d restantes".format(m, s)
                } else {
                    "Apaga la música automáticamente"
                }

                SettingsActionCard(
                    icon = Icons.Default.Timer,
                    title = "Temporizador de Apagado (Sleep Timer)",
                    subtitle = sleepSubtitle,
                    cardBg = cardBg,
                    textColor = if (sleepTimerSeconds != null) Color(0xFF10B981) else textColor,
                    subtextColor = subtextColor,
                    onClick = { showSleepTimerModal = true }
                )
            }

            // 2. Personalization / Theme
            item {
                SectionTitle("APARIENCIA & TEMA", subtextColor)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Tema Visual", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOptionButton(
                                label = "Obsidian",
                                isSelected = currentTheme == "dark",
                                textColor = textColor,
                                bgColor = bgColor,
                                cardBg = cardBg,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentTheme = "dark"
                                    sonoraPrefs.setThemeMode("dark")
                                    onThemeChanged("dark")
                                }
                            )
                            ThemeOptionButton(
                                label = "Warm Paper",
                                isSelected = currentTheme == "light",
                                textColor = textColor,
                                bgColor = bgColor,
                                cardBg = cardBg,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentTheme = "light"
                                    sonoraPrefs.setThemeMode("light")
                                    onThemeChanged("light")
                                }
                            )
                            ThemeOptionButton(
                                label = "Sistema",
                                isSelected = currentTheme == "system",
                                textColor = textColor,
                                bgColor = bgColor,
                                cardBg = cardBg,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    currentTheme = "system"
                                    sonoraPrefs.setThemeMode("system")
                                    onThemeChanged("system")
                                }
                            )
                        }
                    }
                }
            }

            // 3. Backup & Restore
            item {
                SectionTitle("COPIA DE SEGURIDAD & DATOS", subtextColor)
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Backup,
                    title = "Exportar Copia de Seguridad",
                    subtitle = "Copia tus favoritos y listas al portapapeles en JSON",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    onClick = {
                        val json = sonoraPrefs.exportBackupJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Sonora Backup", json))
                        Toast.makeText(context, "Copia copiada al portapapeles", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Restore,
                    title = "Importar Copia de Seguridad",
                    subtitle = "Restaura tus listas y favoritos desde un JSON",
                    cardBg = cardBg,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    onClick = { showImportModal = true }
                )
            }

            // 4. About Info
            item {
                SectionTitle("INFORMACIÓN", subtextColor)
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBg)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Sonora Music Player", fontSize = 14.sp, fontWeight = FontWeight.Black, color = textColor)
                        Text("Versión 2.0.0 Nativa (Jetpack Compose + Media3 ExoPlayer)", fontSize = 11.sp, color = subtextColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("100% Offline • Audio Hi-Res 24-bit • 120 FPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        // Sleep Timer Dialog
        if (showSleepTimerModal) {
            AlertDialog(
                onDismissRequest = { showSleepTimerModal = false },
                title = { Text("Temporizador de Apagado", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        audioPlayer.startSleepTimer(mins)
                                        showSleepTimerModal = false
                                        Toast.makeText(context, "Temporizador iniciado ($mins min)", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("$mins minutos", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor)
                            }
                        }

                        if (sleepTimerSeconds != null) {
                            TextButton(
                                onClick = {
                                    audioPlayer.cancelSleepTimer()
                                    showSleepTimerModal = false
                                    Toast.makeText(context, "Temporizador cancelado", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar Temporizador Activo", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerModal = false }) {
                        Text("Cerrar", color = subtextColor)
                    }
                }
            )
        }

        // Import JSON Dialog
        if (showImportModal) {
            AlertDialog(
                onDismissRequest = { showImportModal = false },
                title = { Text("Importar Copia de Seguridad", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Pega el JSON de tu copia de seguridad:", fontSize = 12.sp, color = subtextColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importJsonText,
                            onValueChange = { importJsonText = it },
                            placeholder = { Text("{ \"version\": 1, ... }") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val success = sonoraPrefs.importBackupJson(importJsonText.trim())
                            if (success) {
                                Toast.makeText(context, "Copia restaurada con éxito", Toast.LENGTH_SHORT).show()
                                showImportModal = false
                            } else {
                                Toast.makeText(context, "JSON inválido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Importar", fontWeight = FontWeight.Bold, color = textColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportModal = false }) {
                        Text("Cancelar", color = subtextColor)
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cardBg: Color,
    textColor: Color,
    subtextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .clickable { onClick() }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(subtitle, fontSize = 11.sp, color = subtextColor)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = subtextColor)
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    isSelected: Boolean,
    textColor: Color,
    bgColor: Color,
    cardBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) textColor else bgColor)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) bgColor else textColor
        )
    }
}
