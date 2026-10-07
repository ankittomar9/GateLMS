import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TodayCard } from '../TodayCard';
import * as queryHooks from '../../../hooks/useQueries';
import { TodayPlanResponse } from '../../../api/types';

const mockedNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

vi.mock('../../../hooks/useQueries', () => ({
  useTodayPlan: vi.fn(),
  useUpdatePlanItemStatus: vi.fn(),
}));

describe('TodayCard Component', () => {
  let queryClient: QueryClient;
  const mutateStatus = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    vi.mocked(queryHooks.useUpdatePlanItemStatus).mockReturnValue({
      mutateAsync: mutateStatus,
      isPending: false,
    } as any);
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <TodayCard />
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('1. items + progress math from fixture renders correct percentages and counts', () => {
    const fixture: TodayPlanResponse = {
      items: [
        {
          id: 'item-1',
          planId: 'plan-1',
          planDate: '2026-10-07',
          subjectId: 'CS-ALGO',
          subjectName: 'Algorithms',
          subjectCode: 'ALGO',
          topicId: 'top-algo-sort',
          topicName: 'Sorting Algorithms',
          targetQuestions: 20,
          targetMinutes: 60,
          doneQuestions: 10,
          doneMinutes: 30,
          status: 'OPEN',
        },
      ],
      targetQuestions: 20,
      doneQuestions: 10,
      minutesTarget: 60,
      minutesDone: 30,
    };

    vi.mocked(queryHooks.useTodayPlan).mockReturnValue({
      data: fixture,
      isLoading: false,
      error: null,
    } as any);

    renderComponent();

    expect(screen.getByTestId('today-card')).toBeInTheDocument();
    expect(screen.getByText('Today\'s Study Targets')).toBeInTheDocument();
    expect(screen.getByText(/10 \/ 20 Questions/)).toBeInTheDocument();
    expect(screen.getByText(/30 \/ 60 Mins/)).toBeInTheDocument();
    expect(screen.getByText(/50% Complete/)).toBeInTheDocument();
    expect(screen.getByText('Sorting Algorithms')).toBeInTheDocument();
    expect(screen.getByText('ALGO')).toBeInTheDocument();
  });

  it('2. DONE state renders celebratory badge when all items completed', () => {
    const fixture: TodayPlanResponse = {
      items: [
        {
          id: 'item-1',
          planId: 'plan-1',
          planDate: '2026-10-07',
          subjectId: 'CS-ALGO',
          subjectName: 'Algorithms',
          subjectCode: 'ALGO',
          topicId: 'top-algo-sort',
          topicName: 'Sorting Algorithms',
          targetQuestions: 10,
          targetMinutes: 30,
          doneQuestions: 10,
          doneMinutes: 30,
          status: 'DONE',
        },
      ],
      targetQuestions: 10,
      doneQuestions: 10,
      minutesTarget: 30,
      minutesDone: 30,
    };

    vi.mocked(queryHooks.useTodayPlan).mockReturnValue({
      data: fixture,
      isLoading: false,
      error: null,
    } as any);

    renderComponent();

    expect(screen.getByTestId('today-all-done')).toBeInTheDocument();
    expect(screen.getByText(/All Completed/)).toBeInTheDocument();
  });

  it('3. empty-state CTA renders when no items and navigates to /planner', () => {
    vi.mocked(queryHooks.useTodayPlan).mockReturnValue({
      data: {
        items: [],
        doneQuestions: 0,
        targetQuestions: 0,
        minutesDone: 0,
        minutesTarget: 0,
      },
      isLoading: false,
      error: null,
    } as any);

    renderComponent();

    expect(screen.getByTestId('today-empty-state')).toBeInTheDocument();
    expect(screen.getByText(/No study plan scheduled for today/i)).toBeInTheDocument();

    const cta = screen.getByTestId('generate-plan-cta');
    expect(cta).toBeInTheDocument();
    fireEvent.click(cta);
    expect(mockedNavigate).toHaveBeenCalledWith('/planner');
  });

  it('4. clicking status toggle invokes update mutation and handles failure rollback', async () => {
    const fixture: TodayPlanResponse = {
      items: [
        {
          id: 'item-1',
          planId: 'plan-1',
          planDate: '2026-10-07',
          subjectId: 'CS-ALGO',
          subjectName: 'Algorithms',
          subjectCode: 'ALGO',
          topicId: 'top-algo-sort',
          topicName: 'Sorting Algorithms',
          targetQuestions: 10,
          targetMinutes: 30,
          doneQuestions: 0,
          doneMinutes: 0,
          status: 'OPEN',
        },
      ],
      targetQuestions: 10,
      doneQuestions: 0,
      minutesTarget: 30,
      minutesDone: 0,
    };

    vi.mocked(queryHooks.useTodayPlan).mockReturnValue({
      data: fixture,
      isLoading: false,
      error: null,
    } as any);

    mutateStatus.mockRejectedValueOnce(new Error('Network disconnected'));

    renderComponent();

    const toggleBtn = screen.getByTestId('today-status-toggle-item-1');
    fireEvent.click(toggleBtn);

    expect(mutateStatus).toHaveBeenCalledWith({
      itemId: 'item-1',
      req: { status: 'DONE' },
    });

    const errorBanner = await screen.findByTestId('today-error-banner');
    expect(errorBanner).toBeInTheDocument();
    expect(screen.getByText(/Network disconnected/i)).toBeInTheDocument();
  });

  it('5. clicking Drill CTA navigates to /practice with preconfigured topic', () => {
    const fixture: TodayPlanResponse = {
      items: [
        {
          id: 'item-1',
          planId: 'plan-1',
          planDate: '2026-10-07',
          subjectId: 'CS-ALGO',
          subjectName: 'Algorithms',
          subjectCode: 'ALGO',
          topicId: 'top-algo-sort',
          topicName: 'Sorting Algorithms',
          targetQuestions: 10,
          targetMinutes: 30,
          doneQuestions: 0,
          doneMinutes: 0,
          status: 'OPEN',
        },
      ],
      targetQuestions: 10,
      doneQuestions: 0,
      minutesTarget: 30,
      minutesDone: 0,
    };

    vi.mocked(queryHooks.useTodayPlan).mockReturnValue({
      data: fixture,
      isLoading: false,
      error: null,
    } as any);

    renderComponent();

    const drillBtn = screen.getByTestId('today-drill-cta-item-1');
    fireEvent.click(drillBtn);

    expect(mockedNavigate).toHaveBeenCalledWith('/practice?topic=top-algo-sort');
  });
});
