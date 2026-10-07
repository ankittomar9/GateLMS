import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FileClock, ArrowRight, CheckCircle2, AlertCircle, Play } from 'lucide-react';
import { usePypStats, useGeneratePypMock } from '../../hooks/useQueries';
import { apiClient } from '../../api/apiClient';
import { StartMockAttemptResponse } from '../../api/types';

export const PypDashboardTile: React.FC = () => {
  const navigate = useNavigate();
  const { data: stats, isLoading } = usePypStats();
  const generatePypMock = useGeneratePypMock();
  const [isAttempting, setIsAttempting] = useState(false);

  // Derive latest year
  const latestYear = React.useMemo(() => {
    if (!stats?.years || stats.years.length === 0) return null;
    return [...stats.years].sort((a, b) => b.year - a.year)[0];
  }, [stats?.years]);

  const isFullPaper = latestYear
    ? stats?.coverage?.fullPaperYears?.includes(latestYear.year)
    : false;

  const handleAttemptLatest = async () => {
    if (!latestYear) {
      navigate('/pyp');
      return;
    }

    setIsAttempting(true);
    try {
      const template = await generatePypMock.mutateAsync(latestYear.year);
      const attempt = await apiClient.post<StartMockAttemptResponse>(`/mocks/${template.id}/attempts`);
      sessionStorage.setItem(`mock_attempt_${attempt.attemptId}`, JSON.stringify(attempt));
      navigate(`/mocks/attempts/${attempt.attemptId}`);
    } catch (err) {
      console.error('Failed to start latest PYP paper attempt:', err);
      navigate('/pyp');
    } finally {
      setIsAttempting(false);
    }
  };

  return (
    <div
      data-testid="pyp-dashboard-tile"
      className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between space-y-4 shadow-sm"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-ds bg-ds-accent-soft text-ds-accent">
            <FileClock className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-ds-primary font-mono">
              Latest GATE PYP Paper
            </h3>
            <span className="text-[11px] text-ds-muted">Official previous-year exam simulator</span>
          </div>
        </div>
        {latestYear && (
          <span className="text-[11px] font-mono px-2 py-0.5 rounded-full bg-ds-surface-raised border border-ds-border text-ds-secondary">
            GATE {latestYear.year}
          </span>
        )}
      </div>

      <div className="space-y-2">
        {isLoading ? (
          <div className="h-10 bg-ds-surface-raised rounded-ds animate-pulse" />
        ) : latestYear ? (
          <div className="flex items-center justify-between bg-ds-surface-raised p-3 rounded-ds border border-ds-border">
            <div>
              <div className="text-sm font-bold text-ds-primary">
                GATE {latestYear.year} CSE
              </div>
              <div className="text-xs text-ds-muted font-mono">
                {latestYear.total} Questions in Bank · 100% Attribution
              </div>
            </div>
            {isFullPaper ? (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold border text-ds-success bg-ds-success/10 border-ds-success/30 flex items-center gap-1">
                <CheckCircle2 className="w-3 h-3" />
                Full 65-Q
              </span>
            ) : (
              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold border text-ds-warning bg-ds-warning/10 border-ds-warning/30 flex items-center gap-1">
                <AlertCircle className="w-3 h-3" />
                Partial
              </span>
            )}
          </div>
        ) : (
          <div className="text-xs text-ds-muted italic py-2">
            No previous year papers available.
          </div>
        )}
      </div>

      <div className="flex items-center gap-2 pt-1">
        <button
          type="button"
          data-testid="attempt-latest-pyp-btn"
          disabled={!latestYear || isAttempting}
          onClick={handleAttemptLatest}
          className="flex-1 flex items-center justify-center gap-1.5 py-2 px-3 rounded-ds bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-semibold disabled:opacity-50 transition-colors shadow-sm"
        >
          <Play className="w-3.5 h-3.5 fill-current" />
          <span>{isAttempting ? 'Starting...' : 'Attempt latest paper'}</span>
        </button>

        <button
          type="button"
          onClick={() => navigate('/pyp')}
          className="py-2 px-3 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-xs font-medium text-ds-primary transition-colors flex items-center gap-1"
        >
          <span>All Papers</span>
          <ArrowRight className="w-3.5 h-3.5 text-ds-muted" />
        </button>
      </div>
    </div>
  );
};
