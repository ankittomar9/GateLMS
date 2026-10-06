import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { 
  Search, 
  Bookmark, 
  ChevronLeft, 
  ChevronRight, 
  RotateCcw,
  HelpCircle,
  FileQuestion
} from 'lucide-react';
import { useQuestions, useSubjects, useTopics, useToggleBookmark } from '../hooks/useQueries';
import { QuestionType, Pass1Status, QuestionSummary } from '../api/types';

export const QuestionsPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // Read state from URL search params
  const qParam = searchParams.get('q') || '';
  const subjectIdParam = searchParams.get('subjectId') || '';
  const topicIdParam = searchParams.get('topicId') || '';
  const typeParam = (searchParams.get('type') as QuestionType) || undefined;
  const yearParam = searchParams.get('year') ? Number(searchParams.get('year')) : undefined;
  const pass1StatusParam = (searchParams.get('pass1Status') as Pass1Status) || undefined;
  const bookmarkedParam = searchParams.get('bookmarked') === 'true' ? true : undefined;
  const pageParam = searchParams.get('page') ? Number(searchParams.get('page')) : 0;

  // Local state for debounced search box
  const [searchInput, setSearchInput] = useState(qParam);

  // Debounce search input by 300ms
  useEffect(() => {
    const handler = setTimeout(() => {
      if (searchInput !== qParam) {
        setSearchParams((prev) => {
          const next = new URLSearchParams(prev);
          if (searchInput.trim()) {
            next.set('q', searchInput.trim());
          } else {
            next.delete('q');
          }
          next.set('page', '0'); // Reset to page 0 on search
          return next;
        });
      }
    }, 300);

    return () => clearTimeout(handler);
  }, [searchInput, qParam, setSearchParams]);

  // Keep local search input synced if external URL changes
  useEffect(() => {
    setSearchInput(qParam);
  }, [qParam]);

  const { data: subjects } = useSubjects();
  const { data: topics } = useTopics(subjectIdParam);
  const toggleBookmark = useToggleBookmark();

  const queryFilters = useMemo(
    () => ({
      q: qParam || undefined,
      subjectId: subjectIdParam || undefined,
      topicId: topicIdParam || undefined,
      type: typeParam,
      year: yearParam,
      pass1Status: pass1StatusParam,
      bookmarked: bookmarkedParam,
      page: pageParam,
      size: 20,
    }),
    [qParam, subjectIdParam, topicIdParam, typeParam, yearParam, pass1StatusParam, bookmarkedParam, pageParam]
  );

  const { data, isLoading, error } = useQuestions(queryFilters);

  const handleFilterChange = (key: string, value: string | boolean | undefined) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      if (value !== undefined && value !== '' && value !== false) {
        next.set(key, String(value));
      } else {
        next.delete(key);
      }
      if (key === 'subjectId') {
        next.delete('topicId'); // Reset cascaded topic
      }
      next.set('page', '0'); // Reset pagination
      return next;
    });
  };

  const handlePageChange = (newPage: number) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', String(newPage));
      return next;
    });
  };

  const handleResetFilters = () => {
    setSearchInput('');
    setSearchParams(new URLSearchParams());
  };

  const getTypeBadge = (type: QuestionType) => {
    switch (type) {
      case 'MCQ':
        return 'text-ds-accent border-ds-accent/30 bg-ds-accent-soft';
      case 'MSQ':
        return 'text-ds-info border-ds-info/30 bg-ds-info/10';
      case 'NAT':
        return 'text-ds-success border-ds-success/30 bg-ds-success/10';
    }
  };

  const getPass1Badge = (status: Pass1Status) => {
    switch (status) {
      case 'SOLVED':
        return 'text-ds-success bg-ds-success/10 border-ds-success/30';
      case 'REVISE':
        return 'text-ds-warning bg-ds-warning/10 border-ds-warning/30';
      case 'FLAGGED':
        return 'text-ds-danger bg-ds-danger/10 border-ds-danger/30';
      case 'UNSEEN':
      default:
        return 'text-ds-muted bg-ds-surface-raised border-ds-border';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <FileQuestion className="w-5 h-5 text-ds-accent" />
              <h1 className="text-xl font-bold text-ds-primary tracking-tight">GATE Overflow PYQ Question Bank</h1>
            </div>
            <p className="text-ds-secondary text-sm max-w-2xl">
              Search and filter official GATE CS questions with Postgres Full-Text Search. Two-pass preparation mode with solution masking.
            </p>
          </div>

          <div className="flex items-center gap-2 font-mono text-xs text-ds-muted bg-ds-surface-raised px-3 py-1.5 rounded-ds border border-ds-border">
            <span>Total Questions:</span>
            <strong className="text-ds-accent">{data?.totalElements ?? '...'}</strong>
          </div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-4 shadow-sm">
        {/* Search Bar */}
        <div className="relative">
          <Search className="w-4 h-4 text-ds-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search questions by keyword, topic, or formula with full-text search..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full bg-ds-surface-input border border-ds-border rounded-ds pl-10 pr-4 py-2 text-sm text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent/60 transition-all"
          />
        </div>

        {/* Dropdowns Row */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3 text-xs">
          {/* Subject Filter */}
          <select
            value={subjectIdParam}
            onChange={(e) => handleFilterChange('subjectId', e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-2.5 py-1.5 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Subjects</option>
            {subjects?.map((s) => (
              <option key={s.id} value={s.id}>
                {s.code} - {s.name}
              </option>
            ))}
          </select>

          {/* Topic Filter */}
          <select
            value={topicIdParam}
            onChange={(e) => handleFilterChange('topicId', e.target.value)}
            disabled={!subjectIdParam}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-2.5 py-1.5 text-ds-secondary disabled:opacity-40 focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Topics</option>
            {topics?.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>

          {/* Type Filter */}
          <select
            value={typeParam || ''}
            onChange={(e) => handleFilterChange('type', e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-2.5 py-1.5 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">All Types</option>
            <option value="MCQ">MCQ (Single Choice)</option>
            <option value="MSQ">MSQ (Multi Choice)</option>
            <option value="NAT">NAT (Numerical)</option>
          </select>

          {/* Pass 1 Status Filter */}
          <select
            value={pass1StatusParam || ''}
            onChange={(e) => handleFilterChange('pass1Status', e.target.value)}
            className="bg-ds-surface-input border border-ds-border rounded-ds px-2.5 py-1.5 text-ds-secondary focus:outline-none focus:border-ds-accent/60"
          >
            <option value="">Pass 1: All</option>
            <option value="UNSEEN">UNSEEN</option>
            <option value="SOLVED">SOLVED</option>
            <option value="REVISE">REVISE</option>
            <option value="FLAGGED">FLAGGED</option>
          </select>

          {/* Bookmarked Filter */}
          <button
            onClick={() => handleFilterChange('bookmarked', !bookmarkedParam)}
            className={`flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-ds border font-medium transition-colors ${
              bookmarkedParam
                ? 'bg-ds-accent-soft border-ds-accent text-ds-accent'
                : 'bg-ds-surface-input border-ds-border text-ds-muted hover:text-ds-primary'
            }`}
          >
            <Bookmark className={`w-3.5 h-3.5 ${bookmarkedParam ? 'fill-current' : ''}`} />
            <span>Bookmarked</span>
          </button>

          {/* Reset Filters */}
          <button
            onClick={handleResetFilters}
            className="flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface-input text-ds-muted hover:text-ds-primary transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset</span>
          </button>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger p-4 rounded-ds text-sm">
          Failed to load questions. Please verify backend service availability.
        </div>
      )}

      {/* Questions List */}
      <div className="bg-ds-surface border border-ds-border rounded-ds overflow-hidden divide-y divide-ds-border shadow-sm">
        {isLoading && (
          <div className="p-4 space-y-3">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-16 bg-ds-surface-raised rounded-ds animate-pulse" />
            ))}
          </div>
        )}

        {!isLoading && data?.content.length === 0 && (
          <div className="p-12 text-center space-y-3">
            <HelpCircle className="w-8 h-8 text-ds-muted mx-auto" />
            <h3 className="text-sm font-semibold text-ds-primary">No questions found</h3>
            <p className="text-xs text-ds-muted max-w-sm mx-auto">
              No questions matched your current filter criteria. Try resetting filters or importing question data.
            </p>
            <button
              onClick={handleResetFilters}
              className="mt-2 text-xs text-ds-accent hover:underline font-medium"
            >
              Reset all filters
            </button>
          </div>
        )}

        {!isLoading &&
          data?.content.map((q: QuestionSummary) => (
            <div
              key={q.id}
              onClick={() => navigate(`/questions/${q.id}`)}
              className="p-4 hover:bg-ds-surface-hover cursor-pointer transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              {/* Question summary metadata & snippet */}
              <div className="min-w-0 space-y-1.5 flex-1">
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold border ${getTypeBadge(q.type)}`}>
                    {q.type}
                  </span>
                  <span className="text-ds-muted font-mono text-[11px]">
                    {q.marks} {q.marks === 1 ? 'Mark' : 'Marks'}
                  </span>
                  <span className="text-ds-muted font-mono text-[11px]">
                    GATE {q.year}
                  </span>
                  <span className="text-ds-muted">•</span>
                  <span className="text-ds-secondary font-mono text-[11px]">
                    {q.externalId}
                  </span>
                  <span className="text-ds-muted">•</span>
                  <span className="text-ds-accent font-medium text-[11px]">
                    {q.subjectName}
                  </span>
                  <span className="text-ds-muted text-[11px]">
                    / {q.topicName}
                  </span>
                </div>

                <p className="text-sm text-ds-secondary line-clamp-2 leading-relaxed">
                  {q.statementSnippet}
                </p>
              </div>

              {/* Status Chips & Actions */}
              <div className="flex items-center gap-3 shrink-0 self-end sm:self-center">
                <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold border ${getPass1Badge(q.pass1Status)}`}>
                  {q.pass1Status}
                </span>

                <button
                  type="button"
                  title="Bookmark"
                  onClick={(e) => {
                    e.stopPropagation();
                    toggleBookmark.mutate(q.id);
                  }}
                  className={`p-1.5 rounded-ds border border-ds-border transition-colors ${
                    q.isBookmarked
                      ? 'text-ds-accent bg-ds-accent-soft border-ds-accent/40'
                      : 'text-ds-muted hover:text-ds-primary bg-ds-surface-input'
                  }`}
                >
                  <Bookmark className={`w-3.5 h-3.5 ${q.isBookmarked ? 'fill-current' : ''}`} />
                </button>
              </div>
            </div>
          ))}
      </div>

      {/* Pagination Controls */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-ds-secondary bg-ds-surface border border-ds-border rounded-ds px-4 py-3 shadow-sm">
          <div>
            Showing <strong className="text-ds-primary">{data.page * data.size + 1}</strong> to{' '}
            <strong className="text-ds-primary">
              {Math.min((data.page + 1) * data.size, data.totalElements)}
            </strong>{' '}
            of <strong className="text-ds-primary">{data.totalElements}</strong>
          </div>

          <div className="flex items-center gap-2">
            <button
              disabled={data.page <= 0}
              onClick={() => handlePageChange(data.page - 1)}
              className="p-1.5 rounded-ds border border-ds-border bg-ds-surface-input hover:bg-ds-surface-hover disabled:opacity-30 transition-colors text-ds-secondary"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="font-mono text-ds-primary">
              {data.page + 1} / {data.totalPages}
            </span>
            <button
              disabled={data.page >= data.totalPages - 1}
              onClick={() => handlePageChange(data.page + 1)}
              className="p-1.5 rounded-ds border border-ds-border bg-ds-surface-input hover:bg-ds-surface-hover disabled:opacity-30 transition-colors text-ds-secondary"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default QuestionsPage;
