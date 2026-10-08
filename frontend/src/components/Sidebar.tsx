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
  ChevronLeft,
  AlertCircle,
  GraduationCap,
  ClipboardCheck,
  PenLine,
  Activity,
  NotebookPen,
  RotateCcw,
  FileClock,
  CalendarRange
} from 'lucide-react';
import { useDesign } from '../context/DesignContext';
import { useDashboardLayout } from '../context/LayoutContext';

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { currentDesign } = useDesign();
  const { leftSidebarCollapsed, setLeftSidebarCollapsed } = useDashboardLayout();
  const [practiceOpen, setPracticeOpen] = useState(true);

  const pathname = location.pathname;
  const isDashboard = pathname === '/';
  const isPlanner = pathname.startsWith('/planner');
  const isPyp = pathname.startsWith('/pyp');
  const isInsights = pathname.startsWith('/insights');
  const isRevision = pathname.startsWith('/revision');
  const isNotes = pathname.startsWith('/notes');
  const isSyllabus = pathname.startsWith('/syllabus');
  const isQuestions = pathname.startsWith('/questions');
  const isCuration = pathname.startsWith('/curation');
  const isAuthor = pathname.startsWith('/author');
  const isPractice = pathname.startsWith('/practice');
  const isMistakes = pathname.startsWith('/mistakes');
  const isMocks = pathname.startsWith('/mocks');

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

  if (leftSidebarCollapsed) {
    return (
      <aside 
        className="w-16 bg-ds-bg-sidebar border-r border-ds-border flex flex-col justify-between shrink-0 select-none min-h-screen transition-all duration-200 py-2"
        data-testid="left-sidebar-collapsed"
      >
        <div>
          {/* Logo & Expand Button */}
          <div className="h-14 flex items-center justify-center border-b border-ds-border relative">
            <button
              type="button"
              onClick={() => setLeftSidebarCollapsed(false)}
              data-testid="left-sidebar-expand-btn"
              title="Expand left sidebar"
              aria-label="Expand left sidebar"
              className="w-9 h-9 rounded-ds bg-ds-accent text-ds-accent-contrast flex items-center justify-center font-bold text-sm shadow-md hover:opacity-90 transition-opacity group"
            >
              <span className="group-hover:hidden">G</span>
              <ChevronRight className="w-4 h-4 hidden group-hover:block" />
            </button>
          </div>

          {/* Compact Icon Nav */}
          <div className="py-3 px-2 space-y-2">
            <button
              type="button"
              onClick={() => navigate('/')}
              title="Dashboard"
              aria-label="Dashboard"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isDashboard)}`}
            >
              <LayoutDashboard className={`w-4 h-4 ${isDashboard ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/planner')}
              title="Planner"
              aria-label="Planner"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isPlanner)}`}
            >
              <CalendarRange className={`w-4 h-4 ${isPlanner ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/insights')}
              title="Insights"
              aria-label="Insights"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isInsights)}`}
            >
              <Activity className={`w-4 h-4 ${isInsights ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/revision')}
              title="Revision"
              aria-label="Revision"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isRevision)}`}
            >
              <RotateCcw className={`w-4 h-4 ${isRevision ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/syllabus')}
              title="Syllabus Taxonomy"
              aria-label="Syllabus Taxonomy"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isSyllabus)}`}
            >
              <GraduationCap className={`w-4 h-4 ${isSyllabus ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/questions')}
              title="Question Bank"
              aria-label="Question Bank"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isQuestions)}`}
            >
              <Database className={`w-4 h-4 ${isQuestions ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/practice')}
              title="Practice Clusters"
              aria-label="Practice Clusters"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isPractice)}`}
            >
              <Code2 className={`w-4 h-4 ${isPractice ? 'text-ds-accent' : 'text-ds-muted'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/mistakes')}
              title="Mistake Notebook"
              aria-label="Mistake Notebook"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isMistakes)}`}
            >
              <AlertCircle className={`w-4 h-4 ${isMistakes ? 'text-ds-accent' : 'text-ds-danger'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/mocks')}
              title="Mock Tests"
              aria-label="Mock Tests"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isMocks)}`}
            >
              <Clock className={`w-4 h-4 ${isMocks ? 'text-ds-accent' : 'text-ds-success'}`} />
            </button>

            <button
              type="button"
              onClick={() => navigate('/notes')}
              title="Notes Vault"
              aria-label="Notes Vault"
              className={`w-10 h-10 mx-auto flex items-center justify-center rounded-ds transition-all ${getActiveNavClass(isNotes)}`}
            >
              <NotebookPen className={`w-4 h-4 ${isNotes ? 'text-ds-accent' : 'text-ds-warning'}`} />
            </button>
          </div>
        </div>

        {/* Profile Avatar Only */}
        <div className="p-2 border-t border-ds-border flex justify-center">
          <div
            title="Ankur Tomar · Target AIR < 50"
            className="w-8 h-8 rounded-full bg-ds-surface-raised border border-ds-border-strong flex items-center justify-center font-bold text-[11px] text-ds-accent cursor-pointer"
          >
            AT
          </div>
        </div>
      </aside>
    );
  }

  return (
    <aside className="w-64 bg-ds-bg-sidebar border-r border-ds-border flex flex-col justify-between shrink-0 select-none min-h-screen transition-all duration-200">
      {/* Top Branding */}
      <div>
        <div 
          className="h-16 flex items-center justify-between px-4 border-b border-ds-border"
        >
          <div 
            onClick={() => navigate('/')}
            className="flex items-center gap-2.5 cursor-pointer hover:opacity-85 transition-opacity"
          >
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

          <button
            type="button"
            onClick={() => setLeftSidebarCollapsed(true)}
            data-testid="left-sidebar-collapse-btn"
            title="Collapse sidebar"
            aria-label="Collapse sidebar"
            className="p-1 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface transition-colors border border-transparent hover:border-ds-border"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>
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
                onClick={() => navigate('/planner')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isPlanner)}`}
              >
                <CalendarRange className={`w-4 h-4 ${isPlanner ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Planner</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/insights')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isInsights)}`}
              >
                <Activity className={`w-4 h-4 ${isInsights ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Insights</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/revision')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isRevision)}`}
              >
                <RotateCcw className={`w-4 h-4 ${isRevision ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Revision</span>
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

              <button
                type="button"
                onClick={() => navigate('/author')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isAuthor)}`}
              >
                <PenLine className={`w-4 h-4 ${isAuthor ? 'text-ds-accent' : 'text-ds-muted'}`} />
                <span>Author Question</span>
              </button>

              {/* Practice Clusters */}
              <div>
                <div className={`w-full flex items-center justify-between px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isPractice)}`}>
                  <button
                    type="button"
                    onClick={() => navigate('/practice')}
                    className="flex items-center gap-3 flex-1 text-left"
                  >
                    <Code2 className={`w-4 h-4 ${isPractice ? 'text-ds-accent' : 'text-ds-muted'}`} />
                    <span>Practice Clusters</span>
                  </button>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setPracticeOpen(!practiceOpen);
                    }}
                    className="p-1 hover:bg-ds-surface rounded-ds text-ds-muted hover:text-ds-primary"
                    aria-label="Toggle practice clusters"
                  >
                    {practiceOpen ? (
                      <ChevronDown className="w-3.5 h-3.5" />
                    ) : (
                      <ChevronRight className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>

                {practiceOpen && (
                  <div className="pl-9 pr-1 py-1 space-y-0.5 text-xs">
                    <button
                      type="button"
                      onClick={() => navigate('/practice?subject=subj_discrete_math')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      Discrete Math & Logic
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/practice?subject=subj_prog_ds')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      C Pointers & DSA
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/practice?subject=subj_os')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      OS / DBMS / Networks
                    </button>
                    <button
                      type="button"
                      onClick={() => navigate('/practice?subject=subj_toc')}
                      className="w-full text-left py-1.5 px-2 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover transition-all"
                    >
                      TOC & Compilers
                    </button>
                  </div>
                )}
              </div>

              <button
                type="button"
                onClick={() => navigate('/mistakes')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isMistakes)}`}
              >
                <AlertCircle className={`w-4 h-4 ${isMistakes ? 'text-ds-accent' : 'text-ds-danger'}`} />
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
                onClick={() => navigate('/mocks')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isMocks)}`}
              >
                <Clock className={`w-4 h-4 ${isMocks ? 'text-ds-accent' : 'text-ds-success'}`} />
                <span>Mock Tests</span>
              </button>

              <button
                type="button"
                onClick={() => navigate('/pyp')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isPyp)}`}
              >
                <FileClock className={`w-4 h-4 ${isPyp ? 'text-ds-accent' : 'text-ds-warning'}`} />
                <span>PYP Papers</span>
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
                onClick={() => navigate('/notes')}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-ds font-medium transition-all ${getActiveNavClass(isNotes)}`}
              >
                <NotebookPen className={`w-4 h-4 ${isNotes ? 'text-ds-accent' : 'text-ds-warning'}`} />
                <span>Notes Vault</span>
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
