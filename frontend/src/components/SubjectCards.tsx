import React from 'react';
import { ArrowRight, BookMarked, Cpu, Binary, Network, ShieldCheck, CheckCircle2 } from 'lucide-react';

interface SubjectCardProps {
  title: string;
  weightage: string;
  topicsCount: number;
  estHours: number;
  textbook: string;
  icon: React.ReactNode;
  tagColor: string;
}

const subjects: SubjectCardProps[] = [
  {
    title: 'Discrete Mathematics & Logic',
    weightage: '14-15 Marks',
    topicsCount: 6,
    estHours: 45,
    textbook: 'Kenneth Rosen §1-§10',
    icon: <Binary className="w-5 h-5 text-amber-400" />,
    tagColor: 'border-amber-500/20 bg-amber-500/10 text-amber-400'
  },
  {
    title: 'Programming & Data Structures',
    weightage: '10-12 Marks',
    topicsCount: 5,
    estHours: 35,
    textbook: 'Kernighan & Ritchie (K&R)',
    icon: <Cpu className="w-5 h-5 text-cyan-400" />,
    tagColor: 'border-cyan-500/20 bg-cyan-500/10 text-cyan-400'
  },
  {
    title: 'Algorithms (CLRS)',
    weightage: '8-10 Marks',
    topicsCount: 4,
    estHours: 40,
    textbook: 'CLRS Chapters 3-24',
    icon: <BookMarked className="w-5 h-5 text-emerald-400" />,
    tagColor: 'border-emerald-500/20 bg-emerald-500/10 text-emerald-400'
  },
  {
    title: 'Systems Core (OS + DBMS + CN)',
    weightage: '24-27 Marks',
    topicsCount: 12,
    estHours: 85,
    textbook: 'Silberschatz, Galvin & Kurose',
    icon: <Network className="w-5 h-5 text-purple-400" />,
    tagColor: 'border-purple-500/20 bg-purple-500/10 text-purple-400'
  }
];

export const SubjectCards: React.FC = () => {
  return (
    <div className="space-y-6">
      {/* Popular Topics Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-bold text-white tracking-wide uppercase flex items-center gap-2">
            <span>Explore High-Yield Subject Clusters</span>
            <span className="text-[11px] text-zinc-500 font-mono normal-case">(GATE CSE Weightage)</span>
          </h2>
          <button className="text-xs text-amber-400 hover:text-amber-300 font-semibold flex items-center gap-1 transition-colors">
            View All 10 Subjects <ArrowRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {subjects.map((sub, idx) => (
            <div
              key={idx}
              className="bg-[#161616] hover:bg-[#1a1a1a] border border-[#262626] hover:border-zinc-700/80 rounded-xl p-5 flex flex-col justify-between transition-all duration-200 group"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="w-10 h-10 rounded-lg bg-[#202020] border border-zinc-800 flex items-center justify-center">
                    {sub.icon}
                  </div>
                  <span className={`text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full border ${sub.tagColor}`}>
                    {sub.weightage}
                  </span>
                </div>

                <div>
                  <h3 className="font-bold text-sm text-zinc-100 group-hover:text-white leading-snug">
                    {sub.title}
                  </h3>
                  <p className="text-[11px] text-zinc-500 mt-1 line-clamp-1">
                    {sub.textbook}
                  </p>
                </div>
              </div>

              <div className="pt-4 border-t border-zinc-800/60 mt-4 flex items-center justify-between">
                <div className="text-[11px] text-zinc-400 font-mono">
                  {sub.topicsCount} topics · {sub.estHours} hrs
                </div>
                <button className="text-xs font-semibold text-zinc-300 group-hover:text-amber-400 flex items-center gap-1 transition-colors">
                  Explore <ArrowRight className="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Must-Have for GATE Section */}
      <div>
        <h2 className="text-sm font-bold text-white tracking-wide uppercase mb-4">
          Must-Have for GATE Preparation
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Card 1: GATE Overflow PYQ Vault */}
          <div className="bg-[#161616] border border-[#262626] hover:border-zinc-700 rounded-xl p-5 flex items-start justify-between gap-4 transition-all">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                  <BookMarked className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-white">GATE Overflow PYQ Vault (1987–2024)</h3>
                  <div className="text-[11px] text-zinc-500 font-mono">2,500 questions cataloged with two-pass tracking</div>
                </div>
              </div>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Track Pass 1 (learning & conceptual foundation) vs Pass 2 (unassisted speed solving in under 3 minutes).
              </p>
              <div className="flex items-center gap-4 pt-2 text-[11px] text-zinc-500 font-mono">
                <span>✓ 2-Pass Verification</span>
                <span>✓ Official Answer Keys</span>
              </div>
            </div>
            <button className="shrink-0 self-center bg-zinc-800 hover:bg-zinc-700 text-zinc-200 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all">
              Launch Vault →
            </button>
          </div>

          {/* Card 2: TCS iON Center Mock Simulator */}
          <div className="bg-[#161616] border border-[#262626] hover:border-zinc-700 rounded-xl p-5 flex items-start justify-between gap-4 transition-all">
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-bold text-sm text-white">TCS iON Center Mock Simulator</h3>
                  <div className="text-[11px] text-zinc-500 font-mono">65 Questions · 100 Marks · 180 Minutes</div>
                </div>
              </div>
              <p className="text-xs text-zinc-400 leading-relaxed">
                Full exam simulation with authentic 5-color question palette, negative marking calculus, and scientific virtual calculator.
              </p>
              <div className="flex items-center gap-4 pt-2 text-[11px] text-zinc-500 font-mono">
                <span>✓ Virtual Calculator</span>
                <span>✓ MSQ / NAT / MCQ</span>
              </div>
            </div>
            <button className="shrink-0 self-center bg-emerald-500/10 hover:bg-emerald-500/20 border border-emerald-500/30 text-emerald-400 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all">
              Start Mock →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
