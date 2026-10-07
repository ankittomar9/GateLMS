import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PracticePage } from '../PracticePage';
import * as queryHooks from '../../hooks/useQueries';
import { SubjectSummary, TopicItem } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useSubjects: vi.fn(),
  useTopics: vi.fn(),
  useCreatePracticeSession: vi.fn(),
  useMistakes: vi.fn(),
  useTopicMasteryStats: vi.fn(),
}));

const mockSubjects: SubjectSummary[] = [
  {
    id: 'subj_algo',
    name: 'Algorithms',
    slug: 'algorithms',
    code: 'CS-ALGO',
    weightageAvg: 9.0,
    priorityTier: 'TIER_1',
    primaryBookRef: 'CLRS',
    topicCount: 4,
  },
  {
    id: 'subj_os',
    name: 'Operating Systems',
    slug: 'operating-systems',
    code: 'CS-OS',
    weightageAvg: 8.0,
    priorityTier: 'TIER_2',
    primaryBookRef: 'Silberschatz',
    topicCount: 3,
  },
];

const mockTopicsAlgo: TopicItem[] = [
  {
    id: 'topic_asymptotic',
    subjectId: 'subj_algo',
    name: 'Asymptotic Analysis',
    slug: 'asymptotic-analysis',
    bookChapterRef: 'CLRS Ch 3',
    masteryLevel: 3,
    lastRevisedAt: '2026-10-01T00:00:00Z',
    nextRevisionDue: '2026-10-10T00:00:00Z',
    version: 1,
  },
  {
    id: 'topic_greedy',
    subjectId: 'subj_algo',
    name: 'Greedy Algorithms',
    slug: 'greedy-algorithms',
    bookChapterRef: 'CLRS Ch 16',
    masteryLevel: 1,
    lastRevisedAt: null,
    nextRevisionDue: null,
    version: 0,
  },
];

describe('PracticePage', () => {
  let queryClient: QueryClient;
  const mockMutateAsync = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    (queryHooks.useSubjects as any).mockReturnValue({
      data: mockSubjects,
      isLoading: false,
    });

    (queryHooks.useTopics as any).mockImplementation((subjectId?: string) => {
      if (subjectId === 'subj_algo') {
        return { data: mockTopicsAlgo, isLoading: false };
      }
      return { data: [], isLoading: false };
    });

    (queryHooks.useMistakes as any).mockReturnValue({
      data: { content: [], totalElements: 4 },
      isLoading: false,
    });

    (queryHooks.useTopicMasteryStats as any).mockReturnValue({
      data: [],
      isLoading: false,
    });

    mockMutateAsync.mockResolvedValue({
      sessionId: 'sess-123',
      attemptId: 'att-123',
      questions: [],
      totalQuestions: 10,
    });

    (queryHooks.useCreatePracticeSession as any).mockReturnValue({
      mutateAsync: mockMutateAsync,
      isPending: false,
    });
  });

  const renderComponent = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <PracticePage />
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('renders page header and cascading topic load on subject select', async () => {
    renderComponent();

    expect(screen.getByText('Practice Clusters')).toBeInTheDocument();

    const subjectSelect = screen.getByLabelText('Subject');
    expect(subjectSelect).toBeInTheDocument();

    // Select Algorithms
    fireEvent.change(subjectSelect, { target: { value: 'subj_algo' } });

    await waitFor(() => {
      expect(screen.getByText('Asymptotic Analysis (Lvl 3/5)')).toBeInTheDocument();
      expect(screen.getByText('Greedy Algorithms (Lvl 1/5)')).toBeInTheDocument();
    });
  });

  it('clamps question count input between 1 and 50', async () => {
    renderComponent();

    const countInput = screen.getByLabelText('Question Count');
    expect(countInput).toHaveValue(10);

    // Try setting to 999
    fireEvent.change(countInput, { target: { value: '999' } });
    expect(countInput).toHaveValue(50);

    // Try setting to -5
    fireEvent.change(countInput, { target: { value: '-5' } });
    expect(countInput).toHaveValue(1);
  });

  it('calls createPracticeSession mutation when Start Practice is submitted', async () => {
    renderComponent();

    const submitBtn = screen.getByRole('button', { name: /start practice session/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          count: 10,
        })
      );
    });
  });

  it('renders due-review mistake badge when due mistakes count > 0', () => {
    renderComponent();

    expect(screen.getByText('4 Mistakes Due for Review')).toBeInTheDocument();
    expect(screen.getByText('Spaced interval queue ready')).toBeInTheDocument();
  });

  it('triggers quick preset cluster session creation on preset card click', async () => {
    renderComponent();

    const algoPreset = screen.getByText('Core Algorithms & Complexity');
    fireEvent.click(algoPreset);

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({
          subjectId: 'subj_algo',
          count: 10,
        })
      );
    });
  });
});
