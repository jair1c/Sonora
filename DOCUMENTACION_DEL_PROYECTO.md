# 🌸 Sonora Music Player (v2.1.0)
### *Reproductor de Música Audiófilo Minimalista y Local para Android*

---

## 📖 1. Visión y Concepto del Proyecto

**Sonora** es un reproductor de música local de alta fidelidad diseñado para brindar una experiencia auditiva pura, libre de distracciones, con una estética visual basada en **formas orgánicas, vinilo en flor de 8 pétalos simétricos, tipografía elegante y una paleta tonal dual (Warm Beige en Modo Claro / Deep OLED Dark en Modo Oscuro)**.

El reproductor opera 100% de manera local y privada en el dispositivo, integrando un motor DSP de audio profesional de 10 bandas, fundido cruzado (*crossfade*), lectura de etiquetas y carátulas embebidas en archivos `.flac`, `.mp3`, `.m4a`, sincronización de letras `.lrc`, y una barra de navegación y accesos totalmente personalizables por el usuario.

---

## 🛠️ 2. Stack Tecnológico

- **Frontend Core**: React 19 + TypeScript
- **Estilos y Diseño**: Tailwind CSS v4 + Lucide Icons + Outfit / Plus Jakarta Sans Fonts
- **Puente Móvil**: Capacitor 8 + Android Native Bridge
- **Motor de Audio & Notificaciones**:
  - `MediaSessionCompat` (Notificación multimedia nativa en la barra de estado y pantalla de bloqueo de Android)
  - `AudioTrack` / `MediaPlayer` con soporte de cambio de velocidad (0.8x a 2.0x) y mezcla de fundido cruzado.
  - Ecualizador paramétrico DSP de 10 bandas y refuerzo de graves.
- **Herramientas de Build**: Vite 8 + Gradle 8 + Android SDK 34 / 35.

---

## 📜 3. Historial Cronológico Completo de Cambios (Changelog de v0.1 a v2.1.0)

### 🔹 Fase 1: Arquitectura Inicial y Motor de Reproducción Local (v0.1 - v0.5)
- **Problema Inicial**: La aplicación dependía de un WebView genérico sin persistencia ni notificación multimedia en la barra de Android; al salir de la app o bloquear el teléfono, la música se pausaba y el sistema cerraba el proceso.
- **Solución**:
  - Se implementó un servicio nativo en Android con `MediaSessionCompat` y `NotificationManagerCompat` en primer plano (*Foreground Service*).
  - Se añadieron controles multimedia directos en la notificación (Reproducir, Pausar, Siguiente, Anterior) accesibles desde la barra de notificaciones y la pantalla de bloqueo.
  - Se integró el escaneo automático del almacenamiento del dispositivo (`MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`) para leer canciones, artistas, álbumes y carpetas reales.

---

### 🔹 Fase 2: Soporte Audiófilo & Corrección de Bugs de Audio (v0.6 - v1.0)
- **Problema de Doble Reproducción / Desfase**: Algunas pistas se escuchaban duplicadas con un ligero eco al cambiar rápidamente de canción.
  - *Causa*: Existían instancias concurrentes de audio no liberadas antes de inicializar la nueva fuente.
  - *Solución*: Implementación de un ciclo de vida estricto con `nativeAudio.stop()` y liberación de buffers antes de montar la nueva pista.
- **Problema de Pausa al Final de Pista**: Al terminar una canción, el reproductor no avanzaba automáticamente y se quedaba congelado en `00:00`.
  - *Solución*: Enrutamiento del evento `onCompletion` nativo de Android hacia el contexto de React para disparar `nextTrack()` de forma fluida.
- **Letras Sincronizadas (.LRC y Embebidas FLAC)**:
  - Soporte de archivos de letras externos `.lrc` y extracción de metadatos incrustados en archivos FLAC sin depender de internet.
- **Fundido Cruzado (Crossfade)**:
  - Sistema de mezcla de audio programable (1s a 12s) que realiza un desvanecimiento (*fade-out / fade-in*) entre la canción actual y la siguiente.

---

### 🔹 Fase 3: Rediseño Visual Orgánico - El Vinilo en Flor de 8 Pétalos (v1.1 - v1.5)
- **Geometría Floral Simétrica**:
  - Creación del componente `OrganicShapes.tsx` con fórmulas trigonométricas de pétalos simétricos exactos:
    $$r(\theta) = R_0 + A \cdot \cos(8\theta)$$
  - Implementación del `clipPath` con flor de 8 pétalos tanto para la carátula giratoria como para el contorno ondulado exterior.
- **Ajuste de Escala Dinámica (80% / 60%)**:
  - Al estar reproduciendo, el vinilo floral se expande al **80%** del contorno con animación suave de giro; al pausar, se contrae con gracia al **60%**.
- **Contorno Ondulado Interactivo con Scrubber Knob**:
  - El avance de la canción se dibuja sobre el contorno ondulado y un punto deslizable (*knob*) viaja exactamente a lo largo de las curvas de los 8 pétalos, permitiendo arrastrar para adelantar o retroceder la canción (*seek* táctil).

---

### 🔹 Fase 4: Personalización Total de Navegación y Herramientas (v1.6 - v1.9)
- **Barra de Navegación Dinámica**:
  - En la sección de **Ajustes**, el usuario puede activar, desactivar, reordenar (subir/bajar) y seleccionar qué pestañas mostrar en la barra inferior (Biblioteca, Canciones, Artistas, Álbumes, Listas, Carpetas, Reproductor, Ajustes).
- **Detección Inteligente de Cabecera**:
  - Si "Ajustes" o "Listas" están presentes en la barra inferior, sus botones correspondientes en la cabecera superior se ocultan de forma automática para evitar duplicidad visual; si se quitan de la barra inferior, reaparecen arriba al instante.
- **Modales con UI Propia (Zero Native Alerts)**:
  - Se eliminaron todos los `window.confirm` / `alert` nativos del sistema y se reemplazaron por diálogos diseñados bajo la estética orgánica de la app (diálogo de confirmación de eliminación con acentos rojos suaves).
- **Auto-centrado de Letras**:
  - En la vista de letras a pantalla completa, la línea activa se desplaza y se centra automáticamente de forma suave en la pantalla, con texto ajustable para evitar desbordamientos laterales.

---

### 🔹 Fase 5: Modo Oscuro OLED, Persistencia y Gestos (v2.0 - v2.1.0)
- **Pestaña Predeterminada en "Canciones"**:
  - La aplicación abre directamente en la pestaña de canciones locales (`Todas las Canciones`).
- **Sistema de Temas (Apariencia & Tema)**:
  - 🖥️ **Sistema**: Sigue automáticamente la configuración de Android.
  - ☀️ **Modo Claro**: Warm Paper / Luxury Beige (`#f5f2ea` / `#eae5da`).
  - 🌙 **Modo Oscuro**: Deep OLED Dark (`#0f0e0d` / `#1a1917` / `#2a2824`).
  - **Knob de Alta Visibilidad**: En modo oscuro, el punto deslizable pasa a blanco radiante con relieve para ser 100% visible y elegante contra el fondo negro.
- **Mini-Reproductor Flotante Persistente**:
  - Permanece visible incluso cuando la música está en pausa.
  - Al presionar una canción pausada en la lista, **se reanuda exactamente desde el segundo donde se pausó** en lugar de reiniciar desde cero.
- **Optimización de Fluidez a 60 FPS**:
  - La pantalla de biblioteca se mantiene en memoria sin destruirse/reconstruirse.
  - El reproductor funciona como una capa flotante con aceleración por hardware (`translate3d(0, 0, 0)`).
- **Gesto de Deslizar hacia Abajo (Swipe Down)**:
  - Deslizar el dedo hacia abajo en el reproductor completo comprime la pantalla suavemente y regresa a la vista previa.
- **Bloqueo de Scroll de Fondo en Modales**:
  - Al abrir el Ecualizador, Temporizador, SonoraStats, Editor ID3 o Diálogos, el fondo queda estático y no rebota ni se desplaza al arrastrar los dedos.

---

## 📁 4. Estructura del Código

```
c:\app\luxTune\
├── src\
│   ├── components\
│   │   ├── ArtistSelectScreen.tsx   # Biblioteca principal: Canciones, Artistas, Álbumes, Listas, Carpetas
│   │   ├── EqualizerModal.tsx       # Ecualizador DSP de 10 bandas + Perfiles Acústicos
│   │   ├── OnboardingScreen.tsx     # Pantalla de bienvenida y permiso de almacenamiento
│   │   ├── OrganicShapes.tsx        # Fórmulas SVG de 8 pétalos y contorno ondulado interactivo
│   │   ├── PlayerScreen.tsx         # Reproductor completo con vinilo 80%/60%, letras y gestos
│   │   ├── SleepTimerModal.tsx      # Temporizador de apagado con fade-out suave
│   │   ├── SongCover.tsx            # Renderizador de carátulas locales/remotas con clipPath
│   │   ├── StatsModal.tsx           # sonoraStats (tiempo de escucha y top canciones)
│   │   ├── TagEditorModal.tsx       # Editor de etiquetas ID3 (Título, Artista, Álbum, Año, Género)
│   │   └── ToolsScreen.tsx          # Ajustes: Tema, Barra de Navegación, Crossfade, Velocidad, Redondez
│   ├── context\
│   │   └── PlayerContext.tsx        # Estado global: Audio, Cola, Tema, Navegación, Persistencia
│   ├── services\
│   │   ├── nativeAudio.ts           # Interfaz con el reproductor nativo Android / Web Audio API
│   │   └── nativeMedia.ts           # Lector de archivos locales y extracción de metadatos
│   ├── App.tsx                      # Contenedor raíz con transiciones aceleradas y navegación
│   ├── index.css                    # Variables CSS de Modo Claro y Modo Oscuro
│   └── main.tsx                     # Punto de entrada de React
├── android\                         # Proyecto Android nativo (Capacitor Bridge, Gradle, Manifest)
├── capacitor.config.json            # Configuración de Sonora (App ID, nombre, permisos)
└── package.json                     # Versión 2.1.0 y dependencias
```

---

## 🚀 5. Guía de Ejecución y Compilación

### Requisitos Previos:
- Node.js (v18 o superior)
- Android Studio / Android SDK (plataforma 34 o 35)
- Dispositivo Android conectado con Depuración USB habilitada

### Comandos de Compilación & Firma:
```powershell
# 1. Iniciar servidor de desarrollo web
npm run dev

# 2. Compilar assets y sincronizar con Android
npm run build
npx cap sync android

# 3. Compilar APK Release Firmada (Producción)
cd android
.\gradlew.bat assembleRelease

# 4. Instalar APK Release en el dispositivo conectado
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

## 💾 7. Claves de Almacenamiento Local (`localStorage`)

| Clave | Descripción |
|---|---|
| `sonora_theme_mode` | Modo de tema seleccionado: `'system'`, `'light'`, `'dark'` |
| `sonora_petal_roundness` | Radio de redondez de los 8 pétalos (0 a 100) |
| `sonora_crossfade_seconds` | Segundos de fundido cruzado (0 a 12s) |
| `sonora_nav_tabs` | Configuración y orden de pestañas de la barra de navegación |
| `sonora_stats` | Minutos reproducidos, canciones escuchadas y artista más escuchado |
| `sonora_custom_playlists` | Listas de reproducción locales creadas por el usuario |
| `sonora_eq_settings` | Ajustes personalizados del ecualizador de 10 bandas |

