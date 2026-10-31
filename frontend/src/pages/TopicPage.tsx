import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  BookOpen,
  HelpCircle,
  CheckCircle2,
  Circle,
  Sparkles,
  AlertTriangle,
  FileQuestion,
  BookMarked,
  Layers,
  ArrowRight,
  ExternalLink,
  Loader2,
  Pencil,
  Trash2,
  RefreshCw,
  Check,
  X
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { TopicPrimerResponse, TopicPrimer } from '../types/journey';
import { MathText } from '../components/MathText';
import { ConceptUnitDialog } from '../components/ConceptUnitDialog';
import { TopicConceptsTab } from '../components/TopicConceptsTab';
import { TopicWorkbookTab } from '../components/TopicWorkbookTab';
import { useQuestions } from '../hooks/useQueries';
import { useAuth } from '../context/AuthContext';

const RichMarkdownText = React.lazy(() => import('../components/ai/RichMarkdownText'));

export const TopicPage: React.FC = () => {
  const { topicId } = useParams<{ topicId: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const tabParam = searchParams.get('tab');
  const conceptParam = searchParams.get('concept');

  const [activeTab, setActiveTab] = useState<'theory' | 'concepts' | 'questions' | 'notebook' | 'workbook'>(
    tabParam === 'concepts' || tabParam === 'questions' || tabParam === 'notebook' || tabParam === 'workbook' ? tabParam : 'theory'
  );
  const [primerData, setPrimerData] = useState<TopicPrimerResponse | null>(null);
  const [loadingPrimer, setLoadingPrimer] = useState<boolean>(true);
  const [generatingPrimer, setGeneratingPrimer] = useState<boolean>(false);
  const [deletingPrimer, setDeletingPrimer] = useState<boolean>(false);
  const [isEditingPrimer, setIsEditingPrimer] = useState<boolean>(false);
  const [savingPrimerEdit, setSavingPrimerEdit] = useState<boolean>(false);
  const [primerEditForm, setPrimerEditForm] = useState<{
    motivation: string;
    coreIdea: string;
    invariant: string;
    workedExample: string;
    traps: string;
  }>({
    motivation: '',
    coreIdea: '',
    invariant: '',
    workedExample: '',
    traps: '',
  });
  const [primerError, setPrimerError] = useState<string | null>(null);

  // Selected concept for ConceptUnitDialog / TopicConceptsTab
  const [selectedConceptSlug, setSelectedConceptSlug] = useState<string | null>(conceptParam);

  useEffect(() => {
    const tab = searchParams.get('tab');
    const concept = searchParams.get('concept');
    if (tab === 'concepts') {
      setActiveTab('concepts');
      if (concept) {
        setSelectedConceptSlug(concept);
      }
    } else if (tab === 'questions' || tab === 'notebook' || tab === 'theory' || tab === 'workbook') {
      setActiveTab(tab);
    }
  }, [searchParams]);

  // Questions for this topic
  const { data: questionsPage, isLoading: loadingQuestions } = useQuestions({
    topicId: topicId,
    size: 50,
  });

  useEffect(() => {
    let isMounted = true;
    async function loadPrimer() {
      if (!topicId) return;
      try {
        setLoadingPrimer(true);
        setPrimerError(null);
        const data = await journeyApi.getTopicPrimer(topicId);
        if (isMounted) {
          setPrimerData(data);
        }
      } catch (err: any) {
        if (isMounted) {
          setPrimerError(err?.message || 'Failed to load topic theory primer');
        }
      } finally {
        if (isMounted) {
          setLoadingPrimer(false);
        }
      }
    }
    loadPrimer();
    return () => {
      isMounted = false;
    };
  }, [topicId]);

  const handleGeneratePrimer = async (force: unknown = false) => {
    if (!topicId || (force === true && !isAdmin)) return;
    try {
      setGeneratingPrimer(true);
      setPrimerError(null);
      const isForce = force === true;
      const generated = isForce
        ? await journeyApi.generateTopicPrimer(topicId, { force: true })
        : await journeyApi.generateTopicPrimer(topicId);
      setPrimerData(generated);
      setIsEditingPrimer(false);
    } catch (err: any) {
      setPrimerError(err?.message || 'Failed to generate topic primer');
    } finally {
      setGeneratingPrimer(false);
    }
  };

  const handleStartEdit = () => {
    if (!isAdmin || !primerData?.primer) return;
    const p = primerData.primer;
    setPrimerEditForm({
      motivation: p.motivation || '',
      coreIdea: p.coreIdea || '',
      invariant: p.invariant || '',
      workedExample: p.workedExample || '',
      traps: p.traps || '',
    });
    setIsEditingPrimer(true);
  };

  const handleSaveEdit = async () => {
    if (!topicId || !isAdmin) return;
    try {
      setSavingPrimerEdit(true);
      setPrimerError(null);
      const updated = await journeyApi.updateTopicPrimer(topicId, primerEditForm);
      if (primerData) {
        setPrimerData({
          ...primerData,
          primer: updated,
        });
      }
      setIsEditingPrimer(false);
    } catch (err: any) {
      setPrimerError(err?.message || 'Failed to update topic primer');
    } finally {
      setSavingPrimerEdit(false);
    }
  };

  const handleDeletePrimer = async () => {
    if (!topicId || !isAdmin) return;
    const confirmed = window.confirm('Are you sure you want to delete this topic theory primer?');
    if (!confirmed) return;
    try {
      setDeletingPrimer(true);
      setPrimerError(null);
      await journeyApi.deleteTopicPrimer(topicId);
      if (primerData) {
        setPrimerData({
          ...primerData,
          primer: null,
        });
      }
      setIsEditingPrimer(false);
    } catch (err: any) {
      setPrimerError(err?.message || 'Failed to delete topic primer');
    } finally {
      setDeletingPrimer(false);
    }
  };

  const primer = primerData?.primer;
  const questions = questionsPage?.content || [];
  const solvedQuestions = questions.filter(
    (q) => q.pass1Status === 'SOLVED'
  );

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-5xl mx-auto" data-testid="topic-page">
      {/* Top Header / Back Navigation */}
      <div className="flex items-center justify-between border-b border-ds-border pb-4">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-xs font-mono font-semibold text-ds-secondary hover:text-ds-primary transition-colors"
          data-testid="back-to-roadmap-btn"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Back to Roadmap</span>
        </button>

        <div className="flex items-center gap-2">
          {primerData?.bookChapterRef && (
            <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-ds-surface border border-ds-border text-ds-muted">
              {primerData.bookChapterRef}
            </span>
          )}
        </div>
      </div>

      {/* Topic Title & Stepper Tabs */}
      <div className="space-y-4">
        <div>
          <div className="text-xs font-mono text-ds-accent uppercase tracking-wider font-semibold">
            Topic Learning Stepper
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-ds-primary mt-1">
            {topicId?.replace(/-/g, ' ').replace('topic ', '').toUpperCase()}
          </h1>
        </div>

        {/* 4 Stepper Tabs (Freely Switchable - PC-5) */}
        <div className="flex border-b border-ds-border gap-2" role="tablist">
          <button
            onClick={() => setActiveTab('theory')}
            role="tab"
            aria-selected={activeTab === 'theory'}
            className={`px-4 py-2.5 text-xs font-semibold font-mono border-b-2 transition-all flex items-center gap-2 ${
              activeTab === 'theory'
                ? 'border-ds-accent text-ds-primary bg-ds-surface-hover'
                : 'border-transparent text-ds-muted hover:text-ds-primary hover:bg-ds-surface'
            }`}
            data-testid="tab-theory"
          >
            <BookOpen className="w-3.5 h-3.5 text-ds-accent" />
            <span>1. Topic Orientation</span>
            {primer && (
              <span className="w-2 h-2 rounded-full bg-ds-success" />
            )}
          </button>

          <button
            onClick={() => setActiveTab('concepts')}
            role="tab"
            aria-selected={activeTab === 'concepts'}
            className={`px-4 py-2.5 text-xs font-semibold font-mono border-b-2 transition-all flex items-center gap-2 ${
              activeTab === 'concepts'
                ? 'border-ds-accent text-ds-primary bg-ds-surface-hover'
                : 'border-transparent text-ds-muted hover:text-ds-primary hover:bg-ds-surface'
            }`}
            data-testid="tab-concepts"
          >
            <Layers className="w-3.5 h-3.5 text-ds-accent" />
            <span>2. Concepts</span>
          </button>

          <button
            onClick={() => setActiveTab('questions')}
            role="tab"
            aria-selected={activeTab === 'questions'}
            className={`px-4 py-2.5 text-xs font-semibold font-mono border-b-2 transition-all flex items-center gap-2 ${
              activeTab === 'questions'
                ? 'border-ds-accent text-ds-primary bg-ds-surface-hover'
                : 'border-transparent text-ds-muted hover:text-ds-primary hover:bg-ds-surface'
            }`}
            data-testid="tab-questions"
          >
            <FileQuestion className="w-3.5 h-3.5 text-ds-info" />
            <span>3. Practice Questions</span>
            <span className="text-[11px] text-ds-muted">({questions.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('notebook')}
            role="tab"
            aria-selected={activeTab === 'notebook'}
            className={`px-4 py-2.5 text-xs font-semibold font-mono border-b-2 transition-all flex items-center gap-2 ${
              activeTab === 'notebook'
                ? 'border-ds-accent text-ds-primary bg-ds-surface-hover'
                : 'border-transparent text-ds-muted hover:text-ds-primary hover:bg-ds-surface'
            }`}
            data-testid="tab-notebook"
          >
            <BookMarked className="w-3.5 h-3.5 text-ds-warning" />
            <span>4. Solved Notebook</span>
            <span className="text-[11px] text-ds-muted">({solvedQuestions.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('workbook')}
            role="tab"
            aria-selected={activeTab === 'workbook'}
            className={`px-4 py-2.5 text-xs font-semibold font-mono border-b-2 transition-all flex items-center gap-2 ${
              activeTab === 'workbook'
                ? 'border-ds-accent text-ds-primary bg-ds-surface-hover'
                : 'border-transparent text-ds-muted hover:text-ds-primary hover:bg-ds-surface'
            }`}
            data-testid="tab-workbook"
          >
            <BookMarked className="w-3.5 h-3.5 text-ds-accent" />
            <span>5. Reference Workbook</span>
          </button>
        </div>
      </div>

      {/* TAB 1: THEORY PRIMER */}
      {activeTab === 'theory' && (
        <div className="space-y-6" data-testid="theory-tab-content">
          {loadingPrimer ? (
            <div className="p-8 text-center text-xs font-mono text-ds-muted">
              Loading topic theory primer...
            </div>
          ) : primer ? (
            <div className="space-y-6">
              {/* Primer Status Badge & Management Controls */}
              <div className="flex flex-wrap items-center justify-between gap-3 bg-ds-surface border border-ds-border rounded-ds p-3 text-xs font-mono">
                <div className="flex items-center gap-2">
                  {primer.status === 'DRAFT' ? (
                    <span
                      className="px-2 py-0.5 rounded bg-ds-warning/15 border border-ds-warning/30 text-ds-warning font-bold text-[11px]"
                      data-testid="draft-badge"
                    >
                      AI-GENERATED — UNREVIEWED
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 rounded bg-ds-success/15 border border-ds-success/30 text-ds-success font-bold text-[11px]">
                      {primer.status}
                    </span>
                  )}
                  {primer.generatorModel && (
                    <span className="text-ds-muted">Model: {primer.generatorModel}</span>
                  )}
                </div>

                {/* Management Action Buttons */}
                {isAdmin && (
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleStartEdit}
                    disabled={isEditingPrimer || generatingPrimer || deletingPrimer}
                    className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-ds-primary transition-colors disabled:opacity-50"
                    data-testid="edit-primer-btn"
                    title="Edit Theory Primer content"
                  >
                    <Pencil className="w-3.5 h-3.5 text-ds-accent" />
                    <span>Edit</span>
                  </button>

                  <button
                    onClick={() => handleGeneratePrimer(true)}
                    disabled={generatingPrimer || deletingPrimer}
                    className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-ds-primary transition-colors disabled:opacity-50"
                    data-testid="regenerate-primer-btn"
                    title="Regenerate Theory Primer with AI"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 text-ds-info ${generatingPrimer ? 'animate-spin' : ''}`} />
                    <span>{generatingPrimer ? 'Regenerating...' : 'Regenerate'}</span>
                  </button>

                  <button
                    onClick={handleDeletePrimer}
                    disabled={deletingPrimer || generatingPrimer}
                    className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold rounded bg-ds-surface-raised hover:bg-ds-danger/15 border border-ds-border text-ds-danger transition-colors disabled:opacity-50"
                    data-testid="delete-primer-btn"
                    title="Delete Theory Primer from database"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>{deletingPrimer ? 'Deleting...' : 'Delete'}</span>
                  </button>
                </div>
                )}
              </div>

              {/* Error Banner if generation failed or parseError is present */}
              {primer.parseError && (
                <div className="bg-ds-danger/10 border border-ds-danger/30 rounded-ds p-4 text-xs font-mono space-y-3" data-testid="primer-error-banner">
                  <div className="flex items-center gap-2 text-ds-danger font-bold uppercase tracking-wider">
                    <AlertTriangle className="w-4 h-4" />
                    <span>Primer Synthesis Incomplete or Provider Error</span>
                  </div>
                  <div className="text-ds-muted text-[11px] bg-ds-bg/60 p-2.5 rounded border border-ds-border font-mono overflow-x-auto whitespace-pre-wrap">
                    {primer.parseError}
                  </div>
                  {isAdmin && (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleGeneratePrimer(true)}
                      disabled={generatingPrimer}
                      className="px-3 py-1.5 bg-ds-accent text-ds-accent-contrast rounded text-xs font-bold flex items-center gap-1.5 hover:opacity-90 disabled:opacity-50"
                    >
                      <RefreshCw className={`w-3.5 h-3.5 ${generatingPrimer ? 'animate-spin' : ''}`} />
                      <span>Retry Synthesis Now</span>
                    </button>
                    <button
                      onClick={handleDeletePrimer}
                      disabled={deletingPrimer}
                      className="px-3 py-1.5 bg-ds-surface border border-ds-border hover:bg-ds-danger/10 text-ds-danger rounded text-xs font-bold flex items-center gap-1.5"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      <span>Discard Failed Draft</span>
                    </button>
                  </div>
                  )}
                </div>
              )}

              {primerError && (
                <div className="bg-ds-danger/10 border border-ds-danger/30 rounded-ds p-3 text-xs font-mono text-ds-danger">
                  {primerError}
                </div>
              )}

              {/* EDIT FORM (when isEditingPrimer is true) */}
              {isAdmin && isEditingPrimer ? (
                <div className="bg-ds-surface border border-ds-accent/40 rounded-ds p-6 space-y-5" data-testid="primer-edit-form">
                  <div className="flex items-center justify-between border-b border-ds-border pb-3">
                    <div className="flex items-center gap-2">
                      <Pencil className="w-4 h-4 text-ds-accent" />
                      <h3 className="font-bold text-sm text-ds-primary font-mono uppercase tracking-wider">
                        Edit Theory Primer
                      </h3>
                    </div>
                    <span className="text-[11px] font-mono text-ds-muted">LaTeX / KaTeX ($...$) supported</span>
                  </div>

                  <div className="space-y-4 text-xs font-mono">
                    <div className="space-y-1.5">
                      <label className="font-bold text-ds-accent uppercase tracking-wider block">
                        Motivation & Engineering Context
                      </label>
                      <textarea
                        value={primerEditForm.motivation}
                        onChange={(e) => setPrimerEditForm({ ...primerEditForm, motivation: e.target.value })}
                        rows={4}
                        className="w-full p-3 rounded bg-ds-bg border border-ds-border text-ds-primary text-xs font-mono focus:border-ds-accent focus:outline-none"
                        placeholder="Why is this topic foundational in GATE CSE..."
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="font-bold text-ds-info uppercase tracking-wider block">
                        Core Idea & Definitions
                      </label>
                      <textarea
                        value={primerEditForm.coreIdea}
                        onChange={(e) => setPrimerEditForm({ ...primerEditForm, coreIdea: e.target.value })}
                        rows={4}
                        className="w-full p-3 rounded bg-ds-bg border border-ds-border text-ds-primary text-xs font-mono focus:border-ds-accent focus:outline-none"
                        placeholder="Mathematical or algorithmic definitions..."
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="font-bold text-ds-warning uppercase tracking-wider block">
                        Core Invariant (Optional)
                      </label>
                      <textarea
                        value={primerEditForm.invariant}
                        onChange={(e) => setPrimerEditForm({ ...primerEditForm, invariant: e.target.value })}
                        rows={2}
                        className="w-full p-3 rounded bg-ds-bg border border-ds-border text-ds-primary text-xs font-mono focus:border-ds-accent focus:outline-none"
                        placeholder="The fundamental invariant that always holds..."
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="font-bold text-ds-success uppercase tracking-wider block">
                        Canonical Worked Example (Step-by-Step)
                      </label>
                      <textarea
                        value={primerEditForm.workedExample}
                        onChange={(e) => setPrimerEditForm({ ...primerEditForm, workedExample: e.target.value })}
                        rows={8}
                        className="w-full p-3 rounded bg-ds-bg border border-ds-border text-ds-primary text-xs font-mono focus:border-ds-accent focus:outline-none"
                        placeholder="Detailed canonical problem with step-by-step numbered calculations..."
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="font-bold text-ds-danger uppercase tracking-wider block">
                        Examiner Traps & Common Distractors (Optional)
                      </label>
                      <textarea
                        value={primerEditForm.traps}
                        onChange={(e) => setPrimerEditForm({ ...primerEditForm, traps: e.target.value })}
                        rows={4}
                        className="w-full p-3 rounded bg-ds-bg border border-ds-border text-ds-primary text-xs font-mono focus:border-ds-accent focus:outline-none"
                        placeholder="Common pitfalls and misconceptions..."
                      />
                    </div>
                  </div>

                  <div className="flex items-center justify-end gap-3 pt-3 border-t border-ds-border">
                    <button
                      onClick={() => setIsEditingPrimer(false)}
                      disabled={savingPrimerEdit}
                      className="px-4 py-2 rounded border border-ds-border text-ds-secondary hover:text-ds-primary text-xs font-mono font-bold flex items-center gap-1.5 transition-colors"
                    >
                      <X className="w-3.5 h-3.5" />
                      <span>Cancel</span>
                    </button>
                    <button
                      onClick={handleSaveEdit}
                      disabled={savingPrimerEdit}
                      className="px-4 py-2 bg-ds-accent text-ds-accent-contrast rounded text-xs font-mono font-bold flex items-center gap-1.5 hover:opacity-90 disabled:opacity-50 transition-opacity"
                    >
                      <Check className="w-3.5 h-3.5" />
                      <span>{savingPrimerEdit ? 'Saving...' : 'Save Changes'}</span>
                    </button>
                  </div>
                </div>
              ) : (
                <>
                  {/* Motivation Section */}
                  <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-2">
                    <div className="text-xs font-mono font-bold text-ds-accent uppercase tracking-wider">
                      Motivation & Engineering Context
                    </div>
                    <div className="text-sm text-ds-primary leading-relaxed">
                      <React.Suspense fallback={<MathText content={primer.motivation} />}>
                        <RichMarkdownText className="text-sm" content={primer.motivation} />
                      </React.Suspense>
                    </div>
                  </div>

                  {/* Core Idea Section */}
                  <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-2">
                    <div className="text-xs font-mono font-bold text-ds-info uppercase tracking-wider">
                      Core Idea & Definitions
                    </div>
                    <div className="text-sm text-ds-primary leading-relaxed">
                      <React.Suspense fallback={<MathText content={primer.coreIdea} />}>
                        <RichMarkdownText className="text-sm" content={primer.coreIdea} />
                      </React.Suspense>
                    </div>
                  </div>

                  {/* Fundamental Invariant */}
                  {primer.invariant && (
                    <div className="bg-ds-surface-raised border border-ds-border-strong rounded-ds p-5 space-y-2">
                      <div className="text-xs font-mono font-bold text-ds-warning uppercase tracking-wider">
                        Core Invariant
                      </div>
                      <div className="text-sm text-ds-primary font-mono leading-relaxed bg-ds-bg/50 p-3 rounded border border-ds-border">
                        <React.Suspense fallback={<MathText content={primer.invariant} />}>
                          <RichMarkdownText className="text-sm font-mono" content={primer.invariant} />
                        </React.Suspense>
                      </div>
                    </div>
                  )}

                  {/* Worked Example (Mandatory >= 150 words) */}
                  <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="text-xs font-mono font-bold text-ds-success uppercase tracking-wider">
                        Canonical Worked Example
                      </div>
                      <span className="text-[11px] font-mono text-ds-muted">Step-by-Step Walkthrough</span>
                    </div>
                    <div className="text-sm text-ds-primary leading-relaxed space-y-2">
                      <React.Suspense fallback={<MathText content={primer.workedExample} />}>
                        <RichMarkdownText className="text-sm" content={primer.workedExample} />
                      </React.Suspense>
                    </div>
                  </div>

                  {/* Traps & Pitfalls */}
                  {primer.traps && (
                    <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-2">
                      <div className="text-xs font-mono font-bold text-ds-danger uppercase tracking-wider flex items-center gap-1.5">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        <span>Examiner Traps & Common Distractors</span>
                      </div>
                      <div className="text-sm text-ds-secondary leading-relaxed">
                        <React.Suspense fallback={<MathText content={primer.traps} />}>
                          <RichMarkdownText className="text-sm" content={primer.traps} />
                        </React.Suspense>
                      </div>
                    </div>
                  )}

                  {/* Prerequisite Concept Units */}
                  {primerData?.conceptSlugs && primerData.conceptSlugs.length > 0 && (
                    <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-3">
                      <div className="text-xs font-mono font-bold text-ds-secondary uppercase tracking-wider flex items-center gap-1.5">
                        <Layers className="w-3.5 h-3.5 text-ds-accent" />
                        <span>Prerequisite Concept Units</span>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {primerData.conceptSlugs.map((slug) => (
                          <button
                            key={slug}
                            onClick={() => {
                              setSelectedConceptSlug(slug);
                              setActiveTab('concepts');
                            }}
                            className="px-3 py-1.5 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-xs font-mono text-ds-primary flex items-center gap-1.5 transition-colors"
                            data-testid={`concept-chip-${slug}`}
                          >
                            <span>{slug}</span>
                            <ExternalLink className="w-3 h-3 text-ds-muted" />
                          </button>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          ) : (
            <div className="bg-ds-surface border border-ds-border rounded-ds p-8 text-center space-y-4" data-testid="empty-primer-box">
              <div className="w-12 h-12 rounded-full bg-ds-accent/10 border border-ds-accent/20 flex items-center justify-center text-ds-accent mx-auto">
                <BookOpen className="w-6 h-6" />
              </div>
              <div className="space-y-1">
                <h3 className="font-bold text-base text-ds-primary">
                  Theory Primer Not Yet Generated
                </h3>
                <p className="text-xs text-ds-secondary max-w-md mx-auto">
                  Study this topic from first principles. Generate a curated theory primer grounded on authentic GATE questions.
                </p>
              </div>

              {primerError && (
                <p className="text-xs text-ds-danger font-medium">{primerError}</p>
              )}

              <button
                onClick={handleGeneratePrimer}
                disabled={generatingPrimer}
                className="px-5 py-2.5 bg-ds-accent text-ds-accent-contrast rounded-ds text-xs font-bold inline-flex items-center gap-2 hover:opacity-90 disabled:opacity-50 transition-all shadow-sm"
                data-testid="generate-primer-btn"
              >
                {generatingPrimer ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Synthesizing Theory Primer...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    <span>Generate Theory Primer</span>
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      )}

      {/* TAB 2: CONCEPTS & LESSONS */}
      {activeTab === 'concepts' && topicId && (
        <TopicConceptsTab
          topicId={topicId}
          isAdmin={isAdmin}
          selectedSlug={selectedConceptSlug}
          onSelectSlug={(slug) => setSelectedConceptSlug(slug)}
        />
      )}

      {/* TAB 3: QUESTIONS */}
      {activeTab === 'questions' && (
        <div className="space-y-4" data-testid="questions-tab-content">
          <div className="flex items-center justify-between text-xs font-mono text-ds-muted pb-2 border-b border-ds-border">
            <span>Showing {questions.length} questions for this topic</span>
            <span>Solved: {solvedQuestions.length} / {questions.length}</span>
          </div>

          {loadingQuestions ? (
            <div className="p-8 text-center text-xs font-mono text-ds-muted">
              Loading topic questions...
            </div>
          ) : questions.length === 0 ? (
            <div className="p-8 text-center text-xs font-mono text-ds-muted bg-ds-surface rounded-ds border border-ds-border">
              No questions found in this topic yet.
            </div>
          ) : (
            <div className="space-y-2.5">
              {questions.map((q) => {
                const isSolved = q.pass1Status === 'SOLVED';
                return (
                  <div
                    key={q.id}
                    onClick={() => navigate(`/questions/${q.id}`)}
                    className="bg-ds-surface hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong rounded-ds p-4 flex items-center justify-between gap-4 transition-all cursor-pointer group shadow-sm"
                    data-testid={`topic-question-${q.id}`}
                  >
                    <div className="flex items-center gap-3">
                      {isSolved ? (
                        <CheckCircle2 className="w-4 h-4 text-ds-success shrink-0" />
                      ) : (
                        <Circle className="w-4 h-4 text-ds-muted shrink-0" />
                      )}

                      <div className="space-y-1">
                        <div className="flex items-center gap-2 text-[11px] font-mono">
                          <span className="font-bold text-ds-accent">{q.year}</span>
                          <span className="text-ds-muted">·</span>
                          <span className="text-ds-secondary">{q.type}</span>
                          <span className="text-ds-muted">·</span>
                          <span className="text-ds-secondary">{q.marks} Marks</span>
                        </div>
                        <div className="text-sm text-ds-primary group-hover:text-ds-accent transition-colors line-clamp-2">
                          <MathText content={q.statement} />
                        </div>
                      </div>
                    </div>

                    <div className="text-ds-muted group-hover:text-ds-primary transition-colors shrink-0">
                      <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* TAB 3: NOTEBOOK */}
      {activeTab === 'notebook' && (
        <div className="space-y-4" data-testid="notebook-tab-content">
          <div className="flex items-center justify-between text-xs font-mono text-ds-muted pb-2 border-b border-ds-border">
            <span>Solved Log ({solvedQuestions.length} entries)</span>
          </div>

          {solvedQuestions.length === 0 ? (
            <div className="p-8 text-center text-xs font-mono text-ds-muted bg-ds-surface rounded-ds border border-ds-border">
              No questions solved in this topic yet. Start solving in the Practice Questions tab!
            </div>
          ) : (
            <div className="space-y-3">
              {solvedQuestions.map((q) => (
                <div
                  key={q.id}
                  onClick={() => navigate(`/questions/${q.id}`)}
                  className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-2 cursor-pointer hover:bg-ds-surface-hover transition-colors"
                >
                  <div className="flex items-center justify-between text-xs font-mono">
                    <span className="text-ds-success font-semibold flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      {q.pass1Status}
                    </span>
                    <span className="text-ds-muted">{q.year} · {q.type}</span>
                  </div>
                  <div className="text-sm text-ds-primary">
                    <MathText content={q.statement} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* TAB 5: REFERENCE WORKBOOK */}
      {activeTab === 'workbook' && (
        <TopicWorkbookTab
          topicId={topicId || ''}
          topicName={primer?.topicName || questions[0]?.topicName || topicId || ''}
          subjectId={questions[0]?.subjectId || ''}
          subjectName={questions[0]?.subjectName || ''}
          bookChapterRef={primerData?.bookChapterRef}
          primer={primer || null}
          questions={questions}
          solvedQuestions={solvedQuestions}
        />
      )}

      {/* Concept Unit Dialog Modal (only when outside concepts tab) */}
      {selectedConceptSlug && activeTab !== 'concepts' && (
        <ConceptUnitDialog
          slug={selectedConceptSlug}
          onClose={() => setSelectedConceptSlug(null)}
        />
      )}
    </div>
  );
};

export default TopicPage;
