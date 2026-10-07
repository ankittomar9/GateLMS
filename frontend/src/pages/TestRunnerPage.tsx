import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Clock, 
  AlertTriangle, 
  CheckCircle, 
  HelpCircle, 
  ChevronLeft, 
  ChevronRight, 
  Bookmark, 
  RotateCcw,
  Send,
  WifiOff
} from 'lucide-react';
import { MathText } from '../components/MathText';
import { 
  useUpsertAttemptAnswer, 
  useSubmitMockAttempt 
} from '../hooks/useQueries';
import { PracticeQuestionDto, StartMockAttemptResponse } from '../api/types';
import { useAiStudio } from '../context/AiStudioContext';

export const TestRunnerPage: React.FC = () => {
  const { attemptId } = useParams<{ attemptId: string }>();
  const navigate = useNavigate();

  const [attemptData, setAttemptData] = useState<StartMockAttemptResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [currentIndex, setCurrentIndex] = useState(0);

  // Per-question state maps
  const [selectedAnswers, setSelectedAnswers] = useState<Record<string, string>>({});
  const [numericAnswers, setNumericAnswers] = useState<Record<string, number | undefined>>({});
  const [questionStates, setQuestionStates] = useState<Record<string, string>>({});

  // Timer state
  const [remainingSeconds, setRemainingSeconds] = useState<number>(10800);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [showConfirmSubmit, setShowConfirmSubmit] = useState(false);

  // Tab switch counter
  const [tabSwitchCount, setTabSwitchCount] = useState(0);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);

  const upsertMutation = useUpsertAttemptAnswer(attemptId || '');
  const submitMutation = useSubmitMockAttempt(attemptId || '');

  // Load attempt state or fetch
  useEffect(() => {
    if (!attemptId) return;

    // Fetch initial attempt data or reconstruct
    const fetchAttempt = async () => {
      try {
        setIsLoading(true);
        // In local development / testing, check if attempt details are loaded
        const resp = await fetch(`/api/v1/mocks/attempts/${attemptId}`);
        if (resp.ok) {
          const data: StartMockAttemptResponse = await resp.json();
          setAttemptData(data);
          initializeFromAttempt(data);
        } else {
          // Fallback fetch from attempts API or practice runner
          const fallbackResp = await fetch(`/api/v1/attempts/${attemptId}/result`);
          if (fallbackResp.ok) {
            // Already submitted
            navigate(`/mocks/attempts/${attemptId}/result`);
            return;
          }
        }
      } catch (err) {
        console.error('Error fetching attempt', err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAttempt();
  }, [attemptId, navigate]);

  const initializeFromAttempt = (data: StartMockAttemptResponse) => {
    // Server time offset calculation
    const clientNow = Date.now();
    const serverTimeMs = new Date(data.serverTime).getTime();
    const startedAtMs = new Date(data.startedAt).getTime();
    const serverOffset = serverTimeMs - clientNow;

    const effectiveServerNow = clientNow + serverOffset;
    const elapsedSeconds = Math.max(0, Math.floor((effectiveServerNow - startedAtMs) / 1000));
    const left = Math.max(0, data.durationSeconds - elapsedSeconds);
    setRemainingSeconds(left);

    // Initial question states
    const states: Record<string, string> = {};
    data.questions.forEach((q, idx) => {
      states[q.id] = idx === 0 ? 'NOT_ANSWERED' : 'NOT_VISITED';
    });
    setQuestionStates(states);
  };

  // Timer countdown hook
  useEffect(() => {
    if (remainingSeconds <= 0 || isSubmitted) return;

    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          handleAutoSubmit();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [remainingSeconds, isSubmitted]);

  // Tab switch detection (client-side advisory)
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden) {
        setTabSwitchCount((c) => c + 1);
      }
    };
    const handleOnline = () => setIsOffline(false);
    const handleOffline = () => setIsOffline(true);

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  const handleAutoSubmit = async () => {
    if (isSubmitted || !attemptId) return;
    setIsSubmitted(true);
    try {
      await submitMutation.mutateAsync({});
      navigate(`/mocks/attempts/${attemptId}/result`);
    } catch (err) {
      console.error('Auto-submit error', err);
      navigate(`/mocks/attempts/${attemptId}/result`);
    }
  };

  const handleManualSubmit = async () => {
    if (!attemptId) return;
    setIsSubmitted(true);
    setShowConfirmSubmit(false);
    try {
      await submitMutation.mutateAsync({});
      navigate(`/mocks/attempts/${attemptId}/result`);
    } catch (err) {
      console.error('Submit error', err);
      navigate(`/mocks/attempts/${attemptId}/result`);
    }
  };

  const { registerActiveQuestion } = useAiStudio();
  const questions = attemptData?.questions || [];
  const currentQuestion: PracticeQuestionDto | undefined = questions[currentIndex];

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
        isLiveAttempt: true,
      });
    }
    return () => registerActiveQuestion(null);
  }, [currentQuestion, registerActiveQuestion]);

  // Parse options safely
  const options = useMemo(() => {
    if (!currentQuestion?.optionsJson) return [];
    try {
      const parsed = JSON.parse(currentQuestion.optionsJson);
      if (Array.isArray(parsed)) return parsed;
      return [];
    } catch {
      return [];
    }
  }, [currentQuestion?.optionsJson]);

  // Current answer state
  const currentSelected = currentQuestion ? selectedAnswers[currentQuestion.id] : undefined;
  const currentNumeric = currentQuestion ? numericAnswers[currentQuestion.id] : undefined;

  // Question navigation and actions
  const persistAnswer = (qId: string, newState: string, selected?: string, numeric?: number) => {
    setQuestionStates((prev) => ({ ...prev, [qId]: newState }));
    upsertMutation.mutate({
      questionId: qId,
      data: {
        selectedOptions: selected,
        numericAnswer: numeric,
        state: newState,
        timeSpentSeconds: 5,
      },
    });
  };

  const handleOptionSelect = (optLabel: string) => {
    if (isSubmitted || !currentQuestion) return;

    if (currentQuestion.type === 'MSQ') {
      const current = currentSelected ? currentSelected.split(',').map((s) => s.trim()) : [];
      let next: string[];
      if (current.includes(optLabel)) {
        next = current.filter((s) => s !== optLabel);
      } else {
        next = [...current, optLabel].sort();
      }
      const val = next.join(',');
      setSelectedAnswers((prev) => ({ ...prev, [currentQuestion.id]: val }));
    } else {
      setSelectedAnswers((prev) => ({ ...prev, [currentQuestion.id]: optLabel }));
    }
  };

  const handleSaveAndNext = () => {
    if (!currentQuestion) return;
    const hasAnswer = Boolean(currentSelected || currentNumeric !== undefined);
    const newState = hasAnswer ? 'ANSWERED' : 'NOT_ANSWERED';
    persistAnswer(currentQuestion.id, newState, currentSelected, currentNumeric);

    if (currentIndex < questions.length - 1) {
      moveToQuestion(currentIndex + 1);
    }
  };

  const handleMarkForReviewAndNext = () => {
    if (!currentQuestion) return;
    const hasAnswer = Boolean(currentSelected || currentNumeric !== undefined);
    const newState = hasAnswer ? 'ANSWERED_AND_MARKED' : 'MARKED_FOR_REVIEW';
    persistAnswer(currentQuestion.id, newState, currentSelected, currentNumeric);

    if (currentIndex < questions.length - 1) {
      moveToQuestion(currentIndex + 1);
    }
  };

  const handleClearResponse = () => {
    if (isSubmitted || !currentQuestion) return;
    setSelectedAnswers((prev) => {
      const copy = { ...prev };
      delete copy[currentQuestion.id];
      return copy;
    });
    setNumericAnswers((prev) => {
      const copy = { ...prev };
      delete copy[currentQuestion.id];
      return copy;
    });
    persistAnswer(currentQuestion.id, 'NOT_ANSWERED', undefined, undefined);
  };

  const moveToQuestion = (index: number) => {
    if (index < 0 || index >= questions.length) return;
    const targetQ = questions[index];
    if (questionStates[targetQ.id] === 'NOT_VISITED') {
      setQuestionStates((prev) => ({ ...prev, [targetQ.id]: 'NOT_ANSWERED' }));
    }
    setCurrentIndex(index);
  };

  // TCS iON State Color Class
  const getStateColor = (state: string) => {
    switch (state) {
      case 'ANSWERED':
        return 'bg-ds-success text-ds-accent-contrast border-ds-success';
      case 'NOT_ANSWERED':
        return 'bg-ds-danger text-ds-accent-contrast border-ds-danger';
      case 'MARKED_FOR_REVIEW':
        return 'bg-purple-600 text-ds-accent-contrast border-purple-600';
      case 'ANSWERED_AND_MARKED':
      case 'ANSWERED_AND_MARKED_FOR_REVIEW':
        return 'bg-purple-700 text-ds-accent-contrast border-purple-700 relative';
      case 'NOT_VISITED':
      default:
        return 'bg-ds-surface text-ds-secondary border-ds-border';
    }
  };

  // Format Timer
  const formatTime = (secs: number) => {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  if (isLoading) {
    return (
      <div className="min-h-[80vh] flex items-center justify-center text-ds-muted">
        Loading test runner session...
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] max-w-full overflow-hidden select-none">
      {/* Offline Banner */}
      {isOffline && (
        <div className="bg-ds-danger text-ds-accent-contrast px-4 py-2 text-xs flex items-center justify-center gap-2">
          <WifiOff className="w-4 h-4" />
          Connection interrupted! Answers are queued locally and will sync when reconnected.
        </div>
      )}

      {/* Tab Switch Advisory Banner */}
      {tabSwitchCount > 0 && (
        <div className="bg-ds-warning/10 border-b border-ds-warning/20 text-ds-warning px-4 py-1.5 text-xs flex items-center justify-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <span>Notice: {tabSwitchCount} window/tab switch events recorded during this session.</span>
        </div>
      )}

      {/* Runner Top Bar */}
      <header className="h-14 bg-ds-surface border-b border-ds-border px-5 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-3">
          <h1 className="font-bold text-sm text-ds-primary tracking-tight">
            {attemptData?.mockTitle || 'GATE Examination Simulation'}
          </h1>
          <span className="text-xs px-2 py-0.5 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-secondary font-mono">
            {questions.length} Questions
          </span>
        </div>

        <div className="flex items-center gap-4">
          <div 
            role="timer"
            aria-label="Remaining Exam Time"
            className={`font-mono text-base font-bold flex items-center gap-2 px-3 py-1 rounded-ds border ${
              remainingSeconds < 300 
                ? 'bg-ds-danger/10 border-ds-danger text-ds-danger animate-pulse' 
                : 'bg-ds-bg-sidebar border-ds-border text-ds-primary'
            }`}
          >
            <Clock className="w-4 h-4" />
            {formatTime(remainingSeconds)}
          </div>

          <button
            type="button"
            onClick={() => setShowConfirmSubmit(true)}
            className="px-4 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast font-semibold text-xs shadow-sm hover:opacity-90 flex items-center gap-1.5"
          >
            <Send className="w-3.5 h-3.5" />
            Submit Exam
          </button>
        </div>
      </header>

      {/* Main Workspace Layout */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left / Center: Question Workspace */}
        <main className="flex-1 flex flex-col overflow-y-auto p-6 space-y-6">
          {currentQuestion ? (
            <div className="bg-ds-surface border border-ds-border rounded-ds p-6 space-y-5 max-w-4xl mx-auto w-full shadow-sm">
              {/* Question Header */}
              <div className="flex items-center justify-between border-b border-ds-border pb-3">
                <div className="flex items-center gap-2 text-xs">
                  <span className="font-bold text-ds-primary text-sm">
                    Question {currentIndex + 1}
                  </span>
                  <span className="px-2 py-0.5 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-secondary font-semibold">
                    {currentQuestion.type}
                  </span>
                  <span className="text-ds-muted">•</span>
                  <span className="text-ds-secondary font-medium">
                    {currentQuestion.marks} {currentQuestion.marks === 1 ? 'Mark' : 'Marks'}
                  </span>
                </div>

                <div className="text-xs text-ds-muted font-mono">
                  {currentQuestion.externalId || currentQuestion.id.slice(0, 8)}
                </div>
              </div>

              {/* Statement */}
              <div className="text-ds-primary leading-relaxed text-sm pt-2">
                <MathText content={currentQuestion.statement} />
              </div>

              {/* Answer Controls: MCQ / MSQ vs NAT */}
              {currentQuestion.type === 'NAT' ? (
                <div className="pt-4 space-y-3">
                  <label className="text-xs font-semibold text-ds-secondary block">
                    Enter Numerical Value (Decimals allowed):
                  </label>
                  <input
                    type="number"
                    step="any"
                    value={currentNumeric ?? ''}
                    disabled={isSubmitted}
                    onChange={(e) => {
                      const val = e.target.value === '' ? undefined : parseFloat(e.target.value);
                      setNumericAnswers((prev) => ({ ...prev, [currentQuestion.id]: val }));
                    }}
                    placeholder="Enter your answer"
                    className="w-64 px-3 py-2 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-primary font-mono text-sm focus:outline-none focus:border-ds-accent"
                  />
                  <span className="text-[11px] text-ds-muted block">
                    Use physical or on-screen keypad to enter answer. No negative marking for NAT.
                  </span>
                </div>
              ) : (
                <div 
                  role="radiogroup" 
                  aria-label="Options" 
                  className="space-y-2.5 pt-3"
                >
                  {options.map((opt: any) => {
                    const label = opt.label || '';
                    const isChecked = currentQuestion.type === 'MSQ'
                      ? (currentSelected?.split(',').map((s) => s.trim()).includes(label) ?? false)
                      : currentSelected === label;

                    return (
                      <div
                        key={label}
                        onClick={() => handleOptionSelect(label)}
                        className={`p-3 rounded-ds border transition-all cursor-pointer flex items-start gap-3 ${
                          isChecked
                            ? 'border-ds-accent bg-ds-accent/10 text-ds-primary'
                            : 'border-ds-border bg-ds-bg-sidebar hover:bg-ds-surface-hover text-ds-secondary'
                        }`}
                      >
                        <div className={`w-5 h-5 rounded-ds border flex items-center justify-center shrink-0 font-bold text-xs mt-0.5 ${
                          isChecked ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent' : 'border-ds-border text-ds-muted'
                        }`}>
                          {label}
                        </div>
                        <div className="flex-1 text-xs text-ds-primary leading-relaxed">
                          <MathText content={opt.text} />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          ) : (
            <div className="p-12 text-center text-ds-muted">No question selected</div>
          )}

          {/* Action Footer Bar */}
          <div className="max-w-4xl mx-auto w-full flex flex-wrap items-center justify-between gap-3 pt-2">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleSaveAndNext}
                disabled={isSubmitted}
                className="px-4 py-2 rounded-ds bg-ds-success text-ds-accent-contrast font-semibold text-xs shadow-sm hover:opacity-90 flex items-center gap-1.5 disabled:opacity-50"
              >
                <CheckCircle className="w-3.5 h-3.5" />
                Save & Next
              </button>

              <button
                type="button"
                onClick={handleMarkForReviewAndNext}
                disabled={isSubmitted}
                className="px-4 py-2 rounded-ds bg-purple-600 text-ds-accent-contrast font-semibold text-xs shadow-sm hover:opacity-90 flex items-center gap-1.5 disabled:opacity-50"
              >
                <Bookmark className="w-3.5 h-3.5" />
                Mark for Review & Next
              </button>

              <button
                type="button"
                onClick={handleClearResponse}
                disabled={isSubmitted}
                className="px-3.5 py-2 rounded-ds border border-ds-border text-ds-secondary hover:text-ds-primary hover:bg-ds-surface text-xs font-medium flex items-center gap-1.5 disabled:opacity-50"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                Clear Response
              </button>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => moveToQuestion(currentIndex - 1)}
                disabled={currentIndex === 0}
                className="px-3 py-2 rounded-ds border border-ds-border text-ds-secondary hover:text-ds-primary text-xs font-semibold flex items-center gap-1 disabled:opacity-30"
              >
                <ChevronLeft className="w-4 h-4" /> Previous
              </button>

              <button
                type="button"
                onClick={() => moveToQuestion(currentIndex + 1)}
                disabled={currentIndex === questions.length - 1}
                className="px-3 py-2 rounded-ds border border-ds-border text-ds-secondary hover:text-ds-primary text-xs font-semibold flex items-center gap-1 disabled:opacity-30"
              >
                Next <ChevronRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        </main>

        {/* Right Palette Navigator */}
        <aside className="w-80 bg-ds-bg-sidebar border-l border-ds-border p-4 flex flex-col justify-between shrink-0 overflow-y-auto">
          <div className="space-y-4">
            <h2 className="text-xs font-bold text-ds-primary uppercase tracking-wider">
              Question Palette
            </h2>

            {/* TCS iON 5-State Legend */}
            <div className="grid grid-cols-2 gap-2 text-[10px] text-ds-secondary border-b border-ds-border pb-3">
              <div className="flex items-center gap-2">
                <span className="w-5 h-5 rounded-ds flex items-center justify-center font-bold bg-ds-success text-ds-accent-contrast">
                  {Object.values(questionStates).filter((s) => s === 'ANSWERED').length}
                </span>
                <span>Answered</span>
              </div>

              <div className="flex items-center gap-2">
                <span className="w-5 h-5 rounded-ds flex items-center justify-center font-bold bg-ds-danger text-ds-accent-contrast">
                  {Object.values(questionStates).filter((s) => s === 'NOT_ANSWERED').length}
                </span>
                <span>Not Answered</span>
              </div>

              <div className="flex items-center gap-2">
                <span className="w-5 h-5 rounded-ds flex items-center justify-center font-bold bg-purple-600 text-ds-accent-contrast">
                  {Object.values(questionStates).filter((s) => s === 'MARKED_FOR_REVIEW').length}
                </span>
                <span>Marked</span>
              </div>

              <div className="flex items-center gap-2">
                <span className="w-5 h-5 rounded-ds flex items-center justify-center font-bold bg-purple-700 text-ds-accent-contrast">
                  {Object.values(questionStates).filter((s) => s === 'ANSWERED_AND_MARKED').length}
                </span>
                <span>Ans & Marked</span>
              </div>

              <div className="flex items-center gap-2 col-span-2">
                <span className="w-5 h-5 rounded-ds flex items-center justify-center font-bold bg-ds-surface border border-ds-border text-ds-secondary">
                  {Object.values(questionStates).filter((s) => s === 'NOT_VISITED').length}
                </span>
                <span>Not Visited</span>
              </div>
            </div>

            {/* Question Grid Buttons */}
            <div className="grid grid-cols-5 gap-2 pt-1 max-h-96 overflow-y-auto pr-1">
              {questions.map((q, idx) => {
                const state = questionStates[q.id] || 'NOT_VISITED';
                const isSelected = idx === currentIndex;

                return (
                  <button
                    key={q.id}
                    type="button"
                    onClick={() => moveToQuestion(idx)}
                    className={`h-9 rounded-ds font-bold text-xs flex items-center justify-center transition-all border ${getStateColor(state)} ${
                      isSelected ? 'ring-2 ring-ds-accent ring-offset-1 font-extrabold' : ''
                    }`}
                  >
                    {idx + 1}
                    {state === 'ANSWERED_AND_MARKED' && (
                      <span className="absolute bottom-0.5 right-0.5 w-1.5 h-1.5 rounded-full bg-ds-success" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          <div className="pt-4 border-t border-ds-border">
            <button
              type="button"
              onClick={() => setShowConfirmSubmit(true)}
              className="w-full py-2.5 rounded-ds bg-ds-accent text-ds-accent-contrast font-bold text-xs shadow-sm hover:opacity-90"
            >
              Submit Entire Exam
            </button>
          </div>
        </aside>
      </div>

      {/* Confirm Submit Dialog Modal */}
      {showConfirmSubmit && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-ds-surface border border-ds-border rounded-ds max-w-md w-full p-6 shadow-xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <h3 className="font-bold text-base text-ds-primary flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-ds-warning" />
              Confirm Exam Submission
            </h3>
            <p className="text-xs text-ds-secondary leading-relaxed">
              Are you sure you want to finalize your mock examination? You will not be able to revisit or modify your answers once submitted.
            </p>

            <div className="bg-ds-bg-sidebar border border-ds-border rounded-ds p-3 text-xs space-y-1.5">
              <div className="flex justify-between">
                <span className="text-ds-secondary">Answered:</span>
                <span className="font-bold text-ds-success">
                  {Object.values(questionStates).filter((s) => s === 'ANSWERED' || s === 'ANSWERED_AND_MARKED').length}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-ds-secondary">Unanswered:</span>
                <span className="font-bold text-ds-danger">
                  {Object.values(questionStates).filter((s) => s === 'NOT_ANSWERED' || s === 'NOT_VISITED').length}
                </span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setShowConfirmSubmit(false)}
                className="px-4 py-2 rounded-ds border border-ds-border text-ds-secondary hover:text-ds-primary text-xs font-semibold"
              >
                Return to Exam
              </button>
              <button
                type="button"
                onClick={handleManualSubmit}
                className="px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-90"
              >
                Confirm & Submit
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
