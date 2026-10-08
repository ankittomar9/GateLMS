import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TopicPage } from '../TopicPage';
import { journeyApi } from '../../api/journeyApi';
import * as queryHooks from '../../hooks/useQueries';
import { TopicPrimerResponse } from '../../types/journey';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ topicId: 'topic_vm' }),
  };
});

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getTopicPrimer: vi.fn(),
    generateTopicPrimer: vi.fn(),
  },
}));

vi.mock('../../hooks/useQueries', () => ({
  useQuestions: vi.fn(),
}));

// Mock MathText to avoid KaTeX rendering issues in test environment
vi.mock('../../components/MathText', () => ({
  MathText: ({ content }: { content: string }) => <span>{content}</span>,
}));

const mockPrimerResponse: TopicPrimerResponse = {
  primer: {
    id: 'primer-1',
    topicId: 'topic_vm',
    motivation: 'Virtual memory decouples logical address space from physical memory.',
    coreIdea: 'Paging divides memory into fixed-size pages and frames with translation via page table.',
    invariant: 'Physical Address = (Frame Number * Page Size) + Offset',
    workedExample: 'Consider a 32-bit virtual address architecture with 4 KB page size. The offset requires 12 bits. The remaining 20 bits index into the page table. If the page table entry is 4 bytes, a single-level page table requires 4 MB contiguous space. Multi-level paging solves this by introducing hierarchical index tables.',
    traps: 'Confusing page size with frame size or forgetting that offset is invariant under translation.',
    status: 'ACCEPTED',
    parseError: null,
    generatorModel: 'AI_STUDIO',
    generatedAt: new Date().toISOString(),
  },
  conceptSlugs: ['paging', 'tlb-hit-ratio', 'page-fault-frequency'],
  bookChapterRef: 'Silberschatz Chapter 9',
  subjectStandardBookRef: 'Silberschatz & Galvin',
};

const mockQuestions = [
  {
    id: 'q-101',
    externalId: 'q-101-ext',
    subjectId: 'sub_os',
    subjectName: 'Operating Systems',
    topicId: 'topic_vm',
    topicName: 'Virtual Memory & Paging',
    statement: 'An OS uses 32-bit virtual addresses and 4KB page size. How many pages exist?',
    difficulty: 'EASY',
    marks: 1.0,
    year: 2022,
    type: 'MCQ',
    pass1Status: 'SOLVED',
    pass2Status: 'SOLVED',
    attemptCount: 1,
    isBookmarked: true,
  },
  {
    id: 'q-102',
    externalId: 'q-102-ext',
    subjectId: 'sub_os',
    subjectName: 'Operating Systems',
    topicId: 'topic_vm',
    topicName: 'Virtual Memory & Paging',
    statement: 'Calculate the effective memory access time with TLB hit ratio 90%.',
    difficulty: 'MEDIUM',
    marks: 2.0,
    year: 2023,
    type: 'MCQ',
    pass1Status: 'UNSEEN',
    pass2Status: 'NOT_ATTEMPTED',
    attemptCount: 0,
    isBookmarked: false,
  },
];

describe('TopicPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(queryHooks.useQuestions).mockReturnValue({
      data: { content: mockQuestions, totalElements: 2 },
      isLoading: false,
    } as any);
  });

  it('renders Theory tab first with primer content (PC-4 theory-first)', async () => {
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('topic-page')).toBeInTheDocument();
    });

    expect(screen.getByText(/Virtual memory decouples logical address space/)).toBeInTheDocument();
    expect(screen.getByText(/Physical Address = \(Frame Number \* Page Size\) \+ Offset/)).toBeInTheDocument();
    expect(screen.getByText(/Consider a 32-bit virtual address architecture/)).toBeInTheDocument();

    // Check concept pills
    expect(screen.getByText('paging')).toBeInTheDocument();
    expect(screen.getByText('tlb-hit-ratio')).toBeInTheDocument();
  });

  it('switches between Theory, Questions, and Notebook tabs (PC-2)', async () => {
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('topic-page')).toBeInTheDocument();
    });

    // Switch to Practice Questions
    const questionsTab = screen.getByTestId('tab-questions');
    fireEvent.click(questionsTab);

    expect(screen.getByText(/An OS uses 32-bit virtual addresses/)).toBeInTheDocument();
    expect(screen.getByText(/Calculate the effective memory access time/)).toBeInTheDocument();

    // Switch to Solved Notebook
    const notebookTab = screen.getByTestId('tab-notebook');
    fireEvent.click(notebookTab);

    // Only solved question (q-101) should be in notebook
    expect(screen.getByText(/An OS uses 32-bit virtual addresses/)).toBeInTheDocument();
    expect(screen.queryByText(/Calculate the effective memory access time/)).not.toBeInTheDocument();
  });

  it('allows generating primer on demand if absent', async () => {
    const emptyPrimerRes: TopicPrimerResponse = {
      primer: null,
      conceptSlugs: [],
      bookChapterRef: null,
      subjectStandardBookRef: null,
    };

    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(emptyPrimerRes);
    vi.mocked(journeyApi.generateTopicPrimer).mockResolvedValue(mockPrimerResponse);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('topic-page')).toBeInTheDocument();
    });

    const generateBtn = screen.getByTestId('generate-primer-btn');
    expect(generateBtn).toBeInTheDocument();

    fireEvent.click(generateBtn);

    await waitFor(() => {
      expect(journeyApi.generateTopicPrimer).toHaveBeenCalledWith('topic_vm');
    });
  });
});
