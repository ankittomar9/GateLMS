import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Compass, 
  ArrowRight, 
  BookOpen, 
  CheckCircle2, 
  Sparkles, 
  Layers, 
  PlayCircle,
  Award
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { JourneyOverviewSubject, JourneyContinue } from '../types/journey';

const CLUSTER_CONFIG: Record<string, { title: string; badge?: string; desc: string }> = {
  MATH_APTITUDE: {
    title: 'Foundation & Engineering Mathematics',
    badge: 'Recommended Start',
    desc: 'Discrete mathematics, linear algebra, probability, and verbal/numerical aptitude.'
  },
  CONCEPTUAL_CORE: {
    title: 'Conceptual & Algorithmic Core',
    desc: 'Data structures, algorithmic analysis, and theory of computation.'
  },
  SYSTEMS_CORE: {
    title: 'Systems & Architecture Core',
    desc: 'Operating systems, relational databases, computer networks, and processor architecture.'
  }
};

export const JourneyHomePage: React.FC = () => {
  const navigate = useNavigate();
  const [subjects, setSubjects] = useState<JourneyOverviewSubject[]>([]);
  const [continueData, setContinueData] = useState<JourneyContinue | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;
    async function loadData() {
      try {
        setLoading(true);
        setError(null);
        const [overviewRes, continueRes] = await Promise.all([
          journeyApi.getOverview(),
          journeyApi.getContinue().catch(() => null)
        ]);
        if (isMounted) {
          setSubjects(overviewRes);
          setContinueData(continueRes);
        }
      } catch (err: any) {
        if (isMounted) {
          setError(err?.message || 'Failed to load journey overview');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }
    loadData();
    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div className="p-8 space-y-6 max-w-7xl mx-auto" data-testid="journey-home-loading">
        <div className="h-20 bg-ds-surface animate-pulse rounded-ds border border-ds-border" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="h-48 bg-ds-surface animate-pulse rounded-ds border border-ds-border" />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 max-w-7xl mx-auto text-center space-y-4">
        <p className="text-ds-danger font-medium">{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="px-4 py-2 bg-ds-surface-raised border border-ds-border text-ds-primary rounded-ds text-sm font-semibold hover:bg-ds-surface"
        >
          Retry
        </button>
      </div>
    );
  }

  // Group subjects by cluster
  const clusterGroups = subjects.reduce((acc, sub) => {
    const key = sub.cluster || 'OTHER';
    if (!acc[key]) acc[key] = [];
    acc[key].push(sub);
    return acc;
  }, {} as Record<string, JourneyOverviewSubject[]>);

  // Cluster ordering
  const clusterOrder = ['MATH_APTITUDE', 'CONCEPTUAL_CORE', 'SYSTEMS_CORE'];
  const allClusterKeys = Array.from(new Set([...clusterOrder, ...Object.keys(clusterGroups)]));

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-7xl mx-auto" data-testid="journey-home-page">
      {/* Header Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-ds-border pb-6">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-ds bg-ds-accent/10 border border-ds-accent/20 flex items-center justify-center text-ds-accent">
              <Compass className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-ds-primary">
                GATE CSE Learning Journey
              </h1>
              <p className="text-xs text-ds-muted font-mono mt-0.5">
                10 Core Subjects + General Aptitude · Zero Sequential Locks · Theory-First Mastery
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-4 text-xs font-mono text-ds-secondary">
          <div className="flex items-center gap-1.5">
            <CheckCircle2 className="w-4 h-4 text-ds-success" />
            <span>Pass 1 Foundation & Ticks</span>
          </div>
          <div className="flex items-center gap-1.5">
            <Award className="w-4 h-4 text-ds-warning" />
            <span>0-5 Topic Mastery</span>
          </div>
        </div>
      </div>

      {/* Continue Where You Left Off Strip */}
      {continueData && continueData.questionId && (
        <div 
          className="bg-ds-surface-raised border border-ds-border-strong rounded-ds p-4 flex items-center justify-between gap-4 shadow-sm"
          data-testid="continue-learning-strip"
        >
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-ds-accent text-ds-accent-contrast flex items-center justify-center shrink-0">
              <PlayCircle className="w-4 h-4" />
            </div>
            <div>
              <div className="text-[11px] font-mono font-semibold text-ds-accent uppercase tracking-wider">
                Continue Where You Left Off
              </div>
              <div className="text-sm font-bold text-ds-primary">
                Resume practice session from your most recent solve
              </div>
            </div>
          </div>
          <button
            onClick={() => {
              if (continueData.questionId) {
                navigate(`/questions/${continueData.questionId}`);
              }
            }}
            className="px-4 py-2 bg-ds-accent text-ds-accent-contrast rounded-ds text-xs font-bold flex items-center gap-1.5 hover:opacity-90 transition-opacity"
          >
            <span>Resume</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Subject Clusters Grid */}
      <div className="space-y-10">
        {allClusterKeys.map((clusterKey) => {
          const list = clusterGroups[clusterKey];
          if (!list || list.length === 0) return null;

          const config = CLUSTER_CONFIG[clusterKey] || {
            title: clusterKey.replace('_', ' '),
            desc: 'Curated topics and exam problems'
          };

          return (
            <div key={clusterKey} className="space-y-4" data-testid={`cluster-${clusterKey}`}>
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2.5">
                    <h2 className="text-base font-bold text-ds-primary tracking-wide">
                      {config.title}
                    </h2>
                    {config.badge && (
                      <span 
                        className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-ds-accent/15 border border-ds-accent/30 text-ds-accent flex items-center gap-1"
                        data-testid="recommended-start-badge"
                      >
                        <Sparkles className="w-3 h-3" />
                        {config.badge}
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-ds-muted mt-0.5">
                    {config.desc}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {list.map((sub) => {
                  const pct = Math.round(sub.readinessPct || 0);
                  const solvedRatio = sub.questionsVisible > 0 
                    ? Math.round((sub.questionsSolved / sub.questionsVisible) * 100) 
                    : 0;

                  return (
                    <div
                      key={sub.subjectId}
                      onClick={() => navigate(`/subjects/${sub.subjectId}`)}
                      className="bg-ds-surface hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong rounded-ds p-5 flex flex-col justify-between transition-all duration-200 cursor-pointer group shadow-sm hover:shadow"
                      data-testid={`subject-card-${sub.subjectId}`}
                    >
                      <div className="space-y-3">
                        <div className="flex items-start justify-between gap-2">
                          <h3 className="font-bold text-base text-ds-primary group-hover:text-ds-accent transition-colors leading-snug">
                            {sub.name}
                          </h3>
                          <span className="text-[11px] font-mono font-bold px-2 py-0.5 rounded bg-ds-surface-raised border border-ds-border text-ds-secondary shrink-0">
                            ~{sub.marksWeight} Marks
                          </span>
                        </div>

                        {sub.standardBookRef && (
                          <div className="flex items-center gap-1.5 text-xs text-ds-muted line-clamp-1">
                            <BookOpen className="w-3.5 h-3.5 shrink-0" />
                            <span className="font-mono text-[11px] truncate">{sub.standardBookRef}</span>
                          </div>
                        )}
                      </div>

                      {/* Progress Stats */}
                      <div className="pt-4 border-t border-ds-border mt-4 space-y-2.5">
                        <div className="flex items-center justify-between text-xs font-mono">
                          <span className="text-ds-muted">Questions Solved</span>
                          <span className="text-ds-primary font-semibold">
                            {sub.questionsSolved} <span className="text-ds-muted">/ {sub.questionsVisible}</span>
                          </span>
                        </div>

                        {/* Progress Bar */}
                        <div className="w-full bg-ds-surface-raised h-1.5 rounded-full overflow-hidden border border-ds-border/50">
                          <div 
                            className="bg-ds-accent h-full transition-all duration-300"
                            style={{ width: `${Math.min(100, solvedRatio)}%` }}
                          />
                        </div>

                        <div className="flex items-center justify-between pt-1 text-[11px] font-mono text-ds-muted">
                          <div className="flex items-center gap-1">
                            <Layers className="w-3 h-3" />
                            <span>{sub.topicsCompleted}/{sub.topicsTotal} topics</span>
                          </div>
                          <div className="flex items-center gap-1 text-ds-secondary group-hover:text-ds-accent font-semibold transition-colors">
                            <span>Open Roadmap</span>
                            <ArrowRight className="w-3 h-3 transition-transform group-hover:translate-x-0.5" />
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default JourneyHomePage;
