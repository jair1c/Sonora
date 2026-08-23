import React, { useState, useEffect } from 'react';
import { usePlayer } from '../context/PlayerContext';
import type { Track } from '../data/musicData';
import { Tag, X, Check } from 'lucide-react';

interface TagEditorModalProps {
  isOpen: boolean;
  onClose: () => void;
  track?: Track;
}

export const TagEditorModal: React.FC<TagEditorModalProps> = ({ isOpen, onClose, track }) => {
  const { currentTrack, updateTrackMetadata } = usePlayer();
  const targetTrack = track || currentTrack;

  const [title, setTitle] = useState(targetTrack?.title || '');
  const [artist, setArtist] = useState(targetTrack?.artist || '');
  const [album, setAlbum] = useState(targetTrack?.album || '');
  const [year, setYear] = useState(targetTrack?.year?.toString() || '2024');
  const [genre, setGenre] = useState(targetTrack?.genre || 'Pop');

  useEffect(() => {
    if (targetTrack) {
      setTitle(targetTrack.title);
      setArtist(targetTrack.artist);
      setAlbum(targetTrack.album);
      setYear(targetTrack.year?.toString() || '2024');
      setGenre(targetTrack.genre || 'Música');
    }
  }, [targetTrack]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = '';
      };
    }
  }, [isOpen]);

  if (!isOpen || !targetTrack) return null;

  const handleSave = () => {
    updateTrackMetadata(targetTrack.id, {
      title,
      artist,
      album,
      year: parseInt(year, 10) || targetTrack.year,
      genre
    });
    onClose();
  };

  return (
    <div
      onClick={onClose}
      onTouchMove={(e) => e.stopPropagation()}
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in touch-none overscroll-contain"
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-md bg-[#F5F2EA] dark:bg-[#161513] rounded-3xl p-6 shadow-2xl border border-[#DED8CD] dark:border-[#2a2824] flex flex-col gap-4 text-[#121212] dark:text-[#f5f2ea] overscroll-contain touch-pan-y"
      >

        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black flex items-center justify-center">
              <Tag size={16} />
            </div>
            <div>
              <h2 className="text-lg font-bold font-outfit text-black dark:text-white">Editor de Etiquetas</h2>
              <p className="text-xs text-[#75726B] dark:text-[#8a857b]">Edita la información de tu archivo local</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-full border border-[#DED8CD] dark:border-[#2a2824] flex items-center justify-center hover:bg-[#EAE5DA] dark:hover:bg-[#252320] transition-colors text-black dark:text-white cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* Input Fields */}
        <div className="flex flex-col gap-3">
          <div className="flex flex-col gap-1">
            <label className="text-[11px] font-bold text-[#75726B] dark:text-[#8a857b] uppercase tracking-wider">Título de la Canción</label>
            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="px-4 py-2.5 bg-[#EAE5DA] dark:bg-[#1a1917] border border-[#DED8CD] dark:border-[#2a2824] rounded-2xl text-sm font-semibold text-[#121212] dark:text-[#f5f2ea] focus:outline-none focus:border-black dark:focus:border-white"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-[11px] font-bold text-[#75726B] dark:text-[#8a857b] uppercase tracking-wider">Artista</label>
            <input
              type="text"
              value={artist}
              onChange={(e) => setArtist(e.target.value)}
              className="px-4 py-2.5 bg-[#EAE5DA] dark:bg-[#1a1917] border border-[#DED8CD] dark:border-[#2a2824] rounded-2xl text-sm font-semibold text-[#121212] dark:text-[#f5f2ea] focus:outline-none focus:border-black dark:focus:border-white"
            />
          </div>

          <div className="flex flex-col gap-1">
            <label className="text-[11px] font-bold text-[#75726B] dark:text-[#8a857b] uppercase tracking-wider">Álbum</label>
            <input
              type="text"
              value={album}
              onChange={(e) => setAlbum(e.target.value)}
              className="px-4 py-2.5 bg-[#EAE5DA] dark:bg-[#1a1917] border border-[#DED8CD] dark:border-[#2a2824] rounded-2xl text-sm font-semibold text-[#121212] dark:text-[#f5f2ea] focus:outline-none focus:border-black dark:focus:border-white"
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-bold text-[#75726B] dark:text-[#8a857b] uppercase tracking-wider">Año</label>
              <input
                type="text"
                value={year}
                onChange={(e) => setYear(e.target.value)}
                className="px-4 py-2.5 bg-[#EAE5DA] dark:bg-[#1a1917] border border-[#DED8CD] dark:border-[#2a2824] rounded-2xl text-sm font-semibold text-[#121212] dark:text-[#f5f2ea] focus:outline-none focus:border-black dark:focus:border-white"
              />
            </div>

            <div className="flex flex-col gap-1">
              <label className="text-[11px] font-bold text-[#75726B] dark:text-[#8a857b] uppercase tracking-wider">Género</label>
              <input
                type="text"
                value={genre}
                onChange={(e) => setGenre(e.target.value)}
                className="px-4 py-2.5 bg-[#EAE5DA] dark:bg-[#1a1917] border border-[#DED8CD] dark:border-[#2a2824] rounded-2xl text-sm font-semibold text-[#121212] dark:text-[#f5f2ea] focus:outline-none focus:border-black dark:focus:border-white"
              />
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex gap-2 mt-2">
          <button
            onClick={onClose}
            className="flex-1 py-3 border border-[#DED8CD] dark:border-[#2a2824] text-xs font-bold text-[#75726B] dark:text-[#8a857b] rounded-2xl hover:bg-[#EAE5DA] dark:hover:bg-[#252320] transition-colors cursor-pointer"
          >
            Cancelar
          </button>
          <button
            onClick={handleSave}
            className="flex-1 py-3 bg-[#121212] dark:bg-white text-[#F5F2EA] dark:text-black font-bold text-xs rounded-2xl flex items-center justify-center gap-1.5 shadow-md cursor-pointer"
          >
            <Check size={16} /> Guardar
          </button>
        </div>
      </div>
    </div>
  );
};
