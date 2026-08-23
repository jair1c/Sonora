import { registerPlugin } from '@capacitor/core';
import type { Track } from '../data/musicData';
import { sampleSongs } from '../data/musicData';

export interface MediaStorePluginInterface {
  checkPermissions(): Promise<{ granted: boolean }>;
  requestMusicPermissions(): Promise<{ granted?: boolean; requested?: boolean }>;
  getLocalAudioFiles(): Promise<{ songs: any[]; count: number }>;
}

const MediaStorePlugin = registerPlugin<MediaStorePluginInterface>('MediaStorePlugin');

/**
 * Scan device audio files using Android MediaStore plugin or local cache/fallbacks
 */
export async function loadDeviceAudioFiles(): Promise<Track[]> {
  try {
    const permStatus = await MediaStorePlugin.checkPermissions();
    if (!permStatus.granted) {
      const req = await MediaStorePlugin.requestMusicPermissions();
      if (!req.granted && !req.requested) {
        console.warn('Storage permission not granted, using sample offline library');
        return getStoredOrSampleSongs();
      }
    }

    const result = await MediaStorePlugin.getLocalAudioFiles();
    if (result && result.songs && result.songs.length > 0) {
      const mappedSongs: Track[] = result.songs.map((s) => ({
        id: s.id,
        title: s.title,
        artist: s.artist,
        album: s.album,
        duration: s.duration || 180,
        coverUrl: s.coverUrl || 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?q=80&w=600&auto=format&fit=crop',
        audioUrl: s.audioUrl,
        filePath: s.filePath,
        size: s.size,
        year: s.year,
        isLiked: false,
        lyrics: parseLrcString(s.lyrics, s.duration || 180)
      }));

      // Cache locally in localStorage for fast immediate reload
      localStorage.setItem('luxTune_local_songs', JSON.stringify(mappedSongs));
      return mappedSongs;
    }
  } catch (err) {
    console.warn('Native MediaStorePlugin not available. Using stored songs.', err);
  }

  return getStoredOrSampleSongs();
}

function getStoredOrSampleSongs(): Track[] {
  const cached = localStorage.getItem('luxTune_local_songs');
  if (cached) {
    try {
      return JSON.parse(cached);
    } catch (e) {
      console.error(e);
    }
  }
  return sampleSongs;
}

/**
 * Parse an .LRC or embedded lyrics string into structured timestamps
 */
export function parseLrcString(lrcContent?: string, duration: number = 180): Array<{ time: number; text: string }> | undefined {
  if (!lrcContent || !lrcContent.trim()) return undefined;

  const rawLines = lrcContent.split('\n').map((l) => l.trim()).filter(Boolean);
  const result: Array<{ time: number; text: string }> = [];
  const regex = /\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)/;

  let hasTimestamps = false;
  for (const line of rawLines) {
    const match = regex.exec(line);
    if (match) {
      hasTimestamps = true;
      const minutes = parseInt(match[1], 10);
      const seconds = parseInt(match[2], 10);
      const millis = match[3] ? parseInt(match[3].padEnd(3, '0').slice(0, 3), 10) : 0;
      const totalSeconds = minutes * 60 + seconds + millis / 1000;
      const text = match[4].trim();
      if (text) {
        result.push({ time: totalSeconds, text });
      }
    }
  }

  if (hasTimestamps && result.length > 0) {
    return result.sort((a, b) => a.time - b.time);
  }

  // Plain unsynced / embedded FLAC lyrics fallback
  if (rawLines.length > 0) {
    const step = Math.max(3, (duration || 180) / rawLines.length);
    return rawLines.map((text, idx) => ({
      time: Math.round(idx * step),
      text
    }));
  }

  return undefined;
}
