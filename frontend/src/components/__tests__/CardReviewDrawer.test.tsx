import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CardReviewDrawer } from '../CardReviewDrawer';
import { revisionApi, DueCardsResponse } from '../../api/revisionApi';

vi.mock('../../api/revisionApi', () => ({
  revisionApi: {
    getDueCards: vi.fn(),
    reviewCard: vi.fn(),
  },
}));

vi.mock('../MathText', () => ({
  MathText: ({ content }: { content: string }) => <div data-testid="mock-mathtext">{content}</div>,
}));

const mockDueCards: DueCardsResponse = {
  dueCount: 2,
  cards: [
    {
      cardId: 'card-uuid-1',
      prompt: 'State the lock-release rule that distinguishes strict 2PL from basic 2PL with $X$-locks.',
      answer: 'Exclusive locks are released only at transaction commit or abort.',
      conceptSlug: 'strict-2pl',
      conceptLabel: 'Strict Two-Phase Locking',
      unitTitle: 'Strict 2PL: The Locking Discipline',
      status: 'DRAFT',
    },
    {
      cardId: 'card-uuid-2',
      prompt: 'Explain how strict 2PL prevents cascading aborts.',
      answer: 'No transaction can read uncommitted data, so no transaction ever depends on an aborted one.',
      conceptSlug: 'strict-2pl',
      conceptLabel: 'Strict Two-Phase Locking',
      unitTitle: 'Strict 2PL: The Locking Discipline',
      status: 'ACCEPTED',
    },
  ],
};

describe('CardReviewDrawer Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1 honest-judge PASS - fetches due cards and renders the first prompt with the answer concealed', async () => {
    vi.mocked(revisionApi.getDueCards).mockResolvedValue(mockDueCards);

    render(<CardReviewDrawer open={true} onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('card-prompt')).toBeInTheDocument();
    });

    expect(screen.getByText(/Card 1 \/ 2/)).toBeInTheDocument();
    expect(screen.getByTestId('mock-mathtext')).toHaveTextContent(/distinguishes strict 2PL/i);
    // Concept context is visible
    expect(screen.getByText('strict-2pl')).toBeInTheDocument();
    // DRAFT badge shown for unreviewed content
    expect(screen.getByText(/AI-GENERATED — UNREVIEWED/i)).toBeInTheDocument();
    // Answer must NOT be rendered before reveal
    expect(screen.queryByTestId('card-answer')).not.toBeInTheDocument();
    // Grade buttons must not exist before reveal (forces the attempt-first contract)
    expect(screen.queryByTestId('grade-solid-btn')).not.toBeInTheDocument();
    expect(revisionApi.reviewCard).not.toHaveBeenCalled();
  });

  it('2 honest-judge PASS - reveal exposes the answer, then SOLID grades with grade 2 and advances', async () => {
    const user = userEvent.setup();
    vi.mocked(revisionApi.getDueCards).mockResolvedValue(mockDueCards);
    vi.mocked(revisionApi.reviewCard).mockResolvedValue({
      nextDueAt: '2026-10-09T12:00:00Z',
      intervalDays: 1,
      ease: 2.5,
    });

    render(<CardReviewDrawer open={true} onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('card-reveal-btn')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('card-reveal-btn'));

    expect(screen.getByTestId('card-answer')).toBeInTheDocument();
    expect(screen.getByTestId('card-answer')).toHaveTextContent(/released only at transaction commit/i);

    await user.click(screen.getByTestId('grade-solid-btn'));

    await waitFor(() => {
      expect(revisionApi.reviewCard).toHaveBeenCalledWith('card-uuid-1', 2);
    });

    // Advanced to card 2, answer concealed again
    await waitFor(() => {
      expect(screen.getByText(/Card 2 \/ 2/)).toBeInTheDocument();
    });
    expect(screen.queryByTestId('card-answer')).not.toBeInTheDocument();
    expect(screen.queryByTestId('card-drawer-complete')).not.toBeInTheDocument();
  });

  it('3 honest-judge PASS - FORGOTTEN grades with 0', async () => {
    const user = userEvent.setup();
    vi.mocked(revisionApi.getDueCards).mockResolvedValue(mockDueCards);
    vi.mocked(revisionApi.reviewCard).mockResolvedValue({
      nextDueAt: '2026-10-09T12:00:00Z',
      intervalDays: 1,
      ease: 2.5,
    });

    render(<CardReviewDrawer open={true} onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('card-reveal-btn')).toBeInTheDocument();
    });
    await user.click(screen.getByTestId('card-reveal-btn'));
    await user.click(screen.getByTestId('grade-forgot-btn'));

    await waitFor(() => {
      expect(revisionApi.reviewCard).toHaveBeenCalledWith('card-uuid-1', 0);
    });
  });

  it('4 honest-judge PASS - grading the last card completes the session without extra fetches', async () => {
    const user = userEvent.setup();
    vi.mocked(revisionApi.getDueCards).mockResolvedValue({
      dueCount: 1,
      cards: [mockDueCards.cards[0]],
    });
    vi.mocked(revisionApi.reviewCard).mockResolvedValue({
      nextDueAt: '2026-10-09T12:00:00Z',
      intervalDays: 1,
      ease: 2.5,
    });

    render(<CardReviewDrawer open={true} onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('card-reveal-btn')).toBeInTheDocument();
    });
    await user.click(screen.getByTestId('card-reveal-btn'));
    await user.click(screen.getByTestId('grade-shaky-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('card-drawer-complete')).toBeInTheDocument();
    });
    expect(screen.getByTestId('card-drawer-complete')).toHaveTextContent(/You reviewed 1 card/i);
    expect(revisionApi.reviewCard).toHaveBeenCalledWith('card-uuid-1', 1);
    expect(revisionApi.getDueCards).toHaveBeenCalledTimes(1);
  });

  it('5 honest-judge PASS - empty due list renders the no-cards-due state', async () => {
    vi.mocked(revisionApi.getDueCards).mockResolvedValue({ dueCount: 0, cards: [] });

    render(<CardReviewDrawer open={true} onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('card-drawer-empty')).toBeInTheDocument();
    });
    expect(screen.getByText(/No Cards Due/i)).toBeInTheDocument();
    expect(revisionApi.reviewCard).not.toHaveBeenCalled();
  });

  it('6 honest-judge PASS - failed review does not advance the card and surfaces the error', async () => {
    const user = userEvent.setup();
    vi.mocked(revisionApi.getDueCards).mockResolvedValue(mockDueCards);
    vi.mocked(revisionApi.reviewCard).mockRejectedValueOnce(new Error('Network down'));

    render(<CardReviewDrawer open={true} onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId('card-reveal-btn')).toBeInTheDocument();
    });
    await user.click(screen.getByTestId('card-reveal-btn'));
    await user.click(screen.getByTestId('grade-solid-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('card-drawer-error')).toBeInTheDocument();
    });
    // Still on card 1 — a failed reschedule must never silently drop the card
    expect(screen.getByText(/Card 1 \/ 2/)).toBeInTheDocument();
    expect(screen.getByTestId('card-answer')).toBeInTheDocument();
  });

  it('7 honest-judge PASS - closed drawer renders nothing and fetches nothing', () => {
    render(<CardReviewDrawer open={false} onClose={vi.fn()} />);
    expect(screen.queryByTestId('card-drawer-body')).not.toBeInTheDocument();
    expect(revisionApi.getDueCards).not.toHaveBeenCalled();
  });
});
