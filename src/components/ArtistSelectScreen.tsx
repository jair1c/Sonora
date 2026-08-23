import React, { useState } from 'react';
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
  X
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
    navTabsConfig
  } = usePlayer();

  const [showEqModal, setShowEqModal] = useState(false);
  const [showSleepModal, setShowSleepModal] = useState(false);
  const [showStatsModal, setShowStatsModal] = useState(false);
  const [showTagModal, setShowTagModal] = useState(false);
  const [selectedTagTrack, setSelectedTagTrack] = useState<Track | null>(null);

  const [showNewPlInput, setShowNewPlInput] = useState(false);
  const [newPlaylistName, setNewPlaylistName] = useState('');
  const [selectedPlaylistForManage, setSelectedPlaylistForManage] = useState<Playlist | null>(null);
  const [playlistAlert, setPlaylistAlert] = useState<string | null>(null);
  const [playlistToDelete, setPlaylistToDelete] = useState<Playlist | null>(null);

  React.useEffect(() => {
    if (selectedPlaylistForManage || playlistToDelete) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [selectedPlaylistForManage, playlistToDelete]);


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

  const filteredTracks = tracks.filter(
    (t) =>
      t.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      t.artist.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (t.album && t.album.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  // Group tracks by album
  const albumsMap = React.useMemo(() => {
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

  // Group tracks by folder path
  const foldersMap = React.useMemo(() => {
    const map = new Map<string, { folderName: string; count: number; tracks: Track[] }>();
    tracks.forEach((t) => {
      const parts = t.filePath ? t.filePath.split('/') : [];
      const folderName = parts.length > 1 ? parts[parts.length - 2] : 'Música Local';
      if (!map.has(folderName)) {
        map.set(folderName, { folderName, count: 1, tracks: [t] });
      } else {
        const entry = map.get(folderName)!;
        entry.count += 1;
        entry.tracks.push(t);
      }
    });
    return Array.from(map.values());
  }, [tracks]);

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
            title="Estadísticas luxStats"
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
          <div>
            <h1 className="text-[28px] sm:text-[32px] leading-tight font-extrabold font-outfit text-black dark:text-white tracking-tight">
              {libraryTab === 'artistas' && 'Elige tus Artistas'}
              {libraryTab === 'canciones' && 'Todas las Canciones'}
              {libraryTab === 'albumes' && 'Tus Álbumes'}
              {libraryTab === 'listas' && 'Listas de Reproducción'}
              {libraryTab === 'carpetas' && 'Explorador de Carpetas'}
            </h1>
            <p className="text-xs text-[#716e68] dark:text-[#918c81] mt-0.5">
              {tracks.length} canciones encontradas en el almacenamiento
            </p>
          </div>

          <button
            onClick={scanLocalMusic}
            disabled={isScanning}
            className="flex items-center gap-1 text-[11px] font-bold px-3 py-1.5 bg-[#eae5da] dark:bg-[#1a1917] hover:bg-[#ded8cd] dark:hover:bg-[#252320] rounded-full border border-[#ded8cd] dark:border-[#2a2824] transition-all text-[#121212] dark:text-[#f5f2ea] cursor-pointer"
          >
            <RefreshCw size={12} className={isScanning ? 'animate-spin' : ''} />
            <span>{isScanning ? 'Buscando...' : 'Escanear'}</span>
          </button>
        </div>

        {/* Search Bar */}
        <div className="mt-3 relative flex items-center">
          <Search size={15} className="absolute left-3.5 text-[#75726b] dark:text-[#7d7970] pointer-events-none" />
          <input
            type="text"
            placeholder="Buscar por artista, canción o álbum..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-[#eae5da] dark:bg-[#1a1917] border border-[#ded8cd] dark:border-[#2a2824] rounded-full py-2 pl-9 pr-4 text-xs text-[#121212] dark:text-[#f5f2ea] placeholder-[#8f8b83] dark:placeholder-[#6b675e] outline-none focus:border-black dark:focus:border-white transition-colors"
          />
        </div>

        {/* Multi-Tab Selector Bar */}
        <div className="flex items-center gap-2 mt-3 overflow-x-auto no-scrollbar pb-1">
          {[
            { id: 'artistas', label: 'Artistas', icon: <Disc3 size={13} /> },
            { id: 'canciones', label: 'Canciones', icon: <Music size={13} /> },
            { id: 'albumes', label: 'Álbumes', icon: <Disc3 size={13} /> },
            ...(!navTabsConfig.some(t => t.id === 'listas' || t.targetTab === 'listas')
              ? [{ id: 'listas', label: 'Listas ♡', icon: <Heart size={13} /> }]
              : []),
            { id: 'carpetas', label: 'Carpetas', icon: <Folder size={13} /> }
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setLibraryTab(tab.id as any)}
              className={`flex items-center gap-1 px-3.5 py-1.5 rounded-full text-xs font-bold transition-all shrink-0 cursor-pointer ${
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

        {/* TAB 2: TODAS LAS CANCIONES */}
        {libraryTab === 'canciones' && (
          <div className="flex flex-col gap-2 py-1">
            {filteredTracks.map((track) => (
              <SongRowItem
                key={track.id}
                track={track}
                isCurrent={currentTrack.id === track.id}
                isPlaying={isPlaying && currentTrack.id === track.id}
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
            ))}
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

        {/* TAB 4: LISTAS (Playlists & Favorites) */}
        {libraryTab === 'listas' && (
          <div className="flex flex-col gap-3 py-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-[#75726b] dark:text-[#8a857b]">Tus Listas de Reproducción</span>
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

            {/* List of Playlists */}
            {playlists.map((pl) => {
              const isFav = pl.id === 'favorites';
              const plTracks = isFav
                ? tracks.filter((t) => t.isLiked)
                : tracks.filter((t) => pl.trackIds.includes(t.id));

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
                    } else if (isFav) {
                      setPlaylistAlert('Aún no tienes canciones favoritas. Pulsa el corazón ♡ en cualquier canción para añadirla aquí.');
                    } else {
                      setSelectedPlaylistForManage(pl);
                    }
                  }}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    if (!isFav) {
                      setPlaylistToDelete(pl);
                    }
                  }}
                  className="flex items-center justify-between p-3.5 bg-[#eae5da] dark:bg-[#1a1917] rounded-2xl hover:bg-[#ded8cd] dark:hover:bg-[#252320] active:scale-[0.99] transition-all cursor-pointer border border-[#ded8cd] dark:border-[#2a2824]"
                >
                  <div className="flex items-center gap-3 truncate">
                    <div className={`w-10 h-10 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 ${isFav ? 'bg-red-500 text-white' : 'bg-black dark:bg-white text-[#f5f2ea] dark:text-black'}`}>
                      {isFav ? <Heart size={18} className="fill-white" /> : pl.name.slice(0, 1).toUpperCase()}
                    </div>
                    <div className="flex flex-col truncate">
                      <span className="text-xs font-bold text-[#121212] dark:text-[#f5f2ea] truncate">{pl.name}</span>
                      <span className="text-[11px] text-[#75726b] dark:text-[#8a857b]">
                        {plTracks.length} {plTracks.length === 1 ? 'canción' : 'canciones'}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 shrink-0 ml-2">
                    {/* Manage / Add songs button */}
                    {!isFav && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedPlaylistForManage(pl);
                        }}
                        className="p-1.5 text-xs text-[#75726b] dark:text-[#8a857b] hover:text-black dark:hover:text-white transition-colors cursor-pointer"
                        title="Gestionar canciones"
                      >
                        <Plus size={16} />
                      </button>
                    )}

                    {/* Play button */}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        if (plTracks.length > 0) {
                          if (currentTrack.id === plTracks[0].id) {
                            if (!isPlaying) togglePlay();
                          } else {
                            playTrack(plTracks[0]);
                          }
                          setActiveScreen('player');
                        } else if (isFav) {
                          setPlaylistAlert('Añade canciones a Favoritos pulsando el icono del corazón ♡ en cualquier canción.');
                        } else {
                          setSelectedPlaylistForManage(pl);
                        }
                      }}
                      className="p-1.5 bg-black dark:bg-white text-white dark:text-black rounded-full hover:scale-105 transition-transform cursor-pointer shadow-sm"
                    >
                      <Play size={12} className="fill-current ml-0.5" />
                    </button>

                    {/* Delete button (except favorites) */}
                    {!isFav && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setPlaylistToDelete(pl);
                        }}
                        className="p-1.5 text-[#75726b] dark:text-[#8a857b] hover:text-red-500 transition-colors cursor-pointer"
                        title="Eliminar lista"
                      >
                        <Trash2 size={15} />
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* TAB 5: CARPETAS */}
        {libraryTab === 'carpetas' && (
          <div className="flex flex-col gap-2 py-1">
            {foldersMap.map((folder, i) => (
              <div
                key={i}
                onClick={() => {
                  if (folder.tracks.length > 0) {
                    if (currentTrack.id === folder.tracks[0].id) {
                      if (!isPlaying) togglePlay();
                    } else {
                      playTrack(folder.tracks[0]);
                    }
                    setActiveScreen('player');
                  }
                }}
                className="flex items-center justify-between p-3.5 bg-[#eae5da] dark:bg-[#1a1917] rounded-2xl hover:bg-[#ded8cd] dark:hover:bg-[#252320] border border-[#ded8cd] dark:border-[#2a2824] transition-all cursor-pointer"
              >
                <div className="flex items-center gap-3 truncate">
                  <div className="w-10 h-10 rounded-xl bg-[#ded8cd] dark:bg-[#2a2824] flex items-center justify-center text-[#121212] dark:text-[#f5f2ea]">
                    <Folder size={18} />
                  </div>
                  <div className="flex flex-col truncate">
                    <span className="text-xs font-bold truncate text-black dark:text-white">{folder.folderName}</span>
                    <span className="text-[11px] text-[#75726b] dark:text-[#8a857b]">{folder.count} archivos de audio</span>
                  </div>
                </div>
                <Play size={16} className="text-[#75726b] dark:text-[#8a857b] shrink-0 ml-2" />
              </div>
            ))}
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
                    {inPl ? <Check size={16} className="text-white dark:text-black" /> : <Plus size={16} className="text-[#75726b] dark:text-[#8a857b]" />}
                  </div>
                );
              })}
            </div>

            <button
              onClick={() => setSelectedPlaylistForManage(null)}
              className="w-full py-3 bg-black dark:bg-white text-white dark:text-black font-bold text-xs rounded-2xl cursor-pointer"
            >
              Listo
            </button>
          </div>
        </div>
      )}

      {/* Friendly Alert Toast */}
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
