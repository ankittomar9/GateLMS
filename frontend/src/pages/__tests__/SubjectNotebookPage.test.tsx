import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SubjectNotebookPage } from '../SubjectNotebookPage';
import { journeyApi } from '../../api/journeyApi';
import { anatomyApi, QuestionAnatomyDto } from '../../api/anatomyApi';
import { SubjectNotebookResponse } from '../../types/journey';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ subjectId: 'subj_dbms' }),
  };
});

vi.mock('../../api/journeyApi', () => ({
  journeyApi: {
    getSubjectNotebook: vi.fn(),
  },
}));

vi.mock('../../api/anatomyApi', () => ({
  anatomyApi: {
    getQuestionAnatomy: vi.fn(),
  },
}));

// Mock MathText
vi.mock('../../components/MathText', () => ({
  MathText: ({ content }: { content: string }) => <span>{content}</span>,
}));

const mockNotebookResponse: SubjectNotebookResponse = {
  subjectId: 'subj_dbms',
  subjectName: 'Databases (DBMS)',
  totalEntries: 2,
  topics: [
    {
      topicId: 'topic_ra',
      topicName: 'Relational Algebra',
      sequence: 1,
      entries: [
        {
          questionId: 'q-nb-1',
          year: 2023,
          source: 'SAMPLE',
          statementSnippet: 'Find the minimum projection attributes for relation R.',
          solvedAt: new Date().toISOString(),
          starred: true,
          anatomyAvailable: true,
        },
      ],
    },
    {
      topicId: 'topic_norm',
      topicName: 'Normalization & Dependencies',
      sequence: 2,
      entries: [
        {
          questionId: 'q-nb-2',
          year: 2022,
          source: 'SAMPLE',
          statementSnippet: 'Check whether functional dependency set F is minimal.',
          solvedAt: new Date().toISOString(),
          starred: false,
          anatomyAvailable: false,
        },
      ],
    },
  ],
};

const mockAnatomyDossier: QuestionAnatomyDto = {
  id: 'anat-1',
  questionId: 'q-nb-1',
  testedConcept: 'Lossless Decomposition Invariant',
  invariantRule: 'R1 ∩ R2 -> R1 or R1 ∩ R2 -> R2',
  trapAnalysis: [
    {
      option: 'A',
      verdict: 'INCORRECT',
      why_plausible: 'Appears intuitive',
      misconception: 'Assumed dependency preserving implies lossless decomposition.',
    },
  ],
  prerequisites: [],
  similarPatternHint: 'Check functional dependency closures step-by-step.',
  status: 'ACCEPTED',
  parseError: null,
  generatorModel: 'gemini-1.5-pro',
  generatedAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

describe('SubjectNotebookPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading state initially', () => {
    vi.mocked(journeyApi.getSubjectNotebook).mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <SubjectNotebookPage />
      </MemoryRouter>
    );

    expect(screen.getByTestId('notebook-loading')).toBeInTheDocument();
  });

  it('renders topic groups, sequence numbers, and solved entries (PC-6)', async () => {
    vi.mocked(journeyApi.getSubjectNotebook).mockResolvedValue(mockNotebookResponse);

    render(
      <MemoryRouter>
        <SubjectNotebookPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-notebook-page')).toBeInTheDocument();
    });

    expect(screen.getByText('Databases (DBMS) Solved Notebook')).toBeInTheDocument();
    expect(screen.getByText('Relational Algebra')).toBeInTheDocument();
    expect(screen.getByText('Normalization & Dependencies')).toBeInTheDocument();

    expect(screen.getByText(/Find the minimum projection attributes/)).toBeInTheDocument();
    expect(screen.getByText(/Check whether functional dependency set F is minimal/)).toBeInTheDocument();
  });

  it('toggles starred only filter and fetches filtered notebook', async () => {
    vi.mocked(journeyApi.getSubjectNotebook).mockResolvedValue(mockNotebookResponse);

    render(
      <MemoryRouter>
        <SubjectNotebookPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-notebook-page')).toBeInTheDocument();
    });

    const starredBtn = screen.getByTestId('filter-starred-btn');
    fireEvent.click(starredBtn);

    await waitFor(() => {
      expect(journeyApi.getSubjectNotebook).toHaveBeenCalledWith('subj_dbms', true);
    });
  });

  it('expands Anatomy Dossier and displays tested concept and invariant (PC-6 zero new AI)', async () => {
    vi.mocked(journeyApi.getSubjectNotebook).mockResolvedValue(mockNotebookResponse);
    vi.mocked(anatomyApi.getQuestionAnatomy).mockResolvedValue(mockAnatomyDossier);

    render(
      <MemoryRouter>
        <SubjectNotebookPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-notebook-page')).toBeInTheDocument();
    });

    const toggleBtn = screen.getByTestId('btn-toggle-anatomy-q-nb-1');
    fireEvent.click(toggleBtn);

    await waitFor(() => {
      expect(anatomyApi.getQuestionAnatomy).toHaveBeenCalledWith('q-nb-1');
      expect(screen.getByText('Lossless Decomposition Invariant')).toBeInTheDocument();
      expect(screen.getByText('R1 ∩ R2 -> R1 or R1 ∩ R2 -> R2')).toBeInTheDocument();
    });
  });

  it('shows fallback message when anatomy dossier is absent', async () => {
    vi.mocked(journeyApi.getSubjectNotebook).mockResolvedValue(mockNotebookResponse);
    vi.mocked(anatomyApi.getQuestionAnatomy).mockRejectedValue(new Error('Dossier not found'));

    render(
      <MemoryRouter>
        <SubjectNotebookPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('subject-notebook-page')).toBeInTheDocument();
    });

    const toggleBtn = screen.getByTestId('btn-toggle-anatomy-q-nb-2');
    fireEvent.click(toggleBtn);

    await waitFor(() => {
      expect(screen.getByText(/Anatomy dossier not yet generated for this question/)).toBeInTheDocument();
    });
  });

  it('renders empty state when topics list is empty', async () => {
    vi.mocked(journeyApi.getSubjectNotebook).mockResolvedValue({
      subjectId: 'subj_dbms',
      subjectName: 'Databases (DBMS)',
      totalEntries: 0,
      topics: [],
    });

    render(
      <MemoryRouter>
        <SubjectNotebookPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('notebook-empty-state')).toBeInTheDocument();
    });

    expect(screen.getByText('No Solved Questions Recorded Yet')).toBeInTheDocument();
  });
});
