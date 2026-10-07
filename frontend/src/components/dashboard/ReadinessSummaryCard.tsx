import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Activity, Calendar, AlertCircle, ArrowRight } from 'lucide-react';
import { readinessApi, ReadinessResponse, ExamCountdown } from '../../api/readinessApi';
import { revisionApi } from '../../api/revisionApi';

export const ReadinessSummaryCard: React.FC = () => {
  const navigate = useNavigate();
  const [readiness, setReadiness] = useState<ReadinessResponse | null>(null);
  const [countdown, setCountdown] = useState<ExamCountdown | null>(null);
  const [dueCount, setDueCount] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        const [r, c, rev] = await Promise.all([
          readinessApi.getReadiness(),
          readinessApi.getCountdown(),
          revisionApi.getOverview().catch(() => null),
        ]);
        if (mounted) {
          setReadiness(r);
          setCountdown(c);
          if (rev) {
            setDueCount((rev.dueMistakes || 0) + (rev.dueTopics || 0));
          }
        }
      } catch {
        // Fallback to honest zero state on network failure
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const score = readiness?.readiness ?? 0;
  // Circumference of circle with r=38 is 2 * PI * 38 ≈ 238.76
  const strokeDashoffset = 238.76 - (238.76 * Math.min(score, 100)) / 100;

  return (
    <div 
      className="bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-4"
      data-testid="readiness-summary-card"
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-ds bg-ds-accent-soft text-ds-accent">
            <Activity className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-ds-primary font-mono">
              Readiness & Countdown
            </h3>
            <span className="text-[11px] text-ds-muted">GATE CSE 2028 Study Intelligence</span>
          </div>
        </div>

        {/* Due for revision Count Chip */}
        <button
          type="button"
          onClick={() => navigate('/revision')}
          className="flex items-center gap-1.5 text-[11px] font-mono px-2.5 py-1 rounded-full border border-ds-warning/40 bg-ds-warning/10 text-ds-warning hover:bg-ds-warning/20 transition-all cursor-pointer"
          title="Items due for revision (due mistakes + due topics)"
          data-testid="due-this-week-chip"
        >
          <AlertCircle className="w-3.5 h-3.5" />
          <span>{loading ? '...' : `${dueCount} Due for revision`}</span>
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center">
        {/* Readiness Circular Ring */}
        <div className="flex items-center gap-4 bg-ds-surface-raised p-3.5 rounded-ds border border-ds-border">
          <div className="relative w-16 h-16 shrink-0 flex items-center justify-center">
            <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
              <circle
                cx="50"
                cy="50"
                r="38"
                className="stroke-ds-border"
                strokeWidth="10"
                fill="transparent"
              />
              <circle
                cx="50"
                cy="50"
                r="38"
                className="stroke-ds-accent transition-all duration-700 ease-out"
                strokeWidth="10"
                strokeDasharray="238.76"
                strokeDashoffset={strokeDashoffset}
                strokeLinecap="round"
                fill="transparent"
              />
            </svg>
            <div className="absolute flex flex-col items-center text-center">
              <span className="text-sm font-black text-ds-primary font-mono">
                {loading ? '--' : `${score}%`}
              </span>
            </div>
          </div>
          <div className="space-y-0.5">
            <div className="text-xs font-bold text-ds-primary">GATE Readiness</div>
            <p className="text-[11px] text-ds-muted leading-tight font-mono">
              Honest aggregate: 0.5M + 0.3A + 0.2C
            </p>
            <button
              type="button"
              onClick={() => navigate('/insights')}
              className="text-[11px] text-ds-accent hover:underline font-semibold flex items-center gap-1 pt-0.5"
            >
              View Analytics <ArrowRight className="w-3 h-3" />
            </button>
          </div>
        </div>

        {/* Exam Countdown Card */}
        <div 
          className="flex items-center gap-3.5 bg-ds-surface-raised p-3.5 rounded-ds border border-ds-border"
          data-testid="dashboard-countdown-card"
        >
          <div className="w-10 h-10 rounded-ds bg-ds-accent-soft border border-ds-accent/30 flex items-center justify-center text-ds-accent shrink-0">
            <Calendar className="w-5 h-5" />
          </div>
          <div className="space-y-0.5">
            <div className="text-[10px] uppercase font-mono text-ds-muted">Target Exam</div>
            <div className="text-xs font-bold text-ds-primary font-mono">
              {countdown?.examDate ?? '2028-02-12'}
            </div>
            <div className="text-[11px] text-ds-accent font-semibold font-mono">
              {loading ? 'Calculating...' : `${countdown?.daysRemaining ?? 0} days (${countdown?.weeksRemaining ?? 0} wks)`}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReadinessSummaryCard;
