import React from 'react';
import { Sparkles, ArrowRight, BookOpen, Layers } from 'lucide-react';

export const HeroPlanner: React.FC = () => {
  return (
    <div className="relative overflow-hidden rounded-ds bg-ds-surface border border-ds-border p-7 shadow-sm">
      <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="max-w-xl space-y-3">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-ds-surface-raised border border-ds-border text-[11px] font-semibold text-ds-accent">
            <Sparkles className="w-3.5 h-3.5 text-ds-accent" />
            <span>PLANLY — Your Personal GATE Strategy Engine</span>
          </div>

          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-ds-primary leading-snug">
            Know what to study every day <br className="hidden sm:inline" />
            <span className="text-ds-secondary font-medium">and readjust as you go.</span>
          </h1>

          <p className="text-xs text-ds-secondary leading-relaxed">
            Targeting GATE 2028 with textbook rigor. Active focus: <strong className="text-ds-primary">Propositional Logic (Rosen §1.3)</strong> and <strong className="text-ds-primary">OS Process Synchronization (Galvin §6)</strong>.
          </p>

          <div className="flex items-center gap-4 pt-1 text-xs text-ds-muted font-mono">
            <span className="flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5 text-ds-accent" />
              10 Subjects Seeded
            </span>
            <span className="flex items-center gap-1.5">
              <Layers className="w-3.5 h-3.5 text-ds-info" />
              2,500 PYQs Trackable
            </span>
          </div>
        </div>

        {/* CTA Section */}
        <div className="shrink-0 flex flex-col sm:flex-row md:flex-col items-start md:items-end gap-3">
          <button className="flex items-center gap-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-6 py-2.5 rounded-ds font-bold text-xs tracking-tight transition-all shadow-md group">
            <span>Resume Today's Plan</span>
            <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
          </button>
          <span className="text-[11px] text-ds-muted font-mono">
            3 Spaced Revisions Due Today
          </span>
        </div>
      </div>
    </div>
  );
};
