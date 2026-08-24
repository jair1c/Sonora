import React, { useState, useEffect, useRef, useMemo } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { PlayerScallopedRing } from './OrganicShapes';
import {
  Heart,
  Repeat,
  Repeat1,
  Shuffle,
  Play,
  Pause,
  Maximize2,
  Minimize2,
  ListMusic,
  X,
  ChevronDown
} from 'lucide-react';

export const PlayerScreen: React.FC = () => {
  const {
    currentTrack,
    isPlaying,
    currentTime,
    duration,
    progressPercent,
    togglePlay,
    nextTrack,
    prevTrack,
    seek,
    toggleRepeat,
    repeatMode,
    toggleShuffle,
    isShuffle,
    isLiked,
    toggleLike,
    setActiveScreen,
    previousScreen,
    tracks,
    playTrack,
    audioQualityBadge
  } = usePlayer();

  const [expandedLyrics, setExpandedLyrics] = useState(false);
  const [showQueue, setShowQueue] = useState(false);
  const activeLyricRef = useRef<HTMLParagraphElement | null>(null);
  const activeQueueItemRef = useRef<HTMLDivElement | null>(null);

  // Swipe-down to dismiss player gesture
  const touchStartY = useRef(0);
  const touchCurrentY = useRef(0);
  const [dragOffset, setDragOffset] = useState(0);
  const [isDragging, setIsDragging] = useState(false);

  // Time formatter mm:ss
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const handleBack = () => {
    setActiveScreen(previousScreen || 'artists');
  };

  const handleTouchStart = (e: React.TouchEvent) => {
    if (expandedLyrics || showQueue) return;
    touchStartY.current = e.touches[0].clientY;
    touchCurrentY.current = e.touches[0].clientY;
    setIsDragging(true);
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (expandedLyrics || showQueue || !isDragging) return;
    touchCurrentY.current = e.touches[0].clientY;
    const delta = touchCurrentY.current - touchStartY.current;
    if (delta > 0) {
      setDragOffset(delta);
    } else {
      setDragOffset(0);
    }
  };

  const handleTouchEnd = () => {
    if (expandedLyrics || showQueue || !isDragging) return;
    const delta = touchCurrentY.current - touchStartY.current;
    setIsDragging(false);
    if (delta > 70) {
      setActiveScreen(previousScreen || 'artists');
    }
    setDragOffset(0);
  };

  // Lock background scroll when sub-modals inside player are open
  useEffect(() => {
    if (expandedLyrics || showQueue) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [expandedLyrics, showQueue]);

  // Find active lyric line based on currentTime
  const lyricsList = currentTrack.lyrics || [];
  const activeLyricIndex = lyricsList.findIndex((lyric, idx) => {
    const nextLyric = lyricsList[idx + 1];
    if (!nextLyric) return currentTime >= lyric.time;
    return currentTime >= lyric.time && currentTime < nextLyric.time;
  });

  // Dynamic sliding window of lyrics for collapsed view (Shows active line + next 3 lines)
  const visibleLyricsPreview = useMemo(() => {
    if (lyricsList.length === 0) return [];
    const currentIdx = activeLyricIndex >= 0 ? activeLyricIndex : 0;
    const start = Math.max(0, currentIdx);
    const end = Math.min(lyricsList.length, currentIdx + 4);
    return lyricsList.slice(start, end);
  }, [lyricsList, activeLyricIndex]);

  // Auto-center currently playing lyric line in fullscreen mode
  useEffect(() => {
    if (expandedLyrics && activeLyricRef.current) {
      activeLyricRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'center'
      });
    }
  }, [activeLyricIndex, expandedLyrics]);

  // Auto-center current song in Queue modal
  useEffect(() => {
    if (showQueue && activeQueueItemRef.current) {
      const timer = setTimeout(() => {
        activeQueueItemRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 120);
      return () => clearTimeout(timer);
    }
  }, [showQueue, currentTrack?.id]);

  return (
    <div
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      style={{
        transform: dragOffset > 0 ? `translate3d(0, ${dragOffset}px, 0)` : undefined,
        transition: isDragging ? 'none' : 'transform 0.25s cubic-bezier(0.32, 0.72, 0, 1)',
        willChange: 'transform'
      }}
      className="fixed inset-0 w-full h-full max-h-screen bg-[#f5f2ea] dark:bg-[#0f0e0d] flex flex-col justify-between select-none text-[#121212] dark:text-[#f5f2ea] overflow-hidden pt-8 pb-5 px-5 font-sans touch-none overscroll-none transition-colors duration-300"
    >
      {/* 1. Header Navigation Bar */}
      <div className="flex items-center justify-between z-10 shrink-0">
        {/* Circular Back button / Compress chevron */}
        <button
          onClick={handleBack}
          className="w-11 h-11 rounded-full border border-[#ded8cd] dark:border-[#2a2824] bg-[#eae5da]/80 dark:bg-[#1a1917]/80 flex items-center justify-center text-[#121212] dark:text-white hover:bg-black hover:text-white transition-all active:scale-95 cursor-pointer shadow-sm pointer-events-auto"
          title="Comprimir reproductor"
        >
          <ChevronDown className="w-6 h-6" />
        </button>

        {/* Screen title: Reproduciendo */}
        <div className="flex flex-col items-center">
          <span className="text-sm sm:text-base font-black tracking-wider text-[#121212] dark:text-white font-outfit uppercase">
            Reproduciendo
          </span>
        </div>

        {/* Action icons (Queue List Button & Favorite Heart Button) */}
        <div className="flex items-center gap-2.5 pointer-events-auto">
          <button
            onClick={() => setShowQueue(true)}
            className="w-11 h-11 rounded-full border border-[#ded8cd] dark:border-[#2a2824] bg-[#eae5da]/80 dark:bg-[#1a1917]/80 flex items-center justify-center text-[#121212] dark:text-white hover:bg-black hover:text-white transition-all active:scale-95 cursor-pointer shadow-sm"
            title="Cola de reproducción"
          >
            <ListMusic className="w-5 h-5" />
          </button>
          <button
            onClick={() => toggleLike(currentTrack.id)}
            className="w-11 h-11 rounded-full border border-[#ded8cd] dark:border-[#2a2824] bg-[#eae5da]/80 dark:bg-[#1a1917]/80 flex items-center justify-center text-[#121212] dark:text-white hover:scale-105 active:scale-95 transition-all cursor-pointer shadow-sm"
            title="Me gusta"
          >
            <Heart
              className={`w-5 h-5 transition-colors ${
                isLiked ? 'fill-black dark:fill-white text-black dark:text-white' : 'text-[#201f1d] dark:text-[#a6a096]'
              }`}
            />
          </button>
        </div>
      </div>

      {/* 2. Time Display & Hi-Fi Badge */}
      <div className="flex flex-col items-center justify-center py-0.5 z-10 shrink-0 gap-1">
        <span className="text-sm font-bold tracking-wider text-[#6b6760] dark:text-[#a19c93] font-outfit">
          {formatTime(currentTime)} <span className="text-[#9e9a91] dark:text-[#5e5a52] font-light mx-1.5">|</span> {formatTime(duration)}
        </span>
        <span className="text-[10px] font-mono font-bold px-2.5 py-0.5 bg-[#eae5da] dark:bg-[#1a1917] rounded-full border border-[#ded8cd] dark:border-[#2a2824] text-[#4a4742] dark:text-[#aba496]">
          {audioQualityBadge}
        </span>
      </div>

      {/* 3. Central Symmetrical 8-Petal Vinyl Artwork & Wavy Scrubber Ring */}
      <div className="relative flex items-center justify-center my-auto py-1 shrink-0 w-[340px] h-[340px] sm:w-[360px] sm:h-[360px] mx-auto overflow-visible">
        {/* Symmetrical Wavy Scrubber Ring */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-auto overflow-visible">
          <PlayerScallopedRing
            progressPercent={progressPercent}
            isPlaying={isPlaying}
            onSeekPercent={(percent) => seek((percent / 100) * duration)}
          />
        </div>

        {/* Album Artwork: 80% when playing, 60% when paused with scale animation */}
        <div
          onClick={togglePlay}
          className={`relative w-[272px] h-[272px] sm:w-[288px] sm:h-[288px] cursor-pointer shadow-2xl overflow-hidden bg-[#e0ded8] dark:bg-[#201e1b] transition-transform duration-700 ease-out pointer-events-auto ${
            isPlaying ? 'scale-100 animate-spin-slow' : 'scale-[0.75]'
          }`}
          style={{ clipPath: 'url(#flower-8-smooth)' }}
        >
          <img
            src={currentTrack.coverUrl || 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop'}
            alt={currentTrack.title}
            onError={(e) => {
              (e.target as HTMLImageElement).src = 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop';
            }}
            className="w-full h-full object-cover grayscale contrast-125 hover:scale-105 transition-transform duration-500"
          />

          {/* Center vinyl pinhole hole */}
          <div className="absolute inset-0 m-auto w-5 h-5 bg-[#f5f2ea] dark:bg-[#0f0e0d] rounded-full border-2 border-black/80 dark:border-white/80 shadow-inner" />
        </div>
      </div>

      {/* 4. Track Metadata */}
      <div className="text-center px-4 pt-1 z-10 shrink-0">
        <h2 className="text-xl sm:text-2xl font-black font-outfit tracking-tight text-[#121212] dark:text-white uppercase truncate">
          {currentTrack.title}
        </h2>
        <p className="text-sm sm:text-base font-semibold text-[#6b6760] dark:text-[#a19c93] mt-0.5 truncate">
          {currentTrack.artist} {currentTrack.album ? `• ${currentTrack.album}` : ''}
        </p>
      </div>

      {/* 5. Playback Controls Row */}
      <div className="px-4 py-1.5 flex items-center justify-between z-10 max-w-[360px] mx-auto w-full shrink-0 pointer-events-auto">
        {/* Repeat Toggle */}
        <button
          onClick={toggleRepeat}
          className={`p-2 transition-colors cursor-pointer active:scale-90 ${
            repeatMode !== 'off' ? 'text-black dark:text-white font-bold' : 'text-[#75726b] dark:text-[#7d7970] hover:text-black dark:hover:text-white'
          }`}
          title="Modo de repetición"
        >
          {repeatMode === 'one' ? (
            <Repeat1 className="w-6 h-6 text-black dark:text-white" />
          ) : (
            <Repeat className="w-6 h-6" />
          )}
        </button>

        {/* Previous Track */}
        <button
          onClick={prevTrack}
          className="p-2 text-[#121212] dark:text-white hover:scale-110 active:scale-90 transition-all cursor-pointer"
          title="Canción anterior"
        >
          <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24">
            <path d="M19 20L9 12l10-8v16zM5 19V5h2v14H5z" />
          </svg>
        </button>

        {/* Play / Pause Large Circular Button */}
        <button
          onClick={togglePlay}
          className="w-16 h-16 sm:w-18 sm:h-18 bg-[#121212] dark:bg-white text-white dark:text-black rounded-full flex items-center justify-center shadow-2xl hover:scale-105 active:scale-95 transition-all cursor-pointer"
          title={isPlaying ? 'Pausa' : 'Reproducir'}
        >
          {isPlaying ? (
            <Pause className="w-7 h-7 fill-current" />
          ) : (
            <Play className="w-7 h-7 fill-current ml-0.5" />
          )}
        </button>

        {/* Next Track */}
        <button
          onClick={nextTrack}
          className="p-2 text-[#121212] dark:text-white hover:scale-110 active:scale-90 transition-all cursor-pointer"
          title="Siguiente canción"
        >
          <svg className="w-6 h-6 fill-current" viewBox="0 0 24 24">
            <path d="M5 4l10 8-10 8V4zm14 1v14h-2V5h2z" />
          </svg>
        </button>

        {/* Shuffle Toggle */}
        <button
          onClick={toggleShuffle}
          className={`p-2 transition-colors cursor-pointer active:scale-90 ${
            isShuffle ? 'text-black dark:text-white font-bold' : 'text-[#75726b] dark:text-[#7d7970] hover:text-black dark:hover:text-white'
          }`}
          title="Modo aleatorio"
        >
          <Shuffle className="w-6 h-6" />
        </button>
      </div>

      {/* 6. Dynamic Rolling Lyrics Bottom Preview Section */}
      <div className="px-2 pt-1 pb-1 z-10 shrink-0 pointer-events-auto">
        <div className="flex items-center justify-between mb-1">
          <span className="text-xs font-extrabold text-[#201f1d] dark:text-[#dedad2] font-outfit uppercase tracking-wider">
            Letras
          </span>
          <button
            onClick={() => setExpandedLyrics(!expandedLyrics)}
            className="text-[#75726b] dark:text-[#918c82] hover:text-black dark:hover:text-white transition-colors cursor-pointer p-1"
            title="Expandir letras"
          >
            {expandedLyrics ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
          </button>
        </div>

        {/* Multi-line Dynamic Sliding Window Preview */}
        <div
          onClick={() => setExpandedLyrics(true)}
          className="cursor-pointer text-xs sm:text-[13px] leading-relaxed line-clamp-2 transition-all font-medium min-h-[36px]"
        >
          {visibleLyricsPreview.length > 0 ? (
            <div className="flex flex-wrap gap-x-2 items-center">
              {visibleLyricsPreview.map((line, idx) => {
                const isCurrentLine = line.text === lyricsList[activeLyricIndex]?.text;
                return (
                  <span
                    key={idx}
                    className={`transition-all duration-300 ${
                      isCurrentLine
                        ? 'text-[#121212] dark:text-white font-black text-sm'
                        : 'text-[#87837b]/75 dark:text-[#807b71] font-medium'
                    }`}
                  >
                    {line.text}
                    {idx < visibleLyricsPreview.length - 1 ? ' • ' : ''}
                  </span>
                );
              })}
            </div>
          ) : (
            <span className="text-[#87837b]/80 dark:text-[#7a766c] italic">
              Toca aquí para ver la letra completa o sincronizar archivos .lrc
            </span>
          )}
        </div>
      </div>

      {/* Expanded Fullscreen Lyrics Overlay */}
      {expandedLyrics && (
        <div className="absolute inset-0 bg-[#f5f2ea]/95 dark:bg-[#0f0e0d]/95 backdrop-blur-md z-50 flex flex-col p-6 animate-fade-in touch-auto">
          <div className="flex items-center justify-between mb-4 pb-2 border-b border-[#ded8cd] dark:border-[#2a2824] shrink-0">
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b]">
                Letras Sincronizadas
              </span>
              <h3 className="text-xl font-black text-black dark:text-white font-outfit truncate max-w-[260px]">
                {currentTrack.title}
              </h3>
            </div>
            <button
              onClick={() => setExpandedLyrics(false)}
              className="p-2.5 bg-[#eae5da] dark:bg-[#1f1d1a] hover:bg-black hover:text-white dark:hover:bg-white dark:hover:text-black rounded-full transition-colors cursor-pointer text-black dark:text-white"
            >
              <Minimize2 size={18} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto no-scrollbar flex flex-col gap-6 py-[36vh] text-center touch-pan-y overflow-x-hidden px-4">
            {lyricsList.length > 0 ? (
              lyricsList.map((line, idx) => {
                const isActive = idx === activeLyricIndex;
                return (
                  <p
                    key={idx}
                    ref={isActive ? activeLyricRef : undefined}
                    onClick={() => seek(line.time)}
                    className={`cursor-pointer transition-all duration-300 font-outfit select-none px-4 max-w-full break-words whitespace-normal leading-snug ${
                      isActive
                        ? 'text-black dark:text-white text-2xl sm:text-3xl font-black opacity-100 my-2 scale-105'
                        : 'text-[#75726b] dark:text-[#8a857b] text-base sm:text-lg font-medium opacity-60 hover:opacity-100 hover:text-black dark:hover:text-white'
                    }`}
                  >
                    {line.text}
                  </p>
                );
              })
            ) : (
              <div className="flex flex-col items-center justify-center my-auto text-[#75726b] dark:text-[#8a857b] gap-2">
                <span className="text-base font-semibold">Sin archivo .lrc sincronizado para esta canción</span>
                <span className="text-xs text-[#9c978e]">Coloca un archivo .lrc con el mismo nombre de la canción en su carpeta o usa un archivo con letras incrustadas</span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Queue Drawer Modal (With Auto-Scroll to Active Song) */}
      {showQueue && (
        <div className="absolute inset-0 bg-[#f5f2ea]/95 dark:bg-[#0f0e0d]/95 backdrop-blur-md z-50 flex flex-col p-6 animate-fade-in touch-auto">
          <div className="flex items-center justify-between mb-4 pb-2 border-b border-[#ded8cd] dark:border-[#2a2824] shrink-0">
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b]">
                Cola de Reproducción
              </span>
              <h3 className="text-lg font-black text-black dark:text-white font-outfit truncate">
                {tracks.length} canciones
              </h3>
            </div>
            <button
              onClick={() => setShowQueue(false)}
              className="p-2 bg-[#eae5da] dark:bg-[#1f1d1a] hover:bg-black hover:text-white rounded-full transition-colors cursor-pointer text-black dark:text-white"
            >
              <X size={16} />
            </button>
          </div>

          <div className="flex-1 overflow-y-auto no-scrollbar flex flex-col gap-2 py-2 touch-pan-y">
            {tracks.map((track, i) => {
              const isCurrent = currentTrack.id === track.id;
              return (
                <div
                  key={track.id}
                  ref={isCurrent ? activeQueueItemRef : undefined}
                  onClick={() => {
                    playTrack(track);
                    setShowQueue(false);
                  }}
                  className={`flex items-center justify-between p-3 rounded-2xl transition-all cursor-pointer ${
                    isCurrent
                      ? 'bg-black dark:bg-white text-white dark:text-black shadow-md'
                      : 'bg-[#eae5da] dark:bg-[#1a1917] text-[#121212] dark:text-[#f5f2ea] hover:bg-[#ded8cd] dark:hover:bg-[#252320]'
                  }`}
                >
                  <div className="flex items-center gap-3 truncate">
                    <span className="text-xs font-bold w-4 text-center">{i + 1}</span>
                    <div className="flex flex-col truncate">
                      <span className="text-xs font-bold truncate">{track.title}</span>
                      <span className={`text-[11px] truncate ${isCurrent ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>
                        {track.artist}
                      </span>
                    </div>
                  </div>
                  <span className={`text-xs font-medium ${isCurrent ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>
                    {formatTime(track.duration || 180)}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
