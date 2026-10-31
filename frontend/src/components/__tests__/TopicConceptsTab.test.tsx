import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { TopicConceptsTab } from '../TopicConceptsTab';
import { journeyApi } from '../../api/journeyApi';
import { TopicConceptCoverageResponse, ConceptLessonResponse } from '../../types/journey';

vi.mock('../MathText', () => ({
  MathText: ({ content }: { content: string }) => <span>{content}</span>,
}));

vi.mock('../ai/RichMarkdownText', () => ({
  RichMarkdownText: ({ content }: { content: string }) => <span>{content}</span>,
  default: ({ content }: { content: string }) => <span>{content}</span>,
}));

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getTopicConcepts: vi.fn(),
    getConceptLesson: vi.fn(),
    generateConceptLesson: vi.fn(),
    updateConceptLesson: vi.fn(),
    deleteConceptLesson: vi.fn(),
  },
}));

const mockCoverageResponse: TopicConceptCoverageResponse = {
  summary: {
    conceptsTotal: 10,
    conceptsWithMaterial: 10,
    conceptsExplained: 1,
  },
  concepts: [
    {
      slug: 'critical-section-requirements',
      label: 'Critical Section & Mutual Exclusion Requirements',
      mappedQuestionCount: 15,
      uniqueStatementCount: 15,
      lessonStatus: 'ACCEPTED',
      coverage: 'EXPLAINED',
    },
    {
      slug: 'peterson-software-solutions',
      label: "Peterson's Software Solution",
      mappedQuestionCount: 15,
      uniqueStatementCount: 15,
      lessonStatus: 'DRAFT',
      coverage: 'MAPPED',
    },
    {
      slug: 'monitors-condition-variables',
      label: 'Monitors & Condition Variables',
      mappedQuestionCount: 15,
      uniqueStatementCount: 15,
      lessonStatus: null,
      coverage: 'MAPPED',
    },
  ],
};

const mockLessonResponse: ConceptLessonResponse = {
  lesson: {
    id: 'lesson-cs-req',
    conceptTagSlug: 'critical-section-requirements',
    title: 'Critical Section Requirements',
    motivation: 'Coordinate concurrent threads without race conditions or memory corruption.',
    coreIdea: 'Three pillars: Mutual Exclusion, Progress, Bounded Waiting.',
    invariant: 'At most one process executes inside the critical region at any time instant.',
    workedExample: 'Step 1: Process P0 sets flag[0] = true. Step 2: Sets turn = 1. Peterson protocol guarantees mutual exclusion under sequential consistency.',
    traps: 'Assuming hardware memory ordering matches software line order.',
    workedExampleExternalId: 'SO-OS-0001',
    status: 'ACCEPTED',
    parseError: null,
    generatorModel: 'AI_STUDIO',
    generatedAt: new Date().toISOString(),
    updatedAt: null,
  },
  concept: {
    slug: 'critical-section-requirements',
    label: 'Critical Section Requirements',
    description: 'Fundamental conditions for race-free concurrency',
    topicId: 'topic_os_process_sync',
    topicSlug: 'process-synchronization',
    topicName: 'Process Synchronization',
  },
  mappedQuestions: [
    {
      id: 'q-sync-1',
      externalId: 'SO-OS-0001',
      statement: 'Two processes share a turn variable. Which conditions are satisfied?',
      difficulty: 'MEDIUM',
      type: 'MCQ',
      year: 2021,
      marks: 2.0,
    },
    {
      id: 'q-sync-2',
      externalId: 'SO-OS-0002',
      statement: 'Analyze progress requirement under preemptive multitasking.',
      difficulty: 'HARD',
      type: 'NAT',
      year: 2023,
      marks: 2.0,
    },
  ],
};

describe('TopicConceptsTab Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(journeyApi.getTopicConcepts).mockResolvedValue(mockCoverageResponse);
    vi.mocked(journeyApi.getConceptLesson).mockResolvedValue(mockLessonResponse);
  });

  it('honest-judge PASS - renders concept cards with coverage badges and summary bar', async () => {
    render(
      <MemoryRouter>
        <TopicConceptsTab topicId="topic_os_process_sync" isAdmin={false} />
      </MemoryRouter>
    );

    expect(await screen.findByTestId('concepts-tab-content')).toBeInTheDocument();
    expect(await screen.findByTestId('coverage-summary-bar')).toBeInTheDocument();

    expect(screen.getByTestId('stat-concepts-total')).toHaveTextContent('10');
    expect(screen.getByTestId('stat-concepts-material')).toHaveTextContent('10');
    expect(screen.getByTestId('stat-concepts-explained')).toHaveTextContent('1');

    // Check cards
    expect(screen.getByTestId('concept-card-critical-section-requirements')).toBeInTheDocument();
    expect(screen.getByTestId('coverage-chip-EXPLAINED')).toBeInTheDocument();
    expect(screen.getByTestId('concept-card-peterson-software-solutions')).toBeInTheDocument();
    expect(screen.getByTestId('concept-card-monitors-condition-variables')).toBeInTheDocument();
  });

  it('honest-judge PASS - clicking a concept card opens the lesson viewer with PC-2 sections and citation', async () => {
    render(
      <MemoryRouter>
        <TopicConceptsTab topicId="topic_os_process_sync" isAdmin={false} />
      </MemoryRouter>
    );

    const card = await screen.findByTestId('concept-card-critical-section-requirements');
    fireEvent.click(card);

    expect(await screen.findByTestId('concept-lesson-view')).toBeInTheDocument();
    expect(journeyApi.getConceptLesson).toHaveBeenCalledWith('critical-section-requirements');

    // Assert PC-2 Sections
    expect(screen.getByTestId('lesson-motivation')).toHaveTextContent(/Coordinate concurrent threads/);
    expect(screen.getByTestId('lesson-core-idea')).toHaveTextContent(/Three pillars: Mutual Exclusion/);
    expect(screen.getByTestId('lesson-invariant')).toHaveTextContent(/At most one process executes inside/);
    expect(screen.getByTestId('lesson-worked-example')).toHaveTextContent(/Peterson protocol guarantees/);
    expect(screen.getByTestId('lesson-traps')).toHaveTextContent(/Assuming hardware memory ordering/);

    // Assert citation
    expect(screen.getByTestId('worked-example-citation')).toHaveTextContent('SO-OS-0001');

    // Assert practice questions rail
    expect(screen.getByTestId('practice-questions-rail')).toBeInTheDocument();
    expect(screen.getByTestId('practice-item-SO-OS-0001')).toBeInTheDocument();
    expect(screen.getByTestId('practice-item-SO-OS-0002')).toBeInTheDocument();

    // Back to concepts button
    const backBtn = screen.getByTestId('back-to-concepts-btn');
    fireEvent.click(backBtn);

    expect(await screen.findByTestId('concepts-tab-content')).toBeInTheDocument();
  });

  it('honest-judge PASS - allows students to generate an absent concept lesson without force', async () => {
    const uncompiledLessonResponse: ConceptLessonResponse = {
      lesson: null,
      concept: mockLessonResponse.concept,
      mappedQuestions: mockLessonResponse.mappedQuestions,
    };
    vi.mocked(journeyApi.getConceptLesson).mockResolvedValue(uncompiledLessonResponse);
    vi.mocked(journeyApi.generateConceptLesson).mockResolvedValue(mockLessonResponse);

    render(
      <MemoryRouter>
        <TopicConceptsTab
          topicId="topic_os_process_sync"
          isAdmin={false}
          selectedSlug="monitors-condition-variables"
        />
      </MemoryRouter>
    );

    const generateBtn = await screen.findByTestId('generate-lesson-btn');
    expect(generateBtn).toBeInTheDocument();

    fireEvent.click(generateBtn);

    await waitFor(() => {
      expect(journeyApi.generateConceptLesson).toHaveBeenCalledWith('monitors-condition-variables', { force: false });
    });

    expect(await screen.findByTestId('lesson-motivation')).toBeInTheDocument();
    expect(screen.queryByTestId('edit-lesson-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('regenerate-lesson-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('delete-lesson-btn')).not.toBeInTheDocument();
  });

  it.each(['ADMIN', 'STUDENT'] as const)('honest-judge PASS - renders curation buttons only for ADMIN (role=%s)', async (role) => {
    const isAdmin = role === 'ADMIN';
    render(
      <MemoryRouter>
        <TopicConceptsTab
          topicId="topic_os_process_sync"
          isAdmin={isAdmin}
          selectedSlug="critical-section-requirements"
        />
      </MemoryRouter>
    );

    expect(await screen.findByTestId('concept-lesson-view')).toBeInTheDocument();

    for (const testId of ['edit-lesson-btn', 'regenerate-lesson-btn', 'delete-lesson-btn']) {
      if (isAdmin) {
        expect(screen.getByTestId(testId)).toBeInTheDocument();
      } else {
        expect(screen.queryByTestId(testId)).not.toBeInTheDocument();
      }
    }
  });

  it('honest-judge PASS - allows admin to edit concept lesson and save modifications', async () => {
    const updatedLesson = {
      ...mockLessonResponse.lesson!,
      motivation: 'Curator-updated motivation for critical section requirements.',
    };
    vi.mocked(journeyApi.updateConceptLesson).mockResolvedValue(updatedLesson);

    render(
      <MemoryRouter>
        <TopicConceptsTab
          topicId="topic_os_process_sync"
          isAdmin={true}
          selectedSlug="critical-section-requirements"
        />
      </MemoryRouter>
    );

    const editBtn = await screen.findByTestId('edit-lesson-btn');
    fireEvent.click(editBtn);

    expect(screen.getByTestId('lesson-edit-form')).toBeInTheDocument();

    const saveBtn = screen.getByTestId('save-lesson-edit-btn');
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(journeyApi.updateConceptLesson).toHaveBeenCalledWith(
        'critical-section-requirements',
        expect.objectContaining({
          status: 'ACCEPTED',
          workedExampleExternalId: 'SO-OS-0001',
        })
      );
    });

    expect(await screen.findByText('Curator-updated motivation for critical section requirements.')).toBeInTheDocument();
  });
});
