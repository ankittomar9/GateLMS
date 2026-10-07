import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { ResultPage } from '../ResultPage';
import { MockResultResponse } from '../../api/types';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockResultFixture: MockResultResponse = {
  attemptId: 'att-res-1234',
  mockId: 'mock-1',
  mockTitle: 'GATE CSE Mock #1',
  startedAt: new Date(Date.now() - 7200000).toISOString(),
  submittedAt: new Date().toISOString(),
  durationSeconds: 7200,
  finishedReason: 'SUBMITTED',
  totalScore: 62.67,
  positiveScore: 65.0,
  negativeScore: 2.33,
  maxMarks: 100.0,
  timeSpentSeconds: 6800,
  sectionScores: [
    {
      subjectId: 'subj_algo',
      subjectName: 'Algorithms',
      totalQuestions: 10,
      attemptedCount: 8,
      correctCount: 6,
      wrongCount: 2,
      unattemptedCount: 2,
      marksAwarded: 10.66,
      maxMarks: 15.0,
      accuracy: 75.0,
      timeSpentSeconds: 1200,
    },
    {
      subjectId: 'subj_os',
      subjectName: 'Operating Systems',
      totalQuestions: 10,
      attemptedCount: 9,
      correctCount: 7,
      wrongCount: 2,
      unattemptedCount: 1,
      marksAwarded: 12.0,
      maxMarks: 15.0,
      accuracy: 77.8,
      timeSpentSeconds: 1100,
    },
  ],
  questions: [
    {
      questionId: 'q-mcq-1',
      externalId: 'GATE-2024-01',
      type: 'MCQ' as any,
      marks: 2,
      statement: 'What is the worst case time complexity of QuickSelect?',
      optionsJson: null,
      subjectId: 'subj_algo',
      subjectName: 'Algorithms',
      topicId: 'topic_algo',
      yourAnswer: 'C',
      numericAnswer: null,
      correctAnswer: 'C',
      numericMin: null,
      numericMax: null,
      isCorrect: true,
      marksAwarded: 2.0,
      state: 'ANSWERED',
      timeSpentSeconds: 120,
      explanation: 'Worst case is $O(n^2)$ when pivot selection is adversarial.',
    },
    {
      questionId: 'q-mcq-2',
      externalId: 'GATE-2024-02',
      type: 'MCQ' as any,
      marks: 2,
      statement: 'Which of the following is NP-Complete?',
      optionsJson: null,
      subjectId: 'subj_algo',
      subjectName: 'Algorithms',
      topicId: 'topic_algo',
      yourAnswer: 'A',
      numericAnswer: null,
      correctAnswer: 'B',
      numericMin: null,
      numericMax: null,
      isCorrect: false,
      marksAwarded: -0.67,
      state: 'ANSWERED',
      timeSpentSeconds: 90,
      explanation: '3-SAT is NP-Complete.',
    },
    {
      questionId: 'q-nat-3',
      externalId: 'GATE-2024-03',
      type: 'NAT' as any,
      marks: 1,
      statement: 'Number of page faults in FIFO with 3 frames:',
      optionsJson: null,
      subjectId: 'subj_os',
      subjectName: 'Operating Systems',
      topicId: 'topic_os',
      yourAnswer: null,
      numericAnswer: null,
      correctAnswer: null,
      numericMin: 9.0,
      numericMax: 9.0,
      isCorrect: null,
      marksAwarded: 0.0,
      state: 'NOT_VISITED',
      timeSpentSeconds: 0,
      explanation: 'Using standard reference string FIFO gives 9 faults.',
    },
  ],
};

vi.mock('../../hooks/useQueries', () => ({
  useMockResult: () => ({
    data: mockResultFixture,
    isLoading: false,
    error: null,
  }),
}));

describe('ResultPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient();
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/mocks/attempts/att-res-1234/result']}>
          <Routes>
            <Route path="/mocks/attempts/:attemptId/result" element={<ResultPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('renders score hero with exact total, positive, negative penalty, and accuracy', () => {
    renderComponent();

    expect(screen.getByText('62.67')).toBeInTheDocument();
    expect(screen.getByText('+65.00')).toBeInTheDocument();
    expect(screen.getByText('−2.33')).toBeInTheDocument();
    expect(screen.getByText(/Out of 100 marks/i)).toBeInTheDocument();
  });

  it('renders section-wise performance table with subjects and stats', () => {
    renderComponent();

    expect(screen.getAllByText('Algorithms').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Operating Systems').length).toBeGreaterThan(0);
    expect(screen.getByText('75.0%')).toBeInTheDocument();
    expect(screen.getByText('77.8%')).toBeInTheDocument();
  });

  it('filters question list by wrong-only and unattempted tabs', () => {
    renderComponent();

    // Default: All 3 questions present
    expect(screen.getByText(/QuickSelect/i)).toBeInTheDocument();
    expect(screen.getByText(/Which of the following is NP-Complete/i)).toBeInTheDocument();
    expect(screen.getByText(/FIFO with 3 frames/i)).toBeInTheDocument();

    // Filter Wrong Only
    const wrongTab = screen.getByRole('button', { name: /Wrong Only/i });
    fireEvent.click(wrongTab);

    expect(screen.getByText(/Which of the following is NP-Complete/i)).toBeInTheDocument();
    expect(screen.queryByText(/QuickSelect/i)).not.toBeInTheDocument();

    // Filter Unattempted Only
    const unattemptedTab = screen.getByRole('button', { name: /Unattempted/i });
    fireEvent.click(unattemptedTab);

    expect(screen.getByText(/FIFO with 3 frames/i)).toBeInTheDocument();
    expect(screen.queryByText(/Which of the following is NP-Complete/i)).not.toBeInTheDocument();
  });

  it('displays signed marks awarded and correct answer diff highlighting', () => {
    renderComponent();

    expect(screen.getByText('+2 Marks')).toBeInTheDocument();
    expect(screen.getByText('-0.67 Marks')).toBeInTheDocument();
    expect(screen.getByText('3-SAT is NP-Complete.')).toBeInTheDocument();
  });
});
