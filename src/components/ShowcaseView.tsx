import React from 'react';
import { OnboardingScreen } from './OnboardingScreen';
import { ArtistSelectScreen } from './ArtistSelectScreen';
import { PlayerScreen } from './PlayerScreen';

export const ShowcaseView: React.FC = () => {
  return (
    <div className="w-full min-h-screen py-10 px-4 md:px-8 flex flex-col items-center justify-center bg-[#b8b3a7]">
      <div className="w-full max-w-[1260px] grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 xl:gap-10 items-center justify-items-center">
        <div className="w-full max-w-[340px] rounded-[44px] overflow-hidden shadow-2xl border-[5px] border-[#e2ded5]">
          <OnboardingScreen />
        </div>
        <div className="w-full max-w-[340px] rounded-[44px] overflow-hidden shadow-2xl border-[5px] border-[#e2ded5]">
          <ArtistSelectScreen />
        </div>
        <div className="w-full max-w-[340px] rounded-[44px] overflow-hidden shadow-2xl border-[5px] border-[#e2ded5]">
          <PlayerScreen />
        </div>
      </div>
    </div>
  );
};
