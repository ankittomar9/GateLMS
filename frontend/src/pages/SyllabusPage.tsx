import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BookOpen, 
  Layers, 
  ChevronRight, 
  Plus, 
  Minus, 
  AlertTriangle, 
  ArrowUpRight,
  GraduationCap
} from 'lucide-react';
import { useSubjects, useTopics, useUpdateTopicMastery, useReadiness } from '../hooks/useQueries';
import { SubjectSummary, TopicItem } from '../api/types';

export const SyllabusPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: subjects, isLoading: subjectsLoading, error: subjectsError } = useSubjects();
  const { data: readinessData } = useReadiness();
  const readinessBySubject = new Map(readinessData?.perSubject?.map((s) => [s.subjectId, s.readiness]) || []);
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
        return 'text-ds-accent border-ds-accent/30 bg-ds-accent-soft';
      case 'TIER_2':
      case 'TIER 2':
        return 'text-ds-info border-ds-info/30 bg-ds-info/10';
      default:
        return 'text-ds-success border-ds-success/30 bg-ds-success/10';
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 relative overflow-hidden shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <GraduationCap className="w-5 h-5 text-ds-accent" />
              <h1 className="text-xl font-bold text-ds-primary tracking-tight">GATE CSE 2028 Syllabus Taxonomy</h1>
            </div>
            <p className="text-ds-secondary text-sm max-w-2xl">
              10 Official Subject Domains normalized to 100 Marks. Track topic-level retention, standard textbook chapters, and revision schedules.
            </p>
          </div>
          <div className="flex items-center gap-3 shrink-0">
            <button
              onClick={() => navigate('/questions')}
              className="flex items-center gap-2 px-4 py-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast font-semibold rounded-ds text-sm transition-all shadow-md"
            >
              <span>Practice Questions</span>
              <ArrowUpRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {conflictError && (
        <div className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger px-4 py-3 rounded-ds text-sm flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-ds-danger" />
            <span>{conflictError}</span>
          </div>
          <button
            onClick={() => setConflictError(null)}
            className="text-xs hover:text-ds-primary underline ml-4"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Main Grid: Subjects Rail & Topics Detail */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Subject Cards List */}
        <div className="lg:col-span-5 space-y-3">
          <h2 className="text-xs font-semibold text-ds-muted uppercase tracking-wider px-1">
            Subjects ({subjects?.length || 0})
          </h2>

          {subjectsLoading && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="h-20 bg-ds-surface border border-ds-border rounded-ds animate-pulse" />
              ))}
            </div>
          )}

          {subjectsError && (
            <div className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger p-4 rounded-ds text-sm">
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
                  className={`p-4 rounded-ds border cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-ds-surface-hover border-ds-accent shadow-md ring-1 ring-ds-accent/30'
                      : 'bg-ds-surface border-ds-border hover:border-ds-border-strong hover:bg-ds-surface-hover'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <div>
                      <span className="text-[10px] font-mono font-semibold text-ds-accent uppercase tracking-wider block">
                        {subject.code}
                      </span>
                      <h3 className="text-sm font-semibold text-ds-primary tracking-tight leading-snug">
                        {subject.name}
                      </h3>
                    </div>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-mono font-semibold border ${getTierColor(subject.priorityTier)}`}>
                      {subject.priorityTier}
                    </span>
                  </div>

                  <div className="flex items-center justify-between text-xs text-ds-secondary pt-2 border-t border-ds-border">
                    <div className="flex items-center gap-3">
                      <div className="flex items-center gap-1.5 font-mono text-ds-primary">
                        <span className="text-ds-accent font-bold">{subject.weightageAvg}</span>
                        <span className="text-ds-muted">Marks</span>
                      </div>
                      {readinessBySubject.has(subject.id) && (
                        <div className="flex items-center gap-1 font-mono text-xs text-ds-secondary">
                          <span className="text-ds-muted">Readiness:</span>
                          <span className="font-semibold text-ds-accent">
                            {Math.round(readinessBySubject.get(subject.id)!)}%
                          </span>
                        </div>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 text-ds-muted">
                      <Layers className="w-3.5 h-3.5 text-ds-muted" />
                      <span>{subject.topicCount} Topics</span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Right Column: Topics Table & Mastery Controller */}
        <div className="lg:col-span-7 bg-ds-surface border border-ds-border rounded-ds p-5 space-y-5 shadow-sm">
          {activeSubject ? (
            <>
              {/* Subject Header Card */}
              <div className="border-b border-ds-border pb-4">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-xs font-mono font-semibold text-ds-accent bg-ds-accent-soft px-2 py-0.5 rounded border border-ds-accent/30">
                    {activeSubject.code}
                  </span>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => navigate(`/questions?subjectId=${activeSubject.id}`)}
                      className="text-xs text-ds-accent hover:underline flex items-center gap-1 font-medium transition-colors"
                    >
                      <span>Filter Questions</span>
                      <ChevronRight className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
                <h2 className="text-lg font-bold text-ds-primary tracking-tight mb-1">
                  {activeSubject.name}
                </h2>
                <div className="flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-ds-secondary">
                  <div className="flex items-center gap-1">
                    <BookOpen className="w-3.5 h-3.5 text-ds-muted" />
                    <span>Primary Reference: <strong className="text-ds-primary">{activeSubject.primaryBookRef}</strong></span>
                  </div>
                  <div>
                    Weightage: <strong className="text-ds-accent font-mono">{activeSubject.weightageAvg}</strong> Marks
                  </div>
                  {readinessBySubject.has(activeSubject.id) && (
                    <div>
                      Readiness: <strong className="text-ds-accent font-mono">{Math.round(readinessBySubject.get(activeSubject.id)!)}%</strong>
                    </div>
                  )}
                </div>
              </div>

              {/* Topics List */}
              <div className="space-y-3">
                <div className="flex items-center justify-between text-xs text-ds-muted px-1 font-semibold uppercase tracking-wider">
                  <span>Topic & Chapter Reference</span>
                  <span>Mastery Level</span>
                </div>

                {topicsLoading && (
                  <div className="space-y-2 py-4">
                    {[...Array(4)].map((_, i) => (
                      <div key={i} className="h-14 bg-ds-surface-raised rounded-ds animate-pulse" />
                    ))}
                  </div>
                )}

                {!topicsLoading && topics?.length === 0 && (
                  <div className="text-center py-10 text-ds-muted text-sm">
                    No topics mapped for this subject yet.
                  </div>
                )}

                <div className="space-y-2 max-h-[580px] overflow-y-auto pr-1">
                  {topics?.map((topic: TopicItem) => {
                    const dotsCount = Math.round(topic.masteryLevel / 20); // 0-5 dots
                    return (
                      <div
                        key={topic.id}
                        className="bg-ds-surface-raised border border-ds-border rounded-ds p-3 hover:border-ds-border-strong transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <h4 className="text-sm font-medium text-ds-primary truncate mb-0.5">
                            {topic.name}
                          </h4>
                          <span className="text-[11px] text-ds-muted block truncate font-mono">
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
                                    ? 'bg-ds-accent shadow-sm'
                                    : 'bg-ds-border'
                                }`}
                              />
                            ))}
                          </div>

                          {/* Numeric % */}
                          <span className="text-xs font-mono font-semibold text-ds-secondary w-10 text-right">
                            {topic.masteryLevel}%
                          </span>

                          {/* Stepper Buttons */}
                          <div className="flex items-center bg-ds-surface rounded-ds border border-ds-border p-0.5">
                            <button
                              title="Decrease Mastery by 10%"
                              disabled={topic.masteryLevel <= 0 || updateMastery.isPending}
                              onClick={() => handleMasteryChange(topic, -10)}
                              className="p-1 text-ds-muted hover:text-ds-primary disabled:opacity-30 transition-colors"
                            >
                              <Minus className="w-3.5 h-3.5" />
                            </button>
                            <button
                              title="Increase Mastery by 10%"
                              disabled={topic.masteryLevel >= 100 || updateMastery.isPending}
                              onClick={() => handleMasteryChange(topic, 10)}
                              className="p-1 text-ds-muted hover:text-ds-primary disabled:opacity-30 transition-colors"
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
            <div className="text-center py-20 text-ds-muted">
              Select a subject to view mapped syllabus topics and mastery.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SyllabusPage;
