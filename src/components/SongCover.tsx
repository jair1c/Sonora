import React, { useState, memo } from 'react';
import { Disc3 } from 'lucide-react';

interface SongCoverProps {
  src?: string;
  title?: string;
  artist?: string;
  className?: string;
  shape?: 'rounded' | 'scallop' | 'circle';
}

export const SongCover: React.FC<SongCoverProps> = memo(({
  src,
  title = '',
  className = 'w-10 h-10 rounded-xl',
  shape = 'rounded'
}) => {
  const [hasError, setHasError] = useState(false);

  const getInitials = (text: string) => {
    if (!text) return '♪';
    const words = text.trim().split(/\s+/);
    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase();
    }
    return text.slice(0, 2).toUpperCase();
  };

  const clipStyle = shape === 'scallop' ? { clipPath: 'url(#flower-8-smooth)' } : undefined;
  const isRealImage = src && !src.startsWith('content://') && !hasError;

  return (
    <div
      style={clipStyle}
      className={`relative overflow-hidden bg-[#e0ded8] shadow-sm flex items-center justify-center shrink-0 ${className}`}
    >
      {isRealImage ? (
        <img
          src={src}
          alt=""
          loading="lazy"
          decoding="async"
          onError={() => setHasError(true)}
          className="w-full h-full object-cover"
        />
      ) : (
        /* Luxury Vinyl Monogram Fallback */
        <div className="w-full h-full bg-gradient-to-br from-[#EAE5DA] via-[#DDD7CA] to-[#C9C2B4] border border-[#D5CEBF] flex flex-col items-center justify-center text-[#121212] select-none p-1">
          <span className="font-extrabold font-outfit text-[11px] tracking-wider text-[#121212]">
            {getInitials(title)}
          </span>
          <Disc3 size={10} className="text-[#121212]/50 mt-0.5" />
        </div>
      )}
    </div>
  );
});
