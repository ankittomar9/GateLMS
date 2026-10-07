import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route, useLocation } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { QuestionDetailPage } from '../QuestionDetailPage';
import { QuestionsPage } from '../QuestionsPage';
import { apiClient } from '../../api/apiClient';
import * as queryHooks from '../../hooks/useQueries';
import { QuestionDetail, QuestionSummary } from '../../api/types';

vi.mock('../../api/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('../../components/MathText', () => ({
  MathText: ({ content }: { content: string }) => <div>{content}</div>,
}));

const mockQuestion: QuestionDetail = {
  id: 'q101',
  externalId: 'GATE-CS-2024-Q15',
  subjectId: 'subj_dbms',
  subjectName: 'Databases',
  topicId: 'top_indexing',
  topicName: 'B+ Trees',
  year: 2024,
  type: 'MCQ',
  marks: 2,
  statement: 'Calculate B+ tree maximum depth',
  optionsJson: JSON.stringify([{ label: 'A', text: 'Depth 3' }, { label: 'B', text: 'Depth 4' }]),
  correctOptions: 'A',
  numericMin: null,
  numericMax: null,
  explanation: 'Formula gives 3.',
  gateOverflowUrl: null,
  isBookmarked: false,
  pass1Status: 'SOLVED',
  pass2Status: 'SOLVED',
  version: 1,
};

describe('bookmarkFlow', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, staleTime: Infinity },
        mutations: { retry: false },
      },
    });

    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === '/questions/q101') {
        return Promise.resolve(mockQuestion) as any;
      }
      if (url.includes('/subjects')) return Promise.resolve([]) as any;
      if (url.includes('/topics')) return Promise.resolve([]) as any;
      if (url.startsWith('/questions')) {
        return Promise.resolve({
          content: [mockQuestion],
          totalElements: 1,
          totalPages: 1,
          size: 20,
          number: 0,
        }) as any;
      }
      return Promise.resolve({}) as any;
    });
  });

  it('1 Toggle fires PATCH /questions/:id/bookmark with body', async () => {
    vi.mocked(apiClient.patch).mockResolvedValueOnce({
      ...mockQuestion,
      isBookmarked: true,
    });

    queryClient.setQueryData(['question', 'q101'], mockQuestion);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/questions/q101']}>
          <Routes>
            <Route path="/questions/:id" element={<QuestionDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('detail-bookmark-btn')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('detail-bookmark-btn'));

    await waitFor(() => {
      expect(apiClient.patch).toHaveBeenCalledWith(
        '/questions/q101/bookmark',
        { bookmarked: true }
      );
    });
  });

  it('2 Detail and card states display active bookmark indicator', async () => {
    const bookmarkedQuestion: QuestionDetail = {
      ...mockQuestion,
      isBookmarked: true,
    };
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === '/questions/q101') return Promise.resolve(bookmarkedQuestion) as any;
      return Promise.resolve([]) as any;
    });
    queryClient.setQueryData(['question', 'q101'], bookmarkedQuestion);

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/questions/q101']}>
          <Routes>
            <Route path="/questions/:id" element={<QuestionDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('detail-bookmark-btn')).toBeInTheDocument();
    });

    expect(screen.getByTestId('detail-bookmark-btn')).toHaveTextContent('Bookmarked');
  });

  it('3 Filter chip navigation toggles bookmarked query parameter', async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url.includes('/subjects')) return Promise.resolve([]) as any;
      if (url.includes('/topics')) return Promise.resolve([]) as any;
      if (url.includes('/questions')) {
        return Promise.resolve({
          content: [],
          totalElements: 0,
          totalPages: 1,
          size: 20,
          number: 0,
        }) as any;
      }
      return Promise.resolve({}) as any;
    });

    let currentLocation: any;
    const LocationWatcher = () => {
      currentLocation = useLocation();
      return null;
    };

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/questions']}>
          <LocationWatcher />
          <QuestionsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Bookmarked/i })).toBeInTheDocument();
    });

    const bookmarkFilterBtn = screen.getByRole('button', { name: /Bookmarked/i });
    fireEvent.click(bookmarkFilterBtn);

    await waitFor(() => {
      expect(currentLocation.search).toContain('bookmarked=true');
    });
  });

  it('4 Optimistic rollback restores state when bookmark mutation fails', async () => {
    // Initial state: not bookmarked
    queryClient.setQueryData(['question', 'q101'], {
      ...mockQuestion,
      isBookmarked: false,
    });

    // Mock failure on patch
    vi.mocked(apiClient.patch).mockRejectedValueOnce(new Error('Server error'));

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/questions/q101']}>
          <Routes>
            <Route path="/questions/:id" element={<QuestionDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('detail-bookmark-btn')).toBeInTheDocument();
    });

    expect(screen.getByTestId('detail-bookmark-btn')).toHaveTextContent('Bookmark');

    // Click to toggle
    fireEvent.click(screen.getByTestId('detail-bookmark-btn'));

    // After mutation failure, query cache should be rolled back to false
    await waitFor(() => {
      const cached = queryClient.getQueryData<QuestionDetail>(['question', 'q101']);
      expect(cached?.isBookmarked).toBe(false);
    });

    expect(screen.getByTestId('detail-bookmark-btn')).toHaveTextContent('Bookmark');
  });
});
