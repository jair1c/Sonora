import React, { useState, useRef, useEffect, useMemo } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { SongCover } from './SongCover';
import { EqualizerModal } from './EqualizerModal';
import { SleepTimerModal } from './SleepTimerModal';
import { StatsModal } from './StatsModal';
import { TagEditorModal } from './TagEditorModal';
import {
  ArrowLeft,
  Check,
  Play,
  Heart,
  Sliders,
  SlidersHorizontal,
  Moon,
  BarChart3,
  Search,
  RefreshCw,
  Plus,
  Trash2,
  MoreVertical,
  Disc3,
  Music,
  Folder,
  X,
  ArrowUpDown,
  Eye,
  EyeOff,
  Flame,
  Clock
} from 'lucide-react';

import type { Track, Playlist } from '../data/musicData';

export const ArtistSelectScreen: React.FC = () => {
  const {
    artists,
    tracks,
    playlists,
    selectedArtistIds,
    toggleArtistSelection,
    setActiveScreen,
    libraryTab,
    setLibraryTab,
    searchQuery,
    setSearchQuery,
    playTrack,
    currentTrack,
    isPlaying,
    togglePlay,
    toggleLike,
    createPlaylist,
    deletePlaylist,
    addTrackToPlaylist,
    removeTrackFromPlaylist,
    scanLocalMusic,
    isScanning,
    navTabsConfig,
    blacklistedFolders,
    toggleFolderBlacklist,
    sortMode,
    setSortMode
  } = usePlayer();

  const [showEqModal, setShowEqModal] = useState(false);
  const [showSleepModal, setShowSleepModal] = useState(false);
  const [showStatsModal, setShowStatsModal] = useState(false);
  const [showTagModal, setShowTagModal] = useState(false);
  const [selectedTagTrack, setSelectedTagTrack] = useState<Track | null>(null);

  const [showSortModal, setShowSortModal] = useState(false);
  const [showNewPlInput, setShowNewPlInput] = useState(false);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [selectedPlaylistForManage, setSelectedPlaylistForManage] = useState<Playlist | null>(null);
  const [playlistAlert, setPlaylistAlert] = useState<string | null>(null);
  const [playlistToDelete, setPlaylistToDelete] = useState<Playlist | null>(null);

  const activeSongRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll to currently playing song on tab open
  useEffect(() => {
    if (libraryTab === 'canciones' && activeSongRef.current) {
      const timer = setTimeout(() => {
        activeSongRef.current?.scrollIntoView({
          behavior: 'smooth',
          block: 'center'
        });
      }, 150);
      return () => clearTimeout(timer);
    }
  }, [libraryTab, currentTrack?.id]);

  useEffect(() => {
    if (selectedPlaylistForManage || playlistToDelete || showSortModal) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [selectedPlaylistForManage, playlistToDelete, showSortModal]);

  const handleBack = () => {
    setActiveScreen('player');
  };

  const handleContinueToPlayer = () => {
    if (selectedArtistIds.length > 0) {
      const selectedTracks = tracks.filter((t) =>
        selectedArtistIds.some(
          (aId) =>
            t.artist.toLowerCase().includes(aId.toLowerCase()) ||
            aId.toLowerCase().includes(t.artist.toLowerCase())
        )
      );
      if (selectedTracks.length > 0) {
        playTrack(selectedTracks[0]);
      }
    }
    setActiveScreen('player');
  };

  // Filtered lists based on search query
  const filteredArtists = artists.filter((a) =>
    a.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredTracks = useMemo(() => {
    const list = tracks.filter(
      (t) =>
        t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.artist.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (t.album && t.album.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    switch (sortMode) {
      case 'az':
        return list.sort((a, b) => a.title.localeCompare(b.title));
      case 'za':
        return list.sort((a, b) => b.title.localeCompare(a.title));
      case 'artist':
        return list.sort((a, b) => a.artist.localeCompare(b.artist));
      case 'recent':
        return list.sort((a, b) => {
          const timeA = a.dateAdded || a.dateModified || a.lastPlayed || 0;
          const timeB = b.dateAdded || b.dateModified || b.lastPlayed || 0;
          return timeB - timeA;
        });
      case 'duration':
        return list.sort((a, b) => (b.duration || 0) - (a.duration || 0));
      default:
        return list;
    }
  }, [tracks, searchQuery, sortMode]);

  // Group tracks by album
  const albumsMap = useMemo(() => {
    const map = new Map<string, { title: string; artist: string; count: number; cover: string; tracks: Track[] }>();
    tracks.forEach((t) => {
      const albName = t.album || 'Álbum Desconocido';
      if (!map.has(albName)) {
        map.set(albName, {
          title: albName,
          artist: t.artist,
          count: 1,
          cover: t.coverUrl,
          tracks: [t]
        });
      } else {
        const entry = map.get(albName)!;
        entry.count += 1;
        entry.tracks.push(t);
      }
    });
    return Array.from(map.values());
  }, [tracks]);

  // Group tracks by folder path including blacklist status
  const foldersMap = useMemo(() => {
    const map = new Map<string, { folderName: string; count: number; tracks: Track[]; isBlacklisted: boolean }>();
    
    // We read all device tracks cached to see blacklisted folders as well
    let baseList: Track[] = tracks;
    try {
      const cached = localStorage.getItem('luxTune_local_songs');
      if (cached) baseList = JSON.parse(cached);
    } catch (e) {}

    baseList.forEach((t) => {
      const parts = t.filePath ? t.filePath.split('/') : [];
      const folderName = parts.length > 1 ? parts[parts.length - 2] : (t.album || 'Música Local');
      const isBl = blacklistedFolders.includes(folderName);
      if (!map.has(folderName)) {
        map.set(folderName, { folderName, count: 1, tracks: [t], isBlacklisted: isBl });
      } else {
        const entry = map.get(folderName)!;
        entry.count += 1;
        entry.tracks.push(t);
      }
    });
    return Array.from(map.values());
  }, [tracks, blacklistedFolders]);

  // Smart Playlists Data
  const favoriteTracks = useMemo(() => tracks.filter(t => t.isLiked), [tracks]);
  const top25Tracks = useMemo(() => [...tracks].sort((a, b) => (b.playCount || 0) - (a.playCount || 0)).slice(0, 25), [tracks]);
  const recentTracks = useMemo(() => [...tracks].sort((a, b) => {
    const timeA = a.dateAdded || a.dateModified || a.lastPlayed || 0;
    const timeB = b.dateAdded || b.dateModified || b.lastPlayed || 0;
    return timeB - timeA;
  }).slice(0, 30), [tracks]);

  const sortLabelMap = {
    'az': 'Nombre (A → Z)',
    'za': 'Nombre (Z → A)',
    'artist': 'Artista (A → Z)',
    'recent': 'Fecha Más Reciente',
    'duration': 'Mayor Duración'
  };


  return (
    <div className="relative w-full h-full min-h-screen bg-[#f5f2ea] dark:bg-[#0f0e0d] flex flex-col justify-between select-none text-[#121212] dark:text-[#f5f2ea] overflow-x-hidden pt-4 pb-20 transition-colors duration-300">
      {/* Top App Bar with Navigation & Tools */}
      <div className="px-6 pt-2 pb-1 flex items-center justify-between z-10">
        <button
          onClick={handleBack}
          className="w-10 h-10 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#eae5da] dark:hover:bg-[#1f1d1a] active:scale-95 transition-all cursor-pointer bg-[#f5f2ea] dark:bg-[#141312] text-[#2c2b29] dark:text-[#f5f2ea]"
        >
          <ArrowLeft size={18} />
        </button>

        <span className="text-[13px] font-bold tracking-tight text-[#121212] dark:text-white uppercase font-outfit">
          Tu Biblioteca Local
        </span>

        {/* Quick Tools Header Actions */}
        <div className="flex items-center gap-1.5">
          <button
            onClick={() => setShowEqModal(true)}
            title="Ecualizador"
            className="w-9 h-9 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#eae5da] dark:hover:bg-[#1f1d1a] active:scale-95 transition-all text-[#2c2b29] dark:text-[#dedad2] bg-[#f5f2ea] dark:bg-[#141312] cursor-pointer"
          >
            <Sliders size={15} />
          </button>
          <button
            onClick={() => setShowSleepModal(true)}
            title="Temporizador de apagado"
            className="w-9 h-9 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#eae5da] dark:hover:bg-[#1f1d1a] active:scale-95 transition-all text-[#2c2b29] dark:text-[#dedad2] bg-[#f5f2ea] dark:bg-[#141312] cursor-pointer"
          >
            <Moon size={15} />
          </button>
          <button
            onClick={() => setShowStatsModal(true)}
            title="Estadísticas sonoraStats"
            className="w-9 h-9 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#eae5da] dark:hover:bg-[#1f1d1a] active:scale-95 transition-all text-[#2c2b29] dark:text-[#dedad2] bg-[#f5f2ea] dark:bg-[#141312] cursor-pointer"
          >
            <BarChart3 size={15} />
          </button>
          {!navTabsConfig.some((t) => t.id === 'ajustes' || t.targetScreen === 'settings') && (
            <button
              onClick={() => setActiveScreen('settings')}
              title="Ajustes"
              className="w-9 h-9 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#eae5da] dark:hover:bg-[#1f1d1a] active:scale-95 transition-all text-[#2c2b29] dark:text-[#dedad2] bg-[#f5f2ea] dark:bg-[#141312] cursor-pointer"
            >
              <SlidersHorizontal size={15} />
            </button>
          )}
        </div>
      </div>

      {/* Hero Header */}
      <div className="px-6 pt-3 pb-2 z-10">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-[#121212] dark:text-white font-outfit uppercase">
            Sonora
          </h1>
          <button
            onClick={() => scanLocalMusic()}
            disabled={isScanning}
            className="flex items-center gap-1.5 text-xs text-[#716e68] dark:text-[#969186] hover:text-black dark:hover:text-white transition-colors cursor-pointer bg-[#eae5da] dark:bg-[#1f1d1a] px-3 py-1.5 rounded-full border border-[#ded8cd] dark:border-[#2a2824]"
          >
            <RefreshCw size={12} className={isScanning ? 'animate-spin' : ''} />
            <span>{isScanning ? 'Escaneando...' : 'Escanear'}</span>
          </button>
        </div>
        <p className="text-xs text-[#716e68] dark:text-[#969186] mt-0.5 font-medium">
          {tracks.length} canciones locales indexadas • 100% Offline
        </p>
      </div>

      {/* Search Input Bar */}
      <div className="px-6 py-1 z-10">
        <div className="flex items-center gap-2 bg-[#eae5da] dark:bg-[#1a1917] px-3.5 py-2 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824] shadow-inner">
          <Search size={15} className="text-[#716e68] dark:text-[#969186] shrink-0" />
          <input
            type="text"
            placeholder="Buscar por canción, artista, álbum o carpeta..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="bg-transparent border-none outline-none text-xs text-[#121212] dark:text-[#f5f2ea] w-full font-medium placeholder-[#8f8b83] dark:placeholder-[#6b675e]"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="text-[#716e68] dark:text-[#969186] hover:text-black dark:hover:text-white text-xs font-bold"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Modern Filter Category Tabs */}
      <div className="px-6 py-2 z-10">
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-1">
          {[
            { id: 'canciones', label: 'Canciones', icon: <Music size={13} /> },
            { id: 'artistas', label: 'Artistas', icon: <Disc3 size={13} /> },
            { id: 'albumes', label: 'Álbumes', icon: <Disc3 size={13} /> },
            { id: 'listas', label: 'Listas ♡', icon: <Heart size={13} /> },
            { id: 'carpetas', label: 'Carpetas', icon: <Folder size={13} /> }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setLibraryTab(tab.id as any)}
              className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-bold transition-all shrink-0 cursor-pointer ${
                libraryTab === tab.id
                  ? 'bg-black dark:bg-white text-white dark:text-black shadow-sm'
                  : 'bg-[#eae5da] dark:bg-[#1a1917] text-[#5e5b54] dark:text-[#a8a397] hover:bg-[#ded8cd] dark:hover:bg-[#252320] border border-[#ded8cd] dark:border-[#2a2824]'
              }`}
            >
              {tab.icon}
              <span>{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Content Area Based on Active Tab */}
      <div className="flex-1 px-6 py-2 z-10">
        {/* TAB 1: ARTISTAS (Honeycomb Scallop Selector) */}
        {libraryTab === 'artistas' && (
          <div className="grid grid-cols-3 gap-y-6 gap-x-3 py-3 place-items-center">
            {filteredArtists.map((artist) => {
              const isSelected = selectedArtistIds.includes(artist.id);

              return (
                <div
                  key={artist.id}
                  onClick={() => toggleArtistSelection(artist.id)}
                  className="flex flex-col items-center cursor-pointer group select-none transition-transform active:scale-95"
                >
                  <div className="relative w-[82px] h-[82px] sm:w-[96px] sm:h-[96px] flex items-center justify-center">
                    <ArtistAvatar
                      artistName={artist.name}
                      avatarUrl={artist.avatarUrl}
                      isSelected={isSelected}
                    />

                    {isSelected && (
                      <div className="absolute -bottom-1 -right-1 w-[22px] h-[22px] bg-white dark:bg-black rounded-full flex items-center justify-center shadow-md border border-[#e2ded5] dark:border-[#33302b] z-10 animate-scale-in">
                        <Check size={13} className="text-black dark:text-white stroke-[3]" />
                      </div>
                    )}
                  </div>

                  <span className={`text-[11.5px] text-center mt-2 font-medium tracking-tight truncate max-w-[85px] ${isSelected ? 'font-bold text-black dark:text-white' : 'text-[#33312e] dark:text-[#dedad2]'}`}>
                    {cleanArtistName(artist.name) || artist.name}
                  </span>
                  <span className="text-[10px] text-[#716e68] dark:text-[#8a857b]">
                    {artist.trackCount || 1} {artist.trackCount === 1 ? 'canción' : 'canciones'}
                  </span>
                </div>
              );
            })}
          </div>
        )}

        {/* TAB 2: TODAS LAS CANCIONES (With Sort Bar & Auto-Scroll) */}
        {libraryTab === 'canciones' && (
          <div className="flex flex-col gap-2 py-1">
            {/* Sort & Action Bar */}
            <div className="flex items-center justify-between pb-1 px-1">
              <span className="text-[11px] font-bold text-[#75726b] dark:text-[#8a857b] uppercase tracking-wider">
                {filteredTracks.length} {filteredTracks.length === 1 ? 'canción' : 'canciones'}
              </span>
              <button
                onClick={() => setShowSortModal(true)}
                className="flex items-center gap-1 text-[11px] font-bold text-black dark:text-white bg-[#eae5da] dark:bg-[#1a1917] px-2.5 py-1 rounded-full border border-[#ded8cd] dark:border-[#2a2824] cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#252320]"
              >
                <ArrowUpDown size={11} />
                <span>{sortLabelMap[sortMode]}</span>
              </button>
            </div>

            {filteredTracks.map((track) => {
              const isCurr = currentTrack?.id === track.id;
              return (
                <div key={track.id} ref={isCurr ? activeSongRef : undefined}>
                  <SongRowItem
                    track={track}
                    isCurrent={isCurr}
                    isPlaying={isPlaying && isCurr}
                    onPlay={() => {
                      if (currentTrack && currentTrack.id === track.id) {
                        if (!isPlaying) togglePlay();
                      } else {
                        playTrack(track);
                      }
                      setActiveScreen('player');
                    }}
                    onToggleLike={() => toggleLike(track.id)}
                    onEditTags={() => {
                      setSelectedTagTrack(track);
                      setShowTagModal(true);
                    }}
                  />
                </div>
              );
            })}
          </div>
        )}

        {/* TAB 3: ÁLBUMES */}
        {libraryTab === 'albumes' && (
          <div className="grid grid-cols-2 gap-3 py-1">
            {albumsMap.map((alb, i) => (
              <div
                key={i}
                onClick={() => {
                  if (alb.tracks.length > 0) {
                    if (currentTrack.id === alb.tracks[0].id) {
                      if (!isPlaying) togglePlay();
                    } else {
                      playTrack(alb.tracks[0]);
                    }
                    setActiveScreen('player');
                  }
                }}
                className="bg-[#eae5da] dark:bg-[#1a1917] p-3 rounded-2xl flex flex-col gap-2 cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#252320] border border-[#ded8cd] dark:border-[#2a2824] transition-all"
              >
                <SongCover
                  src={alb.cover}
                  title={alb.title}
                  artist={alb.artist}
                  shape="scallop"
                  className="w-full aspect-square"
                />

                <div className="flex flex-col truncate">
                  <span className="text-xs font-bold truncate text-black dark:text-white">{alb.title}</span>
                  <span className="text-[11px] text-[#75726b] dark:text-[#8a857b] truncate">{alb.artist} • {alb.count} canciones</span>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* TAB 4: LISTAS (Smart Playlists + Custom Playlists) */}
        {libraryTab === 'listas' && (
          <div className="flex flex-col gap-3.5 py-1">
            {/* Smart Playlists Section */}
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] block mb-2 px-1">
                Listas Inteligentes Automáticas
              </span>
              <div className="grid grid-cols-3 gap-2">
                {/* 1. Favoritas */}
                <div
                  onClick={() => {
                    if (favoriteTracks.length > 0) {
                      playTrack(favoriteTracks[0]);
                      setActiveScreen('player');
                    } else {
                      setPlaylistAlert('Aún no tienes canciones favoritas. Pulsa el corazón ♡ en cualquier canción para añadirla aquí.');
                    }
                  }}
                  className="bg-[#eae5da] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824] flex flex-col items-center text-center cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#252320] active:scale-95 transition-all"
                >
                  <div className="w-10 h-10 rounded-xl bg-red-500 text-white flex items-center justify-center shadow-md mb-1.5">
                    <Heart size={18} className="fill-current" />
                  </div>
                  <span className="text-xs font-bold text-black dark:text-white truncate w-full">Favoritas</span>
                  <span className="text-[10px] text-[#75726b] dark:text-[#8a857b]">{favoriteTracks.length} canciones</span>
                </div>

                {/* 2. Top 25 Más Escuchadas */}
                <div
                  onClick={() => {
                    if (top25Tracks.length > 0) {
                      playTrack(top25Tracks[0]);
                      setActiveScreen('player');
                    } else {
                      setPlaylistAlert('Escucha música para que tus canciones más reproducidas aparezcan aquí automáticamente.');
                    }
                  }}
                  className="bg-[#eae5da] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824] flex flex-col items-center text-center cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#252320] active:scale-95 transition-all"
                >
                  <div className="w-10 h-10 rounded-xl bg-amber-500 text-white flex items-center justify-center shadow-md mb-1.5">
                    <Flame size={18} className="fill-current" />
                  </div>
                  <span className="text-xs font-bold text-black dark:text-white truncate w-full">Top 25</span>
                  <span className="text-[10px] text-[#75726b] dark:text-[#8a857b]">{top25Tracks.length} canciones</span>
                </div>

                {/* 3. Recientes */}
                <div
                  onClick={() => {
                    if (recentTracks.length > 0) {
                      playTrack(recentTracks[0]);
                      setActiveScreen('player');
                    } else {
                      setPlaylistAlert('Tus canciones reproducidas recientemente aparecerán aquí.');
                    }
                  }}
                  className="bg-[#eae5da] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824] flex flex-col items-center text-center cursor-pointer hover:bg-[#ded8cd] dark:hover:bg-[#252320] active:scale-95 transition-all"
                >
                  <div className="w-10 h-10 rounded-xl bg-blue-500 text-white flex items-center justify-center shadow-md mb-1.5">
                    <Clock size={18} />
                  </div>
                  <span className="text-xs font-bold text-black dark:text-white truncate w-full">Recientes</span>
                  <span className="text-[10px] text-[#75726b] dark:text-[#8a857b]">{recentTracks.length} canciones</span>
                </div>
              </div>
            </div>

            {/* Custom User Playlists */}
            <div className="flex items-center justify-between pt-2">
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b] px-1">
                Tus Listas Creadas
              </span>
              <button
                onClick={() => setShowNewPlInput(!showNewPlInput)}
                className="flex items-center gap-1 text-xs font-bold text-black dark:text-white bg-[#eae5da] dark:bg-[#1a1917] px-3 py-1.5 rounded-full border border-[#ded8cd] dark:border-[#2a2824] cursor-pointer"
              >
                <Plus size={13} /> Nueva Lista
              </button>
            </div>

            {showNewPlInput && (
              <div className="flex gap-2 p-2 bg-[#eae5da] dark:bg-[#1a1917] rounded-2xl border border-[#ded8cd] dark:border-[#2a2824]">
                <input
                  type="text"
                  placeholder="Nombre de la lista..."
                  value={newPlaylistName}
                  onChange={(e) => setNewPlaylistName(e.target.value)}
                  className="bg-transparent border-none outline-none text-xs text-[#121212] dark:text-[#f5f2ea] px-2 flex-1 font-semibold placeholder-[#8f8b83] dark:placeholder-[#6b675e]"
                />
                <button
                  onClick={() => {
                    if (newPlaylistName.trim()) {
                      createPlaylist(newPlaylistName.trim());
                      setNewPlaylistName('');
                      setShowNewPlInput(false);
                    }
                  }}
                  className="px-3 py-1 bg-black dark:bg-white text-white dark:text-black rounded-xl text-xs font-bold cursor-pointer"
                >
                  Crear
                </button>
              </div>
            )}

            {/* List of Custom Playlists */}
            {playlists.filter(p => p.id !== 'favorites').map((pl) => {
              const plTracks = tracks.filter((t) => pl.trackIds.includes(t.id));

              return (
                <div
                  key={pl.id}
                  onClick={() => {
                    if (plTracks.length > 0) {
                      if (currentTrack.id === plTracks[0].id) {
                        if (!isPlaying) togglePlay();
                      } else {
                        playTrack(plTracks[0]);
                      }
                      setActiveScreen('player');
                    } else {
                      setSelectedPlaylistForManage(pl);
                    }
                  }}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    setPlaylistToDelete(pl);
                  }}
                  className="flex items-center justify-between p-3.5 bg-[#eae5da] dark:bg-[#1a1917] rounded-2xl hover:bg-[#ded8cd] dark:hover:bg-[#252320] active:scale-[0.99] transition-all cursor-pointer border border-[#ded8cd] dark:border-[#2a2824]"
                >
                  <div className="flex items-center gap-3 truncate">
                    <div className="w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 bg-black dark:bg-white text-[#f5f2ea] dark:text-black">
                      {pl.name.slice(0, 1).toUpperCase()}
                    </div>
                    <div className="flex flex-col truncate">
                      <span className="text-xs font-bold text-[#121212] dark:text-[#f5f2ea] truncate">{pl.name}</span>
                      <span className="text-[11px] text-[#75726b] dark:text-[#8a857b]">
                        {plTracks.length} {plTracks.length === 1 ? 'canción' : 'canciones'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 ml-2">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedPlaylistForManage(pl);
                      }}
                      className="px-2.5 py-1 text-[11px] font-bold bg-[#ded8cd] dark:bg-[#2a2824] hover:bg-black hover:text-white dark:hover:bg-white dark:hover:text-black rounded-lg transition-colors text-[#121212] dark:text-[#f5f2ea]"
                    >
                      Editar
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setPlaylistToDelete(pl);
                      }}
                      className="p-1.5 text-neutral-400 hover:text-red-500 transition-colors"
                      title="Eliminar lista"
                    >
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* TAB 5: CARPETAS (With Blacklist / Hide Toggle) */}
        {libraryTab === 'carpetas' && (
          <div className="flex flex-col gap-2 py-1">
            <div className="flex items-center justify-between pb-1 px-1">
              <span className="text-[11px] font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b]">
                {foldersMap.length} carpetas detectadas
              </span>
              <span className="text-[10px] text-[#75726b] dark:text-[#8a857b]">
                Toca el ojo para ocultar/bloquear
              </span>
            </div>

            {foldersMap.map((folder, i) => {
              const isHidden = folder.isBlacklisted;

              return (
                <div
                  key={i}
                  onClick={() => {
                    if (isHidden) {
                      setPlaylistAlert(`Esta carpeta está oculta en la lista negra. Pulsa el icono para reactivarla.`);
                      return;
                    }
                    if (folder.tracks.length > 0) {
                      if (currentTrack.id === folder.tracks[0].id) {
                        if (!isPlaying) togglePlay();
                      } else {
                        playTrack(folder.tracks[0]);
                      }
                      setActiveScreen('player');
                    }
                  }}
                  className={`flex items-center justify-between p-3.5 rounded-2xl border transition-all cursor-pointer ${
                    isHidden
                      ? 'bg-[#eae5da]/40 dark:bg-[#1a1917]/40 border-dashed border-[#ded8cd] dark:border-[#2a2824] opacity-60'
                      : 'bg-[#eae5da] dark:bg-[#1a1917] hover:bg-[#ded8cd] dark:hover:bg-[#252320] border-[#ded8cd] dark:border-[#2a2824]'
                  }`}
                >
                  <div className="flex items-center gap-3 truncate">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${isHidden ? 'bg-neutral-300 dark:bg-neutral-800 text-neutral-500' : 'bg-[#ded8cd] dark:bg-[#2a2824] text-[#121212] dark:text-[#f5f2ea]'}`}>
                      <Folder size={18} />
                    </div>
                    <div className="flex flex-col truncate">
                      <div className="flex items-center gap-1.5 truncate">
                        <span className={`text-xs font-bold truncate ${isHidden ? 'line-through text-neutral-500' : 'text-black dark:text-white'}`}>
                          {folder.folderName}
                        </span>
                        {isHidden && (
                          <span className="text-[9px] font-bold px-1.5 py-0.5 bg-red-100 dark:bg-red-950/60 text-red-600 dark:text-red-400 rounded-md">
                            Oculta
                          </span>
                        )}
                      </div>
                      <span className="text-[11px] text-[#75726b] dark:text-[#8a857b]">
                        {folder.count} archivos de audio
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 ml-2">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        toggleFolderBlacklist(folder.folderName);
                      }}
                      className={`p-2 rounded-xl border transition-all ${
                        isHidden
                          ? 'bg-red-500 text-white border-red-600'
                          : 'bg-[#ded8cd] dark:bg-[#2a2824] text-black dark:text-white border-[#ded8cd] dark:border-[#2a2824] hover:bg-black hover:text-white'
                      }`}
                      title={isHidden ? 'Quitar de lista negra' : 'Ocultar carpeta / Enviar a lista negra'}
                    >
                      {isHidden ? <EyeOff size={15} /> : <Eye size={15} />}
                    </button>
                    {!isHidden && <Play size={15} className="text-[#75726b] dark:text-[#8a857b] ml-1" />}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Floating Instant Mix Button (Only shown on Artistas tab when artists are selected) */}
      {libraryTab === 'artistas' && selectedArtistIds.length > 0 && (
        <div className="fixed bottom-20 left-6 right-6 max-w-md mx-auto z-20 animate-fade-in">
          <button
            onClick={handleContinueToPlayer}
            className="w-full py-3.5 bg-black dark:bg-white text-white dark:text-black font-semibold text-sm rounded-full hover:bg-neutral-900 active:scale-[0.98] transition-all shadow-2xl cursor-pointer flex items-center justify-center gap-2 border border-neutral-800 dark:border-neutral-200"
          >
            <Play size={15} className="fill-current" />
            <span>Reproducir Mix ({selectedArtistIds.length} {selectedArtistIds.length === 1 ? 'artista' : 'artistas'})</span>
          </button>
        </div>
      )}

      {/* Sort Options Modal */}
      {showSortModal && (
        <div
          onClick={() => setShowSortModal(false)}
          onTouchMove={(e) => e.stopPropagation()}
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-4 animate-fade-in touch-none overscroll-contain"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm bg-[#F5F2EA] dark:bg-[#161513] rounded-3xl p-6 shadow-2xl border border-[#DED8CD] dark:border-[#2a2824] flex flex-col gap-3 text-[#121212] dark:text-[#f5f2ea]"
          >
            <div className="flex items-center justify-between border-b border-[#ded8cd] dark:border-[#2a2824] pb-3">
              <h3 className="font-bold text-base font-outfit text-black dark:text-white">Ordenar Canciones</h3>
              <button
                onClick={() => setShowSortModal(false)}
                className="w-8 h-8 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center text-black dark:text-white"
              >
                <X size={15} />
              </button>
            </div>

            <div className="flex flex-col gap-2 py-1">
              {[
                { id: 'az', label: 'Nombre (A → Z)' },
                { id: 'za', label: 'Nombre (Z → A)' },
                { id: 'artist', label: 'Artista (A → Z)' },
                { id: 'recent', label: 'Fecha de Adición (Más Recientes)' },

                { id: 'duration', label: 'Mayor Duración' }
              ].map((opt) => (
                <button
                  key={opt.id}
                  onClick={() => {
                    setSortMode(opt.id as any);
                    setShowSortModal(false);
                  }}
                  className={`flex items-center justify-between p-3 rounded-2xl text-xs font-bold transition-all cursor-pointer border ${
                    sortMode === opt.id
                      ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-sm'
                      : 'bg-[#eae5da] dark:bg-[#1a1917] text-black dark:text-white border-[#ded8cd] dark:border-[#2a2824] hover:bg-[#ded8cd]'
                  }`}
                >
                  <span>{opt.label}</span>
                  {sortMode === opt.id && <Check size={16} />}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Playlist Manage / Add Songs Modal */}
      {selectedPlaylistForManage && (
        <div
          onClick={() => setSelectedPlaylistForManage(null)}
          onTouchMove={(e) => e.stopPropagation()}
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 animate-fade-in touch-none overscroll-contain"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md bg-[#F5F2EA] dark:bg-[#161513] rounded-3xl p-6 shadow-2xl border border-[#DED8CD] dark:border-[#2a2824] flex flex-col gap-4 text-[#121212] dark:text-[#f5f2ea] max-h-[85vh] overscroll-contain touch-pan-y"
          >
            <div className="flex items-center justify-between border-b border-[#ded8cd] dark:border-[#2a2824] pb-3">
              <div>
                <h3 className="font-bold text-base font-outfit text-black dark:text-white">{selectedPlaylistForManage.name}</h3>
                <p className="text-xs text-[#75726b] dark:text-[#8a857b]">Toca canciones para agregarlas o quitarlas</p>
              </div>
              <button
                onClick={() => setSelectedPlaylistForManage(null)}
                className="w-8 h-8 rounded-full border border-[#ded8cd] dark:border-[#2a2824] flex items-center justify-center text-black dark:text-white cursor-pointer"
              >
                <X size={16} />
              </button>
            </div>

            <div className="overflow-y-auto max-h-[55vh] flex flex-col gap-2 py-1 overscroll-contain touch-pan-y">
              {tracks.map((t) => {
                const inPl = selectedPlaylistForManage.trackIds.includes(t.id);
                return (
                  <div
                    key={t.id}
                    onClick={() => {
                      if (inPl) {
                        removeTrackFromPlaylist(selectedPlaylistForManage.id, t.id);
                        setSelectedPlaylistForManage(prev => prev ? { ...prev, trackIds: prev.trackIds.filter(id => id !== t.id) } : null);
                      } else {
                        addTrackToPlaylist(selectedPlaylistForManage.id, t.id);
                        setSelectedPlaylistForManage(prev => prev ? { ...prev, trackIds: [...prev.trackIds, t.id] } : null);
                      }
                    }}
                    className={`flex items-center justify-between p-2.5 rounded-xl cursor-pointer transition-all border ${
                      inPl
                        ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-sm'
                        : 'bg-[#eae5da] dark:bg-[#1a1917] text-black dark:text-white hover:bg-[#ded8cd] dark:hover:bg-[#252320] border-[#ded8cd] dark:border-[#2a2824]'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 truncate">
                      <SongCover src={t.coverUrl} title={t.title} className="w-8 h-8 rounded-lg" />
                      <div className="flex flex-col truncate">
                        <span className="text-xs font-bold truncate">{t.title}</span>
                        <span className={`text-[10px] truncate ${inPl ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>{t.artist}</span>
                      </div>
                    </div>

                    <div className={`w-5 h-5 rounded-full flex items-center justify-center border ${
                      inPl ? 'bg-white dark:bg-black text-black dark:text-white border-transparent' : 'border-[#aba496]'
                    }`}>
                      {inPl && <Check size={12} strokeWidth={3} />}
                    </div>
                  </div>
                );
              })}
            </div>

            <button
              onClick={() => setSelectedPlaylistForManage(null)}
              className="w-full py-3 bg-black dark:bg-white text-white dark:text-black rounded-full font-bold text-xs cursor-pointer shadow-md"
            >
              Listo
            </button>
          </div>
        </div>
      )}

      {/* Quick Alert Toast */}
      {playlistAlert && (
        <div className="fixed top-12 left-6 right-6 max-w-md mx-auto z-50 bg-black dark:bg-white text-white dark:text-black p-4 rounded-2xl shadow-2xl flex items-center justify-between animate-fade-in text-xs font-medium border border-neutral-800 dark:border-neutral-200">
          <span>{playlistAlert}</span>
          <button onClick={() => setPlaylistAlert(null)} className="ml-3 font-bold text-neutral-400 dark:text-neutral-600 hover:text-white dark:hover:text-black">✕</button>
        </div>
      )}

      {/* Custom Confirmation Modal for Deleting Playlist */}
      {playlistToDelete && (
        <div
          onClick={() => setPlaylistToDelete(null)}
          onTouchMove={(e) => e.stopPropagation()}
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-6 animate-fade-in touch-none overscroll-contain"
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-xs bg-[#F5F2EA] dark:bg-[#161513] rounded-3xl p-6 shadow-2xl border border-[#DED8CD] dark:border-[#2a2824] flex flex-col items-center text-center gap-4 animate-in zoom-in-95 duration-150 text-[#121212] dark:text-[#f5f2ea] overscroll-contain touch-pan-y"
          >
            <div className="w-12 h-12 rounded-full bg-red-100 dark:bg-red-950/50 text-red-600 dark:text-red-400 flex items-center justify-center">
              <Trash2 size={22} />
            </div>
            <div>
              <h3 className="font-bold text-base text-black dark:text-white font-outfit">¿Eliminar lista?</h3>
              <p className="text-xs text-[#75726b] dark:text-[#8a857b] mt-1.5 leading-relaxed">
                ¿Deseas eliminar la lista <strong className="text-black dark:text-white font-bold">"{playlistToDelete.name}"</strong>? Las canciones permanecerán en tu biblioteca.
              </p>
            </div>
            <div className="flex gap-2 w-full mt-2">
              <button
                onClick={() => setPlaylistToDelete(null)}
                className="flex-1 py-2.5 rounded-full border border-[#ded8cd] dark:border-[#2a2824] text-xs font-bold text-[#121212] dark:text-[#f5f2ea] bg-[#eae5da] dark:bg-[#1a1917] hover:bg-[#ded8cd] dark:hover:bg-[#252320] active:scale-95 transition-all cursor-pointer"
              >
                Cancelar
              </button>
              <button
                onClick={() => {
                  deletePlaylist(playlistToDelete.id);
                  setPlaylistToDelete(null);
                }}
                className="flex-1 py-2.5 rounded-full text-xs font-bold text-white bg-red-600 hover:bg-red-700 active:scale-95 transition-all shadow-md cursor-pointer"
              >
                Eliminar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modals */}
      <EqualizerModal isOpen={showEqModal} onClose={() => setShowEqModal(false)} />
      <SleepTimerModal isOpen={showSleepModal} onClose={() => setShowSleepModal(false)} />
      <StatsModal isOpen={showStatsModal} onClose={() => setShowStatsModal(false)} />
      <TagEditorModal isOpen={showTagModal} onClose={() => setShowTagModal(false)} track={selectedTagTrack || undefined} />
    </div>
  );
};

interface SongRowItemProps {
  track: Track;
  isCurrent: boolean;
  isPlaying: boolean;
  onPlay: () => void;
  onToggleLike: () => void;
  onEditTags: () => void;
}

const SongRowItem: React.FC<SongRowItemProps> = React.memo(({
  track,
  isCurrent,
  isPlaying,
  onPlay,
  onToggleLike,
  onEditTags
}) => {
  const formatSec = (seconds?: number) => {
    if (!seconds) return '03:00';
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div
      className={`flex items-center justify-between p-2.5 rounded-2xl transition-all cursor-pointer border ${
        isCurrent
          ? 'bg-black dark:bg-white text-white dark:text-black border-transparent shadow-md'
          : 'bg-[#eae5da] dark:bg-[#1a1917] hover:bg-[#ded8cd] dark:hover:bg-[#252320] text-[#121212] dark:text-[#f5f2ea] border-[#ded8cd] dark:border-[#2a2824]'
      }`}
    >
      <div onClick={onPlay} className="flex items-center gap-3 truncate flex-1">
        <div className="relative shrink-0">
          <SongCover
            src={track.coverUrl}
            title={track.title}
            artist={track.artist}
            className="w-10 h-10 rounded-xl"
          />
          {isCurrent && isPlaying && (
            <div className="absolute inset-0 bg-black/40 dark:bg-white/30 rounded-xl flex items-center justify-center">
              <Play size={16} className="text-white dark:text-black fill-current animate-pulse" />
            </div>
          )}
        </div>

        <div className="flex flex-col truncate">
          <span className="text-xs font-bold truncate">{track.title}</span>
          <span className={`text-[11px] truncate ${isCurrent ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>
            {track.artist} • {track.album}
          </span>
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0 ml-2">
        <span className={`text-[11px] font-medium ${isCurrent ? 'text-neutral-300 dark:text-neutral-700' : 'text-[#75726b] dark:text-[#8a857b]'}`}>
          {formatSec(track.duration)}
        </span>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onToggleLike();
          }}
          className="p-1 hover:scale-110 transition-transform cursor-pointer"
        >
          <Heart
            size={15}
            className={track.isLiked ? 'text-red-500 fill-red-500' : isCurrent ? 'text-white dark:text-black' : 'text-[#75726b] dark:text-[#8a857b]'}
          />
        </button>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onEditTags();
          }}
          className="p-1 text-[#75726b] dark:text-[#8a857b] hover:text-black dark:hover:text-white transition-colors cursor-pointer"
          title="Editar etiquetas ID3"
        >
          <MoreVertical size={15} />
        </button>
      </div>
    </div>
  );
});

interface ArtistAvatarProps {
  artistName: string;
  avatarUrl?: string;
  isSelected: boolean;
}

const cleanArtistName = (name: string) => {
  return name.replace(/\.(mp3|flac|m4a|wav|aac|ogg|opus)$/i, '');
};

const ArtistAvatar: React.FC<ArtistAvatarProps> = React.memo(({
  artistName,
  avatarUrl,
  isSelected
}) => {
  const fallbackImages = [
    'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?q=80&w=300&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=300&auto=format&fit=crop',
    'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=300&auto=format&fit=crop'
  ];

  const imgSource = avatarUrl || fallbackImages[Math.abs(artistName.split('').reduce((a, b) => a + b.charCodeAt(0), 0)) % fallbackImages.length];

  return (
    <div
      className={`relative w-full h-full p-1 transition-all duration-300 rounded-full ${
        isSelected
          ? 'scale-105 filter drop-shadow-md ring-2 ring-black dark:ring-white'
          : 'opacity-90 hover:opacity-100'
      }`}
    >
      <div
        className="w-full h-full overflow-hidden bg-[#ded8cd] dark:bg-[#2a2824] shadow-sm flex items-center justify-center"
        style={{ clipPath: 'url(#flower-8-smooth)' }}
      >
        <img
          src={imgSource}
          alt={artistName}
          loading="lazy"
          className="w-full h-full object-cover grayscale contrast-125"
        />
      </div>
    </div>
  );
});
