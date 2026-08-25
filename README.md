# 🌸 Sonora (v3.0.0)
> **Reproductor de Música Nativo Audiófilo con Diseño Orgánico y Arquitectura Jetpack Compose para Android**

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-3.0.0-emerald.svg)
![Platform](https://img.shields.io/badge/platform-Android_10+-brightgreen.svg)
![Stack](https://img.shields.io/badge/stack-Kotlin_+_Jetpack_Compose_+_Media3-blueviolet.svg)

---

## ✨ Características Principales

- 🌸 **Reproductor Floral de 8 Pétalos Simétricos**: Carátula de vinilo con recorte Bezier orgánico que gira fluidamente, escala de forma dinámica y mantiene un margen áureo del 20% respecto al anillo de onda interactivo (*Wavy Scrubber*).
- 🎛️ **5 Estilos de Controles de Reproducción de Lujo**:
  - *Cápsula Flotante (Luxury Dock)*
  - *Círculos Suizos (Swiss Circles)*
  - *Flor Orgánica (Organic Flower)*
  - *Hi-Fi Squircle*
  - *Píldora Waveform*
- 🎚️ **Ecualizador Nativo DSP de 10 Bandas**: Refuerzo de graves (*Bass Boost*), virtualizador y perfiles acústicos personalizados en modal adaptativo.
- 🔀 **Cola de Reproducción & Modo Aleatorio Dinámico**: Barajado no determinista en tiempo real que nunca repite secuencias previas y restauración instantánea al orden original de la biblioteca al desactivar aleatorio.
- 🌙 **Sistema de Temas Cohesivo**: Modo Claro (*Warm Luxury Linen*), Modo Oscuro (*Deep Velvet OLED*) y Modo Sistema en todas las pantallas, modales y hojas inferiores.
- ⚡ **Reproducción FLAC & Hi-Fi de Ultra Compatibilidad**: Motor AndroidX Media3 ExoPlayer con extractores optimizados y conmutación automática a lectura directa de archivo local ante cabeceras complejas.
- 📜 **Letras Sincronizadas (.LRC y Metadatos Embebidos)**: Extracción instantánea y seguimiento visual centrado en tiempo real.
- 📊 **sonoraStats 2.0**: Panel 2x2 integrado y modal interactivo con estadísticas reales de tiempo de escucha (minutos/horas) y Top 5 de canciones más reproducidas.
- 🎛️ **Barra de Navegación 100% Personalizable**: Modos de etiquetas (Siempre, Solo Iconos, Dinámica en pestaña activa) y reordenamiento de pestañas con insignias numeradas de alto contraste.
- 🔒 **100% Privado y Offline**: Sin recopilación de datos, sin anuncios y sin dependencias de servicios en la nube.

---

## 🛠️ Compilación e Instalación

```powershell
# 1. Compilar APK Release Optimizada (ProGuard / R8)
cd android
.\gradlew.bat assembleRelease

# 2. Instalar directamente en el dispositivo Android vía ADB
adb install -r app\build\outputs\apk\release\app-release.apk
```

---

## 📖 Documentación Completa

Para revisar el historial cronológico de todas las versiones, decisiones de arquitectura, diagramas de flujo y detalles de cada subsistema, consulta [DOCUMENTACION_DEL_PROYECTO.md](./DOCUMENTACION_DEL_PROYECTO.md).
