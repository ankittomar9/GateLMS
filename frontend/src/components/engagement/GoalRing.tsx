import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Target, CheckCircle2, ArrowRight } from 'lucide-react';
import { useEngagementSummary } from '../../hooks/useQueries';

interface GoalRingProps {
  className?: string;
}

export const GoalRing: React.FC<GoalRingProps> = ({ className = '' }) => {
  const navigate = useNavigate();
  const { data: summary, isLoading } = useEngagementSummary();

  const goal = summary?.goal;
  const target = goal?.target ?? 20;
  const done = goal?.done ?? 0;
  const isMet = goal?.met ?? false;

  const progressPct = target > 0 ? Math.min(100, Math.round((done / target) * 100)) : 0;
  
  // Circumference with r = 38 is 2 * PI * 38 ≈ 238.76
  const strokeDashoffset = 238.76 - (238.76 * progressPct) / 100;

  return (
    <div
      data-testid="goal-ring"
      className={`bg-ds-surface border rounded-ds p-5 shadow-sm flex flex-col sm:flex-row items-center justify-between gap-5 transition-all ${
        isMet ? 'border-ds-success/40' : 'border-ds-border'
      } ${className}`}
    >
      <div className="flex items-center gap-5 w-full sm:w-auto">
        {/* Circular Gauge */}
        <div className="relative w-20 h-20 sm:w-24 sm:h-24 shrink-0 flex items-center justify-center">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
            <circle
              cx="50"
              cy="50"
              r="38"
              className="stroke-ds-surface-raised"
              strokeWidth="8"
              fill="transparent"
            />
            <circle
              cx="50"
              cy="50"
              r="38"
              className={`${
                isMet ? 'stroke-ds-success' : 'stroke-ds-accent'
              } transition-all duration-700 ease-out`}
              strokeWidth="8"
              strokeDasharray="238.76"
              strokeDashoffset={isLoading ? 238.76 : strokeDashoffset}
              strokeLinecap="round"
              fill="transparent"
            />
          </svg>
          <div className="absolute flex flex-col items-center text-center">
            <span data-testid="goal-progress-pct" className="text-sm sm:text-base font-bold font-mono text-ds-primary">
              {progressPct}%
            </span>
            <span className="text-[9px] text-ds-muted font-mono uppercase tracking-wider">
              {isMet ? 'Met' : 'Done'}
            </span>
          </div>
        </div>

        {/* Text Info */}
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-ds-primary tracking-wide flex items-center gap-1.5">
              <Target className="w-3.5 h-3.5 text-ds-accent" />
              Daily Practice Goal
            </span>
            {isMet && (
              <span
                data-testid="goal-met-badge"
                className="inline-flex items-center gap-1 text-[10px] px-2 py-0.5 rounded-full bg-ds-success/15 border border-ds-success/30 text-ds-success font-semibold font-mono"
              >
                <CheckCircle2 className="w-3 h-3" />
                Target Met!
              </span>
            )}
          </div>
          <p className="text-xs text-ds-secondary">
            <span data-testid="goal-done-count" className="font-bold font-mono text-ds-primary">
              {done}
            </span>
            {' / '}
            <span data-testid="goal-target-count" className="font-mono text-ds-muted">
              {target}
            </span>
            {' questions completed today'}
          </p>
          <p className="text-[11px] text-ds-muted">
            {isMet
              ? 'Great work! Keep pushing your boundaries with more PYQs.'
              : `${Math.max(0, target - done)} more question${target - done === 1 ? '' : 's'} needed to maintain your daily pace.`}
          </p>
        </div>
      </div>

      {/* CTA Button */}
      <button
        type="button"
        data-testid="goal-practice-cta"
        onClick={() => navigate('/practice')}
        className={`w-full sm:w-auto shrink-0 flex items-center justify-center gap-2 px-4 py-2 rounded-ds text-xs font-bold transition-all shadow-sm ${
          isMet
            ? 'bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-primary border border-ds-border'
            : 'bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast shadow-ds-accent/10'
        }`}
      >
        <span>{isMet ? 'Extra Practice' : 'Practice Now'}</span>
        <ArrowRight className="w-3.5 h-3.5" />
      </button>
    </div>
  );
};
