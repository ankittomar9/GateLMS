import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PYPPage } from '../PYPPage';
import { PracticePage } from '../PracticePage';
import { QuestionsPage } from '../QuestionsPage';
import { PypDashboardTile } from '../../components/dashboard/PypDashboardTile';
import { pypApi } from '../../api/pypApi';
import { apiClient } from '../../api/apiClient';
import { PypStatsResponse, QuestionSummary, PageResponse } from '../../api/types';

vi.mock('../../api/pypApi', () => ({
  pypApi: {
    getPypStats: vi.fn(),
    generatePypMock: vi.fn(),
  },
}));

vi.mock('../../api/apiClient', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockPypStats: PypStatsResponse = {
  years: [
    {
      year: 2024,
      total: 65,
      bySubject: {
        'general-aptitude': 10,
        'operating-systems': 8,
        'computer-networks': 7,
      },
    },
    {
      year: 2023,
      total: 65,
      bySubject: {
        'general-aptitude': 10,
        'algorithms': 9,
      },
    },
    {
      year: 2020,
      total: 45,
      bySubject: {
        'general-aptitude': 10,
        'theory-of-computation': 6,
      },
    },
  ],
  coverage: {
    fullPaperYears: [2024, 2023],
    partialYears: [2020],
  },
};

function renderWithProviders(ui: React.ReactElement, initialRoute = '/') {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[initialRoute]}>
        {ui}
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('PYPPage & GATE PYP Suite', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. renders year grid from fixture ordered newest-first', async () => {
    vi.mocked(pypApi.getPypStats).mockResolvedValue(mockPypStats);

    renderWithProviders(<PYPPage />, '/pyp');

    await waitFor(() => {
      expect(screen.getByTestId('pyp-year-card-2024')).toBeInTheDocument();
    });

    const yearCards = screen.getAllByTestId(/^pyp-year-card-/);
    expect(yearCards).toHaveLength(3);
    expect(yearCards[0]).toHaveAttribute('data-testid', 'pyp-year-card-2024');
    expect(yearCards[1]).toHaveAttribute('data-testid', 'pyp-year-card-2023');
    expect(yearCards[2]).toHaveAttribute('data-testid', 'pyp-year-card-2020');

    expect(screen.getByText('GATE 2024')).toBeInTheDocument();
    expect(screen.getByText('GATE 2020')).toBeInTheDocument();
  });

  it('2. displays shortfall badge for partial paper years and full paper badge for 65-question years', async () => {
    vi.mocked(pypApi.getPypStats).mockResolvedValue(mockPypStats);

    renderWithProviders(<PYPPage />, '/pyp');

    await waitFor(() => {
      expect(screen.getByTestId('pyp-year-card-2024')).toBeInTheDocument();
    });

    const fullBadges = screen.getAllByTestId('full-paper-badge');
    expect(fullBadges.length).toBeGreaterThanOrEqual(2);

    const shortfallBadges = screen.getAllByTestId('shortfall-badge');
    expect(shortfallBadges.length).toBeGreaterThanOrEqual(1);
    expect(shortfallBadges[0]).toHaveTextContent('Partial Paper');
  });

  it('3. clicking "Attempt Paper" triggers generatePypMock mutation', async () => {
    vi.mocked(pypApi.getPypStats).mockResolvedValue(mockPypStats);
    vi.mocked(pypApi.generatePypMock).mockResolvedValue({
      id: 'mock-tpl-2024',
      title: 'GATE CSE 2024 - Official Full Paper',
      durationMinutes: 180,
      totalMarks: 100,
      totalQuestions: 65,
      mode: 'STANDARD',
      attemptCount: 0,
      createdAt: '2026-10-07T00:00:00Z',
      sections: [],
      shortfallWarnings: [],
    });

    // Mock fetch for attempt start
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ attemptId: 'att-2024' }),
    } as any);

    renderWithProviders(<PYPPage />, '/pyp');

    await waitFor(() => {
      expect(screen.getByTestId('attempt-paper-btn-2024')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('attempt-paper-btn-2024'));

    await waitFor(() => {
      expect(pypApi.generatePypMock).toHaveBeenCalledWith(2024);
    });
  });

  it('4. renders PYP badge on question card when source is GATE_PYP', async () => {
    const mockQuestionsPage: PageResponse<QuestionSummary> = {
      content: [
        {
          id: 'q-pyp-1',
          externalId: 'PYP-2024-OS-01',
          subjectId: 'subj-os',
          subjectName: 'Operating Systems',
          topicId: 'top-sync',
          topicName: 'Synchronization',
          year: 2024,
          type: 'MCQ',
          marks: 2,
          statementSnippet: 'What is the Peterson solution bound?',
          gateOverflowUrl: 'https://gateoverflow.in/12345',
          isBookmarked: false,
          pass1Status: 'UNSEEN',
          pass2Status: 'NOT_ATTEMPTED',
          source: 'GATE_PYP',
          version: 1,
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      page: 0,
    };

    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url.includes('/questions')) {
        return Promise.resolve(mockQuestionsPage as any);
      }
      if (url.includes('/subjects')) {
        return Promise.resolve([] as any);
      }
      return Promise.resolve({} as any);
    });

    renderWithProviders(<QuestionsPage />, '/questions');

    await waitFor(() => {
      expect(screen.getByTestId('pyp-badge')).toBeInTheDocument();
    });

    expect(screen.getByTestId('pyp-badge')).toHaveTextContent('PYP 2024');
    expect(screen.getByTestId('year-quick-filters')).toBeInTheDocument();
  });

  it('5. practice config initializes year select from URL search parameter and submits year', async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url.includes('/subjects')) {
        return Promise.resolve([
          { id: 'subj-os', name: 'Operating Systems', slug: 'operating-systems', code: 'CS-OS', weightageAvg: 8, priorityTier: 'TIER_1', primaryBookRef: 'Silberschatz', topicCount: 5 },
        ] as any);
      }
      if (url.includes('/topics')) {
        return Promise.resolve([] as any);
      }
      if (url.includes('/stats/mastery')) {
        return Promise.resolve([] as any);
      }
      if (url.includes('/mistakes')) {
        return Promise.resolve({ totalElements: 0 } as any);
      }
      return Promise.resolve({} as any);
    });

    vi.mocked(apiClient.post).mockResolvedValue({
      sessionId: 'sess-pyp',
      attemptId: 'att-pyp',
      questions: [],
      totalQuestions: 10,
    } as any);

    renderWithProviders(<PracticePage />, '/practice?year=2023&subject=operating-systems');

    await waitFor(() => {
      const yearSelect = screen.getByTestId('year-select') as HTMLSelectElement;
      expect(yearSelect).toBeInTheDocument();
      expect(yearSelect.value).toBe('2023');
    });

    const startButton = screen.getByRole('button', { name: /Start Practice Session/i });
    fireEvent.click(startButton);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(
        '/practice/sessions',
        expect.objectContaining({
          year: 2023,
        })
      );
    });
  });

  it('6. PypDashboardTile renders latest PYP year card and triggers attempt CTA', async () => {
    vi.mocked(pypApi.getPypStats).mockResolvedValue(mockPypStats);
    vi.mocked(pypApi.generatePypMock).mockResolvedValue({
      id: 'mock-latest',
      title: 'GATE CSE 2024 - Full Paper',
      durationMinutes: 180,
      totalMarks: 100,
      totalQuestions: 65,
      mode: 'STANDARD',
      attemptCount: 0,
      createdAt: '2026-10-07T00:00:00Z',
      sections: [],
    });
    vi.mocked(apiClient.post).mockResolvedValue({
      attemptId: 'att-latest',
    } as any);

    renderWithProviders(<PypDashboardTile />, '/');

    await waitFor(() => {
      expect(screen.getByTestId('pyp-dashboard-tile')).toBeInTheDocument();
      expect(screen.getByText('GATE 2024 CSE')).toBeInTheDocument();
    });

    const attemptBtn = screen.getByTestId('attempt-latest-pyp-btn');
    expect(attemptBtn).toBeInTheDocument();
    fireEvent.click(attemptBtn);

    await waitFor(() => {
      expect(pypApi.generatePypMock).toHaveBeenCalledWith(2024);
    });
  });
});
