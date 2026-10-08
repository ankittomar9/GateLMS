import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SimilarQuestionsPanel } from '../SimilarQuestionsPanel';
import { journeyApi } from '../../api/journeyApi';
import { SimilarQuestionsResponse } from '../../types/journey';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getSimilarQuestions: vi.fn(),
  },
}));

// Mock MathText to keep test output simple
vi.mock('../MathText', () => ({
  MathText: ({ content }: { content: string }) => <span>{content}</span>,
}));

const mockConceptResponse: SimilarQuestionsResponse = {
  similarityBasis: 'CONCEPT',
  questions: [
    {
      id: 'q-sim-1',
      externalId: 'ext-1',
      subjectId: 'subj_dbms',
      subjectName: 'DBMS',
      topicId: 'topic_ra',
      topicName: 'Relational Algebra',
      year: 2023,
      type: 'MCQ',
      marks: 2,
      statement: 'Find the minimum number of joins needed in query Q.',
      pass1Status: 'UNSEEN',
      pass2Status: 'NOT_ATTEMPTED',
      isBookmarked: false,
      difficulty: 'HARD',
      createdAt: new Date().toISOString(),
    },
    {
      id: 'q-sim-2',
      externalId: 'ext-2',
      subjectId: 'subj_dbms',
      subjectName: 'DBMS',
      topicId: 'topic_ra',
      topicName: 'Relational Algebra',
      year: 2022,
      type: 'NAT',
      marks: 1,
      statement: 'Calculate the cost of block nested loop join.',
      pass1Status: 'UNSEEN',
      pass2Status: 'NOT_ATTEMPTED',
      isBookmarked: false,
      difficulty: 'MEDIUM',
      createdAt: new Date().toISOString(),
    },
  ],
};

const mockTopicResponse: SimilarQuestionsResponse = {
  similarityBasis: 'TOPIC',
  questions: [
    {
      id: 'q-top-1',
      externalId: 'ext-3',
      subjectId: 'subj_dbms',
      subjectName: 'DBMS',
      topicId: 'topic_ra',
      topicName: 'Relational Algebra',
      year: 2024,
      type: 'MCQ',
      marks: 2,
      statement: 'Topic fallback question statement.',
      pass1Status: 'UNSEEN',
      pass2Status: 'NOT_ATTEMPTED',
      isBookmarked: false,
      difficulty: 'EASY',
      createdAt: new Date().toISOString(),
    },
  ],
};

describe('SimilarQuestionsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    vi.mocked(journeyApi.getSimilarQuestions).mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <SimilarQuestionsPanel questionId="q-main" />
      </MemoryRouter>
    );

    expect(screen.getByTestId('similar-questions-loading')).toBeInTheDocument();
  });

  it('renders panel with Same Concept badge and questions list (PC-5)', async () => {
    vi.mocked(journeyApi.getSimilarQuestions).mockResolvedValue(mockConceptResponse);

    render(
      <MemoryRouter>
        <SimilarQuestionsPanel questionId="q-main" />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('similar-questions-panel')).toBeInTheDocument();
    });

    expect(screen.getByText('Practice More Like This')).toBeInTheDocument();
    expect(screen.getByText('Same Concept')).toBeInTheDocument();

    expect(screen.getByText('Find the minimum number of joins needed in query Q.')).toBeInTheDocument();
    expect(screen.getByText('Calculate the cost of block nested loop join.')).toBeInTheDocument();
  });

  it('renders panel with Same Topic badge when basis is TOPIC (PC-5)', async () => {
    vi.mocked(journeyApi.getSimilarQuestions).mockResolvedValue(mockTopicResponse);

    render(
      <MemoryRouter>
        <SimilarQuestionsPanel questionId="q-main" />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('similar-questions-panel')).toBeInTheDocument();
    });

    expect(screen.getByText('Same Topic')).toBeInTheDocument();
    expect(screen.getByText('Topic fallback question statement.')).toBeInTheDocument();
  });

  it('navigates to question or calls onSelectQuestion when item is clicked', async () => {
    vi.mocked(journeyApi.getSimilarQuestions).mockResolvedValue(mockConceptResponse);
    const onSelect = vi.fn();

    render(
      <MemoryRouter>
        <SimilarQuestionsPanel questionId="q-main" onSelectQuestion={onSelect} />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('similar-questions-panel')).toBeInTheDocument();
    });

    const item = screen.getByTestId('similar-question-q-sim-1');
    fireEvent.click(item);

    expect(onSelect).toHaveBeenCalledWith('q-sim-1');
  });

  it('renders empty state when no similar questions exist', async () => {
    vi.mocked(journeyApi.getSimilarQuestions).mockResolvedValue({
      similarityBasis: 'TOPIC',
      questions: [],
    });

    render(
      <MemoryRouter>
        <SimilarQuestionsPanel questionId="q-main" />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('similar-questions-empty')).toBeInTheDocument();
    });

    expect(screen.getByText(/No similar unsolved questions found/)).toBeInTheDocument();
  });
});
