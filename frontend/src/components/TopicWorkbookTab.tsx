import React, { useEffect, useState } from 'react';
import {
  Download,
  Printer,
  Plus,
  FileText,
  Paperclip,
  Pin,
  ChevronDown,
  ChevronUp,
  CheckCircle2,
  Circle,
  Sparkles,
  Loader2,
  ExternalLink,
  BookOpen,
  HelpCircle,
  AlertCircle
} from 'lucide-react';
import { TopicPrimer } from '../types/journey';
import { QuestionSummary } from '../api/types';
import { noteApi, NoteItem } from '../api/noteApi';
import { anatomyApi, QuestionAnatomyDto } from '../api/anatomyApi';
import { MathText } from './MathText';

const RichMarkdownText = React.lazy(() => import('./ai/RichMarkdownText'));

export type WorkbookQuestion = QuestionSummary & {
  options?: Array<{ key?: string; label?: string; text: string }>;
  correctOptions?: string | null;
  numericMin?: number | null;
  numericMax?: number | null;
  explanation?: string | null;
};

interface TopicWorkbookTabProps {
  topicId: string;
  topicName: string;
  subjectId: string;
  subjectName: string;
  bookChapterRef?: string | null;
  primer: TopicPrimer | null;
  questions: WorkbookQuestion[];
  solvedQuestions: WorkbookQuestion[];
}

export const TopicWorkbookTab: React.FC<TopicWorkbookTabProps> = ({
  topicId,
  topicName,
  subjectId,
  subjectName,
  bookChapterRef,
  primer,
  questions,
  solvedQuestions
}) => {
  // Stitched Notes State
  const [topicNotes, setTopicNotes] = useState<NoteItem[]>([]);
  const [subjectNotes, setSubjectNotes] = useState<NoteItem[]>([]);
  const [loadingNotes, setLoadingNotes] = useState<boolean>(true);
  const [notesError, setNotesError] = useState<string | null>(null);

  // Quick-Add Modal State
  const [isQuickAddOpen, setIsQuickAddOpen] = useState<boolean>(false);
  const [noteTitle, setNoteTitle] = useState<string>('');
  const [noteContent, setNoteContent] = useState<string>('');
  const [isSubjectWide, setIsSubjectWide] = useState<boolean>(false);
  const [savingNote, setSavingNote] = useState<boolean>(false);
  const [saveNoteError, setSaveNoteError] = useState<string | null>(null);

  // Question Dossiers State (Reveal Solution & Anatomy Expand)
  const [revealedSolutions, setRevealedSolutions] = useState<Record<string, boolean>>({});
  const [expandedAnatomy, setExpandedAnatomy] = useState<Record<string, QuestionAnatomyDto | null>>({});
  const [loadingAnatomy, setLoadingAnatomy] = useState<Record<string, boolean>>({});

  // PDF Export State
  const [downloadingPdf, setDownloadingPdf] = useState<boolean>(false);
  const [pdfError, setPdfError] = useState<string | null>(null);

  // Load Notes (paged to exhaustion for both partitions)
  const loadStitchedNotes = async () => {
    if (!topicId) return;
    setLoadingNotes(true);
    setNotesError(null);

    try {
      // 1. Topic Notes - paged to exhaustion
      const allTopicNotes: NoteItem[] = [];
      let topicPage = 0;
      while (true) {
        const resp = await noteApi.getNotes({
          topicId,
          page: topicPage,
          size: 50,
          pinnedFirst: true
        });
        allTopicNotes.push(...resp.content);
        if (resp.isLast || resp.totalPages <= topicPage + 1) break;
        topicPage++;
      }

      // 2. Subject-Wide Notes - paged to exhaustion (filtered for topicId == null)
      const allSubjectNotes: NoteItem[] = [];
      if (subjectId) {
        let subPage = 0;
        while (true) {
          const resp = await noteApi.getNotes({
            subjectId,
            page: subPage,
            size: 50,
            pinnedFirst: true
          });
          const subjectWideOnly = resp.content.filter((n) => !n.topicId);
          allSubjectNotes.push(...subjectWideOnly);
          if (resp.isLast || resp.totalPages <= subPage + 1) break;
          subPage++;
        }
      }

      setTopicNotes(allTopicNotes);
      setSubjectNotes(allSubjectNotes);
    } catch (err: any) {
      setNotesError(err?.message || 'Failed to load notes');
    } finally {
      setLoadingNotes(false);
    }
  };

  useEffect(() => {
    loadStitchedNotes();
  }, [topicId, subjectId]);

  // Handle Quick-Add Save
  const handleSaveNote = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!noteTitle.trim()) return;

    setSavingNote(true);
    setSaveNoteError(null);

    try {
      await noteApi.createTextNote({
        title: noteTitle.trim(),
        contentMd: noteContent,
        subjectId: subjectId || undefined,
        topicId: isSubjectWide ? undefined : topicId
      });

      setNoteTitle('');
      setNoteContent('');
      setIsSubjectWide(false);
      setIsQuickAddOpen(false);
      await loadStitchedNotes();
    } catch (err: any) {
      setSaveNoteError(err?.message || 'Failed to save note');
    } finally {
      setSavingNote(false);
    }
  };

  // Toggle Solution Visibility
  const toggleSolution = (qId: string) => {
    setRevealedSolutions((prev) => ({
      ...prev,
      [qId]: !prev[qId]
    }));
  };

  // Toggle Anatomy Dossier
  const toggleAnatomy = async (qId: string) => {
    if (expandedAnatomy[qId] !== undefined) {
      // Toggle visibility by deleting or setting undefined
      setExpandedAnatomy((prev) => {
        const next = { ...prev };
        delete next[qId];
        return next;
      });
      return;
    }

    setLoadingAnatomy((prev) => ({ ...prev, [qId]: true }));
    try {
      const dossier = await anatomyApi.getQuestionAnatomy(qId);
      setExpandedAnatomy((prev) => ({ ...prev, [qId]: dossier }));
    } catch {
      setExpandedAnatomy((prev) => ({ ...prev, [qId]: null }));
    } finally {
      setLoadingAnatomy((prev) => ({ ...prev, [qId]: false }));
    }
  };

  // Server-side PDF Download
  const handleDownloadPdf = async () => {
    setDownloadingPdf(true);
    setPdfError(null);

    try {
      const response = await fetch(`/api/v1/journey/topics/${encodeURIComponent(topicId)}/workbook/pdf`, {
        method: 'GET',
        headers: {
          Accept: 'application/pdf'
        },
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error(`PDF generation failed with status ${response.status}`);
      }

      // Extract filename from Content-Disposition if present
      let filename = `topic_${topicId}_workbook.pdf`;
      const cd = response.headers.get('Content-Disposition');
      if (cd) {
        const filenameMatch = cd.match(/filename\*?=['"]?(?:UTF-\d['"]*)?([^;\r\n"']*)['"]?/i);
        if (filenameMatch && filenameMatch[1]) {
          filename = decodeURIComponent(filenameMatch[1]);
        }
      }

      const blob = await response.blob();
      const objectUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = objectUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(objectUrl);
    } catch (err: any) {
      setPdfError(err?.message || 'Failed to download workbook PDF');
    } finally {
      setDownloadingPdf(false);
    }
  };

  // Browser Print-to-PDF Fallback
  const handlePrint = () => {
    window.print();
  };

  // Merged Stitched Notes (pinned first)
  const mergedNotes = [
    ...topicNotes.map((n) => ({ ...n, badge: '[Topic Note]' as const })),
    ...subjectNotes.map((n) => ({ ...n, badge: '[Subject-Wide Note]' as const }))
  ].sort((a, b) => {
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
    return new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime();
  });

  const solvedIds = new Set(solvedQuestions.map((q) => q.id));

  return (
    <div className="space-y-8" data-testid="workbook-tab-content">
      {/* --- PRINT-ONLY BOOK COVER HEADER --- */}
      <div className="hidden print:block border-b border-black pb-4 mb-6">
        <h1 className="text-2xl font-bold">{topicName} — Reference Workbook</h1>
        <p className="text-sm text-gray-700 mt-1">
          Subject: {subjectName || 'General'} | Ref: {bookChapterRef || topicId} | Date: {new Date().toISOString().slice(0, 10)}
        </p>
        <p className="text-xs text-gray-500 mt-0.5">
          {questions.length} Questions ({solvedQuestions.length} Solved) • {mergedNotes.length} Revision Notes
        </p>
      </div>

      {/* --- SCREEN TOP BAR: EXPORT CONTROLS & META --- */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 p-4 rounded-ds bg-ds-surface border border-ds-border print:hidden">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-base font-bold text-ds-primary font-mono">Reference Workbook</h2>
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-ds-surface-raised border border-ds-border text-ds-accent">
              Compiled Reference
            </span>
          </div>
          <p className="text-xs text-ds-muted mt-1">
            Curated theory, stitched personal notes, and full question dossiers with solutions.
          </p>
        </div>

        <div className="flex items-center gap-2 self-end sm:self-center">
          <button
            onClick={handleDownloadPdf}
            disabled={downloadingPdf}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-ds-accent text-white text-xs font-semibold hover:bg-ds-accent/90 disabled:opacity-50 transition-colors shadow-sm"
            data-testid="download-pdf-btn"
            title="Download compiled server-side PDF book"
          >
            {downloadingPdf ? (
              <Loader2 className="w-3.5 h-3.5 animate-spin" />
            ) : (
              <Download className="w-3.5 h-3.5" />
            )}
            <span>{downloadingPdf ? 'Compiling PDF...' : 'Download PDF Book'}</span>
          </button>

          <button
            onClick={handlePrint}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-ds-surface-raised border border-ds-border text-ds-primary text-xs font-semibold hover:bg-ds-surface-hover transition-colors"
            data-testid="print-pdf-btn"
            title="Print or Save via Browser PDF"
          >
            <Printer className="w-3.5 h-3.5 text-ds-secondary" />
            <span>Print / Save PDF</span>
          </button>
        </div>
      </div>

      {pdfError && (
        <div className="p-3 rounded-ds bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-2 print:hidden">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{pdfError}</span>
        </div>
      )}

      {/* --- SECTION 1: THEORY PRIMER --- */}
      <section className="space-y-4 break-before-page">
        <div className="flex items-center gap-2 pb-2 border-b border-ds-border">
          <BookOpen className="w-4 h-4 text-ds-accent" />
          <h3 className="text-sm font-mono font-bold text-ds-primary uppercase tracking-wide">
            1. Theory Primer
          </h3>
        </div>

        {primer ? (
          <div className="space-y-4">
            {/* Motivation */}
            <div className="p-4 rounded-ds bg-ds-surface border border-ds-border space-y-2">
              <span className="text-xs font-mono font-bold text-ds-accent uppercase tracking-wider block">
                Motivation & Engineering Context
              </span>
              <div className="text-xs text-ds-primary leading-relaxed">
                <React.Suspense fallback={<MathText content={primer.motivation} />}>
                  <RichMarkdownText content={primer.motivation} />
                </React.Suspense>
              </div>
            </div>

            {/* Core Idea */}
            <div className="p-4 rounded-ds bg-ds-surface border border-ds-border space-y-2">
              <span className="text-xs font-mono font-bold text-ds-accent uppercase tracking-wider block">
                Core Idea & Definitions
              </span>
              <div className="text-xs text-ds-primary leading-relaxed">
                <React.Suspense fallback={<MathText content={primer.coreIdea} />}>
                  <RichMarkdownText content={primer.coreIdea} />
                </React.Suspense>
              </div>
            </div>

            {/* Invariant */}
            {primer.invariant && (
              <div className="p-4 rounded-ds bg-amber-500/5 border border-amber-500/20 space-y-2">
                <span className="text-xs font-mono font-bold text-amber-400 uppercase tracking-wider block">
                  The Stride & Indirection Invariant
                </span>
                <div className="text-xs text-amber-200/90 leading-relaxed font-mono">
                  <React.Suspense fallback={<MathText content={primer.invariant} />}>
                    <RichMarkdownText content={primer.invariant} />
                  </React.Suspense>
                </div>
              </div>
            )}

            {/* Worked Example */}
            <div className="p-4 rounded-ds bg-ds-surface border border-ds-border space-y-2">
              <span className="text-xs font-mono font-bold text-ds-accent uppercase tracking-wider block">
                Canonical Worked Example
              </span>
              <div className="text-xs text-ds-primary leading-relaxed">
                <React.Suspense fallback={<MathText content={primer.workedExample} />}>
                  <RichMarkdownText content={primer.workedExample} />
                </React.Suspense>
              </div>
            </div>

            {/* Traps */}
            {primer.traps && (
              <div className="p-4 rounded-ds bg-rose-500/5 border border-rose-500/20 space-y-2">
                <span className="text-xs font-mono font-bold text-rose-400 uppercase tracking-wider block">
                  Examiner Traps & Common Distractors
                </span>
                <div className="text-xs text-rose-200/90 leading-relaxed">
                  <React.Suspense fallback={<MathText content={primer.traps} />}>
                    <RichMarkdownText content={primer.traps} />
                  </React.Suspense>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="p-6 text-center text-xs font-mono text-ds-muted bg-ds-surface rounded-ds border border-ds-border">
            Theory primer has not been generated for this topic yet.
          </div>
        )}
      </section>

      {/* --- SECTION 2: STITCHED REVISION NOTES --- */}
      <section className="space-y-4 break-before-page">
        <div className="flex items-center justify-between pb-2 border-b border-ds-border">
          <div className="flex items-center gap-2">
            <FileText className="w-4 h-4 text-ds-accent" />
            <h3 className="text-sm font-mono font-bold text-ds-primary uppercase tracking-wide">
              2. Stitched Revision Notes ({mergedNotes.length})
            </h3>
          </div>

          <button
            onClick={() => setIsQuickAddOpen(true)}
            className="flex items-center gap-1 px-2.5 py-1 rounded bg-ds-surface-raised border border-ds-border text-ds-primary text-xs font-semibold hover:bg-ds-surface-hover print:hidden transition-colors"
            data-testid="quick-add-note-btn"
          >
            <Plus className="w-3.5 h-3.5 text-ds-accent" />
            <span>Quick Add Note</span>
          </button>
        </div>

        {/* Quick Add Modal */}
        {isQuickAddOpen && (
          <form
            onSubmit={handleSaveNote}
            className="p-4 rounded-ds bg-ds-surface border border-ds-accent/40 space-y-3 print:hidden animate-in fade-in"
            data-testid="quick-add-note-form"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-mono font-bold text-ds-primary">New Quick Note</span>
              <button
                type="button"
                onClick={() => setIsQuickAddOpen(false)}
                className="text-xs text-ds-muted hover:text-ds-primary"
              >
                Cancel
              </button>
            </div>

            <div>
              <input
                type="text"
                placeholder="Note Title"
                value={noteTitle}
                onChange={(e) => setNoteTitle(e.target.value)}
                className="w-full px-3 py-1.5 text-xs rounded bg-ds-surface-raised border border-ds-border text-ds-primary focus:outline-none focus:border-ds-accent"
                required
                data-testid="quick-add-title-input"
              />
            </div>

            <div>
              <textarea
                placeholder="Note markdown content (LaTeX formulas, key observations)..."
                rows={4}
                value={noteContent}
                onChange={(e) => setNoteContent(e.target.value)}
                className="w-full px-3 py-1.5 text-xs rounded bg-ds-surface-raised border border-ds-border text-ds-primary font-mono focus:outline-none focus:border-ds-accent"
                data-testid="quick-add-content-input"
              />
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-xs text-ds-secondary cursor-pointer">
                <input
                  type="checkbox"
                  checked={isSubjectWide}
                  onChange={(e) => setIsSubjectWide(e.target.checked)}
                  className="rounded border-ds-border text-ds-accent focus:ring-0"
                  data-testid="subject-wide-toggle"
                />
                <span>Save as subject-wide note ({subjectName || 'Subject'})</span>
              </label>

              <button
                type="submit"
                disabled={savingNote || !noteTitle.trim()}
                className="px-3 py-1.5 rounded bg-ds-accent text-white text-xs font-semibold hover:bg-ds-accent/90 disabled:opacity-50 transition-colors"
                data-testid="save-note-btn"
              >
                {savingNote ? 'Saving...' : 'Save Note'}
              </button>
            </div>

            {saveNoteError && (
              <p className="text-xs text-rose-400">{saveNoteError}</p>
            )}
          </form>
        )}

        {/* Notes List */}
        {loadingNotes ? (
          <div className="p-6 text-center text-xs font-mono text-ds-muted flex items-center justify-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin text-ds-accent" />
            <span>Loading stitched notes...</span>
          </div>
        ) : notesError ? (
          <div className="p-4 rounded bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs">
            {notesError}
          </div>
        ) : mergedNotes.length === 0 ? (
          <div className="p-6 text-center text-xs font-mono text-ds-muted bg-ds-surface rounded-ds border border-ds-border">
            No personal notes recorded for this topic or subject yet. Use Quick Add above!
          </div>
        ) : (
          <div className="space-y-3">
            {mergedNotes.map((note) => (
              <div
                key={note.id}
                className="p-4 rounded-ds bg-ds-surface border border-ds-border space-y-2 break-inside-avoid"
                data-testid={`note-card-${note.id}`}
              >
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    {note.isPinned && (
                      <Pin className="w-3.5 h-3.5 text-ds-accent fill-ds-accent" />
                    )}
                    <span
                      className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded ${
                        note.badge === '[Topic Note]'
                          ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20'
                          : 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                      }`}
                    >
                      {note.badge}
                    </span>
                    <span className="font-semibold text-ds-primary">{note.title}</span>
                  </div>
                  <span className="text-[11px] font-mono text-ds-muted">
                    {note.updatedAt?.slice(0, 10)}
                  </span>
                </div>

                {note.filePath ? (
                  <div className="flex items-center gap-2 text-xs pt-1">
                    <Paperclip className="w-3.5 h-3.5 text-ds-muted" />
                    <a
                      href={noteApi.getFileUrl(note.id)}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-ds-accent hover:underline flex items-center gap-1 font-mono"
                    >
                      <span>{note.originalFilename || 'Attachment'}</span>
                      <ExternalLink className="w-3 h-3" />
                    </a>
                  </div>
                ) : note.contentMd ? (
                  <div className="text-xs text-ds-primary leading-relaxed pt-1">
                    <React.Suspense fallback={<MathText content={note.contentMd} />}>
                      <RichMarkdownText content={note.contentMd} />
                    </React.Suspense>
                  </div>
                ) : null}
              </div>
            ))}
          </div>
        )}
      </section>

      {/* --- SECTION 3: QUESTION DOSSIERS --- */}
      <section className="space-y-4 break-before-page">
        <div className="flex items-center justify-between pb-2 border-b border-ds-border">
          <div className="flex items-center gap-2">
            <HelpCircle className="w-4 h-4 text-ds-accent" />
            <h3 className="text-sm font-mono font-bold text-ds-primary uppercase tracking-wide">
              3. Complete Question Dossiers ({questions.length})
            </h3>
          </div>
          <span className="text-xs font-mono text-ds-muted">
            {solvedQuestions.length} / {questions.length} Solved
          </span>
        </div>

        {questions.length === 0 ? (
          <div className="p-6 text-center text-xs font-mono text-ds-muted bg-ds-surface rounded-ds border border-ds-border">
            No practice questions available for this topic.
          </div>
        ) : (
          <div className="space-y-4">
            {questions.map((q, idx) => {
              const isSolved = solvedIds.has(q.id);
              const isRevealed = !!revealedSolutions[q.id];
              const isAnatomyOpen = expandedAnatomy[q.id] !== undefined;
              const dossier = expandedAnatomy[q.id];
              const isLoadingAutopsy = loadingAnatomy[q.id];

              return (
                <div
                  key={q.id}
                  className="p-5 rounded-ds bg-ds-surface border border-ds-border space-y-3 break-inside-avoid"
                  data-testid={`workbook-question-${q.id}`}
                >
                  {/* Question Header */}
                  <div className="flex items-center justify-between text-xs font-mono">
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-ds-accent">Q{idx + 1}.</span>
                      <span
                        className={`flex items-center gap-1 font-semibold ${
                          isSolved ? 'text-ds-success' : 'text-ds-muted'
                        }`}
                      >
                        {isSolved ? (
                          <CheckCircle2 className="w-3.5 h-3.5" />
                        ) : (
                          <Circle className="w-3.5 h-3.5" />
                        )}
                        <span>{isSolved ? 'SOLVED' : 'UNSOLVED'}</span>
                      </span>
                    </div>

                    <div className="text-ds-muted">
                      {q.year ? `GATE ${q.year}` : 'Practice'} · {q.type} · {q.marks} Mark{q.marks > 1 ? 's' : ''}
                    </div>
                  </div>

                  {/* Statement */}
                  <div className="text-xs text-ds-primary leading-relaxed">
                    <MathText content={q.statement || q.statementSnippet || ''} />
                  </div>

                  {/* Options (MCQ/MSQ) */}
                  {q.options && q.options.length > 0 && (
                    <div className="space-y-1.5 pl-2 border-l border-ds-border/60">
                      {q.options.map((opt: any, optIdx: number) => {
                        const optKey = opt.label || opt.key || String.fromCharCode(65 + optIdx);
                        const optText = opt.text || '';
                        return (
                          <div key={optIdx} className="text-xs text-ds-secondary flex items-start gap-1.5">
                            <span className="font-mono font-bold text-ds-muted">{optKey}.</span>
                            <span><MathText content={optText} /></span>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {/* Interactive Buttons (Print Hidden) */}
                  <div className="flex items-center gap-3 pt-2 print:hidden">
                    <button
                      onClick={() => toggleSolution(q.id)}
                      className="flex items-center gap-1 text-xs font-mono font-semibold text-ds-accent hover:underline"
                      data-testid={`reveal-solution-btn-${q.id}`}
                    >
                      {isRevealed ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
                      <span>{isRevealed ? 'Hide Solution' : 'Reveal Solution'}</span>
                    </button>

                    <button
                      onClick={() => toggleAnatomy(q.id)}
                      className="flex items-center gap-1 text-xs font-mono font-semibold text-purple-400 hover:underline"
                      data-testid={`expand-anatomy-btn-${q.id}`}
                    >
                      <Sparkles className="w-3.5 h-3.5" />
                      <span>{isAnatomyOpen ? 'Hide Anatomy' : 'Cognitive Anatomy'}</span>
                    </button>
                  </div>

                  {/* Solution Block (Always visible in print; conditionally on screen) */}
                  <div
                    className={`${
                      isRevealed ? 'block' : 'hidden print:block'
                    } p-3 rounded bg-ds-surface-raised border border-ds-border space-y-2 text-xs`}
                    data-testid={`solution-block-${q.id}`}
                  >
                    <div className="font-mono font-bold text-ds-success">
                      Answer Key: {q.correctOptions || (q.numericMin !== null && q.numericMax !== null ? `${q.numericMin} to ${q.numericMax}` : 'N/A')}
                    </div>
                    {q.explanation && (
                      <div className="text-ds-primary leading-relaxed">
                        <span className="font-semibold text-ds-secondary block mb-1">Official Solution:</span>
                        <React.Suspense fallback={<MathText content={q.explanation} />}>
                          <RichMarkdownText content={q.explanation} />
                        </React.Suspense>
                      </div>
                    )}
                  </div>

                  {/* Anatomy Dossier Block (Always visible in print if available; conditionally on screen) */}
                  {(isAnatomyOpen || (dossier && typeof window === 'undefined')) && (
                    <div
                      className="p-3.5 rounded bg-purple-500/5 border border-purple-500/20 space-y-2 text-xs"
                      data-testid={`anatomy-block-${q.id}`}
                    >
                      {isLoadingAutopsy ? (
                        <div className="flex items-center gap-2 text-xs font-mono text-ds-muted py-1">
                          <Loader2 className="w-4 h-4 animate-spin text-purple-400" />
                          <span>Loading cognitive anatomy...</span>
                        </div>
                      ) : dossier ? (
                        <div className="space-y-2 font-mono">
                          <div>
                            <span className="font-semibold text-purple-400 block mb-0.5">Tested Concept:</span>
                            <div className="text-ds-primary font-sans">
                              <MathText content={dossier.testedConcept} />
                            </div>
                          </div>
                          {dossier.invariantRule && (
                            <div>
                              <span className="font-semibold text-amber-400 block mb-0.5">Core Invariant:</span>
                              <div className="text-amber-200">
                                <MathText content={dossier.invariantRule} />
                              </div>
                            </div>
                          )}
                          {dossier.trapAnalysis && dossier.trapAnalysis.length > 0 && (
                            <div>
                              <span className="font-semibold text-rose-400 block mb-0.5">Distractor Traps:</span>
                              <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 font-sans">
                                {dossier.trapAnalysis.map((t, tIdx) => (
                                  <div key={tIdx} className="p-1.5 rounded bg-ds-surface border border-ds-border text-[11px]">
                                    <span className="font-bold text-ds-primary">Opt {t.option}: </span>
                                    <span className="text-ds-muted">{t.misconception}</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      ) : (
                        <div className="text-xs font-mono text-ds-muted italic">
                          Cognitive anatomy not yet generated for this question.
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
};

export default TopicWorkbookTab;
