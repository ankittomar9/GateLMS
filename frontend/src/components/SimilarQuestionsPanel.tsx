import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, 
  Layers, 
  ArrowRight, 
  CheckCircle2, 
  Circle,
  Loader2,
  HelpCircle
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { SimilarQuestionsResponse } from '../types/journey';
import { QuestionSummary } from '../api/types';
import { MathText } from './MathText';

interface SimilarQuestionsPanelProps {
  questionId: string;
  onSelectQuestion?: (questionId: string) => void;
}

export const SimilarQuestionsPanel: React.FC<SimilarQuestionsPanelProps> = ({
  questionId,
  onSelectQuestion,
}) => {
  const navigate = useNavigate();
  const [data, setData] = useState<SimilarQuestionsResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadSimilar() {
      if (!questionId) return;
      try {
        setLoading(true);
        setError(null);
        const res = await journeyApi.getSimilarQuestions(questionId, 5);
        if (isMounted) {
          setData(res);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err?.message || 'Failed to load similar practice questions');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }
    loadSimilar();
    return () => {
      isMounted = false;
    };
  }, [questionId]);

  if (loading) {
    return (
      <div 
        className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-3"
        data-testid="similar-questions-loading"
      >
        <div className="flex items-center gap-2 text-xs font-mono text-ds-muted">
          <Loader2 className="w-4 h-4 animate-spin text-ds-accent" />
          <span>Finding similar questions...</span>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return null; // Silently fail or hide if unavailable
  }

  const { questions, similarityBasis } = data;

  return (
    <div 
      className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-4 shadow-sm"
      data-testid="similar-questions-panel"
    >
      {/* Header with Basis Badge */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ds-border pb-3">
        <div className="flex items-center gap-2">
          <h3 className="text-sm font-bold text-ds-primary tracking-tight">
            Practice More Like This
          </h3>
          <span 
            className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded flex items-center gap-1 border ${
              similarityBasis === 'CONCEPT'
                ? 'bg-purple-500/15 border-purple-500/30 text-purple-300'
                : 'bg-blue-500/15 border-blue-500/30 text-blue-300'
            }`}
            data-testid="similarity-basis-badge"
          >
            {similarityBasis === 'CONCEPT' ? (
              <>
                <Sparkles className="w-3 h-3 text-purple-400" />
                <span>Same Concept</span>
              </>
            ) : (
              <>
                <Layers className="w-3 h-3 text-blue-400" />
                <span>Same Topic</span>
              </>
            )}
          </span>
        </div>

        <span className="text-[11px] font-mono text-ds-muted">
          {questions.length} question{questions.length === 1 ? '' : 's'} available
        </span>
      </div>

      {/* List of Similar Questions */}
      {questions.length === 0 ? (
        <div 
          className="text-center py-6 text-xs font-mono text-ds-muted space-y-1"
          data-testid="similar-questions-empty"
        >
          <p>No similar unsolved questions found in this topic.</p>
          <p className="text-[11px] text-ds-secondary">Great work! You have tackled all available questions here.</p>
        </div>
      ) : (
        <div className="space-y-2.5" data-testid="similar-questions-list">
          {questions.map((q: QuestionSummary) => {
            const handleItemClick = () => {
              if (onSelectQuestion) {
                onSelectQuestion(q.id);
              } else {
                navigate(`/questions/${q.id}`);
              }
            };

            return (
              <div
                key={q.id}
                onClick={handleItemClick}
                className="p-3 rounded-ds bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong flex items-center justify-between gap-3 cursor-pointer group transition-all"
                data-testid={`similar-question-${q.id}`}
              >
                <div className="space-y-1 flex-1 min-w-0">
                  <div className="flex items-center gap-2 text-[10px] font-mono">
                    {q.year && <span className="font-bold text-ds-accent">{q.year}</span>}
                    <span className="text-ds-muted">·</span>
                    <span className="text-ds-secondary">{q.type}</span>
                    <span className="text-ds-muted">·</span>
                    <span className="text-ds-secondary">{q.marks} Mark{q.marks === 1 ? '' : 's'}</span>
                    {q.difficulty && (
                      <>
                        <span className="text-ds-muted">·</span>
                        <span className="text-ds-muted uppercase">{q.difficulty}</span>
                      </>
                    )}
                  </div>
                  <div className="text-xs text-ds-primary group-hover:text-ds-accent transition-colors line-clamp-1">
                    <MathText content={q.statement || ''} />
                  </div>
                </div>

                <div className="shrink-0 text-ds-muted group-hover:text-ds-accent transition-colors pl-2">
                  <ArrowRight className="w-4 h-4" />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
