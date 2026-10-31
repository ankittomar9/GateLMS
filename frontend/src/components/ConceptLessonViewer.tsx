import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BookOpen,
  Sparkles,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ArrowLeft,
  Loader2,
  Pencil,
  Trash2,
  RefreshCw,
  Check,
  FileQuestion,
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { ConceptLessonResponse } from '../types/journey';
import { MathText } from './MathText';

const RichMarkdownText = React.lazy(() => import('./ai/RichMarkdownText'));

interface ConceptLessonViewerProps {
  slug: string;
  isAdmin: boolean;
  onBack: () => void;
  onLessonMutated?: () => void;
}

export const ConceptLessonViewer: React.FC<ConceptLessonViewerProps> = ({
  slug,
  isAdmin,
  onBack,
  onLessonMutated,
}) => {
  const navigate = useNavigate();

  const [lessonData, setLessonData] = useState<ConceptLessonResponse | null>(null);
  const [loadingLesson, setLoadingLesson] = useState<boolean>(true);
  const [generatingLesson, setGeneratingLesson] = useState<boolean>(false);
  const [deletingLesson, setDeletingLesson] = useState<boolean>(false);
  const [savingLessonEdit, setSavingLessonEdit] = useState<boolean>(false);
  const [isEditingLesson, setIsEditingLesson] = useState<boolean>(false);
  const [lessonError, setLessonError] = useState<string | null>(null);

  const [lessonEditForm, setLessonEditForm] = useState<{
    title: string;
    motivation: string;
    coreIdea: string;
    invariant: string;
    workedExample: string;
    traps: string;
    workedExampleExternalId: string;
    status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  }>({
    title: '',
    motivation: '',
    coreIdea: '',
    invariant: '',
    workedExample: '',
    traps: '',
    workedExampleExternalId: '',
    status: 'DRAFT',
  });

  useEffect(() => {
    let isMounted = true;
    const loadLesson = async () => {
      try {
        setLoadingLesson(true);
        setLessonError(null);
        setIsEditingLesson(false);
        const data = await journeyApi.getConceptLesson(slug);
        if (isMounted) {
          setLessonData(data);
        }
      } catch (err: any) {
        if (isMounted) {
          if (err?.status === 404) {
            setLessonData(null);
          } else {
            setLessonError(err?.message || 'Failed to load concept lesson');
          }
        }
      } finally {
        if (isMounted) {
          setLoadingLesson(false);
        }
      }
    };

    loadLesson();
    return () => {
      isMounted = false;
    };
  }, [slug]);

  const handleGenerateLesson = async (force: boolean = false) => {
    if (!slug || (force && !isAdmin)) return;
    try {
      setGeneratingLesson(true);
      setLessonError(null);
      const generated = await journeyApi.generateConceptLesson(slug, { force });
      setLessonData(generated);
      setIsEditingLesson(false);
      onLessonMutated?.();
    } catch (err: any) {
      setLessonError(err?.message || 'Failed to generate concept lesson');
    } finally {
      setGeneratingLesson(false);
    }
  };

  const handleStartEdit = () => {
    if (!isAdmin || !lessonData?.lesson) return;
    const l = lessonData.lesson;
    setLessonEditForm({
      title: l.title || '',
      motivation: l.motivation || '',
      coreIdea: l.coreIdea || '',
      invariant: l.invariant || '',
      workedExample: l.workedExample || '',
      traps: l.traps || '',
      workedExampleExternalId: l.workedExampleExternalId || '',
      status: l.status,
    });
    setIsEditingLesson(true);
  };

  const handleSaveEdit = async () => {
    if (!slug || !isAdmin) return;
    try {
      setSavingLessonEdit(true);
      setLessonError(null);
      const updated = await journeyApi.updateConceptLesson(slug, lessonEditForm);
      if (lessonData) {
        setLessonData({
          ...lessonData,
          lesson: updated,
        });
      }
      setIsEditingLesson(false);
      onLessonMutated?.();
    } catch (err: any) {
      setLessonError(err?.message || 'Failed to update concept lesson');
    } finally {
      setSavingLessonEdit(false);
    }
  };

  const handleQuickAccept = async () => {
    if (!slug || !isAdmin || !lessonData?.lesson) return;
    try {
      setSavingLessonEdit(true);
      setLessonError(null);
      const updated = await journeyApi.updateConceptLesson(slug, {
        status: 'ACCEPTED',
      });
      if (lessonData) {
        setLessonData({
          ...lessonData,
          lesson: updated,
        });
      }
      onLessonMutated?.();
    } catch (err: any) {
      setLessonError(err?.message || 'Failed to accept concept lesson');
    } finally {
      setSavingLessonEdit(false);
    }
  };

  const handleDeleteLesson = async () => {
    if (!slug || !isAdmin) return;
    const confirmed = window.confirm('Are you sure you want to delete this concept lesson?');
    if (!confirmed) return;
    try {
      setDeletingLesson(true);
      setLessonError(null);
      await journeyApi.deleteConceptLesson(slug);
      if (lessonData) {
        setLessonData({
          ...lessonData,
          lesson: null,
        });
      }
      setIsEditingLesson(false);
      onLessonMutated?.();
    } catch (err: any) {
      setLessonError(err?.message || 'Failed to delete concept lesson');
    } finally {
      setDeletingLesson(false);
    }
  };

  const lesson = lessonData?.lesson;
  const concept = lessonData?.concept;
  const mappedQuestions = lessonData?.mappedQuestions || [];

  return (
    <div className="space-y-6" data-testid="concept-lesson-view">
      {/* Navigation & Lesson Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-ds-border pb-4">
        <div className="flex items-center gap-3">
          <button
            onClick={onBack}
            className="flex items-center gap-1.5 text-xs font-mono font-semibold text-ds-secondary hover:text-ds-primary transition-colors px-2.5 py-1.5 rounded-ds bg-ds-surface border border-ds-border"
            data-testid="back-to-concepts-btn"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Concepts</span>
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-bold text-ds-primary">
                {lesson?.title || concept?.label || slug.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')}
              </h2>
              {lesson ? (
                <span
                  data-testid="lesson-status-badge"
                  className={`px-2 py-0.5 text-xs font-medium rounded-full font-mono ${
                    lesson.status === 'ACCEPTED'
                      ? 'bg-ds-success/15 text-ds-success border border-ds-success/30'
                      : 'bg-ds-warning/15 text-ds-warning border border-ds-warning/30'
                  }`}
                >
                  {lesson.status === 'ACCEPTED' ? 'CURATED' : 'AI-GENERATED — UNREVIEWED'}
                </span>
              ) : (
                <span className="px-2 py-0.5 text-xs font-mono text-ds-muted bg-ds-surface-raised border border-ds-border rounded-full">
                  NOT YET COMPILED
                </span>
              )}
            </div>
            <p className="text-xs text-ds-muted font-mono mt-0.5">concept/{slug}</p>
          </div>
        </div>

        {/* Action buttons */}
        <div className="flex items-center gap-2">
          {isAdmin && lesson && !isEditingLesson && (
            <>
              {lesson.status !== 'ACCEPTED' && (
                <button
                  onClick={handleQuickAccept}
                  disabled={savingLessonEdit}
                  className="flex items-center gap-1 px-3 py-1.5 bg-ds-success/15 hover:bg-ds-success/25 text-ds-success border border-ds-success/30 rounded-ds text-xs font-semibold font-mono transition-colors"
                  data-testid="accept-lesson-btn"
                  title="Mark status as ACCEPTED (Curated)"
                >
                  <Check className="w-3.5 h-3.5" />
                  <span>Accept</span>
                </button>
              )}
              <button
                onClick={handleStartEdit}
                className="flex items-center gap-1 px-3 py-1.5 bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-secondary hover:text-ds-primary border border-ds-border rounded-ds text-xs font-semibold font-mono transition-colors"
                data-testid="edit-lesson-btn"
              >
                <Pencil className="w-3.5 h-3.5" />
                <span>Edit</span>
              </button>
              <button
                onClick={() => handleGenerateLesson(true)}
                disabled={generatingLesson}
                className="flex items-center gap-1 px-3 py-1.5 bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-secondary hover:text-ds-primary border border-ds-border rounded-ds text-xs font-semibold font-mono transition-colors disabled:opacity-50"
                data-testid="regenerate-lesson-btn"
                title="Force re-synthesis via LLM"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${generatingLesson ? 'animate-spin' : ''}`} />
                <span>Regenerate</span>
              </button>
              <button
                onClick={handleDeleteLesson}
                disabled={deletingLesson}
                className="flex items-center gap-1 px-3 py-1.5 bg-ds-danger/10 hover:bg-ds-danger/20 text-ds-danger border border-ds-danger/30 rounded-ds text-xs font-semibold font-mono transition-colors disabled:opacity-50"
                data-testid="delete-lesson-btn"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>Delete</span>
              </button>
            </>
          )}
        </div>
      </div>

      {/* Error notification */}
      {lessonError && (
        <div
          className="p-4 rounded-ds bg-ds-danger/10 border border-ds-danger/25 text-ds-danger text-xs flex items-start gap-2.5"
          data-testid="lesson-error-banner"
        >
          <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Lesson Error</p>
            <p className="mt-0.5 opacity-90">{lessonError}</p>
          </div>
        </div>
      )}

      {/* Loading state */}
      {loadingLesson && (
        <div className="flex flex-col items-center justify-center py-16 text-ds-muted space-y-3">
          <Loader2 className="w-8 h-8 animate-spin text-ds-accent" />
          <p className="text-xs font-mono">Retrieving Concept Lesson...</p>
        </div>
      )}

      {/* Empty state: Not yet generated */}
      {!loadingLesson && !lesson && !isEditingLesson && (
        <div className="bg-ds-surface border border-ds-border rounded-ds p-8 text-center space-y-4">
          <div className="w-12 h-12 rounded-full bg-ds-surface-raised border border-ds-border flex items-center justify-center mx-auto text-ds-muted">
            <BookOpen className="w-6 h-6" />
          </div>
          <div className="max-w-md mx-auto">
            <h3 className="text-sm font-bold text-ds-primary">Concept Lesson Not Yet Compiled</h3>
            <p className="text-xs text-ds-secondary mt-1 leading-relaxed">
              No canonical teaching lesson has been synthesized for this concept yet.
              Synthesize on-demand using grounded questions from the bank.
            </p>
          </div>
          <button
            onClick={() => handleGenerateLesson(false)}
            disabled={generatingLesson}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-ds bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-semibold font-mono shadow-sm disabled:opacity-50 transition-colors"
            data-testid="generate-lesson-btn"
          >
            {generatingLesson ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Synthesizing Lesson...</span>
              </>
            ) : (
              <>
                <Sparkles className="w-3.5 h-3.5" />
                <span>Synthesize Concept Lesson</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Edit Form */}
      {isEditingLesson && (
        <div className="bg-ds-surface border border-ds-border rounded-ds p-6 space-y-4" data-testid="lesson-edit-form">
          <div className="flex items-center justify-between border-b border-ds-border pb-3">
            <h3 className="text-xs font-mono font-semibold text-ds-accent uppercase tracking-wider">
              Edit Concept Lesson (Admin Curation)
            </h3>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsEditingLesson(false)}
                className="px-3 py-1.5 text-xs font-mono font-semibold text-ds-muted hover:text-ds-primary rounded-ds border border-ds-border"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveEdit}
                disabled={savingLessonEdit}
                className="px-3 py-1.5 text-xs font-mono font-semibold text-white bg-ds-accent hover:bg-ds-accent-hover rounded-ds flex items-center gap-1.5"
                data-testid="save-lesson-edit-btn"
              >
                {savingLessonEdit ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Check className="w-3.5 h-3.5" />}
                <span>Save Changes</span>
              </button>
            </div>
          </div>

          <div className="space-y-3">
            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">Status</label>
              <select
                value={lessonEditForm.status}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, status: e.target.value as any })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              >
                <option value="DRAFT">DRAFT</option>
                <option value="REVIEWED">REVIEWED</option>
                <option value="ACCEPTED">ACCEPTED (Curated)</option>
                <option value="REJECTED">REJECTED</option>
              </select>
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">Title</label>
              <input
                type="text"
                value={lessonEditForm.title}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, title: e.target.value })}
                className="w-full text-xs bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">1. Motivation (LaTeX supported)</label>
              <textarea
                rows={3}
                value={lessonEditForm.motivation}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, motivation: e.target.value })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">2. Core Idea & Mechanism (KaTeX formulas)</label>
              <textarea
                rows={4}
                value={lessonEditForm.coreIdea}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, coreIdea: e.target.value })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">3. Fundamental Invariant Rule</label>
              <textarea
                rows={2}
                value={lessonEditForm.invariant}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, invariant: e.target.value })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">
                Worked Example External ID (must be a real mapped question, e.g. SO-OS-0001)
              </label>
              <input
                type="text"
                value={lessonEditForm.workedExampleExternalId}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, workedExampleExternalId: e.target.value })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">4. Canonical Worked Example (Step-by-Step)</label>
              <textarea
                rows={6}
                value={lessonEditForm.workedExample}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, workedExample: e.target.value })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>

            <div>
              <label className="text-xs font-mono text-ds-muted block mb-1">5. Common Pitfalls & Traps (2-4 bullet points)</label>
              <textarea
                rows={3}
                value={lessonEditForm.traps}
                onChange={(e) => setLessonEditForm({ ...lessonEditForm, traps: e.target.value })}
                className="w-full text-xs font-mono bg-ds-surface-raised border border-ds-border rounded-ds p-2 text-ds-primary"
              />
            </div>
          </div>
        </div>
      )}

      {/* Lesson Body Content (PC-2 Sections) */}
      {!loadingLesson && lesson && !isEditingLesson && (
        <div className="space-y-6">
          {/* Grid: Motivation & Core Idea */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-5 rounded-ds bg-ds-surface border border-ds-border">
              <h4 className="text-xs font-semibold text-ds-accent uppercase tracking-wider mb-2 flex items-center gap-1.5 font-mono">
                <Sparkles className="w-3.5 h-3.5" />
                <span>1. Motivation</span>
              </h4>
              <div className="text-xs text-ds-secondary leading-relaxed" data-testid="lesson-motivation">
                <React.Suspense fallback={<MathText content={lesson.motivation} />}>
                  <RichMarkdownText className="text-xs" content={lesson.motivation} />
                </React.Suspense>
              </div>
            </div>

            <div className="p-5 rounded-ds bg-ds-surface border border-ds-border">
              <h4 className="text-xs font-semibold text-ds-success uppercase tracking-wider mb-2 flex items-center gap-1.5 font-mono">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>2. Core Idea & Mechanism</span>
              </h4>
              <div className="text-xs text-ds-secondary leading-relaxed" data-testid="lesson-core-idea">
                <React.Suspense fallback={<MathText content={lesson.coreIdea} />}>
                  <RichMarkdownText className="text-xs" content={lesson.coreIdea} />
                </React.Suspense>
              </div>
            </div>
          </div>

          {/* Invariant */}
          {lesson.invariant && (
            <div className="p-4 rounded-ds bg-ds-warning/10 border border-ds-warning/25">
              <h4 className="text-xs font-semibold text-ds-warning uppercase tracking-wider mb-1.5 font-mono">
                3. Fundamental Invariant
              </h4>
              <div className="text-xs font-medium text-ds-primary" data-testid="lesson-invariant">
                <React.Suspense fallback={<MathText content={lesson.invariant} />}>
                  <RichMarkdownText className="text-xs font-mono" content={lesson.invariant} />
                </React.Suspense>
              </div>
            </div>
          )}

          {/* Worked Example (citing workedExampleExternalId) */}
          <div className="p-5 rounded-ds bg-ds-surface border border-ds-border space-y-3">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <h4 className="text-xs font-semibold text-ds-info uppercase tracking-wider flex items-center gap-1.5 font-mono">
                <ArrowRight className="w-3.5 h-3.5" />
                <span>4. Canonical Worked Example (Step-by-Step)</span>
              </h4>
              {lesson.workedExampleExternalId && (
                <span
                  className="text-xs font-mono px-2 py-0.5 rounded bg-ds-surface-raised border border-ds-border text-ds-muted flex items-center gap-1"
                  data-testid="worked-example-citation"
                >
                  <span>Source:</span>
                  <strong className="text-ds-primary">{lesson.workedExampleExternalId}</strong>
                </span>
              )}
            </div>
            <div
              className="text-xs text-ds-secondary leading-relaxed font-sans bg-ds-surface-raised p-4 rounded-ds border border-ds-border"
              data-testid="lesson-worked-example"
            >
              <React.Suspense fallback={<MathText content={lesson.workedExample} />}>
                <RichMarkdownText className="text-xs" content={lesson.workedExample} />
              </React.Suspense>
            </div>
          </div>

          {/* Traps */}
          {lesson.traps && (
            <div className="p-4 rounded-ds bg-ds-danger/10 border border-ds-danger/25">
              <h4 className="text-xs font-semibold text-ds-danger uppercase tracking-wider mb-2 flex items-center gap-1.5 font-mono">
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>5. Common Pitfalls & Traps</span>
              </h4>
              <div className="text-xs text-ds-secondary leading-relaxed" data-testid="lesson-traps">
                <React.Suspense fallback={<MathText content={lesson.traps} />}>
                  <RichMarkdownText className="text-xs" content={lesson.traps} />
                </React.Suspense>
              </div>
            </div>
          )}

          {/* Practice These Questions Rail (Computed mapped questions) */}
          <div className="p-5 rounded-ds bg-ds-surface border border-ds-border space-y-4" data-testid="practice-questions-rail">
            <div className="flex items-center justify-between border-b border-ds-border pb-3">
              <div>
                <h4 className="text-xs font-semibold text-ds-primary uppercase tracking-wider font-mono flex items-center gap-1.5">
                  <FileQuestion className="w-3.5 h-3.5 text-ds-accent" />
                  <span>Practice These Questions ({mappedQuestions.length})</span>
                </h4>
                <p className="text-xs text-ds-muted mt-0.5">
                  Directly mapped questions testing {lesson.title || slug}
                </p>
              </div>
            </div>

            {mappedQuestions.length === 0 ? (
              <p className="text-xs text-ds-muted italic py-2">
                No questions currently mapped to this concept.
              </p>
            ) : (
              <div className="space-y-2">
                {mappedQuestions.map((q) => (
                  <div
                    key={q.id}
                    className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:border-ds-accent/40 transition-colors"
                    data-testid={`practice-item-${q.externalId}`}
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-semibold text-ds-primary">
                          {q.externalId}
                        </span>
                        <span className="px-1.5 py-0.2 rounded text-[10px] font-mono border border-ds-border text-ds-muted">
                          {q.type}
                        </span>
                        {q.difficulty && (
                          <span className="px-1.5 py-0.2 rounded text-[10px] font-mono bg-ds-warning/10 text-ds-warning border border-ds-warning/25">
                            {q.difficulty}
                          </span>
                        )}
                        {q.year && (
                          <span className="text-[10px] font-mono text-ds-muted">
                            GATE {q.year}
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-ds-secondary line-clamp-2">
                        {q.statement}
                      </p>
                    </div>

                    <button
                      onClick={() => navigate(`/questions/${q.id}`)}
                      className="self-start sm:self-center shrink-0 flex items-center gap-1 px-3 py-1.5 rounded-ds bg-ds-surface hover:bg-ds-surface-hover text-ds-primary border border-ds-border text-xs font-mono font-medium transition-colors"
                      data-testid={`solve-q-${q.externalId}`}
                    >
                      <span>Solve</span>
                      <ArrowRight className="w-3 h-3 text-ds-accent" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
