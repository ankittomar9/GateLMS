import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { MockTestsPage } from '../MockTestsPage';
import { MockTemplateSummaryDto, MockTrendDto } from '../../api/types';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockTemplatesFixture: MockTemplateSummaryDto[] = [
  {
    id: 'tmpl-1',
    title: 'Full Length GATE Mock #1',
    durationMinutes: 180,
    totalMarks: 100,
    totalQuestions: 65,
    mode: 'MOCK',
    createdAt: new Date().toISOString(),
    sections: [
      { subjectId: 'subj_aptitude', mcq: 5, msq: 2, nat: 3, marks: 1 },
      { subjectId: 'subj_algo', mcq: 6, msq: 2, nat: 2, marks: 2 },
    ],
    bestScore: 68.5,
    attemptCount: 3,
  },
];

const mockTrendFixture: MockTrendDto[] = [
  {
    attemptId: 'att-1',
    mockId: 'tmpl-1',
    mockTitle: 'Full Length GATE Mock #1',
    date: new Date().toISOString(),
    totalScore: 55.0,
    maxScore: 100.0,
    percentage: 55.0,
    percentileLike: null,
  },
  {
    attemptId: 'att-2',
    mockId: 'tmpl-1',
    mockTitle: 'Full Length GATE Mock #1',
    date: new Date().toISOString(),
    totalScore: 68.5,
    maxScore: 100.0,
    percentage: 68.5,
    percentileLike: null,
  },
];

vi.mock('../../hooks/useQueries', () => ({
  useMockTemplates: () => ({
    data: mockTemplatesFixture,
    isLoading: false,
    error: null,
  }),
  useMockTrend: () => ({
    data: mockTrendFixture,
    isLoading: false,
  }),
  useSubjects: () => ({
    data: [
      { id: 'subj_algo', name: 'Algorithms' },
      { id: 'subj_aptitude', name: 'General Aptitude' },
    ],
  }),
  useCreateMockTemplate: () => ({
    mutateAsync: vi.fn(),
    isPending: false,
  }),
  useStartMockAttempt: () => ({
    mutateAsync: vi.fn().mockResolvedValue({ attemptId: 'new-attempt-uuid' }),
    isPending: false,
  }),
}));

describe('MockTestsPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient();

    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/attempts')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ attemptId: 'new-attempt-uuid' }),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <MockTestsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('renders page header and list of mock templates', () => {
    renderComponent();

    expect(screen.getByText(/GATE Mock Tests/i)).toBeInTheDocument();
    expect(screen.getAllByText('Full Length GATE Mock #1').length).toBeGreaterThan(0);
    expect(screen.getByText(/65 Questions/i)).toBeInTheDocument();
    expect(screen.getByText(/100 Marks/i)).toBeInTheDocument();
    expect(screen.getByText(/68.50 marks/i)).toBeInTheDocument();
  });

  it('renders score trend banner with completed attempt records', () => {
    renderComponent();

    expect(screen.getByText(/Score Trend Across Mock Attempts/i)).toBeInTheDocument();
    expect(screen.getByText(/2 completed attempts/i)).toBeInTheDocument();
    expect(screen.getByText('55.0%')).toBeInTheDocument();
    expect(screen.getByText('68.5%')).toBeInTheDocument();
  });

  it('opens template builder and computes live question and mark totals', () => {
    renderComponent();

    const openBtn = screen.getByRole('button', { name: /Create Mock Template/i });
    fireEvent.click(openBtn);

    expect(screen.getByText(/Build Mock Test Blueprint/i)).toBeInTheDocument();

    // Default sections: 10Q (1-mark) + 10Q (2-mark) = 20Q, 30 Marks
    expect(screen.getByText('20')).toBeInTheDocument();
    expect(screen.getByText('30')).toBeInTheDocument();
  });

  it('clicking Start Attempt triggers assembly and navigates to runner', async () => {
    renderComponent();

    const startBtn = screen.getByRole('button', { name: /Start Attempt/i });
    fireEvent.click(startBtn);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/mocks/attempts/new-attempt-uuid');
    });
  });
});
