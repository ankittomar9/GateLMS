import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BookOpen, 
  Layers, 
  ChevronRight, 
  Plus, 
  Minus, 
  AlertTriangle, 
  Sparkles, 
  ArrowUpRight,
  GraduationCap
} from 'lucide-react';
import { useSubjects, useTopics, useUpdateTopicMastery } from '../hooks/useQueries';
import { SubjectSummary, TopicItem } from '../api/types';

export const SyllabusPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: subjects, isLoading: subjectsLoading, error: subjectsError } = useSubjects();
  const [selectedSubjectId, setSelectedSubjectId] = useState<string | null>(null);
  const [conflictError, setConflictError] = useState<string | null>(null);

  // Auto-select first subject once loaded if none selected
  const activeSubject = subjects?.find((s) => s.id === (selectedSubjectId || subjects[0]?.id));
  const effectiveSubjectId = activeSubject?.id;

  const { data: topics, isLoading: topicsLoading } = useTopics(effectiveSubjectId);
  const updateMastery = useUpdateTopicMastery();

  const handleMasteryChange = async (topic: TopicItem, delta: number) => {
    setConflictError(null);
    const newMastery = Math.max(0, Math.min(100, topic.masteryLevel + delta));
    if (newMastery === topic.masteryLevel) return;

    try {
      await updateMastery.mutateAsync({
        topicId: topic.id,
        update: {
          masteryLevel: newMastery,
          version: topic.version,
          lastRevisedAt: new Date().toISOString(),
        },
      });
    } catch (err: any) {
      if (err.status === 409) {
        setConflictError(`Conflict on "${topic.name}": Topic was updated elsewhere. Refreshing...`);
      } else {
        setConflictError(err.message || 'Failed to update topic mastery');
      }
    }
  };

  const getTierColor = (tier: string) => {
    switch (tier.toUpperCase()) {
      case 'TIER_1':
      case 'TIER 1':
        return 'text-amber-400 border-amber-500/20 bg-amber-500/10';
      case 'TIER_2':
      case 'TIER 2':
        return 'text-cyan-400 border-cyan-500/20 bg-cyan-500/10';
      default:
        return 'text-emerald-400 border-emerald-500/20 bg-emerald-500/10';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-[#161616] border border-[#262626] rounded-xl p-6 relative overflow-hidden">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <GraduationCap className="w-5 h-5 text-amber-400" />
              <h1 className="text-xl font-bold text-white tracking-tight">GATE CSE 2028 Syllabus Taxonomy</h1>
            </div>
            <p className="text-zinc-400 text-sm max-w-2xl">
              10 Official Subject Domains normalized to 100 Marks. Track topic-level retention, standard textbook chapters, and revision schedules.
            </p>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={() => navigate('/questions')}
              className="flex items-center gap-2 px-4 py-2 bg-amber-500 hover:bg-amber-400 text-black font-semibold rounded-lg text-sm transition-all shadow-md shadow-amber-500/20"
            >
              <span>Practice Questions</span>
              <ArrowUpRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {conflictError && (
        <div className="bg-rose-500/10 border border-rose-500/30 text-rose-300 px-4 py-3 rounded-lg text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-400" />
            <span>{conflictError}</span>
          </div>
          <button
            onClick={() => setConflictError(null)}
            className="text-xs hover:text-white underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Main Grid: Subjects Rail & Topics Detail */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Subject Cards List */}
        <div className="lg:col-span-5 space-y-3">
          <h2 className="text-xs font-semibold text-zinc-400 uppercase tracking-wider px-1">
            Subjects ({subjects?.length || 0})
          </h2>

          {subjectsLoading && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-20 bg-[#161616] border border-[#262626] rounded-xl animate-pulse" />
              ))}
            </div>
          )}

          {subjectsError && (
            <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-4 rounded-xl text-sm">
              Failed to load subjects. Please ensure the backend is running.
            </div>
          )}

          <div className="space-y-2.5 max-h-[720px] overflow-y-auto pr-1">
            {subjects?.map((subject: SubjectSummary) => {
              const isSelected = activeSubject?.id === subject.id;
              return (
                <div
                  key={subject.id}
                  onClick={() => setSelectedSubjectId(subject.id)}
                  className={`p-4 rounded-xl border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-[#1e1e1e] border-amber-500/50 shadow-lg shadow-black/40 ring-1 ring-amber-500/20'
                      : 'bg-[#161616] border-[#262626] hover:border-zinc-700 hover:bg-[#1a1a1a]'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div>
                      <span className="text-[10px] font-mono font-semibold text-amber-400 uppercase tracking-wider block">
                        {subject.code}
                      </span>
                      <h3 className="text-sm font-semibold text-white tracking-tight leading-snug">
                        {subject.name}
                      </h3>
                    </div>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold border ${getTierColor(subject.priorityTier)}`}>
                      {subject.priorityTier}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-xs text-zinc-400 pt-2 border-t border-[#262626]">
                    <div className="flex items-center gap-1.5 font-mono text-zinc-300">
                      <span className="text-amber-400 font-bold">{subject.weightageAvg}</span>
                      <span className="text-zinc-500">Marks</span>
                    </div>
                    <div className="flex items-center gap-1.5 text-zinc-400">
                      <Layers className="w-3.5 h-3.5 text-zinc-500" />
                      <span>{subject.topicCount} Topics</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Column: Topics Table & Mastery Controller */}
        <div className="lg:col-span-7 bg-[#161616] border border-[#262626] rounded-xl p-5 space-y-5">
          {activeSubject ? (
            <>
              {/* Subject Header Card */}
              <div className="border-b border-[#262626] pb-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-mono font-semibold text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">
                    {activeSubject.code}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => navigate(`/questions?subjectId=${activeSubject.id}`)}
                      className="text-xs text-amber-400 hover:text-amber-300 flex items-center gap-1 font-medium transition-colors"
                    >
                      <span>Filter Questions</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
                <h2 className="text-lg font-bold text-white tracking-tight mb-1">
                  {activeSubject.name}
                </h2>
                <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-zinc-400">
                  <div className="flex items-center gap-1">
                    <BookOpen className="w-3.5 h-3.5 text-zinc-500" />
                    <span>Primary Reference: <strong className="text-zinc-300">{activeSubject.primaryBookRef}</strong></span>
                  </div>
                  <div>
                    Weightage: <strong className="text-amber-400 font-mono">{activeSubject.weightageAvg}</strong> Marks
                  </div>
                </div>
              </div>

              {/* Topics List */}
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs text-zinc-400 px-1 font-semibold uppercase tracking-wider">
                  <span>Topic & Chapter Reference</span>
                  <span>Mastery Level</span>
                </div>

                {topicsLoading && (
                  <div className="space-y-2 py-4">
                    {[...Array(4)].map((_, i) => (
                      <div key={i} className="h-14 bg-[#1f1f1f] rounded-lg animate-pulse" />
                    ))}
                  </div>
                )}

                {!topicsLoading && topics?.length === 0 && (
                  <div className="text-center py-10 text-zinc-500 text-sm">
                    No topics mapped for this subject yet.
                  </div>
                )}

                <div className="space-y-2 max-h-[580px] overflow-y-auto pr-1">
                  {topics?.map((topic: TopicItem) => {
                    const dotsCount = Math.round(topic.masteryLevel / 20); // 0-5 dots
                    return (
                      <div
                        key={topic.id}
                        className="bg-[#121212] border border-[#262626] rounded-lg p-3 hover:border-zinc-700 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <h4 className="text-sm font-medium text-white truncate mb-0.5">
                            {topic.name}
                          </h4>
                          <span className="text-[11px] text-zinc-500 block truncate font-mono">
                            {topic.bookChapterRef || 'Standard Syllabus Mapping'}
                          </span>
                        </div>

                        {/* Mastery Stepper + Dots */}
                        <div className="flex items-center gap-3 shrink-0 self-end sm:self-center">
                          {/* 0-5 dots */}
                          <div className="flex items-center gap-1">
                            {[1, 2, 3, 4, 5].map((dot) => (
                              <div
                                key={dot}
                                className={`w-2 h-2 rounded-full transition-colors ${
                                  dot <= dotsCount
                                    ? 'bg-amber-400 shadow-sm shadow-amber-400/50'
                                    : 'bg-zinc-700'
                                }`}
                              />
                            ))}
                          </div>

                          {/* Numeric % */}
                          <span className="text-xs font-mono font-semibold text-zinc-300 w-10 text-right">
                            {topic.masteryLevel}%
                          </span>

                          {/* Stepper Buttons */}
                          <div className="flex items-center bg-[#1a1a1a] rounded-lg border border-[#2a2a2a] p-0.5">
                            <button
                              title="Decrease Mastery by 10%"
                              disabled={topic.masteryLevel <= 0 || updateMastery.isPending}
                              onClick={() => handleMasteryChange(topic, -10)}
                              className="p-1 text-zinc-400 hover:text-white disabled:opacity-30 disabled:hover:text-zinc-400 transition-colors"
                            >
                              <Minus className="w-3.5 h-3.5" />
                            </button>
                            <button
                              title="Increase Mastery by 10%"
                              disabled={topic.masteryLevel >= 100 || updateMastery.isPending}
                              onClick={() => handleMasteryChange(topic, 10)}
                              className="p-1 text-zinc-400 hover:text-white disabled:opacity-30 disabled:hover:text-zinc-400 transition-colors"
                            >
                              <Plus className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          ) : (
            <div className="text-center py-20 text-zinc-500">
              Select a subject to view mapped syllabus topics and mastery.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SyllabusPage;
