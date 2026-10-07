import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, 
  ArrowRight, 
  BookOpen, 
  CheckCircle2, 
  Clock, 
  Target,
  Smile
} from 'lucide-react';
import { useDesign } from '../../context/DesignContext';
import { CurationProgressTile } from './CurationProgressTile';

export const BentoDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { currentDesign } = useDesign();
  const [selectedAnswer, setSelectedAnswer] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<boolean>(false);

  const isSky = currentDesign === 'sky';

  const clusterTiles = [
    {
      title: 'Algorithms (CLRS)',
      weightage: '8-10 Marks',
      progress: 55,
      topics: 'Asymptotic, Divide & Conquer, DP, Greedy',
      query: 'algorithm',
    },
    {
      title: 'Programming & Data Structures',
      weightage: '10-12 Marks',
      progress: 70,
      topics: 'C Pointers, Trees, Heaps, Hashing',
      query: 'pointer',
    },
    {
      title: 'Systems Core (OS + DBMS + CN)',
      weightage: '24-27 Marks',
      progress: 42,
      topics: 'Virtual Memory, Transactions, Congestion',
      query: 'operating',
    },
    {
      title: 'Theory of Computation',
      weightage: '7-9 Marks',
      progress: 60,
      topics: 'DFA/NFA, CFLs, Decidability, Turing Machines',
      query: 'compiler',
    },
  ];

  return (
    <div className="space-y-6 max-w-7xl" data-testid="bento-dashboard">
      {/* 1. Sky Welcome Banner (Mounted only under 'sky' preset) */}
      {isSky && (
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-6 flex items-center justify-between shadow-sm"
          data-testid="sky-welcome-banner"
        >
          <div className="space-y-1">
            <h1 className="text-xl sm:text-2xl font-extrabold text-ds-primary tracking-tight flex items-center gap-2">
              <span>Good to see you, Aspirant</span>
              <Smile className="w-5 h-5 text-ds-accent" />
            </h1>
            <p className="text-xs text-ds-muted font-mono">
              Your personalized GATE CSE 2028 study command center · 485 days to target exam
            </p>
          </div>
          <div className="hidden sm:flex items-center gap-2">
            <span className="text-xs font-mono font-semibold px-3 py-1 rounded-full bg-ds-accent-soft text-ds-accent border border-ds-accent/30">
              Target AIR &lt; 50
            </span>
          </div>
        </div>
      )}

      {/* Bento Grid Container */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* 2. Hero Bento Tile (Col-span-2) — Active Study Focus */}
        <div className="lg:col-span-2 bg-ds-surface border border-ds-border rounded-ds p-7 shadow-sm flex flex-col justify-between space-y-6 relative overflow-hidden group">
          <div className="space-y-4 relative z-10">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-ds-accent-soft border border-ds-accent/30 text-[11px] font-semibold text-ds-accent font-mono">
                <Sparkles className="w-3.5 h-3.5" />
                <span>Active Study Focus · Discrete Mathematics</span>
              </div>
              <span className="text-xs font-mono text-ds-muted">GATE CSE 2028</span>
            </div>

            <div>
              <h2 className="text-2xl md:text-3xl font-extrabold text-ds-primary tracking-tight leading-snug">
                Graph Theory & Tree Structures
              </h2>
              <p className="text-xs text-ds-muted mt-1 font-mono flex items-center gap-1.5">
                <BookOpen className="w-3.5 h-3.5 text-ds-accent" />
                Reference: Kenneth H. Rosen (8th Edition), Chapter 10 (§10.1–§10.5)
              </p>
            </div>

            {/* Bulleted Sub-topics */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 text-xs text-ds-secondary">
              <div className="p-2.5 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-ds-accent shrink-0" />
                <span>Graph Isomorphism & Handshaking Lemma</span>
              </div>
              <div className="p-2.5 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-ds-accent shrink-0" />
                <span>Euler & Hamiltonian Paths (Dirac Theorem)</span>
              </div>
              <div className="p-2.5 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-ds-accent shrink-0" />
                <span>Planar Graphs & Euler's Formula (V - E + R = 2)</span>
              </div>
              <div className="p-2.5 rounded-ds bg-ds-surface-raised border border-ds-border flex items-center gap-2">
                <span className="w-1.5 h-1.5 rounded-full bg-ds-accent shrink-0" />
                <span>Tree Traversals & Minimum Spanning Trees</span>
              </div>
            </div>
          </div>

          {/* Progress and CTA */}
          <div className="space-y-4 pt-2 border-t border-ds-border relative z-10">
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs font-mono">
                <span className="text-ds-muted">Topic Syllabus Completion</span>
                <span className="text-ds-accent font-bold">75% Complete</span>
              </div>
              <div className="w-full h-2 bg-ds-surface-raised rounded-full overflow-hidden">
                <div 
                  className="h-full bg-ds-accent rounded-full transition-all duration-500" 
                  style={{ width: '75%' }} 
                />
              </div>
            </div>

            <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
              <button
                type="button"
                onClick={() => navigate('/questions?q=graph')}
                className="flex items-center gap-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-5 py-2.5 rounded-ds font-bold text-xs tracking-tight transition-all shadow-sm group"
              >
                <span>Practice Topic PYQs</span>
                <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-1" />
              </button>
              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="text-xs text-ds-muted hover:text-ds-primary font-mono transition-colors"
              >
                View Syllabus Hierarchy →
              </button>
            </div>
          </div>
        </div>

        {/* 3. Right Bento Column Tile — Quick Revision & Progress Summary */}
        <div className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm flex flex-col justify-between space-y-5">
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-xs uppercase tracking-wider text-ds-primary font-mono">
                {isSky ? 'Overall Syllabus Progress' : 'Syllabus Metric'}
              </h3>
              <Target className="w-4 h-4 text-ds-accent" />
            </div>

            {/* Circular Gauge */}
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
                    className={`${isSky ? 'stroke-ds-success' : 'stroke-ds-accent'} transition-all duration-700 ease-out`}
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
              <div className="flex items-center justify-between">
                <span className="text-ds-secondary">Algorithms & Theory</span>
                <span className="font-mono text-ds-primary">2.0 / 29</span>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => navigate('/syllabus')}
            className="w-full bg-ds-surface-raised hover:bg-ds-surface-hover border border-ds-border text-ds-secondary hover:text-ds-primary py-2 rounded-ds text-xs font-semibold font-mono transition-all text-center"
          >
            Review All 10 Subjects
          </button>
        </div>
      </div>

      {/* 4. Cluster Bento Tiles (4 Modular Cards) */}
      <div className="space-y-3">
        <h3 className="text-xs font-bold text-ds-primary tracking-wide uppercase font-mono flex items-center justify-between">
          <span>High-Yield Subject Clusters</span>
          <span className="text-xs text-ds-muted normal-case font-normal">Tracked by weightage & progress</span>
        </h3>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {clusterTiles.map((tile, idx) => (
            <div
              key={idx}
              className="bg-ds-surface hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong rounded-ds p-5 flex flex-col justify-between space-y-4 transition-all duration-200"
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full border border-ds-accent/30 bg-ds-accent-soft text-ds-accent">
                    {tile.weightage}
                  </span>
                  <span className="text-xs font-mono text-ds-primary font-bold">
                    {tile.progress}%
                  </span>
                </div>

                <h4 className="font-bold text-sm text-ds-primary leading-snug">
                  {tile.title}
                </h4>
                <p className="text-[11px] text-ds-muted line-clamp-1 font-mono">
                  {tile.topics}
                </p>
              </div>

              <div className="space-y-2 pt-2 border-t border-ds-border">
                <div className="w-full h-1.5 bg-ds-surface-raised rounded-full overflow-hidden">
                  <div
                    className="h-full bg-ds-accent rounded-full transition-all duration-500"
                    style={{ width: `${tile.progress}%` }}
                  />
                </div>

                <button
                  type="button"
                  onClick={() => navigate(`/questions?q=${tile.query}`)}
                  className="w-full flex items-center justify-between text-xs text-ds-muted hover:text-ds-primary pt-1 transition-colors"
                >
                  <span className="font-mono text-[11px]">Explore Topic</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* 5. Integrated Problem of the Day Bento Tile */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-ds-border pb-3">
          <div className="flex items-center gap-2">
            <span className="font-bold text-xs uppercase tracking-wider text-ds-primary font-mono">
              Problem of the Day
            </span>
            <span className="text-[10px] font-mono text-ds-accent bg-ds-accent-soft border border-ds-accent/30 px-2 py-0.5 rounded-full">
              GATE CS 2023 · Pipeline Speedup
            </span>
          </div>
          <div className="flex items-center gap-1 text-[11px] text-ds-muted font-mono">
            <Clock className="w-3 h-3" />
            <span>22h remaining</span>
          </div>
        </div>

        <div className="space-y-2">
          <p className="text-xs text-ds-secondary leading-relaxed font-normal">
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
                  className={`p-3 rounded-ds border text-left text-xs font-mono flex items-center justify-between transition-all ${
                    isSelected
                      ? 'bg-ds-accent-soft border-ds-accent text-ds-accent font-bold shadow-sm'
                      : 'bg-ds-surface-raised border-ds-border text-ds-muted hover:text-ds-primary hover:border-ds-border-strong'
                  }`}
                >
                  <span>
                    <strong className="text-ds-primary mr-2">{choice.label}:</strong>
                    {choice.text}
                  </span>
                  {isSelected && <CheckCircle2 className="w-4 h-4 text-ds-accent" />}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-ds-border">
          <span className="text-[11px] font-mono text-ds-muted">
            {selectedAnswer ? `Selected Option: ${selectedAnswer}` : 'Select an option to submit'}
          </span>
          <button
            type="button"
            disabled={!selectedAnswer || submitted}
            onClick={() => setSubmitted(true)}
            className="bg-ds-accent hover:bg-ds-accent-hover disabled:opacity-40 text-ds-accent-contrast px-4 py-1.5 rounded-ds text-xs font-bold font-mono transition-all shadow-sm"
          >
            {submitted ? 'Submitted Successfully ✓' : 'Submit Answer'}
          </button>
        </div>
      </div>

      {/* 6. Curation Progress Tile */}
      <CurationProgressTile />
    </div>
  );
};
