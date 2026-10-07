import React, { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { 
  AlertCircle, 
  CheckCircle2, 
  Clock, 
  RotateCw, 
  Check, 
  Filter, 
  Sparkles,
  BookOpen,
  Calendar,
  Layers
} from 'lucide-react';
import { useMistakes, useReviewMistake } from '../hooks/useQueries';
import { MistakeErrorCategory, MistakeDto } from '../api/types';
import { MathText } from '../components/MathText';

export const MistakeNotebookPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const dueParam = searchParams.get('due') === 'true';
  const resolvedParam = searchParams.get('resolved') === 'true';

  const [activeTab, setActiveTab] = useState<'due' | 'active' | 'resolved'>(
    dueParam ? 'due' : resolvedParam ? 'resolved' : 'active'
  );
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');
  const [reviewingId, setReviewingId] = useState<string | null>(null);

  const queryParams = {
    due: activeTab === 'due' ? true : undefined,
    resolved: activeTab === 'resolved' ? true : activeTab === 'active' || activeTab === 'due' ? false : undefined,
    page: 0,
    size: 50,
  };

  const { data: mistakePage, isLoading, refetch } = useMistakes(queryParams);
  const reviewMutation = useReviewMistake();

  const handleTabChange = (tab: 'due' | 'active' | 'resolved') => {
    setActiveTab(tab);
    if (tab === 'due') {
      setSearchParams({ due: 'true' });
    } else if (tab === 'resolved') {
      setSearchParams({ resolved: 'true' });
    } else {
      setSearchParams({});
    }
  };

  const handlePerformReview = async (id: string, resolve: boolean = false) => {
    await reviewMutation.mutateAsync({
      id,
      data: { resolved: resolve },
    });
    setReviewingId(null);
  };

  const mistakes = mistakePage?.content || [];
  const filteredMistakes = categoryFilter === 'ALL' 
    ? mistakes 
    : mistakes.filter((m) => m.errorCategory === categoryFilter);

  const getCategoryBadgeClass = (cat: MistakeErrorCategory) => {
    switch (cat) {
      case 'CONCEPTUAL':
        return 'bg-ds-danger-soft text-ds-danger border-ds-danger/30';
      case 'CALCULATION':
        return 'bg-ds-warning-soft text-ds-warning border-ds-warning/30';
      case 'MSQ_TRAP':
        return 'bg-ds-info-soft text-ds-info border-ds-info/30';
      case 'MISREAD':
        return 'bg-ds-accent-soft text-ds-accent border-ds-accent/30';
      default:
        return 'bg-ds-surface text-ds-muted border-ds-border';
    }
  };

  const formatReviewDate = (dateStr: string | null) => {
    if (!dateStr) return 'Not scheduled';
    const date = new Date(dateStr);
    const now = new Date();
    const diffHours = Math.round((date.getTime() - now.getTime()) / (1000 * 60 * 60));

    if (diffHours <= 0) return 'Due today';
    if (diffHours < 24) return `Due in ${diffHours}h`;
    const days = Math.round(diffHours / 24);
    return `Due in ${days} ${days === 1 ? 'day' : 'days'}`;
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ds-primary flex items-center gap-2.5">
            <BookOpen className="w-6 h-6 text-ds-accent" />
            <span>Mistake Notebook</span>
          </h1>
          <p className="text-sm text-ds-muted mt-1">
            Spaced repetition review queue for misconceptions, calculation slips, and MSQ traps.
          </p>
        </div>

        <button
          type="button"
          onClick={() => navigate('/practice')}
          className="flex items-center gap-2 px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-90 transition-opacity"
        >
          <Sparkles className="w-4 h-4" />
          <span>Practice More</span>
        </button>
      </div>

      {/* Tabs and Filters Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-ds-border pb-4">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => handleTabChange('due')}
            className={`px-3.5 py-1.5 rounded-ds text-xs font-medium border transition-colors ${
              activeTab === 'due'
                ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent'
                : 'bg-ds-surface border-ds-border text-ds-secondary hover:text-ds-primary'
            }`}
          >
            Due for Review
          </button>
          <button
            type="button"
            onClick={() => handleTabChange('active')}
            className={`px-3.5 py-1.5 rounded-ds text-xs font-medium border transition-colors ${
              activeTab === 'active'
                ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent'
                : 'bg-ds-surface border-ds-border text-ds-secondary hover:text-ds-primary'
            }`}
          >
            All Active
          </button>
          <button
            type="button"
            onClick={() => handleTabChange('resolved')}
            className={`px-3.5 py-1.5 rounded-ds text-xs font-medium border transition-colors ${
              activeTab === 'resolved'
                ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent'
                : 'bg-ds-surface border-ds-border text-ds-secondary hover:text-ds-primary'
            }`}
          >
            Resolved
          </button>
        </div>

        {/* Category Filter */}
        <div className="flex items-center gap-2">
          <Filter className="w-3.5 h-3.5 text-ds-muted" />
          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="bg-ds-surface border border-ds-border rounded-ds px-2.5 py-1 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
          >
            <option value="ALL">All Categories</option>
            <option value="CONCEPTUAL">Conceptual</option>
            <option value="CALCULATION">Calculation</option>
            <option value="MSQ_TRAP">MSQ Trap</option>
            <option value="MISREAD">Misread</option>
          </select>
        </div>
      </div>

      {/* Loading state */}
      {isLoading && (
        <div className="py-12 text-center text-sm text-ds-muted">Loading mistake notebook queue...</div>
      )}

      {/* Empty State */}
      {!isLoading && filteredMistakes.length === 0 && (
        <div className="bg-ds-surface border border-ds-border rounded-ds p-12 text-center space-y-3">
          <CheckCircle2 className="w-10 h-10 text-ds-success mx-auto opacity-80" />
          <h3 className="text-base font-semibold text-ds-primary">Queue is all caught up!</h3>
          <p className="text-xs text-ds-muted max-w-sm mx-auto">
            {activeTab === 'due'
              ? 'No mistakes are currently due for spaced review. Excellent consistency!'
              : 'No mistakes recorded in this category yet.'}
          </p>
        </div>
      )}

      {/* Mistake Cards List */}
      {!isLoading && filteredMistakes.length > 0 && (
        <div className="space-y-4">
          {filteredMistakes.map((m: MistakeDto) => {
            const isReviewing = reviewingId === m.id;
            return (
              <div
                key={m.id}
                className="bg-ds-surface border border-ds-border rounded-ds p-5 md:p-6 space-y-4 shadow-sm hover:border-ds-border-strong transition-colors"
              >
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ds-border pb-3">
                  <div className="flex items-center gap-2">
                    <span className={`px-2.5 py-0.5 rounded-ds text-xs font-mono font-semibold border ${getCategoryBadgeClass(m.errorCategory)}`}>
                      {m.errorCategory}
                    </span>
                    <span className="text-[11px] font-mono text-ds-muted bg-ds-surface-raised px-2 py-0.5 rounded-ds border border-ds-border">
                      Reviewed {m.reviewCount} {m.reviewCount === 1 ? 'time' : 'times'}
                    </span>
                  </div>

                  <div className="flex items-center gap-3 text-xs text-ds-secondary">
                    <div className="flex items-center gap-1 font-mono">
                      <Clock className="w-3.5 h-3.5 text-ds-muted" />
                      <span>{formatReviewDate(m.nextReviewAt)}</span>
                    </div>
                  </div>
                </div>

                {/* Question Statement Excerpt */}
                <div className="text-xs md:text-sm text-ds-primary leading-relaxed bg-ds-surface-raised/40 p-3 rounded-ds border border-ds-border">
                  <div className="text-[11px] font-semibold text-ds-muted uppercase tracking-wider mb-1">Question</div>
                  <MathText content={m.questionStatement} />
                </div>

                {/* Corrective Takeaway Card */}
                <div className="space-y-1">
                  <div className="text-[11px] font-semibold text-ds-accent uppercase tracking-wider">Corrective Takeaway</div>
                  <div className="text-xs text-ds-secondary leading-relaxed bg-ds-accent-soft/30 border border-ds-accent/20 p-3 rounded-ds">
                    {m.correctiveTakeaway}
                  </div>
                </div>

                {/* Review Flow / Actions */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-ds-border">
                  <span className="text-[11px] text-ds-muted">
                    Logged on {new Date(m.loggedAt).toLocaleDateString()}
                  </span>

                  <div className="flex items-center gap-2">
                    {!m.resolved && (
                      <button
                        type="button"
                        onClick={() => handlePerformReview(m.id, true)}
                        disabled={reviewMutation.isPending}
                        className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-xs text-ds-success font-medium transition-colors"
                      >
                        <Check className="w-3.5 h-3.5" />
                        <span>Resolve Mistake</span>
                      </button>
                    )}

                    <button
                      type="button"
                      onClick={() => handlePerformReview(m.id, false)}
                      disabled={reviewMutation.isPending}
                      className="flex items-center gap-1.5 px-4 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-90 shadow-sm transition-opacity"
                    >
                      <RotateCw className="w-3.5 h-3.5" />
                      <span>{reviewMutation.isPending ? 'Advancing...' : 'Mark Reviewed (+ Interval)'}</span>
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default MistakeNotebookPage;
