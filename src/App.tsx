import React from 'react';
import { PlayerProvider, usePlayer } from './context/PlayerContext';
import { OrganicClipDefs } from './components/OrganicShapes';
import { OnboardingScreen } from './components/OnboardingScreen';
import { ArtistSelectScreen } from './components/ArtistSelectScreen';
import { PlayerScreen } from './components/PlayerScreen';
import { ToolsScreen } from './components/ToolsScreen';
import { SongCover } from './components/SongCover';
import { Library, Disc3, Heart, SlidersHorizontal, Music, Folder, Play, Pause } from 'lucide-react';

const renderNavIcon = (iconName: string, isPlaying: boolean, isActive: boolean) => {
  switch (iconName) {
    case 'Library':
      return <Library className="w-4 h-4" />;
    case 'Heart':
      return <Heart className={`w-4 h-4 ${isActive ? 'fill-current' : ''}`} />;
    case 'Disc3':
      return <Disc3 className={`w-4 h-4 ${isPlaying ? 'animate-spin-slow' : ''}`} />;
    case 'SlidersHorizontal':
      return <SlidersHorizontal className="w-4 h-4" />;
    case 'Music':
      return <Music className="w-4 h-4" />;
    case 'Folder':
      return <Folder className="w-4 h-4" />;
    default:
      return <Disc3 className="w-4 h-4" />;
  }
};

const MobileApp: React.FC = () => {
  const {
    activeScreen,
    setActiveScreen,
    libraryTab,
    setLibraryTab,
    isPlaying,
    togglePlay,
    currentTrack,
    navTabsConfig
  } = usePlayer();

  return (
    <div className="w-full h-full min-h-screen bg-[#f5f2ea] dark:bg-[#0f0e0d] text-[#121212] dark:text-[#f5f2ea] flex flex-col justify-between font-sans relative overflow-x-hidden selection:bg-black selection:text-white transition-colors duration-300">
      {/* SVG Clip Path Definitions */}
      <OrganicClipDefs />

      {/* Main Single Screen Display */}
      <main className="flex-1 w-full h-full pb-16">
        {activeScreen === 'onboarding' ? (
          <OnboardingScreen />
        ) : (
          <>
            <div className={activeScreen === 'artists' ? 'block' : 'hidden'}>
              <ArtistSelectScreen />
            </div>
            <div className={activeScreen === 'settings' ? 'block' : 'hidden'}>
              <ToolsScreen />
            </div>
          </>
        )}
      </main>

      {/* Hardware-Accelerated 60fps Fullscreen Player Slide-Over */}
      <div
        className={`fixed inset-0 z-40 transition-transform duration-300 ease-[cubic-bezier(0.32,0.72,0,1)] will-change-transform ${
          activeScreen === 'player' ? 'translate-y-0 pointer-events-auto' : 'translate-y-full pointer-events-none'
        }`}
      >
        <PlayerScreen />
      </div>

      {/* Persistent Mobile Dynamic Bottom Navigation Bar */}
      {activeScreen !== 'onboarding' && activeScreen !== 'player' && (
        <nav className="fixed bottom-0 left-0 right-0 z-30 bg-[#f5f2ea]/95 dark:bg-[#0f0e0d]/95 backdrop-blur-md border-t border-[#ded9cd] dark:border-[#262420] px-3 py-2 flex items-center justify-around max-w-md mx-auto transition-colors duration-300">
          {navTabsConfig.map((tab) => {
            const isActive = tab.targetScreen === activeScreen && (!tab.targetTab || tab.targetTab === libraryTab);

            return (
              <button
                key={tab.id}
                onClick={() => {
                  if (tab.targetTab) {
                    setLibraryTab(tab.targetTab);
                  } else if (tab.targetScreen === 'artists') {
                    if (libraryTab === 'listas') setLibraryTab('canciones');
                  }
                  setActiveScreen(tab.targetScreen);
                }}
                className={`flex flex-col items-center gap-1 transition-all cursor-pointer py-1 px-2.5 rounded-xl ${
                  isActive
                    ? 'text-black dark:text-white font-bold scale-105'
                    : 'text-[#87837a] dark:text-[#78746c] hover:text-black dark:hover:text-white font-medium'
                }`}
              >
                {renderNavIcon(tab.icon, isPlaying, isActive)}
                <span className="text-[10px] tracking-tight">{tab.label}</span>
              </button>
            );
          })}
        </nav>
      )}

      {/* Mini Player Bar: Stays visible when paused, shows correct Play/Pause state */}
      {activeScreen !== 'player' && activeScreen !== 'onboarding' && currentTrack && currentTrack.id && (
        <div
          onClick={() => setActiveScreen('player')}
          className="fixed bottom-16 left-3 right-3 max-w-md mx-auto bg-black dark:bg-[#1c1a17] text-white p-2.5 px-4 rounded-2xl shadow-xl flex items-center justify-between border border-neutral-800 dark:border-[#33302b] cursor-pointer z-30 animate-in slide-in-from-bottom-2 duration-200"
        >
          <div className="flex items-center gap-3 overflow-hidden">
            <SongCover
              src={currentTrack.coverUrl}
              title={currentTrack.title}
              artist={currentTrack.artist}
              shape="scallop"
              className="w-9 h-9 shrink-0"
            />
            <div className="overflow-hidden">
              <p className="text-xs font-bold font-outfit uppercase truncate text-white">
                {currentTrack.title}
              </p>
              <p className="text-[10px] text-neutral-400 truncate">
                {currentTrack.artist}
              </p>
            </div>
          </div>

          <button
            onClick={(e) => {
              e.stopPropagation();
              togglePlay();
            }}
            className="w-8 h-8 rounded-full bg-white dark:bg-[#f5f2ea] text-black flex items-center justify-center cursor-pointer hover:scale-105 active:scale-95 transition-all ml-2 shrink-0 shadow-sm"
            title={isPlaying ? 'Pausa' : 'Reanudar'}
          >
            {isPlaying ? (
              <Pause className="w-4 h-4 fill-black text-black" />
            ) : (
              <Play className="w-4 h-4 fill-black text-black ml-0.5" />
            )}
          </button>
        </div>
      )}
    </div>
  );
};

export default function App() {
  return (
    <PlayerProvider>
      <MobileApp />
    </PlayerProvider>
  );
}
