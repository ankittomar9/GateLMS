import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BookOpen, 
  ArrowRight, 
  Bookmark, 
  Compass, 
  FileText, 
  GraduationCap, 
  CheckCircle,
  Clock
} from 'lucide-react';

export const EditorialDashboard: React.FC = () => {
  const navigate = useNavigate();

  const subject2x2 = [
    {
      title: 'Discrete Mathematics & Graph Theory',
      textbook: 'Kenneth H. Rosen · Discrete Mathematics and Its Applications (8th Ed)',
      weightage: '15 Marks',
      topicsCount: 6,
      completion: 72,
      estHours: 45,
      query: 'discrete',
    },
    {
      title: 'Algorithms & Computational Complexity',
      textbook: 'Cormen, Leiserson, Rivest & Stein · Introduction to Algorithms (CLRS)',
      weightage: '10 Marks',
      topicsCount: 4,
      completion: 60,
      estHours: 40,
      query: 'algorithm',
    },
    {
      title: 'Computer Architecture & Organization',
      textbook: 'Carl Hamacher · Computer Organization and Embedded Systems (6th Ed)',
      weightage: '10 Marks',
      topicsCount: 5,
      completion: 40,
      estHours: 38,
      query: 'cache',
    },
    {
      title: 'Operating Systems & Kernel Primitives',
      textbook: 'Silberschatz, Galvin & Gagne · Operating System Concepts (10th Ed)',
      weightage: '10 Marks',
      topicsCount: 5,
      completion: 65,
      estHours: 42,
      query: 'operating',
    },
  ];

  return (
    <div className="space-y-9 max-w-6xl" data-testid="editorial-dashboard">
      {/* 1. Active Study Session Hero — Editorial & Academic Obsidian */}
      <div className="relative overflow-hidden rounded-2xl bg-[#0a0a0a] border border-[#1f1f1f] p-8 shadow-2xl">
        {/* Subtle soft indigo ambient accent */}
        <div className="absolute top-0 right-10 w-72 h-72 bg-indigo-600/5 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-[#1e1e1e] pb-4">
            <div className="flex items-center gap-2.5">
              <span className="w-2 h-2 rounded-full bg-indigo-500 animate-pulse" />
              <span className="text-[11px] font-mono uppercase tracking-widest text-indigo-400">
                Textbook Reading & Problem Session
              </span>
            </div>
            <span className="text-xs font-mono text-zinc-500">
              GATE CSE 2028 · Academic Foundation
            </span>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-center">
            <div className="lg:col-span-2 space-y-3">
              <span className="text-xs font-mono text-zinc-400 uppercase tracking-wider block">
                CLRS · Introduction to Algorithms (4th Edition)
              </span>

              <h1 className="text-2xl sm:text-3xl font-serif font-bold text-white tracking-tight leading-snug">
                Chapter 2: Getting Started — §2.3 Designing Algorithms
              </h1>

              <p className="text-xs text-zinc-400 leading-relaxed max-w-xl font-normal">
                Analysis of Divide-and-Conquer paradigms, recurrence formulated for Merge Sort: 
                <span className="text-zinc-200 font-mono"> T(n) = 2T(n/2) + Θ(n)</span>, recursion trees, and worst-case comparison lower bounds.
              </p>

              {/* Section Progress */}
              <div className="pt-2 max-w-md space-y-2">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-zinc-400">Chapter Reading Progress</span>
                  <span className="text-indigo-400 font-semibold">3 / 5 Sections (60%)</span>
                </div>
                <div className="w-full h-1.5 bg-[#181818] rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-indigo-500 rounded-full transition-all duration-500" 
                    style={{ width: '60%' }} 
                  />
                </div>
              </div>
            </div>

            {/* Direct CTA */}
            <div className="lg:border-l lg:border-[#1e1e1e] lg:pl-6 flex flex-col justify-center space-y-3">
              <button
                type="button"
                onClick={() => navigate('/questions?q=algorithm')}
                className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-semibold py-2.5 px-5 rounded-xl text-xs tracking-wide transition-all shadow-lg shadow-indigo-600/10 flex items-center justify-center gap-2 group"
              >
                <span>Start Studying</span>
                <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
              </button>
              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="w-full bg-[#141414] hover:bg-[#1a1a1a] text-zinc-300 border border-[#262626] py-2 px-4 rounded-xl text-xs font-medium transition-all text-center"
              >
                Open Chapter Notes
              </button>
              <span className="text-[11px] font-mono text-zinc-500 text-center block">
                Estimated study time: ~45 mins
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* 2. Subject Cluster Grid — 2x2 Layout */}
      <div className="space-y-4">
        <div className="flex items-center justify-between border-b border-[#1c1c1c] pb-3">
          <div>
            <h2 className="text-base font-serif font-bold text-white tracking-tight">
              Curriculum Architecture & Subject Mastery
            </h2>
            <p className="text-xs text-zinc-500 font-mono mt-0.5">
              Standard textbook weightage distribution across 2x2 core pillars
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/syllabus')}
            className="text-xs font-mono text-indigo-400 hover:text-indigo-300 transition-colors flex items-center gap-1"
          >
            <span>View Full Taxonomy</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {subject2x2.map((item, idx) => (
            <div
              key={idx}
              className="bg-[#0c0c0c] hover:bg-[#111111] border border-[#1e1e1e] hover:border-indigo-500/40 rounded-2xl p-6 transition-all duration-200 space-y-4 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-mono uppercase tracking-wider text-indigo-400 font-bold bg-indigo-950/40 border border-indigo-800/40 px-2.5 py-0.5 rounded-md">
                    {item.weightage}
                  </span>
                  <span className="text-xs font-mono text-zinc-400">
                    {item.topicsCount} Topics · {item.estHours} Hours
                  </span>
                </div>

                <div>
                  <h3 className="font-serif font-bold text-base text-zinc-100 leading-snug">
                    {item.title}
                  </h3>
                  <p className="text-xs text-zinc-500 mt-1 italic font-serif">
                    {item.textbook}
                  </p>
                </div>
              </div>

              {/* Progress and actions */}
              <div className="pt-4 border-t border-[#1a1a1a] space-y-3">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-zinc-400">Mastery Index</span>
                  <span className="text-white font-bold">{item.completion}%</span>
                </div>
                <div className="w-full h-1.5 bg-[#1a1a1a] rounded-full overflow-hidden">
                  <div
                    className="h-full bg-indigo-500 rounded-full transition-all duration-500"
                    style={{ width: `${item.completion}%` }}
                  />
                </div>

                <div className="flex items-center justify-between pt-1">
                  <button
                    type="button"
                    onClick={() => navigate(`/questions?q=${item.query}`)}
                    className="text-xs text-indigo-400 hover:text-indigo-300 font-mono flex items-center gap-1 transition-colors"
                  >
                    <span>Practice PYQs</span>
                    <ArrowRight className="w-3 h-3" />
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/syllabus')}
                    className="text-xs text-zinc-400 hover:text-zinc-200 font-mono transition-colors"
                  >
                    Syllabus Details
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 3. Academic Reading Log & Reference Tracker */}
      <div className="bg-[#0a0a0a] border border-[#1e1e1e] rounded-2xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-serif font-bold text-sm text-zinc-200">
            Recommended Reference Editions & Syllabus Mapping
          </h3>
          <span className="text-[11px] font-mono text-zinc-500">
            GATE Syllabus CSE 2028 Concordance
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-xs">
          <div className="p-3 bg-[#121212] border border-[#1e1e1e] rounded-xl space-y-1">
            <span className="text-indigo-400 font-mono text-[10px] uppercase font-bold block">
              Discrete Mathematics
            </span>
            <span className="font-serif text-zinc-200 block font-medium">Kenneth H. Rosen</span>
            <span className="text-zinc-500 text-[11px] block">Chapters 1, 2, 6, 9, 10</span>
          </div>

          <div className="p-3 bg-[#121212] border border-[#1e1e1e] rounded-xl space-y-1">
            <span className="text-indigo-400 font-mono text-[10px] uppercase font-bold block">
              Algorithms
            </span>
            <span className="font-serif text-zinc-200 block font-medium">CLRS 4th Edition</span>
            <span className="text-zinc-500 text-[11px] block">Chapters 2, 3, 4, 15, 16, 22</span>
          </div>

          <div className="p-3 bg-[#121212] border border-[#1e1e1e] rounded-xl space-y-1">
            <span className="text-indigo-400 font-mono text-[10px] uppercase font-bold block">
              Computer Architecture
            </span>
            <span className="font-serif text-zinc-200 block font-medium">Carl Hamacher 6th Ed</span>
            <span className="text-zinc-500 text-[11px] block">Chapters 5, 8 (Memory Hierarchy)</span>
          </div>
        </div>
      </div>
    </div>
  );
};
