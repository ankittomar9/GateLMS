import React from 'react';
import { Sparkles, ArrowRight, BookOpen, Layers } from 'lucide-react';

export const HeroPlanner: React.FC = () => {
  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-[#141414] via-[#171717] to-[#121212] border border-[#262626] p-7 shadow-xl">
      {/* Decorative ambient glow */}
      <div className="absolute top-0 right-1/4 w-72 h-72 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-10 left-1/3 w-64 h-64 bg-cyan-500/5 rounded-full blur-3xl pointer-events-none" />

      <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="max-w-xl space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#202020] border border-zinc-700/60 text-[11px] font-semibold text-amber-300">
            <Sparkles className="w-3.5 h-3.5 text-amber-400" />
            <span>PLANLY — Your Personal GATE Strategy Engine</span>
          </div>

          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white leading-snug">
            Know what to study every day <br className="hidden sm:inline" />
            <span className="text-zinc-400 font-medium">and readjust as you go.</span>
          </h1>

          <p className="text-xs text-zinc-400 leading-relaxed">
            Targeting GATE 2028 with textbook rigor. Active focus: <strong className="text-zinc-200">Propositional Logic (Rosen §1.3)</strong> and <strong className="text-zinc-200">OS Process Synchronization (Galvin §6)</strong>.
          </p>

          <div className="flex items-center gap-4 pt-1 text-xs text-zinc-500 font-mono">
            <span className="flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5 text-amber-400" />
              10 Subjects Seeded
            </span>
            <span className="flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-cyan-400" />
              2,500 PYQs Trackable
            </span>
          </div>
        </div>

        {/* CTA Section */}
        <div className="shrink-0 flex flex-col sm:flex-row md:flex-col items-start md:items-end gap-3">
          <button className="flex items-center gap-2 bg-white hover:bg-zinc-200 text-black px-6 py-2.5 rounded-xl font-bold text-xs tracking-tight transition-all shadow-lg hover:shadow-white/10 group">
            <span>Resume Today's Plan</span>
            <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
          </button>
          <span className="text-[11px] text-zinc-500 font-mono">
            3 Spaced Revisions Due Today
          </span>
        </div>
      </div>
    </div>
  );
};
