import React, { useState, useEffect } from 'react';
import { X, BookOpen, Sparkles, CheckCircle2, AlertTriangle, HelpCircle, ArrowRight, Loader2, RefreshCw, FileQuestion } from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { anatomyApi, ConceptUnitDto } from '../api/anatomyApi';
import { ConceptLessonResponse, ConceptLesson, ConceptMappedQuestionItem } from '../types/journey';
import { MathText } from './MathText';

interface ConceptUnitDialogProps {
  slug: string | null;
  onClose: () => void;
}

export const ConceptUnitDialog: React.FC<ConceptUnitDialogProps> = ({ slug, onClose }) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [generating, setGenerating] = useState<boolean>(false);
  const [lessonData, setLessonData] = useState<ConceptLessonResponse | null>(null);
  const [legacyUnit, setLegacyUnit] = useState<ConceptUnitDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [revealedCards, setRevealedCards] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setError(null);
    setRevealedCards({});
    setLessonData(null);
    setLegacyUnit(null);

    // Try journeyApi first (WP2 canonical concept lessons)
    journeyApi.getConceptLesson(slug)
      .then((data) => {
        setLessonData(data);
        setLoading(false);
      })
      .catch((journeyErr: any) => {
        // Fallback to legacy anatomyApi if not found in journey concept tags
        anatomyApi.getConceptUnit(slug)
          .then((legacy) => {
            setLegacyUnit(legacy);
            setLoading(false);
          })
          .catch((legacyErr: any) => {
            if (journeyErr.status === 404 || legacyErr.status === 404) {
              setLessonData(null);
              setLegacyUnit(null);
            } else {
              setError(journeyErr.message || legacyErr.message || 'Failed to load concept lesson');
            }
            setLoading(false);
          });
      });
  }, [slug]);

  const handleGenerate = async () => {
    if (!slug) return;
    setGenerating(true);
    setError(null);
    try {
      const generated = await journeyApi.generateConceptLesson(slug);
      setLessonData(generated);
    } catch (err: any) {
      try {
        const legacy = await anatomyApi.generateConceptUnit(slug);
        setLegacyUnit(legacy);
      } catch (legacyErr: any) {
        if (err.status === 409 || legacyErr.status === 409) {
          setError('Cannot generate concept lesson: No questions in the bank have been tagged with this concept yet.');
        } else {
          setError(err.message || legacyErr.message || 'Failed to generate concept lesson');
        }
      }
    } finally {
      setGenerating(false);
    }
  };

  const toggleCardReveal = (cardId: string) => {
    setRevealedCards((prev) => ({ ...prev, [cardId]: !prev[cardId] }));
  };

  if (!slug) return null;

  const lesson = lessonData?.lesson;
  const concept = lessonData?.concept;
  const mappedQuestions = lessonData?.mappedQuestions || [];

  const title = lesson?.title || legacyUnit?.title || concept?.label || slug.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
  const status = lesson?.status || legacyUnit?.status;
  const motivation = lesson?.motivation || legacyUnit?.motivation;
  const coreIdea = lesson?.coreIdea || legacyUnit?.coreIdea;
  const invariant = lesson?.invariant || legacyUnit?.invariant;
  const workedExample = lesson?.workedExample || legacyUnit?.workedExample;
  const traps = lesson?.traps || legacyUnit?.traps;
  const workedExampleExternalId = lesson?.workedExampleExternalId;
  const cards = legacyUnit?.cards || [];

  const hasContent = Boolean(lesson || legacyUnit);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in" data-testid="concept-unit-dialog">
      <div 
        className="relative w-full max-w-4xl max-h-[90vh] flex flex-col bg-[#121316] border border-[#2a2d34] rounded-2xl shadow-2xl overflow-hidden text-neutral-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#242730] bg-[#16181d]">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-indigo-500/10 border border-indigo-500/20 rounded-xl text-indigo-400">
              <BookOpen className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-semibold text-white tracking-tight">
                  {title}
                </h2>
                {status && (
                  <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                    status === 'ACCEPTED'
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                  }`}>
                    {status === 'ACCEPTED' ? 'CURATED' : 'AI-GENERATED — UNREVIEWED'}
                  </span>
                )}
              </div>
              <p className="text-xs text-neutral-400 font-mono">concept/{slug}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
            data-testid="close-concept-dialog-btn"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {loading && (
            <div className="flex flex-col items-center justify-center py-16 text-neutral-400 space-y-3">
              <Loader2 className="w-8 h-8 animate-spin text-indigo-400" />
              <p className="text-sm font-medium">Retrieving Concept Lesson...</p>
            </div>
          )}

          {!loading && error && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-medium">Error loading concept lesson</p>
                <p className="text-xs text-rose-400/80 mt-1">{error}</p>
              </div>
            </div>
          )}

          {!loading && !hasContent && !error && (
            <div className="flex flex-col items-center justify-center py-12 text-center space-y-4">
              <div className="p-3 bg-neutral-800/80 rounded-2xl border border-neutral-700 text-neutral-400">
                <HelpCircle className="w-8 h-8 text-neutral-400" />
              </div>
              <div className="max-w-md">
                <h3 className="text-base font-semibold text-white">Concept Lesson Not Yet Compiled</h3>
                <p className="text-xs text-neutral-400 mt-1.5 leading-relaxed">
                  No canonical teaching lesson has been synthesized for <code className="text-neutral-300 font-mono">{slug}</code> yet. Synthesize on-demand using authentic questions from the bank.
                </p>
              </div>
              <button
                onClick={handleGenerate}
                disabled={generating}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-lg shadow-indigo-600/20 disabled:opacity-50 transition-all"
                data-testid="compile-concept-lesson-btn"
              >
                {generating ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Synthesizing Lesson...</span>
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    <span>Compile Concept Lesson</span>
                  </>
                )}
              </button>
            </div>
          )}

          {!loading && hasContent && (
            <>
              {/* Motivation & Core Idea */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {motivation && (
                  <div className="p-4 rounded-xl bg-[#181a20] border border-[#242730]">
                    <h4 className="text-xs font-semibold text-indigo-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5" /> 1. Motivation
                    </h4>
                    <div className="text-xs text-neutral-300 leading-relaxed">
                      <MathText content={motivation} />
                    </div>
                  </div>
                )}

                {coreIdea && (
                  <div className="p-4 rounded-xl bg-[#181a20] border border-[#242730]">
                    <h4 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5" /> 2. Core Idea & Mechanism
                    </h4>
                    <div className="text-xs text-neutral-300 leading-relaxed">
                      <MathText content={coreIdea} />
                    </div>
                  </div>
                )}
              </div>

              {/* Invariant Rule */}
              {invariant && (
                <div className="p-4 rounded-xl bg-amber-500/5 border border-amber-500/20">
                  <h4 className="text-xs font-semibold text-amber-400 uppercase tracking-wider mb-1.5">
                    3. Fundamental Invariant
                  </h4>
                  <div className="text-sm font-medium text-amber-200">
                    <MathText content={invariant} />
                  </div>
                </div>
              )}

              {/* Worked Example (citing externalId if present) */}
              {workedExample && (
                <div className="p-5 rounded-xl bg-[#181a20] border border-[#242730] space-y-2">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-semibold text-sky-400 uppercase tracking-wider flex items-center gap-1.5">
                      <ArrowRight className="w-3.5 h-3.5" /> 4. Canonical Worked Example (Step-by-Step)
                    </h4>
                    {workedExampleExternalId && (
                      <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-neutral-800 border border-neutral-700 text-neutral-300">
                        Source: <strong className="text-white">{workedExampleExternalId}</strong>
                      </span>
                    )}
                  </div>
                  <div className="text-xs text-neutral-300 leading-relaxed whitespace-pre-wrap font-sans bg-[#131417] p-4 rounded-lg border border-[#202227]">
                    <MathText content={workedExample} />
                  </div>
                </div>
              )}

              {/* Distractor Traps */}
              {traps && (
                <div className="p-4 rounded-xl bg-rose-500/5 border border-rose-500/20">
                  <h4 className="text-xs font-semibold text-rose-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <AlertTriangle className="w-3.5 h-3.5" /> 5. Common Pitfalls & Traps
                  </h4>
                  <div className="text-xs text-neutral-300 leading-relaxed">
                    <MathText content={traps} />
                  </div>
                </div>
              )}

              {/* Practice These Questions Rail (Mapped questions) */}
              {mappedQuestions.length > 0 && (
                <div className="space-y-3 pt-2">
                  <h4 className="text-xs font-semibold text-indigo-400 uppercase tracking-wider flex items-center gap-1.5">
                    <FileQuestion className="w-3.5 h-3.5" /> 6. Practice These Questions ({mappedQuestions.length})
                  </h4>
                  <div className="space-y-2">
                    {mappedQuestions.map((q) => (
                      <div
                        key={q.id}
                        className="p-3 rounded-xl bg-[#181a20] border border-[#242730] flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-xs font-semibold text-white">
                              {q.externalId}
                            </span>
                            <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-neutral-800 border border-neutral-700 text-neutral-300">
                              {q.type}
                            </span>
                            {q.difficulty && (
                              <span className="px-1.5 py-0.5 rounded text-[10px] font-mono bg-amber-500/10 text-amber-300 border border-amber-500/20">
                                {q.difficulty}
                              </span>
                            )}
                          </div>
                          <p className="text-xs text-neutral-400 mt-1 line-clamp-1">
                            {q.statement}
                          </p>
                        </div>
                        <button
                          onClick={() => {
                            onClose();
                            window.location.href = `/questions/${q.id}`;
                          }}
                          className="self-start sm:self-center shrink-0 flex items-center gap-1 px-3 py-1.5 rounded-lg bg-neutral-800 hover:bg-neutral-700 text-white border border-neutral-700 text-xs font-medium transition-colors"
                        >
                          <span>Solve</span>
                          <ArrowRight className="w-3 h-3 text-indigo-400" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Active Recall Self-Check Cards */}
              {cards.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-semibold text-purple-400 uppercase tracking-wider flex items-center gap-1.5">
                    <RefreshCw className="w-3.5 h-3.5" /> Active Recall Cards ({cards.length})
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {cards.map((card, idx) => {
                      const isRevealed = !!revealedCards[card.id];
                      return (
                        <div
                          key={card.id}
                          className="p-4 rounded-xl bg-[#181a20] border border-[#242730] flex flex-col justify-between"
                        >
                          <div>
                            <span className="text-[10px] font-semibold text-neutral-400 tracking-wider">
                              CARD #{idx + 1}
                            </span>
                            <div className="text-xs font-medium text-white mt-1">
                              <MathText content={card.prompt} />
                            </div>
                          </div>

                          <div className="mt-4 pt-3 border-t border-[#242730]/60">
                            {isRevealed ? (
                              <div className="text-xs text-emerald-300 bg-emerald-500/10 p-2.5 rounded-lg border border-emerald-500/20 animate-fade-in">
                                <MathText content={card.answer} />
                              </div>
                            ) : (
                              <button
                                onClick={() => toggleCardReveal(card.id)}
                                className="w-full py-1.5 text-xs font-medium text-neutral-400 hover:text-white bg-neutral-800/60 hover:bg-neutral-800 rounded-lg transition-colors border border-neutral-700/50"
                              >
                                Reveal Answer
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
