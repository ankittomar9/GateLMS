import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MistakeNotebookPage } from '../MistakeNotebookPage';
import * as queryHooks from '../../hooks/useQueries';
import { MistakeDto } from '../../api/types';

vi.mock('../../hooks/useQueries', () => ({
  useMistakes: vi.fn(),
  useReviewMistake: vi.fn(),
}));

const mockMistakes: MistakeDto[] = [
  {
    id: 'm-01',
    questionId: 'q-01',
    questionExternalId: 'GATE-2024-Q1',
    questionStatement: 'Consider a cache with 64-byte block size and 4-way associativity.',
    attemptAnswerId: 'ans-01',
    errorCategory: 'CONCEPTUAL',
    description: 'Confused offset bit calculation with tag bits',
    correctiveTakeaway: 'Always write out the address partition formula: Tag + Set + Offset',
    loggedAt: '2026-10-05T10:00:00Z',
    reviewCount: 0,
    nextReviewAt: '2026-10-06T10:00:00Z',
    resolved: false,
    conceptTags: ['cache-organization', 'set-associative-mapping'],
  },
  {
    id: 'm-02',
    questionId: 'q-02',
    questionExternalId: 'GATE-2024-Q2',
    questionStatement: 'Which of the following statements about context switches is TRUE?',
    attemptAnswerId: 'ans-02',
    errorCategory: 'MSQ_TRAP',
    description: 'Missed option C which was also correct',
    correctiveTakeaway: 'In MSQ, evaluate each statement independently against definitions',
    loggedAt: '2026-10-04T12:00:00Z',
    reviewCount: 2,
    nextReviewAt: '2026-10-11T12:00:00Z',
    resolved: false,
  },
];

describe('MistakeNotebookPage', () => {
  let queryClient: QueryClient;
  const mockReviewMutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    (queryHooks.useMistakes as any).mockReturnValue({
      data: {
        content: mockMistakes,
        totalElements: 2,
        totalPages: 1,
        page: 0,
        size: 50,
      },
      isLoading: false,
      refetch: vi.fn(),
    });

    (queryHooks.useReviewMistake as any).mockReturnValue({
      mutateAsync: mockReviewMutate,
      isPending: false,
    });
  });

  const renderComponent = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/mistakes']}>
          <MistakeNotebookPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('renders page header and tabs', () => {
    renderComponent();

    expect(screen.getByText('Mistake Notebook')).toBeInTheDocument();
    expect(screen.getByText('Due for Review')).toBeInTheDocument();
    expect(screen.getByText('All Active')).toBeInTheDocument();
    expect(screen.getByText('Resolved')).toBeInTheDocument();
  });

  it('renders mistake cards with category chips, question statement, and takeaways', () => {
    renderComponent();

    expect(screen.getByText('CONCEPTUAL')).toBeInTheDocument();
    expect(screen.getByText('MSQ_TRAP')).toBeInTheDocument();
    expect(screen.getByText(/Always write out the address partition formula/i)).toBeInTheDocument();
    expect(screen.getByText(/In MSQ, evaluate each statement independently/i)).toBeInTheDocument();
    expect(screen.getByText(/Reviewed 0 times/i)).toBeInTheDocument();
    expect(screen.getByText(/Reviewed 2 times/i)).toBeInTheDocument();
  });

  it('calls review mutation with scheduled interval advancement', async () => {
    mockReviewMutate.mockResolvedValueOnce({
      ...mockMistakes[0],
      reviewCount: 1,
    });

    renderComponent();

    const reviewButtons = screen.getAllByRole('button', { name: /mark reviewed/i });
    fireEvent.click(reviewButtons[0]);

    await waitFor(() => {
      expect(mockReviewMutate).toHaveBeenCalledWith({
        id: 'm-01',
        data: { resolved: false },
      });
    });
  });

  it('calls review mutation with resolved: true when Resolve Mistake is clicked', async () => {
    mockReviewMutate.mockResolvedValueOnce({
      ...mockMistakes[0],
      resolved: true,
    });

    renderComponent();

    const resolveButtons = screen.getAllByRole('button', { name: /resolve mistake/i });
    fireEvent.click(resolveButtons[0]);

    await waitFor(() => {
      expect(mockReviewMutate).toHaveBeenCalledWith({
        id: 'm-01',
        data: { resolved: true },
      });
    });
  });

  it('filters cards when category dropdown changes', async () => {
    renderComponent();

    const select = screen.getByRole('combobox');
    fireEvent.change(select, { target: { value: 'MSQ_TRAP' } });

    await waitFor(() => {
      expect(screen.queryByText('CONCEPTUAL')).not.toBeInTheDocument();
      expect(screen.getByText('MSQ_TRAP')).toBeInTheDocument();
    });
  });

  it('renders empty queue state when there are no mistakes', () => {
    (queryHooks.useMistakes as any).mockReturnValue({
      data: { content: [], totalElements: 0, totalPages: 0, page: 0, size: 50 },
      isLoading: false,
    });

    renderComponent();

    expect(screen.getByText('Queue is all caught up!')).toBeInTheDocument();
  });

  it('switches between Due for Review and Resolved tabs', () => {
    renderComponent();

    const resolvedTab = screen.getByRole('button', { name: 'Resolved' });
    fireEvent.click(resolvedTab);

    expect(resolvedTab).toHaveClass('bg-ds-accent');
  });

  it('surfaces tested concept chips on mistake cards and opens lesson dialog (PC-7 / WP3)', () => {
    renderComponent();

    expect(screen.getByTestId('mistake-concept-chips')).toBeInTheDocument();
    expect(screen.getByTestId('mistake-concept-chip-cache-organization')).toBeInTheDocument();
    expect(screen.getByTestId('mistake-concept-chip-set-associative-mapping')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('mistake-concept-chip-cache-organization'));
    expect(screen.getByTestId('concept-unit-dialog')).toBeInTheDocument();
  });
});
