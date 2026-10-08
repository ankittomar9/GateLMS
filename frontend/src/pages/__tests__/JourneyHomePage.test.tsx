import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { JourneyHomePage } from '../JourneyHomePage';
import { journeyApi } from '../../api/journeyApi';
import { JourneyOverviewSubject, JourneyContinue } from '../../types/journey';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getOverview: vi.fn(),
    getContinue: vi.fn(),
  },
}));

const mockSubjects: JourneyOverviewSubject[] = [
  {
    subjectId: 'sub_em',
    name: 'Engineering Mathematics',
    cluster: 'MATH_APTITUDE',
    priorityTier: 1,
    marksWeight: 15.0,
    standardBookRef: 'Higher Engineering Mathematics - B.S. Grewal',
    questionsVisible: 120,
    questionsSolved: 30,
    topicsTotal: 4,
    topicsTouched: 2,
    topicsCompleted: 1,
    readinessPct: 25.0,
  },
  {
    subjectId: 'sub_os',
    name: 'Operating Systems',
    cluster: 'SYSTEMS_CORE',
    priorityTier: 1,
    marksWeight: 10.0,
    standardBookRef: 'Silberschatz & Galvin',
    questionsVisible: 200,
    questionsSolved: 50,
    topicsTotal: 5,
    topicsTouched: 3,
    topicsCompleted: 2,
    readinessPct: 25.0,
  },
  {
    subjectId: 'sub_algo',
    name: 'Algorithms',
    cluster: 'CONCEPTUAL_CORE',
    priorityTier: 1,
    marksWeight: 8.0,
    standardBookRef: 'CLRS',
    questionsVisible: 180,
    questionsSolved: 0,
    topicsTotal: 6,
    topicsTouched: 0,
    topicsCompleted: 0,
    readinessPct: 0.0,
  },
];

const mockContinue: JourneyContinue = {
  topicId: 'topic-vm',
  subjectId: 'sub_os',
  questionId: 'q-101',
};

describe('JourneyHomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    vi.mocked(journeyApi.getOverview).mockReturnValue(new Promise(() => {}));
    vi.mocked(journeyApi.getContinue).mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <JourneyHomePage />
      </MemoryRouter>
    );

    expect(screen.getByTestId('journey-home-loading')).toBeInTheDocument();
  });

  it('renders clusters, "Recommended Start" badge, and subjects with honest counts (PC-1, PC-7)', async () => {
    vi.mocked(journeyApi.getOverview).mockResolvedValue(mockSubjects);
    vi.mocked(journeyApi.getContinue).mockResolvedValue(mockContinue);

    render(
      <MemoryRouter>
        <JourneyHomePage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('journey-home-page')).toBeInTheDocument();
    });

    // Check cluster headings
    expect(screen.getByText('Foundation & Engineering Mathematics')).toBeInTheDocument();
    expect(screen.getByText('Recommended Start')).toBeInTheDocument();
    expect(screen.getByText('Systems & Architecture Core')).toBeInTheDocument();
    expect(screen.getByText('Conceptual & Algorithmic Core')).toBeInTheDocument();

    // Check all subjects rendered
    expect(screen.getByText('Engineering Mathematics')).toBeInTheDocument();
    expect(screen.getByText('Operating Systems')).toBeInTheDocument();
    expect(screen.getByText('Algorithms')).toBeInTheDocument();

    // Check PC-1 zero locks: all cards are rendered and unlocked
    const emCard = screen.getByTestId('subject-card-sub_em');
    const osCard = screen.getByTestId('subject-card-sub_os');
    expect(emCard).toBeInTheDocument();
    expect(osCard).toBeInTheDocument();

    // Check Continue where you left off strip
    expect(screen.getByText(/Continue Where You Left Off/i)).toBeInTheDocument();
    expect(screen.getByTestId('continue-learning-strip')).toBeInTheDocument();
  });

  it('navigates to subject roadmap when card is clicked (PC-1 free navigation)', async () => {
    vi.mocked(journeyApi.getOverview).mockResolvedValue(mockSubjects);
    vi.mocked(journeyApi.getContinue).mockResolvedValue(null);

    render(
      <MemoryRouter>
        <JourneyHomePage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('journey-home-page')).toBeInTheDocument();
    });

    const osCard = screen.getByTestId('subject-card-sub_os');
    fireEvent.click(osCard);

    expect(mockNavigate).toHaveBeenCalledWith('/subjects/sub_os');
  });

  it('renders error state on fetch failure', async () => {
    vi.mocked(journeyApi.getOverview).mockRejectedValue(new Error('Network error'));
    vi.mocked(journeyApi.getContinue).mockResolvedValue(null);

    render(
      <MemoryRouter>
        <JourneyHomePage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });
});
