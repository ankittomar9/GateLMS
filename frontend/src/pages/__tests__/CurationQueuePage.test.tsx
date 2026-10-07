import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CurationQueuePage } from '../CurationQueuePage';
import * as queryHooks from '../../hooks/useQueries';
import { PageResponse, QuestionSummary, CurationStatsResponse } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useQuestions: vi.fn(),
  useSubjects: vi.fn(),
  useCurationStats: vi.fn(),
  useUpdateCurationStatus: vi.fn(),
}));

const mockQuestions: QuestionSummary[] = [
  {
    id: 'q-so-1',
    externalId: 'SO-101',
    subjectId: 'subj_os',
    subjectName: 'Operating Systems',
    topicId: 'topic_paging',
    topicName: 'Paging',
    year: 2024,
    type: 'MCQ',
    marks: 1,
    statement: 'What is the theoretical speedup in a pipeline with $k$ stages?',
    statementSnippet: 'What is the theoretical speedup in a pipeline with $k$ stages?',
    gateOverflowUrl: 'https://stackoverflow.com/questions/101',
    isBookmarked: false,
    pass1Status: 'UNSEEN',
    pass2Status: 'NOT_ATTEMPTED',
    curationStatus: 'UNREVIEWED',
    source: 'SO_DERIVED',
    difficulty: 'MEDIUM',
    version: 0,
  },
  {
    id: 'q-so-2',
    externalId: 'SO-102',
    subjectId: 'subj_algo',
    subjectName: 'Algorithms',
    topicId: 'topic_dijkstra',
    topicName: 'Shortest Paths',
    year: 2024,
    type: 'NAT',
    marks: 2,
    statement: 'Calculate the minimum edge weight in $$O(V \\log V)$$ graph.',
    statementSnippet: 'Calculate the minimum edge weight in $$O(V \\log V)$$ graph.',
    gateOverflowUrl: 'https://stackoverflow.com/questions/102',
    isBookmarked: false,
    pass1Status: 'UNSEEN',
    pass2Status: 'NOT_ATTEMPTED',
    curationStatus: 'UNREVIEWED',
    source: 'SO_DERIVED',
    difficulty: 'HARD',
    version: 0,
  },
];

const mockPageResponse: PageResponse<QuestionSummary> = {
  content: mockQuestions,
  page: 0,
  size: 15,
  totalElements: 2,
  totalPages: 1,
};

const mockCurationStats: CurationStatsResponse = {
  totalQuestions: 2550,
  byStatus: {
    UNREVIEWED: 2400,
    ACCEPTED: 120,
    REJECTED: 30,
  },
  bySubject: [
    {
      subjectId: 'subj_os',
      name: 'Operating Systems',
      total: 280,
      unreviewed: 260,
      accepted: 15,
      rejected: 5,
    },
  ],
};

const mockSubjects = [
  { id: 'subj_os', name: 'Operating Systems', code: 'CS-OS', totalQuestions: 280, displayOrder: 1 },
  { id: 'subj_algo', name: 'Algorithms', code: 'CS-ALGO', totalQuestions: 280, displayOrder: 2 },
];

describe('CurationQueuePage Component', () => {
  let mockMutateAsync: any;
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    mockMutateAsync = vi.fn().mockResolvedValue({ id: 'q-so-1', curationStatus: 'ACCEPTED' });

    vi.mocked(queryHooks.useQuestions).mockReturnValue({
      data: mockPageResponse,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    } as any);

    vi.mocked(queryHooks.useSubjects).mockReturnValue({
      data: mockSubjects,
      isLoading: false,
    } as any);

    vi.mocked(queryHooks.useCurationStats).mockReturnValue({
      data: mockCurationStats,
      isLoading: false,
      refetch: vi.fn(),
    } as any);

    vi.mocked(queryHooks.useUpdateCurationStatus).mockReturnValue({
      mutateAsync: mockMutateAsync,
      mutate: vi.fn(),
      isPending: false,
    } as any);
  });

  const renderPage = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/curation']}>
          <CurationQueuePage />
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('renders filters, curation stats banner, and unreviewed question cards with MathText', () => {
    renderPage();

    expect(screen.getByTestId('curation-queue-page')).toBeDefined();
    expect(screen.getByText(/Question Curation Queue/i)).toBeDefined();
    expect(screen.getByText('2550')).toBeDefined();
    expect(screen.getByText('2400')).toBeDefined();
    expect(screen.getByText('120')).toBeDefined();
    expect(screen.getByText('30')).toBeDefined();

    // Cards rendered
    expect(screen.getByTestId('curation-card-q-so-1')).toBeDefined();
    expect(screen.getByTestId('curation-card-q-so-2')).toBeDefined();
    expect(screen.getByText('SO-101')).toBeDefined();
    expect(screen.getByText('SO-102')).toBeDefined();

    // Source and type chips
    expect(screen.getAllByText('SO_DERIVED').length).toBeGreaterThanOrEqual(1);
  });

  it('clicking Accept button fires mutation with status ACCEPTED', async () => {
    const user = userEvent.setup();
    renderPage();

    const acceptBtn = screen.getByTestId('accept-btn-q-so-1');
    await user.click(acceptBtn);

    expect(mockMutateAsync).toHaveBeenCalledWith({
      id: 'q-so-1',
      status: 'ACCEPTED',
      note: undefined,
    });
  });

  it('clicking Reject button with optional note input fires mutation with status REJECTED and note', async () => {
    const user = userEvent.setup();
    renderPage();

    // Open note input for q-so-1
    const addNoteBtns = screen.getAllByRole('button', { name: /Add Note/i });
    await user.click(addNoteBtns[0]);

    const textarea = screen.getByPlaceholderText(/Enter reason or editorial comments/i);
    fireEvent.change(textarea, { target: { value: 'Stem ambiguous, options misaligned with GATE standard' } });

    const rejectBtn = screen.getByTestId('reject-btn-q-so-1');
    await user.click(rejectBtn);

    expect(mockMutateAsync).toHaveBeenCalledWith({
      id: 'q-so-1',
      status: 'REJECTED',
      note: 'Stem ambiguous, options misaligned with GATE standard',
    });
  });

  it('optimistic rollback handles error gracefully when mutation fails', async () => {
    mockMutateAsync.mockRejectedValueOnce(new Error('Network error during curation mutation'));

    const user = userEvent.setup();
    renderPage();

    const acceptBtn = screen.getByTestId('accept-btn-q-so-1');
    await user.click(acceptBtn);

    await waitFor(() => {
      expect(screen.getByTestId('curation-error-banner')).toBeDefined();
      expect(screen.getByText(/Network error during curation mutation/i)).toBeDefined();
    });
  });

  it('keyboard shortcuts: A to accept, R to reject, N to toggle note for first visible question', async () => {
    renderPage();

    // Press 'A'
    fireEvent.keyDown(window, { key: 'a', code: 'KeyA' });
    expect(mockMutateAsync).toHaveBeenCalledWith({
      id: 'q-so-1',
      status: 'ACCEPTED',
      note: undefined,
    });

    // Press 'R'
    fireEvent.keyDown(window, { key: 'r', code: 'KeyR' });
    expect(mockMutateAsync).toHaveBeenCalledWith({
      id: 'q-so-1',
      status: 'REJECTED',
      note: undefined,
    });

    // Press 'N' opens note input
    fireEvent.keyDown(window, { key: 'n', code: 'KeyN' });
    expect(screen.getByPlaceholderText(/Enter reason or editorial comments/i)).toBeDefined();
  });

  it('bulk mode: selects questions and triggers bulk Accept action', async () => {
    const user = userEvent.setup();
    renderPage();

    // Select all questions
    const selectAllCheckbox = screen.getByRole('checkbox', { name: /Select all 2 on page/i });
    await user.click(selectAllCheckbox);

    // Bulk action bar appears
    expect(screen.getByText(/2 questions selected/i)).toBeDefined();

    const bulkAcceptBtn = screen.getByRole('button', { name: /Bulk Accept/i });
    await user.click(bulkAcceptBtn);

    // MutateAsync should be called for each selected question
    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'q-so-1', status: 'ACCEPTED' })
      );
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'q-so-2', status: 'ACCEPTED' })
      );
    });
  });
});
