import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { 
  Play, 
  Sparkles, 
  AlertCircle, 
  BookOpen, 
  Sliders, 
  CheckCircle2, 
  Clock, 
  Flame,
  ArrowRight
} from 'lucide-react';
import { 
  useSubjects, 
  useTopics, 
  useCreatePracticeSession, 
  useMistakes, 
  useTopicMasteryStats 
} from '../hooks/useQueries';
import { QuestionType } from '../api/types';

export const PracticePage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialSubject = searchParams.get('subject') || '';
  const initialTopic = searchParams.get('topic') || searchParams.get('topicId') || '';
  const initialYear = searchParams.get('year') || 'ALL';

  const [selectedSubjectId, setSelectedSubjectId] = useState<string>(initialSubject);
  const [selectedTopicId, setSelectedTopicId] = useState<string>(initialTopic);
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [selectedMarks, setSelectedMarks] = useState<string>('ALL');
  const [selectedCuration, setSelectedCuration] = useState<string>('ALL');
  const [selectedYear, setSelectedYear] = useState<string>(initialYear);
  const [questionCount, setQuestionCount] = useState<number>(10);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { data: subjects = [], isLoading: loadingSubjects } = useSubjects();
  const { data: topics = [], isLoading: loadingTopics } = useTopics(selectedSubjectId || undefined);
  const { data: dueMistakes } = useMistakes({ due: true, resolved: false, size: 1 });
  const { data: masteryStats = [] } = useTopicMasteryStats();

  useEffect(() => {
    if (initialSubject && subjects.length > 0) {
      const match = subjects.find((s) => s.id === initialSubject || s.slug === initialSubject);
      if (match) {
        setSelectedSubjectId(match.id);
      } else {
        setSelectedSubjectId(initialSubject);
      }
    }
  }, [initialSubject, subjects]);

  useEffect(() => {
    const y = searchParams.get('year');
    if (y) {
      setSelectedYear(y);
    }
  }, [searchParams]);

  const createSessionMutation = useCreatePracticeSession();

  const handleSubjectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedSubjectId(e.target.value);
    setSelectedTopicId('');
  };

  const handleStartPractice = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const clampedCount = Math.max(1, Math.min(50, questionCount));

    try {
      const response = await createSessionMutation.mutateAsync({
        subjectId: selectedSubjectId || undefined,
        topicId: selectedTopicId || undefined,
        questionType: selectedType !== 'ALL' ? (selectedType as QuestionType) : undefined,
        marks: selectedMarks !== 'ALL' ? parseInt(selectedMarks, 10) : undefined,
        curationStatus: selectedCuration !== 'ALL' ? selectedCuration : undefined,
        year: selectedYear !== 'ALL' ? parseInt(selectedYear, 10) : undefined,
        count: clampedCount,
      });

      // Save questions in session storage or navigate directly
      sessionStorage.setItem(`practice_session_${response.attemptId}`, JSON.stringify(response));
      navigate(`/practice/sessions/${response.attemptId}`);
    } catch (err: any) {
      setErrorMessage(err?.detail || err?.message || 'Failed to start practice session');
    }
  };

  const handleQuickPreset = async (presetSubjectId: string, presetTopicId?: string) => {
    setErrorMessage(null);
    try {
      const response = await createSessionMutation.mutateAsync({
        subjectId: presetSubjectId,
        topicId: presetTopicId,
        count: 10,
      });
      sessionStorage.setItem(`practice_session_${response.attemptId}`, JSON.stringify(response));
      navigate(`/practice/sessions/${response.attemptId}`);
    } catch (err: any) {
      setErrorMessage(err?.detail || err?.message || 'Failed to start practice session');
    }
  };

  const dueCount = dueMistakes?.totalElements || 0;

  return (
    <div className="max-w-5xl mx-auto space-y-8 pb-12">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-ds-primary flex items-center gap-2.5">
            <Sparkles className="w-6 h-6 text-ds-accent" />
            <span>Practice Clusters</span>
          </h1>
          <p className="text-sm text-ds-muted mt-1">
            Targeted problem solving with immediate server-graded feedback, topic mastery updates, and zero negative marking.
          </p>
        </div>

        {/* Due Mistakes Review Card */}
        {dueCount > 0 && (
          <div className="bg-ds-surface border border-ds-border-strong rounded-ds p-4 flex items-center gap-4 shadow-sm">
            <div className="w-10 h-10 rounded-ds bg-ds-danger-soft border border-ds-danger/30 flex items-center justify-center text-ds-danger shrink-0">
              <AlertCircle className="w-5 h-5" />
            </div>
            <div>
              <div className="text-xs font-semibold text-ds-primary">
                {dueCount} {dueCount === 1 ? 'Mistake' : 'Mistakes'} Due for Review
              </div>
              <div className="text-[11px] text-ds-muted">Spaced interval queue ready</div>
            </div>
            <button
              type="button"
              onClick={() => navigate('/mistakes?due=true')}
              className="px-3 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium hover:opacity-90 transition-opacity ml-2"
            >
              Review
            </button>
          </div>
        )}
      </div>

      {errorMessage && (
        <div className="bg-ds-danger-soft border border-ds-danger/30 text-ds-danger px-4 py-3 rounded-ds text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Main Configuration Card */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 md:p-8 space-y-6 shadow-sm">
        <div className="flex items-center gap-2 border-b border-ds-border pb-4">
          <Sliders className="w-5 h-5 text-ds-accent" />
          <h2 className="text-base font-semibold text-ds-primary">Configure Session</h2>
        </div>

        <form onSubmit={handleStartPractice} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
            {/* Subject Select */}
            <div className="space-y-1.5">
              <label htmlFor="subject-select" className="block text-xs font-medium text-ds-secondary">
                Subject
              </label>
              <select
                id="subject-select"
                aria-label="Subject"
                value={selectedSubjectId}
                onChange={handleSubjectChange}
                disabled={loadingSubjects}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent transition-colors"
              >
                <option value="">All Subjects (Comprehensive Mix)</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
            </div>

            {/* Topic Select (Cascading) */}
            <div className="space-y-1.5">
              <label htmlFor="topic-select" className="block text-xs font-medium text-ds-secondary">
                Topic
              </label>
              <select
                id="topic-select"
                aria-label="Topic"
                value={selectedTopicId}
                onChange={(e) => setSelectedTopicId(e.target.value)}
                disabled={!selectedSubjectId || loadingTopics}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent transition-colors disabled:opacity-50"
              >
                <option value="">
                  {selectedSubjectId ? 'All Topics in Subject' : 'Select a Subject first'}
                </option>
                {topics.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} (Lvl {t.masteryLevel}/5)
                  </option>
                ))}
              </select>
            </div>

            {/* Question Type */}
            <div className="space-y-1.5">
              <label htmlFor="type-select" className="block text-xs font-medium text-ds-secondary">
                Question Type
              </label>
              <select
                id="type-select"
                aria-label="Question Type"
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent transition-colors"
              >
                <option value="ALL">All Types (MCQ, MSQ, NAT)</option>
                <option value="MCQ">Multiple Choice (MCQ)</option>
                <option value="MSQ">Multiple Select (MSQ)</option>
                <option value="NAT">Numerical Answer (NAT)</option>
              </select>
            </div>

            {/* Marks */}
            <div className="space-y-1.5">
              <label htmlFor="marks-select" className="block text-xs font-medium text-ds-secondary">
                Marks Weight
              </label>
              <select
                id="marks-select"
                aria-label="Marks Weight"
                value={selectedMarks}
                onChange={(e) => setSelectedMarks(e.target.value)}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent transition-colors"
              >
                <option value="ALL">All Marks (1 & 2 Marks)</option>
                <option value="1">1 Mark Questions (Speed & Foundations)</option>
                <option value="2">2 Marks Questions (In-depth Problems)</option>
              </select>
            </div>

            {/* Curation Filter */}
            <div className="space-y-1.5">
              <label htmlFor="curation-select" className="block text-xs font-medium text-ds-secondary">
                Curation Status
              </label>
              <select
                id="curation-select"
                aria-label="Curation Status"
                value={selectedCuration}
                onChange={(e) => setSelectedCuration(e.target.value)}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent transition-colors"
              >
                <option value="ALL">All Questions</option>
                <option value="ACCEPTED">Accepted Only (Verified Quality)</option>
                <option value="UNREVIEWED">Unreviewed Included</option>
              </select>
            </div>

            {/* Exam Year Filter (PYP) */}
            <div className="space-y-1.5">
              <label htmlFor="year-select" className="block text-xs font-medium text-ds-secondary">
                Exam Year (PYP)
              </label>
              <select
                id="year-select"
                aria-label="Exam Year"
                data-testid="year-select"
                value={selectedYear}
                onChange={(e) => setSelectedYear(e.target.value)}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary focus:outline-none focus:border-ds-accent transition-colors"
              >
                <option value="ALL">All Years (Any Sourced Question)</option>
                {Array.from({ length: 18 }, (_, i) => 2025 - i).map((yr) => (
                  <option key={yr} value={yr.toString()}>
                    GATE {yr}
                  </option>
                ))}
              </select>
            </div>

            {/* Question Count */}
            <div className="space-y-1.5">
              <div className="flex justify-between items-center">
                <label htmlFor="count-input" className="block text-xs font-medium text-ds-secondary">
                  Questions Count (1–50)
                </label>
                <span className="text-xs font-mono text-ds-accent font-semibold">{questionCount}</span>
              </div>
              <input
                id="count-input"
                aria-label="Question Count"
                type="number"
                min={1}
                max={50}
                value={questionCount}
                onChange={(e) => {
                  const val = parseInt(e.target.value, 10);
                  if (!isNaN(val)) {
                    setQuestionCount(Math.max(1, Math.min(50, val)));
                  }
                }}
                className="w-full bg-ds-bg-base border border-ds-border rounded-ds px-3 py-2 text-sm text-ds-primary font-mono focus:outline-none focus:border-ds-accent transition-colors"
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-ds-border">
            <button
              type="submit"
              disabled={createSessionMutation.isPending}
              className="flex items-center gap-2 px-6 py-2.5 rounded-ds bg-ds-accent text-ds-accent-contrast font-medium text-sm hover:opacity-95 active:scale-[0.98] transition-all disabled:opacity-50 shadow-md"
            >
              <Play className="w-4 h-4 fill-current" />
              <span>{createSessionMutation.isPending ? 'Assembling Session...' : 'Start Practice Session'}</span>
            </button>
          </div>
        </form>
      </div>

      {/* Recommended Quick Presets */}
      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-ds-primary flex items-center gap-2">
          <Flame className="w-4 h-4 text-ds-accent" />
          <span>Quick Practice Clusters</span>
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div
            onClick={() => handleQuickPreset('subj_algo')}
            className="bg-ds-surface border border-ds-border rounded-ds p-4 hover:border-ds-accent cursor-pointer transition-all space-y-2 group"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-ds-accent font-mono uppercase">Algorithms</span>
              <ArrowRight className="w-4 h-4 text-ds-muted group-hover:text-ds-accent transition-colors" />
            </div>
            <div className="text-sm font-semibold text-ds-primary">Core Algorithms & Complexity</div>
            <p className="text-xs text-ds-muted">Asymptotic analysis, greedy, dynamic programming & divide and conquer.</p>
          </div>

          <div
            onClick={() => handleQuickPreset('subj_os')}
            className="bg-ds-surface border border-ds-border rounded-ds p-4 hover:border-ds-accent cursor-pointer transition-all space-y-2 group"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-ds-info font-mono uppercase">Systems</span>
              <ArrowRight className="w-4 h-4 text-ds-muted group-hover:text-ds-accent transition-colors" />
            </div>
            <div className="text-sm font-semibold text-ds-primary">Operating Systems & Virtual Memory</div>
            <p className="text-xs text-ds-muted">Paging, synchronization, scheduling & deadlocks drill.</p>
          </div>

          <div
            onClick={() => handleQuickPreset('subj_discrete_math')}
            className="bg-ds-surface border border-ds-border rounded-ds p-4 hover:border-ds-accent cursor-pointer transition-all space-y-2 group"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-ds-success font-mono uppercase">Discrete Math</span>
              <ArrowRight className="w-4 h-4 text-ds-muted group-hover:text-ds-accent transition-colors" />
            </div>
            <div className="text-sm font-semibold text-ds-primary">Propositional Logic & Graph Theory</div>
            <p className="text-xs text-ds-muted">Predicates, trees, relations & combinatorics fundamentals.</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PracticePage;
