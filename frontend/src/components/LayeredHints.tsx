import React, { useState } from 'react';
import { Lightbulb, ChevronDown, BookOpen, Key, HelpCircle } from 'lucide-react';
import { QuestionAnatomyDto } from '../api/anatomyApi';
import { MathText } from './MathText';

interface LayeredHintsProps {
  anatomy: QuestionAnatomyDto | null;
  mode?: 'study' | 'test' | 'mock';
  onPrerequisiteClick?: (slug: string) => void;
}

export const LayeredHints: React.FC<LayeredHintsProps> = ({
  anatomy,
  mode = 'study',
  onPrerequisiteClick,
}) => {
  // Hard-hidden in mock/test modes or if anatomy is incomplete/errored (Binding spec §5.4)
  if (
    mode !== 'study' ||
    !anatomy ||
    anatomy.testedConcept === 'Generation parsing incomplete' ||
    Boolean(anatomy.parseError)
  ) {
    return null;
  }

  const [revealedLevel, setRevealedLevel] = useState<number>(0);

  const l1Content = anatomy.testedConcept;
  const l2Content = anatomy.invariantRule;
  const firstPrereq = anatomy.prerequisites && anatomy.prerequisites.length > 0
    ? anatomy.prerequisites[0]
    : null;

  return (
    <div className="rounded-xl border border-amber-500/20 bg-[#16171b] p-4 text-neutral-200">
      <div className="flex items-center justify-between pb-3 border-b border-[#242730]">
        <div className="flex items-center gap-2">
          <div className="p-1.5 rounded-lg bg-amber-500/10 text-amber-400">
            <Lightbulb className="w-4 h-4" />
          </div>
          <span className="text-xs font-semibold uppercase tracking-wider text-amber-400">
            Progressive Hints (Study Mode)
          </span>
        </div>
        <span className="text-[11px] text-neutral-400">
          {revealedLevel} of 3 revealed
        </span>
      </div>

      <div className="mt-3 space-y-3">
        {/* Level 1: Tested Concept */}
        <div className="rounded-lg bg-[#1a1c22] p-3 border border-[#242730]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-medium text-neutral-300">
              <HelpCircle className="w-3.5 h-3.5 text-sky-400" />
              <span>Hint 1: What this question tests</span>
            </div>
            {revealedLevel < 1 && (
              <button
                onClick={() => setRevealedLevel(1)}
                className="text-xs font-medium px-2.5 py-1 rounded-md bg-neutral-800 hover:bg-neutral-700 text-sky-300 transition-colors"
              >
                Reveal Hint 1
              </button>
            )}
          </div>
          {revealedLevel >= 1 && (
            <div className="mt-2 text-xs text-neutral-300 pl-5 border-l-2 border-sky-500/40">
              <MathText content={l1Content} />
            </div>
          )}
        </div>

        {/* Level 2: Invariant Rule */}
        <div className="rounded-lg bg-[#1a1c22] p-3 border border-[#242730]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-medium text-neutral-300">
              <Key className="w-3.5 h-3.5 text-amber-400" />
              <span>Hint 2: Core Invariant / Law</span>
            </div>
            {revealedLevel < 2 && (
              <button
                onClick={() => setRevealedLevel(2)}
                disabled={revealedLevel < 1}
                className="text-xs font-medium px-2.5 py-1 rounded-md bg-neutral-800 hover:bg-neutral-700 text-amber-300 disabled:opacity-40 transition-colors"
              >
                Reveal Hint 2
              </button>
            )}
          </div>
          {revealedLevel >= 2 && (
            <div className="mt-2 text-xs text-amber-200/90 pl-5 border-l-2 border-amber-500/40 font-mono">
              <MathText content={l2Content || 'Ground question on fundamental definition'} />
            </div>
          )}
        </div>

        {/* Level 3: Prerequisite Concept */}
        <div className="rounded-lg bg-[#1a1c22] p-3 border border-[#242730]">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-xs font-medium text-neutral-300">
              <BookOpen className="w-3.5 h-3.5 text-emerald-400" />
              <span>Hint 3: Prerequisite to read</span>
            </div>
            {revealedLevel < 3 && (
              <button
                onClick={() => setRevealedLevel(3)}
                disabled={revealedLevel < 2}
                className="text-xs font-medium px-2.5 py-1 rounded-md bg-neutral-800 hover:bg-neutral-700 text-emerald-300 disabled:opacity-40 transition-colors"
              >
                Reveal Hint 3
              </button>
            )}
          </div>
          {revealedLevel >= 3 && (
            <div className="mt-2 text-xs text-neutral-300 pl-5 border-l-2 border-emerald-500/40 flex items-center justify-between">
              <span>
                Recommended Concept:{' '}
                <strong className="text-white">
                  {firstPrereq ? firstPrereq.concept_label : 'Foundational Syllabus Review'}
                </strong>
              </span>
              {firstPrereq && onPrerequisiteClick && (
                <button
                  onClick={() => onPrerequisiteClick(firstPrereq.concept_slug)}
                  className="text-[11px] underline text-indigo-400 hover:text-indigo-300"
                >
                  View Concept Unit &rarr;
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
