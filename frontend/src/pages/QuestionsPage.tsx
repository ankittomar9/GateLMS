import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { 
  Search, 
  Filter, 
  Bookmark, 
  ChevronLeft, 
  ChevronRight, 
  ExternalLink, 
  RotateCcw,
  Sparkles,
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
        return 'text-amber-400 border-amber-500/20 bg-amber-500/10';
      case 'MSQ':
        return 'text-cyan-400 border-cyan-500/20 bg-cyan-500/10';
      case 'NAT':
        return 'text-emerald-400 border-emerald-500/20 bg-emerald-500/10';
    }
  };

  const getPass1Badge = (status: Pass1Status) => {
    switch (status) {
      case 'SOLVED':
        return 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
      case 'REVISE':
        return 'text-amber-400 bg-amber-500/10 border-amber-500/20';
      case 'FLAGGED':
        return 'text-rose-400 bg-rose-500/10 border-rose-500/20';
      case 'UNSEEN':
      default:
        return 'text-zinc-500 bg-zinc-800/50 border-zinc-700/50';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-[#161616] border border-[#262626] rounded-xl p-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <FileQuestion className="w-5 h-5 text-amber-400" />
              <h1 className="text-xl font-bold text-white tracking-tight">GATE Overflow PYQ Question Bank</h1>
            </div>
            <p className="text-zinc-400 text-sm max-w-2xl">
              Search and filter official GATE CS questions with Postgres Full-Text Search. Two-pass preparation mode with solution masking.
            </p>
          </div>

          <div className="flex items-center gap-2 font-mono text-xs text-zinc-400 bg-[#121212] px-3 py-1.5 rounded-lg border border-[#262626]">
            <span>Total Questions:</span>
            <strong className="text-amber-400">{data?.totalElements ?? '...'}</strong>
          </div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-[#161616] border border-[#262626] rounded-xl p-4 space-y-4">
        {/* Search Bar */}
        <div className="relative">
          <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Search questions by keyword, topic, or formula with full-text search..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full bg-[#121212] border border-[#262626] rounded-lg pl-10 pr-4 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-amber-500/50 focus:ring-1 focus:ring-amber-500/30 transition-all"
          />
        </div>

        {/* Dropdowns Row */}
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3 text-xs">
          {/* Subject Filter */}
          <select
            value={subjectIdParam}
            onChange={(e) => handleFilterChange('subjectId', e.target.value)}
            className="bg-[#121212] border border-[#262626] rounded-lg px-2.5 py-1.5 text-zinc-300 focus:outline-none focus:border-amber-500/50"
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
            className="bg-[#121212] border border-[#262626] rounded-lg px-2.5 py-1.5 text-zinc-300 disabled:opacity-40 focus:outline-none focus:border-amber-500/50"
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
            className="bg-[#121212] border border-[#262626] rounded-lg px-2.5 py-1.5 text-zinc-300 focus:outline-none focus:border-amber-500/50"
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
            className="bg-[#121212] border border-[#262626] rounded-lg px-2.5 py-1.5 text-zinc-300 focus:outline-none focus:border-amber-500/50"
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
            className={`flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg border font-medium transition-colors ${
              bookmarkedParam
                ? 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                : 'bg-[#121212] border-[#262626] text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <Bookmark className={`w-3.5 h-3.5 ${bookmarkedParam ? 'fill-amber-400' : ''}`} />
            <span>Bookmarked</span>
          </button>

          {/* Reset Filters */}
          <button
            onClick={handleResetFilters}
            className="flex items-center justify-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#262626] bg-[#121212] text-zinc-400 hover:text-zinc-200 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset</span>
          </button>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-4 rounded-xl text-sm">
          Failed to load questions. Please verify backend service availability.
        </div>
      )}

      {/* Questions List */}
      <div className="bg-[#161616] border border-[#262626] rounded-xl overflow-hidden divide-y divide-[#262626]">
        {isLoading && (
          <div className="p-4 space-y-3">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-16 bg-[#1a1a1a] rounded-lg animate-pulse" />
            ))}
          </div>
        )}

        {!isLoading && data?.content.length === 0 && (
          <div className="p-12 text-center space-y-3">
            <HelpCircle className="w-8 h-8 text-zinc-600 mx-auto" />
            <h3 className="text-sm font-semibold text-zinc-300">No questions found</h3>
            <p className="text-xs text-zinc-500 max-w-sm mx-auto">
              No questions matched your current filter criteria. Try resetting filters or importing question data.
            </p>
            <button
              onClick={handleResetFilters}
              className="mt-2 text-xs text-amber-400 hover:text-amber-300 underline font-medium"
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
              className="p-4 hover:bg-[#1a1a1a] cursor-pointer transition-colors flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              {/* Question summary metadata & snippet */}
              <div className="min-w-0 space-y-1.5 flex-1">
                <div className="flex flex-wrap items-center gap-2 text-xs">
                  <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold border ${getTypeBadge(q.type)}`}>
                    {q.type}
                  </span>
                  <span className="text-zinc-500 font-mono text-[11px]">
                    {q.marks} {q.marks === 1 ? 'Mark' : 'Marks'}
                  </span>
                  <span className="text-zinc-500 font-mono text-[11px]">
                    GATE {q.year}
                  </span>
                  <span className="text-zinc-600">•</span>
                  <span className="text-zinc-400 font-mono text-[11px]">
                    {q.externalId}
                  </span>
                  <span className="text-zinc-600">•</span>
                  <span className="text-amber-400 font-medium text-[11px]">
                    {q.subjectName}
                  </span>
                  <span className="text-zinc-500 text-[11px]">
                    / {q.topicName}
                  </span>
                </div>

                <p className="text-sm text-zinc-300 line-clamp-2 leading-relaxed">
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
                  className={`p-1.5 rounded-lg border border-[#2a2a2a] transition-colors ${
                    q.isBookmarked
                      ? 'text-amber-400 bg-amber-500/10'
                      : 'text-zinc-500 hover:text-zinc-300 bg-[#121212]'
                  }`}
                >
                  <Bookmark className={`w-3.5 h-3.5 ${q.isBookmarked ? 'fill-amber-400' : ''}`} />
                </button>
              </div>
            </div>
          ))}
      </div>

      {/* Pagination Controls */}
      {data && data.totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-zinc-400 bg-[#161616] border border-[#262626] rounded-xl px-4 py-3">
          <div>
            Showing <strong className="text-white">{data.page * data.size + 1}</strong> to{' '}
            <strong className="text-white">
              {Math.min((data.page + 1) * data.size, data.totalElements)}
            </strong>{' '}
            of <strong className="text-white">{data.totalElements}</strong>
          </div>

          <div className="flex items-center gap-2">
            <button
              disabled={data.page <= 0}
              onClick={() => handlePageChange(data.page - 1)}
              className="p-1.5 rounded-lg border border-[#262626] bg-[#121212] hover:bg-[#1a1a1a] disabled:opacity-30 disabled:hover:bg-[#121212] transition-colors text-zinc-300"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="font-mono text-zinc-300">
              {data.page + 1} / {data.totalPages}
            </span>
            <button
              disabled={data.page >= data.totalPages - 1}
              onClick={() => handlePageChange(data.page + 1)}
              className="p-1.5 rounded-lg border border-[#262626] bg-[#121212] hover:bg-[#1a1a1a] disabled:opacity-30 disabled:hover:bg-[#121212] transition-colors text-zinc-300"
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
