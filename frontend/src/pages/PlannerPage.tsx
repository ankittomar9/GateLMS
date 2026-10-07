import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  CalendarRange,
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Play,
  RotateCcw,
  CheckCircle2,
  Circle,
  AlertCircle,
  HelpCircle,
  Info,
  Calendar,
  Layers,
  Clock,
  ArrowRight
} from 'lucide-react';
import {
  useStudyPlans,
  useStudyPlan,
  useGeneratePlan,
  useRegeneratePlan,
  useUpdatePlanItemStatus,
  useSubjects,
  useReadiness
} from '../hooks/useQueries';
import {
  PlanStrategy,
  GeneratePlanRequest,
  PlanItemDto,
  StudyPlanDetailDto
} from '../api/types';

export const PlannerPage: React.FC = () => {
  const navigate = useNavigate();

  // Queries
  const { data: plans = [], isLoading: loadingPlans } = useStudyPlans();
  const [selectedPlanId, setSelectedPlanId] = useState<string | null>(null);

  // Active plan selection: either user selected or first plan
  const activePlanId = selectedPlanId || plans[0]?.id;
  const { data: activePlan, isLoading: loadingPlanDetail } = useStudyPlan(activePlanId);

  const { data: subjects = [] } = useSubjects();
  const { data: readinessData } = useReadiness();

  // Mutations
  const generateMutation = useGeneratePlan();
  const regenerateMutation = useRegeneratePlan();
  const updateStatusMutation = useUpdatePlanItemStatus();

  // Generation form state
  const [isCreating, setIsCreating] = useState(false);
  const [formTitle, setFormTitle] = useState('');
  const [formWeeks, setFormWeeks] = useState(16);
  const [formStrategy, setFormStrategy] = useState<PlanStrategy>('WEIGHTED');
  const [formWeeklyQuestions, setFormWeeklyQuestions] = useState(150);
  const [formWeeklyMinutes, setFormWeeklyMinutes] = useState(600);
  const [formError, setFormError] = useState<string | null>(null);

  // Calendar state
  const [calendarMonth, setCalendarMonth] = useState<Date>(new Date());
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );

  // Popover state
  const [showReasoning, setShowReasoning] = useState(false);
  const [showRegenConfirm, setShowRegenConfirm] = useState(false);

  // Group items by date for the active plan
  const itemsByDate = useMemo(() => {
    const map: Record<string, PlanItemDto[]> = {};
    if (!activePlan?.items) return map;
    for (const item of activePlan.items) {
      if (!map[item.planDate]) {
        map[item.planDate] = [];
      }
      map[item.planDate].push(item);
    }
    return map;
  }, [activePlan]);

  // Subject share preview computation for form preview
  const previewShares = useMemo(() => {
    if (!subjects || subjects.length === 0) return [];
    const rMap = new Map(readinessData?.perSubject?.map((s) => [s.subjectId, s.readiness / 100.0]) || []);
    return subjects.map((s) => {
      const w = s.avgWeightage ?? s.weightageAvg ?? 10.0;
      const r = rMap.get(s.id) ?? 0.0;
      let share = w;
      if (formStrategy === 'WEIGHTED') {
        share = w * (1.0 + 0.5 * (1.0 - r));
      } else if (formStrategy === 'WEAKNESS_FIRST') {
        share = w * (1.0 + (1.0 - r));
      }
      return {
        id: s.id,
        code: s.code || s.slug?.split('-').map(p => p[0]).join('').toUpperCase() || s.id,
        name: s.name,
        share: Math.round(share * 10) / 10,
        readinessPct: Math.round(r * 100),
      };
    }).sort((a, b) => b.share - a.share);
  }, [subjects, readinessData, formStrategy]);

  // Handle Generate Submit
  const handleGenerateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (formWeeks < 1 || formWeeks > 52) {
      setFormError('Weeks must be between 1 and 52');
      return;
    }

    try {
      const result = await generateMutation.mutateAsync({
        title: formTitle.trim() || undefined,
        weeks: formWeeks,
        strategy: formStrategy,
        weeklyTargetQuestions: formWeeklyQuestions,
        weeklyTargetMinutes: formWeeklyMinutes,
      });
      setSelectedPlanId(result.id);
      setIsCreating(false);
    } catch (err: any) {
      setFormError(err.message || 'Failed to generate study plan');
    }
  };

  // Handle Regenerate Confirm
  const handleRegenerate = async () => {
    if (!activePlan) return;
    try {
      await regenerateMutation.mutateAsync(activePlan.id);
      setShowRegenConfirm(false);
    } catch (err: any) {
      alert(`Regeneration failed: ${err.message}`);
    }
  };

  // Handle Status Toggle
  const handleToggleStatus = async (item: PlanItemDto) => {
    const nextStatus = item.status === 'DONE' ? 'OPEN' : 'DONE';
    await updateStatusMutation.mutateAsync({
      itemId: item.id,
      req: { status: nextStatus },
    });
  };

  // Calendar Day cell computation
  const calendarDays = useMemo(() => {
    const year = calendarMonth.getFullYear();
    const month = calendarMonth.getMonth();
    const firstDay = new Date(year, month, 1).getDay(); // 0 is Sunday
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const days: { dateStr: string; dayNum: number; isCurrentMonth: boolean; isRestDay: boolean }[] = [];

    // Prefix empty/prev days
    for (let i = 0; i < firstDay; i++) {
      days.push({ dateStr: '', dayNum: 0, isCurrentMonth: false, isRestDay: i === 0 });
    }

    // Month days
    for (let d = 1; d <= daysInMonth; d++) {
      const dateObj = new Date(year, month, d);
      const isRestDay = dateObj.getDay() === 0; // Sunday rest day
      const yyyy = dateObj.getFullYear();
      const mm = String(dateObj.getMonth() + 1).padStart(2, '0');
      const dd = String(dateObj.getDate()).padStart(2, '0');
      const dateStr = `${yyyy}-${mm}-${dd}`;
      days.push({ dateStr, dayNum: d, isCurrentMonth: true, isRestDay });
    }

    return days;
  }, [calendarMonth]);

  const selectedDayItems = itemsByDate[selectedDate] || [];

  return (
    <div className="space-y-6" data-testid="planner-page">
      {/* Top Banner */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 relative overflow-hidden shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <CalendarRange className="w-5 h-5 text-ds-accent" />
              <h1 className="text-xl font-bold text-ds-primary tracking-tight">GATE Study Planner</h1>
            </div>
            <p className="text-ds-secondary text-sm max-w-2xl">
              Weightage-aware roadmap dynamically paced by syllabus weightage × weakness multipliers. Activity across practice, mocks, and mistake reviews auto-advances your plan.
            </p>
          </div>

          <div className="flex items-center gap-3 shrink-0">
            {plans.length > 0 && !isCreating && (
              <button
                type="button"
                data-testid="regenerate-plan-btn"
                onClick={() => setShowRegenConfirm(true)}
                className="flex items-center gap-1.5 px-3.5 py-2 bg-ds-surface-raised hover:bg-ds-surface text-ds-primary border border-ds-border font-medium rounded-ds text-xs transition-colors"
              >
                <RotateCcw className="w-3.5 h-3.5 text-ds-muted" />
                <span>Regenerate Forward</span>
              </button>
            )}

            <button
              type="button"
              data-testid="toggle-create-plan-btn"
              onClick={() => setIsCreating(!isCreating)}
              className="flex items-center gap-2 px-4 py-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast font-semibold rounded-ds text-sm transition-all shadow-md"
            >
              <Sparkles className="w-4 h-4" />
              <span>{isCreating ? 'View Plans' : 'Generate New Plan'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Confirmation Modal for Regeneration */}
      {showRegenConfirm && (
        <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
          <div 
            data-testid="regen-confirm-modal"
            className="bg-ds-surface border border-ds-border p-6 rounded-ds max-w-md w-full shadow-xl space-y-4"
          >
            <div className="flex items-center gap-2 text-ds-accent">
              <RotateCcw className="w-5 h-5" />
              <h3 className="font-bold text-base text-ds-primary">Confirm Plan Regeneration</h3>
            </div>
            <p className="text-xs text-ds-secondary leading-relaxed">
              This will recalculate schedule items from <strong>today forward</strong> using fresh readiness telemetry. All previously completed (DONE) items will be preserved safely.
            </p>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                data-testid="regen-cancel-btn"
                onClick={() => setShowRegenConfirm(false)}
                className="px-3.5 py-1.5 text-xs text-ds-muted hover:text-ds-primary font-medium"
              >
                Cancel
              </button>
              <button
                type="button"
                data-testid="regen-confirm-btn"
                onClick={handleRegenerate}
                disabled={regenerateMutation.isPending}
                className="px-4 py-1.5 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-semibold rounded-ds shadow-sm"
              >
                {regenerateMutation.isPending ? 'Regenerating...' : 'Confirm & Regenerate'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Generation Form */}
      {isCreating && (
        <div data-testid="plan-generator-form-card" className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm space-y-6">
          <div className="border-b border-ds-border pb-3">
            <h2 className="text-base font-bold text-ds-primary">Generate a Study Plan</h2>
            <p className="text-xs text-ds-muted mt-0.5">
              Specify your timeframe and target pace. The engine analyzes topic questions in the bank and excludes unpracticed topics.
            </p>
          </div>

          {formError && (
            <div data-testid="form-error-banner" className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger px-4 py-2.5 rounded-ds text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <form onSubmit={handleGenerateSubmit} noValidate className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label className="block text-xs font-semibold text-ds-primary mb-1.5">
                  Plan Title (Optional)
                </label>
                <input
                  type="text"
                  data-testid="input-plan-title"
                  placeholder="e.g., 16-Week GATE CSE Ramp-up"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className="w-full px-3 py-2 bg-ds-surface-raised border border-ds-border rounded-ds text-xs text-ds-primary focus:outline-none focus:border-ds-accent font-sans"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-ds-primary mb-1.5">
                  Duration in Weeks (1–52)
                </label>
                <input
                  type="number"
                  data-testid="input-plan-weeks"
                  min="1"
                  max="52"
                  value={formWeeks}
                  onChange={(e) => setFormWeeks(parseInt(e.target.value, 10) || 0)}
                  className="w-full px-3 py-2 bg-ds-surface-raised border border-ds-border rounded-ds text-xs text-ds-primary focus:outline-none focus:border-ds-accent font-mono"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-ds-primary mb-1.5">
                  Weekly Target Questions
                </label>
                <input
                  type="number"
                  data-testid="input-target-questions"
                  min="30"
                  max="500"
                  step="10"
                  value={formWeeklyQuestions}
                  onChange={(e) => setFormWeeklyQuestions(parseInt(e.target.value, 10) || 0)}
                  className="w-full px-3 py-2 bg-ds-surface-raised border border-ds-border rounded-ds text-xs text-ds-primary focus:outline-none focus:border-ds-accent font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-ds-primary mb-1.5">
                  Weekly Target Minutes
                </label>
                <input
                  type="number"
                  data-testid="input-target-minutes"
                  min="60"
                  max="3000"
                  step="30"
                  value={formWeeklyMinutes}
                  onChange={(e) => setFormWeeklyMinutes(parseInt(e.target.value, 10) || 0)}
                  className="w-full px-3 py-2 bg-ds-surface-raised border border-ds-border rounded-ds text-xs text-ds-primary focus:outline-none focus:border-ds-accent font-mono"
                />
              </div>
            </div>

            {/* Strategy Selection Radios */}
            <div className="space-y-3">
              <label className="block text-xs font-semibold text-ds-primary">
                Optimization Strategy
              </label>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <label 
                  data-testid="strategy-radio-weighted"
                  className={`p-3.5 rounded-ds border cursor-pointer transition-all ${
                    formStrategy === 'WEIGHTED' 
                      ? 'bg-ds-surface-raised border-ds-accent ring-1 ring-ds-accent/30' 
                      : 'bg-ds-surface border-ds-border hover:bg-ds-surface-raised'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1.5">
                    <input
                      type="radio"
                      name="strategy"
                      value="WEIGHTED"
                      checked={formStrategy === 'WEIGHTED'}
                      onChange={() => setFormStrategy('WEIGHTED')}
                      className="text-ds-accent focus:ring-ds-accent"
                    />
                    <span className="text-xs font-bold text-ds-primary">WEIGHTED (Recommended)</span>
                  </div>
                  <p className="text-[11px] text-ds-secondary leading-snug">
                    Syllabus weightage multiplied by current weakness multiplier. Balances high-scoring subjects with gaps.
                  </p>
                </label>

                <label 
                  data-testid="strategy-radio-weakness-first"
                  className={`p-3.5 rounded-ds border cursor-pointer transition-all ${
                    formStrategy === 'WEAKNESS_FIRST' 
                      ? 'bg-ds-surface-raised border-ds-accent ring-1 ring-ds-accent/30' 
                      : 'bg-ds-surface border-ds-border hover:bg-ds-surface-raised'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1.5">
                    <input
                      type="radio"
                      name="strategy"
                      value="WEAKNESS_FIRST"
                      checked={formStrategy === 'WEAKNESS_FIRST'}
                      onChange={() => setFormStrategy('WEAKNESS_FIRST')}
                      className="text-ds-accent focus:ring-ds-accent"
                    />
                    <span className="text-xs font-bold text-ds-primary">WEAKNESS_FIRST</span>
                  </div>
                  <p className="text-[11px] text-ds-secondary leading-snug">
                    Sorts by lowest readiness score and front-loads capacity to weak subjects first.
                  </p>
                </label>

                <label 
                  data-testid="strategy-radio-linear"
                  className={`p-3.5 rounded-ds border cursor-pointer transition-all ${
                    formStrategy === 'LINEAR' 
                      ? 'bg-ds-surface-raised border-ds-accent ring-1 ring-ds-accent/30' 
                      : 'bg-ds-surface border-ds-border hover:bg-ds-surface-raised'
                  }`}
                >
                  <div className="flex items-center gap-2 mb-1.5">
                    <input
                      type="radio"
                      name="strategy"
                      value="LINEAR"
                      checked={formStrategy === 'LINEAR'}
                      onChange={() => setFormStrategy('LINEAR')}
                      className="text-ds-accent focus:ring-ds-accent"
                    />
                    <span className="text-xs font-bold text-ds-primary">LINEAR</span>
                  </div>
                  <p className="text-[11px] text-ds-secondary leading-snug">
                    Strict syllabus weightage distribution without weakness amplification.
                  </p>
                </label>
              </div>
            </div>

            {/* Preview Section */}
            <div data-testid="strategy-preview-section" className="space-y-3 bg-ds-surface-raised p-4 rounded-ds border border-ds-border">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-ds-primary flex items-center gap-1.5">
                  <Layers className="w-3.5 h-3.5 text-ds-accent" />
                  Subject Distribution Preview
                </span>
                <span className="text-ds-muted font-mono text-[11px]">
                  Estimated relative shares
                </span>
              </div>

              <div data-testid="preview-chips-list" className="flex flex-wrap gap-2">
                {previewShares.map((ps) => (
                  <span
                    key={ps.id}
                    data-testid={`preview-chip-${ps.id}`}
                    className="px-2.5 py-1 rounded bg-ds-surface border border-ds-border text-xs font-mono flex items-center gap-1.5 shadow-sm"
                  >
                    <span className="font-semibold text-ds-primary">{ps.code}</span>
                    <span className="text-ds-accent font-bold">{ps.share}</span>
                  </span>
                ))}
              </div>

              <div className="text-[11px] text-ds-muted flex items-center gap-1 pt-1">
                <Info className="w-3.5 h-3.5 text-ds-muted shrink-0" />
                <span>Note: Topics with zero questions in the question bank are automatically excluded.</span>
              </div>
            </div>

            {/* Form Actions */}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setIsCreating(false)}
                className="px-4 py-2 text-xs font-medium text-ds-muted hover:text-ds-primary"
              >
                Cancel
              </button>
              <button
                type="submit"
                data-testid="submit-generate-plan-btn"
                disabled={generateMutation.isPending}
                className="px-6 py-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast font-bold rounded-ds text-xs transition-all shadow-md"
              >
                {generateMutation.isPending ? 'Generating Plan...' : 'Generate Plan'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* When No Plans Exist */}
      {!isCreating && plans.length === 0 && !loadingPlans && (
        <div data-testid="planner-empty-state" className="bg-ds-surface border border-ds-border rounded-ds p-12 text-center shadow-sm space-y-4">
          <div className="w-12 h-12 rounded-full bg-ds-accent-soft text-ds-accent flex items-center justify-center mx-auto">
            <CalendarRange className="w-6 h-6" />
          </div>
          <div className="space-y-1 max-w-md mx-auto">
            <h2 className="text-lg font-bold text-ds-primary">No Study Plans Generated Yet</h2>
            <p className="text-xs text-ds-secondary leading-relaxed">
              Create your personalized study roadmap based on GATE CSE syllabus weightage and weakness telemetry.
            </p>
          </div>
          <button
            type="button"
            data-testid="empty-generate-btn"
            onClick={() => setIsCreating(true)}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast font-bold rounded-ds text-xs shadow-md transition-all"
          >
            <Sparkles className="w-4 h-4" />
            <span>Generate Your First Plan</span>
          </button>
        </div>
      )}

      {/* Main Plan View (Calendar + Day Detail) */}
      {!isCreating && activePlan && (
        <div data-testid="active-plan-container" className="space-y-6">
          {/* Plan Header Card */}
          <div className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm space-y-4">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-ds-accent-soft text-ds-accent border border-ds-accent/30">
                    {activePlan.strategy}
                  </span>
                  <span className="text-xs font-mono text-ds-muted">
                    {activePlan.startDate} to {activePlan.endDate} ({activePlan.weeks} Weeks)
                  </span>
                </div>
                <h2 className="text-xl font-bold text-ds-primary tracking-tight">
                  {activePlan.title}
                </h2>
              </div>

              {/* "Why this plan?" button */}
              <button
                type="button"
                data-testid="why-this-plan-btn"
                onClick={() => setShowReasoning(!showReasoning)}
                className="flex items-center gap-1.5 px-3 py-1.5 bg-ds-surface-raised hover:bg-ds-surface text-ds-secondary hover:text-ds-primary border border-ds-border rounded-ds text-xs font-medium transition-colors self-start md:self-auto"
              >
                <HelpCircle className="w-3.5 h-3.5 text-ds-accent" />
                <span>Why this plan?</span>
              </button>
            </div>

            {/* Overall Progress Bar */}
            <div data-testid="plan-overall-progress" className="space-y-2 bg-ds-surface-raised p-4 rounded-ds border border-ds-border">
              <div className="flex items-center justify-between text-xs font-mono">
                <div className="flex items-center gap-3">
                  <span className="text-ds-primary font-bold">
                    {activePlan.totalQuestionsDone} / {activePlan.totalQuestionsTarget} Questions
                  </span>
                  <span className="text-ds-muted">·</span>
                  <span className="text-ds-secondary flex items-center gap-1">
                    <Clock className="w-3.5 h-3.5 text-ds-muted" />
                    {activePlan.totalMinutesDone} / {activePlan.totalMinutesTarget} Mins
                  </span>
                </div>
                <span className="text-ds-accent font-semibold">{activePlan.progressPercentage}% Complete</span>
              </div>
              <div className="w-full h-2 bg-ds-surface rounded-full overflow-hidden border border-ds-border">
                <div
                  className="h-full bg-ds-accent rounded-full transition-all duration-300"
                  style={{ width: `${Math.min(100, activePlan.progressPercentage)}%` }}
                />
              </div>
            </div>

            {/* Reasoning Popover Drawer */}
            {showReasoning && activePlan.generatedReasoning && (
              <div 
                data-testid="why-this-plan-popover" 
                className="bg-ds-surface-raised border border-ds-border rounded-ds p-4 text-xs space-y-3 font-mono"
              >
                <div className="flex items-center justify-between border-b border-ds-border pb-2">
                  <span className="font-bold text-ds-primary flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-ds-accent" />
                    Algorithmic Reasoning Receipt
                  </span>
                  <span className="text-ds-muted text-[11px]">
                    Mode: {String(activePlan.generatedReasoning.mode || 'standard')}
                  </span>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-ds-secondary">
                  <div>
                    <span className="text-ds-muted block mb-1">Execution Mode:</span>
                    <p className="text-ds-primary">{String(activePlan.generatedReasoning.reason || 'Active plan calculation')}</p>
                  </div>
                  <div>
                    <span className="text-ds-muted block mb-1">Excluded Topics:</span>
                    <p className="text-ds-primary">
                      {Array.isArray(activePlan.generatedReasoning.excludedTopics) 
                        ? `${activePlan.generatedReasoning.excludedTopics.length} topics excluded (0 questions in bank)`
                        : '0'}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Calendar & Day Details Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            {/* Calendar Column */}
            <div data-testid="calendar-card" className="lg:col-span-7 bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-4">
              <div className="flex items-center justify-between border-b border-ds-border pb-3">
                <div className="flex items-center gap-2">
                  <Calendar className="w-4 h-4 text-ds-accent" />
                  <h3 className="font-bold text-sm text-ds-primary">
                    {calendarMonth.toLocaleString('default', { month: 'long', year: 'numeric' })}
                  </h3>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setCalendarMonth(new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() - 1, 1))}
                    className="p-1.5 hover:bg-ds-surface-raised text-ds-muted hover:text-ds-primary rounded transition-colors"
                  >
                    <ChevronLeft className="w-4 h-4" />
                  </button>
                  <button
                    type="button"
                    onClick={() => setCalendarMonth(new Date(calendarMonth.getFullYear(), calendarMonth.getMonth() + 1, 1))}
                    className="p-1.5 hover:bg-ds-surface-raised text-ds-muted hover:text-ds-primary rounded transition-colors"
                  >
                    <ChevronRight className="w-4 h-4" />
                  </button>
                </div>
              </div>

              {/* Day of Week Header */}
              <div className="grid grid-cols-7 gap-1 text-center font-mono text-[11px] font-semibold text-ds-muted uppercase">
                <span>Sun</span>
                <span>Mon</span>
                <span>Tue</span>
                <span>Wed</span>
                <span>Thu</span>
                <span>Fri</span>
                <span>Sat</span>
              </div>

              {/* Month Grid */}
              <div data-testid="calendar-month-grid" className="grid grid-cols-7 gap-1.5">
                {calendarDays.map((cd, idx) => {
                  if (!cd.isCurrentMonth) {
                    return <div key={`empty-${idx}`} className="h-14 rounded-ds bg-transparent" />;
                  }

                  const dayItems = itemsByDate[cd.dateStr] || [];
                  const isSelected = selectedDate === cd.dateStr;
                  const dayTarget = dayItems.reduce((acc, i) => acc + i.targetQuestions, 0);
                  const dayDone = dayItems.reduce((acc, i) => acc + i.doneQuestions, 0);
                  const isDone = dayItems.length > 0 && dayItems.every((i) => i.status === 'DONE');

                  return (
                    <div
                      key={cd.dateStr}
                      data-testid={`calendar-day-cell-${cd.dateStr}`}
                      onClick={() => setSelectedDate(cd.dateStr)}
                      className={`h-16 p-1.5 rounded-ds border cursor-pointer transition-all flex flex-col justify-between text-left ${
                        cd.isRestDay ? 'bg-ds-surface-raised/40 opacity-70' : 'bg-ds-surface'
                      } ${
                        isSelected
                          ? 'border-ds-accent ring-1 ring-ds-accent shadow-md bg-ds-surface-hover'
                          : 'border-ds-border hover:border-ds-border-strong hover:bg-ds-surface-hover'
                      }`}
                    >
                      <div className="flex items-center justify-between text-[11px] font-mono">
                        <span className={`font-semibold ${isSelected ? 'text-ds-accent' : 'text-ds-primary'}`}>
                          {cd.dayNum}
                        </span>
                        {cd.isRestDay && (
                          <span className="text-[9px] text-ds-muted font-normal">Rest</span>
                        )}
                        {isDone && !cd.isRestDay && (
                          <CheckCircle2 className="w-3 h-3 text-ds-success" />
                        )}
                      </div>

                      {dayItems.length > 0 ? (
                        <div className="text-[10px] font-mono">
                          <span className={dayDone >= dayTarget ? 'text-ds-success font-bold' : 'text-ds-accent font-semibold'}>
                            {dayDone}/{dayTarget}
                          </span>
                        </div>
                      ) : (
                        <span className="text-[10px] font-mono text-ds-muted">—</span>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Selected Day Items Column */}
            <div data-testid="day-details-card" className="lg:col-span-5 bg-ds-surface border border-ds-border rounded-ds p-5 shadow-sm space-y-4">
              <div className="border-b border-ds-border pb-3 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-sm text-ds-primary">
                    Targets for {selectedDate}
                  </h3>
                  <span className="text-xs text-ds-muted font-mono">
                    {selectedDayItems.length} {selectedDayItems.length === 1 ? 'task' : 'tasks'} scheduled
                  </span>
                </div>
              </div>

              {selectedDayItems.length === 0 ? (
                <div data-testid="day-no-items" className="py-8 text-center text-xs text-ds-muted">
                  No study items scheduled for this date (Rest or unallocated day).
                </div>
              ) : (
                <div data-testid="day-items-list" className="space-y-3">
                  {selectedDayItems.map((item) => {
                    const isDone = item.status === 'DONE';
                    return (
                      <div
                        key={item.id}
                        data-testid={`day-item-${item.id}`}
                        className={`p-3 rounded-ds border transition-all space-y-2.5 ${
                          isDone ? 'bg-ds-surface-raised/60 border-ds-border opacity-85' : 'bg-ds-surface border-ds-border'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex items-start gap-2.5">
                            <button
                              type="button"
                              data-testid={`day-status-toggle-${item.id}`}
                              onClick={() => handleToggleStatus(item)}
                              disabled={updateStatusMutation.isPending}
                              className="mt-0.5 text-ds-muted hover:text-ds-accent transition-colors disabled:opacity-50 shrink-0"
                            >
                              {isDone ? (
                                <CheckCircle2 className="w-4 h-4 text-ds-success" />
                              ) : (
                                <Circle className="w-4 h-4 text-ds-muted" />
                              )}
                            </button>
                            <div>
                              <span className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-ds-accent-soft text-ds-accent border border-ds-accent/30 inline-block mb-1">
                                {item.subjectCode || item.subjectName || 'GATE'}
                              </span>
                              <h4 className={`text-xs font-semibold ${isDone ? 'line-through text-ds-muted' : 'text-ds-primary'}`}>
                                {item.topicName || 'General Topic Practice'}
                              </h4>
                            </div>
                          </div>

                          <button
                            type="button"
                            data-testid={`day-drill-cta-${item.id}`}
                            onClick={() => {
                              if (item.topicId) {
                                navigate(`/practice?topic=${item.topicId}`);
                              } else {
                                navigate('/practice');
                              }
                            }}
                            className="flex items-center gap-1 px-2.5 py-1 bg-ds-accent-soft hover:bg-ds-accent text-ds-accent hover:text-ds-accent-contrast rounded-ds text-xs font-semibold transition-all border border-ds-accent/30 shrink-0"
                          >
                            <Play className="w-3 h-3" />
                            <span>Drill</span>
                          </button>
                        </div>

                        <div className="flex items-center justify-between text-[11px] font-mono text-ds-muted border-t border-ds-border pt-1.5">
                          <span>Target: {item.targetQuestions} Qs ({item.targetMinutes}m)</span>
                          <span className={item.doneQuestions >= item.targetQuestions ? 'text-ds-success font-semibold' : 'text-ds-secondary'}>
                            Done: {item.doneQuestions}/{item.targetQuestions}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
