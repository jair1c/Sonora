# 🌸 Sonora Music Player (v3.9.2)
### *Reproductor de Música Nativo Audiófilo con Identidad Obsidiana & Oro Champaña, Arquitectura Jetpack Compose y Motor de Audio Media3 para Android*

---

## 📖 1. Visión y Concepto del Proyecto

**Sonora** es un reproductor de música local de alta fidelidad diseñado para brindar una experiencia auditiva pura, libre de distracciones, con una estética visual basada en **formas orgánicas, flor de loto acústica y disco de vinilo en tonos Negro Obsidiana profundo y Oro Champaña metálico brillante**.

El reproductor opera **100% de manera local y privada**, construido sobre una arquitectura **100% Nativa en Kotlin y Jetpack Compose** con el motor **AndroidX Media3 (ExoPlayer)**, integrando un procesador DSP de ecualización nativa de 10 bandas, fundido cruzado (*crossfade*) con motor dual DJ de reproducción superpuesta, visualizador de espectro FFT en vivo, notificación multimedia en primer plano persistente, lectura instantánea de metadatos y carátulas embebidas en archivos `.flac`, `.mp3`, `.m4a`, `.wav`, `.ogg`, sincronización de letras `.lrc`, y un sistema de personalización visual sin precedentes.

---

## 🛠️ 2. Stack Tecnológico Nativo

- **Lenguaje Principal**: Kotlin 2.0+ (JVM 21 / Android SDK 34 & 35)
- **ID de Paquete / Namespace**: `com.sonora.app`
- **Framework de UI**: Jetpack Compose (BOM 2024.10.01) + Material3 + Material Icons Extended
- **Motor de Audio & Sesión**:
  - `androidx.media3:media3-exoplayer:1.5.0` (Instancia principal de sesión y cola)
  - `crossfadePlayer` (Instancia auxiliar para mezcla DJ superpuesta simultánea)
  - `androidx.media3:media3-session:1.5.0`
  - `androidx.media3:media3-common:1.5.0`
  - `androidx.media3:media3-ui:1.5.0`
- **Procesamiento de Señal DSP & Visualización**:
  - `android.media.audiofx.Equalizer` y `BassBoost` nativo de 10 bandas
  - `SonoraVisualizerManager` con motor dual FFT por hardware + respaldo de onda armónica procedural
- **Gestión Asíncrona & Reactividad**: Kotlin Coroutines + `StateFlow` / `SharedFlow`
- **Carga de Imágenes & Carátulas**: `io.coil-kt:coil-compose:2.7.0` con soporte para Content URIs de MediaStore
- **Tipografía**: Plus Jakarta Sans / Outfit vía `androidx.compose.ui:ui-text-google-fonts:1.7.5`
- **Persistencia de Datos**: SharedPreferences seguro en `SonoraPreferences.kt` con serialización JSON
- **Herramientas de Compilación**: Gradle 8.14 + Android Gradle Plugin 8.9 + R8 / ProGuard Minification

---

## 📜 3. Historial Cronológico Completo de Cambios

### 🔹 Fase 1: Arquitectura Inicial y Motor de Reproducción Local (v0.1 - v0.5)
- Se implementó un servicio nativo en Android con `MediaSession` y `NotificationManagerCompat` en primer plano (*Foreground Service*).
- Se añadieron controles multimedia directos en la notificación (Reproducir, Pausar, Siguiente, Anterior).
- Se integró el escaneo del almacenamiento del dispositivo (`MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`).

### 🔹 Fase 8: Respaldo Dinámico de Reproducción, Visualizador FFT y Estabilidad 30s (v3.2.0)
- **Sincronización Continua**: Se sincronizó la pista activa y la posición en milisegundos en tiempo real durante cada segundo de reproducción, garantizando que el estado restaurado coincida exactamente con la canción que sonaba.
- **Visualizador Reactivo**: Creación del motor dual `SonoraVisualizerManager.kt` que combina lectura FFT en tiempo real con animación armónica procedural fluida cuando el hardware lo requiera.
- **WakeLock Local**: Activación de `C.WAKE_MODE_LOCAL` para evitar suspensiones de audio en reposo.

### 🔹 Fase 9: Nueva Identidad Visual Lujosa & Notificación Multimedia Persistente (v3.3.0)
- **Nuevo Imagotipo & Asset Branding**: Rediseño completo de la iconografía de la aplicación con estética Negro Obsidiana y Oro Champaña (*Lotus Acoustic Flower & Vinyl Wave*), exportada en todas las densidades mipmap (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) y assets vectoriales adaptativos.
- **Corrección de Watchdog de 30 Segundos**: Eliminación de llamadas directas a `startForegroundService` no gestionadas que causaban la muerte del proceso a los 30s exactos por el temporizador estricto de Android.
- **Notificación en Primer Plano Media3**: Vinculación directa entre `SonoraMediaService` y `DefaultMediaNotificationProvider` con canal `sonora_music_playback`, control de visibilidad pública en pantalla de bloqueo y escucha reactiva de eventos `onIsPlayingChanged` y `onMediaItemTransition`.

### 🔹 Fase 10: Fundido Cruzado DJ Dual-Engine & Migración de Paquete `com.sonora.app` (v3.4.0)
- **True Dual-Engine DJ Crossfade**: Implementación de arquitectura de doble reproductor ExoPlayer simultáneo (`player` + `crossfadePlayer`). Al restar el tiempo de crossfade (e.g. 10s), la cola de la Canción A continúa atenuándose gradualmente mientras la Canción B comienza inmediatamente desde el segundo 0 a ganar volumen simultáneamente. ¡Cero silencios, mezcla 100% superpuesta y fluida!
- **Soporte en Salto de Pistas (*Manual Next Track*)**: Al pulsar Siguiente teniendo activado el fundido cruzado, se activa la misma transición armónica superpuesta entre canciones.
- **Migración Integral a `com.sonora.app`**: Refactorización del espacio de nombres y Application ID en `build.gradle`, `AndroidManifest.xml`, `strings.xml`, `shortcuts.xml`, `Typography.kt` y `SonoraWidgetProvider.kt`.

### 🔹 Fase 11: Corrección de Bordes de Ícono Adaptativo & Vector de Notificación Multimedia (v3.4.1)
- **Eliminación de Insets y Bordes Blancos en Launchers**: Corrección en `mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml` eliminando el `android:inset="16.7%"` del fondo y configurando fondo 100% sólido obsidiana `#161513` con el imagotipo dorado centrado en la zona segura (62% de canvas), eliminando cualquier artefacto blanco en launchers circulares, squircles, Smart Launcher o Pixel Material You.
- **Ícono Vectorial de Notificación de Estado (`ic_notification_sonora.xml`)**: Incorporación de icono vectorial monochrome en `res/drawable/` y configuración explícita en `DefaultMediaNotificationProvider.setSmallIcon(...)` para evitar que Android intente renderizar el XML adaptativo en la barra de notificaciones y desbloquear la visualización instantánea en pantalla de bloqueo y panel multimedia.

### 🔹 Fase 12: Arquitectura Reactiva de Notificación MediaStyle & Compatibilidad Android 16 Baklava (v3.4.2)
- **Notificación Reactiva NotificationCompat.MediaStyle**: Implementación de notificación explícita en `SonoraMediaService` acoplada reactivamente a los flujos `currentSong` e `isPlaying` de `SonoraAudioPlayer`. Incorpora portadas en alta resolución, acciones directas (*Anterior, Play/Pause, Siguiente, Cerrar*) y control `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK` en Android 14/15/16.
- **Gestor de Permiso de Notificación en Ajustes**: Tarjeta de estado en tiempo real en la pantalla de Ajustes que detecta `areNotificationsEnabled()` y permite al usuario abrir la pantalla de configuración del sistema de Android 16 con un solo toque.

### 🔹 Fase 13: Integración de Token MediaSession con Samsung One UI & Panel de Control (v3.4.3)
- **Vinculación de Token de Sesión en MediaStyle**: Se vinculó `MediaSessionCompat.Token` en `MediaStyle.setMediaSession(token)` y `android.mediaSession` en los extras de la notificación. Esto permite a la interfaz de Samsung One UI (One UI 6/7) reconocer a Sonora como un reproductor multimedia activo del sistema e integrarlo directamente en el widget de Control Multimedia del Panel Rápido (Quick Settings), la pantalla de bloqueo de Samsung y el control de salida multimedia.

### 🔹 Fase 14: Compatibilidad Universal Multi-OEM & Receptor de Botones de Auriculares (v3.5.0)
- **Ecosistema Multi-Capa (Xiaomi HyperOS / MIUI, OPPO ColorOS, OnePlus OxygenOS, Vivo OriginOS, Huawei MagicOS, Motorola, Nothing, Pixel)**:
  - Configuración silenciosa y sin vibración del canal de audio para evitar pitidos o vibraciones en transiciones de pista en HyperOS y ColorOS.
  - Asignación de categoría `NotificationCompat.CATEGORY_TRANSPORT` para enrutamiento nativo a los centros de control multimedia de todas las marcas.
  - Registro de `MediaButtonReceiver` y filtro de acción `android.intent.action.MEDIA_BUTTON` para compatibilidad universal con auriculares Bluetooth (Galaxy Buds, Xiaomi Buds, AirPods, etc.) y cables jack/USB-C (1 clic: Play/Pause, 2 clics: Siguiente, 3 clics: Anterior).
  - Tarjeta en Ajustes con detección de fabricante de dispositivo (`Build.MANUFACTURER`) y enlace de 1 toque para deshabilitar la optimización agresiva de batería (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`).

---

## 📂 4. Estructura de Directorios del Código Fuente

```
android/app/src/main/java/com/sonora/music/
├── SonoraNativeActivity.kt              # Actividad principal con Jetpack Compose y gestión de permisos
├── data/
│   ├── local/
│   │   └── SonoraPreferences.kt         # Almacenamiento local SharedPreferences tipado
│   ├── model/
│   │   ├── Album.kt, Artist.kt, Song.kt # Modelos inmutables de datos de audio
│   │   └── SonoraStats.kt               # Métricas y estadísticas de reproducción
│   └── repository/
│       └── MediaStoreRepository.kt      # Repositorio de lectura MediaStore y parser .LRC
├── service/
│   ├── SonoraAudioPlayer.kt             # Motor ExoPlayer, cola, shuffle y crossfade
│   ├── SonoraMediaService.kt            # Servicio MediaSessionService y notificación en primer plano
│   ├── SonoraVisualizerManager.kt       # Gestor dual de espectro de audio FFT y armónicos
│   └── SonoraEqualizerManager.kt        # Gestor DSP de 10 bandas y BassBoost
├── ui/
│   ├── components/
│   ├── screens/
│   │   ├── NativeLibraryScreen.kt       # Biblioteca completa (Canciones, Álbumes, Artistas, Carpetas)
│   │   ├── NativePlayerScreen.kt        # Reproductor floral con 5 estilos de controles y visualizador en vivo
│   │   ├── SettingsScreen.kt            # Ajustes, personalización y temas
│   │   └── WelcomeScreen.kt             # Pantalla de bienvenida orgánica
│   └── theme/
└── widget/
    └── SonoraWidgetProvider.kt          # Widget interactivo tipo cápsula para la pantalla de inicio
```

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

### 🔹 Fase 7: Servicio de Fondo Media3, Persistencia Continua & Escáner Multi-Formato Audiófilo (v3.1.0)
- 🔔 **Notificación Multimedia en Primer Plano y Pantalla de Bloqueo (`MediaSessionService`)**:
  - Creación de `SonoraMediaService.kt` vinculado a AndroidX Media3 con gestión automática de foco de audio y controles interactivos en la barra de estado y pantalla de bloqueo.
  - Solicitud dinámica en tiempo de ejecución del permiso `POST_NOTIFICATIONS` para Android 13+ (Tiramisu / Android 14 / Android 15).
- ⏱️ **Persistencia Continua & Reanudación al Segundo Exacto**:
  - Guardado en segundo plano de la pista activa, cola de reproducción y posición en milisegundos en `SonoraPreferences.kt`.
  - Restauración instantánea del reproductor flotante y precarga de ExoPlayer al abrir o reiniciar la app, reanudando la música desde el segundo exacto donde se detuvo.
- 🎼 **Escáner Universal Multi-Formato Audiófilo**:
  - Eliminación de filtros que descartaban archivos sin duración previa (`DURATION = 0`).
  - Soporte exhaustivo para `.flac`, `.wav`, `.m4a`, `.mp3`, `.aac`, `.ogg`, `.opus`, `.wma`, `.alac`, `.aiff`, `.dsf`, `.dff`, `.ape`, `.mid`.
  - Búsqueda de respaldo en `MediaStore.Files` y lectura en caliente de etiquetas y duración mediante `MediaMetadataRetriever`.
- 🎚️ **Ecualizador Responsivo Orgánico**:
  - Ajuste del modal del ecualizador a su contenido real (`wrapContentHeight`), eliminando espacios vacíos bajo la tarjeta de refuerzo de graves.
- 👆 **Navegación Fluida al Tocar la Canción en Reproducción**:
  - Al pulsar la pista activa en cualquier lista o pantalla de detalle, la app despliega el reproductor completo sin reiniciar la canción ni perder la posición.

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

---

## 🚀 Fase 15 — Crossfade sin Corte Abrupto (v3.5.1)

### Problema Detectado
Al inicio del crossfade (cuando faltaban los N segundos configurados), se producía un **corte perceptible de ~50–200 ms** en el audio. El sonido de la canción A se interrumpía abruptamente antes de que empezara la mezcla con la canción B.

### Causa Raíz
En el método `performCrossfadeTransition()`, la canción B se cargaba en el momento exacto del trigger con:
```
player.setMediaItems(...) → player.prepare() → player.play()
```
El método `prepare()` tarda entre 50 y 200 ms en abrir el archivo de audio, inicializar el codec y llenar el buffer de decodificación. Durante ese lapso, el audio de la canción A ya había sido interrumpido, generando el silencio perceptible.

### Solución Implementada (Preloading Anticipado)

#### `SonoraAudioPlayer.kt`
- **`preloadNextSong(nextSong: Song)`**: Nuevo método privado que carga la canción B silenciosamente en el `crossfadePlayer` a volumen `0f` con `playWhenReady = false`. Se llama **2× antes** del inicio del crossfade (si crossfade = 10s, el preload ocurre 20s antes del fin de la canción).
- **`performCrossfadeTransition()`**: Refactorizado para:
  1. Detectar si la canción B ya está precargada (`preloadedSong?.id == nextSong.id`)
  2. Si está preloaded: simplemente ejecuta `crossfadePlayer.play()` sin latencia
  3. Si NO está preloaded (ej: usuario salta manualmente): carga y reproduce inmediatamente como fallback
  4. Curva de fade **sinusoidal (ease-in-out)** en lugar de lineal para una transición más natural tipo DJ
  5. **60 pasos/segundo** (16 ms cada uno) en lugar de 20 pasos anteriores → mezcla de volumen más fluida
  6. Al finalizar el fade, el `player` principal retoma el control desde la posición exacta donde llegó el `crossfadePlayer`
- **`startPositionTracking()`**: Se añadió una ventana de preload (`preloadWindowMs = crossfadeMs * 2`) que dispara `preloadNextSong()` antes de que llegue el trigger del crossfade.
- **`playSong()`**: Cancela el preload y resetea `preloadedSong` al cambiar de canción manualmente.
- **`release()`**: Libera el `preloadJob` correctamente al destruir el player.

### Resultado
La transición entre canciones A → B ahora es completamente fluida, sin cortes ni silencios perceptibles. La curva sinusoidal produce una mezcla audiblemente más natural, similar a un fundido DJ profesional.

### Versión
`versionCode 351` · `versionName "3.5.1"` · Commit: `main`

---

## 🚀 Fase 16 — Backup/Restore por Archivo + Indicador de Batería Reactivo (v3.9.2)

### Cambios

#### 1. Botón "Aplicar" / "Restaurar" invisible — Fix visual
El botón de restauración ahora usa fondo dorado `Color(0xFFD4AF37)` con texto `Color(0xFF121212)` (negro), siempre legible independientemente del tema.

#### 2. Sistema de Backup/Restore por Archivo `.sonora`

**Problema anterior**: El backup funcionaba copiando un JSON al portapapeles y pegándolo manualmente en un campo de texto. Era frágil, incómodo y no soportaba todos los ajustes.

**Solución**: Se reemplazó completamente por el **Storage Access Framework (SAF)** de Android:
- **Exportar**: `ActivityResultContracts.CreateDocument("application/octet-stream")` → el sistema muestra el selector de carpeta del dispositivo para guardar `sonora-backup-{fecha}.sonora`
- **Restaurar**: `ActivityResultContracts.OpenDocument()` con filtro `["*/*"]` → el usuario selecciona el archivo `.sonora` desde cualquier carpeta

**Cobertura completa del backup (v2)**:
| Campo | Antes | Ahora |
|---|---|---|
| `themeMode` | ✅ | ✅ |
| `petalRoundness` | ✅ | ✅ |
| `crossfadeSeconds` | ✅ | ✅ |
| `playbackSpeed` | ✅ | ✅ |
| `sortMode` (orden de canciones) | ❌ | ✅ |
| `navLabelMode` | ✅ | ✅ |
| `playerControlsStyle` | ✅ | ✅ |
| `eqPreset` | ✅ | ✅ |
| `bassBoost` | ✅ | ✅ |
| `preAmpGain` | ✅ | ✅ |
| `autoVolumeLeveling` | ❌ | ✅ |
| `likedSongIds` (favoritos) | ✅ | ✅ |
| `recentSongIds` (recientes) | export ✅ / import ❌ | ✅ ✅ |
| `playCounts` (estadísticas) | ✅ | ✅ |
| `blacklistedFolders` (carpetas ocultas) | ✅ | ✅ |
| `customPlaylists` | ✅ | ✅ |
| `navTabs` (orden de pestañas) | ✅ | ✅ |
| `totalListeningSeconds` | ✅ | ✅ |

**Archivos modificados**:
- `SonoraPreferences.kt`: `exportBackupJson()` (versión 1→2), `importBackupJson()` (restaura todos los campos)
- `SettingsScreen.kt`: launchers SAF `exportLauncher` / `importLauncher`, botones actualizados, dialog de portapapeles eliminado

#### 3. Indicador de Batería Reactivo

**Problema**: `isIgnoringBatteryOptimizations` era un `remember {}` estático calculado una sola vez al abrir Settings. Al aceptar "sin restricciones" en el sistema y volver, el estado no se actualizaba hasta reiniciar la pantalla.

**Solución**: Se convirtió a `mutableStateOf` + `DisposableEffect` con `LifecycleEventObserver`:
```kotlin
var isIgnoringBatteryOptimizations by remember { mutableStateOf(...) }
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(...)
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```
Ahora el indicador "Ilimitado" aparece automáticamente al volver desde los ajustes del sistema, sin necesidad de salir y volver a entrar a Settings.

### Versión
`versionCode 360` · `versionName "3.6.0"` · Commit: `main`

---

## 🚀 Fase 17 — Crossfade sin Corte en Song B + Notificación en Tiempo Real (v3.9.2)

### Problema Detectado
Después de la v3.5.1 (preloading de Song B), el corte se movió del inicio al final:
- **Corte en Song B**: al terminar el fade de 10s, `player.setMediaItems()` + `prepare()` trasladaba Song B al player principal, causando un corte de 50–200ms en Song B.
- **Notificación stuck en Song A**: durante todo el crossfade, el player principal seguía en Song A (Song B estaba en `crossfadePlayer`). El MediaSession (fuente de la notificación) reportaba Song A hasta que ocurría el handoff.

### Causa Raíz (Arquitectura Invertida)

```
v3.5.1 — INCORRECTO:
  crossfadePlayer → Song B (fading IN, vol 0→1)
  player          → Song A (fading OUT, vol 1→0)
  Al finalizar: player.setMediaItems(Song B) + prepare() → CUT en Song B
  Notificación: MediaSession = Song A durante 10s completos

v3.6.1 — CORRECTO:
  crossfadePlayer → Song A (tail, fading OUT, vol 1→0)  ← preloaded 20s antes
  player          → Song B (fading IN, vol 0→1)         ← seekTo() sin prepare()
  Al finalizar: crossfadePlayer.stop() únicamente → CERO corte
  Notificación: MediaSession = Song B desde el primer segundo del fade
```

### Solución Implementada

#### `preloadCurrentTailOnCrossfadePlayer(currentSong: Song)`
- Reemplaza a `preloadNextSong()`. Ahora preloads la **cola de Song A** en `crossfadePlayer`
- Se dispara en la ventana de preload (2× crossfade antes del final de la canción)
- `crossfadePlayer.setMediaItem(currentSong, 0L)` + `prepare()` silenciosamente
- En el trigger, hace `seekTo(currentPosition)` para sincronizarse exactamente con donde está Song A

#### `performCrossfadeTransition()` — Arquitectura corregida

1. **crossfadePlayer**: `seekTo(currentPos)` → `volume = 1.0f` → `play()` (Song A continúa aquí, ya buffered)
2. **player**: `seekTo(nextIndex, 0L)` → Song B instantáneo (ya en la cola del ExoPlayer, sin `setMediaItems`/`prepare`)
3. **Fade**: `crossfadePlayer` vol 1→0 (A), `player` vol 0→1 (B) — curva sinusoidal
4. **Fin**: `crossfadePlayer.stop()` únicamente — Song B ya lleva N segundos en `player`

#### `onMediaItemTransition` (listener del player)
El guard `if (!isCrossfading) { player.volume = 1.0f }` previene que el listener resetee el volumen de Song B a 1.0f cuando `seekTo(nextIndex)` dispara la transición de mediaItem durante el crossfade.

### Resultado
- ✅ Sin corte al inicio del crossfade (Song A preloaded)
- ✅ Sin corte al final del crossfade (Song B ya en player, sin handoff)
- ✅ Notificación refleja Song B desde el primer segundo del fade (MediaSession ligado a `player`)

### Versión
`versionCode 361` · `versionName "3.6.1"` · Commit: `main`

---

## 🚀 Fase 18 — Crossfade Simétrico de Doble Reproductor + Fotos Reales de Artistas (v3.9.2)

### 1. Crossfade Simétrico Sin Cortes (Symmetric Player Swapping)
- **Problema anterior:** Al intentar mover Canción A entre reproductores o llamar `seekTo(nextIndex)` en el reproductor principal, ExoPlayer cortaba el buffer de Canción A provocando un micro-corte.
- **Nueva Arquitectura:** Implementación de dos reproductores simétricos (`player1` y `player2`) con punteros dinámicos `activePlayer` y `standbyPlayer`.
  - **Canción A** continúa reproduciéndose en `activePlayer` mientras su volumen baja de `1.0` a `0.0`.
  - **Canción B** inicia desde `00:00` en `standbyPlayer` subiendo su volumen de `0.0` a `1.0`.
  - **Metadatos y Notificación:** Se actualizan a Canción B inmediatamente al inicio de la mezcla.
  - **Al terminar la mezcla:** `activePlayer` se detiene y los punteros se intercambian (`activePlayer` pasa a ser el reproductor que ya tiene a Canción B sonando al 100%).
  - **Resultado:** Cero cortes en Canción A, cero cortes en Canción B y cero cortes al final del crossfade.

### 2. Fotos Reales de Artistas (API Pública Deezer + iTunes)
- Se creó `ArtistImageRepository` que consulta de forma optimizada y gratuita las fotos oficiales de los artistas en alta resolución (`picture_big` / `600x600`).
- Sistema de caché en memoria (`ConcurrentHashMap`) para evitar llamadas redundantes.
- Fallback automático a la carátula del álbum en caso de no encontrar coincidencia o no tener conexión.
- Integrado tanto en la cuadrícula de la pestaña **Artistas** (`NativeHomeScreen`) como en la cabecera hero de **Detalle del Artista** (`ArtistDetailScreen`).

### 3. Ajustes de UI y Seek
- Píldora **Reproducir Mix** ajustada a `135dp` del fondo.
- Desvanecimiento rápido de 120ms en `seekTo` para eliminar chasquidos de decodificación al arrastrar la barra de progreso bruscamente.

### Versión
`versionCode 370` · `versionName "3.7.0"` · Commit: `main`

