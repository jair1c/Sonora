import React, { useState, useEffect } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { Moon, X, Check, PowerOff } from 'lucide-react';

interface SleepTimerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const PRESET_MINUTES = [15, 30, 45, 60, 90];

export const SleepTimerModal: React.FC<SleepTimerModalProps> = ({ isOpen, onClose }) => {
  const { sleepTimer, startSleepTimer, cancelSleepTimer } = usePlayer();
  const [selectedMinutes, setSelectedMinutes] = useState(30);
  const [stopAtTrackEnd, setStopAtTrackEnd] = useState(false);
  const [fadeOut, setFadeOut] = useState(true);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const formatRemaining = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div
      onClick={onClose}
      onTouchMove={(e) => e.stopPropagation()}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in touch-none overscroll-contain"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md bg-[#F5F2EA] dark:bg-[#161513] rounded-3xl p-6 shadow-2xl border border-[#DED8CD] dark:border-[#2a2824] flex flex-col gap-5 text-[#121212] dark:text-[#f5f2ea] overscroll-contain touch-pan-y"
      >

        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black flex items-center justify-center">
              <Moon size={16} />
            </div>
            <div>
              <h2 className="text-lg font-bold font-outfit text-black dark:text-white">Temporizador de Apagado</h2>
              <p className="text-xs text-[#75726B] dark:text-[#8a857b]">Detiene la música al dormir</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full border border-[#DED8CD] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#EAE5DA] dark:hover:bg-[#252320] transition-colors text-black dark:text-white cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* Active Timer Display */}
        {sleepTimer.active ? (
          <div className="flex flex-col items-center justify-center py-6 bg-[#EAE5DA] dark:bg-[#1a1917] rounded-2xl gap-2 border border-[#ded8cd] dark:border-[#2a2824]">
            <span className="text-xs uppercase tracking-widest text-[#75726B] dark:text-[#8a857b] font-semibold">Tiempo Restante</span>
            <span className="text-4xl font-extrabold tracking-tight text-[#121212] dark:text-white font-mono">
              {formatRemaining(sleepTimer.remainingSeconds)}
            </span>
            <p className="text-xs text-[#75726B] dark:text-[#8a857b]">
              {sleepTimer.stopAtTrackEnd ? 'Se detendrá al terminar la pista actual' : 'Desvaneciendo suavemente el volumen'}
            </p>
            <button
              onClick={cancelSleepTimer}
              className="mt-3 px-5 py-2 bg-red-600/10 text-red-600 dark:text-red-400 rounded-full font-bold text-xs hover:bg-red-600/20 transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              <PowerOff size={14} /> Cancelar Temporizador
            </button>
          </div>
        ) : (
          <>
            {/* Minute Preset Selection */}
            <div className="flex flex-col gap-2">
              <label className="text-xs font-semibold uppercase tracking-wider text-[#75726B] dark:text-[#8a857b]">Duración</label>
              <div className="grid grid-cols-5 gap-2">
                {PRESET_MINUTES.map((min) => (
                  <button
                    key={min}
                    onClick={() => setSelectedMinutes(min)}
                    className={`py-3 rounded-2xl text-xs font-bold transition-all cursor-pointer border ${
                      selectedMinutes === min
                        ? 'bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black border-transparent shadow-md scale-105'
                        : 'bg-[#EAE5DA] dark:bg-[#1a1917] text-[#4A4742] dark:text-[#a8a397] hover:bg-[#DED8CD] dark:hover:bg-[#252320] border-[#ded8cd] dark:border-[#2a2824]'
                    }`}
                  >
                    {min} min
                  </button>
                ))}
              </div>
            </div>

            {/* Smart Options */}
            <div className="flex flex-col gap-2">
              <div
                onClick={() => setStopAtTrackEnd(!stopAtTrackEnd)}
                className="flex items-center justify-between p-3.5 bg-[#EAE5DA] dark:bg-[#1a1917] rounded-2xl cursor-pointer hover:bg-[#E2DDD2] dark:hover:bg-[#252320] transition-colors border border-[#ded8cd] dark:border-[#2a2824]"
              >
                <div className="flex flex-col">
                  <span className="text-xs font-bold text-[#121212] dark:text-white">Terminar canción actual</span>
                  <span className="text-[11px] text-[#75726B] dark:text-[#8a857b]">No corta la última canción a mitad</span>
                </div>
                <div className={`w-5 h-5 rounded-full border flex items-center justify-center ${stopAtTrackEnd ? 'bg-[#121212] dark:bg-white border-[#121212] dark:border-white text-white dark:text-black' : 'border-[#B0AAA0] dark:border-[#4a463e]'}`}>
                  {stopAtTrackEnd && <Check size={12} />}
                </div>
              </div>

              <div
                onClick={() => setFadeOut(!fadeOut)}
                className="flex items-center justify-between p-3.5 bg-[#EAE5DA] dark:bg-[#1a1917] rounded-2xl cursor-pointer hover:bg-[#E2DDD2] dark:hover:bg-[#252320] transition-colors border border-[#ded8cd] dark:border-[#2a2824]"
              >
                <div className="flex flex-col">
                  <span className="text-xs font-bold text-[#121212] dark:text-white">Desvanecimiento suave</span>
                  <span className="text-[11px] text-[#75726B] dark:text-[#8a857b]">Baja el volumen en los últimos 60s</span>
                </div>
                <div className={`w-5 h-5 rounded-full border flex items-center justify-center ${fadeOut ? 'bg-[#121212] dark:bg-white border-[#121212] dark:border-white text-white dark:text-black' : 'border-[#B0AAA0] dark:border-[#4a463e]'}`}>
                  {fadeOut && <Check size={12} />}
                </div>
              </div>
            </div>

            <button
              onClick={() => {
                startSleepTimer(selectedMinutes, stopAtTrackEnd, fadeOut);
                onClose();
              }}
              className="w-full py-3 bg-black dark:bg-white text-white dark:text-black font-bold text-xs rounded-2xl cursor-pointer"
            >
              Iniciar Temporizador ({selectedMinutes} min)
            </button>
          </>
        )}
      </div>
    </div>
  );
};
