import React, { useRef, useEffect, useState, useMemo } from 'react';
import { usePlayer } from '../context/PlayerContext';

// Symmetrical 8-Lobe Smooth Harmonic Wave generator (300x300 viewBox with safe padding)
export function getFlower8ContourPath(roundness: number = 50): string {
  // roundness: 0 (pure circle) to 100 (deep wave)
  const amp = (Math.max(0, Math.min(100, roundness)) / 100) * 18;
  const rBase = 132 - amp; // Maximum peak radius is 132 (18px safe margin from 150 boundary)
  const steps = 64;
  const cx = 150;
  const cy = 150;
  const points: { x: number; y: number }[] = [];

  for (let i = 0; i < steps; i++) {
    const t = (i / steps) * 2 * Math.PI - Math.PI / 2;
    const r = rBase + amp * Math.cos(8 * (t + Math.PI / 2));
    const x = cx + r * Math.cos(t);
    const y = cy + r * Math.sin(t);
    points.push({ x: Number(x.toFixed(4)), y: Number(y.toFixed(4)) });
  }

  let d = `M ${points[0].x} ${points[0].y}`;
  for (let i = 0; i < points.length; i++) {
    const p0 = points[(i - 1 + points.length) % points.length];
    const p1 = points[i];
    const p2 = points[(i + 1) % points.length];
    const p3 = points[(i + 2) % points.length];
    const cp1x = p1.x + (p2.x - p0.x) / 6;
    const cp1y = p1.y + (p2.y - p0.y) / 6;
    const cp2x = p2.x - (p3.x - p1.x) / 6;
    const cp2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C ${cp1x.toFixed(4)} ${cp1y.toFixed(4)}, ${cp2x.toFixed(4)} ${cp2y.toFixed(4)}, ${p2.x.toFixed(4)} ${p2.y.toFixed(4)}`;
  }
  d += ' Z';
  return d;
}

// Symmetrical 8-Lobe Smooth Harmonic Clip Path generator (0..1 Bounding Box)
export function getFlower8ClipPath(roundness: number = 50): string {
  // 0..1 bounding box
  const amp = (Math.max(0, Math.min(100, roundness)) / 100) * 0.075;
  const rBase = 0.49 - amp;
  const steps = 64;
  const cx = 0.5;
  const cy = 0.5;
  const points: { x: number; y: number }[] = [];

  for (let i = 0; i < steps; i++) {
    const t = (i / steps) * 2 * Math.PI - Math.PI / 2;
    const r = rBase + amp * Math.cos(8 * (t + Math.PI / 2));
    const x = cx + r * Math.cos(t);
    const y = cy + r * Math.sin(t);
    points.push({ x: Number(x.toFixed(3)), y: Number(y.toFixed(3)) });
  }

  let d = `M ${points[0].x} ${points[0].y}`;
  for (let i = 0; i < points.length; i++) {
    const p0 = points[(i - 1 + points.length) % points.length];
    const p1 = points[i];
    const p2 = points[(i + 1) % points.length];
    const p3 = points[(i + 2) % points.length];
    const cp1x = p1.x + (p2.x - p0.x) / 6;
    const cp1y = p1.y + (p2.y - p0.y) / 6;
    const cp2x = p2.x - (p3.x - p1.x) / 6;
    const cp2y = p2.y - (p3.y - p1.y) / 6;
    d += ` C ${cp1x.toFixed(3)} ${cp1y.toFixed(3)}, ${cp2x.toFixed(3)} ${cp2y.toFixed(3)}, ${p2.x.toFixed(3)} ${p2.y.toFixed(3)}`;
  }
  d += ' Z';
  return d;
}

export const OrganicClipDefs: React.FC = () => {
  const { petalRoundness } = usePlayer();
  const dynamicFlowerD = useMemo(() => getFlower8ClipPath(petalRoundness), [petalRoundness]);

  return (
    <svg className="absolute w-0 h-0 pointer-events-none" aria-hidden="true">
      <defs>
        {/* Dynamic Symmetrical 8-Petal Flower ClipPath (Controlled by user setting) */}
        <clipPath id="flower-8-smooth" clipPathUnits="objectBoundingBox">
          <path d={dynamicFlowerD} />
        </clipPath>

        {/* 12-Lobe Starburst */}
        <clipPath id="scallop-star-12" clipPathUnits="objectBoundingBox">
          <path d="M 0.5 0.02 C 0.54 0.02, 0.59 0.08, 0.63 0.07 C 0.68 0.06, 0.72 0.12, 0.77 0.13 C 0.82 0.14, 0.85 0.21, 0.89 0.24 C 0.93 0.27, 0.94 0.35, 0.97 0.4 C 0.99 0.45, 0.97 0.53, 0.97 0.59 C 0.97 0.65, 0.93 0.73, 0.91 0.78 C 0.87 0.84, 0.81 0.89, 0.76 0.93 C 0.7 0.96, 0.63 0.96, 0.57 0.98 C 0.51 1.0, 0.43 0.97, 0.37 0.97 C 0.31 0.97, 0.25 0.91, 0.2 0.88 C 0.14 0.85, 0.09 0.78, 0.06 0.72 C 0.03 0.66, 0.05 0.57, 0.04 0.5 C 0.03 0.43, 0.06 0.34, 0.08 0.28 C 0.11 0.21, 0.17 0.16, 0.22 0.12 C 0.28 0.07, 0.35 0.07, 0.41 0.04 C 0.44 0.02, 0.47 0.02, 0.5 0.02 Z" />
        </clipPath>

        {/* Organic Scallop Cloud */}
        <clipPath id="scallop-cloud" clipPathUnits="objectBoundingBox">
          <path d="M 0.5 0.05 C 0.65 0.02, 0.82 0.08, 0.92 0.22 C 1.01 0.37, 0.98 0.58, 0.96 0.74 C 0.93 0.9, 0.78 1.01, 0.6 0.99 C 0.42 0.98, 0.27 0.99, 0.14 0.88 C 0.01 0.76, -0.02 0.54, 0.03 0.36 C 0.08 0.19, 0.25 0.08, 0.42 0.06 Z" />
        </clipPath>
      </defs>
    </svg>
  );
};

// Background organic line decorations
export const BackgroundCurves: React.FC = () => {
  return (
    <svg
      className="absolute inset-0 w-full h-full pointer-events-none stroke-[#d3cec4] stroke-[1.2] fill-none opacity-80"
      viewBox="0 0 380 720"
      preserveAspectRatio="none"
    >
      <path d="M 30,0 C 70,120 180,140 180,240 C 180,340 70,380 90,500 C 100,560 160,600 190,720" />
      <path d="M 380,180 C 310,220 290,320 340,430 C 370,490 360,610 380,680" />
      <path d="M 0,280 C 60,310 100,380 90,460" />
    </svg>
  );
};

// Symmetrical 8-Petal Wavy Contour Scrubber
export const PlayerScallopedRing: React.FC<{
  progressPercent: number;
  isPlaying: boolean;
  onSeekPercent?: (percent: number) => void;
}> = ({ progressPercent, isPlaying, onSeekPercent }) => {
  const { petalRoundness } = usePlayer();
  const svgRef = useRef<SVGSVGElement | null>(null);
  const pathRef = useRef<SVGPathElement | null>(null);
  const [dotPos, setDotPos] = useState<{ x: number; y: number }>({ x: 150, y: 18 });
  const [pathLength, setPathLength] = useState<number>(0);

  const contourPathD = useMemo(() => getFlower8ContourPath(petalRoundness), [petalRoundness]);

  useEffect(() => {
    if (pathRef.current) {
      const len = pathRef.current.getTotalLength();
      setPathLength(len);
    }
  }, [contourPathD]);

  // Update dot position strictly along the 8-lobe wavy contour perimeter
  useEffect(() => {
    if (pathRef.current && pathLength > 0) {
      const targetDist = (Math.max(0, Math.min(100, progressPercent)) / 100) * pathLength;
      const pt = pathRef.current.getPointAtLength(targetDist);
      setDotPos({ x: pt.x, y: pt.y });
    }
  }, [progressPercent, pathLength, contourPathD]);

  const handlePointerSeek = (e: React.PointerEvent<SVGSVGElement>) => {
    if (!onSeekPercent || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const touchX = e.clientX - (rect.left + rect.width / 2);
    const touchY = e.clientY - (rect.top + rect.height / 2);

    let deg = Math.atan2(touchY, touchX) * (180 / Math.PI) + 90;
    if (deg < 0) deg += 360;

    const percent = Math.min(100, Math.max(0, (deg / 360) * 100));
    onSeekPercent(percent);
  };

  return (
    <div className="relative w-full h-full flex items-center justify-center pointer-events-auto overflow-visible">
      <svg
        ref={svgRef}
        viewBox="0 0 300 300"
        onPointerDown={(e) => {
          e.currentTarget.setPointerCapture(e.pointerId);
          handlePointerSeek(e);
        }}
        onPointerMove={(e) => {
          if (e.buttons === 1) handlePointerSeek(e);
        }}
        className={`w-full h-full cursor-pointer select-none transition-transform duration-700 overflow-visible ${
          isPlaying ? 'scale-[1.01]' : 'scale-100'
        }`}
      >
        {/* 1. Symmetrical 8-Petal Wavy Contour Line */}
        <path
          ref={pathRef}
          d={contourPathD}
          fill="none"
          stroke="currentColor"
          strokeWidth="1.4"
          className="text-[#b8b2a7] dark:text-[#444038] transition-colors"
        />

        {/* 2. Symmetrical 8-Petal Progress Stroke Trace */}
        {pathLength > 0 && (
          <path
            d={contourPathD}
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeDasharray={`${(progressPercent / 100) * pathLength} ${pathLength}`}
            strokeLinecap="round"
            className="text-[#121212] dark:text-[#ffffff] transition-all duration-75"
          />
        )}

        {/* 3. Scrubber Knob / Punto Deslizable (High Contrast & Visible in Light/Dark) */}
        <circle
          cx={dotPos.x}
          cy={dotPos.y}
          r="6.5"
          stroke="currentColor"
          strokeWidth="1.5"
          className="fill-black dark:fill-white text-[#f5f2ea] dark:text-[#0f0e0d] shadow-xl cursor-pointer transition-all duration-75"
        />
      </svg>
    </div>
  );
};
