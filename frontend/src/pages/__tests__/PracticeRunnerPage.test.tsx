import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PracticeRunnerPage } from '../PracticeRunnerPage';
import * as queryHooks from '../../hooks/useQueries';
import { PracticeSessionResponse, PracticeVerdictResponse, PracticeFinishResponse } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useSubmitPracticeAnswer: vi.fn(),
  useSkipPracticeQuestion: vi.fn(),
  useRevealPracticeQuestion: vi.fn(),
  useFinishPracticeSession: vi.fn(),
  useCreateMistake: vi.fn(),
}));

const mockSession: PracticeSessionResponse = {
  sessionId: 'sess-001',
  attemptId: 'att-001',
  totalQuestions: 2,
  questions: [
    {
      id: 'q-mcq-1',
      externalId: 'GATE-2024-01',
      type: 'MCQ',
      marks: 2,
      statement: 'What is the runtime of merge sort on $n$ elements?',
      optionsJson: JSON.stringify([
        { label: 'A', text: '$O(n)$' },
        { label: 'B', text: '$O(n \\log n)$' },
        { label: 'C', text: '$O(n^2)$' },
        { label: 'D', text: '$O(1)$' },
      ]),
      topicId: 'topic_algo',
      subjectId: 'subj_algo',
      year: 2024,
    },
    {
      id: 'q-nat-2',
      externalId: 'GATE-2024-02',
      type: 'NAT',
      marks: 1,
      statement: 'Enter the value of $2^{10}$:',
      optionsJson: null,
      topicId: 'topic_algo',
      subjectId: 'subj_algo',
      year: 2024,
    },
  ],
};

describe('PracticeRunnerPage', () => {
  let queryClient: QueryClient;
  const mockSubmitMutate = vi.fn();
  const mockSkipMutate = vi.fn();
  const mockRevealMutate = vi.fn();
  const mockFinishMutate = vi.fn();
  const mockMistakeMutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    sessionStorage.setItem('practice_session_att-001', JSON.stringify(mockSession));

    (queryHooks.useSubmitPracticeAnswer as any).mockReturnValue({
      mutateAsync: mockSubmitMutate,
      isPending: false,
    });
    (queryHooks.useSkipPracticeQuestion as any).mockReturnValue({
      mutateAsync: mockSkipMutate,
      isPending: false,
    });
    (queryHooks.useRevealPracticeQuestion as any).mockReturnValue({
      mutateAsync: mockRevealMutate,
      isPending: false,
    });
    (queryHooks.useFinishPracticeSession as any).mockReturnValue({
      mutateAsync: mockFinishMutate,
      isPending: false,
    });
    (queryHooks.useCreateMistake as any).mockReturnValue({
      mutateAsync: mockMistakeMutate,
      isPending: false,
    });
  });

  const renderComponent = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/practice/sessions/att-001']}>
          <Routes>
            <Route path="/practice/sessions/:attemptId" element={<PracticeRunnerPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('guarantees NO answer data or explanation in rendered DOM before submit (Honest-Judge Gate)', () => {
    const { container } = renderComponent();

    // Verify statement and options render
    expect(screen.getByText(/runtime of merge sort/i)).toBeInTheDocument();

    // Honest-Judge Assertion: Verify absence of sensitive answer indicators
    expect(screen.queryByText(/Explanation:/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Correct Option/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/Accepted Range/i)).not.toBeInTheDocument();
    expect(container.textContent).not.toContain('Divide-and-conquer recurrence');
  });

  it('handles option selection and answer submit flow', async () => {
    mockSubmitMutate.mockResolvedValueOnce({
      correct: true,
      marksAwarded: 2.0,
      correctOptions: 'B',
      explanation: 'Merge sort recurrence is T(n) = 2T(n/2) + O(n).',
      topicMastery: 4,
      mistakeLogPrompt: false,
    });

    renderComponent();

    // Click option B
    const optB = screen.getByText('B');
    fireEvent.click(optB);

    // Click submit
    const submitBtn = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockSubmitMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          questionId: 'q-mcq-1',
          selectedOptions: 'B',
        })
      );
    });
  });

  it('renders graded verdict panel with explanation and correct answer post-grade', async () => {
    mockSubmitMutate.mockResolvedValueOnce({
      correct: true,
      marksAwarded: 2.0,
      correctOptions: 'B',
      explanation: 'Recurrence T(n) = 2T(n/2) + O(n) yields Theta(n log n).',
      topicMastery: 4,
      mistakeLogPrompt: false,
    });

    renderComponent();

    const optB = screen.getByText('B');
    fireEvent.click(optB);
    const submitBtn = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Correct Answer!')).toBeInTheDocument();
      expect(screen.getByText('+2 Marks')).toBeInTheDocument();
      expect(screen.getByText('Explanation:')).toBeInTheDocument();
      expect(screen.getByText(/Recurrence T\(n\)/i)).toBeInTheDocument();
    });
  });

  it('displays mistake logging form on wrong answer and posts takeaway', async () => {
    mockSubmitMutate.mockResolvedValueOnce({
      correct: false,
      marksAwarded: 0.0,
      correctOptions: 'B',
      explanation: 'Divide and conquer takes O(n log n).',
      topicMastery: 2,
      mistakeLogPrompt: true,
    });
    mockMistakeMutate.mockResolvedValueOnce({ id: 'm-01' });

    renderComponent();

    // Click wrong option A
    const optA = screen.getByText('A');
    fireEvent.click(optA);
    const submitBtn = screen.getByRole('button', { name: /submit answer/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Incorrect')).toBeInTheDocument();
      expect(screen.getByText('Log to Mistake Notebook')).toBeInTheDocument();
    });

    // Select category MSQ_TRAP and write takeaway
    const trapBtn = screen.getByText('MSQ_TRAP');
    fireEvent.click(trapBtn);

    const textarea = screen.getByPlaceholderText(/What is your corrective takeaway/i);
    fireEvent.change(textarea, { target: { value: 'Forgot logarithmic factor in tree height' } });

    const saveMistakeBtn = screen.getByRole('button', { name: /save mistake/i });
    fireEvent.click(saveMistakeBtn);

    await waitFor(() => {
      expect(mockMistakeMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          questionId: 'q-mcq-1',
          errorCategory: 'MSQ_TRAP',
          correctiveTakeaway: 'Forgot logarithmic factor in tree height',
        })
      );
      expect(screen.getByText('Mistake saved to spaced review queue!')).toBeInTheDocument();
    });
  });

  it('skips question and advances to next question', async () => {
    mockSkipMutate.mockResolvedValueOnce(undefined);

    renderComponent();

    const skipBtn = screen.getByRole('button', { name: /skip/i });
    fireEvent.click(skipBtn);

    await waitFor(() => {
      expect(mockSkipMutate).toHaveBeenCalledWith('q-mcq-1');
      // Should advance to Q2 (NAT question)
      expect(screen.getByText(/Enter the value of/i)).toBeInTheDocument();
    });
  });

  it('shows reveal confirmation dialog and executes reveal on confirm', async () => {
    mockRevealMutate.mockResolvedValueOnce({
      correct: false,
      marksAwarded: 0.0,
      correctOptions: 'B',
      explanation: 'Revealed solution explanation',
      topicMastery: 3,
      mistakeLogPrompt: false,
    });

    renderComponent();

    const revealBtn = screen.getByRole('button', { name: /reveal answer/i });
    fireEvent.click(revealBtn);

    expect(screen.getByText('Reveal Answer?')).toBeInTheDocument();
    expect(screen.getByText(/mark this question as ASSISTED/i)).toBeInTheDocument();

    const confirmBtn = screen.getByRole('button', { name: /confirm reveal/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(mockRevealMutate).toHaveBeenCalledWith('q-mcq-1');
      expect(screen.getByText(/Revealed solution explanation/i)).toBeInTheDocument();
    });
  });

  it('renders updated topic mastery level delta chip post-grade', async () => {
    mockSubmitMutate.mockResolvedValueOnce({
      correct: true,
      marksAwarded: 2.0,
      correctOptions: 'B',
      explanation: 'Nice work!',
      topicMastery: 5,
      mistakeLogPrompt: false,
    });

    renderComponent();

    fireEvent.click(screen.getByText('B'));
    fireEvent.click(screen.getByRole('button', { name: /submit answer/i }));

    await waitFor(() => {
      expect(screen.getByText('Topic Mastery: Lvl 5/5')).toBeInTheDocument();
    });
  });

  it('renders summary view with per-topic breakdown when session is finished', async () => {
    mockFinishMutate.mockResolvedValueOnce({
      correct: 1,
      wrong: 1,
      skipped: 0,
      totalQuestions: 2,
      timeSpentSeconds: 120,
      byTopic: [
        { topicId: 'topic_algo', name: 'Algorithms', correct: 1, total: 2 },
      ],
    });

    renderComponent();

    const finishBtn = screen.getByRole('button', { name: /finish session/i });
    fireEvent.click(finishBtn);

    await waitFor(() => {
      expect(screen.getByText('Practice Session Summary')).toBeInTheDocument();
      expect(screen.getByText('Correct (Solved)')).toBeInTheDocument();
      expect(screen.getByText('Wrong Answers')).toBeInTheDocument();
      expect(screen.getByText('Performance By Topic')).toBeInTheDocument();
      expect(screen.getByText('Algorithms')).toBeInTheDocument();
      expect(screen.getByText('50%')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /return to dashboard/i })).toBeInTheDocument();
    });
  });
});
