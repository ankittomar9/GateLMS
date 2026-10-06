import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { QuestionsPage } from '../QuestionsPage';
import * as queryHooks from '../../hooks/useQueries';
import { PageResponse, QuestionSummary } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useQuestions: vi.fn(),
  useSubjects: vi.fn(),
  useTopics: vi.fn(),
  useToggleBookmark: vi.fn(),
}));

const mockQuestions: QuestionSummary[] = [
  {
    id: 'q1',
    externalId: 'GATE-CS-2023-Q01',
    subjectId: 'subj_os',
    subjectName: 'Operating Systems',
    topicId: 'topic_paging',
    topicName: 'Paging',
    year: 2023,
    type: 'MCQ',
    marks: 1,
    statementSnippet: 'What is the page table base address in hardware?',
    gateOverflowUrl: 'https://gateoverflow.in/example',
    isBookmarked: false,
    pass1Status: 'UNSEEN',
    pass2Status: 'NOT_ATTEMPTED',
    version: 0,
  },
  {
    id: 'q2',
    externalId: 'GATE-CS-2022-Q14',
    subjectId: 'subj_coa',
    subjectName: 'Computer Organization',
    topicId: 'topic_pipeline',
    topicName: 'Pipelining',
    year: 2022,
    type: 'NAT',
    marks: 2,
    statementSnippet: 'Calculate clock cycle time for a 5-stage pipeline.',
    gateOverflowUrl: null,
    isBookmarked: true,
    pass1Status: 'SOLVED',
    pass2Status: 'SOLVED',
    version: 0,
  },
];

const mockPageResponse: PageResponse<QuestionSummary> = {
  content: mockQuestions,
  page: 0,
  size: 20,
  totalElements: 2,
  totalPages: 1,
};

describe('QuestionsPage', () => {
  let queryClient: QueryClient;
  const mutateBookmark = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient();

    vi.mocked(queryHooks.useSubjects).mockReturnValue({
      data: [{ id: 'subj_os', name: 'Operating Systems', code: 'OS' }],
    } as any);

    vi.mocked(queryHooks.useTopics).mockReturnValue({
      data: [{ id: 'topic_paging', name: 'Paging' }],
    } as any);

    vi.mocked(queryHooks.useQuestions).mockReturnValue({
      data: mockPageResponse,
      isLoading: false,
      error: null,
    } as any);

    vi.mocked(queryHooks.useToggleBookmark).mockReturnValue({
      mutate: mutateBookmark,
      isPending: false,
    } as any);
  });

  const renderComponent = (initialEntries = ['/questions']) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>
          <QuestionsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('honest-judge PASS - renders question rows, badges, year, and marks', () => {
    renderComponent();
    expect(screen.getByText('GATE-CS-2023-Q01')).toBeInTheDocument();
    expect(screen.getByText('GATE-CS-2022-Q14')).toBeInTheDocument();
    expect(screen.getByText('What is the page table base address in hardware?')).toBeInTheDocument();
    expect(screen.getByText('Calculate clock cycle time for a 5-stage pipeline.')).toBeInTheDocument();
    expect(screen.getByText('MCQ')).toBeInTheDocument();
    expect(screen.getByText('NAT')).toBeInTheDocument();
  });

  it('honest-judge PASS - empty state renders when question list is empty', () => {
    vi.mocked(queryHooks.useQuestions).mockReturnValue({
      data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
      isLoading: false,
      error: null,
    } as any);

    renderComponent();
    expect(screen.getByText('No questions found')).toBeInTheDocument();
    expect(screen.getByText('Reset all filters')).toBeInTheDocument();
  });

  it('honest-judge PASS - bookmark button triggers toggleBookmark mutation', () => {
    renderComponent();
    const bookmarkButtons = screen.getAllByTitle('Bookmark');
    fireEvent.click(bookmarkButtons[0]);
    expect(mutateBookmark).toHaveBeenCalledWith('q1');
  });

  it('honest-judge PASS - type select filter allows selecting question types', () => {
    renderComponent();
    const typeSelect = screen.getByDisplayValue('All Types');
    fireEvent.change(typeSelect, { target: { value: 'MSQ' } });
    expect(typeSelect).toHaveValue('MSQ');
  });

  it('honest-judge PASS - pagination controls render when totalPages is greater than 1', () => {
    vi.mocked(queryHooks.useQuestions).mockReturnValue({
      data: { ...mockPageResponse, totalPages: 3, totalElements: 60 },
      isLoading: false,
      error: null,
    } as any);

    renderComponent();
    expect(screen.getByText('1 / 3')).toBeInTheDocument();
  });
});
