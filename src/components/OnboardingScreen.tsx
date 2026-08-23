import React from 'react';
import { BackgroundCurves } from './OrganicShapes';
import { usePlayer } from '../context/PlayerContext';
import { Sparkles, Music2 } from 'lucide-react';

export const OnboardingScreen: React.FC = () => {
  const { setActiveScreen, scanLocalMusic, isScanning } = usePlayer();

  const handleStart = async () => {
    localStorage.setItem('luxTune_onboarding_done', 'true');
    scanLocalMusic();
    setActiveScreen('artists');
  };

  return (
    <div className="relative w-full h-screen max-h-screen bg-[#f5f2ea] flex flex-col justify-between select-none text-[#121212] overflow-hidden pt-3 pb-6 px-6">
      {/* Decorative Organic Background Lines */}
      <BackgroundCurves />

      {/* Top Header */}
      <div className="relative z-10 pt-1 flex items-center justify-between shrink-0">
        {/* Sonora Logo */}
        <div className="flex items-center tracking-tight">
          <span className="font-extrabold text-[24px] tracking-tight font-outfit text-black">Sonora</span>
        </div>


        {/* Skip button */}
        <button
          onClick={handleStart}
          className="text-xs text-[#2c2b29] font-semibold hover:text-black transition-colors cursor-pointer py-1 px-2 rounded-full border border-[#ded8cd]"
        >
          Saltar
        </button>
      </div>

      {/* Artistic Organic Bubbles Section */}
      <div className="relative z-10 flex-1 w-full my-auto flex items-center justify-center min-h-[220px] max-h-[300px]">
        {/* Top Right Starburst Artist */}
        <div className="absolute right-2 top-0 w-[120px] h-[120px] sm:w-[140px] sm:h-[140px] flex items-center justify-center animate-pulse-subtle">
          <div className="absolute inset-0 scale-110 pointer-events-none">
            <svg viewBox="0 0 100 100" className="w-full h-full stroke-[#aba496] stroke-[1] fill-none">
              <path d="M 50 3 C 54 3, 59 10, 64 7 C 70 4, 75 12, 80 11 C 87 10, 89 20, 94 22 C 99 24, 98 35, 100 40 C 102 46, 97 55, 98 61 C 98 68, 93 76, 90 82 C 86 88, 78 91, 73 95 C 67 98, 60 95, 54 99 C 47 101, 41 96, 35 98 C 29 99, 24 92, 19 90 C 13 88, 9 79, 6 73 C 3 66, 8 57, 6 50 C 4 43, 8 34, 9 27 C 11 19, 19 15, 24 10 C 30 5, 37 8, 43 4 C 46 2, 48 3, 50 3 Z" />
            </svg>
          </div>
          <div
            className="w-full h-full shadow-lg overflow-hidden bg-[#e0ded8]"
            style={{ clipPath: 'url(#scallop-star-12)' }}
          >
            <img
              src="https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?q=80&w=400&auto=format&fit=crop"
              alt=""
              className="w-full h-full object-cover grayscale contrast-125"
            />
          </div>
        </div>

        {/* Middle Left Cloud/Scalloped Artist */}
        <div className="absolute left-2 top-10 w-[115px] h-[115px] sm:w-[135px] sm:h-[135px] flex items-center justify-center">
          <div className="absolute inset-0 scale-105 pointer-events-none">
            <svg viewBox="0 0 100 100" className="w-full h-full stroke-[#aba496] stroke-[1] fill-none">
              <path d="M 50 5 C 65 2, 82 8, 92 22 C 101 37, 98 58, 96 74 C 93 90, 78 101, 60 99 C 42 98, 27 99, 14 88 C 1 76, -2 54, 3 36 C 8 19, 25 8, 42 6 Z" />
            </svg>
          </div>
          <div
            className="w-full h-full shadow-lg overflow-hidden bg-[#e0ded8]"
            style={{ clipPath: 'url(#scallop-cloud)' }}
          >
            <img
              src="https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=400&auto=format&fit=crop"
              alt=""
              className="w-full h-full object-cover grayscale contrast-125"
            />
          </div>
        </div>

        {/* Bottom Right Circular Scallop Artist */}
        <div className="absolute right-3 bottom-0 w-[115px] h-[115px] sm:w-[135px] sm:h-[135px] flex items-center justify-center">
          <div className="absolute inset-0 scale-105 pointer-events-none">
            <svg viewBox="0 0 100 100" className="w-full h-full stroke-[#aba496] stroke-[1] fill-none">
              <circle cx="50" cy="50" r="48" />
            </svg>
          </div>
          <div
            className="w-full h-full rounded-full shadow-lg overflow-hidden border border-[#d8d3c9]"
            style={{ clipPath: 'url(#flower-8-smooth)' }}
          >
            <img
              src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=400&auto=format&fit=crop"
              alt=""
              className="w-full h-full object-cover grayscale contrast-125 sepia-[0.35]"
            />
          </div>
        </div>
      </div>

      {/* Bottom Content & CTA */}
      <div className="relative z-10 shrink-0">
        {/* Headline */}
        <h1 className="text-[28px] sm:text-[32px] leading-[1.15] text-[#121212] tracking-tight mb-2">
          <span className="font-extrabold font-outfit text-black">Eleva </span>
          <span className="font-light text-[#3a3937]">Cada</span>
          <br />
          <span className="font-light text-[#3a3937]">Momento con la </span>
          <span className="font-extrabold font-outfit text-black">Música</span>
        </h1>

        {/* Subtitle */}
        <p className="text-[12px] leading-relaxed text-[#716e68] font-normal max-w-[320px] mb-4">
          Sumérgete en un mundo donde cada ritmo realza tu estado de ánimo y cada melodía cuenta tu historia local.
        </p>

        {/* Comenzar Button */}
        <button
          onClick={handleStart}
          disabled={isScanning}
          className="w-full py-3.5 bg-black text-white font-semibold text-sm rounded-full hover:bg-neutral-900 active:scale-[0.98] transition-all shadow-md cursor-pointer flex items-center justify-center gap-2"
        >
          <Music2 size={16} />
          {isScanning ? 'Escaneando Tu Música...' : 'Explorar Mi Música'}
        </button>

        {/* Offline Audio Badge */}
        <div className="mt-2.5 text-center text-[11px] text-[#716e68] flex items-center justify-center gap-1">
          <Sparkles size={12} className="text-[#121212]" />
          <span>Reproducción 100% Offline de Alta Fidelidad</span>
        </div>
      </div>
    </div>
  );
};
