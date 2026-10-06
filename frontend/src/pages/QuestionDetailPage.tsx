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
        return 'text-amber-400 border-amber-500/20 bg-amber-500/10';
      case 'MSQ':
        return 'text-cyan-400 border-cyan-500/20 bg-cyan-500/10';
      case 'NAT':
        return 'text-emerald-400 border-emerald-500/20 bg-emerald-500/10';
      default:
        return 'text-zinc-400 border-zinc-700 bg-zinc-800';
    }
  };

  if (isLoading) {
    return (
      <div className="p-8 max-w-4xl mx-auto space-y-6">
        <div className="h-8 bg-[#161616] rounded-lg animate-pulse w-48" />
        <div className="h-48 bg-[#161616] rounded-xl animate-pulse" />
        <div className="space-y-3">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-16 bg-[#161616] rounded-lg animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (error || !q) {
    return (
      <div className="p-8 max-w-2xl mx-auto text-center space-y-4">
        <AlertCircle className="w-10 h-10 text-rose-500 mx-auto" />
        <h2 className="text-lg font-bold text-white">Question not found</h2>
        <p className="text-sm text-zinc-400">
          The requested question could not be loaded or does not exist.
        </p>
        <button
          onClick={() => navigate('/questions')}
          className="px-4 py-2 bg-amber-500 text-black font-semibold rounded-lg text-sm hover:bg-amber-400 transition-colors"
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
          className="flex items-center gap-2 text-xs text-zinc-400 hover:text-white transition-colors"
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
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-[#262626] bg-[#161616] hover:bg-[#1f1f1f] text-xs text-zinc-300 transition-colors"
            >
              <span>GATE Overflow Discussion</span>
              <ExternalLink className="w-3.5 h-3.5 text-zinc-500" />
            </a>
          )}

          <button
            type="button"
            onClick={() => toggleBookmark.mutate(q.id)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs transition-colors ${
              q.isBookmarked
                ? 'bg-amber-500/10 border-amber-500/30 text-amber-400'
                : 'bg-[#161616] border-[#262626] text-zinc-400 hover:text-zinc-200'
            }`}
          >
            <Bookmark className={`w-3.5 h-3.5 ${q.isBookmarked ? 'fill-amber-400' : ''}`} />
            <span>{q.isBookmarked ? 'Bookmarked' : 'Bookmark'}</span>
          </button>
        </div>
      </div>

      {conflictError && (
        <div className="bg-rose-500/10 border border-rose-500/30 text-rose-300 px-4 py-3 rounded-lg text-sm flex items-center justify-between">
          <span>{conflictError}</span>
          <button
            onClick={() => {
              setConflictError(null);
              refetch();
            }}
            className="text-xs text-amber-400 hover:underline ml-4"
          >
            Reload Latest
          </button>
        </div>
      )}

      {/* Main Question Card */}
      <div className="bg-[#161616] border border-[#262626] rounded-xl p-6 md:p-8 space-y-6">
        {/* Header Tags */}
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[#262626] pb-4">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`px-2.5 py-1 rounded text-xs font-mono font-semibold border ${getTypeBadge(q.type)}`}>
              {q.type}
            </span>
            <span className="font-mono text-xs text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2.5 py-1 rounded">
              {q.marks} {q.marks === 1 ? 'Mark' : 'Marks'}
            </span>
            <span className="font-mono text-xs text-zinc-400 bg-[#1f1f1f] px-2.5 py-1 rounded border border-[#2a2a2a]">
              GATE {q.year}
            </span>
            <span className="font-mono text-xs text-zinc-500">
              ID: {q.externalId}
            </span>
          </div>

          <div className="text-xs text-zinc-400 flex items-center gap-1.5">
            <span className="text-zinc-300 font-medium">{q.subjectName}</span>
            <span className="text-zinc-600">/</span>
            <span className="text-zinc-400">{q.topicName}</span>
          </div>
        </div>

        {/* Question Statement */}
        <div className="text-base text-zinc-100 leading-relaxed font-normal">
          <MathText content={q.statement} />
        </div>

        {/* Multiple Choice Options List (MCQ / MSQ) */}
        {options.length > 0 && (
          <div className="space-y-3 pt-2">
            <div className="text-xs font-semibold uppercase tracking-wider text-zinc-500">
              Options:
            </div>
            {options.map((opt) => {
              const isCorrect = correctOptionLabels.includes(opt.label);
              const highlightCorrect = solutionRevealed && isCorrect;

              return (
                <div
                  key={opt.label}
                  className={`p-4 rounded-xl border transition-all flex items-start gap-3.5 ${
                    highlightCorrect
                      ? 'bg-emerald-500/10 border-emerald-500/50 text-emerald-200'
                      : 'bg-[#121212] border-[#262626] text-zinc-300'
                  }`}
                >
                  <span
                    className={`w-7 h-7 rounded-lg flex items-center justify-center font-mono text-xs font-bold shrink-0 border ${
                      highlightCorrect
                        ? 'bg-emerald-500 text-black border-emerald-400'
                        : 'bg-[#1f1f1f] text-zinc-400 border-[#2a2a2a]'
                    }`}
                  >
                    {opt.label}
                  </span>
                  <div className="flex-1 pt-0.5 text-sm">
                    <MathText content={opt.text} />
                  </div>
                  {highlightCorrect && (
                    <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
                  )}
                </div>
              );
            })}
          </div>
        )}

        {/* NAT Question Input Area */}
        {q.type === 'NAT' && !solutionRevealed && (
          <div className="bg-[#121212] border border-[#262626] rounded-xl p-5 space-y-3">
            <div className="text-xs font-semibold text-zinc-400">Numerical Answer (NAT):</div>
            <div className="flex items-center gap-3">
              <input
                type="number"
                placeholder="Enter calculated value..."
                className="bg-[#1a1a1a] border border-[#262626] rounded-lg px-3.5 py-2 text-sm text-white focus:outline-none focus:border-amber-500/50 w-64"
              />
              <span className="text-xs text-zinc-500">
                Self-verify before revealing solution.
              </span>
            </div>
          </div>
        )}

        {/* Reveal Solution / Two-Pass Toggle */}
        <div className="border-t border-[#262626] pt-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="text-xs text-zinc-400">
            {solutionRevealed ? (
              <span className="text-emerald-400 font-medium flex items-center gap-1.5">
                <Check className="w-4 h-4" /> Solution Unmasked (Review carefully)
              </span>
            ) : (
              <span className="text-zinc-500">
                Honest-Attempt Mode: Attempt calculation before revealing solution key.
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={() => setSolutionRevealed(!solutionRevealed)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-semibold transition-all ${
              solutionRevealed
                ? 'bg-zinc-800 text-zinc-300 hover:bg-zinc-700 border border-zinc-700'
                : 'bg-amber-500 hover:bg-amber-400 text-black shadow-md shadow-amber-500/20'
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
          <div className="bg-[#101915] border border-emerald-500/30 rounded-xl p-6 space-y-4 animate-in fade-in">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-emerald-400 uppercase tracking-wider flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4" />
                <span>Answer Key & Detailed Explanation</span>
              </h3>
            </div>

            {/* Correct Key */}
            <div className="text-xs text-zinc-300">
              {q.type === 'NAT' ? (
                <div>
                  Accepted Tolerance Range:{' '}
                  <strong className="text-emerald-400 font-mono text-sm">
                    [{q.numericMin} to {q.numericMax}]
                  </strong>
                </div>
              ) : (
                <div>
                  Correct Option(s):{' '}
                  <strong className="text-emerald-400 font-mono text-sm">
                    {correctOptionLabels.join(', ')}
                  </strong>
                </div>
              )}
            </div>

            {/* Explanation with MathText */}
            {q.explanation ? (
              <div className="pt-2 text-sm text-zinc-200 border-t border-emerald-500/20 leading-relaxed">
                <MathText content={q.explanation} />
              </div>
            ) : (
              <p className="text-xs text-zinc-500 italic">
                No formal explanation authored for this question yet.
              </p>
            )}
          </div>
        )}
      </div>

      {/* Two-Pass Preparation State Toggles */}
      <div className="bg-[#161616] border border-[#262626] rounded-xl p-6 space-y-5">
        <h3 className="text-xs font-semibold text-zinc-400 uppercase tracking-wider">
          Two-Pass Preparation Status Tracker
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Pass 1 Tracker */}
          <div className="space-y-2">
            <label className="text-xs text-zinc-400 block font-medium">
              Pass 1 (Concept Building):
            </label>
            <div className="flex flex-wrap gap-2">
              {(['UNSEEN', 'SOLVED', 'REVISE', 'FLAGGED'] as Pass1Status[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => handlePass1Change(st)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-mono font-medium border transition-colors ${
                    q.pass1Status === st
                      ? 'bg-amber-500 text-black border-amber-400 font-bold'
                      : 'bg-[#121212] border-[#262626] text-zinc-400 hover:text-zinc-200'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>
          </div>

          {/* Pass 2 Tracker */}
          <div className="space-y-2">
            <label className="text-xs text-zinc-400 block font-medium">
              Pass 2 (Speed & Accuracy Timed):
            </label>
            <div className="flex flex-wrap gap-2">
              {(['NOT_ATTEMPTED', 'SOLVED', 'FAILED', 'TIMEOUT'] as Pass2Status[]).map((st) => (
                <button
                  key={st}
                  type="button"
                  onClick={() => handlePass2Change(st)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-mono font-medium border transition-colors ${
                    q.pass2Status === st
                      ? 'bg-cyan-500 text-black border-cyan-400 font-bold'
                      : 'bg-[#121212] border-[#262626] text-zinc-400 hover:text-zinc-200'
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
