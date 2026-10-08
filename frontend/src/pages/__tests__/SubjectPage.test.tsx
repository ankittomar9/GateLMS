import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SubjectPage } from '../SubjectPage';
import { journeyApi } from '../../api/journeyApi';
import { JourneyTopicSummary } from '../../types/journey';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ subjectId: 'sub_os' }),
  };
});

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getSubjectTopics: vi.fn(),
  },
}));

const mockTopics: JourneyTopicSummary[] = [
  {
    topicId: 'topic_proc',
    name: 'Processes & Threads',
    sequence: 1,
    primerStatus: 'ACCEPTED',
    masteryLevel: 80,
    questionsVisible: 25,
    questionsSolved: 25,
    completed: true,
  },
  {
    topicId: 'topic_cpu',
    name: 'CPU Scheduling Algorithms',
    sequence: 2,
    primerStatus: null,
    masteryLevel: 40,
    questionsVisible: 30,
    questionsSolved: 10,
    completed: false,
  },
  {
    topicId: 'topic_vm',
    name: 'Virtual Memory & Paging',
    sequence: 3,
    primerStatus: 'DRAFT',
    masteryLevel: 10,
    questionsVisible: 35,
    questionsSolved: 0,
    completed: false,
  },
];

describe('SubjectPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    vi.mocked(journeyApi.getSubjectTopics).mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <SubjectPage />
      </MemoryRouter>
    );

    expect(screen.getByTestId('subject-page-loading')).toBeInTheDocument();
  });

  it('renders ordered roadmap topics, completion ticks, and counters (PC-1, PC-2)', async () => {
    vi.mocked(journeyApi.getSubjectTopics).mockResolvedValue(mockTopics);

    render(
      <MemoryRouter>
        <SubjectPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-page')).toBeInTheDocument();
    });

    expect(screen.getByText('Processes & Threads')).toBeInTheDocument();
    expect(screen.getByText('CPU Scheduling Algorithms')).toBeInTheDocument();
    expect(screen.getByText('Virtual Memory & Paging')).toBeInTheDocument();

    // Check sequence numbers
    expect(screen.getByText('#1')).toBeInTheDocument();
    expect(screen.getByText('#2')).toBeInTheDocument();
    expect(screen.getByText('#3')).toBeInTheDocument();

    // Check completion badge on topic 1
    expect(screen.getByText('Done')).toBeInTheDocument();
    expect(screen.getByText('1')).toBeInTheDocument(); // 1 Completed
  });

  it('navigates to topic details when topic row is clicked (PC-1 zero locks)', async () => {
    vi.mocked(journeyApi.getSubjectTopics).mockResolvedValue(mockTopics);

    render(
      <MemoryRouter>
        <SubjectPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-page')).toBeInTheDocument();
    });

    const topicRow = screen.getByTestId('topic-row-topic_cpu');
    fireEvent.click(topicRow);

    expect(mockNavigate).toHaveBeenCalledWith('/topics/topic_cpu');
  });

  it('navigates to subject solved notebook', async () => {
    vi.mocked(journeyApi.getSubjectTopics).mockResolvedValue(mockTopics);

    render(
      <MemoryRouter>
        <SubjectPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-page')).toBeInTheDocument();
    });

    const notebookBtn = screen.getByTestId('subject-notebook-btn');
    fireEvent.click(notebookBtn);

    expect(mockNavigate).toHaveBeenCalledWith('/subjects/sub_os/notebook');
  });
});
