export interface LyricLine {
  time: number;
  text: string;
}

export interface Track {
  id: string;
  title: string;
  artist: string;
  album: string;
  duration: number; // in seconds
  coverUrl: string;
  audioUrl: string;
  filePath?: string;
  size?: number;
  year?: number;
  genre?: string;
  bitrate?: string;
  lyrics?: LyricLine[];
  isLiked?: boolean;
  playCount?: number;
  lastPlayed?: number;
}

export interface Artist {
  id: string;
  name: string;
  avatarUrl: string;
  trackCount?: number;
  genre?: string;
}

export interface Playlist {
  id: string;
  name: string;
  trackIds: string[];
  coverUrl?: string;
  createdAt: number;
}

export const sampleSongs: Track[] = [
  {
    id: 'clash-of-titans',
    title: 'CLASH OF TITANS',
    artist: 'Lana Del Rey',
    album: 'Did You Know That There\'s a Tunnel Under Ocean Blvd',
    duration: 220, // 03:40
    coverUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=800&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-serene-view-443.mp3',
    year: 2024,
    genre: 'Indie Pop',
    bitrate: '320 kbps (FLAC)',
    isLiked: true,
    playCount: 14,
    lyrics: [
      { time: 0, text: '♪ (Intro Instrumental Melódico) ♪' },
      { time: 15, text: 'Bajo el cielo abierto de la medianoche' },
      { time: 28, text: 'Las sombras bailan al compás del viento' },
      { time: 42, text: 'En cada nota resuena el eco de tu voz' },
      { time: 58, text: 'Recordando los momentos que dejamos atrás' },
      { time: 74, text: 'Clash of titans en el firmamento' },
      { time: 83, text: 'Donde el tiempo se detiene y la música vive' },
      { time: 98, text: 'Elevando cada latido con su dulce melodía' },
      { time: 120, text: 'Y en el silencio encontramos nuestra armonía' },
      { time: 150, text: 'Bajo las luces suaves del anochecer...' },
      { time: 185, text: '♪ (Solo de Guitarra y Piano) ♪' },
      { time: 210, text: 'Donde el viaje nunca termina.' }
    ]
  },
  {
    id: 'midnight-serenade',
    title: 'MIDNIGHT SERENADE',
    artist: 'Billie Eilish',
    album: 'Hit Me Hard and Soft',
    duration: 204, // 03:24
    coverUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=600&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-chill-bro-494.mp3',
    year: 2024,
    genre: 'Alternative',
    bitrate: '320 kbps',
    isLiked: false,
    playCount: 8,
    lyrics: [
      { time: 0, text: '♪ (Vibraciones Suaves) ♪' },
      { time: 18, text: 'Step into the shadows where we used to meet' },
      { time: 45, text: 'Heartbeats racing on the empty street' },
      { time: 78, text: 'You know what they say about the quiet ones' },
      { time: 110, text: 'Holding on until the morning comes' },
      { time: 150, text: 'Midnight serenade in my mind.' }
    ]
  },
  {
    id: 'golden-hour',
    title: 'GOLDEN HOUR DREAMS',
    artist: 'Drake',
    album: 'For All The Dogs',
    duration: 195, // 03:15
    coverUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=600&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-tech-house-vibes-130.mp3',
    year: 2023,
    genre: 'Hip Hop / R&B',
    bitrate: '256 kbps',
    isLiked: true,
    playCount: 19,
    lyrics: [
      { time: 0, text: '♪ (Beat Intro) ♪' },
      { time: 12, text: 'City lights shining from the penthouse view' },
      { time: 34, text: 'Looking at the world thinking about you' },
      { time: 65, text: 'Never looking back when the bass drops low' },
      { time: 102, text: 'Golden hour moments everywhere we go.' }
    ]
  },
  {
    id: 'astral-projection',
    title: 'ASTRAL PROJECTION',
    artist: 'Travis Scott',
    album: 'UTOPIA',
    duration: 245, // 04:05
    coverUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=600&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-hazy-after-hours-132.mp3',
    year: 2023,
    genre: 'Trap / Psychedelic',
    bitrate: '320 kbps (Lossless)',
    isLiked: false,
    playCount: 11,
    lyrics: [
      { time: 0, text: '♪ (Atmospheric Synths) ♪' },
      { time: 20, text: 'Taking off into the stratosphere' },
      { time: 55, text: 'Nothing can stop what is coming near' },
      { time: 90, text: 'Open your eyes to the highest peak.' }
    ]
  },
  {
    id: 'rap-god-legacy',
    title: 'RAP GOD LEGACY',
    artist: 'Eminem',
    album: 'The Death of Slim Shady',
    duration: 260, // 04:20
    coverUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=600&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-game-level-music-689.mp3',
    year: 2024,
    genre: 'Hip Hop',
    bitrate: '320 kbps',
    isLiked: true,
    playCount: 22,
    lyrics: [
      { time: 0, text: '♪ (Heavy Bass Intro) ♪' },
      { time: 15, text: 'Lyrics flowing at the speed of light' },
      { time: 40, text: 'Standing tall in the darkest night' },
      { time: 80, text: 'Unstoppable rhythm, unstoppable mind.' }
    ]
  },
  {
    id: 'positions-in-time',
    title: 'POSITIONS IN TIME',
    artist: 'Ariana Grande',
    album: 'Eternal Sunshine',
    duration: 182, // 03:02
    coverUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=600&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-silent-descent-579.mp3',
    year: 2024,
    genre: 'Pop / R&B',
    bitrate: '320 kbps',
    isLiked: false,
    playCount: 6,
    lyrics: [
      { time: 0, text: '♪ (Smooth Strings) ♪' },
      { time: 22, text: 'Finding peace in the morning light' },
      { time: 50, text: 'Holding on to what feels so right' },
      { time: 90, text: 'Eternal sunshine inside your heart.' }
    ]
  },
  {
    id: 'forest-hills-drive',
    title: 'FOREST HILLS ECHOES',
    artist: 'J. Cole',
    album: 'The Off-Season',
    duration: 215, // 03:35
    coverUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=600&auto=format&fit=crop',
    audioUrl: 'https://assets.mixkit.co/music/preview/mixkit-delightful-4.mp3',
    year: 2021,
    genre: 'Hip Hop / Soul',
    bitrate: '320 kbps',
    isLiked: true,
    playCount: 15,
    lyrics: [
      { time: 0, text: '♪ (Soul Sample) ♪' },
      { time: 18, text: 'Memories of the porch steps at night' },
      { time: 48, text: 'Writing verses till the morning light' },
      { time: 88, text: 'Every word written with real soul.' }
    ]
  }
];

export const ARTISTS_DATA: Artist[] = [
  {
    id: 'lana-del-rey',
    name: 'Lana Del Rey',
    avatarUrl: 'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?q=80&w=400&auto=format&fit=crop',
    trackCount: 12,
    genre: 'Indie Pop'
  },
  {
    id: 'billie-eilish',
    name: 'Billie Eilish',
    avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=400&auto=format&fit=crop',
    trackCount: 8,
    genre: 'Alternative'
  },
  {
    id: 'drake',
    name: 'Drake',
    avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=400&auto=format&fit=crop',
    trackCount: 15,
    genre: 'Hip Hop'
  },
  {
    id: 'eminem',
    name: 'Eminem',
    avatarUrl: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=400&auto=format&fit=crop',
    trackCount: 9,
    genre: 'Rap'
  },
  {
    id: 'travis-scott',
    name: 'Travis Scott',
    avatarUrl: 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=400&auto=format&fit=crop',
    trackCount: 6,
    genre: 'Trap'
  },
  {
    id: 'ariana-grande',
    name: 'Ariana Grande',
    avatarUrl: 'https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=400&auto=format&fit=crop',
    trackCount: 11,
    genre: 'Pop'
  },
  {
    id: 'j-cole',
    name: 'J. Cole',
    avatarUrl: 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?q=80&w=400&auto=format&fit=crop',
    trackCount: 7,
    genre: 'Hip Hop'
  },
  {
    id: 'bad-bunny',
    name: 'Bad Bunny',
    avatarUrl: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=400&auto=format&fit=crop',
    trackCount: 14,
    genre: 'Urbano Latino'
  },
  {
    id: 'dua-lipa',
    name: 'Dua Lipa',
    avatarUrl: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=400&auto=format&fit=crop',
    trackCount: 10,
    genre: 'Disco Pop'
  },
  {
    id: 'taylor-swift',
    name: 'Taylor Swift',
    avatarUrl: 'https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?q=80&w=400&auto=format&fit=crop',
    trackCount: 18,
    genre: 'Folk Pop'
  },
  {
    id: 'joji',
    name: 'Joji',
    avatarUrl: 'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?q=80&w=400&auto=format&fit=crop',
    trackCount: 5,
    genre: 'Lo-Fi / R&B'
  },
  {
    id: 'the-weeknd',
    name: 'The Weeknd',
    avatarUrl: 'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?q=80&w=400&auto=format&fit=crop',
    trackCount: 16,
    genre: 'Synthwave / R&B'
  }
];

export const TRACKS_DATA = sampleSongs;
