import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { TopicWorkbookTab, WorkbookQuestion } from '../TopicWorkbookTab';
import { noteApi, NoteItem, NotePageResponse } from '../../api/noteApi';
import { anatomyApi, QuestionAnatomyDto } from '../../api/anatomyApi';
import { TopicPrimer } from '../../types/journey';

vi.mock('../../api/noteApi', () => ({
  noteApi: {
    getNotes: vi.fn(),
    createTextNote: vi.fn(),
    getFileUrl: vi.fn((id) => `/api/v1/notes/${id}/file`),
  },
}));

vi.mock('../../api/anatomyApi', () => ({
  anatomyApi: {
    getQuestionAnatomy: vi.fn(),
  },
}));

vi.mock('../ai/RichMarkdownText', () => ({
  default: ({ content }: { content: string }) => <div data-testid="rich-markdown">{content}</div>,
}));

describe('TopicWorkbookTab', () => {
  const mockTopicId = 'top_c_pointers';
  const mockSubjectId = 'sub_cs';

  const mockPrimer: TopicPrimer = {
    id: 'primer-1',
    topicId: mockTopicId,
    motivation: 'Motivation text for C pointers.',
    coreIdea: 'Core idea explanation.',
    invariant: 'Invariant rule: pointer arithmetic scales by stride.',
    workedExample: 'Step 1: int *p = arr; Step 2: *(p+1).',
    traps: 'Trap: sizeof(arr) vs sizeof(ptr).',
    status: 'ACCEPTED',
    parseError: null,
    generatorModel: 'gemini-3.5-flash',
    generatedAt: '2026-11-01T00:00:00Z',
  };

  const mockQuestions: WorkbookQuestion[] = [
    {
      id: 'q1',
      externalId: 'ext-q1',
      subjectId: mockSubjectId,
      subjectName: 'Computer Science',
      topicId: mockTopicId,
      topicName: 'C Pointers & Memory',
      gateOverflowUrl: null,
      isBookmarked: false,
      pass2Status: 'PENDING' as any,
      version: 1,
      statement: 'What is the value of *(p + 2)?',
      type: 'MCQ' as any,
      marks: 2,
      year: 2024,
      options: [
        { key: 'A', text: 'Option A value' },
        { key: 'B', text: 'Option B value' },
      ],
      correctOptions: 'A',
      explanation: 'Official explanation: pointer scales by 2 * 4 bytes.',
      pass1Status: 'SOLVED' as any,
    },
    {
      id: 'q2',
      externalId: 'ext-q2',
      subjectId: mockSubjectId,
      subjectName: 'Computer Science',
      topicId: mockTopicId,
      topicName: 'C Pointers & Memory',
      gateOverflowUrl: null,
      isBookmarked: false,
      pass2Status: 'PENDING' as any,
      version: 1,
      statement: 'Calculate the total offset in bytes.',
      type: 'NAT' as any,
      marks: 1,
      year: 2023,
      numericMin: 8,
      numericMax: 8,
      explanation: 'Offset calculation yields 8.',
      pass1Status: 'PENDING' as any,
    },
  ];

  const mockTopicNote: NoteItem = {
    id: 'note-1',
    title: 'Pointer Quirks',
    filePath: null,
    originalFilename: null,
    contentType: null,
    fileSizeBytes: null,
    contentMd: 'Pointers scale according to their type.',
    subjectId: mockSubjectId,
    subjectName: 'Computer Science',
    topicId: mockTopicId,
    topicName: 'C Pointers',
    isPinned: true,
    kind: 'TEXT',
    version: 1,
    createdAt: '2026-10-30T10:00:00Z',
    updatedAt: '2026-10-31T12:00:00Z',
  };

  const mockSubjectNote: NoteItem = {
    id: 'note-2',
    title: 'General OS Memory',
    filePath: null,
    originalFilename: null,
    contentType: null,
    fileSizeBytes: null,
    contentMd: 'Stack grows down, heap grows up.',
    subjectId: mockSubjectId,
    subjectName: 'Computer Science',
    topicId: null, // subject-wide note
    topicName: null,
    isPinned: false,
    kind: 'TEXT',
    version: 1,
    createdAt: '2026-10-29T10:00:00Z',
    updatedAt: '2026-10-30T10:00:00Z',
  };

  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(noteApi.getNotes).mockImplementation(async (params: any) => {
      if (params?.topicId === mockTopicId) {
        return {
          content: [mockTopicNote],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          isFirst: true,
          isLast: true,
        };
      }
      if (params?.subjectId === mockSubjectId) {
        return {
          content: [mockSubjectNote],
          page: 0,
          size: 50,
          totalElements: 1,
          totalPages: 1,
          isFirst: true,
          isLast: true,
        };
      }
      return {
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 0,
        isFirst: true,
        isLast: true,
      };
    });
  });

  it('renders stitched notes with badges and pinned-first ordering', async () => {
    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        bookChapterRef="CLRS Ch 3"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[mockQuestions[0]]}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Pointer Quirks')).toBeInTheDocument();
      expect(screen.getByText('General OS Memory')).toBeInTheDocument();
    });

    expect(screen.getByText('[Topic Note]')).toBeInTheDocument();
    expect(screen.getByText('[Subject-Wide Note]')).toBeInTheDocument();

    const noteCards = screen.getAllByTestId(/^note-card-/);
    expect(noteCards[0]).toHaveTextContent('Pointer Quirks'); // pinned first
    expect(noteCards[1]).toHaveTextContent('General OS Memory');
  });

  it('supports pagination to exhaustion for notes', async () => {
    const page0Note: NoteItem = { ...mockTopicNote, id: 'n-p0', title: 'Page 0 Note' };
    const page1Note: NoteItem = { ...mockTopicNote, id: 'n-p1', title: 'Page 1 Note (51st Note)' };

    vi.mocked(noteApi.getNotes).mockImplementation(async (params: any) => {
      if (params?.topicId === mockTopicId) {
        if (params.page === 0) {
          return {
            content: [page0Note],
            page: 0,
            size: 50,
            totalElements: 2,
            totalPages: 2,
            isFirst: true,
            isLast: false,
          };
        }
        return {
          content: [page1Note],
          page: 1,
          size: 50,
          totalElements: 2,
          totalPages: 2,
          isFirst: false,
          isLast: true,
        };
      }
      return { content: [], page: 0, size: 50, totalElements: 0, totalPages: 0, isFirst: true, isLast: true };
    });

    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[]}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Page 0 Note')).toBeInTheDocument();
      expect(screen.getByText('Page 1 Note (51st Note)')).toBeInTheDocument();
    });
  });

  it('quick add note submits with topicId when unchecked, and without topicId when subject-wide checked', async () => {
    vi.mocked(noteApi.createTextNote).mockResolvedValue({} as any);

    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[]}
      />
    );

    // 1. Topic note (unchecked)
    fireEvent.click(screen.getByTestId('quick-add-note-btn'));
    expect(screen.getByTestId('quick-add-note-form')).toBeInTheDocument();

    fireEvent.change(screen.getByTestId('quick-add-title-input'), { target: { value: 'Topic Specific Note' } });
    fireEvent.change(screen.getByTestId('quick-add-content-input'), { target: { value: 'Content for topic.' } });
    fireEvent.click(screen.getByTestId('save-note-btn'));

    await waitFor(() => {
      expect(noteApi.createTextNote).toHaveBeenCalledWith({
        title: 'Topic Specific Note',
        contentMd: 'Content for topic.',
        subjectId: mockSubjectId,
        topicId: mockTopicId,
      });
    });

    // 2. Subject-wide note (checked)
    fireEvent.click(screen.getByTestId('quick-add-note-btn'));
    fireEvent.change(screen.getByTestId('quick-add-title-input'), { target: { value: 'Subject Wide Rule' } });
    fireEvent.click(screen.getByTestId('subject-wide-toggle'));
    fireEvent.click(screen.getByTestId('save-note-btn'));

    await waitFor(() => {
      expect(noteApi.createTextNote).toHaveBeenCalledWith({
        title: 'Subject Wide Rule',
        contentMd: '',
        subjectId: mockSubjectId,
        topicId: undefined,
      });
    });
  });

  it('renders all questions numbered with statement, options and solved badges', async () => {
    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[mockQuestions[0]]}
      />
    );

    expect(screen.getByText('Q1.')).toBeInTheDocument();
    expect(screen.getByText('Q2.')).toBeInTheDocument();
    expect(screen.getByText('SOLVED')).toBeInTheDocument();
    expect(screen.getByText('UNSOLVED')).toBeInTheDocument();
    expect(screen.getByText('What is the value of *(p + 2)?')).toBeInTheDocument();
    expect(screen.getByText('Calculate the total offset in bytes.')).toBeInTheDocument();
  });

  it('toggles reveal solution to display answer key and official explanation', async () => {
    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[]}
      />
    );

    const solBlock1 = screen.getByTestId('solution-block-q1');
    expect(solBlock1).toHaveClass('hidden');

    fireEvent.click(screen.getByTestId('reveal-solution-btn-q1'));
    expect(solBlock1).toHaveClass('block');
    expect(solBlock1).toHaveTextContent('Answer Key: A');
    expect(solBlock1).toHaveTextContent('Official explanation: pointer scales by 2 * 4 bytes.');

    // Toggle hide
    fireEvent.click(screen.getByTestId('reveal-solution-btn-q1'));
    expect(solBlock1).toHaveClass('hidden');
  });

  it('expands cognitive anatomy dossier on click', async () => {
    const mockDossier: QuestionAnatomyDto = {
      id: 'anat-1',
      questionId: 'q1',
      testedConcept: 'Pointer Scaled Stride',
      invariantRule: 'p + i == p + i * sizeof(*p)',
      trapAnalysis: [{ option: 'B', misconception: 'Added raw bytes without scaling stride.', verdict: 'TRAP' as any, why_plausible: 'Common error' }],
      prerequisites: [],
      similarPatternHint: null,
      parseError: null,
      generatorModel: null,
      updatedAt: null,
      status: 'ACCEPTED',
      generatedAt: '2026-10-31T00:00:00Z',
    };
    vi.mocked(anatomyApi.getQuestionAnatomy).mockResolvedValue(mockDossier);

    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[]}
      />
    );

    expect(screen.queryByTestId('anatomy-block-q1')).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId('expand-anatomy-btn-q1'));

    await waitFor(() => {
      expect(screen.getByTestId('anatomy-block-q1')).toBeInTheDocument();
    });

    expect(screen.getByText('Pointer Scaled Stride')).toBeInTheDocument();
    expect(screen.getByText('p + i == p + i * sizeof(*p)')).toBeInTheDocument();
    expect(screen.getByText('Added raw bytes without scaling stride.')).toBeInTheDocument();
  });

  it('invokes server-side PDF download blob flow on click', async () => {
    const mockBlob = new Blob(['%PDF-1.4 test bytes'], { type: 'application/pdf' });
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      headers: {
        get: (h: string) => (h.toLowerCase() === 'content-disposition' ? 'attachment; filename="topic_c-pointers_workbook.pdf"' : null),
      },
      blob: async () => mockBlob,
    });
    global.fetch = mockFetch;

    const createObjectURLMock = vi.fn(() => 'blob:http://localhost/test-blob');
    const revokeObjectURLMock = vi.fn();
    window.URL.createObjectURL = createObjectURLMock;
    window.URL.revokeObjectURL = revokeObjectURLMock;

    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[]}
      />
    );

    const downloadBtn = screen.getByTestId('download-pdf-btn');
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/journey/topics/top_c_pointers/workbook/pdf', expect.any(Object));
      expect(createObjectURLMock).toHaveBeenCalled();
    });
  });

  it('print classes exist structurally for @media print', () => {
    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={mockPrimer}
        questions={mockQuestions}
        solvedQuestions={[]}
      />
    );

    // Cover header carries hidden print:block
    const printCover = screen.getByText(/C Pointers & Memory — Reference Workbook/).closest('div');
    expect(printCover).toHaveClass('hidden');
    expect(printCover).toHaveClass('print:block');

    // Questions have break-inside-avoid
    const q1Card = screen.getByTestId('workbook-question-q1');
    expect(q1Card).toHaveClass('break-inside-avoid');
  });

  it('renders graceful empty states when no notes, no questions, and no primer', async () => {
    vi.mocked(noteApi.getNotes).mockResolvedValue({
      content: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
      isFirst: true,
      isLast: true,
    });

    render(
      <TopicWorkbookTab
        topicId={mockTopicId}
        topicName="C Pointers & Memory"
        subjectId={mockSubjectId}
        subjectName="Computer Science"
        primer={null}
        questions={[]}
        solvedQuestions={[]}
      />
    );

    await waitFor(() => {
      expect(screen.getByText('Theory primer has not been generated for this topic yet.')).toBeInTheDocument();
      expect(screen.getByText('No personal notes recorded for this topic or subject yet. Use Quick Add above!')).toBeInTheDocument();
      expect(screen.getByText('No practice questions available for this topic.')).toBeInTheDocument();
    });
  });
});
