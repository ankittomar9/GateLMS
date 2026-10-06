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
  CheckCircle2
} from 'lucide-react';

export const MinimalistDashboard: React.FC = () => {
  const navigate = useNavigate();

  const subjectClusters = [
    {
      title: 'Discrete Math & Logic',
      weightage: '14-15 Marks',
      topicsCount: 6,
      estHours: 45,
      textbook: 'Kenneth Rosen §1–§10',
      icon: <Binary className="w-5 h-5 text-blue-400" />,
      query: 'discrete',
    },
    {
      title: 'Programming & Data Structures',
      weightage: '10-12 Marks',
      topicsCount: 5,
      estHours: 35,
      textbook: 'Kernighan & Ritchie (K&R)',
      icon: <Cpu className="w-5 h-5 text-cyan-400" />,
      query: 'pointer',
    },
    {
      title: 'Algorithms Core',
      weightage: '8-10 Marks',
      topicsCount: 4,
      estHours: 40,
      textbook: 'CLRS Chapters 3–24',
      icon: <BookMarked className="w-5 h-5 text-emerald-400" />,
      query: 'algorithm',
    },
    {
      title: 'Systems Core (OS + DBMS + CN)',
      weightage: '24-27 Marks',
      topicsCount: 12,
      estHours: 85,
      textbook: 'Silberschatz, Galvin & Kurose',
      icon: <Network className="w-5 h-5 text-indigo-400" />,
      query: 'operating',
    },
  ];

  return (
    <div className="space-y-8" data-testid="minimalist-dashboard">
      {/* 1. Active Study Session Hero — Modern Minimalist Slate */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-[#0b1329] via-[#0f172a] to-[#0d1b38] border border-slate-800 p-7 shadow-xl">
        {/* Subtle electric blue ambient glow */}
        <div className="absolute top-0 right-1/4 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -bottom-10 left-1/3 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="max-w-xl space-y-3.5">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-blue-500/10 border border-blue-500/30 text-[11px] font-semibold text-blue-400">
              <Sparkles className="w-3.5 h-3.5 text-blue-400" />
              <span>ACTIVE STUDY SESSION · OPERATING SYSTEMS</span>
            </div>

            <div>
              <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight text-white leading-snug">
                Process Synchronization & IPC
              </h1>
              <p className="text-xs text-slate-400 mt-1.5 leading-relaxed">
                Critical sections, Peterson's algorithm, hardware instructions (Test-and-Set), semaphores, and classical synchronization problems.
              </p>
            </div>

            {/* Syllabus Chapter References */}
            <div className="flex flex-wrap items-center gap-3 pt-1 text-xs">
              <span className="flex items-center gap-1.5 text-blue-300 bg-blue-950/40 px-2.5 py-1 rounded-md border border-blue-800/40">
                <BookOpen className="w-3.5 h-3.5 text-blue-400" />
                Silberschatz §5 (OS Concepts)
              </span>
              <span className="flex items-center gap-1.5 text-slate-300 bg-slate-900 px-2.5 py-1 rounded-md border border-slate-800">
                <BookOpen className="w-3.5 h-3.5 text-slate-400" />
                Tanenbaum §2.3 (Modern OS)
              </span>
            </div>

            {/* Linear Progress Bar */}
            <div className="space-y-1.5 pt-2">
              <div className="flex items-center justify-between text-xs font-mono">
                <span className="text-slate-400">Chapter Mastery Progress</span>
                <span className="text-blue-400 font-bold">68% Complete</span>
              </div>
              <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-blue-500 to-cyan-400 rounded-full transition-all duration-500" 
                  style={{ width: '68%' }} 
                />
              </div>
            </div>
          </div>

          {/* Action CTAs */}
          <div className="shrink-0 flex flex-col sm:flex-row lg:flex-col items-start lg:items-end gap-3">
            <button
              type="button"
              onClick={() => navigate('/questions?q=synchronization')}
              className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white px-6 py-2.5 rounded-xl font-bold text-xs tracking-tight transition-all shadow-lg shadow-blue-600/20 group"
            >
              <span>Resume Study Session</span>
              <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
            </button>
            <button
              type="button"
              onClick={() => navigate('/syllabus')}
              className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white font-medium transition-colors"
            >
              <span>View Full OS Taxonomy</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 2. Subject Cluster Grid — 4-Column Layout */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-bold text-white tracking-wide uppercase flex items-center gap-2">
            <span>High-Yield Subject Clusters</span>
            <span className="text-[11px] text-slate-400 font-mono normal-case">(GATE CSE Mark Weightage)</span>
          </h2>
          <button 
            type="button"
            onClick={() => navigate('/syllabus')}
            className="text-xs text-blue-400 hover:text-blue-300 font-semibold flex items-center gap-1 transition-colors"
          >
            All 10 Subjects <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {subjectClusters.map((sub, idx) => (
            <div
              key={idx}
              className="bg-[#0f172a] hover:bg-[#131f38] border border-slate-800 hover:border-blue-500/40 rounded-xl p-5 flex flex-col justify-between transition-all duration-200 group shadow-sm"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center">
                    {sub.icon}
                  </div>
                  <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full border border-blue-500/30 bg-blue-500/10 text-blue-400">
                    {sub.weightage}
                  </span>
                </div>

                <div>
                  <h3 className="font-bold text-sm text-slate-100 group-hover:text-white leading-snug">
                    {sub.title}
                  </h3>
                  <p className="text-[11px] text-slate-400 mt-1 line-clamp-1 font-mono">
                    {sub.textbook}
                  </p>
                </div>
              </div>

              <div className="pt-4 border-t border-slate-800/80 mt-4 flex items-center justify-between">
                <div className="text-[11px] text-slate-400 font-mono">
                  {sub.topicsCount} topics · {sub.estHours} hrs
                </div>
                <button
                  type="button"
                  onClick={() => navigate(`/questions?q=${sub.query}`)}
                  className="text-xs font-semibold text-slate-300 group-hover:text-blue-400 flex items-center gap-1 transition-colors"
                >
                  Explore <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 3. Must-Have for GATE Preparation */}
      <div className="space-y-4">
        <h2 className="text-sm font-bold text-white tracking-wide uppercase">
          Essential GATE Preparation Tools
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-[#0f172a] border border-slate-800 rounded-xl p-5 flex items-start justify-between gap-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
                  <BookMarked className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-white">GATE Overflow PYQ Vault</h3>
                  <div className="text-[11px] text-slate-400 font-mono">Two-Pass Mastery Tracking</div>
                </div>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                Pass 1 foundation reviews with full solution derivations, followed by timed Pass 2 evaluations.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/questions')}
              className="shrink-0 self-center bg-blue-600/20 hover:bg-blue-600/30 text-blue-300 border border-blue-500/30 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all"
            >
              Launch Vault →
            </button>
          </div>

          <div className="bg-[#0f172a] border border-slate-800 rounded-xl p-5 flex items-start justify-between gap-4">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                  <CheckCircle2 className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-white">TCS iON Mock Exam Simulator</h3>
                  <div className="text-[11px] text-slate-400 font-mono">Real Test Center Environment</div>
                </div>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed">
                65 Questions, 180 Minutes, 100 Marks with official Virtual Calculator and TCS iON color palette.
              </p>
            </div>
            <button
              type="button"
              onClick={() => navigate('/syllabus')}
              className="shrink-0 self-center bg-slate-800 hover:bg-slate-700 text-slate-200 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all"
            >
              Explore Mocks →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
