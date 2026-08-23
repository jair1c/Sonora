import React, { useState, useEffect } from 'react';
import { fetchArtistImage, cleanArtistName } from '../services/artistImages';

interface ArtistAvatarProps {
  artistName: string;
  avatarUrl?: string;
  className?: string;
  isSelected?: boolean;
}

export const ArtistAvatar: React.FC<ArtistAvatarProps> = ({
  artistName,
  avatarUrl,
  className = '',
  isSelected = false
}) => {
  const [imageUrl, setImageUrl] = useState<string | null>(
    avatarUrl && !avatarUrl.startsWith('content://') ? avatarUrl : null
  );
  const [hasError, setHasError] = useState(false);

  const cleanName = cleanArtistName(artistName);

  useEffect(() => {
    if (avatarUrl && !avatarUrl.startsWith('content://')) {
      setImageUrl(avatarUrl);
      setHasError(false);
      return;
    }

    let isMounted = true;

    async function load() {
      if (imageUrl && !imageUrl.startsWith('content://')) {
        return;
      }

      setHasError(false);
      try {
        const found = await fetchArtistImage(artistName);
        if (isMounted) {
          if (found) {
            setImageUrl(found);
          } else {
            setHasError(true);
          }
        }
      } catch {
        if (isMounted) {
          setHasError(true);
        }
      }
    }

    load();

    return () => {
      isMounted = false;
    };
  }, [artistName]);

  // Generate 2 initials (e.g., "Morat" -> "MO", "Rels B" -> "RB", "Feid" -> "FE")
  const getInitials = (name: string) => {
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  const initials = getInitials(cleanName || 'AR');

  return (
    <div
      className={`w-full h-full shadow-md overflow-hidden bg-[#e0ded8] transition-all duration-300 relative ${
        isSelected ? 'scale-105 filter drop-shadow-md' : 'opacity-90 hover:opacity-100'
      } ${className}`}
      style={{ clipPath: 'url(#flower-8-smooth)' }}
    >
      {imageUrl && !hasError ? (
        <img
          src={imageUrl}
          alt=""
          onError={() => setHasError(true)}
          className={`w-full h-full object-cover transition-all duration-300 ${
            isSelected ? 'contrast-115 scale-105' : 'grayscale-[0.35] contrast-110'
          }`}
        />
      ) : (
        /* Luxury Monogram Fallback inside 8-Lobe Scallop Shape */
        <div className="w-full h-full bg-gradient-to-br from-[#EAE5DA] via-[#DDD7CA] to-[#C9C2B4] flex flex-col items-center justify-center text-[#121212] select-none p-2 border border-[#D5CEBF]">
          <span className="font-extrabold font-outfit text-base tracking-wider text-[#121212] drop-shadow-sm">
            {initials}
          </span>
          <div className="w-4 h-[1.5px] bg-[#121212]/30 mt-1 rounded-full" />
        </div>
      )}
    </div>
  );
};
