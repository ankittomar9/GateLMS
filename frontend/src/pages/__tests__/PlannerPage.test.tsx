import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PlannerPage } from '../PlannerPage';
import * as queryHooks from '../../hooks/useQueries';
import { StudyPlanDetailDto, StudyPlanSummaryDto, SubjectSummary } from '../../api/types';

const mockedNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

vi.mock('../../hooks/useQueries', () => ({
  useStudyPlans: vi.fn(),
  useStudyPlan: vi.fn(),
  useGeneratePlan: vi.fn(),
  useRegeneratePlan: vi.fn(),
  useUpdatePlanItemStatus: vi.fn(),
  useSubjects: vi.fn(),
  useReadiness: vi.fn(),
}));

describe('PlannerPage Component Tests', () => {
  let queryClient: QueryClient;
  const mutateGenerate = vi.fn();
  const mutateRegenerate = vi.fn();
  const mutateUpdateStatus = vi.fn();

  const todayStr = new Date().toISOString().split('T')[0];

  const mockPlanSummary: StudyPlanSummaryDto = {
    id: 'plan-uuid-1',
    title: '16-Week GATE CSE Ramp-up',
    startDate: '2026-10-01',
    endDate: '2027-01-21',
    weeks: 16,
    weeklyTargetQuestions: 150,
    weeklyTargetMinutes: 600,
    strategy: 'WEIGHTED',
    totalQuestionsTarget: 2400,
    totalQuestionsDone: 350,
    totalMinutesTarget: 9600,
    totalMinutesDone: 1400,
    progressPercentage: 14.6,
    itemsCount: 96,
    openItemsCount: 82,
    doneItemsCount: 14,
    skippedItemsCount: 0,
    createdAt: '2026-10-01T00:00:00Z',
  };

  const mockPlanDetail: StudyPlanDetailDto = {
    id: 'plan-uuid-1',
    title: '16-Week GATE CSE Ramp-up',
    startDate: '2026-10-01',
    endDate: '2027-01-21',
    weeks: 16,
    weeklyTargetQuestions: 150,
    weeklyTargetMinutes: 600,
    strategy: 'WEIGHTED',
    totalQuestionsTarget: 2400,
    totalQuestionsDone: 350,
    totalMinutesTarget: 9600,
    totalMinutesDone: 1400,
    progressPercentage: 14.6,
    generatedReasoning: {
      mode: 'weighted',
      reason: 'active_plan',
      excludedTopics: [{ id: 'top-empty-1', reason: 'zero_questions' }],
    },
    createdAt: '2026-10-01T00:00:00Z',
    items: [
      {
        id: 'item-today-1',
        planId: 'plan-uuid-1',
        planDate: todayStr,
        subjectId: 'subj_algo',
        subjectName: 'Algorithms',
        subjectCode: 'ALGO',
        topicId: 'top-algo-dp',
        topicName: 'Dynamic Programming',
        targetQuestions: 15,
        targetMinutes: 45,
        doneQuestions: 5,
        doneMinutes: 15,
        status: 'OPEN',
      },
    ],
    itemsByDate: {
      [todayStr]: [
        {
          id: 'item-today-1',
          planId: 'plan-uuid-1',
          planDate: todayStr,
          subjectId: 'subj_algo',
          subjectName: 'Algorithms',
          subjectCode: 'ALGO',
          topicId: 'top-algo-dp',
          topicName: 'Dynamic Programming',
          targetQuestions: 15,
          targetMinutes: 45,
          doneQuestions: 5,
          doneMinutes: 15,
          status: 'OPEN',
        },
      ],
    },
  };

  const mockSubjects: SubjectSummary[] = [
    {
      id: 'subj_algo',
      name: 'Algorithms',
      slug: 'algorithms',
      code: 'ALGO',
      weightageAvg: 10.0,
      priorityTier: 'TIER_1',
      primaryBookRef: 'CLRS',
      topicCount: 4,
    },
    {
      id: 'subj_os',
      name: 'Operating Systems',
      slug: 'operating-systems',
      code: 'OS',
      weightageAvg: 8.0,
      priorityTier: 'TIER_1',
      primaryBookRef: 'Silberschatz',
      topicCount: 5,
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    vi.mocked(queryHooks.useStudyPlans).mockReturnValue({
      data: [mockPlanSummary],
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useStudyPlan).mockReturnValue({
      data: mockPlanDetail,
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useSubjects).mockReturnValue({
      data: mockSubjects,
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useReadiness).mockReturnValue({
      data: {
        readiness: 50,
        perSubject: [
          { subjectId: 'subj_algo', name: 'Algorithms', weight: 10.0, m: 0.5, a: 0.5, c: 0.5, readiness: 50.0 },
          { subjectId: 'subj_os', name: 'Operating Systems', weight: 8.0, m: 0.2, a: 0.2, c: 0.2, readiness: 20.0 },
        ],
        computedAt: '2026-10-07T00:00:00Z',
      },
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useGeneratePlan).mockReturnValue({
      mutateAsync: mutateGenerate,
      isPending: false,
    } as any);

    vi.mocked(queryHooks.useRegeneratePlan).mockReturnValue({
      mutateAsync: mutateRegenerate,
      isPending: false,
    } as any);

    vi.mocked(queryHooks.useUpdatePlanItemStatus).mockReturnValue({
      mutateAsync: mutateUpdateStatus,
      isPending: false,
    } as any);
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <PlannerPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('1. renders empty state when no plans exist and triggers form toggle', () => {
    vi.mocked(queryHooks.useStudyPlans).mockReturnValue({
      data: [],
      isLoading: false,
      error: null,
    } as any);

    renderComponent();

    expect(screen.getByTestId('planner-empty-state')).toBeInTheDocument();
    expect(screen.getByText(/No Study Plans Generated Yet/i)).toBeInTheDocument();

    const emptyBtn = screen.getByTestId('empty-generate-btn');
    fireEvent.click(emptyBtn);

    expect(screen.getByTestId('plan-generator-form-card')).toBeInTheDocument();
  });

  it('2. generation form validation displays error banner on invalid weeks', () => {
    renderComponent();

    // Open form
    const toggleBtn = screen.getByTestId('toggle-create-plan-btn');
    fireEvent.click(toggleBtn);

    const weeksInput = screen.getByTestId('input-plan-weeks');
    fireEvent.change(weeksInput, { target: { value: '0' } });

    const submitBtn = screen.getByTestId('submit-generate-plan-btn');
    fireEvent.click(submitBtn);

    expect(screen.getByTestId('form-error-banner')).toBeInTheDocument();
    expect(screen.getByText(/Weeks must be between 1 and 52/i)).toBeInTheDocument();
    expect(mutateGenerate).not.toHaveBeenCalled();
  });

  it('3. generation form submit posts payload with selected strategy', async () => {
    mutateGenerate.mockResolvedValueOnce({ id: 'new-plan-1' });

    renderComponent();

    fireEvent.click(screen.getByTestId('toggle-create-plan-btn'));

    const titleInput = screen.getByTestId('input-plan-title');
    fireEvent.change(titleInput, { target: { value: 'Custom Sprint' } });

    const weeksInput = screen.getByTestId('input-plan-weeks');
    fireEvent.change(weeksInput, { target: { value: '8' } });

    // Select LINEAR strategy
    const linearRadio = screen.getByLabelText(/LINEAR/i);
    fireEvent.click(linearRadio);

    fireEvent.click(screen.getByTestId('submit-generate-plan-btn'));

    await waitFor(() => {
      expect(mutateGenerate).toHaveBeenCalledWith({
        title: 'Custom Sprint',
        weeks: 8,
        strategy: 'LINEAR',
        weeklyTargetQuestions: 150,
        weeklyTargetMinutes: 600,
      });
    });
  });

  it('4. preview chips render subject distribution and notes', () => {
    renderComponent();

    fireEvent.click(screen.getByTestId('toggle-create-plan-btn'));

    expect(screen.getByTestId('strategy-preview-section')).toBeInTheDocument();
    expect(screen.getByTestId('preview-chip-subj_algo')).toBeInTheDocument();
    expect(screen.getByTestId('preview-chip-subj_os')).toBeInTheDocument();
    expect(screen.getByText(/Topics with zero questions in the question bank are automatically excluded/i)).toBeInTheDocument();
  });

  it('5. calendar month grid calculates day cells and displays target/done ratios', () => {
    renderComponent();

    expect(screen.getByTestId('calendar-month-grid')).toBeInTheDocument();
    const todayCell = screen.getByTestId(`calendar-day-cell-${todayStr}`);
    expect(todayCell).toBeInTheDocument();
    expect(todayCell).toHaveTextContent('5/15');
  });

  it('6. day drill deep-link navigates to /practice with topic query param', () => {
    renderComponent();

    const drillBtn = screen.getByTestId('day-drill-cta-item-today-1');
    fireEvent.click(drillBtn);

    expect(mockedNavigate).toHaveBeenCalledWith('/practice?topic=top-algo-dp');
  });

  it('7. status toggle mutation updates item status to DONE', () => {
    renderComponent();

    const toggleBtn = screen.getByTestId('day-status-toggle-item-today-1');
    fireEvent.click(toggleBtn);

    expect(mutateUpdateStatus).toHaveBeenCalledWith({
      itemId: 'item-today-1',
      req: { status: 'DONE' },
    });
  });

  it('8. regenerate confirm dialog triggers regenerate mutation and preserves DONE items', async () => {
    mutateRegenerate.mockResolvedValueOnce({ id: 'plan-uuid-1' });

    renderComponent();

    const regenBtn = screen.getByTestId('regenerate-plan-btn');
    fireEvent.click(regenBtn);

    expect(screen.getByTestId('regen-confirm-modal')).toBeInTheDocument();
    expect(screen.getByText(/All previously completed \(DONE\) items will be preserved safely/i)).toBeInTheDocument();

    const confirmBtn = screen.getByTestId('regen-confirm-btn');
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(mutateRegenerate).toHaveBeenCalledWith('plan-uuid-1');
    });
  });

  it('9. "Why this plan?" button toggles algorithmic reasoning drawer', () => {
    renderComponent();

    expect(screen.queryByTestId('why-this-plan-popover')).not.toBeInTheDocument();

    const whyBtn = screen.getByTestId('why-this-plan-btn');
    fireEvent.click(whyBtn);

    expect(screen.getByTestId('why-this-plan-popover')).toBeInTheDocument();
    expect(screen.getByText(/Algorithmic Reasoning Receipt/i)).toBeInTheDocument();
    expect(screen.getByText(/1 topics excluded \(0 questions in bank\)/i)).toBeInTheDocument();
  });
});
