import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Calendar, 
  CheckCircle2, 
  Circle, 
  ArrowRight, 
  Sparkles, 
  Clock, 
  BookOpen, 
  AlertCircle,
  Play
} from 'lucide-react';
import { useTodayPlan, useUpdatePlanItemStatus } from '../../hooks/useQueries';
import { PlanItemDto } from '../../api/types';

export const TodayCard: React.FC = () => {
  const navigate = useNavigate();
  const { data, isLoading, error } = useTodayPlan();
  const updateStatusMutation = useUpdatePlanItemStatus();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  if (isLoading) {
    return (
      <div 
        data-testid="today-card-loading" 
        className="rounded-ds bg-ds-surface border border-ds-border p-6 shadow-sm animate-pulse space-y-4"
      >
        <div className="h-6 bg-ds-surface-raised rounded w-1/3" />
        <div className="h-4 bg-ds-surface-raised rounded w-2/3" />
        <div className="h-16 bg-ds-surface-raised rounded" />
      </div>
    );
  }

  const items = data?.items || [];
  const targetQuestions = data?.targetQuestions || 0;
  const doneQuestions = data?.doneQuestions || 0;
  const minutesTarget = data?.minutesTarget || 0;
  const minutesDone = data?.minutesDone || 0;

  const progressPct = targetQuestions > 0 
    ? Math.min(100, Math.round((doneQuestions / targetQuestions) * 100)) 
    : 0;

  const allDone = items.length > 0 && items.every((i) => i.status === 'DONE');

  const handleToggleStatus = async (item: PlanItemDto) => {
    setErrorMessage(null);
    const nextStatus = item.status === 'DONE' ? 'OPEN' : 'DONE';
    try {
      await updateStatusMutation.mutateAsync({
        itemId: item.id,
        req: { status: nextStatus },
      });
    } catch (err: any) {
      setErrorMessage(err.message || 'Failed to update item status');
    }
  };

  const handleDrill = (item: PlanItemDto) => {
    if (item.topicId) {
      navigate(`/practice?topic=${item.topicId}`);
    } else if (item.subjectId) {
      navigate(`/practice?subject=${item.subjectId}`);
    } else {
      navigate('/practice');
    }
  };

  if (!items || items.length === 0) {
    return (
      <div 
        data-testid="today-card" 
        className="relative overflow-hidden rounded-ds bg-ds-surface border border-ds-border p-7 shadow-sm"
      >
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6" data-testid="today-empty-state">
          <div className="max-w-xl space-y-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-ds-surface-raised border border-ds-border text-[11px] font-semibold text-ds-accent">
              <Calendar className="w-3.5 h-3.5 text-ds-accent" />
              <span>Study Planner · Today</span>
            </div>
            <h2 className="text-xl md:text-2xl font-bold tracking-tight text-ds-primary leading-snug">
              No study plan scheduled for today
            </h2>
            <p className="text-xs text-ds-secondary leading-relaxed">
              Generate a syllabus-weighted study plan to receive structured daily target questions, textbook reading chapters, and automatic progress tracking.
            </p>
          </div>

          <div className="shrink-0">
            <button
              type="button"
              data-testid="generate-plan-cta"
              onClick={() => navigate('/planner')}
              className="flex items-center gap-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-5 py-2.5 rounded-ds font-bold text-xs tracking-tight transition-all shadow-md group"
            >
              <span>Generate a Plan</span>
              <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div 
      data-testid="today-card" 
      className="relative overflow-hidden rounded-ds bg-ds-surface border border-ds-border p-6 shadow-sm space-y-5"
    >
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 border-b border-ds-border pb-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Calendar className="w-4 h-4 text-ds-accent" />
            <h2 className="text-base font-bold text-ds-primary tracking-tight">Today's Study Targets</h2>
            {allDone && (
              <span 
                data-testid="today-all-done" 
                className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-ds-success/10 text-ds-success border border-ds-success/30 flex items-center gap-1"
              >
                <CheckCircle2 className="w-3 h-3" />
                All Completed
              </span>
            )}
          </div>
          <p className="text-xs text-ds-secondary">
            {items.length} planned topic {items.length === 1 ? 'task' : 'tasks'} for today. Practice activity updates progress automatically.
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/planner')}
          className="text-xs text-ds-accent hover:text-ds-primary hover:underline font-medium flex items-center gap-1 transition-colors"
        >
          <span>Open Full Planner</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>

      {errorMessage && (
        <div 
          data-testid="today-error-banner"
          className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger px-3 py-2 rounded-ds text-xs flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-ds-danger shrink-0" />
            <span>{errorMessage}</span>
          </div>
          <button 
            type="button" 
            onClick={() => setErrorMessage(null)} 
            className="text-xs underline hover:text-ds-primary"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Progress Bar Summary */}
      <div data-testid="today-progress" className="space-y-2 bg-ds-surface-raised p-4 rounded-ds border border-ds-border">
        <div className="flex items-center justify-between text-xs font-mono">
          <div className="flex items-center gap-3">
            <span className="text-ds-primary font-bold">{doneQuestions} / {targetQuestions} Questions</span>
            <span className="text-ds-muted">·</span>
            <span className="text-ds-secondary flex items-center gap-1">
              <Clock className="w-3.5 h-3.5 text-ds-muted" />
              {minutesDone} / {minutesTarget} Mins
            </span>
          </div>
          <span className="text-ds-accent font-semibold">{progressPct}% Complete</span>
        </div>
        <div className="w-full h-2 bg-ds-surface rounded-full overflow-hidden border border-ds-border">
          <div 
            className="h-full bg-ds-accent rounded-full transition-all duration-300" 
            style={{ width: `${progressPct}%` }} 
          />
        </div>
      </div>

      {/* Items List */}
      <div data-testid="today-items-list" className="space-y-2.5">
        {items.map((item) => {
          const isDone = item.status === 'DONE';
          return (
            <div 
              key={item.id} 
              data-testid={`today-item-${item.id}`}
              className={`p-3.5 rounded-ds border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                isDone 
                  ? 'bg-ds-surface-raised/50 border-ds-border opacity-85' 
                  : 'bg-ds-surface border-ds-border hover:border-ds-border-strong'
              }`}
            >
              <div className="flex items-start gap-3">
                <button
                  type="button"
                  data-testid={`today-status-toggle-${item.id}`}
                  onClick={() => handleToggleStatus(item)}
                  disabled={updateStatusMutation.isPending}
                  className="mt-0.5 text-ds-muted hover:text-ds-accent transition-colors disabled:opacity-50 shrink-0"
                  title={isDone ? 'Mark as open' : 'Mark as done'}
                >
                  {isDone ? (
                    <CheckCircle2 className="w-4 h-4 text-ds-success" />
                  ) : (
                    <Circle className="w-4 h-4 text-ds-muted" />
                  )}
                </button>

                <div className="space-y-1">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-ds-accent-soft text-ds-accent border border-ds-accent/30">
                      {item.subjectCode || item.subjectName || 'GATE'}
                    </span>
                    <h3 className={`text-sm font-semibold tracking-tight ${isDone ? 'line-through text-ds-muted' : 'text-ds-primary'}`}>
                      {item.topicName || 'General Topic Practice'}
                    </h3>
                  </div>
                  <div className="flex items-center gap-3 text-xs font-mono text-ds-muted">
                    <span>Target: {item.targetQuestions} Qs ({item.targetMinutes}m)</span>
                    <span>·</span>
                    <span className={item.doneQuestions >= item.targetQuestions ? 'text-ds-success font-semibold' : 'text-ds-secondary'}>
                      Done: {item.doneQuestions} Qs ({item.doneMinutes}m)
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                <button
                  type="button"
                  data-testid={`today-drill-cta-${item.id}`}
                  onClick={() => handleDrill(item)}
                  className="flex items-center gap-1.5 px-3 py-1.5 bg-ds-accent-soft hover:bg-ds-accent text-ds-accent hover:text-ds-accent-contrast rounded-ds text-xs font-semibold transition-all border border-ds-accent/30 shadow-sm"
                >
                  <Play className="w-3 h-3" />
                  <span>Drill</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
