import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Award, 
  CheckCircle2, 
  XCircle, 
  HelpCircle, 
  Clock, 
  ArrowRight, 
  RotateCcw, 
  Filter, 
  AlertTriangle 
} from 'lucide-react';
import { MathText } from '../components/MathText';
import { useMockResult } from '../hooks/useQueries';

export const ResultPage: React.FC = () => {
  const { attemptId } = useParams<{ attemptId: string }>();
  const navigate = useNavigate();
  const { data: result, isLoading, error } = useMockResult(attemptId || '');

  const [filterType, setFilterType] = useState<'ALL' | 'WRONG' | 'UNATTEMPTED'>('ALL');

  if (isLoading) {
    return (
      <div className="p-16 text-center text-ds-muted">
        Loading examination results...
      </div>
    );
  }

  if (error || !result) {
    return (
      <div className="p-12 text-center bg-ds-surface border border-ds-border rounded-ds space-y-3">
        <AlertTriangle className="w-10 h-10 text-ds-danger mx-auto" />
        <h2 className="text-base font-bold text-ds-primary">Result Not Available</h2>
        <p className="text-xs text-ds-secondary">
          This attempt has not been submitted or the result could not be retrieved.
        </p>
        <button
          type="button"
          onClick={() => navigate('/mocks')}
          className="px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold"
        >
          Back to Mock Tests
        </button>
      </div>
    );
  }

  const {
    mockTitle,
    totalScore,
    positiveScore,
    negativeScore,
    maxMarks,
    finishedReason,
    questions,
    sectionScores
  } = result;

  const totalQuestions = questions.length;
  const correctCount = questions.filter((q) => q.isCorrect === true).length;
  const wrongCount = questions.filter((q) => q.isCorrect === false).length;
  const unattemptedCount = totalQuestions - correctCount - wrongCount;

  // Filtered questions
  const filteredQuestions = questions.filter((q) => {
    if (filterType === 'WRONG') return q.isCorrect === false;
    if (filterType === 'UNATTEMPTED') return q.isCorrect === null || q.state === 'NOT_VISITED' || q.state === 'NOT_ANSWERED';
    return true;
  });

  // Weakest subject identification
  const weakestSubject = sectionScores.length > 0
    ? [...sectionScores].sort((a, b) => a.accuracy - b.accuracy)[0]
    : null;

  return (
    <div className="max-w-6xl mx-auto space-y-8 pb-16">
      {/* Top Banner */}
      <div className="border-b border-ds-border pb-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-mono text-ds-muted uppercase tracking-wider block">
            Examination Review • {finishedReason}
          </span>
          <h1 className="text-2xl font-bold text-ds-primary mt-1 tracking-tight flex items-center gap-2">
            <Award className="w-6 h-6 text-ds-accent" />
            {mockTitle}
          </h1>
        </div>

        <div className="flex items-center gap-2">
          {weakestSubject && (
            <button
              type="button"
              onClick={() => navigate(`/practice?subjectId=${weakestSubject.subjectId}`)}
              className="px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold shadow-sm hover:opacity-90 flex items-center gap-1.5"
            >
              Practice Weakest Subject ({weakestSubject.subjectName})
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          )}
          <button
            type="button"
            onClick={() => navigate('/mocks')}
            className="px-3.5 py-2 rounded-ds border border-ds-border text-ds-secondary hover:text-ds-primary text-xs font-semibold"
          >
            All Mocks
          </button>
        </div>
      </div>

      {/* Score Hero Trio */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-ds-surface border border-ds-border rounded-ds p-4 text-center shadow-sm">
          <span className="text-xs text-ds-muted block">Final Score (Clamped)</span>
          <span className="text-3xl font-extrabold text-ds-accent block mt-1 font-mono">
            {totalScore.toFixed(2)}
          </span>
          <span className="text-[11px] text-ds-secondary block mt-0.5">
            Out of {maxMarks} marks
          </span>
        </div>

        <div className="bg-ds-surface border border-ds-border rounded-ds p-4 text-center shadow-sm">
          <span className="text-xs text-ds-muted block">Positive Score</span>
          <span className="text-3xl font-extrabold text-ds-success block mt-1 font-mono">
            +{positiveScore.toFixed(2)}
          </span>
          <span className="text-[11px] text-ds-secondary block mt-0.5">
            {correctCount} correct
          </span>
        </div>

        <div className="bg-ds-surface border border-ds-border rounded-ds p-4 text-center shadow-sm">
          <span className="text-xs text-ds-muted block">Negative Penalty</span>
          <span className="text-3xl font-extrabold text-ds-danger block mt-1 font-mono">
            −{negativeScore.toFixed(2)}
          </span>
          <span className="text-[11px] text-ds-secondary block mt-0.5">
            {wrongCount} wrong (GATE −⅓)
          </span>
        </div>

        <div className="bg-ds-surface border border-ds-border rounded-ds p-4 text-center shadow-sm">
          <span className="text-xs text-ds-muted block">Accuracy</span>
          <span className="text-3xl font-extrabold text-ds-primary block mt-1 font-mono">
            {correctCount + wrongCount > 0
              ? `${Math.round((correctCount / (correctCount + wrongCount)) * 100)}%`
              : '0%'}
          </span>
          <span className="text-[11px] text-ds-secondary block mt-0.5">
            {unattemptedCount} unattempted
          </span>
        </div>
      </div>

      {/* Per-Section Score Breakdown Table */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-3">
        <h2 className="text-sm font-semibold text-ds-primary">Section-wise Performance Breakdown</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-ds-border text-ds-muted">
                <th className="py-2.5 px-3 font-semibold">Subject / Section</th>
                <th className="py-2.5 px-3 font-semibold text-center">Questions</th>
                <th className="py-2.5 px-3 font-semibold text-center">Attempted</th>
                <th className="py-2.5 px-3 font-semibold text-center text-ds-success">Correct</th>
                <th className="py-2.5 px-3 font-semibold text-center text-ds-danger">Wrong</th>
                <th className="py-2.5 px-3 font-semibold text-center">Marks Awarded</th>
                <th className="py-2.5 px-3 font-semibold text-center">Accuracy</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-ds-border">
              {sectionScores.map((sec) => (
                <tr key={sec.subjectId} className="hover:bg-ds-surface-hover transition-colors">
                  <td className="py-2.5 px-3 font-medium text-ds-primary">
                    {sec.subjectName}
                  </td>
                  <td className="py-2.5 px-3 text-center text-ds-secondary font-mono">{sec.totalQuestions}</td>
                  <td className="py-2.5 px-3 text-center text-ds-secondary font-mono">{sec.attemptedCount}</td>
                  <td className="py-2.5 px-3 text-center font-bold font-mono text-ds-success">{sec.correctCount}</td>
                  <td className="py-2.5 px-3 text-center font-bold font-mono text-ds-danger">{sec.wrongCount}</td>
                  <td className="py-2.5 px-3 text-center font-bold font-mono text-ds-primary">
                    {sec.marksAwarded.toFixed(2)} / {sec.maxMarks}
                  </td>
                  <td className="py-2.5 px-3 text-center font-bold font-mono text-ds-accent">
                    {sec.accuracy.toFixed(1)}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Detailed Question Review List */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <h2 className="text-base font-bold text-ds-primary">Question Review & Solutions</h2>

          {/* Filter Pills */}
          <div className="flex items-center gap-1.5 bg-ds-surface border border-ds-border p-1 rounded-ds self-start">
            <button
              type="button"
              onClick={() => setFilterType('ALL')}
              className={`px-3 py-1 rounded-ds text-xs font-semibold transition-all ${
                filterType === 'ALL' ? 'bg-ds-accent text-ds-accent-contrast' : 'text-ds-secondary hover:text-ds-primary'
              }`}
            >
              All ({totalQuestions})
            </button>
            <button
              type="button"
              onClick={() => setFilterType('WRONG')}
              className={`px-3 py-1 rounded-ds text-xs font-semibold transition-all ${
                filterType === 'WRONG' ? 'bg-ds-danger text-ds-accent-contrast' : 'text-ds-secondary hover:text-ds-primary'
              }`}
            >
              Wrong Only ({wrongCount})
            </button>
            <button
              type="button"
              onClick={() => setFilterType('UNATTEMPTED')}
              className={`px-3 py-1 rounded-ds text-xs font-semibold transition-all ${
                filterType === 'UNATTEMPTED' ? 'bg-ds-surface-hover text-ds-primary border border-ds-border' : 'text-ds-secondary hover:text-ds-primary'
              }`}
            >
              Unattempted ({unattemptedCount})
            </button>
          </div>
        </div>

        {/* Question Cards */}
        <div className="space-y-4">
          {filteredQuestions.map((q, idx) => {
            const isCorrect = q.isCorrect === true;
            const isWrong = q.isCorrect === false;
            const isUnattempted = !isCorrect && !isWrong;

            return (
              <div
                key={q.questionId}
                className={`bg-ds-surface border rounded-ds p-5 space-y-4 shadow-sm ${
                  isCorrect
                    ? 'border-ds-success/40'
                    : isWrong
                    ? 'border-ds-danger/40'
                    : 'border-ds-border'
                }`}
              >
                {/* Header */}
                <div className="flex items-center justify-between border-b border-ds-border pb-3">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-xs text-ds-primary">
                      Q{idx + 1}
                    </span>
                    <span className="text-[11px] px-2 py-0.5 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-secondary">
                      {q.type}
                    </span>
                    <span className="text-[11px] text-ds-muted">
                      {q.subjectName}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span
                      className={`text-xs font-mono font-bold px-2 py-0.5 rounded-ds ${
                        q.marksAwarded > 0
                          ? 'bg-ds-success/15 text-ds-success'
                          : q.marksAwarded < 0
                          ? 'bg-ds-danger/15 text-ds-danger'
                          : 'bg-ds-surface-hover text-ds-secondary'
                      }`}
                    >
                      {q.marksAwarded > 0 ? `+${q.marksAwarded}` : q.marksAwarded} Marks
                    </span>
                    {isCorrect && <CheckCircle2 className="w-4 h-4 text-ds-success" />}
                    {isWrong && <XCircle className="w-4 h-4 text-ds-danger" />}
                    {isUnattempted && <HelpCircle className="w-4 h-4 text-ds-muted" />}
                  </div>
                </div>

                {/* Statement */}
                <div className="text-xs text-ds-primary leading-relaxed">
                  <MathText content={q.statement} />
                </div>

                {/* Your Answer vs Correct Answer Diff */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs bg-ds-bg-sidebar border border-ds-border rounded-ds p-3">
                  <div>
                    <span className="text-ds-muted text-[11px] block">Your Response:</span>
                    <span className={`font-mono font-bold mt-0.5 block ${
                      isCorrect ? 'text-ds-success' : isWrong ? 'text-ds-danger' : 'text-ds-muted'
                    }`}>
                      {q.yourAnswer || (q.numericAnswer !== null && q.numericAnswer !== undefined ? q.numericAnswer : 'Not Answered')}
                    </span>
                  </div>

                  <div>
                    <span className="text-ds-muted text-[11px] block">Correct Answer:</span>
                    <span className="font-mono font-bold text-ds-success mt-0.5 block">
                      {q.correctAnswer || (q.numericMin !== null ? `[${q.numericMin}, ${q.numericMax}]` : 'N/A')}
                    </span>
                  </div>
                </div>

                {/* Explanation */}
                {q.explanation && (
                  <div className="pt-2 border-t border-ds-border text-xs text-ds-secondary space-y-1">
                    <span className="font-semibold text-ds-primary text-[11px] block">
                      Detailed Solution:
                    </span>
                    <div className="leading-relaxed pl-2 border-l-2 border-ds-accent text-ds-primary">
                      <MathText content={q.explanation} />
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
