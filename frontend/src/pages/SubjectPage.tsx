import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  ArrowLeft, 
  CheckCircle2, 
  Circle, 
  BookOpen, 
  Award, 
  ArrowRight,
  Sparkles,
  BookMarked
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { JourneyTopicSummary } from '../types/journey';

export const SubjectPage: React.FC = () => {
  const { subjectId } = useParams<{ subjectId: string }>();
  const navigate = useNavigate();
  const [topics, setTopics] = useState<JourneyTopicSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadTopics() {
      if (!subjectId) return;
      try {
        setLoading(true);
        setError(null);
        const data = await journeyApi.getSubjectTopics(subjectId);
        if (isMounted) {
          setTopics(data);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err?.message || 'Failed to load subject roadmap');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }
    loadTopics();
    return () => {
      isMounted = false;
    };
  }, [subjectId]);

  if (loading) {
    return (
      <div className="p-8 space-y-6 max-w-5xl mx-auto" data-testid="subject-page-loading">
        <div className="h-16 bg-ds-surface animate-pulse rounded-ds border border-ds-border" />
        <div className="space-y-3">
          {[1, 2, 3, 4, 5].map((i) => (
            <div key={i} className="h-20 bg-ds-surface animate-pulse rounded-ds border border-ds-border" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 max-w-5xl mx-auto text-center space-y-4">
        <p className="text-ds-danger font-medium">{error}</p>
        <button
          onClick={() => navigate('/')}
          className="px-4 py-2 bg-ds-surface-raised border border-ds-border text-ds-primary rounded-ds text-sm font-semibold hover:bg-ds-surface"
        >
          Back to Learning Journey
        </button>
      </div>
    );
  }

  const totalQuestions = topics.reduce((acc, t) => acc + t.questionsVisible, 0);
  const solvedQuestions = topics.reduce((acc, t) => acc + t.questionsSolved, 0);
  const completedTopics = topics.filter((t) => t.completed).length;

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-5xl mx-auto" data-testid="subject-page">
      {/* Top Navigation */}
      <div className="flex items-center justify-between border-b border-ds-border pb-4">
        <button
          onClick={() => navigate('/')}
          className="flex items-center gap-1.5 text-xs font-mono font-semibold text-ds-secondary hover:text-ds-primary transition-colors"
          data-testid="back-to-journey-btn"
        >
          <ArrowLeft className="w-3.5 h-3.5" />
          <span>All Subjects</span>
        </button>

        <button
          onClick={() => navigate(`/subjects/${subjectId}/notebook`)}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-ds-surface hover:bg-ds-surface-hover border border-ds-border rounded-ds text-xs font-semibold text-ds-primary transition-all"
          data-testid="subject-notebook-btn"
        >
          <BookMarked className="w-3.5 h-3.5 text-ds-warning" />
          <span>Subject Solved Notebook</span>
        </button>
      </div>

      {/* Header Summary */}
      <div className="space-y-3">
        <div className="flex items-center gap-2">
          <span className="text-xs font-mono px-2 py-0.5 rounded bg-ds-accent/10 border border-ds-accent/20 text-ds-accent font-bold uppercase">
            Curated Roadmap
          </span>
          <span className="text-xs font-mono text-ds-muted">
            Ordered by GATE Syllabus Sequence
          </span>
        </div>
        <h1 className="text-2xl font-bold tracking-tight text-ds-primary">
          Subject Roadmap: {subjectId?.toUpperCase()}
        </h1>
        <div className="flex items-center gap-6 text-xs font-mono text-ds-muted">
          <div>
            <span className="text-ds-primary font-bold">{topics.length}</span> Topics
          </div>
          <div>
            <span className="text-ds-primary font-bold">{solvedQuestions}</span> / {totalQuestions} Solved
          </div>
          <div>
            <span className="text-ds-success font-bold">{completedTopics}</span> Completed
          </div>
        </div>
      </div>

      {/* Topics Roadmap List */}
      <div className="space-y-3" data-testid="topics-roadmap-list">
        {topics.map((t) => {
          const ratio = t.questionsVisible > 0 ? (t.questionsSolved / t.questionsVisible) * 100 : 0;
          return (
            <div
              key={t.topicId}
              onClick={() => navigate(`/topics/${t.topicId}`)}
              className="bg-ds-surface hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong rounded-ds p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition-all duration-150 cursor-pointer group shadow-sm"
              data-testid={`topic-row-${t.topicId}`}
            >
              {/* Left Side: Sequence + Title */}
              <div className="flex items-start sm:items-center gap-3.5">
                <div className="w-8 h-8 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center justify-center text-xs font-mono font-bold text-ds-muted shrink-0 group-hover:text-ds-accent group-hover:border-ds-accent/30 transition-colors">
                  #{t.sequence}
                </div>

                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-sm text-ds-primary group-hover:text-ds-accent transition-colors">
                      {t.name}
                    </h3>
                    {t.completed && (
                      <span className="text-[10px] font-mono font-bold px-1.5 py-0.2 rounded bg-ds-success/15 text-ds-success border border-ds-success/30 flex items-center gap-1">
                        <CheckCircle2 className="w-2.5 h-2.5" />
                        Done
                      </span>
                    )}
                  </div>
                  <div className="flex items-center gap-3 text-xs font-mono text-ds-muted">
                    <span>{t.questionsSolved} / {t.questionsVisible} solved</span>
                    {t.primerStatus && (
                      <span className="flex items-center gap-1 text-[11px] text-ds-accent">
                        <BookOpen className="w-3 h-3" />
                        Theory {t.primerStatus}
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* Right Side: Progress Bar + Mastery Chip */}
              <div className="flex items-center gap-4 self-end sm:self-center">
                <div className="w-24 sm:w-32 hidden sm:block">
                  <div className="w-full bg-ds-surface-raised h-1.5 rounded-full overflow-hidden border border-ds-border/50">
                    <div 
                      className="bg-ds-accent h-full transition-all duration-300"
                      style={{ width: `${Math.min(100, ratio)}%` }}
                    />
                  </div>
                </div>

                <div className="flex items-center gap-1.5 px-2.5 py-1 rounded bg-ds-surface-raised border border-ds-border text-xs font-mono font-semibold text-ds-secondary">
                  <Award className="w-3.5 h-3.5 text-ds-warning" />
                  <span>M{t.masteryLevel}/5</span>
                </div>

                <div className="text-ds-muted group-hover:text-ds-primary transition-colors">
                  <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default SubjectPage;
