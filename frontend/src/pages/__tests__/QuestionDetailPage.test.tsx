import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { QuestionDetailPage } from '../QuestionDetailPage';
import * as queryHooks from '../../hooks/useQueries';
import { QuestionDetail } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useQuestion: vi.fn(),
  useUpdatePass1Status: vi.fn(),
  useUpdatePass2Status: vi.fn(),
  useToggleBookmark: vi.fn(),
}));

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getConceptLesson: vi.fn().mockResolvedValue({
      lesson: {
        id: 'lesson-1',
        conceptTagSlug: 'paging',
        title: 'Paging Architecture',
        motivation: 'Decouple logical address space',
        coreIdea: 'Translate page to frame',
        invariant: 'PA = Frame * Size + Offset',
        workedExample: 'Example step by step calculation',
        traps: 'Confusing page and frame',
        workedExampleExternalId: 'GATE-CS-2023-Q01',
        status: 'ACCEPTED',
      },
      concept: {
        slug: 'paging',
        label: 'Paging Architecture',
      },
      mappedQuestions: [],
    }),
  },
}));

const mockMcqQuestion: QuestionDetail = {
  id: 'q1',
  externalId: 'GATE-CS-2023-Q01',
  subjectId: 'subj_os',
  subjectName: 'Operating Systems',
  topicId: 'topic_paging',
  topicName: 'Paging',
  year: 2023,
  type: 'MCQ',
  marks: 1,
  statement: 'What is the page table base address in hardware?',
  optionsJson: JSON.stringify([
    { label: 'A', text: 'Stored in Program Counter' },
    { label: 'B', text: 'Stored in Page Table Base Register (PTBR)' },
    { label: 'C', text: 'Stored in Instruction Register' },
    { label: 'D', text: 'Stored in Stack Pointer' },
  ]),
  correctOptions: 'B',
  numericMin: null,
  numericMax: null,
  explanation: 'PTBR holds the physical base address of the current process page table.',
  gateOverflowUrl: 'https://gateoverflow.in/example',
  isBookmarked: false,
  pass1Status: 'UNSEEN',
  pass2Status: 'NOT_ATTEMPTED',
  version: 0,
};

describe('QuestionDetailPage', () => {
  let queryClient: QueryClient;
  const mutatePass1 = vi.fn();
  const mutatePass2 = vi.fn();
  const mutateBookmark = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient();

    vi.mocked(queryHooks.useQuestion).mockReturnValue({
      data: mockMcqQuestion,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    } as any);

    vi.mocked(queryHooks.useUpdatePass1Status).mockReturnValue({
      mutateAsync: mutatePass1,
      isPending: false,
    } as any);

    vi.mocked(queryHooks.useUpdatePass2Status).mockReturnValue({
      mutateAsync: mutatePass2,
      isPending: false,
    } as any);

    vi.mocked(queryHooks.useToggleBookmark).mockReturnValue({
      mutate: mutateBookmark,
      isPending: false,
    } as any);
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/questions/q1']}>
          <Routes>
            <Route path="/questions/:id" element={<QuestionDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('honest-judge PASS - renders question statement, options, marks, and year', () => {
    renderComponent();
    expect(screen.getByText('What is the page table base address in hardware?')).toBeInTheDocument();
    expect(screen.getByText(/GATE-CS-2023-Q01/)).toBeInTheDocument();
    expect(screen.getByText('1 Mark')).toBeInTheDocument();
    expect(screen.getByText('Stored in Page Table Base Register (PTBR)')).toBeInTheDocument();
  });

  it('honest-judge PASS - answer key and explanation are hidden by default', () => {
    renderComponent();
    // Explanation and correct answer box must NOT be in document before reveal
    expect(screen.queryByText('Answer Key & Detailed Explanation')).toBeNull();
    expect(screen.queryByText('PTBR holds the physical base address of the current process page table.')).toBeNull();
    expect(screen.getByText('Reveal Solution & Key')).toBeInTheDocument();
  });

  it('honest-judge PASS - clicking Reveal Solution & Key unmasks correct options and explanation', () => {
    renderComponent();
    const revealButton = screen.getByText('Reveal Solution & Key');
    fireEvent.click(revealButton);

    expect(screen.getByText('Answer Key & Detailed Explanation')).toBeInTheDocument();
    expect(screen.getByText('PTBR holds the physical base address of the current process page table.')).toBeInTheDocument();
    expect(screen.getByText('Mask Solution')).toBeInTheDocument();
  });

  it('honest-judge PASS - clicking Pass 1 status button calls updatePass1 mutation with correct payload', () => {
    renderComponent();
    const solvedButtons = screen.getAllByRole('button', { name: 'SOLVED' });
    fireEvent.click(solvedButtons[0]); // First SOLVED button is Pass 1

    expect(mutatePass1).toHaveBeenCalledWith({ id: 'q1', status: 'SOLVED' });
  });

  it('honest-judge PASS - NAT question displays tolerance range after solution reveal', () => {
    const mockNat: QuestionDetail = {
      ...mockMcqQuestion,
      type: 'NAT',
      optionsJson: null,
      correctOptions: null,
      numericMin: 2.99,
      numericMax: 3.01,
      explanation: 'Calculated speedup is 3.00.',
    };

    vi.mocked(queryHooks.useQuestion).mockReturnValue({
      data: mockNat,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    } as any);

    renderComponent();
    expect(screen.getByText('Numerical Answer (NAT):')).toBeInTheDocument();

    const revealBtn = screen.getByText('Reveal Solution & Key');
    fireEvent.click(revealBtn);

    expect(screen.getByText('[2.99 to 3.01]')).toBeInTheDocument();
    expect(screen.getByText('Calculated speedup is 3.00.')).toBeInTheDocument();
  });

  it('honest-judge PASS - renders concept chips in solution panel when revealed and clicking chip opens concept lesson dialog (PC-3)', async () => {
    const mockWithConcepts: QuestionDetail = {
      ...mockMcqQuestion,
      conceptTags: ['paging', 'ptbr-hardware'],
    };

    vi.mocked(queryHooks.useQuestion).mockReturnValue({
      data: mockWithConcepts,
      isLoading: false,
      error: null,
      refetch: vi.fn(),
    } as any);

    renderComponent();

    // Solution not revealed -> concept chips not shown
    expect(screen.queryByTestId('solution-concept-chips')).toBeNull();

    // Reveal solution
    fireEvent.click(screen.getByText('Reveal Solution & Key'));

    expect(screen.getByTestId('solution-concept-chips')).toBeInTheDocument();
    expect(screen.getByTestId('concept-chip-paging')).toBeInTheDocument();
    expect(screen.getByTestId('concept-chip-ptbr-hardware')).toBeInTheDocument();

    // Clicking concept chip opens dialog
    fireEvent.click(screen.getByTestId('concept-chip-paging'));

    expect(await screen.findByTestId('concept-unit-dialog')).toBeInTheDocument();
    expect(await screen.findByText('Paging Architecture')).toBeInTheDocument();
  });
});

