import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  RotateCcw, 
  AlertCircle, 
  BookMarked, 
  Layers, 
  History, 
  Play, 
  Clock, 
  Sparkles, 
  Award,
  ArrowRight,
  RefreshCw
} from 'lucide-react';
import { revisionApi, RevisionOverviewDto, RevisionDrillSource } from '../api/revisionApi';

export const RevisionPage: React.FC = () => {
  const navigate = useNavigate();
  const [overview, setOverview] = useState<RevisionOverviewDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const fetchOverview = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await revisionApi.getOverview();
      setOverview(data);
    } catch (err: any) {
      setError(err?.message || 'Failed to load revision overview');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOverview();
  }, []);

  const handleStartDrill = async (source: RevisionDrillSource) => {
    try {
      setActionLoading(source);
      setActionError(null);
      const session = await revisionApi.createDrill({ source, count: 15 });
      sessionStorage.setItem(`practice_session_${session.attemptId}`, JSON.stringify(session));
      navigate(`/practice/runner/${session.attemptId}?drill=true&source=${source}`);
    } catch (err: any) {
      const msg = err?.detail || err?.message || 'Failed to start drill';
      setActionError(msg);
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-12 text-ds-muted text-sm" data-testid="revision-loading">
        <RefreshCw className="w-5 h-5 animate-spin mr-2" />
        <span>Loading revision cockpit...</span>
      </div>
    );
  }

  if (error && !overview) {
    return (
      <div className="max-w-4xl mx-auto p-6 space-y-4" data-testid="revision-error">
        <div className="bg-ds-danger-soft border border-ds-danger/30 text-ds-danger p-4 rounded-ds flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 flex-shrink-0" />
            <span className="text-sm font-medium">{error}</span>
          </div>
          <button
            type="button"
            onClick={fetchOverview}
            className="px-3 py-1 rounded bg-ds-danger text-ds-accent-contrast text-xs font-semibold"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const dueMistakes = overview?.dueMistakes ?? 0;
  const dueTopics = overview?.dueTopics ?? 0;
  const pass2Pending = overview?.pass2Pending ?? 0;
  const bookmarked = overview?.bookmarked ?? 0;
  const drillsCompleted = overview?.drillsCompleted ?? 0;
  const totalDue = dueMistakes + dueTopics + pass2Pending;
  const isAllClear = totalDue === 0 && bookmarked === 0;

  return (
    <div className="max-w-6xl mx-auto space-y-8 p-4 md:p-6" data-testid="revision-page">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-ds-border pb-6">
        <div>
          <h1 className="text-2xl font-bold text-ds-primary flex items-center gap-2">
            <RotateCcw className="w-6 h-6 text-ds-accent" />
            <span>Revision Cockpit</span>
          </h1>
          <p className="text-xs text-ds-muted mt-1">
            Unified forgetting-curve queue: due mistakes, spaced repetition topics, Pass-2 redemption, and bookmarks.
          </p>
        </div>
        <div className="flex items-center gap-3">
          {overview?.generatedAt && (
            <div className="text-[11px] font-mono text-ds-muted bg-ds-surface-raised px-3 py-1.5 rounded-ds border border-ds-border">
              Updated: {new Date(overview.generatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
            </div>
          )}
          <button
            type="button"
            onClick={fetchOverview}
            className="p-1.5 text-ds-muted hover:text-ds-primary border border-ds-border rounded-ds bg-ds-surface hover:bg-ds-surface-hover transition-colors"
            title="Refresh counts"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Action Error Banner */}
      {actionError && (
        <div className="bg-ds-danger-soft border border-ds-danger/30 text-ds-danger p-3 rounded-ds text-xs flex items-center justify-between" data-testid="action-error-banner">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{actionError}</span>
          </div>
          <button
            type="button"
            onClick={() => setActionError(null)}
            className="text-xs underline font-medium hover:opacity-80"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* "Due Today" Hero Banner */}
      {totalDue > 0 && (
        <div 
          className="bg-gradient-to-r from-ds-accent/15 via-ds-accent/10 to-transparent border border-ds-accent/30 rounded-ds p-6 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-sm"
          data-testid="due-today-hero"
        >
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-xs font-semibold text-ds-accent tracking-wide uppercase">
              <Sparkles className="w-4 h-4" />
              <span>Due Today</span>
            </div>
            <h2 className="text-xl font-bold text-ds-primary">
              You have <span className="text-ds-accent font-mono">{totalDue}</span> items waiting for revision
            </h2>
            <p className="text-xs text-ds-secondary max-w-xl">
              Target active recall before memory decay sets in. Start with your due mistakes or weak topics.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => handleStartDrill(dueMistakes > 0 ? 'DUE_MISTAKES' : dueTopics > 0 ? 'TOPIC' : 'PASS2')}
              disabled={actionLoading !== null}
              className="flex items-center gap-2 px-5 py-2.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-95 shadow-sm transition-opacity"
            >
              <Play className="w-4 h-4 fill-current" />
              <span>Start Priority Drill</span>
            </button>
          </div>
        </div>
      )}

      {/* All Clear Empty State */}
      {isAllClear && (
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-10 text-center space-y-4 max-w-xl mx-auto shadow-sm"
          data-testid="revision-empty-state"
        >
          <div className="w-12 h-12 rounded-full bg-ds-success-soft text-ds-success mx-auto flex items-center justify-center">
            <Award className="w-6 h-6" />
          </div>
          <div className="space-y-1">
            <h2 className="text-lg font-bold text-ds-primary">Nothing Due for Revision!</h2>
            <p className="text-xs text-ds-muted">
              All caught up! You have 0 due mistakes, 0 due topics, and 0 Pass-2 pending questions right now.
            </p>
          </div>
          <div className="pt-2">
            <button
              type="button"
              onClick={() => navigate('/mocks')}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium hover:opacity-95 transition-opacity"
            >
              <span>Take a Mock Test</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* Four Source Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4" data-testid="source-cards-grid">
        {/* 1. Due Mistakes */}
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between space-y-4 shadow-sm hover:border-ds-border-strong transition-colors"
          data-testid="source-card-due-mistakes"
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="w-9 h-9 rounded-ds bg-ds-danger-soft text-ds-danger flex items-center justify-center">
                <AlertCircle className="w-5 h-5" />
              </div>
              <span className="font-mono text-2xl font-bold text-ds-primary" data-testid="count-due-mistakes">
                {dueMistakes}
              </span>
            </div>
            <div>
              <h3 className="text-sm font-bold text-ds-primary">Due Mistakes</h3>
              <p className="text-xs text-ds-muted mt-1 leading-relaxed">
                Mistake logs whose spaced-repetition review window is currently open.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => handleStartDrill('DUE_MISTAKES')}
            disabled={actionLoading !== null || dueMistakes === 0}
            className={`w-full py-2 px-3 rounded-ds text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors ${
              dueMistakes > 0
                ? 'bg-ds-accent text-ds-accent-contrast hover:opacity-95'
                : 'bg-ds-surface-raised text-ds-muted cursor-not-allowed border border-ds-border'
            }`}
            data-testid="btn-drill-due-mistakes"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>{actionLoading === 'DUE_MISTAKES' ? 'Starting...' : 'Drill Mistakes'}</span>
          </button>
        </div>

        {/* 2. Due Topics */}
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between space-y-4 shadow-sm hover:border-ds-border-strong transition-colors"
          data-testid="source-card-due-topics"
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="w-9 h-9 rounded-ds bg-ds-warning-soft text-ds-warning flex items-center justify-center">
                <History className="w-5 h-5" />
              </div>
              <span className="font-mono text-2xl font-bold text-ds-primary" data-testid="count-due-topics">
                {dueTopics}
              </span>
            </div>
            <div>
              <h3 className="text-sm font-bold text-ds-primary">Due Topics</h3>
              <p className="text-xs text-ds-muted mt-1 leading-relaxed">
                Syllabus topics where the spaced revision interval has expired.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => handleStartDrill('TOPIC')}
            disabled={actionLoading !== null || dueTopics === 0}
            className={`w-full py-2 px-3 rounded-ds text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors ${
              dueTopics > 0
                ? 'bg-ds-accent text-ds-accent-contrast hover:opacity-95'
                : 'bg-ds-surface-raised text-ds-muted cursor-not-allowed border border-ds-border'
            }`}
            data-testid="btn-drill-due-topics"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>{actionLoading === 'TOPIC' ? 'Starting...' : 'Drill Topics'}</span>
          </button>
        </div>

        {/* 3. Pass-2 Backlog */}
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between space-y-4 shadow-sm hover:border-ds-border-strong transition-colors"
          data-testid="source-card-pass2"
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="w-9 h-9 rounded-ds bg-ds-accent-soft text-ds-accent flex items-center justify-center">
                <Layers className="w-5 h-5" />
              </div>
              <span className="font-mono text-2xl font-bold text-ds-primary" data-testid="count-pass2">
                {pass2Pending}
              </span>
            </div>
            <div>
              <h3 className="text-sm font-bold text-ds-primary">Pass-2 Redemption</h3>
              <p className="text-xs text-ds-muted mt-1 leading-relaxed">
                Questions previously FAILED or ASSISTED in Pass 1 waiting for clean redemption.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => handleStartDrill('PASS2')}
            disabled={actionLoading !== null || pass2Pending === 0}
            className={`w-full py-2 px-3 rounded-ds text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors ${
              pass2Pending > 0
                ? 'bg-ds-accent text-ds-accent-contrast hover:opacity-95'
                : 'bg-ds-surface-raised text-ds-muted cursor-not-allowed border border-ds-border'
            }`}
            data-testid="btn-drill-pass2"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>{actionLoading === 'PASS2' ? 'Starting...' : 'Drill Pass-2'}</span>
          </button>
        </div>

        {/* 4. Bookmarked Questions */}
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between space-y-4 shadow-sm hover:border-ds-border-strong transition-colors"
          data-testid="source-card-bookmarked"
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="w-9 h-9 rounded-ds bg-ds-surface-raised text-ds-secondary flex items-center justify-center border border-ds-border">
                <BookMarked className="w-5 h-5" />
              </div>
              <span className="font-mono text-2xl font-bold text-ds-primary" data-testid="count-bookmarked">
                {bookmarked}
              </span>
            </div>
            <div>
              <h3 className="text-sm font-bold text-ds-primary">Bookmarked</h3>
              <p className="text-xs text-ds-muted mt-1 leading-relaxed">
                Questions you explicitly flagged during practice or tests for deeper review.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => handleStartDrill('BOOKMARKED')}
            disabled={actionLoading !== null || bookmarked === 0}
            className={`w-full py-2 px-3 rounded-ds text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors ${
              bookmarked > 0
                ? 'bg-ds-accent text-ds-accent-contrast hover:opacity-95'
                : 'bg-ds-surface-raised text-ds-muted cursor-not-allowed border border-ds-border'
            }`}
            data-testid="btn-drill-bookmarked"
          >
            <Play className="w-3.5 h-3.5 fill-current" />
            <span>{actionLoading === 'BOOKMARKED' ? 'Starting...' : 'Drill Bookmarks'}</span>
          </button>
        </div>
      </div>

      {/* Drills Completed Stat Card */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-4 flex items-center justify-between text-xs text-ds-secondary">
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-ds-muted" />
          <span>Lifetime Weakness Drills Completed:</span>
          <span className="font-mono font-bold text-ds-primary" data-testid="drills-completed-count">{drillsCompleted}</span>
        </div>
        <button
          type="button"
          onClick={() => navigate('/questions?bookmarked=true')}
          className="text-xs text-ds-accent hover:underline flex items-center gap-1 font-medium"
        >
          <span>View All Bookmarked Questions</span>
          <ArrowRight className="w-3 h-3" />
        </button>
      </div>
    </div>
  );
};
