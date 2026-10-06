import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BookOpen, 
  ArrowRight, 
  Play, 
  BookMarked,
  Layers
} from 'lucide-react';
import { useDesign } from '../../context/DesignContext';

export const EditorialDashboard: React.FC = () => {
  const navigate = useNavigate();
  const { currentDesign } = useDesign();

  const isAurora = currentDesign === 'aurora';

  const subject2x2 = [
    {
      title: 'Discrete Mathematics & Graph Theory',
      textbook: 'Kenneth H. Rosen · Discrete Mathematics and Its Applications (8th Ed)',
      weightage: isAurora ? '14–15 Marks' : '15 Marks',
      topicsCount: 6,
      completion: 72,
      estHours: 45,
      query: 'discrete',
      badgeClass: isAurora ? 'text-ds-danger bg-ds-danger/10 border-ds-danger/30' : 'text-ds-accent bg-ds-accent-soft border-ds-accent/30',
    },
    {
      title: 'Algorithms & Computational Complexity',
      textbook: 'Cormen, Leiserson, Rivest & Stein · Introduction to Algorithms (CLRS)',
      weightage: isAurora ? '8–10 Marks' : '10 Marks',
      topicsCount: 4,
      completion: 60,
      estHours: 40,
      query: 'algorithm',
      badgeClass: isAurora ? 'text-ds-danger bg-ds-danger/10 border-ds-danger/30' : 'text-ds-accent bg-ds-accent-soft border-ds-accent/30',
    },
    {
      title: 'Computer Architecture & Organization',
      textbook: 'Carl Hamacher · Computer Organization and Embedded Systems (6th Ed)',
      weightage: isAurora ? '9–11 Marks' : '10 Marks',
      topicsCount: 5,
      completion: 40,
      estHours: 38,
      query: 'cache',
      badgeClass: isAurora ? 'text-ds-danger bg-ds-danger/10 border-ds-danger/30' : 'text-ds-accent bg-ds-accent-soft border-ds-accent/30',
    },
    {
      title: 'Operating Systems & Kernel Primitives',
      textbook: 'Silberschatz, Galvin & Gagne · Operating System Concepts (10th Ed)',
      weightage: isAurora ? '10–12 Marks' : '10 Marks',
      topicsCount: 5,
      completion: 65,
      estHours: 42,
      query: 'operating',
      badgeClass: isAurora ? 'text-ds-danger bg-ds-danger/10 border-ds-danger/30' : 'text-ds-accent bg-ds-accent-soft border-ds-accent/30',
    },
  ];

  return (
    <div className="space-y-9 max-w-6xl" data-testid="editorial-dashboard">
      {/* 1. Active Study Session Hero — Editorial / Aurora */}
      <div className="relative overflow-hidden rounded-ds bg-ds-surface border border-ds-border p-8 shadow-sm">
        <div className="relative z-10 space-y-6">
          <div className="flex flex-wrap items-center justify-between gap-4 border-b border-ds-border pb-4">
            <div className="flex items-center gap-2.5">
              <span className="w-2 h-2 rounded-full bg-ds-accent animate-pulse" />
              <span className="text-[11px] font-mono uppercase tracking-widest text-ds-accent">
                {isAurora ? "Today's Study Session" : "Textbook Reading & Problem Session"}
              </span>
            </div>
            <span className="text-xs font-mono text-ds-muted">
              GATE CSE 2028 · Academic Foundation
            </span>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-center">
            <div className="lg:col-span-2 space-y-3">
              <span className="text-xs font-mono text-ds-muted uppercase tracking-wider block">
                CLRS · Introduction to Algorithms (4th Edition)
              </span>

              <h1 className="text-2xl sm:text-3xl font-serif font-bold text-ds-primary tracking-tight leading-snug">
                Chapter 2: Getting Started — §2.3 Designing Algorithms
              </h1>

              <p className="text-xs text-ds-secondary leading-relaxed max-w-xl font-normal">
                Analysis of Divide-and-Conquer paradigms, recurrence formulated for Merge Sort: 
                <span className="text-ds-primary font-mono"> T(n) = 2T(n/2) + Θ(n)</span>, recursion trees, and worst-case comparison lower bounds.
              </p>

              {/* Section Progress */}
              <div className="pt-2 max-w-md space-y-2">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-ds-muted">Chapter Reading Progress</span>
                  <span className="text-ds-accent font-semibold">3 / 5 Sections (60%)</span>
                </div>
                <div className="w-full h-1.5 bg-ds-surface-raised rounded-full overflow-hidden">
                  <div 
                    className="h-full bg-ds-accent rounded-full transition-all duration-500" 
                    style={{ width: '60%' }} 
                  />
                </div>
              </div>
            </div>

            {/* Direct CTA or Aurora Book Card */}
            <div className="lg:border-l lg:border-ds-border lg:pl-6 flex flex-col justify-center space-y-3">
              {isAurora && (
                <div className="p-3 bg-ds-surface-raised border border-ds-border rounded-ds space-y-2 mb-1" data-testid="aurora-book-card">
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-10 rounded bg-ds-accent text-ds-accent-contrast flex items-center justify-center font-bold text-xs shrink-0 shadow">
                      CLRS
                    </div>
                    <div>
                      <div className="text-xs font-bold text-ds-primary line-clamp-1">Introduction to Algorithms</div>
                      <div className="text-[10px] text-ds-muted line-clamp-1">Cormen, Leiserson, Rivest, Stein</div>
                    </div>
                  </div>
                  <div className="text-[10px] font-mono text-ds-accent flex items-center justify-between pt-1 border-t border-ds-border">
                    <span>Active: Chapter 2</span>
                    <span>§2.3 Merge Sort</span>
                  </div>
                </div>
              )}

              <button
                type="button"
                onClick={() => navigate('/questions?q=algorithm')}
                className="w-full bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast font-semibold py-2.5 px-5 rounded-ds text-xs tracking-wide transition-all shadow-sm flex items-center justify-center gap-2 group"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Start Studying</span>
              </button>
              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="w-full bg-ds-surface-raised hover:bg-ds-surface-hover text-ds-secondary hover:text-ds-primary border border-ds-border py-2 px-4 rounded-ds text-xs font-medium transition-all text-center"
              >
                Open Chapter Notes
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* 2. Subject Cluster Grid — 2x2 Layout */}
      <div className="space-y-4">
        <div className="flex items-center justify-between border-b border-ds-border pb-3">
          <div>
            <h2 className="text-base font-serif font-bold text-ds-primary tracking-tight">
              Curriculum Architecture & Subject Mastery
            </h2>
            <p className="text-xs text-ds-muted font-mono mt-0.5">
              Standard textbook weightage distribution across 2x2 core pillars
            </p>
          </div>
          <button
            type="button"
            onClick={() => navigate('/syllabus')}
            className="text-xs font-mono text-ds-accent hover:underline transition-colors flex items-center gap-1"
          >
            <span>View Full Taxonomy</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {subject2x2.map((item, idx) => (
            <div
              key={idx}
              className="bg-ds-surface hover:bg-ds-surface-hover border border-ds-border hover:border-ds-border-strong rounded-ds p-6 transition-all duration-200 space-y-4 flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span className={`text-xs font-mono uppercase tracking-wider font-bold px-2.5 py-0.5 rounded-ds border ${item.badgeClass}`}>
                    {item.weightage}
                  </span>
                  <span className="text-xs font-mono text-ds-muted">
                    {item.topicsCount} Topics · {item.estHours} Hours
                  </span>
                </div>

                <div>
                  <h3 className="font-serif font-bold text-base text-ds-primary leading-snug">
                    {item.title}
                  </h3>
                  <p className="text-xs text-ds-muted mt-1 italic font-serif">
                    {item.textbook}
                  </p>
                </div>
              </div>

              {/* Progress and actions */}
              <div className="pt-4 border-t border-ds-border space-y-3">
                <div className="flex items-center justify-between text-xs font-mono">
                  <span className="text-ds-muted">Mastery Index</span>
                  <span className="text-ds-primary font-bold">{item.completion}%</span>
                </div>
                <div className="w-full h-1.5 bg-ds-surface-raised rounded-full overflow-hidden">
                  <div
                    className="h-full bg-ds-accent rounded-full transition-all duration-500"
                    style={{ width: `${item.completion}%` }}
                  />
                </div>

                <div className="flex items-center justify-between pt-1">
                  <button
                    type="button"
                    onClick={() => navigate(`/questions?q=${item.query}`)}
                    className="text-xs text-ds-accent hover:underline font-mono flex items-center gap-1 transition-colors"
                  >
                    <span>Practice PYQs</span>
                    <ArrowRight className="w-3 h-3" />
                  </button>
                  <button
                    type="button"
                    onClick={() => navigate('/syllabus')}
                    className="text-xs text-ds-muted hover:text-ds-primary font-mono transition-colors"
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
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h3 className="font-serif font-bold text-sm text-ds-primary">
            Recommended Reference Editions & Syllabus Mapping
          </h3>
          <span className="text-[11px] font-mono text-ds-muted">
            GATE Syllabus CSE 2028 Concordance
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 text-xs">
          <div className="p-3 bg-ds-surface-raised border border-ds-border rounded-ds space-y-1">
            <span className="text-ds-accent font-mono text-[10px] uppercase font-bold block">
              Discrete Mathematics
            </span>
            <span className="font-serif text-ds-primary block font-medium">Kenneth H. Rosen</span>
            <span className="text-ds-muted text-[11px] block">Chapters 1, 2, 6, 9, 10</span>
          </div>

          <div className="p-3 bg-ds-surface-raised border border-ds-border rounded-ds space-y-1">
            <span className="text-ds-accent font-mono text-[10px] uppercase font-bold block">
              Algorithms
            </span>
            <span className="font-serif text-ds-primary block font-medium">CLRS 4th Edition</span>
            <span className="text-ds-muted text-[11px] block">Chapters 2, 3, 4, 15, 16, 22</span>
          </div>

          <div className="p-3 bg-ds-surface-raised border border-ds-border rounded-ds space-y-1">
            <span className="text-ds-accent font-mono text-[10px] uppercase font-bold block">
              Computer Architecture
            </span>
            <span className="font-serif text-ds-primary block font-medium">Carl Hamacher 6th Ed</span>
            <span className="text-ds-muted text-[11px] block">Chapters 5, 8 (Memory Hierarchy)</span>
          </div>
        </div>
      </div>
    </div>
  );
};
