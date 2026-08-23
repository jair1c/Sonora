import React, { useEffect } from 'react';
import { usePlayer, EQ_PRESETS } from '../context/PlayerContext';
import { Sliders, X } from 'lucide-react';

interface EqualizerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const FREQUENCIES = ['31Hz', '62Hz', '125Hz', '250Hz', '500Hz', '1kHz', '2kHz', '4kHz', '8kHz', '16kHz'];

export const EqualizerModal: React.FC<EqualizerModalProps> = ({ isOpen, onClose }) => {
  const { equalizer, setEqualizer, applyEqPreset, setBandGain } = usePlayer();

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <div
      onClick={onClose}
      onTouchMove={(e) => e.stopPropagation()}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in touch-none overscroll-contain"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md bg-[#F5F2EA] dark:bg-[#161513] rounded-3xl p-6 shadow-2xl border border-[#DED8CD] dark:border-[#2a2824] flex flex-col gap-5 text-[#121212] dark:text-[#f5f2ea] max-h-[90vh] overflow-y-auto overscroll-contain touch-pan-y"
      >

        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black flex items-center justify-center">
              <Sliders size={16} />
            </div>
            <div>
              <h2 className="text-lg font-bold font-outfit text-black dark:text-white">Ecualizador Sonora</h2>
              <p className="text-xs text-[#75726B] dark:text-[#8a857b]">Audio de alta fidelidad 10 bandas</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full border border-[#DED8CD] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#EAE5DA] dark:hover:bg-[#252320] transition-colors text-black dark:text-white cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* Enable / Disable Toggle */}
        <div className="flex items-center justify-between bg-[#EAE5DA] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824]">
          <span className="text-sm font-semibold">Activar Ecualizador</span>
          <button
            onClick={() => setEqualizer({ ...equalizer, enabled: !equalizer.enabled })}
            className={`w-12 h-6 rounded-full transition-colors relative p-0.5 ${equalizer.enabled ? 'bg-[#121212] dark:bg-white' : 'bg-[#C9C3B6] dark:bg-[#33302b]'}`}
          >
            <div className={`w-5 h-5 rounded-full bg-[#F5F2EA] dark:bg-[#0f0e0d] transition-transform ${equalizer.enabled ? 'translate-x-6' : 'translate-x-0'}`} />
          </button>
        </div>

        {/* Presets Horizontal Scroll */}
        <div className="flex flex-col gap-2">
          <label className="text-xs font-semibold uppercase tracking-wider text-[#75726B] dark:text-[#8a857b]">Perfiles Acústicos</label>
          <div className="flex gap-2 overflow-x-auto pb-1 no-scrollbar">
            {Object.keys(EQ_PRESETS).map((presetName) => (
              <button
                key={presetName}
                onClick={() => applyEqPreset(presetName)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all cursor-pointer border ${
                  equalizer.preset === presetName
                    ? 'bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black border-transparent shadow-sm'
                    : 'bg-[#EAE5DA] dark:bg-[#1a1917] text-[#4A4742] dark:text-[#a8a397] hover:bg-[#DED8CD] dark:hover:bg-[#252320] border-[#ded8cd] dark:border-[#2a2824]'
                }`}
              >
                {presetName}
              </button>
            ))}
          </div>
        </div>

        {/* 10 Band Gain Sliders */}
        <div className="flex flex-col gap-2">
          <div className="flex justify-between items-center text-xs text-[#75726B] dark:text-[#8a857b]">
            <span>+12 dB</span>
            <span className="font-semibold text-[#121212] dark:text-white">Ganancia por Frecuencia</span>
            <span>-12 dB</span>
          </div>

          <div className="flex items-center justify-between gap-1 h-36 bg-[#EAE5DA] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824]">
            {equalizer.bands.map((gain, i) => (
              <div key={i} className="flex flex-col items-center h-full flex-1 justify-between">
                <span className="text-[9px] font-bold text-[#75726B] dark:text-[#8a857b]">{gain > 0 ? `+${gain}` : gain}</span>
                <input
                  type="range"
                  min="-12"
                  max="12"
                  step="1"
                  value={gain}
                  disabled={!equalizer.enabled}
                  onChange={(e) => setBandGain(i, parseInt(e.target.value, 10))}
                  className="h-20 w-1.5 appearance-none bg-[#C9C3B6] dark:bg-[#33302b] rounded-full accent-[#121212] dark:accent-white -rotate-90 cursor-pointer"
                />
                <span className="text-[8px] text-[#75726B] dark:text-[#8a857b] truncate w-full text-center mt-1">{FREQUENCIES[i]}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Bass Boost & Virtualizer Sliders */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-[#EAE5DA] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824] flex flex-col gap-1.5">
            <div className="flex justify-between items-center text-xs">
              <span className="font-semibold text-black dark:text-white">Refuerzo Graves</span>
              <span className="text-[10px] text-[#75726B] dark:text-[#8a857b]">{equalizer.bassBoost}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              disabled={!equalizer.enabled}
              value={equalizer.bassBoost}
              onChange={(e) => setEqualizer({ ...equalizer, bassBoost: parseInt(e.target.value, 10) })}
              className="w-full accent-black dark:accent-white cursor-pointer"
            />
          </div>

          <div className="bg-[#EAE5DA] dark:bg-[#1a1917] p-3 rounded-2xl border border-[#ded8cd] dark:border-[#2a2824] flex flex-col gap-1.5">
            <div className="flex justify-between items-center text-xs">
              <span className="font-semibold text-black dark:text-white">Virtualizador 3D</span>
              <span className="text-[10px] text-[#75726B] dark:text-[#8a857b]">{equalizer.virtualizer}%</span>
            </div>
            <input
              type="range"
              min="0"
              max="100"
              disabled={!equalizer.enabled}
              value={equalizer.virtualizer}
              onChange={(e) => setEqualizer({ ...equalizer, virtualizer: parseInt(e.target.value, 10) })}
              className="w-full accent-black dark:accent-white cursor-pointer"
            />
          </div>
        </div>

        <button
          onClick={onClose}
          className="w-full py-3 bg-black dark:bg-white text-white dark:text-black font-bold text-xs rounded-2xl cursor-pointer"
        >
          Aplicar y Cerrar
        </button>
      </div>
    </div>
  );
};
