import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Compass, 
  Code2, 
  Database, 
  Calculator, 
  BookmarkCheck, 
  Clock, 
  FileText, 
  ChevronDown, 
  ChevronRight, 
  Sparkles,
  AlertCircle,
  GraduationCap,
  BookOpen
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [practiceOpen, setPracticeOpen] = useState(true);

  const pathname = location.pathname;
  const isDashboard = pathname === '/';
  const isSyllabus = pathname.startsWith('/syllabus');
  const isQuestions = pathname.startsWith('/questions');

  return (
    <aside className="w-64 bg-[#0d0d0d] border-r border-[#262626] flex flex-col justify-between shrink-0 select-none min-h-screen">
      {/* Top Branding */}
      <div>
        <div 
          onClick={() => navigate('/')}
          className="h-16 flex items-center justify-between px-5 border-b border-[#262626] cursor-pointer hover:bg-[#121212] transition-colors"
        >
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-amber-500 to-amber-300 flex items-center justify-center font-bold text-black text-sm shadow-md shadow-amber-500/20">
              G
            </div>
            <div>
              <span className="font-bold text-base tracking-tight text-white flex items-center gap-1.5">
                Gate<span className="text-amber-400">LMS</span>
              </span>
              <span className="text-[10px] text-zinc-500 block leading-tight font-mono">GATE CSE 2028</span>
            </div>
          </div>
        </div>

        {/* Navigation Sections */}
        <div className="py-4 px-3 space-y-6 text-sm">
          {/* Section: PREP */}
          <div>
            <div className="text-[11px] font-semibold tracking-wider text-zinc-500 uppercase px-3 mb-1.5">
              Prep
            </div>
            <div className="space-y-0.5">
              <button
                type="button"
                onClick={() => navigate('/')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition-all ${
                  isDashboard
                    ? 'bg-[#1f1f1f] text-white shadow-sm border border-zinc-700/50'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-[#161616]'
                }`}
              >
                <LayoutDashboard className={`w-4 h-4 ${isDashboard ? 'text-amber-400' : 'text-zinc-400'}`} />
                <span>Dashboard</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition-all ${
                  isSyllabus
                    ? 'bg-[#1f1f1f] text-white shadow-sm border border-zinc-700/50'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-[#161616]'
                }`}
              >
                <GraduationCap className={`w-4 h-4 ${isSyllabus ? 'text-amber-400' : 'text-zinc-400'}`} />
                <span>Syllabus Taxonomy</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/questions')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium transition-all ${
                  isQuestions
                    ? 'bg-[#1f1f1f] text-white shadow-sm border border-zinc-700/50'
                    : 'text-zinc-400 hover:text-zinc-200 hover:bg-[#161616]'
                }`}
              >
                <Database className={`w-4 h-4 ${isQuestions ? 'text-amber-400' : 'text-zinc-400'}`} />
                <span>Question Bank</span>
              </button>

              {/* Collapsible Practice */}
              <div>
                <button
                  type="button"
                  onClick={() => setPracticeOpen(!practiceOpen)}
                  className="w-full flex items-center justify-between px-3 py-2 rounded-lg text-zinc-400 hover:text-zinc-200 hover:bg-[#161616] font-medium transition-all"
                >
                  <div className="flex items-center gap-3">
                    <Code2 className="w-4 h-4 text-zinc-400" />
                    <span>Practice Clusters</span>
                  </div>
                  {practiceOpen ? (
                    <ChevronDown className="w-3.5 h-3.5 text-zinc-500" />
                  ) : (
                    <ChevronRight className="w-3.5 h-3.5 text-zinc-500" />
                  )}
                </button>

                {practiceOpen && (
                  <div className="pl-9 pr-1 py-1 space-y-0.5 text-xs">
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=discrete')}
                      className="w-full text-left py-1.5 px-2 rounded text-zinc-400 hover:text-white hover:bg-[#1a1a1a] transition-all"
                    >
                      Discrete Math & Logic
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=pointer')}
                      className="w-full text-left py-1.5 px-2 rounded text-zinc-400 hover:text-white hover:bg-[#1a1a1a] transition-all"
                    >
                      C Pointers & DSA
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=operating')}
                      className="w-full text-left py-1.5 px-2 rounded text-zinc-400 hover:text-white hover:bg-[#1a1a1a] transition-all"
                    >
                      OS / DBMS / Networks
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=compiler')}
                      className="w-full text-left py-1.5 px-2 rounded text-zinc-400 hover:text-white hover:bg-[#1a1a1a] transition-all"
                    >
                      TOC & Compilers
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => navigate('/questions?pass1Status=FLAGGED')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium text-zinc-400 hover:text-zinc-200 hover:bg-[#161616] transition-all"
              >
                <AlertCircle className="w-4 h-4 text-rose-400" />
                <span>Mistake Notebook</span>
              </button>
            </div>
          </div>

          {/* Section: EXAMS */}
          <div>
            <div className="text-[11px] font-semibold tracking-wider text-zinc-500 uppercase px-3 mb-1.5">
              Exams & Drills
            </div>
            <div className="space-y-0.5">
              <button
                type="button"
                onClick={() => navigate('/')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium text-zinc-400 hover:text-zinc-200 hover:bg-[#161616] transition-all"
              >
                <Clock className="w-4 h-4 text-emerald-400" />
                <span>TCS iON Mocks (Phase 3)</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/questions')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium text-zinc-400 hover:text-zinc-200 hover:bg-[#161616] transition-all"
              >
                <Database className="w-4 h-4 text-cyan-400" />
                <span>GATE Overflow PYQs</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-lg font-medium text-zinc-400 hover:text-zinc-200 hover:bg-[#161616] transition-all"
              >
                <FileText className="w-4 h-4 text-amber-400" />
                <span>iPad Notes Vault</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Profile Card */}
      <div className="p-3 border-t border-[#262626]">
        <div className="bg-[#161616] border border-[#262626] rounded-xl p-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-zinc-800 border border-zinc-700 flex items-center justify-center font-bold text-xs text-amber-300">
              AT
            </div>
            <div>
              <div className="font-semibold text-xs text-white">Ankur Tomar</div>
              <div className="text-[10px] text-zinc-500 font-mono">AIR &lt; 50 Target</div>
            </div>
          </div>
          <span className="text-[10px] font-semibold bg-amber-500/10 text-amber-400 px-2 py-0.5 rounded-full border border-amber-500/20">
            2028
          </span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
