import React, { useState } from 'react';
import { Info, Clock, ArrowUpRight } from 'lucide-react';
import { useDashboardLayout } from '../context/LayoutContext';

export const RightSidebar: React.FC = () => {
  const { currentLayout } = useDashboardLayout();
  const [bentoAnswer, setBentoAnswer] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<boolean>(false);

  return (
    <aside
      className="w-80 bg-ds-bg border-l border-ds-border p-5 space-y-6 shrink-0 select-none hidden xl:block min-h-screen transition-colors duration-200"
      data-testid="right-sidebar"
    >
      {/* 1. Syllabus Completion Gauge / Ring */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-bold text-xs uppercase tracking-wider text-ds-primary">
            GATE Syllabus Progress
          </h3>
          <Info className="w-3.5 h-3.5 text-ds-muted hover:text-ds-primary cursor-pointer" />
        </div>

        {/* Circular Progress Gauge */}
        <div className="flex flex-col items-center justify-center py-2">
          <div className="relative w-36 h-36 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              <circle
                cx="50"
                cy="50"
                r="40"
                className="stroke-ds-border"
                strokeWidth="8"
                fill="transparent"
              />
              <circle
                cx="50"
                cy="50"
                r="40"
                className="stroke-ds-accent transition-all duration-700 ease-out"
                strokeWidth="8"
                strokeDasharray="251.2"
                strokeDashoffset="210"
                strokeLinecap="round"
                fill="transparent"
              />
            </svg>

            <div className="absolute flex flex-col items-center text-center">
              <span className="text-2xl font-black text-ds-primary leading-none font-mono">16.5</span>
              <span className="text-[10px] text-ds-muted font-mono mt-0.5">/ 100 Marks</span>
            </div>
          </div>
        </div>

        {/* Breakdown Legend */}
        <div className="space-y-2 text-xs pt-1 border-t border-ds-border">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-ds-success" />
              <span className="text-ds-secondary">Foundation (GA + Math)</span>
            </div>
            <span className="font-mono text-ds-primary">9.0 / 29</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-ds-accent" />
              <span className="text-ds-secondary">Core Systems (OS, DBMS, CN)</span>
            </div>
            <span className="font-mono text-ds-primary">5.5 / 42</span>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-ds-info" />
              <span className="text-ds-secondary">Theory & Algorithms</span>
            </div>
            <span className="font-mono text-ds-primary">2.0 / 29</span>
          </div>
        </div>
      </div>

      {/* 2. Problem of the Day — Distraction-Free (Zero Points Badges) */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5 font-bold text-xs uppercase tracking-wider text-ds-primary">
            <span>Problem of the Day</span>
            <ArrowUpRight className="w-3.5 h-3.5 text-ds-muted" />
          </div>
          <span className="text-[10px] font-mono text-ds-muted border border-ds-border px-2 py-0.5 rounded-full">
            Daily Drill
          </span>
        </div>

        <div className="space-y-1">
          <div className="text-[11px] font-semibold text-ds-accent font-mono">
            GATE CS 2023 · NAT · 2 Marks
          </div>
          <p className="text-xs font-medium text-ds-secondary leading-relaxed">
            An instruction pipeline has 5 stages with execution times 2ns, 3ns, 5ns, 4ns, and 2ns. Calculate speedup for 1000 tasks.
          </p>
        </div>

        {/* If Bento layout, show one-click interactive answer submission */}
        {currentLayout === 'bento' ? (
          <div className="pt-2 border-t border-ds-border space-y-2">
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
                    className={`py-1.5 px-2 rounded-ds text-left text-[11px] font-mono border transition-all ${
                      isSelected
                        ? 'bg-ds-accent-soft border-ds-accent text-ds-accent font-bold'
                        : 'bg-ds-surface-raised border-ds-border text-ds-muted hover:text-ds-primary'
                    }`}
                  >
                    {opt}
                  </button>
                );
              })}
            </div>
            <div className="flex items-center justify-between pt-1">
              <div className="flex items-center gap-1 text-[11px] text-ds-muted font-mono">
                <Clock className="w-3 h-3" />
                <span>22h left</span>
              </div>
              <button
                type="button"
                disabled={!bentoAnswer || submitted}
                onClick={() => setSubmitted(true)}
                className="bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-3 py-1 rounded-ds text-xs font-bold transition-all disabled:opacity-50"
              >
                {submitted ? 'Submitted ✓' : 'Submit Answer'}
              </button>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-between pt-2 border-t border-ds-border">
            <div className="flex items-center gap-1 text-[11px] text-ds-muted font-mono">
              <Clock className="w-3 h-3" />
              <span>22 hrs left</span>
            </div>
            <button
              type="button"
              className="bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-3.5 py-1.5 rounded-ds text-xs font-bold transition-all shadow-md"
            >
              Solve Problem
            </button>
          </div>
        )}
      </div>

      {/* 3. Spaced Repetition Due Today */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-2.5">
        <div className="flex items-center justify-between text-xs">
          <span className="font-bold text-ds-primary uppercase tracking-wider text-[11px]">Due for Revision</span>
          <span className="text-[11px] font-mono bg-ds-danger/10 text-ds-danger px-2 py-0.5 rounded-full border border-ds-danger/20">
            3 Topics
          </span>
        </div>
        <div className="space-y-1.5 text-xs">
          <div className="p-2 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center justify-between">
            <span className="text-ds-secondary font-medium truncate">TCP Congestion Window</span>
            <span className="text-[10px] font-mono text-ds-muted">+7d</span>
          </div>
          <div className="p-2 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center justify-between">
            <span className="text-ds-secondary font-medium truncate">Paging & TLB Calculations</span>
            <span className="text-[10px] font-mono text-ds-muted">+3d</span>
          </div>
          <div className="p-2 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center justify-between">
            <span className="text-ds-secondary font-medium truncate">Master Theorem Cases</span>
            <span className="text-[10px] font-mono text-ds-muted">+1d</span>
          </div>
        </div>
      </div>
    </aside>
  );
};
