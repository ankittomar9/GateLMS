import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  Bookmark, 
  ExternalLink, 
  Eye, 
  EyeOff, 
  CheckCircle2, 
  AlertCircle, 
  Clock, 
  Check, 
  Layers,
  HelpCircle,
  FileQuestion
} from 'lucide-react';
import { 
  useQuestion, 
  useUpdatePass1Status, 
  useUpdatePass2Status, 
  useToggleBookmark 
} from '../hooks/useQueries';
import { MathText } from '../components/MathText';
import { Pass1Status, Pass2Status, QuestionType } from '../api/types';

interface OptionItem {
  label: string;
  text: string;
}

export const QuestionDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [solutionRevealed, setSolutionRevealed] = useState(false);
  const [conflictError, setConflictError] = useState<string | null>(null);

  const { data: q, isLoading, error, refetch } = useQuestion(id);
  const updatePass1 = useUpdatePass1Status();
  const updatePass2 = useUpdatePass2Status();
  const toggleBookmark = useToggleBookmark();

  const handlePass1Change = async (status: Pass1Status) => {
    if (!q) return;
    setConflictError(null);
    try {
      await updatePass1.mutateAsync({ id: q.id, status });
    } catch (err: any) {
      if (err.status === 409) {
        setConflictError('Question status updated elsewhere. Please refresh to load latest changes.');
      } else {
        setConflictError(err.message || 'Failed to update Pass 1 status');
      }
    }
  };

  const handlePass2Change = async (status: Pass2Status) => {
    if (!q) return;
    setConflictError(null);
    try {
      await updatePass2.mutateAsync({ id: q.id, status });
    } catch (err: any) {
      if (err.status === 409) {
        setConflictError('Question status updated elsewhere. Please refresh to load latest changes.');
      } else {
        setConflictError(err.message || 'Failed to update Pass 2 status');
      }
    }
  };

  // Parse options safely
  const options: OptionItem[] = React.useMemo(() => {
    if (!q?.optionsJson) return [];
    try {
      const parsed = JSON.parse(q.optionsJson);
      if (Array.isArray(parsed)) {
        return parsed.map((item) => ({
          label: String(item.label || item.key || ''),
          text: String(item.text || item.value || ''),
        }));
      }
      if (typeof parsed === 'object') {
        return Object.entries(parsed).map(([label, text]) => ({
          label,
          text: String(text),
        }));
      }
    } catch {
      return [];
    }
    return [];
  }, [q?.optionsJson]);

  const correctOptionLabels = React.useMemo(() => {
    if (!q?.correctOptions) return [];
    try {
      const parsed = JSON.parse(q.correctOptions);
      if (Array.isArray(parsed)) return parsed.map(String);
    } catch {
      // If it's a comma-separated or plain string like "A" or "A,B"
      return q.correctOptions.split(',').map((s) => s.trim());
    }
    return [q.correctOptions.trim()];
  }, [q?.correctOptions]);

  const getTypeBadge = (type?: QuestionType) => {
    switch (type) {
      case 'MCQ':
        return 'text-ds-accent border-ds-accent/30 bg-ds-accent-soft';
      case 'MSQ':
        return 'text-ds-info border-ds-info/30 bg-ds-info/10';
      case 'NAT':
        return 'text-ds-success border-ds-success/30 bg-ds-success/10';
      default:
        return 'text-ds-muted border-ds-border bg-ds-surface-raised';
    }
  };

  if (isLoading) {
    return (
      <div className="p-8 max-w-4xl mx-auto space-y-6">
        <div className="h-8 bg-ds-surface-raised rounded-ds animate-pulse w-48" />
        <div className="h-48 bg-ds-surface rounded-ds animate-pulse" />
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-16 bg-ds-surface rounded-ds animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error || !q) {
    return (
      <div className="p-8 max-w-2xl mx-auto text-center space-y-4">
        <AlertCircle className="w-10 h-10 text-ds-danger mx-auto" />
        <h2 className="text-lg font-bold text-ds-primary">Question not found</h2>
        <p className="text-sm text-ds-muted">
          The requested question could not be loaded or does not exist.
        </p>
        <button
          onClick={() => navigate('/questions')}
          className="px-4 py-2 bg-ds-accent text-ds-accent-contrast font-semibold rounded-ds text-sm hover:bg-ds-accent-hover transition-colors"
        >
          Return to Question Bank
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12">
      {/* Back button and quick actions */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-xs text-ds-muted hover:text-ds-primary transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Questions</span>
        </button>

        <div className="flex items-center gap-3">
          {q.gateOverflowUrl && (
            <a
              href={q.gateOverflowUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface hover:bg-ds-surface-hover text-xs text-ds-secondary transition-colors"
            >
              <span>GATE Overflow Discussion</span>
              <ExternalLink className="w-3.5 h-3.5 text-ds-muted" />
            </a>
          )}

          <button
            type="button"
            onClick={() => toggleBookmark.mutate(q.id)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-ds border text-xs transition-colors ${
              q.isBookmarked
                ? 'bg-ds-accent-soft border-ds-accent/30 text-ds-accent'
                : 'bg-ds-surface border-ds-border text-ds-muted hover:text-ds-secondary'
            }`}
          >
            <Bookmark className={`w-3.5 h-3.5 ${q.isBookmarked ? 'fill-ds-accent text-ds-accent' : ''}`} />
            <span>{q.isBookmarked ? 'Bookmarked' : 'Bookmark'}</span>
          </button>
        </div>
      </div>

      {conflictError && (
        <div className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger px-4 py-3 rounded-ds text-sm flex items-center justify-between">
          <span>{conflictError}</span>
          <button
            onClick={() => {
              setConflictError(null);
              refetch();
            }}
            className="text-xs text-ds-accent hover:underline ml-4"
          >
            Reload Latest
          </button>
        </div>
      )}

      {/* Main Question Card */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 md:p-8 space-y-6 shadow-ds-card">
        {/* Header Tags */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ds-border pb-4">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`px-2.5 py-1 rounded-ds text-xs font-mono font-semibold border ${getTypeBadge(q.type)}`}>
              {q.type}
            </span>
            <span className="font-mono text-xs text-ds-accent bg-ds-accent-soft border border-ds-accent/30 px-2.5 py-1 rounded-ds">
              {q.marks} {q.marks === 1 ? 'Mark' : 'Marks'}
            </span>
            <span className="font-mono text-xs text-ds-muted bg-ds-surface-raised px-2.5 py-1 rounded-ds border border-ds-border">
              GATE {q.year}
            </span>
            {q.source && (
              <span className="font-mono text-xs text-ds-secondary bg-ds-surface-raised px-2.5 py-1 rounded-ds border border-ds-border">
                {q.source}
              </span>
            )}
            {q.difficulty && (
              <span className="font-mono text-xs px-2.5 py-1 rounded-ds border text-ds-warning bg-ds-warning/10 border-ds-warning/30">
                {q.difficulty}
              </span>
            )}
            {q.curationStatus && (
              <span className={`font-mono text-xs px-2.5 py-1 rounded-ds border ${
                q.curationStatus === 'ACCEPTED'
                  ? 'text-ds-success bg-ds-success/10 border-ds-success/30'
                  : q.curationStatus === 'REJECTED'
                  ? 'text-ds-danger bg-ds-danger/10 border-ds-danger/30'
                  : 'text-ds-muted bg-ds-surface-raised border-ds-border'
              }`}>
                {q.curationStatus}
              </span>
            )}
            <span className="font-mono text-xs text-ds-muted">
              ID: {q.externalId}
            </span>
          </div>

          <div className="text-xs text-ds-muted flex items-center gap-1.5">
            <span className="text-ds-secondary font-medium">{q.subjectName}</span>
            <span className="text-ds-muted">/</span>
            <span className="text-ds-muted">{q.topicName}</span>
          </div>
        </div>

        {q.curationNote && (
          <div className="p-3 bg-ds-surface-raised border border-ds-border rounded-ds text-xs text-ds-secondary">
            <strong className="text-ds-primary font-mono">Curation Note:</strong> {q.curationNote}
          </div>
        )}

        {/* Question Statement */}
        <div className="text-base text-ds-primary leading-relaxed font-normal">
          <MathText content={q.statement} />
        </div>

        {/* Multiple Choice Options List (MCQ / MSQ) */}
        {options.length > 0 && (
          <div className="space-y-3 pt-2">
            <div className="text-xs font-semibold uppercase tracking-wider text-ds-muted">
              Options:
            </div>
            {options.map((opt) => {
              const isCorrect = correctOptionLabels.includes(opt.label);
              const highlightCorrect = solutionRevealed && isCorrect;

              return (
                <div
                  key={opt.label}
                  className={`p-4 rounded-ds border transition-all flex items-start gap-3.5 ${
                    highlightCorrect
                      ? 'bg-ds-success/10 border-ds-success/50 text-ds-success'
                      : 'bg-ds-surface-raised border-ds-border text-ds-secondary'
                  }`}
                >
                  <span
                    className={`w-7 h-7 rounded-ds flex items-center justify-center font-mono text-xs font-bold shrink-0 border ${
                      highlightCorrect
                        ? 'bg-ds-success text-ds-accent-contrast border-ds-success'
                        : 'bg-ds-surface text-ds-muted border-ds-border'
                    }`}
                  >
                    {opt.label}
                  </span>
                  <div className="flex-1 pt-0.5 text-sm text-ds-primary">
                    <MathText content={opt.text} />
                  </div>
                  {highlightCorrect && (
                    <CheckCircle2 className="w-5 h-5 text-ds-success shrink-0 mt-0.5" />
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* NAT Question Input Area */}
        {q.type === 'NAT' && !solutionRevealed && (
          <div className="bg-ds-surface-raised border border-ds-border rounded-ds p-5 space-y-3">
            <div className="text-xs font-semibold text-ds-secondary">Numerical Answer (NAT):</div>
            <div className="flex items-center gap-3">
              <input
                type="number"
                placeholder="Enter calculated value..."
                className="bg-ds-surface-input border border-ds-border rounded-ds px-3.5 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent/60 w-64"
              />
              <span className="text-xs text-ds-muted">
                Self-verify before revealing solution.
              </span>
            </div>
          </div>
        )}

        {/* Reveal Solution / Two-Pass Toggle */}
        <div className="border-t border-ds-border pt-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="text-xs text-ds-muted">
            {solutionRevealed ? (
              <span className="text-ds-success font-medium flex items-center gap-1.5">
                <Check className="w-4 h-4" /> Solution Unmasked (Review carefully)
              </span>
            ) : (
              <span className="text-ds-muted">
                Honest-Attempt Mode: Attempt calculation before revealing solution key.
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={() => setSolutionRevealed(!solutionRevealed)}
            className={`flex items-center gap-2 px-4 py-2 rounded-ds text-xs font-semibold transition-all ${
              solutionRevealed
                ? 'bg-ds-surface-raised text-ds-secondary hover:bg-ds-surface-hover border border-ds-border'
                : 'bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast shadow-md'
            }`}
          >
            {solutionRevealed ? (
              <>
                <EyeOff className="w-4 h-4" />
                <span>Mask Solution</span>
              </>
            ) : (
              <>
                <Eye className="w-4 h-4" />
                <span>Reveal Solution & Key</span>
              </>
            )}
          </button>
        </div>

        {/* Solution Details (Revealed) */}
        {solutionRevealed && (
          <div className="bg-ds-surface-raised border border-ds-success/30 rounded-ds p-6 space-y-4 animate-in fade-in">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-ds-success uppercase tracking-wider flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4" />
                <span>Answer Key & Detailed Explanation</span>
              </h3>
            </div>

            {/* Correct Key */}
            <div className="text-xs text-ds-secondary">
              {q.type === 'NAT' ? (
                <div>
                  Accepted Tolerance Range:{' '}
                  <strong className="text-ds-success font-mono text-sm">
                    [{q.numericMin} to {q.numericMax}]
                  </strong>
                </div>
              ) : (
                <div>
                  Correct Option(s):{' '}
                  <strong className="text-ds-success font-mono text-sm">
                    {correctOptionLabels.join(', ')}
                  </strong>
                </div>
              )}
            </div>

            {/* Explanation with MathText */}
            {q.explanation ? (
              <div className="pt-2 text-sm text-ds-primary border-t border-ds-border leading-relaxed">
                <MathText content={q.explanation} />
              </div>
            ) : (
              <p className="text-xs text-ds-muted italic">
                No formal explanation authored for this question yet.
              </p>
            )}
          </div>
        )}
      </div>

      {/* Two-Pass Preparation State Toggles */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 space-y-5 shadow-ds-card">
        <h3 className="text-xs font-semibold text-ds-muted uppercase tracking-wider">
          Two-Pass Preparation Status Tracker
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Pass 1 Tracker */}
          <div className="space-y-2">
            <label className="text-xs text-ds-secondary block font-medium">
              Pass 1 (Concept Building):
            </label>
            <div className="flex flex-wrap gap-2">
              {(['UNSEEN', 'SOLVED', 'REVISE', 'FLAGGED'] as Pass1Status[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => handlePass1Change(st)}
                  className={`px-3 py-1.5 rounded-ds text-xs font-mono font-medium border transition-colors ${
                    q.pass1Status === st
                      ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent font-bold'
                      : 'bg-ds-surface-raised border-ds-border text-ds-muted hover:text-ds-secondary'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>

          {/* Pass 2 Tracker */}
          <div className="space-y-2">
            <label className="text-xs text-ds-secondary block font-medium">
              Pass 2 (Speed & Accuracy Timed):
            </label>
            <div className="flex flex-wrap gap-2">
              {(['NOT_ATTEMPTED', 'SOLVED', 'FAILED', 'TIMEOUT'] as Pass2Status[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => handlePass2Change(st)}
                  className={`px-3 py-1.5 rounded-ds text-xs font-mono font-medium border transition-colors ${
                    q.pass2Status === st
                      ? 'bg-ds-info text-ds-accent-contrast border-ds-info font-bold'
                      : 'bg-ds-surface-raised border-ds-border text-ds-muted hover:text-ds-secondary'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default QuestionDetailPage;
