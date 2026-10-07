import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  Code2, 
  Database, 
  Clock, 
  FileText, 
  ChevronDown, 
  ChevronRight, 
  AlertCircle,
  GraduationCap,
  ClipboardCheck
} from 'lucide-react';
import { useDesign } from '../context/DesignContext';

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { currentDesign } = useDesign();
  const [practiceOpen, setPracticeOpen] = useState(true);

  const pathname = location.pathname;
  const isDashboard = pathname === '/';
  const isSyllabus = pathname.startsWith('/syllabus');
  const isQuestions = pathname.startsWith('/questions');
  const isCuration = pathname.startsWith('/curation');

  // Active item styling: 3px left border on aurora & sky, background tint on tuf & ink
  const getActiveNavClass = (isActive: boolean) => {
    if (!isActive) {
      return 'text-ds-secondary hover:text-ds-primary hover:bg-ds-surface';
    }
    if (currentDesign === 'aurora' || currentDesign === 'sky') {
      return 'bg-ds-surface-hover text-ds-primary font-semibold border-l-[3px] border-ds-accent shadow-sm';
    }
    return 'bg-ds-surface-hover text-ds-primary font-semibold shadow-sm border border-ds-border-strong';
  };

  return (
    <aside className="w-64 bg-ds-bg-sidebar border-r border-ds-border flex flex-col justify-between shrink-0 select-none min-h-screen transition-colors">
      {/* Top Branding */}
      <div>
        <div 
          onClick={() => navigate('/')}
          className="h-16 flex items-center justify-between px-5 border-b border-ds-border cursor-pointer hover:bg-ds-surface transition-colors"
        >
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-ds bg-ds-accent text-ds-accent-contrast flex items-center justify-center font-bold text-sm shadow-md">
              G
            </div>
            <div>
              <span className="font-bold text-base tracking-tight text-ds-primary flex items-center gap-1.5">
                Gate<span className="text-ds-accent">LMS</span>
              </span>
              <span className="text-[10px] text-ds-muted block leading-tight font-mono">GATE CSE 2028</span>
            </div>
          </div>
        </div>

        {/* Navigation Sections */}
        <div className="py-4 px-3 space-y-6 text-sm">
          {/* Section: PREP */}
          <div>
            <div className="text-[11px] font-semibold tracking-wider text-ds-muted uppercase px-3 mb-1.5">
              Prep
            </div>
            <div className="space-y-0.5">
              <button
                type="button"
                onClick={() => navigate('/')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isDashboard)}`}
              >
                <LayoutDashboard className={`w-4 h-4 ${isDashboard ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Dashboard</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isSyllabus)}`}
              >
                <GraduationCap className={`w-4 h-4 ${isSyllabus ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Syllabus Taxonomy</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/questions')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isQuestions)}`}
              >
                <Database className={`w-4 h-4 ${isQuestions ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Question Bank</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/curation')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isCuration)}`}
              >
                <ClipboardCheck className={`w-4 h-4 ${isCuration ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Curation Queue</span>
              </button>

              {/* Collapsible Practice */}
              <div>
                <button
                  type="button"
                  onClick={() => setPracticeOpen(!practiceOpen)}
                  className="w-full flex items-center justify-between px-3 py-2 rounded-ds text-ds-secondary hover:text-ds-primary hover:bg-ds-surface font-medium transition-all"
                >
                  <div className="flex items-center gap-3">
                    <Code2 className="w-4 h-4 text-ds-muted" />
                    <span>Practice Clusters</span>
                  </div>
                  {practiceOpen ? (
                    <ChevronDown className="w-3.5 h-3.5 text-ds-muted" />
                  ) : (
                    <ChevronRight className="w-3.5 h-3.5 text-ds-muted" />
                  )}
                </button>

                {practiceOpen && (
                  <div className="pl-9 pr-1 py-1 space-y-0.5 text-xs">
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=discrete')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      Discrete Math & Logic
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=pointer')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      C Pointers & DSA
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=operating')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      OS / DBMS / Networks
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/questions?q=compiler')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      TOC & Compilers
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => navigate('/questions?pass1Status=FLAGGED')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium text-ds-secondary hover:text-ds-primary hover:bg-ds-surface transition-all"
              >
                <AlertCircle className="w-4 h-4 text-ds-danger" />
                <span>Mistake Notebook</span>
              </button>
            </div>
          </div>

          {/* Section: EXAMS */}
          <div>
            <div className="text-[11px] font-semibold tracking-wider text-ds-muted uppercase px-3 mb-1.5">
              Exams & Drills
            </div>
            <div className="space-y-0.5">
              <button
                type="button"
                onClick={() => navigate('/')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium text-ds-secondary hover:text-ds-primary hover:bg-ds-surface transition-all"
              >
                <Clock className="w-4 h-4 text-ds-success" />
                <span>TCS iON Mocks (Phase 3)</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/questions')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium text-ds-secondary hover:text-ds-primary hover:bg-ds-surface transition-all"
              >
                <Database className="w-4 h-4 text-ds-info" />
                <span>GATE Overflow PYQs</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/syllabus')}
                className="w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium text-ds-secondary hover:text-ds-primary hover:bg-ds-surface transition-all"
              >
                <FileText className="w-4 h-4 text-ds-warning" />
                <span>iPad Notes Vault</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Profile Card */}
      <div className="p-3 border-t border-ds-border">
        <div className="bg-ds-surface border border-ds-border rounded-ds p-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-ds-surface-raised border border-ds-border-strong flex items-center justify-center font-bold text-xs text-ds-accent">
              AT
            </div>
            <div>
              <div className="font-semibold text-xs text-ds-primary">Ankur Tomar</div>
              <div className="text-[10px] text-ds-muted font-mono">AIR &lt; 50 Target</div>
            </div>
          </div>
          <span className="text-[10px] font-semibold bg-ds-accent-soft text-ds-accent px-2 py-0.5 rounded-full border border-ds-accent/30 font-mono">
            2028
          </span>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
