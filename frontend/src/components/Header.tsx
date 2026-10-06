import React from 'react';
import { Search, Flame, Plus } from 'lucide-react';
import { LayoutSwitcher } from './LayoutSwitcher';

export const Header: React.FC = () => {
  return (
    <header className="h-16 border-b border-[#262626] bg-[#0d0d0d]/90 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-30">
      {/* Search Input (Ctrl + K) */}
      <div className="relative w-72 md:w-96">
        <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          placeholder="Search subjects, Rosen theorems, GATE PYQs... (Ctrl + K)"
          className="w-full bg-[#161616] border border-[#262626] rounded-lg pl-9 pr-12 py-1.5 text-xs text-zinc-200 placeholder:text-zinc-600 focus:outline-none focus:border-amber-500/50 transition-all"
        />
        <kbd className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] bg-zinc-800 text-zinc-400 px-1.5 py-0.5 rounded border border-zinc-700 font-mono">
          ⌘K
        </kbd>
      </div>

      {/* Header Actions: Layout Switcher, Streak, Quick Log */}
      <div className="flex items-center gap-3 md:gap-4">
        {/* Client-Side Layout Switcher Engine */}
        <LayoutSwitcher />

        {/* Streak Counter */}
        <div className="flex items-center gap-1.5 bg-[#161616] border border-[#262626] px-3 py-1.5 rounded-lg text-xs font-semibold text-zinc-300">
          <Flame className="w-4 h-4 text-amber-500 animate-pulse" />
          <span>14</span>
          <span className="text-[10px] text-zinc-500 font-normal hidden sm:inline">days streak</span>
        </div>

        {/* Quick Action */}
        <button className="flex items-center gap-1.5 bg-amber-500 hover:bg-amber-400 text-black px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all shadow-md shadow-amber-500/10">
          <Plus className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">Quick Log</span>
        </button>
      </div>
    </header>
  );
};
