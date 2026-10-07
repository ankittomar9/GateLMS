import React from 'react';
import { Flame } from 'lucide-react';
import { useEngagementSummary } from '../../hooks/useQueries';

export const StreakChip: React.FC = () => {
  const { data: summary, isLoading } = useEngagementSummary();

  const streak = summary?.streak ?? 0;
  const isAtRisk = summary?.streakAtRisk ?? false;
  const isBroken = streak === 0 && !isAtRisk;
  const protectedDate = summary?.protectedDate;

  let tooltipText = 'Loading streak...';
  if (!isLoading) {
    if (protectedDate) {
      tooltipText = `Streak protected on ${protectedDate} (${streak} days active)`;
    } else if (isAtRisk) {
      tooltipText = `Streak at risk! Practice today to keep your ${streak}-day streak alive`;
    } else if (streak > 0) {
      tooltipText = `${streak}-day active study streak`;
    } else {
      tooltipText = 'No active streak. Complete a question today to start one!';
    }
  }

  return (
    <div
      data-testid="streak-chip"
      title={tooltipText}
      className={`flex items-center gap-1.5 px-2.5 py-1 sm:px-3 sm:py-1.5 rounded-ds text-xs font-semibold border transition-all cursor-default select-none ${
        isAtRisk
          ? 'bg-ds-surface border-ds-danger/40 text-ds-danger'
          : isBroken
          ? 'bg-ds-surface border-ds-border text-ds-muted'
          : 'bg-ds-surface border-ds-border text-ds-primary shadow-sm'
      }`}
    >
      <Flame
        data-testid={isAtRisk ? 'streak-at-risk' : isBroken ? 'streak-broken' : 'streak-active'}
        className={`w-4 h-4 transition-colors ${
          isAtRisk
            ? 'text-ds-danger animate-pulse'
            : isBroken
            ? 'text-ds-muted opacity-60'
            : 'text-ds-accent animate-pulse'
        }`}
      />
      <span className="font-mono font-bold">{streak}</span>
      <span className="text-[10px] text-ds-muted font-normal hidden sm:inline">
        {streak === 1 ? 'day' : 'days'}
      </span>
    </div>
  );
};
