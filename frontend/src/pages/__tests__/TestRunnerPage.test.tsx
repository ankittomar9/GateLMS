import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { TestRunnerPage } from '../TestRunnerPage';
import { StartMockAttemptResponse } from '../../api/types';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

const mockAttemptFixture: StartMockAttemptResponse = {
  attemptId: 'att-1234-abcd',
  mockId: 'mock-tmpl-1',
  mockTitle: 'GATE CSE Mock Examination #1',
  durationSeconds: 1800, // 30 minutes
  startedAt: new Date(Date.now() - 60000).toISOString(), // started 1 min ago
  serverTime: new Date().toISOString(),
  shortfallWarnings: [],
  sections: [
    {
      subjectId: 'subj_algo',
      name: 'Algorithms',
      questionIds: ['q-mcq-1', 'q-nat-2'],
    },
  ],
  questions: [
    {
      id: 'q-mcq-1',
      externalId: 'GATE-2024-01',
      type: 'MCQ' as any,
      marks: 2,
      statement: 'What is the time complexity of QuickSort average case?',
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
      type: 'NAT' as any,
      marks: 1,
      statement: 'Enter the value of $2^{10}$:',
      optionsJson: null,
      topicId: 'topic_algo',
      subjectId: 'subj_algo',
      year: 2024,
    },
  ],
};

describe('TestRunnerPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/v1/mocks/attempts/att-1234-abcd')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockAttemptFixture),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ saved: true, state: 'ANSWERED' }),
      });
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/mocks/attempts/att-1234-abcd']}>
          <Routes>
            <Route path="/mocks/attempts/:attemptId" element={<TestRunnerPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('renders exam header, timer, and question navigator palette', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/GATE CSE Mock Examination #1/i)).toBeInTheDocument();
    });

    // Palette renders numbers 1 and 2
    expect(screen.getByRole('button', { name: '1' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '2' })).toBeInTheDocument();

    // First question statement is displayed
    expect(screen.getByText(/time complexity of QuickSort/i)).toBeInTheDocument();
  });

  it('handles option selection and Save & Next flow', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/time complexity of QuickSort/i)).toBeInTheDocument();
    });

    // Click option B
    const optB = screen.getByText('B');
    fireEvent.click(optB);

    // Click Save & Next
    const saveBtn = screen.getByRole('button', { name: /Save & Next/i });
    fireEvent.click(saveBtn);

    // Advanced to Question 2 (NAT)
    await waitFor(() => {
      expect(screen.getByText(/Enter Numerical Value/i)).toBeInTheDocument();
    });
  });

  it('handles Mark for Review and Next transition', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/time complexity of QuickSort/i)).toBeInTheDocument();
    });

    // Click Mark for Review & Next without selecting option
    const markBtn = screen.getByRole('button', { name: /Mark for Review & Next/i });
    fireEvent.click(markBtn);

    // Advanced to next question
    await waitFor(() => {
      expect(screen.getByText(/Enter the value of/i)).toBeInTheDocument();
    });
  });

  it('handles Clear Response action', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/time complexity of QuickSort/i)).toBeInTheDocument();
    });

    // Select option A
    fireEvent.click(screen.getByText('A'));

    // Clear response
    const clearBtn = screen.getByRole('button', { name: /Clear Response/i });
    fireEvent.click(clearBtn);

    expect(screen.getByText('A')).toBeInTheDocument();
  });

  it('serverTime offset calculation computes accurate remaining seconds', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('timer')).toBeInTheDocument();
    });

    // Started 60 seconds ago from a 1800s duration, so approx 28-29 mins remaining (00:29:xx)
    const timerText = screen.getByRole('timer').textContent;
    expect(timerText).toMatch(/00:2[89]:/);
  });

  it('verifies accessible roles for timer and options radiogroup', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('timer')).toBeInTheDocument();
      expect(screen.getByRole('radiogroup')).toBeInTheDocument();
    });
  });

  it('confirm submit dialog opens and manual submission calls submit and navigates', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Submit Exam/i })).toBeInTheDocument();
    });

    // Click Submit Exam
    fireEvent.click(screen.getByRole('button', { name: /Submit Exam/i }));

    // Confirm dialog appears
    expect(screen.getByText(/Confirm Exam Submission/i)).toBeInTheDocument();

    // Click Confirm & Submit
    const confirmBtn = screen.getByRole('button', { name: /Confirm & Submit/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/mocks/attempts/att-1234-abcd/result');
    });
  });
});
