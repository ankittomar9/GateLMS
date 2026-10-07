import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SyllabusPage } from '../SyllabusPage';
import * as queryHooks from '../../hooks/useQueries';
import { SubjectSummary, TopicItem } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useSubjects: vi.fn(),
  useTopics: vi.fn(),
  useUpdateTopicMastery: vi.fn(),
  useReadiness: vi.fn(),
}));

const mockSubjects: SubjectSummary[] = [
  {
    id: 'subj_os',
    name: 'Operating Systems',
    slug: 'operating-systems',
    code: 'OS',
    weightageAvg: 10.0,
    priorityTier: 'TIER_1',
    primaryBookRef: 'Silberschatz & Galvin',
    topicCount: 4,
  },
  {
    id: 'subj_algo',
    name: 'Algorithms',
    slug: 'algorithms',
    code: 'ALGO',
    weightageAvg: 8.0,
    priorityTier: 'TIER_1',
    primaryBookRef: 'CLRS',
    topicCount: 5,
  },
];

const mockTopics: TopicItem[] = [
  {
    id: 'topic_paging',
    subjectId: 'subj_os',
    name: 'Virtual Memory & Paging',
    slug: 'paging',
    bookChapterRef: 'Silberschatz Chapter 9',
    masteryLevel: 60,
    lastRevisedAt: null,
    nextRevisionDue: null,
    version: 1,
  },
  {
    id: 'topic_cpu_sched',
    subjectId: 'subj_os',
    name: 'CPU Scheduling Algorithms',
    slug: 'cpu-scheduling',
    bookChapterRef: 'Silberschatz Chapter 5',
    masteryLevel: 40,
    lastRevisedAt: null,
    nextRevisionDue: null,
    version: 1,
  },
];

describe('SyllabusPage', () => {
  let queryClient: QueryClient;
  const mutateMastery = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient();

    vi.mocked(queryHooks.useSubjects).mockReturnValue({
      data: mockSubjects,
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useTopics).mockReturnValue({
      data: mockTopics,
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useUpdateTopicMastery).mockReturnValue({
      mutateAsync: mutateMastery,
      isPending: false,
    } as any);

    vi.mocked(queryHooks.useReadiness).mockReturnValue({
      data: {
        readiness: 75,
        perSubject: [
          { subjectId: 'subj_os', name: 'Operating Systems', weight: 10.0, m: 0.8, a: 0.7, c: 0.6, readiness: 75.0 },
          { subjectId: 'subj_algo', name: 'Algorithms', weight: 8.0, m: 0.6, a: 0.5, c: 0.4, readiness: 65.0 }
        ],
        computedAt: new Date().toISOString()
      },
      isLoading: false,
      error: null,
    } as any);
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <SyllabusPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('honest-judge PASS - renders subjects list with code, name, and weightage', () => {
    renderComponent();
    expect(screen.getAllByText('Operating Systems')[0]).toBeInTheDocument();
    expect(screen.getByText('Algorithms')).toBeInTheDocument();
    expect(screen.getAllByText('OS')[0]).toBeInTheDocument();
    expect(screen.getByText('ALGO')).toBeInTheDocument();
    expect(screen.getAllByText('10')[0]).toBeInTheDocument(); // 10 Marks
  });

  it('honest-judge PASS - renders topic list for the active subject', () => {
    renderComponent();
    expect(screen.getByText('Virtual Memory & Paging')).toBeInTheDocument();
    expect(screen.getByText('CPU Scheduling Algorithms')).toBeInTheDocument();
    expect(screen.getByText('Silberschatz Chapter 9')).toBeInTheDocument();
    expect(screen.getByText('60%')).toBeInTheDocument();
  });

  it('honest-judge PASS - mastery stepper button calls updateMastery with incremented level', async () => {
    renderComponent();
    const plusButtons = screen.getAllByTitle('Increase Mastery by 10%');
    fireEvent.click(plusButtons[0]);

    expect(mutateMastery).toHaveBeenCalledWith(
      expect.objectContaining({
        topicId: 'topic_paging',
        update: expect.objectContaining({
          masteryLevel: 70,
          version: 1,
        }),
      })
    );
  });

  it('honest-judge FAIL - deliberate-conflict 409 error during update displays conflict notification', async () => {
    mutateMastery.mockRejectedValue({
      status: 409,
      message: 'Topic version mismatch',
    });

    renderComponent();
    const plusButtons = screen.getAllByTitle('Increase Mastery by 10%');
    await fireEvent.click(plusButtons[0]);

    expect(
      await screen.findByText(/Conflict on "Virtual Memory & Paging"/i)
    ).toBeInTheDocument();
  });

  it('honest-judge PASS - renders subjects with numeric priorityTier, avgWeightage, and standardBookRef without throwing', () => {
    vi.mocked(queryHooks.useSubjects).mockReturnValue({
      data: [
        {
          id: 'subj_aptitude',
          name: 'General Aptitude',
          slug: 'general-aptitude',
          avgWeightage: 15.0,
          priorityTier: 1,
          standardBookRef: 'RS Aggarwal / Wren & Martin',
          topicCount: 2,
        },
      ] as any,
      isLoading: false,
      error: null,
    } as any);

    renderComponent();
    expect(screen.getAllByText('General Aptitude')[0]).toBeInTheDocument();
    expect(screen.getAllByText('GA')[0]).toBeInTheDocument();
    expect(screen.getAllByText('TIER 1')[0]).toBeInTheDocument();
    expect(screen.getAllByText('15')[0]).toBeInTheDocument();
    expect(screen.getByText('RS Aggarwal / Wren & Martin')).toBeInTheDocument();
  });
});
