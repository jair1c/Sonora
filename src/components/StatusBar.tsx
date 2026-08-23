import React from 'react';

interface StatusBarProps {
  time?: string;
  dark?: boolean;
}

export const StatusBar: React.FC<StatusBarProps> = ({ time = '12:41', dark = true }) => {
  const colorClass = dark ? 'text-black fill-black' : 'text-white fill-white';

  return (
    <div className={`w-full pt-3 px-6 pb-2 flex items-center justify-between text-xs font-semibold select-none ${colorClass}`}>
      {/* Time */}
      <span className="tracking-tight text-[13px] font-medium font-outfit">{time}</span>

      {/* Dynamic Island / Speaker Pill simulation if needed, or sleek icons */}
      <div className="flex items-center gap-1.5">
        {/* Cellular Signal Bars */}
        <svg className="w-4 h-3.5" viewBox="0 0 17 12" fill="currentColor">
          <rect x="0" y="8.5" width="2.5" height="3.5" rx="0.7" />
          <rect x="4.2" y="6" width="2.5" height="6" rx="0.7" />
          <rect x="8.5" y="3.5" width="2.5" height="8.5" rx="0.7" />
          <rect x="12.8" y="0.5" width="2.5" height="11.5" rx="0.7" />
        </svg>

        {/* Wi-Fi Icon */}
        <svg className="w-3.5 h-3" viewBox="0 0 16 12" fill="currentColor">
          <path d="M8 9.5a1.5 1.5 0 1 1 0 3 1.5 1.5 0 0 1 0-3Zm-3.8-3.2a5.4 5.4 0 0 1 7.6 0 .8.8 0 0 1-1.1 1.1 3.8 3.8 0 0 0-5.4 0 .8.8 0 0 1-1.1-1.1Zm-3-3a9.6 9.6 0 0 1 13.6 0 .8.8 0 0 1-1.1 1.1 8 8 0 0 0-11.4 0 .8.8 0 0 1-1.1-1.1Z" />
        </svg>

        {/* Battery Icon with inner level */}
        <div className="flex items-center">
          <div className="w-[20px] h-[10.5px] border border-current rounded-[3px] p-[1.5px] flex items-center">
            <div className="w-[85%] h-full bg-current rounded-[1px]" />
          </div>
          <div className="w-[1.2px] h-[3.5px] bg-current rounded-r-[1px] -ml-[0.5px]" />
        </div>
      </div>
    </div>
  );
};
