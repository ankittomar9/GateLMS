import React, { useCallback, useEffect, useState } from 'react';
import {
  X,
  Brain,
  Eye,
  EyeOff,
  Loader2,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  PartyPopper,
  RotateCcw,
} from 'lucide-react';
import { revisionApi, DueCardDto, CardGrade } from '../api/revisionApi';
import { MathText } from './MathText';

interface CardReviewDrawerProps {
  open: boolean;
  onClose: () => void;
  /** Called after the drawer closes having reviewed at least one card, so the page can refresh counts. */
  onSessionComplete?: (reviewedCount: number) => void;
}

const GRADE_OPTIONS: { grade: CardGrade; label: string; hint: string; testId: string; className: string }[] = [
  {
    grade: 0,
    label: 'Forgot',
    hint: 'Blank or wrong — reschedule tomorrow',
    testId: 'grade-forgot-btn',
    className: 'bg-rose-600/90 hover:bg-rose-500 text-white border-rose-500/40',
  },
  {
    grade: 1,
    label: 'Shaky',
    hint: 'Partial or slow — lengthen slightly',
    testId: 'grade-shaky-btn',
    className: 'bg-amber-600/90 hover:bg-amber-500 text-white border-amber-500/40',
  },
  {
    grade: 2,
    label: 'Solid',
    hint: 'Confident and fast — expand the interval',
    testId: 'grade-solid-btn',
    className: 'bg-emerald-600/90 hover:bg-emerald-500 text-white border-emerald-500/40',
  },
];

/**
 * CardReviewDrawer — the WP2 flashcard flow (PHASE7-M1-ANATOMY-SPEC §6.3):
 * prompt -> user attempts aloud / on paper -> reveal answer -> self-grade 0/1/2 -> next.
 * SM-2-lite rescheduling happens server-side; the drawer only advances the queue.
 */
export const CardReviewDrawer: React.FC<CardReviewDrawerProps> = ({ open, onClose, onSessionComplete }) => {
  const [cards, setCards] = useState<DueCardDto[]>([]);
  const [index, setIndex] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reviewedCount, setReviewedCount] = useState(0);
  const [sessionDone, setSessionDone] = useState(false);

  const loadDueCards = useCallback(async () => {
    setLoading(true);
    setError(null);
    setSessionDone(false);
    try {
      const data = await revisionApi.getDueCards();
      setCards(data.cards);
      setIndex(0);
      setRevealed(false);
      setReviewedCount(0);
    } catch (err: any) {
      setError(err?.message || 'Failed to load due cards');
      setCards([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (open) {
      loadDueCards();
    }
  }, [open, loadDueCards]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        handleFinish();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  if (!open) return null;

  const currentCard = cards[index] as DueCardDto | undefined;

  const handleReveal = () => setRevealed(true);

  const handleGrade = async (grade: CardGrade) => {
    if (!currentCard || submitting) return;
    setSubmitting(true);
    setError(null);
    try {
      await revisionApi.reviewCard(currentCard.cardId, grade);
      const nextReviewed = reviewedCount + 1;
      setReviewedCount(nextReviewed);
      if (index + 1 >= cards.length) {
        setSessionDone(true);
      } else {
        setIndex(index + 1);
        setRevealed(false);
      }
    } catch (err: any) {
      setError(err?.detail || err?.message || 'Failed to record review — the card was not rescheduled.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFinish = () => {
    onSessionComplete?.(reviewedCount);
    onClose();
  };

  const handleRestart = () => {
    loadDueCards();
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-label="Retrieval card review session"
      onClick={handleFinish}
    >
      <div
        className="relative w-full max-w-2xl max-h-[90vh] flex flex-col bg-[#121316] border border-[#2a2d34] rounded-2xl shadow-2xl overflow-hidden text-neutral-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#242730] bg-[#16181d]">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-purple-500/10 border border-purple-500/20 rounded-xl text-purple-400">
              <Brain className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-white tracking-tight">Retrieval Cards</h2>
              <p className="text-xs text-neutral-400">
                Active recall &middot; SM-2-lite spaced repetition
              </p>
            </div>
          </div>
          <button
            onClick={handleFinish}
            className="p-1.5 rounded-lg text-neutral-400 hover:text-white hover:bg-neutral-800 transition-colors"
            aria-label="Close card review session"
            data-testid="card-drawer-close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-5" data-testid="card-drawer-body">
          {loading && (
            <div className="flex flex-col items-center justify-center py-16 text-neutral-400 space-y-3" data-testid="card-drawer-loading">
              <Loader2 className="w-8 h-8 animate-spin text-purple-400" />
              <p className="text-sm font-medium">Loading due cards...</p>
            </div>
          )}

          {!loading && error && (
            <div
              className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm flex items-start gap-3"
              data-testid="card-drawer-error"
              role="alert"
            >
              <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-medium">Something went wrong</p>
                <p className="text-xs text-rose-400/80 mt-1">{error}</p>
                {cards.length === 0 && (
                  <button
                    onClick={handleRestart}
                    className="mt-2 text-xs underline text-rose-300 hover:text-rose-200"
                  >
                    Try again
                  </button>
                )}
              </div>
            </div>
          )}

          {!loading && !error && cards.length === 0 && (
            <div className="flex flex-col items-center justify-center py-12 text-center space-y-3" data-testid="card-drawer-empty">
              <div className="p-3 bg-neutral-800/80 rounded-2xl border border-neutral-700 text-neutral-400">
                <CheckCircle2 className="w-8 h-8 text-emerald-400" />
              </div>
              <h3 className="text-base font-semibold text-white">No Cards Due</h3>
              <p className="text-xs text-neutral-400 max-w-sm leading-relaxed">
                Nothing is scheduled for retrieval right now. Cards enroll automatically after a wrong
                attempt on a diagnosed question and resurface on an expanding schedule.
              </p>
            </div>
          )}

          {!loading && !error && sessionDone && (
            <div className="flex flex-col items-center justify-center py-12 text-center space-y-3" data-testid="card-drawer-complete">
              <div className="p-3 bg-emerald-500/10 rounded-2xl border border-emerald-500/20">
                <PartyPopper className="w-8 h-8 text-emerald-400" />
              </div>
              <h3 className="text-base font-semibold text-white">Session Complete</h3>
              <p className="text-xs text-neutral-400">
                You reviewed <span className="font-mono text-emerald-300 font-semibold">{reviewedCount}</span>{' '}
                {reviewedCount === 1 ? 'card' : 'cards'}. Each one was rescheduled by the SM-2-lite algorithm.
              </p>
              <button
                onClick={handleFinish}
                className="mt-2 inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-lg shadow-indigo-600/20 transition-all"
                data-testid="card-drawer-done-btn"
              >
                <CheckCircle2 className="w-4 h-4" />
                Done
              </button>
            </div>
          )}

          {!loading && !sessionDone && currentCard && (
            <>
              {/* Progress */}
              <div className="flex items-center justify-between text-xs text-neutral-400" data-testid="card-drawer-progress">
                <span className="font-mono">
                  Card {index + 1} / {cards.length}
                </span>
                <span className="flex items-center gap-1.5">
                  <RefreshCw className="w-3 h-3" />
                  {reviewedCount} reviewed
                </span>
              </div>
              <div className="h-1 w-full bg-neutral-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-purple-500 transition-all"
                  style={{ width: `${((index) / cards.length) * 100}%` }}
                />
              </div>

              {/* Concept context */}
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2 text-neutral-400">
                  <span className="font-mono text-purple-300">{currentCard.conceptSlug}</span>
                  <span aria-hidden="true">&middot;</span>
                  <span>{currentCard.conceptLabel}</span>
                </div>
                {currentCard.status === 'DRAFT' && (
                  <span className="px-2 py-0.5 text-[10px] font-medium rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20">
                    AI-GENERATED — UNREVIEWED
                  </span>
                )}
              </div>

              {/* Prompt */}
              <div className="p-5 rounded-xl bg-[#181a20] border border-[#242730]" data-testid="card-prompt">
                <h4 className="text-[10px] font-semibold text-purple-400 uppercase tracking-widest mb-2">
                  Prompt — attempt this aloud or on paper first
                </h4>
                <div className="text-sm font-medium text-white leading-relaxed">
                  <MathText content={currentCard.prompt} />
                </div>
              </div>

              {/* Answer */}
              {revealed ? (
                <div
                  className="p-5 rounded-xl bg-emerald-500/5 border border-emerald-500/20 animate-fade-in"
                  data-testid="card-answer"
                >
                  <h4 className="text-[10px] font-semibold text-emerald-400 uppercase tracking-widest mb-2">
                    Answer — {currentCard.unitTitle}
                  </h4>
                  <div className="text-xs text-neutral-200 leading-relaxed">
                    <MathText content={currentCard.answer} />
                  </div>
                </div>
              ) : (
                <button
                  onClick={handleReveal}
                  className="w-full py-3 inline-flex items-center justify-center gap-2 rounded-xl bg-neutral-800/70 hover:bg-neutral-800 border border-neutral-700/60 text-neutral-200 text-sm font-medium transition-colors"
                  data-testid="card-reveal-btn"
                >
                  <Eye className="w-4 h-4" />
                  Reveal Answer
                  <span className="text-[10px] text-neutral-500 ml-2 font-normal">ESC to exit</span>
                </button>
              )}

              {/* Self-grade */}
              {revealed && (
                <div className="space-y-2 animate-fade-in" data-testid="card-grade-group">
                  <h4 className="text-[10px] font-semibold text-neutral-400 uppercase tracking-widest">
                    How well did you recall it?
                  </h4>
                  <div className="grid grid-cols-3 gap-3">
                    {GRADE_OPTIONS.map((opt) => (
                      <button
                        key={opt.grade}
                        onClick={() => handleGrade(opt.grade)}
                        disabled={submitting}
                        data-testid={opt.testId}
                        aria-label={`${opt.label}: ${opt.hint}`}
                        className={`flex flex-col items-center gap-1 px-3 py-3 rounded-xl border text-xs font-semibold shadow-lg transition-all disabled:opacity-50 ${opt.className}`}
                      >
                        {submitting ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <>
                            <span>{opt.label}</span>
                            <span className="text-[10px] font-normal opacity-80 leading-tight">{opt.hint}</span>
                          </>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Footer */}
        {!loading && !sessionDone && cards.length > 0 && (
          <div className="px-6 py-3 border-t border-[#242730] bg-[#16181d] flex items-center justify-between text-[10px] text-neutral-500">
            <span className="flex items-center gap-1.5">
              <RotateCcw className="w-3 h-3" />
              FORGOTTEN resets to tomorrow &middot; SOLID expands 1&rarr;3&rarr;7&rarr;14&rarr;30 days
            </span>
            <span className="font-mono">SM-2-lite</span>
          </div>
        )}
      </div>
    </div>
  );
};
