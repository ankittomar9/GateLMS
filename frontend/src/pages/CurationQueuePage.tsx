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
  Clock,
  BookOpen,
  ShieldCheck,
  Edit3,
  Save,
  Loader2,
  Key,
  AlertOctagon
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
import { anatomyApi, QuestionAnatomyDto, CurationAnatomyUpdateRequest } from '../api/anatomyApi';
import { journeyApi, CurationPrimerUpdateRequest } from '../api/journeyApi';
import { TopicPrimer } from '../types/journey';

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

  // Active Tab: Questions, Anatomy Dossiers, or Topic Primers
  const [activeTab, setActiveTab] = useState<'questions' | 'anatomy' | 'primers'>('questions');

  // Topic Primers Curation State
  const [primerDrafts, setPrimerDrafts] = useState<TopicPrimer[]>([]);
  const [primerLoading, setPrimerLoading] = useState<boolean>(false);
  const [primerPage, setPrimerPage] = useState<number>(0);
  const [primerTotalPages, setPrimerTotalPages] = useState<number>(0);
  const [primerTotalElements, setPrimerTotalElements] = useState<number>(0);
  const [editingPrimerId, setEditingPrimerId] = useState<string | null>(null);
  const [primerEditForm, setPrimerEditForm] = useState<CurationPrimerUpdateRequest>({});

  const loadPrimerDrafts = useCallback(async () => {
    setPrimerLoading(true);
    try {
      const res = await journeyApi.getCurationPrimers(primerPage, 15);
      setPrimerDrafts(res.content);
      setPrimerTotalPages(res.totalPages);
      setPrimerTotalElements(res.totalElements);
    } catch (err: any) {
      setActionError(err.message || 'Failed to load topic primer drafts');
    } finally {
      setPrimerLoading(false);
    }
  }, [primerPage]);

  useEffect(() => {
    if (activeTab === 'primers') {
      loadPrimerDrafts();
    }
  }, [activeTab, loadPrimerDrafts]);

  const handlePrimerStatus = async (id: string, status: 'ACCEPTED' | 'REJECTED') => {
    setActionError(null);
    try {
      await journeyApi.updateCurationPrimer(id, { status });
      await loadPrimerDrafts();
    } catch (err: any) {
      setActionError(err.message || 'Failed to update topic primer status');
    }
  };

  const handleStartPrimerEdit = (item: TopicPrimer) => {
    setEditingPrimerId(item.id);
    setPrimerEditForm({
      motivation: item.motivation,
      coreIdea: item.coreIdea,
      invariant: item.invariant || '',
      workedExample: item.workedExample,
      traps: item.traps || '',
      status: item.status,
    });
  };

  const handleSavePrimerEdit = async (id: string) => {
    setActionError(null);
    try {
      await journeyApi.updateCurationPrimer(id, primerEditForm);
      setEditingPrimerId(null);
      setPrimerEditForm({});
      await loadPrimerDrafts();
    } catch (err: any) {
      setActionError(err.message || 'Failed to update topic primer');
    }
  };

  // Anatomy Curation State
  const [anatomyDrafts, setAnatomyDrafts] = useState<QuestionAnatomyDto[]>([]);
  const [anatomyLoading, setAnatomyLoading] = useState<boolean>(false);
  const [anatomyPage, setAnatomyPage] = useState<number>(0);
  const [anatomyTotalPages, setAnatomyTotalPages] = useState<number>(0);
  const [anatomyTotalElements, setAnatomyTotalElements] = useState<number>(0);
  const [editingAnatomyId, setEditingAnatomyId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<CurationAnatomyUpdateRequest>({});

  const loadAnatomyDrafts = useCallback(async () => {
    setAnatomyLoading(true);
    try {
      const res = await anatomyApi.getCurationDrafts(anatomyPage, 15);
      setAnatomyDrafts(res.content);
      setAnatomyTotalPages(res.totalPages);
      setAnatomyTotalElements(res.totalElements);
    } catch (err: any) {
      setActionError(err.message || 'Failed to load anatomy drafts');
    } finally {
      setAnatomyLoading(false);
    }
  }, [anatomyPage]);

  useEffect(() => {
    if (activeTab === 'anatomy') {
      loadAnatomyDrafts();
    }
  }, [activeTab, loadAnatomyDrafts]);

  const handleAnatomyStatus = async (id: string, status: 'ACCEPTED' | 'REJECTED') => {
    setActionError(null);
    try {
      await anatomyApi.updateCurationAnatomy(id, { status });
      await loadAnatomyDrafts();
    } catch (err: any) {
      setActionError(err.message || 'Failed to update anatomy dossier status');
    }
  };

  const handleStartAnatomyEdit = (item: QuestionAnatomyDto) => {
    setEditingAnatomyId(item.id);
    setEditForm({
      testedConcept: item.testedConcept,
      invariantRule: item.invariantRule || '',
      similarPatternHint: item.similarPatternHint || '',
      status: item.status,
    });
  };

  const handleSaveAnatomyEdit = async (id: string) => {
    setActionError(null);
    try {
      await anatomyApi.updateCurationAnatomy(id, editForm);
      setEditingAnatomyId(null);
      setEditForm({});
      await loadAnatomyDrafts();
    } catch (err: any) {
      setActionError(err.message || 'Failed to update anatomy dossier');
    }
  };

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

      {/* Curation Queue Tabs */}
      <div className="flex items-center gap-3 border-b border-ds-border pb-3">
        <button
          type="button"
          onClick={() => setActiveTab('questions')}
          className={`flex items-center gap-2 px-4 py-2 rounded-ds text-xs font-semibold transition-all ${
            activeTab === 'questions'
              ? 'bg-ds-accent text-ds-accent-contrast shadow-sm'
              : 'bg-ds-surface border border-ds-border text-ds-muted hover:text-ds-secondary'
          }`}
        >
          <ClipboardCheck className="w-4 h-4" />
          <span>Questions Queue</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('anatomy')}
          className={`flex items-center gap-2 px-4 py-2 rounded-ds text-xs font-semibold transition-all ${
            activeTab === 'anatomy'
              ? 'bg-ds-accent text-ds-accent-contrast shadow-sm'
              : 'bg-ds-surface border border-ds-border text-ds-muted hover:text-ds-secondary'
          }`}
        >
          <Sparkles className="w-4 h-4" />
          <span>Anatomy Dossiers {anatomyTotalElements > 0 ? `(${anatomyTotalElements})` : ''}</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('primers')}
          className={`flex items-center gap-2 px-4 py-2 rounded-ds text-xs font-semibold transition-all ${
            activeTab === 'primers'
              ? 'bg-ds-accent text-ds-accent-contrast shadow-sm'
              : 'bg-ds-surface border border-ds-border text-ds-muted hover:text-ds-secondary'
          }`}
          data-testid="tab-curation-primers"
        >
          <BookOpen className="w-4 h-4" />
          <span>Topic Primers {primerTotalElements > 0 ? `(${primerTotalElements})` : ''}</span>
        </button>
      </div>

      {activeTab === 'questions' && (
        <>
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
        </>
      )}

      {/* Anatomy Dossiers Tab Content */}
      {activeTab === 'anatomy' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-ds-primary tracking-tight flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              <span>Pending &amp; Draft Question Anatomy Dossiers</span>
            </h2>
            <button
              onClick={() => loadAnatomyDrafts()}
              className="text-xs px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface hover:bg-ds-surface-hover text-ds-secondary flex items-center gap-1.5 transition-colors"
            >
              <Clock className="w-3.5 h-3.5 text-ds-muted" />
              <span>Refresh Queue</span>
            </button>
          </div>

          {anatomyLoading && (
            <div className="p-12 text-center text-xs text-ds-muted flex flex-col items-center justify-center gap-3">
              <Loader2 className="w-6 h-6 animate-spin text-indigo-400" />
              <span>Loading Anatomy Dossiers Queue...</span>
            </div>
          )}

          {!anatomyLoading && anatomyDrafts.length === 0 && (
            <div className="p-12 rounded-ds border border-ds-border bg-ds-surface text-center space-y-2">
              <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
              <h3 className="text-sm font-semibold text-ds-primary">No Draft Dossiers in Queue</h3>
              <p className="text-xs text-ds-muted max-w-md mx-auto">
                All generated anatomy dossiers have been reviewed, or no questions have been generated yet.
              </p>
            </div>
          )}

          {!anatomyLoading && anatomyDrafts.length > 0 && (
            <div className="space-y-4">
              {anatomyDrafts.map((dossier) => {
                const isEditing = editingAnatomyId === dossier.id;

                return (
                  <div
                    key={dossier.id}
                    className="p-5 rounded-ds border border-ds-border bg-ds-surface space-y-4 shadow-sm"
                  >
                    {/* Header: Question Reference + Status */}
                    <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-ds-border">
                      <div className="flex items-center gap-2.5">
                        <span className="font-mono text-xs font-semibold text-ds-accent bg-ds-accent-soft border border-ds-accent/30 px-2.5 py-1 rounded-ds">
                          Q-ID: {dossier.questionId.slice(0, 8)}...
                        </span>
                        <span className={`px-2 py-0.5 rounded text-[11px] font-semibold border ${
                          dossier.status === 'ACCEPTED'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : dossier.status === 'REJECTED'
                            ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                            : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                        }`}>
                          {dossier.status}
                        </span>
                        {dossier.generatorModel && (
                          <span className="text-[10px] font-mono text-ds-muted bg-ds-surface-raised px-2 py-0.5 rounded border border-ds-border">
                            {dossier.generatorModel}
                          </span>
                        )}
                      </div>

                      {/* Actions: Accept, Reject, Edit */}
                      <div className="flex items-center gap-2">
                        {dossier.status !== 'ACCEPTED' && (
                          <button
                            type="button"
                            onClick={() => handleAnatomyStatus(dossier.id, 'ACCEPTED')}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium transition-colors"
                          >
                            <Check className="w-3.5 h-3.5" />
                            <span>Accept</span>
                          </button>
                        )}
                        {dossier.status !== 'REJECTED' && (
                          <button
                            type="button"
                            onClick={() => handleAnatomyStatus(dossier.id, 'REJECTED')}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-rose-600/20 hover:bg-rose-600/30 text-rose-300 border border-rose-500/30 text-xs font-medium transition-colors"
                          >
                            <X className="w-3.5 h-3.5" />
                            <span>Reject</span>
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => isEditing ? setEditingAnatomyId(null) : handleStartAnatomyEdit(dossier)}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface-raised hover:bg-ds-surface-hover text-xs text-ds-secondary transition-colors"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                          <span>{isEditing ? 'Cancel' : 'Edit'}</span>
                        </button>
                      </div>
                    </div>

                    {isEditing ? (
                      /* Inline Edit Form */
                      <div className="space-y-3 pt-2">
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Tested Concept:</label>
                          <textarea
                            value={editForm.testedConcept || ''}
                            onChange={(e) => setEditForm(prev => ({ ...prev, testedConcept: e.target.value }))}
                            rows={2}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Invariant Rule:</label>
                          <textarea
                            value={editForm.invariantRule || ''}
                            onChange={(e) => setEditForm(prev => ({ ...prev, invariantRule: e.target.value }))}
                            rows={2}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary font-mono"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Pattern Hint:</label>
                          <input
                            type="text"
                            value={editForm.similarPatternHint || ''}
                            onChange={(e) => setEditForm(prev => ({ ...prev, similarPatternHint: e.target.value }))}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2 text-xs text-ds-primary"
                          />
                        </div>
                        <div className="flex justify-end pt-2">
                          <button
                            type="button"
                            onClick={() => handleSaveAnatomyEdit(dossier.id)}
                            className="flex items-center gap-1.5 px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold"
                          >
                            <Save className="w-3.5 h-3.5" />
                            <span>Save Changes</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      /* Read-Only Dossier Preview */
                      <div className="space-y-3 text-xs">
                        <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border">
                          <span className="font-semibold text-indigo-400 mb-1 flex items-center gap-1.5">
                            <Sparkles className="w-3 h-3" /> 1. Tested Concept:
                          </span>
                          <div className="text-ds-primary">
                            <MathText content={dossier.testedConcept} />
                          </div>
                        </div>

                        {dossier.invariantRule && (
                          <div className="p-3 rounded-ds bg-amber-500/5 border border-amber-500/20 text-amber-200">
                            <span className="font-semibold text-amber-400 mb-1 flex items-center gap-1.5">
                              <Key className="w-3 h-3" /> 2. Core Invariant:
                            </span>
                            <div className="font-mono">
                              <MathText content={dossier.invariantRule} />
                            </div>
                          </div>
                        )}

                        {dossier.trapAnalysis && dossier.trapAnalysis.length > 0 && (
                          <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border">
                            <span className="font-semibold text-rose-400 mb-1 flex items-center gap-1.5">
                              <AlertOctagon className="w-3 h-3" /> 3. Distractor Autopsy ({dossier.trapAnalysis.length} options evaluated):
                            </span>
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2">
                              {dossier.trapAnalysis.map((trap, idx) => (
                                <div key={idx} className="p-2 rounded bg-ds-surface border border-ds-border text-[11px]">
                                  <div className="flex items-center justify-between mb-1">
                                    <strong className="text-white">Option {trap.option}</strong>
                                    <span className={trap.verdict === 'CORRECT' ? 'text-emerald-400' : 'text-rose-400'}>
                                      {trap.verdict}
                                    </span>
                                  </div>
                                  <div className="text-ds-muted italic"><MathText content={trap.misconception} /></div>
                                </div>
                              ))}
                            </div>
                          </div>
                        )}

                        {dossier.prerequisites && dossier.prerequisites.length > 0 && (
                          <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border flex flex-wrap items-center gap-2">
                            <span className="font-semibold text-emerald-400 flex items-center gap-1.5 mr-2">
                              <BookOpen className="w-3 h-3" /> 4. Prerequisites:
                            </span>
                            {dossier.prerequisites.map((p, idx) => (
                              <span key={idx} className="px-2 py-0.5 rounded text-[11px] bg-neutral-800 text-neutral-300 border border-neutral-700">
                                {p.concept_label} ({p.concept_slug})
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}

              {/* Anatomy Pagination */}
              {anatomyTotalPages > 1 && (
                <div className="flex items-center justify-between text-xs text-ds-muted pt-4 border-t border-ds-border">
                  <span>
                    Page {anatomyPage + 1} of {anatomyTotalPages}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={anatomyPage === 0}
                      onClick={() => setAnatomyPage((p) => Math.max(0, p - 1))}
                      className="p-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover disabled:opacity-40"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      disabled={anatomyPage >= anatomyTotalPages - 1}
                      onClick={() => setAnatomyPage((p) => p + 1)}
                      className="p-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover disabled:opacity-40"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {activeTab === 'primers' && (
        <div className="space-y-4" data-testid="curation-primers-tab-content">
          <div className="flex items-center justify-between">
            <span className="text-xs text-ds-muted">
              {primerTotalElements} topic primer{primerTotalElements === 1 ? '' : 's'} recorded
            </span>
            <button
              type="button"
              onClick={loadPrimerDrafts}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface text-xs text-ds-secondary hover:text-ds-primary"
            >
              <Clock className="w-3.5 h-3.5 text-ds-muted" />
              <span>Refresh Queue</span>
            </button>
          </div>

          {primerLoading && (
            <div className="p-12 text-center text-xs text-ds-muted flex flex-col items-center justify-center gap-3">
              <Loader2 className="w-6 h-6 animate-spin text-indigo-400" />
              <span>Loading Topic Primers Queue...</span>
            </div>
          )}

          {!primerLoading && primerDrafts.length === 0 && (
            <div className="p-12 rounded-ds border border-ds-border bg-ds-surface text-center space-y-2">
              <CheckCircle2 className="w-8 h-8 text-emerald-400 mx-auto" />
              <h3 className="text-sm font-semibold text-ds-primary">No Topic Primers in Queue</h3>
              <p className="text-xs text-ds-muted max-w-md mx-auto">
                No topic primers have been generated yet or all have been reviewed.
              </p>
            </div>
          )}

          {!primerLoading && primerDrafts.length > 0 && (
            <div className="space-y-4">
              {primerDrafts.map((primer) => {
                const isEditing = editingPrimerId === primer.id;

                return (
                  <div
                    key={primer.id}
                    className="p-5 rounded-ds border border-ds-border bg-ds-surface space-y-4 shadow-sm"
                    data-testid={`curation-primer-card-${primer.id}`}
                  >
                    {/* Header: Topic + Subject + Status */}
                    <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-ds-border">
                      <div className="flex items-center gap-2.5">
                        <span className="font-semibold text-sm text-ds-primary">
                          {primer.topicName || `Topic: ${primer.topicId.slice(0, 8)}...`}
                        </span>
                        {primer.subjectName && (
                          <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-ds-surface-raised text-ds-muted border border-ds-border">
                            {primer.subjectName}
                          </span>
                        )}
                        <span className={`px-2 py-0.5 rounded text-[11px] font-semibold border ${
                          primer.status === 'ACCEPTED'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : primer.status === 'REJECTED'
                            ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                            : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                        }`}>
                          {primer.status}
                        </span>
                        {primer.generatedBy && (
                          <span className="text-[10px] font-mono text-ds-muted bg-ds-surface-raised px-2 py-0.5 rounded border border-ds-border">
                            {primer.generatedBy}
                          </span>
                        )}
                      </div>

                      {/* Actions: Accept, Reject, Edit */}
                      <div className="flex items-center gap-2">
                        {primer.status !== 'ACCEPTED' && (
                          <button
                            type="button"
                            onClick={() => handlePrimerStatus(primer.id, 'ACCEPTED')}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium transition-colors"
                            data-testid={`btn-accept-primer-${primer.id}`}
                          >
                            <Check className="w-3.5 h-3.5" />
                            <span>Accept</span>
                          </button>
                        )}
                        {primer.status !== 'REJECTED' && (
                          <button
                            type="button"
                            onClick={() => handlePrimerStatus(primer.id, 'REJECTED')}
                            className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-rose-600/20 hover:bg-rose-600/30 text-rose-300 border border-rose-500/30 text-xs font-medium transition-colors"
                            data-testid={`btn-reject-primer-${primer.id}`}
                          >
                            <X className="w-3.5 h-3.5" />
                            <span>Reject</span>
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => {
                            if (isEditing) {
                              setEditingPrimerId(null);
                            } else {
                              handleStartPrimerEdit(primer);
                            }
                          }}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover text-xs font-medium text-ds-secondary"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                          <span>{isEditing ? 'Cancel' : 'Edit'}</span>
                        </button>
                      </div>
                    </div>

                    {isEditing ? (
                      /* Inline Edit Form */
                      <div className="space-y-3 pt-2">
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Motivation (Why study this?):</label>
                          <textarea
                            value={primerEditForm.motivation || ''}
                            onChange={(e) => setPrimerEditForm(prev => ({ ...prev, motivation: e.target.value }))}
                            rows={2}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Core Idea:</label>
                          <textarea
                            value={primerEditForm.coreIdea || ''}
                            onChange={(e) => setPrimerEditForm(prev => ({ ...prev, coreIdea: e.target.value }))}
                            rows={3}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Core Invariant:</label>
                          <textarea
                            value={primerEditForm.invariant || ''}
                            onChange={(e) => setPrimerEditForm(prev => ({ ...prev, invariant: e.target.value }))}
                            rows={2}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary font-mono"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Worked Example (&ge;150 words):</label>
                          <textarea
                            value={primerEditForm.workedExample || ''}
                            onChange={(e) => setPrimerEditForm(prev => ({ ...prev, workedExample: e.target.value }))}
                            rows={5}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary"
                          />
                        </div>
                        <div>
                          <label className="text-[11px] font-semibold text-ds-muted uppercase">Traps &amp; Common Pitfalls:</label>
                          <textarea
                            value={primerEditForm.traps || ''}
                            onChange={(e) => setPrimerEditForm(prev => ({ ...prev, traps: e.target.value }))}
                            rows={2}
                            className="w-full mt-1 bg-ds-surface-input border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary"
                          />
                        </div>
                        <div className="flex justify-end pt-2">
                          <button
                            type="button"
                            onClick={() => handleSavePrimerEdit(primer.id)}
                            className="flex items-center gap-1.5 px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold"
                          >
                            <Save className="w-3.5 h-3.5" />
                            <span>Save Changes</span>
                          </button>
                        </div>
                      </div>
                    ) : (
                      /* Read-Only Primer Preview */
                      <div className="space-y-3 text-xs">
                        <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border">
                          <span className="font-semibold text-indigo-400 mb-1 flex items-center gap-1.5">
                            <BookOpen className="w-3 h-3" /> 1. Motivation:
                          </span>
                          <div className="text-ds-primary">
                            <MathText content={primer.motivation} />
                          </div>
                        </div>

                        <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border">
                          <span className="font-semibold text-cyan-400 mb-1 flex items-center gap-1.5">
                            <Sparkles className="w-3 h-3" /> 2. Core Idea:
                          </span>
                          <div className="text-ds-primary">
                            <MathText content={primer.coreIdea} />
                          </div>
                        </div>

                        {primer.invariant && (
                          <div className="p-3 rounded-ds bg-amber-500/5 border border-amber-500/20 text-amber-200">
                            <span className="font-semibold text-amber-400 mb-1 flex items-center gap-1.5">
                              <Key className="w-3 h-3" /> 3. Core Invariant:
                            </span>
                            <div className="font-mono">
                              <MathText content={primer.invariant} />
                            </div>
                          </div>
                        )}

                        <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border">
                          <span className="font-semibold text-emerald-400 mb-1 flex items-center gap-1.5">
                            <BookOpen className="w-3 h-3" /> 4. Worked Example:
                          </span>
                          <div className="text-ds-primary leading-relaxed">
                            <MathText content={primer.workedExample} />
                          </div>
                        </div>

                        {primer.traps && (
                          <div className="p-3 rounded-ds bg-rose-500/5 border border-rose-500/20 text-rose-200">
                            <span className="font-semibold text-rose-400 mb-1 flex items-center gap-1.5">
                              <AlertOctagon className="w-3 h-3" /> 5. Common Traps:
                            </span>
                            <div>
                              <MathText content={primer.traps} />
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}

              {/* Primer Pagination */}
              {primerTotalPages > 1 && (
                <div className="flex items-center justify-between text-xs text-ds-muted pt-4 border-t border-ds-border">
                  <span>
                    Page {primerPage + 1} of {primerTotalPages}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      disabled={primerPage === 0}
                      onClick={() => setPrimerPage((p) => Math.max(0, p - 1))}
                      className="p-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover disabled:opacity-40"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      disabled={primerPage >= primerTotalPages - 1}
                      onClick={() => setPrimerPage((p) => p + 1)}
                      className="p-1.5 rounded-ds border border-ds-border hover:bg-ds-surface-hover disabled:opacity-40"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CurationQueuePage;
