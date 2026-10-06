import React from 'react';
import { Info, Clock, Coins, Sparkles, AlertCircle, ArrowUpRight } from 'lucide-react';

export const RightSidebar: React.FC = () => {
  return (
    <aside className="w-80 bg-[#0d0d0d] border-l border-[#262626] p-5 space-y-6 shrink-0 select-none hidden xl:block min-h-screen">
      {/* 1. GATE Syllabus Progress Gauge */}
      <div className="bg-[#161616] border border-[#262626] rounded-2xl p-5 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-xs uppercase tracking-wider text-zinc-300">
            GATE Syllabus Progress
          </h3>
          <Info className="w-3.5 h-3.5 text-zinc-500 hover:text-zinc-300 cursor-pointer" />
        </div>

        {/* Circular Progress Gauge */}
        <div className="flex flex-col items-center justify-center py-2">
          <div className="relative w-36 h-36 flex items-center justify-center">
            {/* SVG Radial Gauge */}
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              {/* Background circle */}
              <circle
                cx="50"
                cy="50"
                r="40"
                className="stroke-[#262626]"
                strokeWidth="8"
                fill="transparent"
              />
              {/* Progress circle */}
              <circle
                cx="50"
                cy="50"
                r="40"
                className="stroke-amber-500 transition-all duration-1000 ease-out"
                strokeWidth="8"
                strokeDasharray="251.2"
                strokeDashoffset="210" /* showing initial progress */
                strokeLinecap="round"
                fill="transparent"
              />
            </svg>

            {/* Inner text */}
            <div className="absolute flex flex-col items-center text-center">
              <span className="text-2xl font-black text-white leading-none font-mono">16.5</span>
              <span className="text-[10px] text-zinc-500 font-mono mt-0.5">/ 100 Marks</span>
            </div>
          </div>
        </div>

        {/* Breakdown Legend */}
        <div className="space-y-2 text-xs pt-1 border-t border-zinc-800/80">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span className="text-zinc-400">Foundation (GA + Math)</span>
            </div>
            <span className="font-mono text-zinc-300">9.0 / 29</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-amber-400" />
              <span className="text-zinc-400">Core Systems (OS, DBMS, CN)</span>
            </div>
            <span className="font-mono text-zinc-300">5.5 / 42</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-rose-400" />
              <span className="text-zinc-400">Theory & Algorithms</span>
            </div>
            <span className="font-mono text-zinc-300">2.0 / 29</span>
          </div>
        </div>
      </div>

      {/* 2. One-Stop Platform Callout */}
      <div className="bg-gradient-to-b from-[#181818] to-[#121212] border border-[#262626] rounded-2xl p-5 space-y-3 relative overflow-hidden">
        <div className="inline-block px-2.5 py-0.5 rounded-full bg-amber-500/10 border border-amber-500/20 text-[10px] font-semibold text-amber-400">
          One-stop platform
        </div>
        <h4 className="font-bold text-sm text-white leading-snug">
          Too many tabs open?
        </h4>
        <p className="text-xs text-zinc-400 leading-relaxed">
          Time lost never comes back. Find GATE Overflow, Kenneth Rosen notes, and mock results all in one portal.
        </p>
        <button className="w-full bg-[#202020] hover:bg-[#262626] border border-zinc-700/60 text-zinc-200 py-2 rounded-xl text-xs font-semibold transition-all">
          Open Focus Mode
        </button>
      </div>

      {/* 3. Problem of the Day */}
      <div className="bg-[#161616] border border-[#262626] rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 font-bold text-xs uppercase tracking-wider text-zinc-300">
            <span>Problem of the Day</span>
            <ArrowUpRight className="w-3.5 h-3.5 text-zinc-500" />
          </div>
          <div className="flex items-center gap-1 text-[11px] font-mono text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded-full border border-amber-500/20">
            <Coins className="w-3 h-3" />
            <span>+20</span>
          </div>
        </div>

        <div className="space-y-1">
          <div className="text-[11px] font-semibold text-cyan-400 font-mono">
            GATE CS 2023 · NAT · 2 Marks
          </div>
          <p className="text-xs font-medium text-zinc-200 line-clamp-2">
            An instruction pipeline has 5 stages with execution times 2ns, 3ns, 5ns, 4ns, and 2ns. Calculate speedup...
          </p>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-zinc-800/80">
          <div className="flex items-center gap-1 text-[11px] text-zinc-500 font-mono">
            <Clock className="w-3 h-3" />
            <span>22 hrs left</span>
          </div>
          <button className="bg-amber-500 hover:bg-amber-400 text-black px-3 py-1.5 rounded-lg text-xs font-bold transition-all shadow-md shadow-amber-500/10">
            Solve Problem
          </button>
        </div>
      </div>

      {/* 4. Spaced Repetition Due Today */}
      <div className="bg-[#161616] border border-[#262626] rounded-2xl p-4 space-y-2.5">
        <div className="flex items-center justify-between text-xs">
          <span className="font-bold text-zinc-300 uppercase tracking-wider text-[11px]">Due for Revision</span>
          <span className="text-[11px] font-mono bg-rose-500/10 text-rose-400 px-2 py-0.5 rounded-full border border-rose-500/20">
            3 Topics
          </span>
        </div>
        <div className="space-y-1.5 text-xs">
          <div className="p-2 rounded-lg bg-[#202020] border border-zinc-800/60 flex items-center justify-between">
            <span className="text-zinc-300 font-medium truncate">TCP Congestion Window</span>
            <span className="text-[10px] font-mono text-zinc-500">+7d</span>
          </div>
          <div className="p-2 rounded-lg bg-[#202020] border border-zinc-800/60 flex items-center justify-between">
            <span className="text-zinc-300 font-medium truncate">Paging & TLB Calculations</span>
            <span className="text-[10px] font-mono text-zinc-500">+3d</span>
          </div>
        </div>
      </div>
    </aside>
  );
};
