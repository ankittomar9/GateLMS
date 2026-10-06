import React, { useState } from 'react';
import { Info, Clock, ArrowUpRight, CheckCircle2 } from 'lucide-react';
import { useDashboardLayout } from '../context/LayoutContext';

export const RightSidebar: React.FC = () => {
  const { currentLayout } = useDashboardLayout();
  const [bentoAnswer, setBentoAnswer] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<boolean>(false);

  // Theme-specific styling tokens
  const themeStyles = {
    minimalist: {
      asideBg: 'bg-[#0b1329]',
      cardBg: 'bg-[#0f172a]/90',
      borderColor: 'border-slate-800',
      headerText: 'text-slate-200',
      accentColor: 'stroke-blue-500',
      accentText: 'text-blue-400',
      btnBg: 'bg-blue-600 hover:bg-blue-500 text-white',
    },
    editorial: {
      asideBg: 'bg-[#070707]',
      cardBg: 'bg-[#0d0d0d]',
      borderColor: 'border-[#1e1e1e]',
      headerText: 'text-zinc-100 font-serif',
      accentColor: 'stroke-indigo-500',
      accentText: 'text-indigo-400',
      btnBg: 'bg-indigo-600 hover:bg-indigo-500 text-white',
    },
    bento: {
      asideBg: 'bg-[#0c0c0c]',
      cardBg: 'bg-[#141414]',
      borderColor: 'border-zinc-800',
      headerText: 'text-zinc-200 font-mono',
      accentColor: 'stroke-amber-400',
      accentText: 'text-amber-400',
      btnBg: 'bg-amber-500 hover:bg-amber-400 text-black',
    },
  }[currentLayout];

  return (
    <aside
      className={`w-80 ${themeStyles.asideBg} border-l ${themeStyles.borderColor} p-5 space-y-6 shrink-0 select-none hidden xl:block min-h-screen transition-colors duration-200`}
      data-testid="right-sidebar"
    >
      {/* 1. Syllabus Completion Gauge / Ring */}
      <div className={`${themeStyles.cardBg} border ${themeStyles.borderColor} rounded-2xl p-5 space-y-4`}>
        <div className="flex items-center justify-between">
          <h3 className={`font-bold text-xs uppercase tracking-wider ${themeStyles.headerText}`}>
            GATE Syllabus Progress
          </h3>
          <Info className="w-3.5 h-3.5 text-zinc-500 hover:text-zinc-300 cursor-pointer" />
        </div>

        {/* Circular Progress Gauge */}
        <div className="flex flex-col items-center justify-center py-2">
          <div className="relative w-36 h-36 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              <circle
                cx="50"
                cy="50"
                r="40"
                className="stroke-zinc-800/80"
                strokeWidth="8"
                fill="transparent"
              />
              <circle
                cx="50"
                cy="50"
                r="40"
                className={`${themeStyles.accentColor} transition-all duration-700 ease-out`}
                strokeWidth="8"
                strokeDasharray="251.2"
                strokeDashoffset="210"
                strokeLinecap="round"
                fill="transparent"
              />
            </svg>

            <div className="absolute flex flex-col items-center text-center">
              <span className="text-2xl font-black text-white leading-none font-mono">16.5</span>
              <span className="text-[10px] text-zinc-500 font-mono mt-0.5">/ 100 Marks</span>
            </div>
          </div>
        </div>

        {/* Breakdown Legend */}
        <div className="space-y-2 text-xs pt-1 border-t border-zinc-800/60">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span className="text-zinc-400">Foundation (GA + Math)</span>
            </div>
            <span className="font-mono text-zinc-300">9.0 / 29</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-blue-400" />
              <span className="text-zinc-400">Core Systems (OS, DBMS, CN)</span>
            </div>
            <span className="font-mono text-zinc-300">5.5 / 42</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-purple-400" />
              <span className="text-zinc-400">Theory & Algorithms</span>
            </div>
            <span className="font-mono text-zinc-300">2.0 / 29</span>
          </div>
        </div>
      </div>

      {/* 2. Problem of the Day — Distraction-Free (Zero Points Badges) */}
      <div className={`${themeStyles.cardBg} border ${themeStyles.borderColor} rounded-2xl p-5 space-y-3`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 font-bold text-xs uppercase tracking-wider text-zinc-200">
            <span>Problem of the Day</span>
            <ArrowUpRight className="w-3.5 h-3.5 text-zinc-500" />
          </div>
          <span className="text-[10px] font-mono text-zinc-500 border border-zinc-800 px-2 py-0.5 rounded-full">
            Daily Drill
          </span>
        </div>

        <div className="space-y-1">
          <div className={`text-[11px] font-semibold ${themeStyles.accentText} font-mono`}>
            GATE CS 2023 · NAT · 2 Marks
          </div>
          <p className="text-xs font-medium text-zinc-300 leading-relaxed">
            An instruction pipeline has 5 stages with execution times 2ns, 3ns, 5ns, 4ns, and 2ns. Calculate speedup for 1000 tasks.
          </p>
        </div>

        {/* If Bento layout, show one-click interactive answer submission */}
        {currentLayout === 'bento' ? (
          <div className="pt-2 border-t border-zinc-800/80 space-y-2">
            <div className="grid grid-cols-2 gap-1.5 text-xs">
              {['A: 3.20', 'B: 3.85', 'C: 4.12', 'D: 4.95'].map((opt) => {
                const optKey = opt.slice(0, 1);
                const isSelected = bentoAnswer === optKey;
                return (
                  <button
                    key={optKey}
                    type="button"
                    onClick={() => {
                      setBentoAnswer(optKey);
                      setSubmitted(false);
                    }}
                    className={`py-1.5 px-2 rounded-lg text-left text-[11px] font-mono border transition-all ${
                      isSelected
                        ? 'bg-amber-500/20 border-amber-500/60 text-amber-300 font-bold'
                        : 'bg-[#181818] border-zinc-800 text-zinc-400 hover:text-zinc-200'
                    }`}
                  >
                    {opt}
                  </button>
                );
              })}
            </div>
            <div className="flex items-center justify-between pt-1">
              <div className="flex items-center gap-1 text-[11px] text-zinc-500 font-mono">
                <Clock className="w-3 h-3" />
                <span>22h left</span>
              </div>
              <button
                type="button"
                disabled={!bentoAnswer || submitted}
                onClick={() => setSubmitted(true)}
                className={`${themeStyles.btnBg} px-3 py-1 rounded-lg text-xs font-bold transition-all disabled:opacity-50`}
              >
                {submitted ? 'Submitted ✓' : 'Submit Answer'}
              </button>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-between pt-2 border-t border-zinc-800/80">
            <div className="flex items-center gap-1 text-[11px] text-zinc-500 font-mono">
              <Clock className="w-3 h-3" />
              <span>22 hrs left</span>
            </div>
            <button
              type="button"
              className={`${themeStyles.btnBg} px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all shadow-md`}
            >
              Solve Problem
            </button>
          </div>
        )}
      </div>

      {/* 3. Spaced Repetition Due Today */}
      <div className={`${themeStyles.cardBg} border ${themeStyles.borderColor} rounded-2xl p-4 space-y-2.5`}>
        <div className="flex items-center justify-between text-xs">
          <span className="font-bold text-zinc-300 uppercase tracking-wider text-[11px]">Due for Revision</span>
          <span className="text-[11px] font-mono bg-rose-500/10 text-rose-400 px-2 py-0.5 rounded-full border border-rose-500/20">
            3 Topics
          </span>
        </div>
        <div className="space-y-1.5 text-xs">
          <div className="p-2 rounded-lg bg-[#181818] border border-zinc-800/60 flex items-center justify-between">
            <span className="text-zinc-300 font-medium truncate">TCP Congestion Window</span>
            <span className="text-[10px] font-mono text-zinc-500">+7d</span>
          </div>
          <div className="p-2 rounded-lg bg-[#181818] border border-zinc-800/60 flex items-center justify-between">
            <span className="text-zinc-300 font-medium truncate">Paging & TLB Calculations</span>
            <span className="text-[10px] font-mono text-zinc-500">+3d</span>
          </div>
          <div className="p-2 rounded-lg bg-[#181818] border border-zinc-800/60 flex items-center justify-between">
            <span className="text-zinc-300 font-medium truncate">Master Theorem Cases</span>
            <span className="text-[10px] font-mono text-zinc-500">+1d</span>
          </div>
        </div>
      </div>
    </aside>
  );
};
