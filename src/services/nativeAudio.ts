import { registerPlugin } from '@capacitor/core';

interface NativeAudioPluginInterface {
  play(options: { path: string; title: string; artist: string; cover: string }): Promise<{ success: boolean }>;
  pause(): Promise<{ success: boolean }>;
  resume(): Promise<{ success: boolean }>;
  seek(options: { positionMs: number }): Promise<{ success: boolean }>;
  setSpeed(options: { speed: number }): Promise<{ success: boolean }>;
  setVolume(options: { volume: number }): Promise<{ success: boolean }>;
  setCrossfade(options: { seconds: number }): Promise<{ success: boolean }>;
  setNextTrack(options: { path: string; title: string; artist: string; cover: string }): Promise<{ success: boolean }>;
  getPosition(): Promise<{ currentPositionMs: number; durationMs: number; isPlaying: boolean }>;

  addListener(eventName: 'trackEnded', listenerFunc: () => void): Promise<any>;
  addListener(eventName: 'playStateChanged', listenerFunc: (data: { isPlaying: boolean }) => void): Promise<any>;
  addListener(eventName: 'nextRequested', listenerFunc: () => void): Promise<any>;
  addListener(eventName: 'prevRequested', listenerFunc: () => void): Promise<any>;
  addListener(eventName: 'trackAutoSwapped', listenerFunc: (data: { path: string }) => void): Promise<any>;

}

const NativeAudio = registerPlugin<NativeAudioPluginInterface>('NativeAudioPlugin');

export { NativeAudio };
