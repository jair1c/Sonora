import React from 'react';
import { motion } from 'framer-motion';
import { BackgroundCurves } from './OrganicShapes';
import { usePlayer } from '../context/PlayerContext';
import { Sparkles, Music2, Disc3 } from 'lucide-react';

export const OnboardingScreen: React.FC = () => {
  const { setActiveScreen, scanLocalMusic, isScanning } = usePlayer();

  const handleStart = async () => {
    localStorage.setItem('luxTune_onboarding_done', 'true');
    scanLocalMusic();
    setActiveScreen('artists');
  };

  return (
    <div
      onTouchMove={(e) => e.stopPropagation()}
      className="fixed inset-0 w-full h-full max-h-screen bg-[#F5F2EA] dark:bg-[#0F0E0D] flex flex-col justify-between select-none text-[#121212] dark:text-[#f5f2ea] overflow-hidden pt-4 pb-6 px-6 touch-none overscroll-none"
    >
      {/* Decorative Organic Background Lines with Breathing Flow */}
      <BackgroundCurves />

      {/* Floating Acoustic Particles */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden z-0">
        {[...Array(6)].map((_, i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0.2, y: '100vh', x: `${15 + i * 14}%` }}
            animate={{
              opacity: [0.2, 0.6, 0.2],
              y: ['100vh', '-10vh'],
              x: [`${15 + i * 14}%`, `${18 + i * 13 + (i % 2 === 0 ? 5 : -5)}%`]
            }}
            transition={{
              duration: 12 + i * 3,
              repeat: Infinity,
              ease: 'linear',
              delay: i * 2.2
            }}
            className="absolute text-[#aba496]/40 dark:text-[#555047]/40"
          >
            {i % 2 === 0 ? <Disc3 size={14 + (i % 3) * 4} /> : <Sparkles size={12 + (i % 3) * 4} />}
          </motion.div>
        ))}
      </div>

      {/* Top Header */}
      <div className="relative z-10 pt-1 flex items-center justify-between shrink-0">
        {/* Sonora Logo with subtle float */}
        <motion.div
          initial={{ opacity: 0, x: -15 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
          className="flex items-center tracking-tight"
        >
          <span className="font-extrabold text-[24px] tracking-tight font-outfit text-black dark:text-white">Sonora</span>
        </motion.div>

        {/* Skip button */}
        <motion.button
          initial={{ opacity: 0, x: 15 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, ease: 'easeOut' }}
          onClick={handleStart}
          className="text-xs text-[#2c2b29] dark:text-[#aba496] font-semibold hover:text-black dark:hover:text-white transition-colors cursor-pointer py-1.5 px-3 rounded-full border border-[#ded8cd] dark:border-[#2a2824] bg-[#eae5da]/60 dark:bg-[#1a1917]/60 active:scale-95"
        >
          Saltar
        </motion.button>
      </div>

      {/* Artistic Dynamic Organic Bubbles Section (Wheel + Bounce Physics) */}
      <div className="relative z-10 flex-1 w-full my-auto flex items-center justify-center min-h-[240px] max-h-[340px] overflow-visible">
        {/* 1. Top Right Starburst Artist (Floating Orbital Drift + Rotating Wheel Contour) */}
        <motion.div
          drag
          dragConstraints={{ left: -50, right: 30, top: -30, bottom: 50 }}
          dragElastic={0.4}
          whileTap={{ scale: 0.92 }}
          animate={{
            y: [0, -14, 6, 0],
            x: [0, 8, -6, 0],
            rotate: [0, 6, -4, 0]
          }}
          transition={{
            duration: 7,
            repeat: Infinity,
            ease: 'easeInOut'
          }}
          className="absolute right-3 top-2 w-[125px] h-[125px] sm:w-[145px] sm:h-[145px] flex items-center justify-center cursor-grab active:cursor-grabbing z-20"
        >
          {/* Rotating Outer Scallop Wheel */}
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 32, repeat: Infinity, ease: 'linear' }}
            className="absolute inset-0 scale-110 pointer-events-none"
          >
            <svg viewBox="0 0 100 100" className="w-full h-full stroke-[#9e978b] dark:stroke-[#5a544a] stroke-[1.2] fill-none">
              <path d="M 50 3 C 54 3, 59 10, 64 7 C 70 4, 75 12, 80 11 C 87 10, 89 20, 94 22 C 99 24, 98 35, 100 40 C 102 46, 97 55, 98 61 C 98 68, 93 76, 90 82 C 86 88, 78 91, 73 95 C 67 98, 60 95, 54 99 C 47 101, 41 96, 35 98 C 29 99, 24 92, 19 90 C 13 88, 9 79, 6 73 C 3 66, 8 57, 6 50 C 4 43, 8 34, 9 27 C 11 19, 19 15, 24 10 C 30 5, 37 8, 43 4 C 46 2, 48 3, 50 3 Z" />
            </svg>
          </motion.div>
          <div
            className="w-full h-full shadow-2xl overflow-hidden bg-[#e0ded8] dark:bg-[#201e1b] pointer-events-none"
            style={{ clipPath: 'url(#scallop-star-12)' }}
          >
            <img
              src="https://images.unsplash.com/photo-1508214751196-bcfd4ca60f91?q=80&w=400&auto=format&fit=crop"
              alt=""
              className="w-full h-full object-cover grayscale contrast-125 select-none"
            />
          </div>
        </motion.div>

        {/* 2. Middle Left Cloud/Scalloped Artist (Counter-Floating + Elastic Pendulum Rebound) */}
        <motion.div
          drag
          dragConstraints={{ left: -30, right: 60, top: -40, bottom: 40 }}
          dragElastic={0.4}
          whileTap={{ scale: 0.92 }}
          animate={{
            y: [0, 16, -10, 0],
            x: [0, -10, 8, 0],
            rotate: [0, -8, 10, 0]
          }}
          transition={{
            duration: 8.5,
            repeat: Infinity,
            ease: 'easeInOut',
            delay: 0.6
          }}
          className="absolute left-3 top-12 w-[120px] h-[120px] sm:w-[140px] sm:h-[140px] flex items-center justify-center cursor-grab active:cursor-grabbing z-30"
        >
          {/* Rotating Cloud Wheel */}
          <motion.div
            animate={{ rotate: -360 }}
            transition={{ duration: 38, repeat: Infinity, ease: 'linear' }}
            className="absolute inset-0 scale-105 pointer-events-none"
          >
            <svg viewBox="0 0 100 100" className="w-full h-full stroke-[#9e978b] dark:stroke-[#5a544a] stroke-[1.2] fill-none">
              <path d="M 50 5 C 65 2, 82 8, 92 22 C 101 37, 98 58, 96 74 C 93 90, 78 101, 60 99 C 42 98, 27 99, 14 88 C 1 76, -2 54, 3 36 C 8 19, 25 8, 42 6 Z" />
            </svg>
          </motion.div>
          <div
            className="w-full h-full shadow-2xl overflow-hidden bg-[#e0ded8] dark:bg-[#201e1b] pointer-events-none"
            style={{ clipPath: 'url(#scallop-cloud)' }}
          >
            <img
              src="https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=400&auto=format&fit=crop"
              alt=""
              className="w-full h-full object-cover grayscale contrast-125 select-none"
            />
          </div>
        </motion.div>

        {/* 3. Bottom Right 8-Petal Flower Artist (Harmonic Sway + Wheel Contour) */}
        <motion.div
          drag
          dragConstraints={{ left: -50, right: 30, top: -50, bottom: 30 }}
          dragElastic={0.4}
          whileTap={{ scale: 0.92 }}
          animate={{
            y: [0, -12, 12, 0],
            x: [0, -12, 6, 0],
            rotate: [0, 12, -8, 0]
          }}
          transition={{
            duration: 7.8,
            repeat: Infinity,
            ease: 'easeInOut',
            delay: 1.2
          }}
          className="absolute right-4 bottom-2 w-[120px] h-[120px] sm:w-[140px] sm:h-[140px] flex items-center justify-center cursor-grab active:cursor-grabbing z-20"
        >
          {/* Rotating Harmonic Flower Wheel */}
          <motion.div
            animate={{ rotate: 360 }}
            transition={{ duration: 28, repeat: Infinity, ease: 'linear' }}
            className="absolute inset-0 scale-105 pointer-events-none"
          >
            <svg viewBox="0 0 100 100" className="w-full h-full stroke-[#9e978b] dark:stroke-[#5a544a] stroke-[1.2] fill-none">
              <circle cx="50" cy="50" r="48" />
            </svg>
          </motion.div>
          <div
            className="w-full h-full rounded-full shadow-2xl overflow-hidden border border-[#d8d3c9] dark:border-[#33302b] pointer-events-none"
            style={{ clipPath: 'url(#flower-8-smooth)' }}
          >
            <img
              src="https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=400&auto=format&fit=crop"
              alt=""
              className="w-full h-full object-cover grayscale contrast-125 sepia-[0.3] select-none"
            />
          </div>
        </motion.div>
      </div>

      {/* Bottom Content & CTA (Static & Locked) */}
      <div className="relative z-10 shrink-0">
        {/* Headline with Fade-in */}
        <motion.h1
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="text-[28px] sm:text-[32px] leading-[1.15] text-[#121212] dark:text-[#f5f2ea] tracking-tight mb-2"
        >
          <span className="font-extrabold font-outfit text-black dark:text-white">Eleva </span>
          <span className="font-light text-[#3a3937] dark:text-[#a8a398]">Cada</span>
          <br />
          <span className="font-light text-[#3a3937] dark:text-[#a8a398]">Momento con la </span>
          <span className="font-extrabold font-outfit text-black dark:text-white">Música</span>
        </motion.h1>

        {/* Subtitle */}
        <motion.p
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.3 }}
          className="text-[12px] leading-relaxed text-[#716e68] dark:text-[#969186] font-normal max-w-[320px] mb-4"
        >
          Sumérgete en un mundo donde cada ritmo realza tu estado de ánimo y cada melodía cuenta tu historia local.
        </motion.p>

        {/* Comenzar Button with pulse */}
        <motion.button
          initial={{ opacity: 0, y: 15 }}
          animate={{ opacity: 1, y: 0 }}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.97 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          onClick={handleStart}
          disabled={isScanning}
          className="w-full py-3.5 bg-black dark:bg-white text-white dark:text-black font-semibold text-sm rounded-full hover:bg-neutral-900 active:scale-[0.98] transition-all shadow-xl cursor-pointer flex items-center justify-center gap-2 border border-neutral-800 dark:border-neutral-200"
        >
          <Music2 size={16} />
          {isScanning ? 'Escaneando Tu Música...' : 'Explorar Mi Música'}
        </motion.button>

        {/* Offline Audio Badge */}
        <div className="mt-2.5 text-center text-[11px] text-[#716e68] dark:text-[#969186] flex items-center justify-center gap-1">
          <Sparkles size={12} className="text-[#121212] dark:text-[#f5f2ea]" />
          <span>Reproducción 100% Offline de Alta Fidelidad</span>
        </div>
      </div>
    </div>
  );
};
