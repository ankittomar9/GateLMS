import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { 
  ClipboardCheck, 
  Search, 
  Check, 
  X, 
  ExternalLink, 
  MessageSquare, 
  ChevronLeft, 
  ChevronRight,
  Filter,
  Layers,
  Sparkles,
  CheckCircle2,
  XCircle,
  Clock
} from 'lucide-react';
import { 
  useQuestions, 
  useSubjects, 
  useUpdateCurationStatus, 
  useCurationStats 
} from '../hooks/useQueries';
import { 
  CurationStatus, 
  QuestionSource, 
  QuestionDifficulty, 
  QuestionType,
  QuestionSummary 
} from '../api/types';
import { MathText } from '../components/MathText';

export const CurationQueuePage: React.FC = () => {
  // Filter States
  const [subjectId, setSubjectId] = useState<string>('');
  const [curationStatus, setCurationStatus] = useState<CurationStatus | ''>('UNREVIEWED');
  const [source, setSource] = useState<QuestionSource | ''>('');
  const [difficulty, setDifficulty] = useState<QuestionDifficulty | ''>('');
  const [searchInput, setSearchInput] = useState<string>('');
  const [page, setPage] = useState<number>(0);

  // Note expansion per question ID
  const [expandedNotes, setExpandedNotes] = useState<Record<string, string>>({});
  const [showNoteInput, setShowNoteInput] = useState<Record<string, boolean>>({});

  // Bulk selection
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());
  const [bulkNote, setBulkNote] = useState<string>('');
  const [bulkProcessing, setBulkProcessing] = useState<boolean>(false);

  // Queries
  const { data: subjects } = useSubjects();
  const { data: curationStats, refetch: refetchStats } = useCurationStats();

  const queryFilters = useMemo(() => ({
    q: searchInput.trim() || undefined,
    subjectId: subjectId || undefined,
    curationStatus: (curationStatus || undefined) as CurationStatus | undefined,
    source: (source || undefined) as QuestionSource | undefined,
    difficulty: (difficulty || undefined) as QuestionDifficulty | undefined,
    page,
    size: 15,
  }), [searchInput, subjectId, curationStatus, source, difficulty, page]);

  const { data, isLoading, refetch } = useQuestions(queryFilters);
  const updateCuration = useUpdateCurationStatus();

  // Error state for mutations
  const [actionError, setActionError] = useState<string | null>(null);

  // Reset page when filters change
  const handleFilterChange = (setter: (val: any) => void, val: any) => {
    setter(val);
    setPage(0);
    setSelectedIds(new Set());
  };

  const handleAction = async (id: string, status: CurationStatus) => {
    setActionError(null);
    try {
      const note = expandedNotes[id] || undefined;
      await updateCuration.mutateAsync({ id, status, note });
      refetchStats();
    } catch (err: any) {
      setActionError(err.message || 'Failed to update curation status');
    }
  };

  // Bulk actions
  const handleSelectAll = () => {
    if (!data?.content) return;
    if (selectedIds.size === data.content.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(data.content.map((q) => q.id)));
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleBulkAction = async (status: CurationStatus) => {
    if (selectedIds.size === 0) return;
    setBulkProcessing(true);
    setActionError(null);
    try {
      for (const id of Array.from(selectedIds)) {
        await updateCuration.mutateAsync({ id, status, note: bulkNote || undefined });
      }
      setSelectedIds(new Set());
      setBulkNote('');
      refetch();
      refetchStats();
    } catch (err: any) {
      setActionError(err.message || 'Failed to execute bulk curation action');
    } finally {
      setBulkProcessing(false);
    }
  };

  // Keyboard navigation
  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      // Don't trigger if user is typing in input or textarea
      const target = e.target as HTMLElement;
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') return;

      const visible = data?.content || [];
      if (visible.length === 0) return;
      const first = visible[0];

      if (e.key === 'a' || e.key === 'A') {
        e.preventDefault();
        handleAction(first.id, 'ACCEPTED');
      } else if (e.key === 'r' || e.key === 'R') {
        e.preventDefault();
        handleAction(first.id, 'REJECTED');
      } else if (e.key === 'n' || e.key === 'N') {
        e.preventDefault();
        setShowNoteInput((prev) => ({ ...prev, [first.id]: !prev[first.id] }));
      }
    },
    [data?.content, expandedNotes]
  );

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  const getStatusBadge = (st?: CurationStatus) => {
    switch (st) {
      case 'ACCEPTED':
        return 'text-ds-success bg-ds-success/10 border-ds-success/30';
      case 'REJECTED':
        return 'text-ds-danger bg-ds-danger/10 border-ds-danger/30';
      case 'UNREVIEWED':
      default:
        return 'text-ds-warning bg-ds-warning/10 border-ds-warning/30';
    }
  };

  return (
    <div className="space-y-6 pb-12" data-testid="curation-queue-page">
      {/* Top Banner & Curation Stats */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-ds-card">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <ClipboardCheck className="w-5 h-5 text-ds-accent" />
              <h1 className="text-xl font-bold text-ds-primary tracking-tight">Question Curation Queue</h1>
            </div>
            <p className="text-ds-secondary text-sm max-w-2xl">
              Inspect, accept, reject, or annotate questions imported from Stack Overflow and synthetic banks.
              Use keyboard shortcuts: <kbd className="px-1.5 py-0.5 rounded bg-ds-surface-raised border border-ds-border text-[11px] font-mono">A</kbd> Accept, <kbd className="px-1.5 py-0.5 rounded bg-ds-surface-raised border border-ds-border text-[11px] font-mono">R</kbd> Reject, <kbd className="px-1.5 py-0.5 rounded bg-ds-surface-raised border border-ds-border text-[11px] font-mono">N</kbd> Note.
            </p>
          </div>

          {/* Quick Counter Grid */}
          <div className="flex items-center gap-3">
            <div className="bg-ds-surface-raised border border-ds-border px-3.5 py-2 rounded-ds text-center min-w-[70px]">
              <span className="text-[10px] text-ds-muted uppercase font-mono block">Total</span>
              <strong className="text-xs text-ds-primary font-bold">{curationStats?.totalQuestions ?? '...'}</strong>
            </div>
            <div className="bg-ds-surface-raised border border-ds-warning/30 px-3.5 py-2 rounded-ds text-center min-w-[70px]">
              <span className="text-[10px] text-ds-warning uppercase font-mono block">Pending</span>
              <strong className="text-xs text-ds-warning font-bold">{curationStats?.byStatus?.UNREVIEWED ?? 0}</strong>
            </div>
            <div className="bg-ds-surface-raised border border-ds-success/30 px-3.5 py-2 rounded-ds text-center min-w-[70px]">
              <span className="text-[10px] text-ds-success uppercase font-mono block">Accepted</span>
              <strong className="text-xs text-ds-success font-bold">{curationStats?.byStatus?.ACCEPTED ?? 0}</strong>
            </div>
            <div className="bg-ds-surface-raised border border-ds-danger/30 px-3.5 py-2 rounded-ds text-center min-w-[70px]">
              <span className="text-[10px] text-ds-danger uppercase font-mono block">Rejected</span>
              <strong className="text-xs text-ds-danger font-bold">{curationStats?.byStatus?.REJECTED ?? 0}</strong>
            </div>
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {actionError && (
        <div
          data-testid="curation-error-banner"
          className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger p-3.5 rounded-ds text-xs flex items-center justify-between"
        >
          <span>{actionError}</span>
          <button
            type="button"
            onClick={() => setActionError(null)}
            className="text-xs hover:underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-4 shadow-ds-card">
        {/* Search Input */}
        <div className="relative">
          <Search className="w-4 h-4 text-ds-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Filter questions by keywords, formulas, or topic..."
            value={searchInput}
            onChange={(e) => handleFilterChange(setSearchInput, e.target.value)}
            className="w-full bg-ds-surface-input border border-ds-border rounded-ds pl-10 pr-4 py-2 text-sm text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent/60 transition-all"
          />
        </div>

        {/* Filter Dropdowns */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          {/* Status Filter */}
          <select
            data-testid="curation-status-filter"
            value={curationStatus}
            onChange={(e) => handleFilterChange(setCurationStatus, e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Curation Statuses</option>
            <option value="UNREVIEWED">UNREVIEWED (Pending)</option>
            <option value="ACCEPTED">ACCEPTED</option>
            <option value="REJECTED">REJECTED</option>
          </select>

          {/* Subject Filter */}
          <select
            value={subjectId}
            onChange={(e) => handleFilterChange(setSubjectId, e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Subjects</option>
            {subjects?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>

          {/* Difficulty Filter */}
          <select
            value={difficulty}
            onChange={(e) => handleFilterChange(setDifficulty, e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Difficulties</option>
            <option value="EASY">EASY</option>
            <option value="MEDIUM">MEDIUM</option>
            <option value="HARD">HARD</option>
          </select>

          {/* Source Filter */}
          <select
            value={source}
            onChange={(e) => handleFilterChange(setSource, e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Sources</option>
            <option value="SO_DERIVED">Stack Overflow (SO_DERIVED)</option>
            <option value="SAMPLE">Sample Questions</option>
            <option value="APT">General Aptitude (APT)</option>
            <option value="AUTHORED">Authored</option>
            <option value="GATE_PYP">GATE PYP</option>
          </select>
        </div>
      </div>

      {/* Bulk Operations Action Bar */}
      {selectedIds.size > 0 && (
        <div className="bg-ds-surface-raised border border-ds-accent/40 rounded-ds p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-md animate-in fade-in">
          <div className="flex items-center gap-3">
            <span className="text-xs font-semibold text-ds-primary">
              {selectedIds.size} questions selected
            </span>
            <input
              type="text"
              placeholder="Optional curation note for selected..."
              value={bulkNote}
              onChange={(e) => setBulkNote(e.target.value)}
              className="bg-ds-surface-input border border-ds-border rounded-ds px-3 py-1.5 text-xs text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent/60 w-64"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={bulkProcessing}
              onClick={() => handleBulkAction('ACCEPTED')}
              className="px-3.5 py-1.5 bg-ds-success text-ds-accent-contrast rounded-ds text-xs font-semibold hover:opacity-90 transition-all flex items-center gap-1.5"
            >
              <Check className="w-3.5 h-3.5" />
              <span>Bulk Accept</span>
            </button>
            <button
              type="button"
              disabled={bulkProcessing}
              onClick={() => handleBulkAction('REJECTED')}
              className="px-3.5 py-1.5 bg-ds-danger text-ds-accent-contrast rounded-ds text-xs font-semibold hover:opacity-90 transition-all flex items-center gap-1.5"
            >
              <X className="w-3.5 h-3.5" />
              <span>Bulk Reject</span>
            </button>
          </div>
        </div>
      )}

      {/* Header bar with Select All */}
      <div className="flex items-center justify-between text-xs text-ds-muted px-1">
        <label className="flex items-center gap-2 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={Boolean(data?.content?.length && selectedIds.size === data.content.length)}
            onChange={handleSelectAll}
            className="rounded border-ds-border"
          />
          <span>Select all {data?.content?.length ?? 0} on page</span>
        </label>
        <span>
          Showing {data?.content?.length ?? 0} of {data?.totalElements ?? 0} questions
        </span>
      </div>

      {/* Questions Curation Cards List */}
      <div className="space-y-4">
        {isLoading && (
          <div className="space-y-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="h-40 bg-ds-surface rounded-ds animate-pulse border border-ds-border" />
            ))}
          </div>
        )}

        {!isLoading && (!data?.content || data.content.length === 0) && (
          <div className="text-center py-16 bg-ds-surface rounded-ds border border-ds-border space-y-3">
            <CheckCircle2 className="w-10 h-10 text-ds-success mx-auto" />
            <h3 className="text-base font-bold text-ds-primary">Queue Clear!</h3>
            <p className="text-xs text-ds-muted max-w-md mx-auto">
              No questions found matching the selected filters. All pending questions may have been curated.
            </p>
          </div>
        )}

        {data?.content?.map((q) => {
          const isSelected = selectedIds.has(q.id);
          const noteText = expandedNotes[q.id] || '';
          const isNoteOpen = showNoteInput[q.id] || false;

          return (
            <div
              key={q.id}
              data-testid={`curation-card-${q.id}`}
              className={`bg-ds-surface border rounded-ds p-5 space-y-4 transition-all ${
                isSelected ? 'border-ds-accent' : 'border-ds-border'
              }`}
            >
              {/* Card Header */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ds-border pb-3 text-xs">
                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={() => toggleSelect(q.id)}
                    className="rounded border-ds-border"
                  />
                  <span className="font-mono font-bold text-ds-primary">{q.externalId}</span>
                  <span className="px-2 py-0.5 rounded text-[11px] font-mono border text-ds-accent bg-ds-accent-soft border-ds-accent/30">
                    {q.type}
                  </span>
                  <span className="font-mono text-ds-muted bg-ds-surface-raised px-2 py-0.5 rounded border border-ds-border text-[11px]">
                    {q.marks} {q.marks === 1 ? 'Mark' : 'Marks'}
                  </span>
                  {q.difficulty && (
                    <span className="font-mono text-[11px] text-ds-secondary bg-ds-surface-raised px-2 py-0.5 rounded border border-ds-border">
                      {q.difficulty}
                    </span>
                  )}
                  {q.source && (
                    <span className="font-mono text-[10px] text-ds-muted bg-ds-surface-raised px-1.5 py-0.5 rounded">
                      {q.source}
                    </span>
                  )}
                </div>

                <div className="flex items-center gap-3">
                  <span className={`px-2 py-0.5 rounded text-[11px] font-mono font-semibold border ${getStatusBadge(q.curationStatus)}`}>
                    {q.curationStatus || 'UNREVIEWED'}
                  </span>
                  {q.gateOverflowUrl && (
                    <a
                      href={q.gateOverflowUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-ds-muted hover:text-ds-primary flex items-center gap-1"
                    >
                      <span>Source</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  )}
                </div>
              </div>

              {/* Statement Snippet / Text */}
              <div className="text-sm text-ds-primary leading-relaxed">
                <MathText content={q.statement || q.statementSnippet || ''} />
              </div>

              {/* Existing Curation Note if present */}
              {q.curationNote && !isNoteOpen && (
                <div className="bg-ds-surface-raised border border-ds-border rounded-ds p-2.5 text-xs text-ds-secondary flex items-start gap-2">
                  <MessageSquare className="w-3.5 h-3.5 text-ds-accent shrink-0 mt-0.5" />
                  <span>{q.curationNote}</span>
                </div>
              )}

              {/* Expandable Note Input */}
              {isNoteOpen && (
                <div className="space-y-2 pt-2">
                  <textarea
                    rows={2}
                    placeholder="Enter reason or editorial comments..."
                    value={noteText}
                    onChange={(e) => setExpandedNotes({ ...expandedNotes, [q.id]: e.target.value })}
                    className="w-full bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent/60"
                  />
                </div>
              )}

              {/* Action Buttons Row */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
                <div className="text-xs text-ds-muted">
                  <span>{q.subjectName}</span> / <span className="text-ds-secondary">{q.topicName}</span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowNoteInput({ ...showNoteInput, [q.id]: !isNoteOpen })}
                    className="px-2.5 py-1.5 rounded-ds border border-ds-border bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-secondary text-xs flex items-center gap-1.5 transition-colors"
                  >
                    <MessageSquare className="w-3.5 h-3.5" />
                    <span>{isNoteOpen ? 'Hide Note' : 'Add Note'}</span>
                  </button>

                  <button
                    type="button"
                    data-testid={`accept-btn-${q.id}`}
                    onClick={() => handleAction(q.id, 'ACCEPTED')}
                    className="px-3 py-1.5 rounded-ds bg-ds-success text-ds-accent-contrast font-semibold text-xs hover:opacity-90 flex items-center gap-1.5 transition-all shadow-sm"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>Accept</span>
                  </button>

                  <button
                    type="button"
                    data-testid={`reject-btn-${q.id}`}
                    onClick={() => handleAction(q.id, 'REJECTED')}
                    className="px-3 py-1.5 rounded-ds bg-ds-danger text-ds-accent-contrast font-semibold text-xs hover:opacity-90 flex items-center gap-1.5 transition-all shadow-sm"
                  >
                    <X className="w-3.5 h-3.5" />
                    <span>Reject</span>
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Pagination Controls */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between border-t border-ds-border pt-4 text-xs text-ds-muted">
          <span>
            Page {data.page + 1} of {data.totalPages}
          </span>
          <div className="flex items-center gap-2">
            <button
              type="button"
              disabled={data.page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className="p-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover disabled:opacity-40"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              disabled={data.page >= data.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className="p-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover disabled:opacity-40"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default CurationQueuePage;
