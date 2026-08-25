# 🌸 Sonora Music Player (v3.0.0)
### *Reproductor de Música Nativo Audiófilo con Diseño Orgánico y Arquitectura Jetpack Compose para Android*

---

## 📖 1. Visión y Concepto del Proyecto

**Sonora** es un reproductor de música local de alta fidelidad diseñado para brindar una experiencia auditiva pura, libre de distracciones, con una estética visual basada en **formas orgánicas, vinilo en flor de 8 pétalos simétricos, tipografía elegante y una paleta tonal dual (Warm Linen Beige en Modo Claro / Deep Velvet OLED en Modo Oscuro)**.

El reproductor opera **100% de manera local y privada**, construido sobre una arquitectura **100% Nativa en Kotlin y Jetpack Compose** con el motor **AndroidX Media3 (ExoPlayer)**, integrando un procesador DSP de ecualización nativa de 10 bandas, fundido cruzado (*crossfade*), lectura instantánea de metadatos y carátulas embebidas en archivos `.flac`, `.mp3`, `.m4a`, `.wav`, `.ogg`, sincronización de letras `.lrc`, y un sistema de personalización visual sin precedentes.

---

## 🛠️ 2. Stack Tecnológico Nativo

- **Lenguaje Principal**: Kotlin 2.0+ (JVM 17 / Android SDK 34 & 35)
- **Framework de UI**: Jetpack Compose (BOM 2024.10.01) + Material3 + Material Icons Extended
- **Motor de Audio & Sesión**:
  - `androidx.media3:media3-exoplayer:1.5.0`
  - `androidx.media3:media3-session:1.5.0`
  - `androidx.media3:media3-common:1.5.0`
  - `androidx.media3:media3-ui:1.5.0`
- **Procesamiento de Señal DSP**: `android.media.audiofx.Equalizer` y `BassBoost` nativo de 10 bandas
- **Gestión Asíncrona & Reactividad**: Kotlin Coroutines + `StateFlow` / `SharedFlow`
- **Carga de Imágenes & Carátulas**: `io.coil-kt:coil-compose:2.7.0` con soporte para Content URIs de MediaStore
- **Tipografía**: Plus Jakarta Sans / Outfit vía `androidx.compose.ui:ui-text-google-fonts:1.7.5`
- **Persistencia de Datos**: SharedPreferences seguro en `SonoraPreferences.kt` con serialización JSON
- **Herramientas de Compilación**: Gradle 8.14 + Android Gradle Plugin 8.9 + R8 / ProGuard Minification

---

## 📜 3. Historial Cronológico Completo de Cambios (Changelog de v0.1 a v3.0.0)

### 🔹 Fase 1: Arquitectura Inicial y Motor de Reproducción Local (v0.1 - v0.5)
- **Problema Inicial**: Prototipo híbrido en WebView sin persistencia ni notificación multimedia en la barra de Android; al salir de la app o bloquear el teléfono, la música se pausaba y el sistema cerraba el proceso.
- **Solución**:
  - Se implementó un servicio nativo en Android con `MediaSessionCompat` y `NotificationManagerCompat` en primer plano (*Foreground Service*).
  - Se añadieron controles multimedia directos en la notificación (Reproducir, Pausar, Siguiente, Anterior).
  - Se integró el escaneo del almacenamiento del dispositivo (`MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`).

---

### 🔹 Fase 2: Soporte Audiófilo & Corrección de Bugs de Audio (v0.6 - v1.0)
- **Problema de Doble Reproducción / Desfase**: Instancias concurrentes generaban eco al cambiar rápido de canción.
  - *Solución*: Implementación de un ciclo de vida estricto con parada y liberación de buffers antes de montar la nueva pista.
- **Letras Sincronizadas (.LRC y Embebidas FLAC)**:
  - Soporte de archivos `.lrc` externos y extracción de metadatos incrustados en archivos FLAC sin conexión a internet.
- **Fundido Cruzado (Crossfade)**:
  - Sistema de mezcla de audio programable (1s a 12s) que realiza un desvanecimiento suave (*fade-out / fade-in*).

---

### 🔹 Fase 3: Rediseño Visual Orgánico - El Vinilo en Flor de 8 Pétalos (v1.1 - v1.5)
- **Geometría Floral Simétrica**:
  - Fórmulas trigonométricas de 8 pétalos simétricos exactos:
    $$r(\theta) = R_0 + A \cdot \cos(8\theta)$$
- **Contorno Ondulado Interactivo con Scrubber**:
  - El avance de la canción se dibuja sobre el contorno ondulado con un punto deslizable (*knob*) para adelantar o retroceder la canción (*seek* táctil).

---

### 🔹 Fase 4: Personalización Total de Navegación y Herramientas (v1.6 - v1.9)
- **Barra de Navegación Dinámica**:
  - En **Ajustes**, el usuario puede activar, desactivar, reordenar y seleccionar qué pestañas mostrar.
- **Modales Orgánicos**:
  - Eliminación de alertas nativas del sistema, reemplazándolas por diálogos cohesivos con la estética de la app.

---

### 🔹 Fase 5: Modo Oscuro OLED, Persistencia y Gestos (v2.0 - v2.1.0)
- **Sistema de Temas Dual**:
  - ☀️ **Modo Claro**: Warm Paper / Luxury Linen (`#F5F2EA` / `#EAE5DA`).
  - 🌙 **Modo Oscuro**: Deep OLED Velvet (`#0F0E0D` / `#1A1917` / `#2A2824`).
- **Mini-Reproductor Flotante Persistente**:
  - Permanece visible en pausa y reanuda la canción exactamente desde el segundo pausado.

---

### 🔹 Fase 6: La Gran Transformación Nativa Jetpack Compose & Media3 (v3.0.0)
- 🚀 **Reescritura Nativa 100% en Kotlin & Jetpack Compose**:
  - Sustitución integral del WebView por arquitectura declarativa moderna Compose a 120 FPS sin retardos de renderizado.
- ⚡ **Motor AndroidX Media3 ExoPlayer con Respaldo FLAC**:
  - Implementación de `SonoraAudioPlayer.kt` con `DefaultExtractorsFactory` (búsqueda de tasa de bits constante) y **mecanismo de recuperación automática a lectura directa de archivo local (`Uri.fromFile`)**, garantizando reproducción perfecta de cualquier archivo FLAC o Hi-Fi (ej. `23 - Morat.flac`).
- 🌸 **Carátula Floral con Margen Áureo del 20%**:
  - Dimensionamiento del contorno de proceso (*Wavy Scrubber*) y la carátula en flor de 8 pétalos (`242.dp`), garantizando que al expandirse al 100% durante la reproducción mantenga **un 20% exacto de margen libre respecto al contorno de onda**.
- 🎛️ **5 Estilos de Controles de Reproducción de Lujo**:
  1. *Cápsula Flotante (Luxury Dock)*
  2. *Círculos Suizos (Swiss Circles)*
  3. *Flor Orgánica (Organic Flower)*
  4. *Hi-Fi Squircle*
  5. *Píldora Waveform*
- 🔀 **Cola de Reproducción & Modo Aleatorio Irrepetible**:
  - Al activar Aleatorio, la cola se baraja con entropía basada en nanosegundos (`Random(System.nanoTime())`) generando secuencias únicas e irrepetibles.
  - Al desactivar Aleatorio, la cola retorna inmediatamente a su orden original de biblioteca, conservando la posición de reproducción sin cortes.
- 📊 **sonoraStats 2.0**:
  - Estadísticas precisas de tiempo de escucha acumulado (minutos y horas) y Top 5 de canciones más escuchadas, sincronizadas en tiempo real tanto en la tarjeta de Ajustes como en el modal dedicado.
- 🎛️ **Personalización Avanzada de Barra Inferior**:
  - 3 modos de etiquetas: *Siempre*, *Solo Iconos*, y *Dinámica (etiqueta solo en la pestaña activa)*.
  - Reordenamiento de pestañas con tarjetas de alto contraste e insignias circulares numeradas.
- 🌓 **Consistencia Universal de Temas**:
  - Todas las pantallas (Biblioteca, Reproductor, Cola, Ajustes, Modales y Hojas Inferiores) respetan estrictamente el modo Claro, Oscuro o Sistema.

---

## 📁 4. Estructura del Código Nativo

```
c:\app\luxTune\android\app\src\main\java\com\sonora\music\
├── MainActivity.kt                      # Actividad principal con soporte Edge-to-Edge y barra de estado
├── data\
│   ├── model\
│   │   ├── Song.kt                      # Modelo de datos de canción (ID, título, artista, carátula, calidad)
│   │   ├── LyricLine.kt                 # Modelo de línea de letra sincronizada (tiempo ms y texto)
│   │   └── Playlist.kt                  # Modelo de listas de reproducción personalizadas
│   ├── local\
│   │   └── SonoraPreferences.kt         # Gestión thread-safe de preferencias, temas, tabs y estadísticas
│   └── repository\
│       └── MediaStoreRepository.kt      # Repositorio optimizado de lectura MediaStore y parser de letras .LRC
├── service\
│   ├── SonoraAudioPlayer.kt             # Motor de audio AndroidX Media3 ExoPlayer, cola, shuffle y crossfade
│   ├── SonoraMediaSessionService.kt     # Servicio en primer plano para reproducción en segundo plano
│   └── SonoraEqualizerManager.kt        # Gestor de ecualizador DSP de 10 bandas y refuerzo de graves
└── ui\
    ├── components\
    │   ├── EqualizerModal.kt            # Modal de ecualizador DSP con controles deslizantes
    │   ├── OrganicShapes.kt             # Algoritmo de forma de flor de 8 pétalos simétricos
    │   ├── QueueBottomSheet.kt          # Hoja inferior de cola de reproducción con reordenamiento
    │   ├── SleepTimerModal.kt           # Modal de temporizador de apagado
    │   ├── SongItemRow.kt               # Fila de canción con menú contextual de 3 puntos
    │   ├── SongMenuBottomSheet.kt       # Menú de acciones (favoritos, detalles, agregar a lista)
    │   ├── StatsModal.kt                # Modal de estadísticas sonoraStats 2.0 (Top 5 canciones)
    │   ├── TagEditorModal.kt            # Editor de metadatos ID3
    │   └── WavyScrubberRing.kt          # Anillo de progreso circular ondulado con control táctil
    ├── screens\
    │   ├── NativeLibraryScreen.kt       # Pantalla de biblioteca (Canciones, Álbumes, Artistas, Carpetas, Listas)
    │   ├── NativePlayerScreen.kt        # Pantalla completa de reproductor con los 5 estilos de controles
    │   └── SettingsScreen.kt            # Pantalla de ajustes, personalización de barra y temas
    └── theme\
        ├── Color.kt                     # Paleta de colores Warm Linen / Deep Velvet OLED
        ├── Theme.kt                     # Proveedor de tema Compose SonoraTheme
        └── Type.kt                      # Tipografía Plus Jakarta Sans
```

---

## 🚀 5. Guía de Ejecución y Compilación

### Requisitos Previos:
- Java Development Kit (JDK 17 o superior)
- Android SDK (Plataforma 34 o 35)
- Dispositivo Android conectado con Depuración USB habilitada

### Comandos de Compilación & Firma:
```powershell
# 1. Compilar APK Release Firmada y Optimizada con R8/ProGuard
cd android
.\gradlew.bat assembleRelease

# 2. Instalar APK Release directamente en el dispositivo conectado
adb install -r app\build\outputs\apk\release\app-release.apk
```

---

## 🔐 6. Credenciales de Firma de Producción (Release Keystore)

| Parámetro | Valor |
|---|---|
| **Ruta del Keystore** | `android/app/sonora-release-key.jks` |
| **Alias de la Llave** | `sonora-key` |
| **Contraseña del Almacén (Store Password)** | `SonoraMusic2026!` |
| **Contraseña de la Llave (Key Password)** | `SonoraMusic2026!` |
| **Algoritmo & Tamaño** | RSA 2048-bit (Validez: 10,000 días / 27 años) |

---

## 💾 7. Claves de Almacenamiento Local (`SonoraPreferences`)

| Clave | Tipo | Descripción |
|---|---|---|
| `theme_mode` | String | Modo de tema: `"system"`, `"light"`, `"dark"` |
| `petal_roundness` | Int | Amplitud y redondez de la flor de 8 pétalos (0 a 100) |
| `crossfade_seconds` | Int | Segundos de fundido cruzado (0 a 12s) |
| `nav_tabs` | String JSON | Lista ordenada de pestañas activas en la barra de navegación |
| `nav_label_mode` | String | Modo de etiquetas: `"always"`, `"icons_only"`, `"active_only"` |
| `player_controls_style` | String | Estilo de controles: `"dock"`, `"circles"`, `"organic"`, `"squircle"`, `"waveform"` |
| `liked_song_ids` | Set\<String\> | Conjunto de IDs de canciones marcadas como favoritas |
| `play_counts` | String JSON | Diccionario de número de reproducciones por ID de canción |
| `total_listening_minutes` | Int | Minutos acumulados totales de reproducción de audio |
| `eq_enabled` | Boolean | Estado del ecualizador DSP (activado/desactivado) |
| `eq_preset_idx` | Int | Índice del perfil acústico seleccionado |
| `eq_band_levels` | String JSON | Ganancia por banda en mB (-1000 a +1000) |
| `eq_bass_boost` | Int | Nivel de refuerzo de graves (0 a 1000) |


