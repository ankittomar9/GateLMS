import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TopicPage } from '../TopicPage';
import { journeyApi } from '../../api/journeyApi';
import * as queryHooks from '../../hooks/useQueries';
import { TopicPrimerResponse } from '../../types/journey';
import { useAuth, User } from '../../context/AuthContext';

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
    updateTopicPrimer: vi.fn(),
    deleteTopicPrimer: vi.fn(),
    getTopicConcepts: vi.fn().mockResolvedValue({
      summary: { conceptsTotal: 10, conceptsWithMaterial: 10, conceptsExplained: 1 },
      concepts: [
        {
          slug: 'paging',
          label: 'Paging Architecture',
          mappedQuestionCount: 2,
          uniqueStatementCount: 2,
          lessonStatus: 'ACCEPTED',
          coverage: 'EXPLAINED',
        },
      ],
    }),
    getConceptLesson: vi.fn(),
  },
}));

vi.mock('../../hooks/useQueries', () => ({
  useQuestions: vi.fn(),
}));

vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn(),
}));

const mockAuthRole = (role: User['role']) => {
  vi.mocked(useAuth).mockReturnValue({
    user: { userId: 'user-1', username: 'test-user', displayName: 'Test User', role },
    isLoading: false,
    login: vi.fn(),
    logout: vi.fn(),
    refetchUser: vi.fn(),
  });
};

// Mock MathText to avoid KaTeX rendering issues in test environment
vi.mock('../../components/MathText', () => ({
  MathText: ({ content }: { content: string }) => <span>{content}</span>,
}));

vi.mock('../../components/ai/RichMarkdownText', () => ({
  RichMarkdownText: ({ content }: { content: string }) => <span>{content}</span>,
  default: ({ content }: { content: string }) => <span>{content}</span>,
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
    mockAuthRole('STUDENT');
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

  it('switches between Orientation, Concepts, Questions, and Notebook tabs freely (PC-5)', async () => {
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('topic-page')).toBeInTheDocument();
    });

    // Switch to Concepts tab (PC-5)
    const conceptsTab = screen.getByTestId('tab-concepts');
    fireEvent.click(conceptsTab);
    expect(await screen.findByTestId('concepts-tab-content')).toBeInTheDocument();

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

  it('allows students to generate an absent primer without force', async () => {
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

    const generateBtn = await screen.findByTestId('generate-primer-btn');
    expect(generateBtn).toBeInTheDocument();

    fireEvent.click(generateBtn);

    await waitFor(() => {
      expect(journeyApi.generateTopicPrimer).toHaveBeenCalledWith('topic_vm');
    });
    expect(await screen.findByText(mockPrimerResponse.primer!.motivation)).toBeInTheDocument();
    expect(journeyApi.generateTopicPrimer).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId('edit-primer-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('regenerate-primer-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('delete-primer-btn')).not.toBeInTheDocument();
  });

  it.each(['ADMIN', 'STUDENT'] as const)('renders primer management controls only for ADMIN (role=%s)', async (role) => {
    mockAuthRole(role);
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);

    render(<MemoryRouter><TopicPage /></MemoryRouter>);

    expect(await screen.findByText(mockPrimerResponse.primer!.motivation)).toBeInTheDocument();
    for (const testId of ['edit-primer-btn', 'regenerate-primer-btn', 'delete-primer-btn']) {
      if (role === 'ADMIN') {
        expect(screen.getByTestId(testId)).toBeInTheDocument();
      } else {
        expect(screen.queryByTestId(testId)).not.toBeInTheDocument();
      }
    }
  });

  it('hides failed-draft forced retry and discard actions from students', async () => {
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue({
      ...mockPrimerResponse,
      primer: { ...mockPrimerResponse.primer!, parseError: 'Provider unavailable' },
    });

    render(<MemoryRouter><TopicPage /></MemoryRouter>);

    expect(await screen.findByTestId('primer-error-banner')).toBeInTheDocument();
    expect(screen.getByText('Provider unavailable')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /retry synthesis now/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /discard failed draft/i })).not.toBeInTheDocument();
    expect(screen.queryByTestId('regenerate-primer-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('delete-primer-btn')).not.toBeInTheDocument();
  });

  it('allows editing primer and saving modifications', async () => {
    mockAuthRole('ADMIN');
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);
    const updatedPrimer = {
      ...mockPrimerResponse.primer!,
      motivation: 'Updated motivation text by teacher',
    };
    vi.mocked(journeyApi.updateTopicPrimer).mockResolvedValue(updatedPrimer);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('edit-primer-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('edit-primer-btn'));
    expect(screen.getByTestId('primer-edit-form')).toBeInTheDocument();

    const textareas = screen.getAllByRole('textbox');
    fireEvent.change(textareas[0], { target: { value: 'Updated motivation text by teacher' } });

    const saveBtn = screen.getByRole('button', { name: /save changes/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(journeyApi.updateTopicPrimer).toHaveBeenCalledWith(
        'topic_vm',
        expect.objectContaining({ motivation: 'Updated motivation text by teacher' })
      );
    });
  });

  it('allows regenerating primer with force=true', async () => {
    mockAuthRole('ADMIN');
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);
    vi.mocked(journeyApi.generateTopicPrimer).mockResolvedValue(mockPrimerResponse);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('regenerate-primer-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('regenerate-primer-btn'));

    await waitFor(() => {
      expect(journeyApi.generateTopicPrimer).toHaveBeenCalledWith('topic_vm', { force: true });
    });
  });

  it('allows deleting primer and confirms via window.confirm', async () => {
    mockAuthRole('ADMIN');
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(mockPrimerResponse);
    vi.mocked(journeyApi.deleteTopicPrimer).mockResolvedValue({ status: 'deleted', topicId: 'topic_vm' });
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('delete-primer-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('delete-primer-btn'));

    expect(confirmSpy).toHaveBeenCalled();
    await waitFor(() => {
      expect(journeyApi.deleteTopicPrimer).toHaveBeenCalledWith('topic_vm');
    });
    confirmSpy.mockRestore();
  });

  it('renders parseError alert banner when primer contains generation error', async () => {
    mockAuthRole('ADMIN');
    const errorPrimerRes: TopicPrimerResponse = {
      ...mockPrimerResponse,
      primer: {
        ...mockPrimerResponse.primer!,
        parseError: 'Provider returned 5xx server error: 503 Service Unavailable',
      },
    };
    vi.mocked(journeyApi.getTopicPrimer).mockResolvedValue(errorPrimerRes);

    render(
      <MemoryRouter>
        <TopicPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('primer-error-banner')).toBeInTheDocument();
    });
    expect(screen.getByText(/503 Service Unavailable/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry synthesis now/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /discard failed draft/i })).toBeInTheDocument();
  });
});
