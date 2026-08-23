import React, { useEffect } from 'react';
import { usePlayer } from '../context/PlayerContext';
import { BarChart3, X, Clock, PlayCircle } from 'lucide-react';
import { SongCover } from './SongCover';

interface StatsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const StatsModal: React.FC<StatsModalProps> = ({ isOpen, onClose }) => {
  const { stats, tracks } = usePlayer();

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const hours = Math.floor(stats.totalPlayTimeMinutes / 60);
  const minutes = stats.totalPlayTimeMinutes % 60;

  // Top 3 most played tracks
  const topTracks = [...tracks].sort((a, b) => (b.playCount || 0) - (a.playCount || 0)).slice(0, 3);

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
              <BarChart3 size={16} />
            </div>
            <div>
              <h2 className="text-lg font-bold font-outfit text-black dark:text-white">sonoraStats</h2>
              <p className="text-xs text-[#75726B] dark:text-[#8a857b]">Tus estadísticas locales de escucha</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full border border-[#DED8CD] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#EAE5DA] dark:hover:bg-[#252320] transition-colors text-black dark:text-white cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-[#EAE5DA] dark:bg-[#1a1917] p-4 rounded-2xl flex flex-col gap-1 border border-[#ded8cd] dark:border-[#2a2824]">
            <div className="flex items-center gap-1.5 text-[#75726B] dark:text-[#8a857b] text-xs font-semibold">
              <Clock size={14} /> Tiempo Total
            </div>
            <span className="text-2xl font-extrabold text-[#121212] dark:text-white">
              {hours}h {minutes}m
            </span>
          </div>

          <div className="bg-[#EAE5DA] dark:bg-[#1a1917] p-4 rounded-2xl flex flex-col gap-1 border border-[#ded8cd] dark:border-[#2a2824]">
            <div className="flex items-center gap-1.5 text-[#75726B] dark:text-[#8a857b] text-xs font-semibold">
              <PlayCircle size={14} /> Reproducciones
            </div>
            <span className="text-2xl font-extrabold text-[#121212] dark:text-white">
              {stats.totalTracksPlayed}
            </span>
          </div>
        </div>

        {/* Top Tracks */}
        <div className="flex flex-col gap-2">
          <label className="text-xs font-semibold uppercase tracking-wider text-[#75726B] dark:text-[#8a857b]">Canciones Más Escuchadas</label>
          <div className="flex flex-col gap-2">
            {topTracks.map((track, i) => (
              <div key={track.id} className="flex items-center justify-between p-3 bg-[#EAE5DA] dark:bg-[#1a1917] rounded-2xl border border-[#ded8cd] dark:border-[#2a2824]">
                <div className="flex items-center gap-3 truncate">
                  <span className="w-5 text-center font-bold text-xs text-[#75726B] dark:text-[#8a857b]">#{i + 1}</span>
                  <SongCover src={track.coverUrl} title={track.title} artist={track.artist} className="w-9 h-9 rounded-xl" />
                  <div className="flex flex-col truncate">
                    <span className="text-xs font-bold truncate text-black dark:text-white">{track.title}</span>
                    <span className="text-[11px] text-[#75726B] dark:text-[#8a857b] truncate">{track.artist}</span>
                  </div>
                </div>
                <span className="text-xs font-bold text-[#121212] dark:text-white ml-2 shrink-0">{track.playCount || 0} reproducciones</span>
              </div>
            ))}
          </div>
        </div>

        {/* Close Button */}
        <button
          onClick={onClose}
          className="w-full py-3 bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black rounded-2xl font-bold text-sm hover:opacity-90 transition-opacity cursor-pointer"
        >
          Cerrar Estadísticas
        </button>
      </div>
    </div>
  );
};
