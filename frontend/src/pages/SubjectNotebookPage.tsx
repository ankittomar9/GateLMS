import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  BookMarked, 
  Star, 
  Sparkles, 
  Key, 
  AlertOctagon, 
  ChevronDown, 
  ChevronUp, 
  ExternalLink,
  Layers,
  Clock,
  CheckCircle2,
  Filter,
  Loader2
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { anatomyApi, QuestionAnatomyDto } from '../api/anatomyApi';
import { SubjectNotebookResponse, NotebookEntry } from '../types/journey';
import { MathText } from '../components/MathText';

export const SubjectNotebookPage: React.FC = () => {
  const { subjectId } = useParams<{ subjectId: string }>();
  const navigate = useNavigate();

  const [notebook, setNotebook] = useState<SubjectNotebookResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [starredOnly, setStarredOnly] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Expanded anatomy dossiers by questionId
  const [expandedAnatomy, setExpandedAnatomy] = useState<Record<string, QuestionAnatomyDto | null>>({});
  const [loadingAnatomy, setLoadingAnatomy] = useState<Record<string, boolean>>({});

  useEffect(() => {
    let isMounted = true;
    async function loadNotebook() {
      if (!subjectId) return;
      try {
        setLoading(true);
        setError(null);
        const data = await journeyApi.getSubjectNotebook(subjectId, starredOnly);
        if (isMounted) {
          setNotebook(data);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err?.message || 'Failed to load subject notebook');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }
    loadNotebook();
    return () => {
      isMounted = false;
    };
  }, [subjectId, starredOnly]);

  const toggleAnatomy = async (questionId: string) => {
    if (expandedAnatomy[questionId] !== undefined) {
      // Toggle off
      const next = { ...expandedAnatomy };
      delete next[questionId];
      setExpandedAnatomy(next);
      return;
    }

    // Fetch anatomy dossier
    setLoadingAnatomy((prev) => ({ ...prev, [questionId]: true }));
    try {
      const dossier = await anatomyApi.getQuestionAnatomy(questionId);
      setExpandedAnatomy((prev) => ({ ...prev, [questionId]: dossier }));
    } catch {
      setExpandedAnatomy((prev) => ({ ...prev, [questionId]: null }));
    } finally {
      setLoadingAnatomy((prev) => ({ ...prev, [questionId]: false }));
    }
  };

  if (loading) {
    return (
      <div className="p-8 space-y-6 max-w-5xl mx-auto" data-testid="notebook-loading">
        <div className="h-16 bg-ds-surface animate-pulse rounded-ds border border-ds-border" />
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-28 bg-ds-surface animate-pulse rounded-ds border border-ds-border" />
          ))}
        </div>
      </div>
    );
  }

  if (error || !notebook) {
    return (
      <div className="p-8 max-w-5xl mx-auto text-center space-y-4">
        <p className="text-ds-danger font-medium">{error || 'Notebook not found'}</p>
        <button
          onClick={() => navigate(`/subjects/${subjectId}`)}
          className="px-4 py-2 bg-ds-surface-raised border border-ds-border text-ds-primary rounded-ds text-sm font-semibold hover:bg-ds-surface"
        >
          Back to Subject
        </button>
      </div>
    );
  }

  const { subjectName, topics, totalEntries } = notebook;

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-5xl mx-auto" data-testid="subject-notebook-page">
      {/* Top Header & Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-ds-border pb-5">
        <button
          onClick={() => navigate(`/subjects/${subjectId}`)}
          className="flex items-center gap-1.5 text-xs font-mono font-semibold text-ds-secondary hover:text-ds-primary transition-colors"
          data-testid="back-to-subject-btn"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>Roadmap: {subjectName}</span>
        </button>

        {/* Filter Toggle: All vs Starred Only */}
        <div className="flex items-center gap-2 bg-ds-surface border border-ds-border p-1 rounded-ds">
          <button
            type="button"
            onClick={() => setStarredOnly(false)}
            className={`px-3 py-1.5 rounded-ds text-xs font-semibold font-mono transition-all ${
              !starredOnly
                ? 'bg-ds-accent text-ds-accent-contrast shadow-sm'
                : 'text-ds-muted hover:text-ds-secondary'
            }`}
            data-testid="filter-all-btn"
          >
            All Solved ({totalEntries})
          </button>
          <button
            type="button"
            onClick={() => setStarredOnly(true)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-ds text-xs font-semibold font-mono transition-all ${
              starredOnly
                ? 'bg-ds-accent text-ds-accent-contrast shadow-sm'
                : 'text-ds-muted hover:text-ds-secondary'
            }`}
            data-testid="filter-starred-btn"
          >
            <Star className="w-3.5 h-3.5 text-ds-warning fill-ds-warning" />
            <span>Starred Only</span>
          </button>
        </div>
      </div>

      {/* Subject Title & Stats */}
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <BookMarked className="w-5 h-5 text-ds-warning" />
          <h1 className="text-2xl font-bold tracking-tight text-ds-primary">
            {subjectName} Solved Notebook
          </h1>
        </div>
        <p className="text-xs text-ds-muted font-mono">
          Structured log of solved exam problems · Grouped by syllabus topic · Grounded concept autopsies
        </p>
      </div>

      {/* Topics Solved Groups */}
      {topics.length === 0 ? (
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-12 text-center space-y-3 shadow-sm"
          data-testid="notebook-empty-state"
        >
          <div className="w-12 h-12 rounded-full bg-ds-surface-raised border border-ds-border flex items-center justify-center mx-auto text-ds-muted">
            <BookMarked className="w-6 h-6" />
          </div>
          <h3 className="text-base font-bold text-ds-primary">
            {starredOnly ? 'No Starred Questions in this Subject' : 'No Solved Questions Recorded Yet'}
          </h3>
          <p className="text-xs text-ds-secondary max-w-md mx-auto">
            {starredOnly
              ? 'Star key questions during practice or review to build your high-yield revision set.'
              : 'Start tackling questions from the subject roadmap. Solved questions will automatically appear here with full concept dossiers.'}
          </p>
          <button
            type="button"
            onClick={() => navigate(`/subjects/${subjectId}`)}
            className="px-4 py-2 bg-ds-accent text-ds-accent-contrast rounded-ds text-xs font-semibold inline-flex items-center gap-2 hover:opacity-90"
          >
            <span>Go to Subject Roadmap</span>
          </button>
        </div>
      ) : (
        <div className="space-y-6" data-testid="notebook-topics-list">
          {topics.map((topicGroup) => (
            <div 
              key={topicGroup.topicId}
              className="bg-ds-surface border border-ds-border rounded-ds overflow-hidden shadow-sm space-y-0"
              data-testid={`topic-group-${topicGroup.topicId}`}
            >
              {/* Topic Group Header */}
              <div className="bg-ds-surface-raised border-b border-ds-border px-5 py-3 flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <span className="text-xs font-mono font-bold text-ds-muted">
                    #{topicGroup.sequence}
                  </span>
                  <h3 className="font-bold text-sm text-ds-primary">
                    {topicGroup.topicName}
                  </h3>
                </div>
                <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-ds-surface border border-ds-border text-ds-secondary">
                  {topicGroup.entries.length} solved
                </span>
              </div>

              {/* Topic Solved Entries */}
              <div className="divide-y divide-ds-border">
                {topicGroup.entries.map((entry: NotebookEntry) => {
                  const isExpanded = expandedAnatomy[entry.questionId] !== undefined;
                  const dossier = expandedAnatomy[entry.questionId];
                  const isLoadingDossier = loadingAnatomy[entry.questionId];

                  return (
                    <div 
                      key={entry.questionId}
                      className="p-5 space-y-3 transition-colors hover:bg-ds-surface-hover/30"
                      data-testid={`notebook-entry-${entry.questionId}`}
                    >
                      {/* Entry Metadata & Badges */}
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div className="flex items-center gap-2 text-[11px] font-mono">
                          {entry.year && (
                            <span className="font-bold text-ds-accent">
                              GATE {entry.year}
                            </span>
                          )}
                          {entry.source && (
                            <span className="text-ds-muted">
                              [{entry.source}]
                            </span>
                          )}
                          {entry.starred && (
                            <span className="flex items-center gap-1 text-ds-warning font-semibold">
                              <Star className="w-3 h-3 fill-ds-warning text-ds-warning" />
                              <span>Starred</span>
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => toggleAnatomy(entry.questionId)}
                            className="flex items-center gap-1 px-2.5 py-1 rounded bg-ds-surface-raised border border-ds-border text-[11px] font-mono font-semibold text-ds-secondary hover:text-ds-primary transition-colors"
                            data-testid={`btn-toggle-anatomy-${entry.questionId}`}
                          >
                            <Sparkles className="w-3 h-3 text-ds-accent" />
                            <span>{isExpanded ? 'Hide Dossier' : 'Concept Dossier'}</span>
                            {isExpanded ? (
                              <ChevronUp className="w-3 h-3 text-ds-muted" />
                            ) : (
                              <ChevronDown className="w-3 h-3 text-ds-muted" />
                            )}
                          </button>

                          <button
                            type="button"
                            onClick={() => navigate(`/questions/${entry.questionId}`)}
                            className="p-1.5 rounded hover:bg-ds-surface-raised text-ds-muted hover:text-ds-primary transition-colors"
                            title="Open full question"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>

                      {/* Statement Snippet */}
                      <div className="text-xs text-ds-primary leading-relaxed">
                        <MathText content={entry.statementSnippet} />
                      </div>

                      {/* Expanded Anatomy Dossier View (Zero new AI calls - PC-6) */}
                      {isExpanded && (
                        <div 
                          className="mt-3 p-4 rounded-ds bg-ds-surface-raised border border-ds-border space-y-3 text-xs animate-in fade-in"
                          data-testid={`anatomy-dossier-${entry.questionId}`}
                        >
                          {isLoadingDossier ? (
                            <div className="flex items-center gap-2 text-xs font-mono text-ds-muted py-2">
                              <Loader2 className="w-4 h-4 animate-spin text-ds-accent" />
                              <span>Loading concept autopsy...</span>
                            </div>
                          ) : dossier ? (
                            <div className="space-y-3">
                              <div>
                                <span className="font-semibold text-indigo-400 block mb-1">
                                  1. Tested Concept:
                                </span>
                                <div className="text-ds-primary">
                                  <MathText content={dossier.testedConcept} />
                                </div>
                              </div>

                              {dossier.invariantRule && (
                                <div className="p-2.5 rounded bg-amber-500/5 border border-amber-500/20 text-amber-200">
                                  <span className="font-semibold text-amber-400 block mb-1">
                                    2. Core Invariant:
                                  </span>
                                  <div className="font-mono">
                                    <MathText content={dossier.invariantRule} />
                                  </div>
                                </div>
                              )}

                              {dossier.trapAnalysis && dossier.trapAnalysis.length > 0 && (
                                <div>
                                  <span className="font-semibold text-rose-400 block mb-1.5">
                                    3. Distractor Autopsy:
                                  </span>
                                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                                    {dossier.trapAnalysis.map((trap, idx) => (
                                      <div key={idx} className="p-2 rounded bg-ds-surface border border-ds-border text-[11px]">
                                        <div className="font-bold text-ds-primary mb-0.5">Option {trap.option}</div>
                                        <div className="text-ds-muted italic"><MathText content={trap.misconception} /></div>
                                      </div>
                                    ))}
                                  </div>
                                </div>
                              )}
                            </div>
                          ) : (
                            <div className="text-xs font-mono text-ds-muted italic py-1">
                              Anatomy dossier not yet generated for this question. Open question detail to study textbook derivation.
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default SubjectNotebookPage;
