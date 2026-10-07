import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Award, 
  Clock, 
  Plus, 
  Play, 
  Layers, 
  CheckCircle2, 
  TrendingUp, 
  X,
  AlertCircle
} from 'lucide-react';
import { 
  useMockTemplates, 
  useCreateMockTemplate, 
  useStartMockAttempt, 
  useMockTrend, 
  useSubjects 
} from '../hooks/useQueries';
import { BlueprintSection, CreateMockTemplateRequest } from '../api/types';

export const MockTestsPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: templates, isLoading, error } = useMockTemplates();
  const { data: trendData } = useMockTrend();
  const { data: subjects } = useSubjects();

  const createMutation = useCreateMockTemplate();
  const [startingMockId, setStartingMockId] = useState<string | null>(null);

  // Modal State
  const [isBuilderOpen, setIsBuilderOpen] = useState(false);
  const [templateTitle, setTemplateTitle] = useState('GATE CSE Full Length Mock');
  const [durationMinutes, setDurationMinutes] = useState(180);
  const [sections, setSections] = useState<BlueprintSection[]>([
    { subjectId: 'subj_general_aptitude', mcq: 5, msq: 2, nat: 3, marks: 1 },
    { subjectId: 'subj_algo', mcq: 6, msq: 2, nat: 2, marks: 2 },
  ]);

  const handleStartAttempt = async (mockId: string) => {
    try {
      setStartingMockId(mockId);
      // Explicitly trigger API call and navigate
      const response = await fetch(`/api/v1/mocks/${mockId}/attempts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) throw new Error('Failed to start attempt');
      const data = await response.json();
      navigate(`/mocks/attempts/${data.attemptId}`);
    } catch (err) {
      console.error(err);
      alert('Could not start mock attempt. Please ensure question bank is populated.');
    } finally {
      setStartingMockId(null);
    }
  };

  const handleAddSection = () => {
    const firstSubjId = subjects?.[0]?.id || 'subj_algo';
    setSections([...sections, { subjectId: firstSubjId, mcq: 4, msq: 2, nat: 2, marks: 2 }]);
  };

  const handleRemoveSection = (index: number) => {
    setSections(sections.filter((_, i) => i !== index));
  };

  const handleSectionChange = (index: number, field: keyof BlueprintSection, value: any) => {
    const updated = [...sections];
    updated[index] = { ...updated[index], [field]: value };
    setSections(updated);
  };

  // Live Calculations
  const totalQuestions = sections.reduce((sum, s) => sum + (s.mcq || 0) + (s.msq || 0) + (s.nat || 0), 0);
  const totalMarks = sections.reduce((sum, s) => {
    const count = (s.mcq || 0) + (s.msq || 0) + (s.nat || 0);
    return sum + count * (s.marks || 1);
  }, 0);

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!templateTitle.trim() || sections.length === 0) return;

    const payload: CreateMockTemplateRequest = {
      title: templateTitle.trim(),
      durationMinutes,
      totalMarks,
      sections,
      shuffle: true,
      questionPool: {
        curationStatus: ['ACCEPTED', 'UNREVIEWED'],
      },
    };

    try {
      await createMutation.mutateAsync(payload);
      setIsBuilderOpen(false);
    } catch (err) {
      console.error('Failed to create template', err);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto pb-12">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-ds-border pb-5">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ds-primary flex items-center gap-2">
            <Award className="w-6 h-6 text-ds-accent" />
            GATE Mock Tests
          </h1>
          <p className="text-sm text-ds-secondary mt-1">
            Official TCS iON simulated exam environment with GATE-accurate negative marking and blueprint assembly.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsBuilderOpen(true)}
          className="inline-flex items-center gap-2 px-4 py-2.5 rounded-ds bg-ds-accent text-ds-accent-contrast font-semibold text-sm shadow-sm hover:opacity-90 transition-all self-start"
        >
          <Plus className="w-4 h-4" />
          Create Mock Template
        </button>
      </div>

      {/* Score Trend Banner */}
      {trendData && trendData.length > 0 && (
        <div className="bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-ds-primary flex items-center gap-2">
              <TrendingUp className="w-4 h-4 text-ds-accent" />
              Score Trend Across Mock Attempts
            </h2>
            <span className="text-xs text-ds-muted">{trendData.length} completed attempts</span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-3">
            {trendData.slice(-6).map((item, idx) => (
              <div key={item.attemptId || idx} className="bg-ds-bg-sidebar border border-ds-border rounded-ds p-2.5 text-center">
                <span className="text-[10px] text-ds-muted block truncate">{item.mockTitle}</span>
                <span className="text-base font-bold text-ds-accent block mt-0.5">
                  {item.totalScore.toFixed(1)} <span className="text-[10px] text-ds-secondary font-normal">/ {item.maxScore}</span>
                </span>
                <span className="text-[10px] font-mono text-ds-success block mt-0.5">
                  {item.percentage.toFixed(1)}%
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Templates List */}
      <div>
        <h2 className="text-base font-semibold text-ds-primary mb-3">Available Mock Templates</h2>

        {isLoading ? (
          <div className="p-12 text-center text-ds-muted bg-ds-surface border border-ds-border rounded-ds">
            Loading mock templates...
          </div>
        ) : error ? (
          <div className="p-6 text-center text-ds-danger bg-ds-surface border border-ds-border rounded-ds flex items-center justify-center gap-2">
            <AlertCircle className="w-5 h-5" />
            Failed to load mock templates.
          </div>
        ) : !templates || templates.length === 0 ? (
          <div className="p-12 text-center bg-ds-surface border border-ds-border rounded-ds space-y-3">
            <Award className="w-10 h-10 text-ds-muted mx-auto" />
            <p className="text-sm text-ds-secondary">No mock templates found. Create your first blueprint to get started.</p>
            <button
              type="button"
              onClick={() => setIsBuilderOpen(true)}
              className="inline-flex items-center gap-2 px-3.5 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold"
            >
              <Plus className="w-3.5 h-3.5" />
              Create First Template
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {templates.map((tmpl) => (
              <div 
                key={tmpl.id} 
                className="bg-ds-surface border border-ds-border rounded-ds p-5 flex flex-col justify-between hover:border-ds-border-strong shadow-sm transition-all"
              >
                <div className="space-y-3">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-semibold text-base text-ds-primary tracking-tight">
                      {tmpl.title}
                    </h3>
                    <span className="text-xs px-2 py-0.5 rounded-ds bg-ds-surface-hover text-ds-secondary font-mono border border-ds-border">
                      {tmpl.mode}
                    </span>
                  </div>

                  <div className="flex items-center gap-4 text-xs text-ds-secondary">
                    <span className="flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-ds-muted" />
                      {tmpl.durationMinutes} mins
                    </span>
                    <span className="flex items-center gap-1.5">
                      <Layers className="w-3.5 h-3.5 text-ds-muted" />
                      {tmpl.totalQuestions} Questions
                    </span>
                    <span className="flex items-center gap-1.5 font-medium text-ds-primary">
                      {tmpl.totalMarks} Marks
                    </span>
                  </div>

                  {/* Section summary chips */}
                  <div className="flex flex-wrap gap-1.5 pt-1">
                    {tmpl.sections.map((sec, i) => (
                      <span 
                        key={i}
                        className="text-[11px] px-2 py-0.5 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-secondary"
                      >
                        {sec.subjectId.replace('subj_', '').replace('_', ' ')}: {sec.mcq + sec.msq + sec.nat}Q
                      </span>
                    ))}
                  </div>

                  {tmpl.bestScore !== null && tmpl.bestScore !== undefined && (
                    <div className="text-xs text-ds-success bg-ds-surface-hover px-2.5 py-1.5 rounded-ds border border-ds-border flex items-center justify-between">
                      <span>Personal Best</span>
                      <span className="font-bold font-mono">{tmpl.bestScore.toFixed(2)} marks</span>
                    </div>
                  )}
                </div>

                <div className="pt-5 mt-3 border-t border-ds-border flex items-center justify-between">
                  <span className="text-xs text-ds-muted">
                    {tmpl.attemptCount} {tmpl.attemptCount === 1 ? 'attempt' : 'attempts'}
                  </span>
                  <button
                    type="button"
                    onClick={() => handleStartAttempt(tmpl.id)}
                    disabled={startingMockId === tmpl.id}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-90 transition-all disabled:opacity-50"
                  >
                    <Play className="w-3.5 h-3.5 fill-current" />
                    {startingMockId === tmpl.id ? 'Assembling...' : 'Start Attempt'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Blueprint Builder Modal */}
      {isBuilderOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-ds-surface border border-ds-border rounded-ds max-w-2xl w-full p-6 shadow-xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-ds-border pb-3">
              <h3 className="font-bold text-lg text-ds-primary flex items-center gap-2">
                <Layers className="w-5 h-5 text-ds-accent" />
                Build Mock Test Blueprint
              </h3>
              <button
                type="button"
                onClick={() => setIsBuilderOpen(false)}
                className="p-1.5 rounded-ds hover:bg-ds-surface-hover text-ds-muted hover:text-ds-primary"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-2 space-y-1">
                  <label className="text-xs font-semibold text-ds-secondary block">Test Title</label>
                  <input
                    type="text"
                    value={templateTitle}
                    onChange={(e) => setTemplateTitle(e.target.value)}
                    className="w-full px-3 py-2 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-primary text-sm focus:outline-none focus:border-ds-accent"
                    required
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-ds-secondary block">Duration (Minutes)</label>
                  <input
                    type="number"
                    value={durationMinutes}
                    onChange={(e) => setDurationMinutes(parseInt(e.target.value) || 60)}
                    min={10}
                    max={360}
                    className="w-full px-3 py-2 rounded-ds bg-ds-bg-sidebar border border-ds-border text-ds-primary text-sm focus:outline-none focus:border-ds-accent"
                    required
                  />
                </div>
              </div>

              {/* Sections Table */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-ds-secondary">Sections & Weightage</span>
                  <button
                    type="button"
                    onClick={handleAddSection}
                    className="text-xs font-semibold text-ds-accent hover:underline flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Section
                  </button>
                </div>

                <div className="space-y-2.5 max-h-64 overflow-y-auto pr-1">
                  {sections.map((sec, idx) => (
                    <div key={idx} className="p-3 bg-ds-bg-sidebar border border-ds-border rounded-ds grid grid-cols-12 gap-2 items-center">
                      <div className="col-span-4">
                        <select
                          value={sec.subjectId}
                          onChange={(e) => handleSectionChange(idx, 'subjectId', e.target.value)}
                          className="w-full text-xs px-2 py-1.5 rounded-ds bg-ds-surface border border-ds-border text-ds-primary"
                        >
                          {subjects?.map((s) => (
                            <option key={s.id} value={s.id}>{s.name}</option>
                          )) || (
                            <option value={sec.subjectId}>{sec.subjectId}</option>
                          )}
                        </select>
                      </div>

                      <div className="col-span-2">
                        <input
                          type="number"
                          placeholder="MCQ"
                          value={sec.mcq}
                          onChange={(e) => handleSectionChange(idx, 'mcq', parseInt(e.target.value) || 0)}
                          className="w-full text-xs px-2 py-1.5 rounded-ds bg-ds-surface border border-ds-border text-ds-primary"
                          min={0}
                        />
                      </div>

                      <div className="col-span-2">
                        <input
                          type="number"
                          placeholder="MSQ"
                          value={sec.msq}
                          onChange={(e) => handleSectionChange(idx, 'msq', parseInt(e.target.value) || 0)}
                          className="w-full text-xs px-2 py-1.5 rounded-ds bg-ds-surface border border-ds-border text-ds-primary"
                          min={0}
                        />
                      </div>

                      <div className="col-span-2">
                        <input
                          type="number"
                          placeholder="NAT"
                          value={sec.nat}
                          onChange={(e) => handleSectionChange(idx, 'nat', parseInt(e.target.value) || 0)}
                          className="w-full text-xs px-2 py-1.5 rounded-ds bg-ds-surface border border-ds-border text-ds-primary"
                          min={0}
                        />
                      </div>

                      <div className="col-span-1">
                        <input
                          type="number"
                          placeholder="Marks"
                          value={sec.marks || 1}
                          onChange={(e) => handleSectionChange(idx, 'marks', parseInt(e.target.value) || 1)}
                          className="w-full text-xs px-1.5 py-1.5 rounded-ds bg-ds-surface border border-ds-border text-ds-primary text-center"
                          min={1}
                          max={2}
                        />
                      </div>

                      <div className="col-span-1 text-right">
                        <button
                          type="button"
                          onClick={() => handleRemoveSection(idx)}
                          disabled={sections.length === 1}
                          className="p-1 rounded-ds text-ds-danger hover:bg-ds-surface-hover disabled:opacity-30"
                        >
                          <X className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Blueprint Summary Footer */}
              <div className="bg-ds-surface-hover border border-ds-border rounded-ds p-3 flex items-center justify-between text-xs">
                <span className="text-ds-secondary">
                  Total Questions: <strong className="text-ds-primary font-mono">{totalQuestions}</strong>
                </span>
                <span className="text-ds-secondary">
                  Total Marks: <strong className="text-ds-accent font-mono">{totalMarks}</strong>
                </span>
                <span className="text-ds-secondary">
                  Negative Scheme: <strong className="text-ds-danger">GATE (−⅓)</strong>
                </span>
              </div>

              <div className="pt-2 flex items-center justify-end gap-2.5">
                <button
                  type="button"
                  onClick={() => setIsBuilderOpen(false)}
                  className="px-4 py-2 rounded-ds border border-ds-border text-ds-secondary hover:text-ds-primary text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending || totalQuestions === 0}
                  className="px-4 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:opacity-90 disabled:opacity-50"
                >
                  {createMutation.isPending ? 'Creating...' : 'Save Blueprint Template'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
