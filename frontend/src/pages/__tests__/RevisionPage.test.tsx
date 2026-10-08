import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { RevisionPage } from '../RevisionPage';
import { PracticeRunnerPage } from '../PracticeRunnerPage';
import { revisionApi, RevisionOverviewDto } from '../../api/revisionApi';
import { ApiError } from '../../api/apiClient';

vi.mock('../../api/revisionApi', () => ({
  revisionApi: {
    getOverview: vi.fn(),
    getDueCards: vi.fn(),
    reviewCard: vi.fn(),
    createDrill: vi.fn(),
    redrillMistake: vi.fn(),
    toggleBookmark: vi.fn(),
  },
}));

vi.mock('../../components/MathText', () => ({
  MathText: ({ content }: { content: string }) => <div data-testid="mock-mathtext">{content}</div>,
}));

vi.mock('../../hooks/useQueries', () => ({
  useSubmitPracticeAnswer: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useSkipPracticeQuestion: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useRevealPracticeQuestion: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useFinishPracticeSession: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useCreateMistake: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

const mockOverview: RevisionOverviewDto = {
  dueMistakes: 5,
  dueTopics: 3,
  pass2Pending: 4,
  dueCards: 2,
  bookmarked: 8,
  drillsCompleted: 12,
  generatedAt: '2026-10-07T10:00:00Z',
};

describe('RevisionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1 Renders four source cards with counts from fixture', async () => {
    vi.mocked(revisionApi.getOverview).mockResolvedValue(mockOverview);

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('revision-page')).toBeInTheDocument();
    });

    expect(screen.getByTestId('count-due-mistakes')).toHaveTextContent('5');
    expect(screen.getByTestId('count-due-topics')).toHaveTextContent('3');
    expect(screen.getByTestId('count-pass2')).toHaveTextContent('4');
    expect(screen.getByTestId('count-due-cards')).toHaveTextContent('2');
    expect(screen.getByTestId('count-bookmarked')).toHaveTextContent('8');
    expect(screen.getByTestId('drills-completed-count')).toHaveTextContent('12');
  });

  it('2 Drill CTA posts correct source payload and navigates', async () => {
    vi.mocked(revisionApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(revisionApi.createDrill).mockResolvedValue({
      sessionId: 'sess-1',
      attemptId: 'att-1',
      questions: [],
      totalQuestions: 0,
    });

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('btn-drill-due-mistakes')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('btn-drill-due-mistakes'));

    await waitFor(() => {
      expect(revisionApi.createDrill).toHaveBeenCalledWith({
        source: 'DUE_MISTAKES',
        count: 15,
      });
    });
  });

  it('3 Displays due-today hero banner when queue has items', async () => {
    vi.mocked(revisionApi.getOverview).mockResolvedValue(mockOverview);

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('due-today-hero')).toBeInTheDocument();
    });

    // 5 + 3 + 4 + 2 = 14 total due (mistakes + topics + pass2 + retrieval cards)
    expect(screen.getByTestId('due-today-hero')).toHaveTextContent('14');
  });

  it('4 Displays empty state when all revision queues are clear', async () => {
    vi.mocked(revisionApi.getOverview).mockResolvedValue({
      dueMistakes: 0,
      dueTopics: 0,
      pass2Pending: 0,
      dueCards: 0,
      bookmarked: 0,
      drillsCompleted: 5,
      generatedAt: '2026-10-07T10:00:00Z',
    });

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('revision-empty-state')).toBeInTheDocument();
    });

    expect(screen.queryByTestId('due-today-hero')).not.toBeInTheDocument();
    expect(screen.getByText('Nothing Due for Revision!')).toBeInTheDocument();
    expect(screen.getByText('Take a Mock Test')).toBeInTheDocument();
  });

  it('5 Drill runner renders Weakness Drill badge from search params', async () => {
    const fakeSession = {
      sessionId: 'sess-drill-1',
      attemptId: 'att-drill-1',
      questions: [
        {
          id: 'q-drill-1',
          statement: 'TCP Slow Start Question',
          type: 'MCQ',
          marks: 1,
        },
      ],
      totalQuestions: 1,
    };
    sessionStorage.setItem('practice_session_att-drill-1', JSON.stringify(fakeSession));

    render(
      <MemoryRouter initialEntries={['/practice/sessions/att-drill-1?drill=true&source=DUE_MISTAKES']}>
        <Routes>
          <Route path="/practice/sessions/:attemptId" element={<PracticeRunnerPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('drill-runner-badge')).toBeInTheDocument();
    });

    expect(screen.getByTestId('drill-runner-badge')).toHaveTextContent('Weakness Drill · DUE_MISTAKES');
  });

  it('6 Overview error shows retry banner and recovers on retry', async () => {
    vi.mocked(revisionApi.getOverview).mockRejectedValueOnce(new Error('Network error'));

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('revision-error')).toBeInTheDocument();
    });

    expect(screen.getByText('Network error')).toBeInTheDocument();

    // Now resolve on retry
    vi.mocked(revisionApi.getOverview).mockResolvedValueOnce(mockOverview);
    fireEvent.click(screen.getByText('Retry'));

    await waitFor(() => {
      expect(screen.getByTestId('revision-page')).toBeInTheDocument();
    });
  });

  it('7 Action error banner is displayed when drill creation fails', async () => {
    vi.mocked(revisionApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(revisionApi.createDrill).mockRejectedValueOnce(
      new ApiError(409, 'Conflict', undefined, 'No candidates for this drill source')
    );

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('btn-drill-due-mistakes')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('btn-drill-due-mistakes'));

    await waitFor(() => {
      expect(screen.getByTestId('action-error-banner')).toBeInTheDocument();
    });

    expect(screen.getByText('No candidates for this drill source')).toBeInTheDocument();
  });

  it('8 WP2 - Review Cards opens the flashcard drawer, grades a card, and refreshes the overview', async () => {
    vi.mocked(revisionApi.getOverview).mockResolvedValue(mockOverview);
    vi.mocked(revisionApi.getDueCards).mockResolvedValue({
      dueCount: 1,
      cards: [
        {
          cardId: 'card-uuid-1',
          prompt: 'State the lock-release rule that distinguishes strict 2PL from basic 2PL.',
          answer: 'Exclusive locks are released only at transaction commit or abort.',
          conceptSlug: 'strict-2pl',
          conceptLabel: 'Strict Two-Phase Locking',
          unitTitle: 'Strict 2PL: The Locking Discipline',
          status: 'DRAFT',
        },
      ],
    });
    vi.mocked(revisionApi.reviewCard).mockResolvedValue({
      nextDueAt: '2026-10-09T12:00:00Z',
      intervalDays: 1,
      ease: 2.5,
    });

    render(
      <MemoryRouter>
        <RevisionPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('btn-review-cards')).toBeInTheDocument();
    });

    // Tile renders the dueCards count from the overview
    expect(screen.getByTestId('count-due-cards')).toHaveTextContent('2');

    fireEvent.click(screen.getByTestId('btn-review-cards'));

    // Drawer fetches the due queue and renders the first prompt
    await waitFor(() => {
      expect(screen.getByTestId('card-prompt')).toBeInTheDocument();
    });
    expect(screen.getByTestId('card-prompt')).toHaveTextContent(/lock-release rule/i);

    // Reveal then grade SOLID
    fireEvent.click(screen.getByTestId('card-reveal-btn'));
    await waitFor(() => {
      expect(screen.getByTestId('grade-solid-btn')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('grade-solid-btn'));

    await waitFor(() => {
      expect(revisionApi.reviewCard).toHaveBeenCalledWith('card-uuid-1', 2);
    });

    // Single card reviewed -> session complete -> close -> overview refetched
    await waitFor(() => {
      expect(screen.getByTestId('card-drawer-complete')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('card-drawer-done-btn'));

    await waitFor(() => {
      expect(revisionApi.getOverview).toHaveBeenCalledTimes(2);
    });
  });
});
