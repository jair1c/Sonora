# 🌸 Sonora (v2.1.0)
> **Reproductor de Música Audiófilo y Local con Diseño Orgánico para Android**

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Version](https://img.shields.io/badge/version-2.1.0-emerald.svg)
![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)
![Framework](https://img.shields.io/badge/stack-React_19_+_Capacitor_8-black.svg)

---

## ✨ Características Principales

- 🌸 **Reproductor Floral de 8 Pétalos Simétricos**: Carátula de vinilo giratoria que se adapta dinámicamente (80% al reproducir, 60% al pausar) y contorno interactivo para adelantar/retroceder.
- 🎚️ **Ecualizador DSP de 10 Bandas**: Refuerzo de graves (*Bass Boost*), virtualizador 3D y perfiles acústicos personalizados.
- 🌙 **Sistema de Temas Completo**: Modo Claro (*Warm Paper Beige*), Modo Oscuro (*Deep OLED Dark*) y Modo Automático según el sistema.
- 🔀 **Fundido Cruzado (Crossfade)**: Transición acústica sin silencios entre pistas (1s a 12s).
- 🎛️ **Navegación 100% Personalizable**: Ordena, oculta o muestra tus secciones favoritas en la barra inferior.
- 🏷️ **Editor de Etiquetas ID3**: Modifica título, artista, álbum, año y género de tus canciones locales.
- 📜 **Letras Sincronizadas (.LRC y Embebidas FLAC)**: Centrado automático de la línea en reproducción.
- 📊 **sonoraStats**: Estadísticas de tiempo de escucha local y tus canciones más escuchadas.
- 🔒 **100% Privado y Offline**: Sin recopilación de datos, sin anuncios y sin conexión obligatoria.

---

## 🛠️ Instalación y Compilación

```bash
# 1. Instalar dependencias
npm install

# 2. Servidor de desarrollo
npm run dev

# 3. Compilar para Android
npm run build
npx cap sync android
cd android && ./gradlew assembleDebug
```

---

## 📖 Documentación Completa

Para revisar el historial de cambios de todas las versiones, decisiones de arquitectura y detalles de cada componente, consulta [DOCUMENTACION_DEL_PROYECTO.md](./DOCUMENTACION_DEL_PROYECTO.md).
