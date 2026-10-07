import React from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipboardCheck, ArrowRight } from 'lucide-react';
import { useCurationStats } from '../../hooks/useQueries';

export const CurationProgressTile: React.FC = () => {
  const navigate = useNavigate();
  const { data: stats, isLoading } = useCurationStats();

  const total = stats?.totalQuestions ?? 0;
  const accepted = stats?.byStatus?.ACCEPTED ?? 0;
  const unreviewed = stats?.byStatus?.UNREVIEWED ?? 0;
  const percent = total > 0 ? Math.round((accepted / total) * 100) : 0;

  return (
    <div
      data-testid="curation-progress-tile"
      className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between space-y-4 shadow-sm"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-ds bg-ds-accent-soft text-ds-accent">
            <ClipboardCheck className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-ds-primary font-mono">
              Curation Queue
            </h3>
            <span className="text-[11px] text-ds-muted">Question bank quality control</span>
          </div>
        </div>
        <span className="text-[11px] font-mono px-2 py-0.5 rounded-full bg-ds-surface-raised border border-ds-border text-ds-secondary">
          {isLoading ? '...' : `${unreviewed} pending`}
        </span>
      </div>

      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs font-mono">
          <span className="text-ds-secondary">Reviewed & Accepted</span>
          <span className="text-ds-primary font-semibold">
            {accepted} / {total} ({percent}%)
          </span>
        </div>
        <div className="w-full h-1.5 bg-ds-surface-raised rounded-full overflow-hidden">
          <div
            className="h-full bg-ds-accent transition-all duration-500 rounded-full"
            style={{ width: `${Math.min(percent, 100)}%` }}
          />
        </div>
      </div>

      <button
        type="button"
        onClick={() => navigate('/curation')}
        className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-xs font-medium text-ds-primary transition-colors"
      >
        <span>Open Curation Queue</span>
        <ArrowRight className="w-3.5 h-3.5 text-ds-muted" />
      </button>
    </div>
  );
};
