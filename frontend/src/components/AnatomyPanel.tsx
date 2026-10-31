import React, { useState, useEffect } from 'react';
import { 
  Sparkles, 
  Key, 
  AlertOctagon, 
  BookOpen, 
  Compass, 
  Lock, 
  CheckCircle2, 
  Loader2, 
  AlertCircle,
  ExternalLink,
  ShieldCheck,
  ChevronRight,
  AlertTriangle,
  RefreshCw
} from 'lucide-react';
import { anatomyApi, QuestionAnatomyDto } from '../api/anatomyApi';
import { MathText } from './MathText';
import { ConceptUnitDialog } from './ConceptUnitDialog';

interface AnatomyPanelProps {
  questionId: string;
  hasAttempted?: boolean;
  isAdmin?: boolean;
  mode?: 'study' | 'test' | 'mock';
  testSubmitted?: boolean;
}

export const AnatomyPanel: React.FC<AnatomyPanelProps> = ({
  questionId,
  hasAttempted = false,
  isAdmin = false,
  mode = 'study',
  testSubmitted = false,
}) => {
  const [anatomy, setAnatomy] = useState<QuestionAnatomyDto | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [generating, setGenerating] = useState<boolean>(false);
  const [locked, setLocked] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeConceptSlug, setActiveConceptSlug] = useState<string | null>(null);

  // In test/mock mode, gating rule: visible only post-submission (Binding spec §5.4)
  const isGatedByMode = (mode === 'test' || mode === 'mock') && !testSubmitted;

  useEffect(() => {
    if (!questionId || isGatedByMode) return;

    setLoading(true);
    setLocked(false);
    setError(null);

    anatomyApi.getQuestionAnatomy(questionId)
      .then((data) => {
        setAnatomy(data);
        setLoading(false);
      })
      .catch((err: any) => {
        if (err.status === 403) {
          setLocked(true);
        } else if (err.status === 404) {
          setAnatomy(null);
        } else {
          setError(err.message || 'Failed to load anatomy dossier');
        }
        setLoading(false);
      });
  }, [questionId, hasAttempted, isGatedByMode]);

  const handleGenerate = async (force: boolean = false) => {
    setGenerating(true);
    setError(null);
    try {
      const generated = await anatomyApi.generateQuestionAnatomy(questionId, { force });
      setAnatomy(generated);
      setLocked(false);
    } catch (err: any) {
      if (err.status === 403) {
        setLocked(true);
      } else {
        setError(err.message || 'Failed to generate question anatomy');
      }
    } finally {
      setGenerating(false);
    }
  };

  if (isGatedByMode) {
    return (
      <div className="p-4 rounded-xl bg-[#16171a] border border-[#26282f] text-neutral-400 text-xs flex items-center gap-2.5">
        <Lock className="w-4 h-4 text-neutral-500" />
        <span>Examiner Anatomy is concealed during active test sessions. Available in post-submission review.</span>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="p-8 rounded-xl bg-[#16171a] border border-[#26282f] flex items-center justify-center gap-3 text-neutral-400 text-xs">
        <Loader2 className="w-4 h-4 animate-spin text-indigo-400" />
        <span>Loading Examiner Anatomy Dossier...</span>
      </div>
    );
  }

  if (locked) {
    return (
      <div className="p-6 rounded-2xl bg-[#16171b] border border-amber-500/20 text-neutral-300">
        <div className="flex items-start gap-4">
          <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white">Anatomy Dossier Locked Until Attempt</h4>
            <p className="text-xs text-neutral-400 mt-1 leading-relaxed">
              To preserve authentic problem-solving diagnostics, the examiner deconstruction and distractor autopsy unlock only after you submit your first attempt on this question.
            </p>
            {isAdmin && (
              <button
                onClick={() => handleGenerate(true)}
                disabled={generating}
                className="mt-3 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium transition-all"
              >
                {generating ? 'Generating (Admin Bypass)...' : 'Admin: Force Generate & View'}
              </button>
            )}
          </div>
        </div>
      </div>
    );
  }

  if (!anatomy) {
    return (
      <div className="p-6 rounded-2xl bg-[#16171b] border border-[#252830] text-center space-y-3">
        <div className="inline-flex p-3 rounded-2xl bg-indigo-500/10 text-indigo-400">
          <Sparkles className="w-6 h-6" />
        </div>
        <div>
          <h4 className="text-sm font-semibold text-white">No Anatomy Dossier Generated Yet</h4>
          <p className="text-xs text-neutral-400 mt-1 max-w-md mx-auto">
            Generate an examiner autopsy containing the tested concept, invariant rules, distractor trap analysis, and prerequisite units.
          </p>
        </div>
        {error && (
          <p className="text-xs text-rose-400">{error}</p>
        )}
        <button
          onClick={() => handleGenerate(false)}
          disabled={generating}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-lg shadow-indigo-600/20 disabled:opacity-50 transition-all"
        >
          {generating ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Generating Breakdown...
            </>
          ) : (
            <>
              <Sparkles className="w-4 h-4" />
              Generate Examiner Breakdown
            </>
          )}
        </button>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-[#272a33] bg-[#141518] p-6 text-neutral-200 space-y-6">
      {/* Dossier Header & Badge */}
      <div className="flex flex-wrap items-center justify-between gap-3 pb-4 border-b border-[#242730]">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-400">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white tracking-tight">Question Anatomy Dossier</h3>
            <p className="text-[11px] text-neutral-400">Examiner Intent &amp; Cognitive Misconception Autopsy</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {anatomy.status === 'ACCEPTED' ? (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <ShieldCheck className="w-3 h-3" /> CURATED
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
              AI-GENERATED — UNREVIEWED
            </span>
          )}
          {anatomy.generatorModel && (
            <span className="text-[10px] text-neutral-400 font-mono bg-neutral-900 px-2 py-0.5 rounded-md border border-neutral-800">
              {anatomy.generatorModel}
            </span>
          )}
          {isAdmin && (
            <button
              onClick={() => handleGenerate(true)}
              disabled={generating}
              title="Force regenerate anatomy dossier"
              className="p-1 rounded hover:bg-neutral-800 text-neutral-400 hover:text-white transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${generating ? 'animate-spin' : ''}`} />
            </button>
          )}
        </div>
      </div>

      {/* Incomplete / Error Banner */}
      {(anatomy.parseError || anatomy.testedConcept === 'Generation parsing incomplete') && (
        <div className="bg-amber-500/10 border border-amber-500/30 rounded-xl p-4 text-xs space-y-3" data-testid="anatomy-error-banner">
          <div className="flex items-center gap-2 text-amber-400 font-semibold">
            <AlertTriangle className="w-4 h-4" />
            <span>Anatomy Dossier Synthesis Incomplete or Provider Error</span>
          </div>
          <p className="text-neutral-400 text-xs leading-relaxed">
            The previous AI generation attempt timed out or failed to parse. You can retry generating a complete examiner autopsy.
          </p>
          {anatomy.parseError && (
            <div className="text-neutral-400 text-[11px] bg-black/40 p-2.5 rounded border border-neutral-800 font-mono overflow-x-auto whitespace-pre-wrap">
              {anatomy.parseError}
            </div>
          )}
          {error && (
            <p className="text-xs text-rose-400 font-medium">{error}</p>
          )}
          <div className="flex items-center gap-2">
            <button
              onClick={() => handleGenerate(true)}
              disabled={generating}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-medium shadow-md shadow-indigo-600/20 disabled:opacity-50 transition-all cursor-pointer"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${generating ? 'animate-spin' : ''}`} />
              <span>{generating ? 'Regenerating Dossier...' : 'Retry Generation'}</span>
            </button>
          </div>
        </div>
      )}

      {/* Section 1: What this tests */}
      {anatomy.testedConcept && anatomy.testedConcept !== 'Generation parsing incomplete' && (
        <div className="space-y-1.5">
          <h4 className="text-xs font-semibold text-indigo-400 uppercase tracking-wider flex items-center gap-1.5">
            <Sparkles className="w-3.5 h-3.5" /> 1. What This Tests
          </h4>
          <div className="p-3.5 rounded-xl bg-[#191a20] border border-[#23262f] text-xs text-neutral-200 leading-relaxed font-medium">
            <MathText content={anatomy.testedConcept} />
          </div>
        </div>
      )}

      {/* Section 2: The Invariant Rule */}
      {anatomy.invariantRule && (
        <div className="space-y-1.5">
          <h4 className="text-xs font-semibold text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
            <Key className="w-3.5 h-3.5" /> 2. The Core Invariant / Mathematical Law
          </h4>
          <div className="p-3.5 rounded-xl bg-amber-500/5 border border-amber-500/20 text-xs font-mono text-amber-200 leading-relaxed">
            <MathText content={anatomy.invariantRule} />
          </div>
        </div>
      )}

      {/* Section 3: Distractor Autopsy */}
      {anatomy.trapAnalysis && anatomy.trapAnalysis.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-xs font-semibold text-rose-400 uppercase tracking-wider flex items-center gap-1.5">
            <AlertOctagon className="w-3.5 h-3.5" /> 3. Distractor Autopsy &amp; Traps
          </h4>
          <div className="overflow-hidden rounded-xl border border-[#242730] bg-[#17181e]">
            <table className="w-full text-left text-xs">
              <thead className="bg-[#1b1d24] text-neutral-400 font-medium border-b border-[#242730]">
                <tr>
                  <th className="py-2.5 px-3.5 w-16">Option</th>
                  <th className="py-2.5 px-3.5 w-24">Verdict</th>
                  <th className="py-2.5 px-3.5">Why Plausible (Cognitive Trap)</th>
                  <th className="py-2.5 px-3.5">Underlying Misconception</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#242730]/60">
                {anatomy.trapAnalysis.map((item, idx) => {
                  const isCorrect = item.verdict?.toUpperCase() === 'CORRECT';
                  return (
                    <tr key={idx} className="hover:bg-neutral-800/20 transition-colors">
                      <td className="py-2.5 px-3.5 font-bold font-mono text-white">
                        {item.option}
                      </td>
                      <td className="py-2.5 px-3.5">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                          isCorrect 
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' 
                            : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        }`}>
                          {item.verdict}
                        </span>
                      </td>
                      <td className="py-2.5 px-3.5 text-neutral-300">
                        <MathText content={item.why_plausible} />
                      </td>
                      <td className="py-2.5 px-3.5 text-neutral-400 italic">
                        <MathText content={item.misconception} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Section 4: Prerequisites */}
      {anatomy.prerequisites && anatomy.prerequisites.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-xs font-semibold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
            <BookOpen className="w-3.5 h-3.5" /> 4. Prerequisite Concept Units
          </h4>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            {anatomy.prerequisites.map((prereq, idx) => (
              <button
                key={idx}
                onClick={() => setActiveConceptSlug(prereq.concept_slug)}
                className="p-3 rounded-xl bg-[#191a20] hover:bg-[#1f2129] border border-[#242730] hover:border-indigo-500/40 text-left transition-all flex items-center justify-between group"
              >
                <div>
                  <div className="text-xs font-semibold text-white group-hover:text-indigo-300 transition-colors">
                    {prereq.concept_label}
                  </div>
                  <div className="text-[10px] text-neutral-400 font-mono mt-0.5">
                    concept/{prereq.concept_slug}
                  </div>
                </div>
                <div className="p-1 rounded-lg bg-neutral-800 text-neutral-400 group-hover:text-white group-hover:bg-indigo-600 transition-all">
                  <ChevronRight className="w-3.5 h-3.5" />
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Section 5: Pattern Hint */}
      {anatomy.similarPatternHint && (
        <div className="space-y-1.5">
          <h4 className="text-xs font-semibold text-purple-400 uppercase tracking-wider flex items-center gap-1.5">
            <Compass className="w-3.5 h-3.5" /> 5. Pattern Recognition &amp; Problem Family
          </h4>
          <div className="p-3.5 rounded-xl bg-[#191a20] border border-[#23262f] text-xs text-neutral-300 leading-relaxed">
            <MathText content={anatomy.similarPatternHint} />
          </div>
        </div>
      )}

      {/* Concept Unit Modal */}
      <ConceptUnitDialog
        slug={activeConceptSlug}
        onClose={() => setActiveConceptSlug(null)}
      />
    </div>
  );
};
