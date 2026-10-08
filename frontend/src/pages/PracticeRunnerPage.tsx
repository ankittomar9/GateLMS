import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { 
  CheckCircle2, 
  XCircle, 
  HelpCircle, 
  SkipForward, 
  Eye, 
  ArrowLeft, 
  ArrowRight, 
  Clock, 
  Bookmark, 
  RotateCcw, 
  Check, 
  AlertTriangle,
  TrendingUp,
  Award,
  Sparkles,
  BookOpen
} from 'lucide-react';
import { 
  PracticeSessionResponse, 
  PracticeQuestionDto, 
  PracticeVerdictResponse, 
  PracticeFinishResponse,
  AttemptAnswerState,
  MistakeErrorCategory
} from '../api/types';
import { 
  useSubmitPracticeAnswer, 
  useSkipPracticeQuestion, 
  useRevealPracticeQuestion, 
  useFinishPracticeSession,
  useCreateMistake
} from '../hooks/useQueries';
import { MathText } from '../components/MathText';
import { useAiStudio } from '../context/AiStudioContext';
import { AnatomyPanel } from '../components/AnatomyPanel';

interface OptionItem {
  label: string;
  text: string;
}

export const PracticeRunnerPage: React.FC = () => {
  const { attemptId = '' } = useParams<{ attemptId: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isDrill = searchParams.get('drill') === 'true' || searchParams.has('source');
  const drillSource = searchParams.get('source');

  // Load session from storage or create mock fallback
  const [session, setSession] = useState<PracticeSessionResponse | null>(null);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [selectedOptions, setSelectedOptions] = useState<string>('');
  const [numericAnswer, setNumericAnswer] = useState<string>('');
  const [timeSpent, setTimeSpent] = useState<number>(0);

  // Per-question state maps
  const [verdicts, setVerdicts] = useState<Record<string, PracticeVerdictResponse>>({});
  const [questionStates, setQuestionStates] = useState<Record<string, AttemptAnswerState>>({});
  const [questionTimes, setQuestionTimes] = useState<Record<string, number>>({});
  const [markedForReview, setMarkedForReview] = useState<Record<string, boolean>>({});

  // Mistake form state
  const [selectedMistakeCategory, setSelectedMistakeCategory] = useState<MistakeErrorCategory>('CONCEPTUAL');
  const [mistakeTakeaway, setMistakeTakeaway] = useState<string>('');
  const [mistakeLoggedForQuestion, setMistakeLoggedForQuestion] = useState<Record<string, boolean>>({});

  // Reveal confirm modal
  const [showRevealConfirm, setShowRevealConfirm] = useState<boolean>(false);

  // Summary state
  const [finishSummary, setFinishSummary] = useState<PracticeFinishResponse | null>(null);

  // Mutations
  const submitMutation = useSubmitPracticeAnswer(attemptId);
  const skipMutation = useSkipPracticeQuestion(attemptId);
  const revealMutation = useRevealPracticeQuestion(attemptId);
  const finishMutation = useFinishPracticeSession(attemptId);
  const createMistakeMutation = useCreateMistake();

  // Load session on mount
  useEffect(() => {
    const cached = sessionStorage.getItem(`practice_session_${attemptId}`);
    if (cached) {
      try {
        const parsed: PracticeSessionResponse = JSON.parse(cached);
        setSession(parsed);
        // Initialize states
        const initialStates: Record<string, AttemptAnswerState> = {};
        parsed.questions.forEach((q) => {
          initialStates[q.id] = 'NOT_VISITED';
        });
        if (parsed.questions.length > 0) {
          initialStates[parsed.questions[0].id] = 'NOT_ANSWERED';
        }
        setQuestionStates(initialStates);
      } catch (err) {
        console.error('Failed to parse cached practice session', err);
      }
    }
  }, [attemptId]);

  // Advisory per-question timer
  useEffect(() => {
    if (finishSummary) return;
    const interval = setInterval(() => {
      setTimeSpent((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [finishSummary, currentIndex]);

  const { registerActiveQuestion } = useAiStudio();
  const currentQuestion: PracticeQuestionDto | undefined = session?.questions[currentIndex];

  useEffect(() => {
    if (currentQuestion) {
      registerActiveQuestion({
        id: currentQuestion.id,
        externalId: currentQuestion.externalId,
        statement: currentQuestion.statement,
        type: currentQuestion.type,
        year: currentQuestion.year,
        marks: currentQuestion.marks,
        subjectId: currentQuestion.subjectId,
        topicId: currentQuestion.topicId,
        optionsJson: currentQuestion.optionsJson,
        selectedAnswer: selectedOptions || numericAnswer || undefined,
        isLiveAttempt: !finishSummary,
      });
    }
    return () => registerActiveQuestion(null);
  }, [currentQuestion, selectedOptions, numericAnswer, finishSummary, registerActiveQuestion]);

  // Helper to parse options safely
  const parseOptions = (jsonStr: string | null): OptionItem[] => {
    if (!jsonStr) return [];
    try {
      const parsed = JSON.parse(jsonStr);
      if (Array.isArray(parsed)) {
        return parsed.map((item: any) => ({
          label: String(item.label || ''),
          text: String(item.text || item.statement || ''),
        }));
      }
      if (typeof parsed === 'object') {
        return Object.entries(parsed).map(([label, text]) => ({
          label,
          text: String(text),
        }));
      }
    } catch {
      // fallback
    }
    return [];
  };

  const optionsList = parseOptions(currentQuestion?.optionsJson ?? null);
  const currentVerdict = currentQuestion ? verdicts[currentQuestion.id] : undefined;

  // Handle option click (MCQ vs MSQ)
  const handleOptionClick = (label: string) => {
    if (currentVerdict) return; // Locked once graded
    if (!currentQuestion) return;

    if (currentQuestion.type === 'MCQ') {
      setSelectedOptions(label);
    } else if (currentQuestion.type === 'MSQ') {
      const existing = selectedOptions ? selectedOptions.split(',').map((s) => s.trim()) : [];
      const updated = existing.includes(label)
        ? existing.filter((item) => item !== label)
        : [...existing, label].sort();
      setSelectedOptions(updated.join(','));
    }
  };

  const isOptionSelected = (label: string) => {
    if (!selectedOptions) return false;
    if (currentQuestion?.type === 'MCQ') {
      return selectedOptions === label;
    }
    const set = selectedOptions.split(',').map((s) => s.trim());
    return set.includes(label);
  };

  // Submit Answer
  const handleSubmit = async () => {
    if (!currentQuestion || submitMutation.isPending) return;

    const numVal = numericAnswer.trim() ? parseFloat(numericAnswer) : undefined;
    const optsVal = selectedOptions.trim() || undefined;

    const verdict = await submitMutation.mutateAsync({
      questionId: currentQuestion.id,
      selectedOptions: optsVal,
      numericAnswer: numVal,
      timeSpentSeconds: timeSpent,
    });

    setVerdicts((prev) => ({ ...prev, [currentQuestion.id]: verdict }));
    setQuestionTimes((prev) => ({ ...prev, [currentQuestion.id]: timeSpent }));

    // Update state palette
    const isMarked = markedForReview[currentQuestion.id];
    setQuestionStates((prev) => ({
      ...prev,
      [currentQuestion.id]: isMarked ? 'ANSWERED_AND_MARKED' : 'ANSWERED',
    }));
  };

  // Skip Question
  const handleSkip = async () => {
    if (!currentQuestion || skipMutation.isPending) return;

    await skipMutation.mutateAsync(currentQuestion.id);

    setQuestionStates((prev) => ({
      ...prev,
      [currentQuestion.id]: 'NOT_ANSWERED',
    }));

    handleNext();
  };

  // Reveal Question
  const handleConfirmReveal = async () => {
    if (!currentQuestion || revealMutation.isPending) return;
    setShowRevealConfirm(false);

    const verdict = await revealMutation.mutateAsync(currentQuestion.id);
    setVerdicts((prev) => ({ ...prev, [currentQuestion.id]: verdict }));

    setQuestionStates((prev) => ({
      ...prev,
      [currentQuestion.id]: 'NOT_ANSWERED',
    }));
  };

  // Toggle Review Mark
  const handleToggleReview = () => {
    if (!currentQuestion) return;
    const currentMark = markedForReview[currentQuestion.id] || false;
    const nextMark = !currentMark;
    setMarkedForReview((prev) => ({ ...prev, [currentQuestion.id]: nextMark }));

    const currentState = questionStates[currentQuestion.id];
    if (nextMark) {
      if (currentState === 'ANSWERED') {
        setQuestionStates((prev) => ({ ...prev, [currentQuestion.id]: 'ANSWERED_AND_MARKED' }));
      } else {
        setQuestionStates((prev) => ({ ...prev, [currentQuestion.id]: 'MARKED_FOR_REVIEW' }));
      }
    } else {
      if (currentState === 'ANSWERED_AND_MARKED') {
        setQuestionStates((prev) => ({ ...prev, [currentQuestion.id]: 'ANSWERED' }));
      } else if (currentState === 'MARKED_FOR_REVIEW') {
        setQuestionStates((prev) => ({ ...prev, [currentQuestion.id]: 'NOT_ANSWERED' }));
      }
    }
  };

  // Navigation
  const handleJumpTo = (index: number) => {
    if (!session || index < 0 || index >= session.questions.length) return;
    setCurrentIndex(index);
    setSelectedOptions('');
    setNumericAnswer('');
    setTimeSpent(0);

    const targetId = session.questions[index].id;
    if (questionStates[targetId] === 'NOT_VISITED') {
      setQuestionStates((prev) => ({ ...prev, [targetId]: 'NOT_ANSWERED' }));
    }
  };

  const handleNext = () => {
    if (session && currentIndex < session.questions.length - 1) {
      handleJumpTo(currentIndex + 1);
    }
  };

  const handlePrev = () => {
    if (currentIndex > 0) {
      handleJumpTo(currentIndex - 1);
    }
  };

  // Finish session
  const handleFinish = async () => {
    const summary = await finishMutation.mutateAsync();
    setFinishSummary(summary);
  };

  // Log mistake
  const handleLogMistake = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentQuestion || !mistakeTakeaway.trim()) return;

    await createMistakeMutation.mutateAsync({
      questionId: currentQuestion.id,
      errorCategory: selectedMistakeCategory,
      correctiveTakeaway: mistakeTakeaway.trim(),
    });

    setMistakeLoggedForQuestion((prev) => ({ ...prev, [currentQuestion.id]: true }));
    setMistakeTakeaway('');
  };

  // Format time (MM:SS)
  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  // Palette color mapper (TCS iON 5 states)
  const getPaletteClass = (index: number) => {
    if (!session) return 'bg-ds-surface border-ds-border text-ds-muted';
    const qId = session.questions[index].id;
    const isCurrent = index === currentIndex;
    const state = questionStates[qId] || 'NOT_VISITED';

    let base = 'border ';
    if (isCurrent) {
      base += 'ring-2 ring-ds-accent ';
    }

    switch (state) {
      case 'ANSWERED':
        return base + 'bg-ds-success-soft text-ds-success border-ds-success font-semibold';
      case 'ANSWERED_AND_MARKED':
        return base + 'bg-ds-info-soft text-ds-info border-ds-info font-semibold';
      case 'MARKED_FOR_REVIEW':
        return base + 'bg-ds-warning-soft text-ds-warning border-ds-warning font-semibold';
      case 'NOT_ANSWERED':
        return base + 'bg-ds-danger-soft text-ds-danger border-ds-danger font-medium';
      case 'NOT_VISITED':
      default:
        return base + 'bg-ds-surface text-ds-muted border-ds-border';
    }
  };

  // If session finished, show summary view
  if (finishSummary) {
    return (
      <div className="max-w-4xl mx-auto space-y-8 pb-12">
        <div className="bg-ds-surface border border-ds-border rounded-ds p-6 md:p-8 space-y-6 shadow-sm">
          <div className="flex items-center justify-between border-b border-ds-border pb-4">
            <div>
              <h1 className="text-2xl font-bold text-ds-primary flex items-center gap-2">
                <Award className="w-6 h-6 text-ds-accent" />
                <span>Practice Session Summary</span>
              </h1>
              <p className="text-xs text-ds-muted mt-1">Review breakdown and topic mastery deltas.</p>
            </div>
            <div className="flex items-center gap-2 text-xs font-mono text-ds-secondary bg-ds-surface-raised px-3 py-1.5 rounded-ds border border-ds-border">
              <Clock className="w-4 h-4 text-ds-muted" />
              <span>Total Time: {formatTime(finishSummary.timeSpentSeconds)}</span>
            </div>
          </div>

          {/* Stat Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-ds-bg-base border border-ds-border rounded-ds p-4 text-center">
              <div className="text-2xl font-bold font-mono text-ds-primary">{finishSummary.totalQuestions}</div>
              <div className="text-xs text-ds-muted mt-1">Total Questions</div>
            </div>
            <div className="bg-ds-success-soft border border-ds-success/30 rounded-ds p-4 text-center">
              <div className="text-2xl font-bold font-mono text-ds-success">{finishSummary.correct}</div>
              <div className="text-xs text-ds-success font-medium mt-1">Correct (Solved)</div>
            </div>
            <div className="bg-ds-danger-soft border border-ds-danger/30 rounded-ds p-4 text-center">
              <div className="text-2xl font-bold font-mono text-ds-danger">{finishSummary.wrong}</div>
              <div className="text-xs text-ds-danger font-medium mt-1">Wrong Answers</div>
            </div>
            <div className="bg-ds-bg-base border border-ds-border rounded-ds p-4 text-center">
              <div className="text-2xl font-bold font-mono text-ds-muted">{finishSummary.skipped}</div>
              <div className="text-xs text-ds-muted mt-1">Skipped / Unattempted</div>
            </div>
          </div>

          {/* By Topic Table */}
          {finishSummary.byTopic && finishSummary.byTopic.length > 0 && (
            <div className="space-y-3 pt-4 border-t border-ds-border">
              <h3 className="text-sm font-semibold text-ds-primary">Performance By Topic</h3>
              <div className="border border-ds-border rounded-ds overflow-hidden">
                <table className="w-full text-xs text-left">
                  <thead className="bg-ds-surface-raised text-ds-secondary border-b border-ds-border font-medium">
                    <tr>
                      <th className="py-2.5 px-4">Topic</th>
                      <th className="py-2.5 px-4 text-center">Correct</th>
                      <th className="py-2.5 px-4 text-center">Total</th>
                      <th className="py-2.5 px-4 text-right">Accuracy</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-ds-border">
                    {finishSummary.byTopic.map((top) => {
                      const acc = top.total > 0 ? Math.round((top.correct / top.total) * 100) : 0;
                      return (
                        <tr key={top.topicId} className="hover:bg-ds-surface-hover">
                          <td className="py-2.5 px-4 font-medium text-ds-primary">{top.name}</td>
                          <td className="py-2.5 px-4 text-center font-mono text-ds-success">{top.correct}</td>
                          <td className="py-2.5 px-4 text-center font-mono text-ds-secondary">{top.total}</td>
                          <td className="py-2.5 px-4 text-right font-mono font-semibold text-ds-primary">{acc}%</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Action CTAs */}
          <div className="flex flex-wrap items-center justify-between gap-4 pt-4 border-t border-ds-border">
            <button
              type="button"
              onClick={() => navigate('/practice')}
              className="flex items-center gap-2 px-4 py-2 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-xs text-ds-secondary font-medium transition-colors"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Configure New Session</span>
            </button>

            <div className="flex items-center gap-3">
              {finishSummary.wrong > 0 && (
                <button
                  type="button"
                  onClick={() => navigate('/mistakes')}
                  className="flex items-center gap-2 px-4 py-2 rounded-ds bg-ds-danger-soft border border-ds-danger/30 text-ds-danger text-xs font-semibold hover:opacity-90 transition-opacity"
                >
                  <BookOpen className="w-4 h-4" />
                  <span>Review Mistake Notebook</span>
                </button>
              )}
              <button
                type="button"
                onClick={() => navigate('/')}
                className="flex items-center gap-2 px-5 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-95 shadow-sm transition-opacity"
              >
                <span>Return to Dashboard</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (!session || !currentQuestion) {
    return (
      <div className="max-w-4xl mx-auto py-12 text-center space-y-4">
        <div className="text-sm text-ds-muted">No active practice session found.</div>
        <button
          type="button"
          onClick={() => navigate('/practice')}
          className="px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium"
        >
          Go to Practice Setup
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Top Banner & Control Bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-ds-surface border border-ds-border rounded-ds p-4">
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => navigate(isDrill ? '/revision' : '/practice')}
            className="flex items-center gap-1.5 text-xs text-ds-muted hover:text-ds-primary transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Exit</span>
          </button>
          <div className="h-4 w-px bg-ds-border" />
          <div className="text-xs font-medium text-ds-primary flex items-center gap-2">
            {isDrill && (
              <span
                data-testid="drill-runner-badge"
                className="px-2 py-0.5 rounded text-[11px] font-semibold bg-ds-accent-soft text-ds-accent border border-ds-accent/30"
              >
                Weakness Drill · {drillSource || 'CUSTOM'}
              </span>
            )}
            <span>
              Question <span className="font-mono font-bold text-ds-accent">{currentIndex + 1}</span> of{' '}
              <span className="font-mono text-ds-muted">{session.totalQuestions}</span>
            </span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Per-question timer */}
          <div className="flex items-center gap-1.5 text-xs font-mono text-ds-secondary bg-ds-surface-raised px-3 py-1 rounded-ds border border-ds-border">
            <Clock className="w-3.5 h-3.5 text-ds-muted" />
            <span>{formatTime(timeSpent)}</span>
          </div>

          {/* Mark for review button */}
          <button
            type="button"
            onClick={handleToggleReview}
            className={`flex items-center gap-1.5 px-3 py-1 rounded-ds text-xs font-medium border transition-colors ${
              markedForReview[currentQuestion.id]
                ? 'bg-ds-warning-soft border-ds-warning/40 text-ds-warning'
                : 'bg-ds-surface-raised border-ds-border text-ds-muted hover:text-ds-secondary'
            }`}
          >
            <Bookmark className={`w-3.5 h-3.5 ${markedForReview[currentQuestion.id] ? 'fill-current' : ''}`} />
            <span>Mark for Review</span>
          </button>

          {/* Finish Button */}
          <button
            type="button"
            onClick={handleFinish}
            disabled={finishMutation.isPending}
            className="px-3.5 py-1 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border-strong text-xs font-semibold text-ds-primary transition-colors"
          >
            {finishMutation.isPending ? 'Submitting...' : 'Finish Session'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Main Question & Solution Column */}
        <div className="lg:col-span-3 space-y-6">
          <div className="bg-ds-surface border border-ds-border rounded-ds p-6 md:p-8 space-y-6 shadow-sm">
            {/* Question Header Meta */}
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ds-border pb-4">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-0.5 rounded-ds text-xs font-mono font-semibold bg-ds-accent-soft text-ds-accent border border-ds-accent/30">
                  {currentQuestion.type}
                </span>
                <span className="px-2 py-0.5 rounded-ds text-xs font-mono text-ds-secondary bg-ds-surface-raised border border-ds-border">
                  {currentQuestion.marks} {currentQuestion.marks === 1 ? 'Mark' : 'Marks'}
                </span>
                {currentQuestion.year && (
                  <span className="px-2 py-0.5 rounded-ds text-xs font-mono text-ds-muted bg-ds-surface-raised border border-ds-border">
                    GATE {currentQuestion.year}
                  </span>
                )}
              </div>
            </div>

            {/* Question Statement */}
            <div className="text-sm md:text-base text-ds-primary leading-relaxed">
              <MathText content={currentQuestion.statement} />
            </div>

            {/* Answer Input Area */}
            {currentQuestion.type === 'NAT' ? (
              <div className="space-y-3 pt-2">
                <label htmlFor="nat-answer" className="block text-xs font-medium text-ds-secondary">
                  Enter Numerical Answer (Decimals allowed):
                </label>
                <div className="max-w-xs">
                  <input
                    id="nat-answer"
                    type="number"
                    step="any"
                    value={numericAnswer}
                    disabled={Boolean(currentVerdict)}
                    onChange={(e) => setNumericAnswer(e.target.value)}
                    placeholder="e.g. 3.14"
                    className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary font-mono focus:outline-none focus:border-ds-accent transition-colors disabled:opacity-60"
                  />
                </div>
              </div>
            ) : (
              <div className="space-y-3 pt-2" role="radiogroup" aria-label="Question options">
                {optionsList.map((opt) => {
                  const selected = isOptionSelected(opt.label);
                  return (
                    <div
                      key={opt.label}
                      onClick={() => handleOptionClick(opt.label)}
                      className={`flex items-start gap-3 p-3.5 rounded-ds border cursor-pointer transition-all ${
                        selected
                          ? 'bg-ds-accent-soft border-ds-accent/50 text-ds-primary shadow-sm'
                          : 'bg-ds-surface-raised border-ds-border hover:bg-ds-surface text-ds-secondary hover:text-ds-primary'
                      } ${currentVerdict ? 'cursor-default pointer-events-none' : ''}`}
                    >
                      <div
                        className={`w-6 h-6 rounded-ds flex items-center justify-center font-mono text-xs font-bold shrink-0 border transition-colors ${
                          selected
                            ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent'
                            : 'bg-ds-surface text-ds-muted border-ds-border'
                        }`}
                      >
                        {opt.label}
                      </div>
                      <div className="text-sm pt-0.5">
                        <MathText content={opt.text} />
                      </div>
                    </div>
                  );
                })}
              </div>
            )}

            {/* Actions Bar (Submit / Skip / Reveal) */}
            {!currentVerdict && (
              <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-ds-border">
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setShowRevealConfirm(true)}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface hover:bg-ds-surface-hover text-xs text-ds-secondary hover:text-ds-primary transition-colors"
                  >
                    <Eye className="w-3.5 h-3.5 text-ds-muted" />
                    <span>Reveal Answer</span>
                  </button>
                  <button
                    type="button"
                    onClick={handleSkip}
                    disabled={skipMutation.isPending}
                    className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface hover:bg-ds-surface-hover text-xs text-ds-secondary hover:text-ds-primary transition-colors"
                  >
                    <SkipForward className="w-3.5 h-3.5 text-ds-muted" />
                    <span>Skip</span>
                  </button>
                </div>

                <button
                  type="button"
                  onClick={handleSubmit}
                  disabled={submitMutation.isPending || (!selectedOptions && !numericAnswer.trim())}
                  className="flex items-center gap-2 px-6 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-95 shadow-sm disabled:opacity-50 transition-all"
                >
                  <Check className="w-4 h-4" />
                  <span>{submitMutation.isPending ? 'Grading...' : 'Submit Answer'}</span>
                </button>
              </div>
            )}

            {/* Inline Graded Verdict Panel */}
            {currentVerdict && (
              <div
                aria-live="polite"
                className={`p-6 rounded-ds border space-y-4 ${
                  currentVerdict.correct
                    ? 'bg-ds-success-soft border-ds-success/40 text-ds-primary'
                    : 'bg-ds-danger-soft border-ds-danger/40 text-ds-primary'
                }`}
              >
                <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ds-border pb-3">
                  <div className="flex items-center gap-2.5">
                    {currentVerdict.correct ? (
                      <CheckCircle2 className="w-5 h-5 text-ds-success shrink-0" />
                    ) : (
                      <XCircle className="w-5 h-5 text-ds-danger shrink-0" />
                    )}
                    <span className="font-bold text-sm">
                      {currentVerdict.correct ? 'Correct Answer!' : 'Incorrect'}
                    </span>
                    <span className="font-mono text-xs text-ds-secondary">
                      +{currentVerdict.marksAwarded} Marks
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="flex items-center gap-1 px-2.5 py-0.5 rounded-ds bg-ds-surface border border-ds-border text-[11px] font-mono text-ds-secondary">
                      <TrendingUp className="w-3 h-3 text-ds-accent" />
                      <span>Topic Mastery: Lvl {currentVerdict.topicMastery}/5</span>
                    </span>
                  </div>
                </div>

                {/* Explanation and Answers */}
                <div className="space-y-2 text-xs">
                  {currentVerdict.correctOptions && (
                    <div>
                      <span className="font-semibold text-ds-secondary">Correct Option(s): </span>
                      <span className="font-mono font-bold text-ds-primary">{currentVerdict.correctOptions}</span>
                    </div>
                  )}
                  {currentVerdict.numericMin !== null && currentVerdict.numericMin !== undefined && (
                    <div>
                      <span className="font-semibold text-ds-secondary">Accepted Range: </span>
                      <span className="font-mono font-bold text-ds-primary">
                        [{currentVerdict.numericMin} to {currentVerdict.numericMax}]
                      </span>
                    </div>
                  )}
                  {currentVerdict.explanation && (
                    <div className="mt-2 text-ds-secondary leading-relaxed bg-ds-surface/60 p-3 rounded-ds border border-ds-border">
                      <div className="font-semibold text-ds-primary mb-1">Explanation:</div>
                      <MathText content={currentVerdict.explanation} />
                    </div>
                  )}
                </div>

                {/* Log Mistake Form if Incorrect */}
                {!currentVerdict.correct && currentVerdict.mistakeLogPrompt && (
                  <div className="mt-4 pt-4 border-t border-ds-border space-y-3">
                    <div className="text-xs font-semibold text-ds-primary flex items-center gap-2">
                      <AlertTriangle className="w-4 h-4 text-ds-warning" />
                      <span>Log to Mistake Notebook</span>
                    </div>

                    {mistakeLoggedForQuestion[currentQuestion.id] ? (
                      <div className="text-xs text-ds-success bg-ds-surface p-2 rounded-ds border border-ds-border flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Mistake saved to spaced review queue!</span>
                      </div>
                    ) : (
                      <form onSubmit={handleLogMistake} className="space-y-3">
                        <div className="flex flex-wrap gap-2">
                          {(['CONCEPTUAL', 'CALCULATION', 'MSQ_TRAP', 'MISREAD'] as MistakeErrorCategory[]).map(
                            (cat) => (
                              <button
                                key={cat}
                                type="button"
                                onClick={() => setSelectedMistakeCategory(cat)}
                                className={`px-2.5 py-1 rounded-ds text-xs font-mono transition-colors border ${
                                  selectedMistakeCategory === cat
                                    ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent font-semibold'
                                    : 'bg-ds-surface border-ds-border text-ds-muted hover:text-ds-primary'
                                }`}
                              >
                                {cat}
                              </button>
                            )
                          )}
                        </div>

                        <textarea
                          rows={2}
                          value={mistakeTakeaway}
                          onChange={(e) => setMistakeTakeaway(e.target.value)}
                          placeholder="What is your corrective takeaway? (e.g. Always check boundary conditions)"
                          className="w-full bg-ds-surface border border-ds-border rounded-ds p-2.5 text-xs text-ds-primary focus:outline-none focus:border-ds-accent transition-colors"
                        />

                        <div className="flex justify-end">
                          <button
                            type="submit"
                            disabled={createMistakeMutation.isPending || !mistakeTakeaway.trim()}
                            className="px-3.5 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium hover:opacity-90 disabled:opacity-50 transition-opacity"
                          >
                            {createMistakeMutation.isPending ? 'Logging...' : 'Save Mistake'}
                          </button>
                        </div>
                      </form>
                    )}
                  </div>
                )}

                {/* Next button */}
                <div className="flex justify-end pt-2">
                  <button
                    type="button"
                    onClick={handleNext}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-ds bg-ds-surface hover:bg-ds-surface-hover border border-ds-border text-xs font-semibold text-ds-primary transition-colors"
                  >
                    <span>Next Question</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            )}

            {/* Examiner Question Anatomy Dossier (Review Step) */}
            {currentVerdict && (
              <AnatomyPanel
                questionId={currentQuestion.id}
                hasAttempted={true}
                mode="study"
              />
            )}
          </div>
        </div>

        {/* Right Palette Column */}
        <div className="space-y-4">
          <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-4 shadow-sm">
            <h3 className="text-xs font-semibold text-ds-secondary uppercase tracking-wider">
              Question Palette
            </h3>

            {/* TCS iON 5-state Palette Grid */}
            <div className="grid grid-cols-5 gap-2">
              {session.questions.map((q, idx) => (
                <button
                  key={q.id}
                  type="button"
                  onClick={() => handleJumpTo(idx)}
                  className={`h-9 rounded-ds text-xs font-mono flex items-center justify-center transition-all ${getPaletteClass(
                    idx
                  )}`}
                >
                  {idx + 1}
                </button>
              ))}
            </div>

            {/* Legend */}
            <div className="pt-3 border-t border-ds-border space-y-2 text-[11px] text-ds-secondary">
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-ds-success-soft border border-ds-success" />
                <span>Answered</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-ds-danger-soft border border-ds-danger" />
                <span>Not Answered</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-ds-warning-soft border border-ds-warning" />
                <span>Marked for Review</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-ds-info-soft border border-ds-info" />
                <span>Answered & Marked</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3 h-3 rounded bg-ds-surface border border-ds-border" />
                <span>Not Visited</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Reveal Confirmation Modal */}
      {showRevealConfirm && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-ds-surface border border-ds-border rounded-ds p-6 max-w-sm w-full space-y-4 shadow-xl">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-ds bg-ds-warning-soft border border-ds-warning/30 flex items-center justify-center text-ds-warning shrink-0">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-ds-primary">Reveal Answer?</h4>
                <p className="text-xs text-ds-muted mt-0.5">
                  Revealing will mark this question as ASSISTED without awarding marks.
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowRevealConfirm(false)}
                className="px-3 py-1.5 rounded-ds border border-ds-border text-xs text-ds-secondary hover:text-ds-primary"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmReveal}
                className="px-4 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium hover:opacity-90"
              >
                Confirm Reveal
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PracticeRunnerPage;
