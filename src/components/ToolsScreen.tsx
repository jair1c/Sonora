import React, { useState } from 'react';
import {
  Sliders,
  Moon,
  BarChart3,
  RefreshCw,
  Volume2,
  Trash2,
  ShieldCheck,
  LayoutGrid,
  ArrowUp,
  ArrowDown,
  RotateCcw,
  Check,
  Disc3,
  Sun,
  Smartphone,
  Download,
  Upload
} from 'lucide-react';

import { usePlayer, AVAILABLE_NAV_OPTIONS } from '../context/PlayerContext';
import { EqualizerModal } from './EqualizerModal';
import { SleepTimerModal } from './SleepTimerModal';
import { StatsModal } from './StatsModal';

const SPEEDS = [0.8, 1.0, 1.25, 1.5, 2.0];

export const ToolsScreen: React.FC = () => {
  const {
    playbackSpeed,
    setPlaybackSpeed,
    crossfadeSeconds,
    setCrossfadeSeconds,
    scanLocalMusic,
    isScanning,
    tracks,
    stats,
    sleepTimer,
    navTabsConfig,
    setNavTabsConfig,
    resetNavTabsConfig,
    petalRoundness,
    setPetalRoundness,
    themeMode,
    setThemeMode,
    setActiveScreen,
    exportBackupData,
    importBackupData
  } = usePlayer();


  const [showEq, setShowEq] = useState(false);
  const [showSleep, setShowSleep] = useState(false);
  const [showStats, setShowStats] = useState(false);
  const [cacheCleared, setCacheCleared] = useState(false);

  const handleClearCache = () => {
    Object.keys(localStorage).forEach((key) => {
      if (key.startsWith('lux_art_img_') || key.startsWith('lux_alb_img_')) {
        localStorage.removeItem(key);
      }
    });
    setCacheCleared(true);
    setTimeout(() => setCacheCleared(false), 3000);
  };

  // Move tab up/down in nav bar
  const moveTab = (index: number, direction: 'up' | 'down') => {
    const newTabs = [...navTabsConfig];
    const targetIdx = direction === 'up' ? index - 1 : index + 1;
    if (targetIdx < 0 || targetIdx >= newTabs.length) return;
    const temp = newTabs[index];
    newTabs[index] = newTabs[targetIdx];
    newTabs[targetIdx] = temp;
    setNavTabsConfig(newTabs);
  };

  const toggleTabEnabled = (tabId: string) => {
    const exists = navTabsConfig.some((t) => t.id === tabId);
    if (exists) {
      if (navTabsConfig.length <= 2) {
        alert('Debes mantener al menos 2 pestañas activas en la barra.');
        return;
      }
      setNavTabsConfig(navTabsConfig.filter((t) => t.id !== tabId));
    } else {
      const template = AVAILABLE_NAV_OPTIONS.find((t) => t.id === tabId);
      if (template) {
        if (navTabsConfig.length >= 5) {
          alert('El número máximo recomendado de pestañas es 5 para una óptima visualización.');
          return;
        }
        setNavTabsConfig([...navTabsConfig, { ...template, enabled: true }]);
      }
    }
  };

  return (
    <div className="flex flex-col h-full bg-[#f5f2ea] dark:bg-[#0f0e0d] text-[#121212] dark:text-[#f5f2ea] overflow-y-auto px-5 pt-8 pb-32 animate-fade-in no-scrollbar transition-colors duration-300">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <button
            onClick={() => setActiveScreen('artists')}
            className="w-10 h-10 rounded-full border border-[#ded8cd] dark:border-[#2a2824] bg-[#eae5da] dark:bg-[#1a1917] flex items-center justify-center hover:bg-black hover:text-white dark:hover:bg-white dark:hover:text-black transition-all cursor-pointer shadow-sm active:scale-95 text-black dark:text-white"
            title="Volver"
          >
            <ArrowUp className="w-5 h-5 -rotate-90" />
          </button>
          <div>
            <span className="text-[11px] font-bold uppercase tracking-widest text-[#75726b] dark:text-[#8a857b] font-outfit">
              CENTRO DE CONTROL
            </span>
            <h1 className="text-2xl font-black text-black dark:text-white font-outfit tracking-tight">
              Ajustes & Herramientas
            </h1>
          </div>
        </div>
        <span className="px-2.5 py-1 bg-[#eae5da] dark:bg-[#1a1917] border border-[#ded8cd] dark:border-[#2a2824] text-black dark:text-[#f5f2ea] text-[11px] font-mono font-bold rounded-full">
          Sonora v2.1.0 • Offline
        </span>

      </div>

      <div className="flex flex-col gap-6">
        {/* Quick Access Utility Grid */}
        <div className="grid grid-cols-2 gap-3">
          {/* Equalizer Tile */}
          <div
            onClick={() => setShowEq(true)}
            className="p-4 bg-[#eae5da] dark:bg-[#1a1917] rounded-2xl flex flex-col justify-between h-32 cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#23211e] active:scale-98 transition-all border border-[#ded8cd] dark:border-[#2a2824]"
          >
            <div className="flex items-center justify-between">
              <div className="w-8 h-8 rounded-full bg-black dark:bg-white text-[#f5f2ea] dark:text-black flex items-center justify-center">
                <Sliders size={15} />
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#ded8cd] dark:bg-[#2a2824] text-black dark:text-white">
                10 Bandas
              </span>
            </div>
            <div>
              <span className="text-xs font-bold text-black dark:text-white block">Ecualizador Gráfico</span>
              <span className="text-[10px] text-[#75726b] dark:text-[#8a857b]">Graves, perfiles acústicos</span>
            </div>
          </div>

          {/* Sleep Timer Tile */}
          <div
            onClick={() => setShowSleep(true)}
            className={`p-4 rounded-2xl flex flex-col justify-between h-32 cursor-pointer active:scale-98 transition-all border ${
              sleepTimer.active
                ? 'bg-black dark:bg-white text-white dark:text-black border-black dark:border-white'
                : 'bg-[#eae5da] dark:bg-[#1a1917] text-black dark:text-white hover:bg-[#ded8cd] dark:hover:bg-[#23211e] border-[#ded8cd] dark:border-[#2a2824]'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center ${sleepTimer.active ? 'bg-white dark:bg-black text-black dark:text-white' : 'bg-black dark:bg-white text-white dark:text-black'}`}>
                <Moon size={15} />
              </div>
              {sleepTimer.active && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-white/20 dark:bg-black/20 text-white dark:text-black font-mono">
                  {Math.ceil(sleepTimer.remainingSeconds / 60)} min
                </span>
              )}
            </div>
            <div>
              <span className="text-xs font-bold block">Temporizador de Apagado</span>
              <span className={`text-[10px] ${sleepTimer.active ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>
                {sleepTimer.active ? 'Activo con fade-out' : 'Pausa al dormir'}
              </span>
            </div>
          </div>

          {/* Stats Tile */}
          <div
            onClick={() => setShowStats(true)}
            className="p-4 bg-[#eae5da] dark:bg-[#1a1917] rounded-2xl flex flex-col justify-between h-32 cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#23211e] active:scale-98 transition-all border border-[#ded8cd] dark:border-[#2a2824]"
          >
            <div className="flex items-center justify-between">
              <div className="w-8 h-8 rounded-full bg-black dark:bg-white text-[#f5f2ea] dark:text-black flex items-center justify-center">
                <BarChart3 size={15} />
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-[#ded8cd] dark:bg-[#2a2824] text-black dark:text-white">
                {stats.totalTracksPlayed} canciones
              </span>
            </div>
            <div>
              <span className="text-xs font-bold text-black dark:text-white block">sonoraStats</span>
              <span className="text-[10px] text-[#75726b] dark:text-[#8a857b]">{stats.totalPlayTimeMinutes} min escuchados</span>
            </div>
          </div>

          {/* Rescan Tile */}
          <div
            onClick={() => !isScanning && scanLocalMusic()}
            className={`p-4 rounded-2xl flex flex-col justify-between h-32 cursor-pointer active:scale-98 transition-all border border-[#ded8cd] dark:border-[#2a2824] ${
              isScanning ? 'bg-black dark:bg-white text-white dark:text-black' : 'bg-[#eae5da] dark:bg-[#1a1917] text-black dark:text-white hover:bg-[#ded8cd] dark:hover:bg-[#23211e]'
            }`}
          >
            <div className="flex items-center justify-between">
              <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isScanning ? 'bg-white dark:bg-black text-black dark:text-white' : 'bg-black dark:bg-white text-white dark:text-black'}`}>
                <RefreshCw size={15} className={isScanning ? 'animate-spin' : ''} />
              </div>
              <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${isScanning ? 'bg-white/20 dark:bg-black/20 text-white dark:text-black' : 'bg-[#ded8cd] dark:bg-[#2a2824] text-black dark:text-white'}`}>
                {tracks.length} Pistas
              </span>
            </div>
            <div>
              <span className="text-xs font-bold block">{isScanning ? 'Escaneando...' : 'Re-escanear'}</span>
              <span className={`text-[10px] ${isScanning ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>
                {isScanning ? 'Buscando archivos...' : 'Actualizar biblioteca'}
              </span>
            </div>
          </div>
        </div>

        {/* Appearance & Theme Mode Section */}
        <div className="flex flex-col gap-3 bg-[#eae5da] dark:bg-[#1a1917] p-4 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] flex items-center gap-1.5 font-outfit">
              <Sun size={14} /> Apariencia & Tema
            </span>
            <span className="text-[10px] font-bold text-black dark:text-white bg-[#ded8cd] dark:bg-[#2a2824] px-2.5 py-0.5 rounded-full capitalize font-mono">
              {themeMode === 'system' ? 'Automático' : themeMode === 'dark' ? 'Oscuro' : 'Claro'}
            </span>
          </div>

          <p className="text-[11px] text-[#75726b] dark:text-[#8a857b]">
            Elige el estilo visual de la aplicación según tus preferencias o el tema del dispositivo.
          </p>

          <div className="grid grid-cols-3 gap-2 pt-1">
            <button
              onClick={() => setThemeMode('system')}
              className={`p-3 rounded-2xl flex flex-col items-center gap-1.5 text-xs font-bold transition-all cursor-pointer border ${
                themeMode === 'system'
                  ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-md'
                  : 'bg-[#ded8cd]/60 dark:bg-[#252320] text-[#121212] dark:text-[#dedad2] border-[#ded8cd] dark:border-[#33302b] hover:bg-[#ded8cd]'
              }`}
            >
              <Smartphone size={18} />
              <span className="text-[11px]">Sistema</span>
            </button>

            <button
              onClick={() => setThemeMode('light')}
              className={`p-3 rounded-2xl flex flex-col items-center gap-1.5 text-xs font-bold transition-all cursor-pointer border ${
                themeMode === 'light'
                  ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-md'
                  : 'bg-[#ded8cd]/60 dark:bg-[#252320] text-[#121212] dark:text-[#dedad2] border-[#ded8cd] dark:border-[#33302b] hover:bg-[#ded8cd]'
              }`}
            >
              <Sun size={18} />
              <span className="text-[11px]">Claro</span>
            </button>

            <button
              onClick={() => setThemeMode('dark')}
              className={`p-3 rounded-2xl flex flex-col items-center gap-1.5 text-xs font-bold transition-all cursor-pointer border ${
                themeMode === 'dark'
                  ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-md'
                  : 'bg-[#ded8cd]/60 dark:bg-[#252320] text-[#121212] dark:text-[#dedad2] border-[#ded8cd] dark:border-[#33302b] hover:bg-[#ded8cd]'
              }`}
            >
              <Moon size={18} />
              <span className="text-[11px]">Oscuro</span>
            </button>
          </div>
        </div>

        {/* Petal Roundness / Curvature Setting */}
        <div className="flex flex-col gap-3 bg-[#eae5da] dark:bg-[#1a1917] p-4 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] flex items-center gap-1.5 font-outfit">
              <Disc3 size={14} /> Redondez de los Pétalos
            </span>
            <span className="text-xs font-mono font-bold text-black dark:text-white bg-[#ded8cd] dark:bg-[#2a2824] px-2.5 py-0.5 rounded-full">
              {petalRoundness}%
            </span>
          </div>

          <p className="text-[11px] text-[#75726b] dark:text-[#8a857b]">
            Modifica la curvatura y profundidad de los 8 pétalos de la carátula y el contorno del reproductor.
          </p>

          <input
            type="range"
            min="0"
            max="100"
            step="5"
            value={petalRoundness}
            onChange={(e) => setPetalRoundness(parseInt(e.target.value, 10))}
            className="w-full accent-black dark:accent-white cursor-pointer"
          />

          <div className="flex justify-between text-[10px] text-[#75726b] dark:text-[#8a857b]">
            <span>0% (Círculo)</span>
            <span>50% (Suave)</span>
            <span>100% (Pétalos marcados)</span>
          </div>
        </div>

        {/* Custom Bottom Navigation Bar Manager */}
        <div className="flex flex-col gap-3 bg-[#eae5da] dark:bg-[#1a1917] p-4 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] flex items-center gap-1.5 font-outfit">
              <LayoutGrid size={14} /> Personalizar Barra Inferior
            </span>
            <button
              onClick={resetNavTabsConfig}
              className="text-[11px] font-bold text-[#75726b] dark:text-[#8a857b] hover:text-black dark:hover:text-white flex items-center gap-1 cursor-pointer"
              title="Restablecer pestañas por defecto"
            >
              <RotateCcw size={12} /> Restablecer
            </button>
          </div>

          <p className="text-[11px] text-[#75726b] dark:text-[#8a857b]">
            Selecciona qué accesos directos mostrar en la barra inferior y reorganiza su orden.
          </p>

          {/* Available Options Picker */}
          <div className="flex flex-wrap gap-1.5 pt-1">
            {AVAILABLE_NAV_OPTIONS.map((opt) => {
              const isPinned = navTabsConfig.some((t) => t.id === opt.id);
              return (
                <button
                  key={opt.id}
                  onClick={() => toggleTabEnabled(opt.id)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer ${
                    isPinned
                      ? 'bg-black dark:bg-white text-white dark:text-black shadow-sm'
                      : 'bg-[#ded8cd] dark:bg-[#2a2824] text-[#75726b] dark:text-[#8a857b] hover:text-black dark:hover:text-white'
                  }`}
                >
                  {isPinned ? <Check size={12} strokeWidth={3} /> : '+'}
                  {opt.label}
                </button>
              );
            })}
          </div>

          {/* Active Navigation Tabs Ordering List */}
          <div className="flex flex-col gap-1.5 mt-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b]">
              Pestañas activas (Arrastrar / Reordenar):
            </span>

            {navTabsConfig.map((tab, idx) => (
              <div
                key={tab.id}
                className="flex items-center justify-between p-2.5 bg-[#f5f2ea] dark:bg-[#141312] rounded-xl border border-[#ded8cd] dark:border-[#2a2824]"
              >
                <div className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded-full bg-black dark:bg-white text-white dark:text-black text-[10px] font-bold flex items-center justify-center">
                    {idx + 1}
                  </span>
                  <span className="text-xs font-bold text-black dark:text-white">{tab.label}</span>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    disabled={idx === 0}
                    onClick={() => moveTab(idx, 'up')}
                    className="p-1 rounded-lg text-black dark:text-white hover:bg-[#ded8cd] dark:hover:bg-[#2a2824] disabled:opacity-30 disabled:hover:bg-transparent cursor-pointer"
                    title="Mover a la izquierda"
                  >
                    <ArrowUp size={14} />
                  </button>
                  <button
                    disabled={idx === navTabsConfig.length - 1}
                    onClick={() => moveTab(idx, 'down')}
                    className="p-1 rounded-lg text-black dark:text-white hover:bg-[#ded8cd] dark:hover:bg-[#2a2824] disabled:opacity-30 disabled:hover:bg-transparent cursor-pointer"
                    title="Mover a la derecha"
                  >
                    <ArrowDown size={14} />
                  </button>
                  <button
                    onClick={() => toggleTabEnabled(tab.id)}
                    className="p-1 rounded-lg text-red-500 hover:bg-red-50 dark:hover:bg-red-950/30 cursor-pointer ml-1"
                    title="Quitar pestaña"
                  >
                    ✕
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Audio Engine Configuration Card */}
        <div className="flex flex-col gap-4 bg-[#eae5da] dark:bg-[#1a1917] p-4 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] transition-colors">
          <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] flex items-center gap-1.5 font-outfit">
            <Volume2 size={14} /> Motor de Audio & Mezcla
          </span>

          {/* Crossfade Selector */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-black dark:text-white">Fundido Cruzado (Crossfade)</span>
              <span className="text-xs font-mono font-bold text-black dark:text-white bg-[#ded8cd] dark:bg-[#2a2824] px-2 py-0.5 rounded-full">
                {crossfadeSeconds === 0 ? 'Desactivado' : `${crossfadeSeconds}s`}
              </span>
            </div>
            <p className="text-[10px] text-[#75726b] dark:text-[#8a857b]">
              La siguiente canción comenzará a sonar gradualmente antes de terminar la actual.
            </p>
            <div className="flex gap-1.5 pt-1">
              {[0, 2, 4, 6, 8, 10].map((sec) => (
                <button
                  key={sec}
                  onClick={() => setCrossfadeSeconds(sec)}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-xl transition-all cursor-pointer border ${
                    crossfadeSeconds === sec
                      ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-sm'
                      : 'bg-[#ded8cd]/60 dark:bg-[#252320] text-[#121212] dark:text-[#dedad2] border-[#ded8cd] dark:border-[#33302b] hover:bg-[#ded8cd]'
                  }`}
                >
                  {sec === 0 ? 'Off' : `${sec}s`}
                </button>
              ))}
            </div>
          </div>

          <div className="h-px bg-[#ded8cd] dark:bg-[#2a2824]" />

          {/* Playback Speed Selector */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-black dark:text-white">Velocidad de Reproducción</span>
              <span className="text-xs font-mono font-bold text-black dark:text-white bg-[#ded8cd] dark:bg-[#2a2824] px-2 py-0.5 rounded-full">
                {playbackSpeed}x
              </span>
            </div>
            <div className="flex gap-1.5 pt-1">
              {SPEEDS.map((spd) => (
                <button
                  key={spd}
                  onClick={() => setPlaybackSpeed(spd)}
                  className={`flex-1 py-1.5 text-xs font-bold rounded-xl transition-all cursor-pointer border ${
                    playbackSpeed === spd
                      ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-sm'
                      : 'bg-[#ded8cd]/60 dark:bg-[#252320] text-[#121212] dark:text-[#dedad2] border-[#ded8cd] dark:border-[#33302b] hover:bg-[#ded8cd]'
                  }`}
                >
                  {spd}x
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Backup & Data Management */}
        <div className="flex flex-col gap-3 bg-[#eae5da] dark:bg-[#1a1917] p-4 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] transition-colors">
          <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] flex items-center gap-1.5 font-outfit">
            <Download size={14} /> Copia de Seguridad & Datos
          </span>

          <p className="text-xs text-[#75726b] dark:text-[#8a857b]">
            Exporta tus listas creadas, favoritos y estadísticas a un archivo .json para restaurarlos en cualquier momento.
          </p>

          <div className="grid grid-cols-2 gap-2">
            <button
              onClick={exportBackupData}
              className="py-2.5 px-3 bg-black dark:bg-white text-white dark:text-black rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-sm active:scale-95 transition-all"
            >
              <Download size={13} /> Exportar
            </button>
            <label className="py-2.5 px-3 bg-[#ded8cd] dark:bg-[#2a2824] text-black dark:text-white rounded-xl font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow-sm active:scale-95 transition-all text-center">
              <Upload size={13} /> Restaurar
              <input
                type="file"
                accept=".json"
                className="hidden"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) {
                    const reader = new FileReader();
                    reader.onload = (event) => {
                      const content = event.target?.result as string;
                      if (content) {
                        const success = importBackupData(content);
                        if (success) alert('¡Copia de seguridad restaurada con éxito!');
                        else alert('Error: El archivo no tiene un formato válido.');
                      }
                    };
                    reader.readAsText(file);
                  }
                }}
              />
            </label>
          </div>
        </div>

        {/* Storage & Cache Management */}
        <div className="flex flex-col gap-3 bg-[#eae5da] dark:bg-[#1a1917] p-4 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] transition-colors">
          <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] flex items-center gap-1.5 font-outfit">
            <Trash2 size={14} /> Almacenamiento & Caché
          </span>

          <p className="text-xs text-[#75726b] dark:text-[#8a857b]">
            Libera memoria borrando carátulas e imágenes en caché de artistas locales.
          </p>

          <button
            onClick={handleClearCache}
            className={`w-full py-2.5 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all cursor-pointer ${
              cacheCleared
                ? 'bg-emerald-600 text-white'
                : 'bg-black dark:bg-white text-white dark:text-black hover:bg-neutral-800 active:scale-98 shadow-sm'
            }`}
          >
            {cacheCleared ? <Check size={14} /> : <Trash2 size={14} />}
            {cacheCleared ? 'Caché de Imágenes Limpiada' : 'Limpiar Caché de Carátulas'}
          </button>
        </div>

        {/* Privacy & Engine Badge */}
        <div className="p-4 bg-[#eae5da]/60 dark:bg-[#1a1917]/60 rounded-3xl border border-[#ded8cd] dark:border-[#2a2824] flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-black dark:bg-white text-white dark:text-black flex items-center justify-center shrink-0">
            <ShieldCheck size={20} />
          </div>
          <div>
            <span className="text-xs font-bold text-black dark:text-white block font-outfit">
              100% Privado y Sin Rastreo
            </span>
            <span className="text-[11px] text-[#75726b] dark:text-[#8a857b] leading-tight block">
              Toda la música y metadatos se procesan localmente en tu dispositivo.
            </span>
          </div>
        </div>


        {/* Sonora Version Info */}
        <div className="flex flex-col items-center justify-center py-2 pb-6 gap-1">
          <img src="/sonora_logo.svg" alt="Sonora" className="w-10 h-10 rounded-xl shadow-md mb-1" />
          <span className="text-xs font-extrabold font-outfit uppercase tracking-widest text-black dark:text-white">
            Sonora
          </span>
          <span className="text-[11px] font-mono text-[#75726b] dark:text-[#8a857b] block">
            Versión 2.1.0 • Audio Engine Hi-Fi
          </span>
        </div>
      </div>



      {/* Modals */}
      <EqualizerModal isOpen={showEq} onClose={() => setShowEq(false)} />
      <SleepTimerModal isOpen={showSleep} onClose={() => setShowSleep(false)} />
      <StatsModal isOpen={showStats} onClose={() => setShowStats(false)} />
    </div>
  );
};
