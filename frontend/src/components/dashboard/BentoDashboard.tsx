import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, 
  ArrowRight, 
  BookOpen, 
  Binary, 
  Cpu, 
  Network, 
  FileCode, 
  CheckCircle2, 
  Clock, 
  ArrowUpRight,
  Target
} from 'lucide-react';

export const BentoDashboard: React.FC = () => {
  const navigate = useNavigate();
  const [selectedAnswer, setSelectedAnswer] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<boolean>(false);

  const clusterTiles = [
    {
      title: 'Algorithms (CLRS)',
      weightage: '8-10 Marks',
      progress: 55,
      barColor: 'from-emerald-500 to-teal-400',
      badgeClass: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
      topics: 'Asymptotic, Divide & Conquer, DP, Greedy',
      query: 'algorithm',
    },
    {
      title: 'Programming & Data Structures',
      weightage: '10-12 Marks',
      progress: 70,
      barColor: 'from-cyan-500 to-blue-400',
      badgeClass: 'text-cyan-400 bg-cyan-500/10 border-cyan-500/20',
      topics: 'C Pointers, Trees, Heaps, Hashing',
      query: 'pointer',
    },
    {
      title: 'Systems Core (OS + DBMS + CN)',
      weightage: '24-27 Marks',
      progress: 42,
      barColor: 'from-purple-500 to-indigo-400',
      badgeClass: 'text-purple-400 bg-purple-500/10 border-purple-500/20',
      topics: 'Virtual Memory, Transactions, Congestion',
      query: 'operating',
    },
    {
      title: 'Theory of Computation',
      weightage: '7-9 Marks',
      progress: 60,
      barColor: 'from-amber-500 to-yellow-400',
      badgeClass: 'text-amber-400 bg-amber-500/10 border-amber-500/20',
      topics: 'DFA/NFA, CFLs, Decidability, Turing Machines',
      query: 'compiler',
    },
  ];

  return (
    <div className="space-y-6 max-w-7xl" data-testid="bento-dashboard">
      {/* Bento Grid Container */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* 1. Hero Bento Tile (Col-span-2) — Active Study Focus */}
        <div className="lg:col-span-2 bg-[#141414] border border-zinc-800 rounded-3xl p-7 shadow-xl flex flex-col justify-between space-y-6 relative overflow-hidden group">
          {/* Ambient subtle glow */}
          <div className="absolute top-0 right-0 w-80 h-80 bg-amber-500/5 rounded-full blur-3xl pointer-events-none" />

          <div className="space-y-4 relative z-10">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-[11px] font-semibold text-amber-400 font-mono">
                <Sparkles className="w-3.5 h-3.5" />
                <span>ACTIVE STUDY FOCUS · DISCRETE MATHEMATICS</span>
              </div>
              <span className="text-xs font-mono text-zinc-500">GATE CSE 2028</span>
            </div>

            <div>
              <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-tight leading-snug">
                Graph Theory & Tree Structures
              </h1>
              <p className="text-xs text-zinc-400 mt-1 font-mono flex items-center gap-1.5">
                <BookOpen className="w-3.5 h-3.5 text-amber-400" />
                Reference: Kenneth H. Rosen (8th Edition), Chapter 10 (§10.1–§10.5)
              </p>
            </div>

            {/* Bulleted Sub-topics */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 text-xs text-zinc-300">
              <div className="p-2.5 rounded-xl bg-[#1a1a1a] border border-zinc-800/80 flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0" />
                <span>Graph Isomorphism & Handshaking Lemma</span>
              </div>
              <div className="p-2.5 rounded-xl bg-[#1a1a1a] border border-zinc-800/80 flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0" />
                <span>Euler & Hamiltonian Paths (Dirac Theorem)</span>
              </div>
              <div className="p-2.5 rounded-xl bg-[#1a1a1a] border border-zinc-800/80 flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0" />
                <span>Planar Graphs & Euler's Formula (V - E + R = 2)</span>
              </div>
              <div className="p-2.5 rounded-xl bg-[#1a1a1a] border border-zinc-800/80 flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0" />
                <span>Tree Traversals & Minimum Spanning Trees</span>
              </div>
            </div>
          </div>

          {/* Progress and CTA */}
          <div className="space-y-4 pt-2 border-t border-zinc-800/80 relative z-10">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs font-mono">
                <span className="text-zinc-400">Topic Syllabus Completion</span>
                <span className="text-amber-400 font-bold">75% Complete</span>
              </div>
              <div className="w-full h-2 bg-zinc-900 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-amber-500 to-amber-300 rounded-full transition-all duration-500" 
                  style={{ width: '75%' }} 
                />
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
              <button
                type="button"
                onClick={() => navigate('/questions?q=graph')}
                className="flex items-center gap-2 bg-amber-500 hover:bg-amber-400 text-black px-5 py-2.5 rounded-xl font-bold text-xs tracking-tight transition-all shadow-md shadow-amber-500/10 group"
              >
                <span>Practice Topic PYQs</span>
                <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
              </button>
              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="text-xs text-zinc-400 hover:text-zinc-200 font-mono transition-colors"
              >
                View Syllabus Hierarchy →
              </button>
            </div>
          </div>
        </div>

        {/* 2. Right Bento Column Tile — Quick Revision & Progress Summary */}
        <div className="bg-[#141414] border border-zinc-800 rounded-3xl p-6 shadow-xl flex flex-col justify-between space-y-5">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-xs uppercase tracking-wider text-zinc-300 font-mono">
                Syllabus Metric
              </h3>
              <Target className="w-4 h-4 text-amber-400" />
            </div>

            {/* Circular Gauge */}
            <div className="flex flex-col items-center justify-center py-2">
              <div className="relative w-32 h-32 flex items-center justify-center">
                <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
                  <circle
                    cx="50"
                    cy="50"
                    r="38"
                    className="stroke-zinc-800"
                    strokeWidth="8"
                    fill="transparent"
                  />
                  <circle
                    cx="50"
                    cy="50"
                    r="38"
                    className="stroke-amber-400 transition-all duration-700 ease-out"
                    strokeWidth="8"
                    strokeDasharray="238.7"
                    strokeDashoffset="199"
                    strokeLinecap="round"
                    fill="transparent"
                  />
                </svg>
                <div className="absolute flex flex-col items-center text-center">
                  <span className="text-xl font-black text-white font-mono">16.5%</span>
                  <span className="text-[10px] text-zinc-500 font-mono">16.5 / 100 M</span>
                </div>
              </div>
            </div>

            <div className="space-y-1.5 text-xs pt-1 border-t border-zinc-800/80">
              <div className="flex items-center justify-between">
                <span className="text-zinc-400">Foundation (GA+EM)</span>
                <span className="font-mono text-zinc-200">9.0 / 29</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-zinc-400">Systems Core</span>
                <span className="font-mono text-zinc-200">5.5 / 42</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-zinc-400">Algorithms & Theory</span>
                <span className="font-mono text-zinc-200">2.0 / 29</span>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => navigate('/syllabus')}
            className="w-full bg-[#1e1e1e] hover:bg-[#252525] border border-zinc-700/60 text-zinc-200 py-2 rounded-xl text-xs font-semibold font-mono transition-all text-center"
          >
            Review All 10 Subjects
          </button>
        </div>
      </div>

      {/* 3. Cluster Bento Tiles (4 Modular Cards) */}
      <div className="space-y-3">
        <h2 className="text-sm font-bold text-white tracking-wide uppercase font-mono flex items-center justify-between">
          <span>High-Yield Subject Clusters</span>
          <span className="text-xs text-zinc-500 normal-case font-normal">Tracked by weightage & progress</span>
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {clusterTiles.map((tile, idx) => (
            <div
              key={idx}
              className="bg-[#141414] hover:bg-[#181818] border border-zinc-800 hover:border-zinc-700 rounded-2xl p-5 flex flex-col justify-between space-y-4 transition-all duration-200"
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full border ${tile.badgeClass}`}>
                    {tile.weightage}
                  </span>
                  <span className="text-xs font-mono text-zinc-400 font-bold">
                    {tile.progress}%
                  </span>
                </div>

                <h3 className="font-bold text-sm text-zinc-100 leading-snug">
                  {tile.title}
                </h3>
                <p className="text-[11px] text-zinc-500 line-clamp-1 font-mono">
                  {tile.topics}
                </p>
              </div>

              <div className="space-y-2 pt-2 border-t border-zinc-800/80">
                <div className="w-full h-1.5 bg-zinc-900 rounded-full overflow-hidden">
                  <div
                    className={`h-full bg-gradient-to-r ${tile.barColor} rounded-full transition-all duration-500`}
                    style={{ width: `${tile.progress}%` }}
                  />
                </div>

                <button
                  type="button"
                  onClick={() => navigate(`/questions?q=${tile.query}`)}
                  className="w-full flex items-center justify-between text-xs text-zinc-400 hover:text-white pt-1 transition-colors"
                >
                  <span className="font-mono text-[11px]">Explore Topic</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 4. Integrated Problem of the Day Bento Tile (One-Click Answer Submission) */}
      <div className="bg-[#141414] border border-zinc-800 rounded-3xl p-6 shadow-xl space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-zinc-800 pb-3">
          <div className="flex items-center gap-2">
            <span className="font-bold text-xs uppercase tracking-wider text-zinc-200 font-mono">
              Problem of the Day
            </span>
            <span className="text-[10px] font-mono text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-full">
              GATE CS 2023 · Pipeline Speedup
            </span>
          </div>
          <div className="flex items-center gap-1 text-[11px] text-zinc-500 font-mono">
            <Clock className="w-3 h-3" />
            <span>22h remaining</span>
          </div>
        </div>

        <div className="space-y-2">
          <p className="text-xs text-zinc-300 leading-relaxed font-normal">
            An instruction pipeline has 5 stages with execution times 2ns, 3ns, 5ns, 4ns, and 2ns. In the absence of hazards, what is the theoretical maximum speedup achieved over non-pipelined execution for 1,000 instructions?
          </p>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2.5 pt-2">
            {[
              { label: 'A', text: '3.20' },
              { label: 'B', text: '3.85' },
              { label: 'C', text: '4.12' },
              { label: 'D', text: '4.95' },
            ].map((choice) => {
              const isSelected = selectedAnswer === choice.label;
              return (
                <button
                  key={choice.label}
                  type="button"
                  onClick={() => {
                    setSelectedAnswer(choice.label);
                    setSubmitted(false);
                  }}
                  className={`p-3 rounded-xl border text-left text-xs font-mono flex items-center justify-between transition-all ${
                    isSelected
                      ? 'bg-amber-500/20 border-amber-500 text-amber-300 font-bold shadow-md shadow-amber-500/10'
                      : 'bg-[#181818] border-zinc-800 text-zinc-400 hover:text-zinc-200 hover:border-zinc-700'
                  }`}
                >
                  <span>
                    <strong className="text-zinc-200 mr-2">{choice.label}:</strong>
                    {choice.text}
                  </span>
                  {isSelected && <CheckCircle2 className="w-4 h-4 text-amber-400" />}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-zinc-800/80">
          <span className="text-[11px] font-mono text-zinc-500">
            {selectedAnswer ? `Selected Option: ${selectedAnswer}` : 'Select an option to submit'}
          </span>
          <button
            type="button"
            disabled={!selectedAnswer || submitted}
            onClick={() => setSubmitted(true)}
            className="bg-amber-500 hover:bg-amber-400 disabled:opacity-40 text-black px-4 py-1.5 rounded-xl text-xs font-bold font-mono transition-all shadow-md shadow-amber-500/10"
          >
            {submitted ? 'Submitted Successfully ✓' : 'Submit Answer'}
          </button>
        </div>
      </div>
    </div>
  );
};
