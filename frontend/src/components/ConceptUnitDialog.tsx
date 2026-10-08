import React, { useState, useEffect } from 'react';
import { X, BookOpen, Sparkles, CheckCircle2, AlertTriangle, HelpCircle, ArrowRight, Loader2, RefreshCw } from 'lucide-react';
import { anatomyApi, ConceptUnitDto } from '../api/anatomyApi';
import { MathText } from './MathText';

interface ConceptUnitDialogProps {
  slug: string | null;
  onClose: () => void;
}

export const ConceptUnitDialog: React.FC<ConceptUnitDialogProps> = ({ slug, onClose }) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [generating, setGenerating] = useState<boolean>(false);
  const [unit, setUnit] = useState<ConceptUnitDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [revealedCards, setRevealedCards] = useState<Record<string, boolean>>({});

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setError(null);
    setRevealedCards({});

    anatomyApi.getConceptUnit(slug)
      .then((data) => {
        setUnit(data);
        setLoading(false);
      })
      .catch((err: any) => {
        if (err.status === 404) {
          setUnit(null);
        } else {
          setError(err.message || 'Failed to load concept unit');
        }
        setLoading(false);
      });
  }, [slug]);

  const handleGenerate = async () => {
    if (!slug) return;
    setGenerating(true);
    setError(null);
    try {
      const generated = await anatomyApi.generateConceptUnit(slug);
      setUnit(generated);
    } catch (err: any) {
      if (err.status === 409) {
        setError('Cannot generate concept unit: No questions in the bank have been tagged with this concept yet.');
      } else {
        setError(err.message || 'Failed to generate concept unit');
      }
    } finally {
      setGenerating(false);
    }
  };

  const toggleCardReveal = (cardId: string) => {
    setRevealedCards((prev) => ({ ...prev, [cardId]: !prev[cardId] }));
  };

  if (!slug) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in">
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
                  {unit ? unit.title : slug.split('-').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ')}
                </h2>
                {unit && (
                  <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                    unit.status === 'ACCEPTED'
                      ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      : 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                  }`}>
                    {unit.status === 'ACCEPTED' ? 'CURATED' : 'AI-GENERATED — UNREVIEWED'}
                  </span>
                )}
              </div>
              <p className="text-xs text-neutral-400 font-mono">concept/{slug}</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body Content */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {loading && (
            <div className="flex flex-col items-center justify-center py-16 text-neutral-400 space-y-3">
              <Loader2 className="w-8 h-8 animate-spin text-indigo-400" />
              <p className="text-sm font-medium">Retrieving Concept Unit...</p>
            </div>
          )}

          {!loading && error && (
            <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-medium">Error loading concept</p>
                <p className="text-xs text-rose-400/80 mt-1">{error}</p>
              </div>
            </div>
          )}

          {!loading && !unit && !error && (
            <div className="flex flex-col items-center justify-center py-12 text-center space-y-4">
              <div className="p-3 bg-neutral-800/80 rounded-2xl border border-neutral-700 text-neutral-400">
                <HelpCircle className="w-8 h-8 text-neutral-400" />
              </div>
              <div className="max-w-md">
                <h3 className="text-base font-semibold text-white">Concept Unit Not Yet Compiled</h3>
                <p className="text-xs text-neutral-400 mt-1.5 leading-relaxed">
                  No canonical teaching unit has been compiled for <code className="text-neutral-300 font-mono">{slug}</code> yet. Compile on-demand by synthesizing tagged questions' dossiers.
                </p>
              </div>
              <button
                onClick={handleGenerate}
                disabled={generating}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-lg shadow-indigo-600/20 disabled:opacity-50 transition-all"
              >
                {generating ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Synthesizing Unit...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    Compile Concept Unit
                  </>
                )}
              </button>
            </div>
          )}

          {!loading && unit && (
            <>
              {/* Motivation & Core Idea */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="p-4 rounded-xl bg-[#181a20] border border-[#242730]">
                  <h4 className="text-xs font-semibold text-indigo-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5" /> 1. Motivation
                  </h4>
                  <div className="text-xs text-neutral-300 leading-relaxed">
                    <MathText content={unit.motivation} />
                  </div>
                </div>

                <div className="p-4 rounded-xl bg-[#181a20] border border-[#242730]">
                  <h4 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <CheckCircle2 className="w-3.5 h-3.5" /> 2. Core Idea & Mechanism
                  </h4>
                  <div className="text-xs text-neutral-300 leading-relaxed">
                    <MathText content={unit.coreIdea} />
                  </div>
                </div>
              </div>

              {/* Invariant Rule */}
              {unit.invariant && (
                <div className="p-4 rounded-xl bg-amber-500/5 border border-amber-500/20">
                  <h4 className="text-xs font-semibold text-amber-400 uppercase tracking-wider mb-1.5">
                    3. Fundamental Invariant
                  </h4>
                  <div className="text-sm font-medium text-amber-200">
                    <MathText content={unit.invariant} />
                  </div>
                </div>
              )}

              {/* Worked Example (Mandatory PC-1) */}
              <div className="p-5 rounded-xl bg-[#181a20] border border-[#242730]">
                <h4 className="text-xs font-semibold text-sky-400 uppercase tracking-wider mb-3 flex items-center gap-1.5">
                  <ArrowRight className="w-3.5 h-3.5" /> 4. Canonical Worked Example (Step-by-Step)
                </h4>
                <div className="text-xs text-neutral-300 leading-relaxed whitespace-pre-wrap font-sans bg-[#131417] p-4 rounded-lg border border-[#202227]">
                  <MathText content={unit.workedExample} />
                </div>
              </div>

              {/* Distractor Traps */}
              {unit.traps && (
                <div className="p-4 rounded-xl bg-rose-500/5 border border-rose-500/20">
                  <h4 className="text-xs font-semibold text-rose-400 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <AlertTriangle className="w-3.5 h-3.5" /> 5. Common Pitfalls & Traps
                  </h4>
                  <div className="text-xs text-neutral-300 leading-relaxed">
                    <MathText content={unit.traps} />
                  </div>
                </div>
              )}

              {/* Active Recall Self-Check Cards */}
              {unit.cards && unit.cards.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-semibold text-purple-400 uppercase tracking-wider flex items-center gap-1.5">
                    <RefreshCw className="w-3.5 h-3.5" /> 6. Active Recall Cards ({unit.cards.length})
                  </h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {unit.cards.map((card, idx) => {
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

              {/* Deep Link Reference */}
              {unit.deepLinkRef && (
                <div className="px-4 py-3 rounded-xl bg-neutral-900 border border-neutral-800 flex items-center justify-between text-xs">
                  <span className="text-neutral-400">Standard Reference:</span>
                  <span className="font-mono text-indigo-300 font-medium">{unit.deepLinkRef}</span>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};
