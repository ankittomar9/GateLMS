import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  ArrowRight, 
  Binary, 
  Cpu, 
  BookMarked, 
  Network, 
  Sparkles, 
  ExternalLink,
  BookOpen,
  CheckCircle2,
  Quote
} from 'lucide-react';
import { useDesign } from '../../context/DesignContext';
import { CurationProgressTile } from './CurationProgressTile';
import { ReadinessSummaryCard } from './ReadinessSummaryCard';
import { PypDashboardTile } from './PypDashboardTile';
import { TodayCard } from '../planner/TodayCard';

export const MinimalistDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { currentDesign } = useDesign();

  const isInk = currentDesign === 'ink';

  const subjectClusters = [
    {
      title: 'Discrete Math & Logic',
      weightage: '14-15 Marks',
      topicsCount: 6,
      estHours: 45,
      textbook: 'Kenneth Rosen §1–§10',
      icon: <Binary className="w-5 h-5 text-ds-accent" />,
      query: 'discrete',
    },
    {
      title: 'Programming & Data Structures',
      weightage: '10-12 Marks',
      topicsCount: 5,
      estHours: 35,
      textbook: 'Kernighan & Ritchie (K&R)',
      icon: <Cpu className="w-5 h-5 text-ds-info" />,
      query: 'pointer',
    },
    {
      title: 'Algorithms Core',
      weightage: '8-10 Marks',
      topicsCount: 4,
      estHours: 40,
      textbook: 'CLRS Chapters 3–24',
      icon: <BookMarked className="w-5 h-5 text-ds-success" />,
      query: 'algorithm',
    },
    {
      title: 'Systems Core (OS + DBMS + CN)',
      weightage: '24-27 Marks',
      topicsCount: 12,
      estHours: 85,
      textbook: 'Silberschatz, Galvin & Kurose',
      icon: <Network className="w-5 h-5 text-ds-accent" />,
      query: 'operating',
    },
  ];

  return (
    <div className="space-y-8" data-testid="minimalist-dashboard">
      {/* Ink Layout: Two-column layout if isInk, standard if not */}
      <div className={isInk ? 'grid grid-cols-1 lg:grid-cols-12 gap-6' : 'space-y-8'}>
        {/* Main Column */}
        <div className={isInk ? 'lg:col-span-8 space-y-6' : 'space-y-8'}>
          <ReadinessSummaryCard />
          <TodayCard />

          {/* Active Study Session Hero */}
          <div className="relative overflow-hidden rounded-ds bg-ds-surface border border-ds-border p-7 shadow-sm">
            <div className="relative z-10 flex flex-col justify-between gap-5">
              <div className="space-y-3">
                <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-ds-accent-soft border border-ds-accent/30 text-[11px] font-semibold text-ds-accent uppercase tracking-wider">
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>Active Topic · Operating Systems</span>
                </div>

                <div>
                  <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-ds-primary leading-snug">
                    Process Synchronization & IPC
                  </h1>
                  <p className="text-xs text-ds-secondary mt-1.5 leading-relaxed">
                    Critical sections, Peterson's algorithm, hardware instructions (Test-and-Set), semaphores, and classical synchronization problems.
                  </p>
                </div>

                {/* Syllabus Chapter References */}
                <div className="flex flex-wrap items-center gap-2.5 pt-1 text-xs">
                  <span className="flex items-center gap-1.5 text-ds-primary bg-ds-surface-raised px-2.5 py-1 rounded-ds border border-ds-border font-mono text-[11px]">
                    <BookOpen className="w-3.5 h-3.5 text-ds-accent" />
                    Silberschatz §5 (OS Concepts)
                  </span>
                  <span className="flex items-center gap-1.5 text-ds-secondary bg-ds-surface-raised px-2.5 py-1 rounded-ds border border-ds-border font-mono text-[11px]">
                    <BookOpen className="w-3.5 h-3.5 text-ds-muted" />
                    Tanenbaum §2.3 (Modern OS)
                  </span>
                </div>

                {/* Linear Progress Bar */}
                <div className="space-y-1.5 pt-2">
                  <div className="flex items-center justify-between text-xs font-mono">
                    <span className="text-ds-muted uppercase tracking-wider text-[11px]">
                      {isInk ? 'Chapter Mastery' : 'Mastery Progress'}
                    </span>
                    <span className="text-ds-accent font-bold">
                      {isInk ? '66% complete' : '68% Complete'}
                    </span>
                  </div>
                  <div className="w-full h-2 bg-ds-surface-raised rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-ds-accent rounded-full transition-all duration-500" 
                      style={{ width: isInk ? '66%' : '68%' }} 
                    />
                  </div>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-ds-border">
                <button
                  type="button"
                  onClick={() => navigate('/questions?q=synchronization')}
                  className="flex items-center gap-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-5 py-2 rounded-ds font-bold text-xs tracking-tight transition-all shadow-sm"
                >
                  <span>Resume Study Session</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
                <button
                  type="button"
                  onClick={() => navigate('/syllabus')}
                  className="flex items-center gap-1.5 text-xs text-ds-muted hover:text-ds-primary font-medium transition-colors"
                >
                  <span>View Full OS Taxonomy</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          </div>

          {/* Subject Cluster Grid */}
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-bold text-ds-primary tracking-wider uppercase flex items-center gap-2">
                <span>High-Yield Subject Clusters</span>
                <span className="text-[11px] text-ds-muted font-mono normal-case">(GATE CSE Mark Weightage)</span>
              </h2>
              <button 
                type="button"
                onClick={() => navigate('/syllabus')}
                className="text-xs text-ds-accent hover:underline font-semibold flex items-center gap-1 transition-colors"
              >
                All 10 Subjects <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className={isInk ? 'grid grid-cols-1 sm:grid-cols-2 gap-4' : 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4'}>
              {subjectClusters.map((sub, idx) => (
                <div
                  key={idx}
                  className="bg-ds-surface hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong rounded-ds p-5 flex flex-col justify-between transition-all duration-200 group shadow-sm"
                >
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="w-9 h-9 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center justify-center">
                        {sub.icon}
                      </div>
                      <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full border border-ds-accent/30 bg-ds-accent-soft text-ds-accent">
                        {sub.weightage}
                      </span>
                    </div>

                    <div>
                      <h3 className="font-bold text-sm text-ds-primary leading-snug">
                        {sub.title}
                      </h3>
                      <p className="text-[11px] text-ds-muted mt-1 line-clamp-1 font-mono">
                        {sub.textbook}
                      </p>
                    </div>
                  </div>

                  <div className="pt-3 border-t border-ds-border mt-4 flex items-center justify-between">
                    <div className="text-[11px] text-ds-muted font-mono">
                      {sub.topicsCount} topics · {sub.estHours} hrs
                    </div>
                    <button
                      type="button"
                      onClick={() => navigate(`/questions?q=${sub.query}`)}
                      className="text-xs font-semibold text-ds-secondary group-hover:text-ds-accent flex items-center gap-1 transition-colors"
                    >
                      Explore <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Ink Secondary Inline Column (≈35%) */}
        {isInk && (
          <div className="lg:col-span-4 space-y-6" data-testid="ink-inline-column">
            {/* Circular Syllabus Ring in Ink */}
            <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold uppercase tracking-wider text-ds-muted font-mono">
                  Syllabus Progress
                </span>
                <span className="text-[11px] font-mono text-ds-success bg-ds-success/10 px-2 py-0.5 rounded-full border border-ds-success/20">
                  On Track
                </span>
              </div>

              <div className="flex flex-col items-center justify-center py-2">
                <div className="relative w-32 h-32 flex items-center justify-center">
                  <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                    <circle
                      cx="50"
                      cy="50"
                      r="38"
                      className="stroke-ds-surface-raised"
                      strokeWidth="8"
                      fill="transparent"
                    />
                    <circle
                      cx="50"
                      cy="50"
                      r="38"
                      className="stroke-ds-success transition-all duration-700 ease-out"
                      strokeWidth="8"
                      strokeDasharray="238.7"
                      strokeDashoffset="199"
                      strokeLinecap="round"
                      fill="transparent"
                    />
                  </svg>
                  <div className="absolute flex flex-col items-center text-center">
                    <span className="text-xl font-black text-ds-primary font-mono">16.5%</span>
                    <span className="text-[10px] text-ds-muted font-mono">16.5 / 100 M</span>
                  </div>
                </div>
              </div>

              <div className="space-y-1.5 text-xs pt-1 border-t border-ds-border">
                <div className="flex items-center justify-between">
                  <span className="text-ds-secondary">Foundation (GA+EM)</span>
                  <span className="font-mono text-ds-primary">9.0 / 29</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-ds-secondary">Core Systems</span>
                  <span className="font-mono text-ds-primary">5.5 / 42</span>
                </div>
              </div>
            </div>

            {/* Problem of the Day Inline Tile */}
            <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold uppercase tracking-wider text-ds-muted font-mono">
                  Problem of the Day
                </span>
                <span className="text-[10px] font-mono text-ds-accent">Daily Drill</span>
              </div>
              <p className="text-xs text-ds-secondary leading-relaxed">
                An instruction pipeline has 5 stages with execution times 2ns, 3ns, 5ns, 4ns, and 2ns. Calculate speedup for 1000 tasks.
              </p>
              <button
                type="button"
                onClick={() => navigate('/questions')}
                className="w-full bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast py-1.5 rounded-ds text-xs font-bold transition-all text-center"
              >
                Solve Problem
              </button>
            </div>

            {/* Curation Queue Inline Tile */}
            <CurationProgressTile />

            {/* PYP Paper Tile */}
            <PypDashboardTile />
          </div>
        )}
      </div>

      {/* Ink Bottom Quote Banner */}
      {isInk && (
        <div className="bg-ds-surface border border-ds-border rounded-ds p-5 flex items-center gap-4 text-xs shadow-sm" data-testid="ink-quote-banner">
          <div className="w-9 h-9 rounded-ds bg-ds-accent-soft text-ds-accent flex items-center justify-center shrink-0">
            <Quote className="w-4 h-4" />
          </div>
          <div>
            <p className="text-ds-primary font-medium italic">
              "An algorithm must be seen to be believed, and textbook rigor always outlasts shortcut memorization."
            </p>
            <span className="text-[11px] text-ds-muted font-mono block mt-0.5">
              — Donald E. Knuth, The Art of Computer Programming
            </span>
          </div>
        </div>
      )}

      {/* Non-Ink: Essential Tools Cards */}
      {!isInk && (
        <div className="space-y-4">
          <h2 className="text-xs font-bold text-ds-primary tracking-wider uppercase">
            Essential GATE Preparation Tools
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-ds-surface border border-ds-border rounded-ds p-5 flex items-start justify-between gap-4">
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-ds bg-ds-accent-soft border border-ds-accent/30 flex items-center justify-center text-ds-accent">
                    <BookMarked className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-ds-primary">GATE Overflow PYQ Vault</h3>
                    <div className="text-[11px] text-ds-muted font-mono">Two-Pass Mastery Tracking</div>
                  </div>
                </div>
                <p className="text-xs text-ds-secondary leading-relaxed">
                  Pass 1 foundation reviews with full solution derivations, followed by timed Pass 2 evaluations.
                </p>
              </div>
              <button
                type="button"
                onClick={() => navigate('/questions')}
                className="shrink-0 self-center bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-primary border border-ds-border px-3.5 py-2 rounded-ds text-xs font-semibold transition-all"
              >
                Launch Vault →
              </button>
            </div>

            <div className="bg-ds-surface border border-ds-border rounded-ds p-5 flex items-start justify-between gap-4">
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-ds bg-ds-success/15 border border-ds-success/30 flex items-center justify-center text-ds-success">
                    <CheckCircle2 className="w-4 h-4" />
                  </div>
                  <div>
                    <h3 className="font-bold text-sm text-ds-primary">TCS iON Mock Exam Simulator</h3>
                    <div className="text-[11px] text-ds-muted font-mono">Real Test Center Environment</div>
                  </div>
                </div>
                <p className="text-xs text-ds-secondary leading-relaxed">
                  65 Questions, 180 Minutes, 100 Marks with official Virtual Calculator and TCS iON color palette.
                </p>
              </div>
              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="shrink-0 self-center bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-primary border border-ds-border px-3.5 py-2 rounded-ds text-xs font-semibold transition-all"
              >
                Explore Mocks →
              </button>
            </div>

            <CurationProgressTile />
            <PypDashboardTile />
          </div>
        </div>
      )}
    </div>
  );
};
