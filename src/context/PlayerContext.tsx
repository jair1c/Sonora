import React, { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { Capacitor } from '@capacitor/core';
import { sampleSongs } from '../data/musicData';
import type { Track, Artist, Playlist } from '../data/musicData';
import { loadDeviceAudioFiles } from '../services/nativeMedia';
import { NativeAudio } from '../services/nativeAudio';

export const EQ_PRESETS: Record<string, number[]> = {
  'Plano': [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
  'Refuerzo de Graves': [6, 5, 4, 2, 0, 0, 0, 1, 2, 3],
  'Acústico': [3, 2, 1, 2, 3, 3, 2, 2, 3, 2],
  'Vocal / Pop': [-1, 0, 1, 3, 4, 4, 3, 2, 1, 0],
  'Electrónica': [5, 4, 2, 0, -1, 1, 3, 4, 5, 5],
  'Rock Clásico': [4, 3, 1, -1, -1, 1, 2, 3, 4, 4]
};

export interface EqualizerSettings {
  enabled: boolean;
  preset: string;
  bassBoost: number;
  virtualizer: number;
  bands: number[];
}

export interface SleepTimerState {
  active: boolean;
  totalMinutes: number;
  remainingSeconds: number;
  stopAtTrackEnd: boolean;
  fadeOut: boolean;
}

export interface LuxStats {
  totalPlayTimeMinutes: number;
  totalTracksPlayed: number;
  topArtist: string;
}

export interface NavTabItem {
  id: string;
  label: string;
  icon: 'Library' | 'Heart' | 'Disc3' | 'SlidersHorizontal' | 'Music' | 'Folder';
  enabled: boolean;
  targetScreen: 'artists' | 'player' | 'settings';
  targetTab?: 'artistas' | 'canciones' | 'albumes' | 'listas' | 'carpetas';
}

export interface PlayerToolItem {
  id: 'speed' | 'eq' | 'timer' | 'tags' | 'volume';
  label: string;
  enabled: boolean;
}

export const DEFAULT_NAV_TABS: NavTabItem[] = [
  { id: 'biblioteca', label: 'Biblioteca', icon: 'Library', enabled: true, targetScreen: 'artists' },
  { id: 'listas', label: 'Listas ♡', icon: 'Heart', enabled: true, targetScreen: 'artists', targetTab: 'listas' },
  { id: 'reproductor', label: 'Reproductor', icon: 'Disc3', enabled: true, targetScreen: 'player' },
  { id: 'ajustes', label: 'Ajustes', icon: 'SlidersHorizontal', enabled: true, targetScreen: 'settings' }
];

export const AVAILABLE_NAV_OPTIONS: NavTabItem[] = [
  { id: 'biblioteca', label: 'Biblioteca', icon: 'Library', enabled: true, targetScreen: 'artists' },
  { id: 'canciones', label: 'Canciones', icon: 'Music', enabled: false, targetScreen: 'artists', targetTab: 'canciones' },
  { id: 'albumes', label: 'Álbumes', icon: 'Disc3', enabled: false, targetScreen: 'artists', targetTab: 'albumes' },
  { id: 'artistas', label: 'Artistas', icon: 'Disc3', enabled: false, targetScreen: 'artists', targetTab: 'artistas' },
  { id: 'listas', label: 'Listas ♡', icon: 'Heart', enabled: true, targetScreen: 'artists', targetTab: 'listas' },
  { id: 'carpetas', label: 'Carpetas', icon: 'Folder', enabled: false, targetScreen: 'artists', targetTab: 'carpetas' },
  { id: 'reproductor', label: 'Reproductor', icon: 'Disc3', enabled: true, targetScreen: 'player' },
  { id: 'ajustes', label: 'Ajustes', icon: 'SlidersHorizontal', enabled: true, targetScreen: 'settings' }
];

export const DEFAULT_PLAYER_TOOLS: PlayerToolItem[] = [
  { id: 'speed', label: 'Velocidad (1x)', enabled: true },
  { id: 'eq', label: 'Ecualizador', enabled: true },
  { id: 'timer', label: 'Temporizador', enabled: true },
  { id: 'tags', label: 'Etiquetas', enabled: true },
  { id: 'volume', label: 'Volumen', enabled: true }
];

interface PlayerContextType {
  // Audio playback state
  currentTrack: Track;
  isPlaying: boolean;
  currentTime: number;
  duration: number;
  progressPercent: number;
  volume: number;
  isShuffle: boolean;
  repeatMode: 'off' | 'all' | 'one';
  isLiked: boolean;
  playbackSpeed: number;
  crossfadeSeconds: number;
  
  // Audio actions
  togglePlay: () => void;
  playTrack: (track: Track) => void;
  nextTrack: () => void;
  prevTrack: () => void;
  seek: (time: number) => void;
  setVolume: (vol: number) => void;
  toggleShuffle: () => void;
  toggleRepeat: () => void;
  toggleLike: (trackId?: string) => void;
  setPlaybackSpeed: (speed: number) => void;
  setCrossfadeSeconds: (sec: number) => void;

  // Library & Music state
  tracks: Track[];
  artists: Artist[];
  playlists: Playlist[];
  isScanning: boolean;
  scanLocalMusic: () => Promise<void>;
  createPlaylist: (name: string) => void;
  deletePlaylist: (playlistId: string) => void;
  addTrackToPlaylist: (playlistId: string, trackId: string) => void;
  removeTrackFromPlaylist: (playlistId: string, trackId: string) => void;
  updateTrackMetadata: (trackId: string, updatedData: Partial<Track>) => void;

  // Equalizer & Audio processing
  equalizer: EqualizerSettings;
  setEqualizer: (eq: EqualizerSettings) => void;
  applyEqPreset: (presetName: string) => void;
  setBandGain: (bandIndex: number, gain: number) => void;

  // Smart Sleep Timer
  sleepTimer: SleepTimerState;
  startSleepTimer: (minutes: number, stopAtTrackEnd?: boolean, fadeOut?: boolean) => void;
  cancelSleepTimer: () => void;

  // App navigation & selection
  activeScreen: 'onboarding' | 'artists' | 'player' | 'settings';
  setActiveScreen: (screen: 'onboarding' | 'artists' | 'player' | 'settings') => void;
  previousScreen: 'artists' | 'settings';
  selectedArtistIds: string[];
  toggleArtistSelection: (artistId: string) => void;
  clearArtistSelection: () => void;
  libraryTab: 'artistas' | 'canciones' | 'albumes' | 'listas' | 'carpetas';
  setLibraryTab: (tab: 'artistas' | 'canciones' | 'albumes' | 'listas' | 'carpetas') => void;
  searchQuery: string;
  setSearchQuery: (query: string) => void;

  // Appearance & Theme Mode
  themeMode: 'system' | 'light' | 'dark';
  setThemeMode: (mode: 'system' | 'light' | 'dark') => void;

  // Custom Navigation Bar Tabs
  navTabsConfig: NavTabItem[];
  setNavTabsConfig: (tabs: NavTabItem[]) => void;
  resetNavTabsConfig: () => void;

  // Petal Roundness / Curvature (0 to 100)
  petalRoundness: number;
  setPetalRoundness: (val: number) => void;

  // Stats
  stats: LuxStats;
}

const PlayerContext = createContext<PlayerContextType | null>(null);

export const PlayerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [tracks, setTracks] = useState<Track[]>(() => {
    const cached = localStorage.getItem('luxTune_local_songs');
    if (cached) {
      try { return JSON.parse(cached); } catch (e) { console.error(e); }
    }
    return sampleSongs;
  });

  const [currentTrackIndex, setCurrentTrackIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(220);
  const [volume, setVolumeState] = useState(0.85);
  const [isShuffle, setIsShuffle] = useState(false);
  const [repeatMode, setRepeatMode] = useState<'off' | 'all' | 'one'>('off');
  const [playbackSpeed, setPlaybackSpeedState] = useState(1.0);
  const [crossfadeSeconds, setCrossfadeSecondsState] = useState<number>(() => {
    const cached = localStorage.getItem('luxTune_crossfade');
    return cached ? parseInt(cached, 10) : 2;
  });
  const [isScanning, setIsScanning] = useState(false);

  const [activeScreen, setActiveScreenState] = useState<'onboarding' | 'artists' | 'player' | 'settings'>(() => {
    return localStorage.getItem('luxTune_onboarding_done') ? 'artists' : 'onboarding';
  });

  const [previousScreen, setPreviousScreen] = useState<'artists' | 'settings'>('artists');

  const setActiveScreen = (screen: 'onboarding' | 'artists' | 'player' | 'settings') => {
    setActiveScreenState((prev) => {
      if (prev !== 'player' && screen === 'player') {
        setPreviousScreen(prev === 'settings' ? 'settings' : 'artists');
      }
      return screen;
    });
  };

  // Appearance & Theme Mode
  const [themeMode, setThemeModeState] = useState<'system' | 'light' | 'dark'>(() => {
    const cached = localStorage.getItem('luxTune_theme_mode');
    return (cached as 'system' | 'light' | 'dark') || 'system';
  });

  const setThemeMode = (mode: 'system' | 'light' | 'dark') => {
    setThemeModeState(mode);
    localStorage.setItem('luxTune_theme_mode', mode);
  };

  // Apply dark/light theme dynamically to root document
  useEffect(() => {
    const applyTheme = () => {
      const isDark =
        themeMode === 'dark' ||
        (themeMode === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

      if (isDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    };

    applyTheme();

    if (themeMode === 'system') {
      const media = window.matchMedia('(prefers-color-scheme: dark)');
      const listener = () => applyTheme();
      media.addEventListener('change', listener);
      return () => media.removeEventListener('change', listener);
    }
  }, [themeMode]);

  // Default libraryTab to 'canciones'
  const [libraryTab, setLibraryTab] = useState<'artistas' | 'canciones' | 'albumes' | 'listas' | 'carpetas'>('canciones');
  const [selectedArtistIds, setSelectedArtistIds] = useState<string[]>([]);
  const [searchQuery, setSearchQuery] = useState('');


  // Customizable Bottom Navigation Tabs
  const [navTabsConfig, setNavTabsState] = useState<NavTabItem[]>(() => {
    const cached = localStorage.getItem('luxTune_nav_tabs');
    if (cached) {
      try { return JSON.parse(cached); } catch (e) { console.error(e); }
    }
    return DEFAULT_NAV_TABS;
  });

  const setNavTabsConfig = (tabs: NavTabItem[]) => {
    setNavTabsState(tabs);
    localStorage.setItem('luxTune_nav_tabs', JSON.stringify(tabs));
  };

  const resetNavTabsConfig = () => {
    setNavTabsState(DEFAULT_NAV_TABS);
    localStorage.setItem('luxTune_nav_tabs', JSON.stringify(DEFAULT_NAV_TABS));
  };

  // Petal Roundness / Curvature (0 to 100)
  const [petalRoundness, setPetalRoundnessState] = useState<number>(() => {
    const cached = localStorage.getItem('luxTune_petal_roundness');
    return cached ? parseInt(cached, 10) : 50;
  });

  const setPetalRoundness = (val: number) => {
    setPetalRoundnessState(val);
    localStorage.setItem('luxTune_petal_roundness', val.toString());
  };


  // Local Playlists
  const [playlists, setPlaylists] = useState<Playlist[]>(() => {
    const cached = localStorage.getItem('luxTune_playlists');
    if (cached) {
      try { return JSON.parse(cached); } catch (e) { console.error(e); }
    }
    return [
      { id: 'favorites', name: 'Favoritos ♡', trackIds: [], createdAt: Date.now() },
      { id: 'chill-mix', name: 'Sesión Nocturna', trackIds: [], createdAt: Date.now() }
    ];
  });

  // Equalizer State
  const [equalizer, setEqualizer] = useState<EqualizerSettings>({
    enabled: true,
    preset: 'Plano',
    bassBoost: 35,
    virtualizer: 20,
    bands: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
  });

  // Sleep Timer State
  const [sleepTimer, setSleepTimer] = useState<SleepTimerState>({
    active: false,
    totalMinutes: 0,
    remainingSeconds: 0,
    stopAtTrackEnd: false,
    fadeOut: true
  });

  // Local Listening Stats (Functional starting from 0)
  const [stats, setStats] = useState<LuxStats>(() => {
    const cached = localStorage.getItem('luxTune_stats');
    if (cached) {
      try { return JSON.parse(cached); } catch (e) { console.error(e); }
    }
    return {
      totalPlayTimeMinutes: 0,
      totalTracksPlayed: 0,
      topArtist: ''
    };
  });

  const audioRef = useRef<HTMLAudioElement | null>(null);
  const sleepIntervalRef = useRef<number | null>(null);
  const positionSyncRef = useRef<number | null>(null);
  
  // Realtime mutable refs to avoid stale closures during auto-advance
  const currentTrackIndexRef = useRef(0);
  const tracksRef = useRef<Track[]>(tracks);
  const isShuffleRef = useRef(false);
  const repeatModeRef = useRef<'off' | 'all' | 'one'>('off');
  const playbackSpeedRef = useRef(1.0);
  const volumeRef = useRef(0.85);

  useEffect(() => { currentTrackIndexRef.current = currentTrackIndex; }, [currentTrackIndex]);
  useEffect(() => { tracksRef.current = tracks; }, [tracks]);
  useEffect(() => { isShuffleRef.current = isShuffle; }, [isShuffle]);
  useEffect(() => { repeatModeRef.current = repeatMode; }, [repeatMode]);
  useEffect(() => { playbackSpeedRef.current = playbackSpeed; }, [playbackSpeed]);
  useEffect(() => { volumeRef.current = volume; }, [volume]);

  const currentTrack = tracks[currentTrackIndex] || tracks[0];
  const isLiked = !!currentTrack?.isLiked;

  // Derive dynamic artists from indexed tracks
  const artists: Artist[] = React.useMemo(() => {
    const artistMap = new Map<string, { count: number; avatar: string; genre: string }>();

    tracks.forEach((t) => {
      const artName = t.artist || 'Artista Desconocido';
      if (!artistMap.has(artName)) {
        artistMap.set(artName, {
          count: 1,
          avatar: t.coverUrl,
          genre: t.album || 'Música Local'
        });
      } else {
        const entry = artistMap.get(artName)!;
        entry.count += 1;
      }
    });

    return Array.from(artistMap.entries()).map(([name, data], idx) => ({
      id: `artist-${idx}-${name.toLowerCase().replace(/\s+/g, '-')}`,
      name,
      genre: data.genre,
      avatarUrl: data.avatar,
      trackCount: data.count
    }));
  }, [tracks]);

  // Scan local audio files
  const scanLocalMusic = useCallback(async () => {
    setIsScanning(true);
    try {
      const scanned = await loadDeviceAudioFiles();
      if (scanned && scanned.length > 0) {
        setTracks(scanned);
        tracksRef.current = scanned;
        localStorage.setItem('luxTune_local_songs', JSON.stringify(scanned));
      }
    } catch (err) {
      console.error('Error scanning local music:', err);
    } finally {
      setIsScanning(false);
    }
  }, []);

  useEffect(() => {
    scanLocalMusic();
  }, [scanLocalMusic]);

  const setCrossfadeSeconds = useCallback((sec: number) => {
    setCrossfadeSecondsState(sec);
    localStorage.setItem('luxTune_crossfade', String(sec));
    if (Capacitor.isNativePlatform()) {
      NativeAudio.setCrossfade({ seconds: sec }).catch(() => {});
    }
  }, []);

  // Execute playback on a specific index safely
  const executePlayIndex = useCallback((targetIndex: number) => {
    const currentList = tracksRef.current;
    if (!currentList || currentList.length === 0) return;

    const safeIdx = (targetIndex + currentList.length) % currentList.length;
    currentTrackIndexRef.current = safeIdx;
    setCurrentTrackIndex(safeIdx);

    const track = currentList[safeIdx];
    setDuration(track.duration || 180);
    setCurrentTime(0);
    setIsPlaying(true);

    const isNative = Capacitor.isNativePlatform();

    if (isNative) {
      // ONLY play via native Android service to prevent double audio echo!
      NativeAudio.play({
        path: track.filePath || track.audioUrl,
        title: track.title,
        artist: track.artist,
        cover: track.coverUrl
      }).catch(console.error);

      NativeAudio.setSpeed({ speed: playbackSpeedRef.current }).catch(() => {});
      NativeAudio.setVolume({ volume: volumeRef.current }).catch(() => {});
      NativeAudio.setCrossfade({ seconds: crossfadeSeconds }).catch(() => {});
    } else {
      // Play via HTML5 Audio in web browser only
      if (audioRef.current) {
        audioRef.current.src = track.audioUrl;
        audioRef.current.playbackRate = playbackSpeedRef.current;
        audioRef.current.volume = volumeRef.current;
        audioRef.current.play().catch(() => {});
      }
    }

    if ('mediaSession' in navigator) {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: track.title,
        artist: track.artist,
        album: track.album || 'luxTune',
        artwork: track.coverUrl ? [{ src: track.coverUrl, sizes: '512x512', type: 'image/jpeg' }] : []
      });
    }

    // Increment play stats and update localStorage
    setTracks(prev => prev.map(t => t.id === track.id ? { ...t, playCount: (t.playCount || 0) + 1, lastPlayed: Date.now() } : t));
    setStats(prev => {
      const updated: LuxStats = {
        totalTracksPlayed: prev.totalTracksPlayed + 1,
        totalPlayTimeMinutes: prev.totalPlayTimeMinutes + Math.round((track.duration || 180) / 60),
        topArtist: track.artist || prev.topArtist
      };
      localStorage.setItem('luxTune_stats', JSON.stringify(updated));
      return updated;
    });
  }, [crossfadeSeconds]);

  const nextTrack = useCallback(() => {
    const list = tracksRef.current;
    if (!list || list.length === 0) return;

    if (isShuffleRef.current) {
      const randomIdx = Math.floor(Math.random() * list.length);
      executePlayIndex(randomIdx);
    } else {
      const nextIdx = (currentTrackIndexRef.current + 1) % list.length;
      executePlayIndex(nextIdx);
    }
  }, [executePlayIndex]);

  const prevTrack = useCallback(() => {
    const list = tracksRef.current;
    if (!list || list.length === 0) return;

    if (currentTime > 5) {
      seek(0);
    } else {
      const prevIdx = (currentTrackIndexRef.current - 1 + list.length) % list.length;
      executePlayIndex(prevIdx);
    }
  }, [currentTime, executePlayIndex]);

  const playTrack = useCallback((track: Track) => {
    const idx = tracksRef.current.findIndex((t) => t.id === track.id);
    executePlayIndex(idx !== -1 ? idx : 0);
  }, [executePlayIndex]);

  // Setup HTML5 Audio element fallback (ONLY used when running on non-native platform)
  useEffect(() => {
    if (Capacitor.isNativePlatform()) return;

    const audio = new Audio();
    audio.preload = 'auto';
    audioRef.current = audio;

    const handleTimeUpdate = () => {
      setCurrentTime(Math.floor(audio.currentTime));
    };

    const handleLoadedMetadata = () => {
      if (audio.duration && !isNaN(audio.duration)) {
        setDuration(Math.floor(audio.duration));
      }
    };

    const handleEnded = () => {
      if (repeatModeRef.current === 'one') {
        audio.currentTime = 0;
        audio.play().catch(() => {});
      } else {
        nextTrack();
      }
    };

    audio.addEventListener('timeupdate', handleTimeUpdate);
    audio.addEventListener('loadedmetadata', handleLoadedMetadata);
    audio.addEventListener('ended', handleEnded);

    return () => {
      audio.removeEventListener('timeupdate', handleTimeUpdate);
      audio.removeEventListener('loadedmetadata', handleLoadedMetadata);
      audio.removeEventListener('ended', handleEnded);
      audio.pause();
    };
  }, [nextTrack]);

  // Setup NativeAudio callbacks from native MusicPlayerService
  useEffect(() => {
    if (!Capacitor.isNativePlatform()) return;

    let unsubs: Array<() => void> = [];

    NativeAudio.addListener('trackEnded', () => {
      if (repeatModeRef.current === 'one') {
        seek(0);
        NativeAudio.resume().catch(() => {});
      } else {
        nextTrack();
      }
    }).then(res => unsubs.push(() => res?.remove?.()));

    NativeAudio.addListener('playStateChanged', ({ isPlaying: nativePlaying }) => {
      setIsPlaying(nativePlaying);
    }).then(res => unsubs.push(() => res?.remove?.()));

    NativeAudio.addListener('nextRequested', () => {
      nextTrack();
    }).then(res => unsubs.push(() => res?.remove?.()));

    NativeAudio.addListener('prevRequested', () => {
      prevTrack();
    }).then(res => unsubs.push(() => res?.remove?.()));

    NativeAudio.addListener('trackAutoSwapped', ({ path: newPath }) => {
      const list = tracksRef.current;
      if (!list || list.length === 0) return;
      let newIdx = list.findIndex(t => (t.filePath || t.audioUrl) === newPath);
      if (newIdx === -1) {
        newIdx = (currentTrackIndexRef.current + 1) % list.length;
      }
      currentTrackIndexRef.current = newIdx;
      setCurrentTrackIndex(newIdx);
      const track = list[newIdx];
      if (track) {
        setDuration(track.duration || 180);
        setCurrentTime(0);
        setIsPlaying(true);
        // Increment play stats
        setTracks(prev => prev.map(t => t.id === track.id ? { ...t, playCount: (t.playCount || 0) + 1, lastPlayed: Date.now() } : t));
        setStats(prev => {
          const updated: LuxStats = {
            totalTracksPlayed: prev.totalTracksPlayed + 1,
            totalPlayTimeMinutes: prev.totalPlayTimeMinutes + Math.round((track.duration || 180) / 60),
            topArtist: track.artist || prev.topArtist
          };
          localStorage.setItem('luxTune_stats', JSON.stringify(updated));
          return updated;
        });
      }
    }).then(res => unsubs.push(() => res?.remove?.()));

    return () => {
      unsubs.forEach(u => u());
    };
  }, [nextTrack, prevTrack]);

  // Continuously feed the upcoming next track into the native DJ crossfade engine
  useEffect(() => {
    if (!Capacitor.isNativePlatform() || tracks.length === 0) return;
    const list = tracks;
    const nextIdx = (currentTrackIndex + 1) % list.length;
    const nextTrackObj = list[nextIdx];
    if (nextTrackObj) {
      NativeAudio.setNextTrack({
        path: nextTrackObj.filePath || nextTrackObj.audioUrl,
        title: nextTrackObj.title,
        artist: nextTrackObj.artist,
        cover: nextTrackObj.coverUrl
      }).catch(() => {});
    }
  }, [currentTrackIndex, tracks]);


  // Native position synchronization ticker
  useEffect(() => {
    if (isPlaying && Capacitor.isNativePlatform()) {
      positionSyncRef.current = window.setInterval(async () => {
        try {
          const pos = await NativeAudio.getPosition();
          if (pos && pos.durationMs > 0) {
            setCurrentTime(Math.floor(pos.currentPositionMs / 1000));
            setDuration(Math.floor(pos.durationMs / 1000));
          }
        } catch (ignored) {}
      }, 500);
    } else {
      if (positionSyncRef.current) clearInterval(positionSyncRef.current);
    }

    return () => {
      if (positionSyncRef.current) clearInterval(positionSyncRef.current);
    };
  }, [isPlaying]);

  // Handle sleep timer countdown
  useEffect(() => {
    if (sleepTimer.active && sleepTimer.remainingSeconds > 0) {
      sleepIntervalRef.current = window.setInterval(() => {
        setSleepTimer((prev) => {
          if (prev.remainingSeconds <= 1) {
            setIsPlaying(false);
            if (Capacitor.isNativePlatform()) {
              NativeAudio.pause().catch(() => {});
            } else if (audioRef.current) {
              audioRef.current.pause();
            }
            return { ...prev, active: false, remainingSeconds: 0 };
          }
          // Gradual volume fade-out in the last 30 seconds
          if (prev.fadeOut && prev.remainingSeconds <= 30) {
            const fadedVol = Math.max(0, (prev.remainingSeconds / 30) * volume);
            if (Capacitor.isNativePlatform()) {
              NativeAudio.setVolume({ volume: fadedVol }).catch(() => {});
            } else if (audioRef.current) {
              audioRef.current.volume = fadedVol;
            }
          }
          return { ...prev, remainingSeconds: prev.remainingSeconds - 1 };
        });
      }, 1000);
    } else {
      if (sleepIntervalRef.current) clearInterval(sleepIntervalRef.current);
    }

    return () => {
      if (sleepIntervalRef.current) clearInterval(sleepIntervalRef.current);
    };
  }, [sleepTimer.active, sleepTimer.remainingSeconds, volume]);

  const startSleepTimer = useCallback((minutes: number, stopAtTrackEnd: boolean = false, fadeOut: boolean = true) => {
    setSleepTimer({
      active: true,
      totalMinutes: minutes,
      remainingSeconds: minutes * 60,
      stopAtTrackEnd,
      fadeOut
    });
  }, []);

  const cancelSleepTimer = useCallback(() => {
    setSleepTimer(prev => ({ ...prev, active: false, remainingSeconds: 0 }));
    if (Capacitor.isNativePlatform()) {
      NativeAudio.setVolume({ volume }).catch(() => {});
    } else if (audioRef.current) {
      audioRef.current.volume = volume;
    }
  }, [volume]);

  const togglePlay = useCallback(() => {
    setIsPlaying((prev) => {
      const next = !prev;
      const track = tracksRef.current[currentTrackIndexRef.current] || tracksRef.current[0];

      if (next) {
        if (Capacitor.isNativePlatform()) {
          NativeAudio.resume().catch(() => {
            if (track) {
              NativeAudio.play({
                path: track.filePath || track.audioUrl,
                title: track.title,
                artist: track.artist,
                cover: track.coverUrl
              }).catch(() => {});
            }
          });
        } else if (audioRef.current) {
          if (!audioRef.current.src || audioRef.current.src === '') {
            if (track) audioRef.current.src = track.audioUrl;
          }
          audioRef.current.play().catch(() => {});
        }
      } else {
        if (Capacitor.isNativePlatform()) {
          NativeAudio.pause().catch(() => {});
        } else if (audioRef.current) {
          audioRef.current.pause();
        }
      }
      return next;
    });
  }, []);

  const seek = useCallback((time: number) => {
    const clamped = Math.max(0, Math.min(time, duration));
    setCurrentTime(clamped);
    if (Capacitor.isNativePlatform()) {
      NativeAudio.seek({ positionMs: Math.round(clamped * 1000) }).catch(() => {});
    } else if (audioRef.current) {
      audioRef.current.currentTime = clamped;
    }
  }, [duration]);

  const setVolume = useCallback((vol: number) => {
    const clamped = Math.max(0, Math.min(vol, 1));
    setVolumeState(clamped);
    volumeRef.current = clamped;
    if (Capacitor.isNativePlatform()) {
      NativeAudio.setVolume({ volume: clamped }).catch(() => {});
    } else if (audioRef.current) {
      audioRef.current.volume = clamped;
    }
  }, []);

  const setPlaybackSpeed = useCallback((speed: number) => {
    setPlaybackSpeedState(speed);
    playbackSpeedRef.current = speed;
    if (Capacitor.isNativePlatform()) {
      NativeAudio.setSpeed({ speed }).catch(() => {});
    } else if (audioRef.current) {
      audioRef.current.playbackRate = speed;
    }
  }, []);

  const toggleShuffle = useCallback(() => {
    setIsShuffle((prev) => !prev);
  }, []);

  const toggleRepeat = useCallback(() => {
    setRepeatMode((prev) => {
      if (prev === 'off') return 'all';
      if (prev === 'all') return 'one';
      return 'off';
    });
  }, []);

  const toggleLike = useCallback((trackId?: string) => {
    const targetId = trackId || currentTrack.id;
    setTracks(prev => {
      const updated = prev.map(t => t.id === targetId ? { ...t, isLiked: !t.isLiked } : t);
      localStorage.setItem('luxTune_local_songs', JSON.stringify(updated));
      return updated;
    });
  }, [currentTrack.id]);

  const toggleArtistSelection = useCallback((artistId: string) => {
    setSelectedArtistIds((prev) =>
      prev.includes(artistId)
        ? prev.filter((id) => id !== artistId)
        : [...prev, artistId]
    );
  }, []);

  const clearArtistSelection = useCallback(() => {
    setSelectedArtistIds([]);
  }, []);

  // Equalizer presets & bands
  const applyEqPreset = useCallback((presetName: string) => {
    const presetBands = EQ_PRESETS[presetName] || [0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
    setEqualizer(prev => ({
      ...prev,
      preset: presetName,
      bands: [...presetBands]
    }));
  }, []);

  const setBandGain = useCallback((bandIndex: number, gain: number) => {
    setEqualizer(prev => {
      const newBands = [...prev.bands];
      newBands[bandIndex] = gain;
      return { ...prev, preset: 'Personalizado', bands: newBands };
    });
  }, []);

  // Playlists management
  const createPlaylist = useCallback((name: string) => {
    const newPl: Playlist = {
      id: 'pl-' + Date.now(),
      name,
      trackIds: [],
      createdAt: Date.now()
    };
    setPlaylists(prev => {
      const updated = [...prev, newPl];
      localStorage.setItem('luxTune_playlists', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const deletePlaylist = useCallback((playlistId: string) => {
    if (playlistId === 'favorites') return;
    setPlaylists(prev => {
      const updated = prev.filter(pl => pl.id !== playlistId);
      localStorage.setItem('luxTune_playlists', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const addTrackToPlaylist = useCallback((playlistId: string, trackId: string) => {
    setPlaylists(prev => {
      const updated = prev.map(pl => {
        if (pl.id === playlistId && !pl.trackIds.includes(trackId)) {
          return { ...pl, trackIds: [...pl.trackIds, trackId] };
        }
        return pl;
      });
      localStorage.setItem('luxTune_playlists', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const removeTrackFromPlaylist = useCallback((playlistId: string, trackId: string) => {
    setPlaylists(prev => {
      const updated = prev.map(pl => {
        if (pl.id === playlistId) {
          return { ...pl, trackIds: pl.trackIds.filter(id => id !== trackId) };
        }
        return pl;
      });
      localStorage.setItem('luxTune_playlists', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const updateTrackMetadata = useCallback((trackId: string, updatedData: Partial<Track>) => {
    setTracks(prev => {
      const updated = prev.map(t => t.id === trackId ? { ...t, ...updatedData } : t);
      localStorage.setItem('luxTune_local_songs', JSON.stringify(updated));
      return updated;
    });
  }, []);

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0;

  return (
    <PlayerContext.Provider
      value={{
        currentTrack,
        isPlaying,
        currentTime,
        duration,
        progressPercent,
        volume,
        isShuffle,
        repeatMode,
        isLiked,
        playbackSpeed,
        crossfadeSeconds,
        togglePlay,
        playTrack,
        nextTrack,
        prevTrack,
        seek,
        setVolume,
        toggleShuffle,
        toggleRepeat,
        toggleLike,
        setPlaybackSpeed,
        setCrossfadeSeconds,
        tracks,
        artists,
        playlists,
        isScanning,
        scanLocalMusic,
        createPlaylist,
        deletePlaylist,
        addTrackToPlaylist,
        removeTrackFromPlaylist,
        updateTrackMetadata,
        equalizer,
        setEqualizer,
        applyEqPreset,
        setBandGain,
        sleepTimer,
        startSleepTimer,
        cancelSleepTimer,
        activeScreen,
        setActiveScreen,
        previousScreen,
        themeMode,
        setThemeMode,
        selectedArtistIds,
        toggleArtistSelection,
        clearArtistSelection,
        libraryTab,
        setLibraryTab,
        searchQuery,
        setSearchQuery,
        navTabsConfig,
        setNavTabsConfig,
        resetNavTabsConfig,
        petalRoundness,
        setPetalRoundness,
        stats
      }}


    >
      {children}
    </PlayerContext.Provider>
  );
};

export const usePlayer = () => {
  const context = useContext(PlayerContext);
  if (!context) {
    throw new Error('usePlayer must be used within a PlayerProvider');
  }
  return context;
};
