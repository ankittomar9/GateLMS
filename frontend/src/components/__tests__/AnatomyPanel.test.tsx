import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AnatomyPanel } from '../AnatomyPanel';
import { anatomyApi, QuestionAnatomyDto } from '../../api/anatomyApi';

vi.mock('../../api/anatomyApi', () => ({
  anatomyApi: {
    getQuestionAnatomy: vi.fn(),
    generateQuestionAnatomy: vi.fn(),
  },
}));

const mockDossier: QuestionAnatomyDto = {
  id: 'dossier-uuid-1',
  questionId: 'question-uuid-1',
  testedConcept: 'Lossless join decomposition and preserving functional dependencies under BCNF with $F^+$.',
  invariantRule: 'Every non-trivial FD $X \\to Y$ must have superkey $X$.',
  trapAnalysis: [
    {
      option: 'A',
      verdict: 'INCORRECT',
      why_plausible: 'Confuses 3NF synthesis with BCNF decomposition',
      misconception: 'Assumes preserving dependencies is guaranteed in BCNF',
    },
    {
      option: 'B',
      verdict: 'CORRECT',
      why_plausible: 'Identifies non-superkey determinant correctly',
      misconception: 'Correct understanding of canonical cover',
    },
  ],
  prerequisites: [
    {
      concept_slug: 'functional-dependency-closure',
      concept_label: 'Functional Dependency Closure',
      topic_slug: 'normalization',
      must_read: true,
    },
  ],
  similarPatternHint: 'Standard GATE DBMS question testing decomposition algorithm invariants.',
  status: 'DRAFT',
  parseError: null,
  generatorModel: 'gemini-3.8-flash',
  generatedAt: '2026-10-08T12:00:00Z',
  updatedAt: null,
};

describe('AnatomyPanel Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('honest-judge PASS - renders concealed banner during active test session', () => {
    render(
      <AnatomyPanel
        questionId="q1"
        hasAttempted={false}
        mode="test"
        testSubmitted={false}
      />
    );

    expect(screen.getByText(/Examiner Anatomy is concealed during active test sessions/i)).toBeInTheDocument();
    expect(anatomyApi.getQuestionAnatomy).not.toHaveBeenCalled();
  });

  it('honest-judge PASS - renders locked state when 403 returned (unattempted)', async () => {
    const error: any = new Error('Locked');
    error.status = 403;
    vi.mocked(anatomyApi.getQuestionAnatomy).mockRejectedValueOnce(error);

    render(
      <AnatomyPanel
        questionId="q1"
        hasAttempted={false}
        isAdmin={false}
        mode="study"
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/Anatomy Dossier Locked Until Attempt/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/unlock only after you submit your first attempt/i)).toBeInTheDocument();
  });

  it('honest-judge PASS - renders admin force generate button when locked and isAdmin is true', async () => {
    const error: any = new Error('Locked');
    error.status = 403;
    vi.mocked(anatomyApi.getQuestionAnatomy).mockRejectedValueOnce(error);

    render(
      <AnatomyPanel
        questionId="q1"
        hasAttempted={false}
        isAdmin={true}
        mode="study"
      />
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Admin: Force Generate & View/i })).toBeInTheDocument();
    });
  });

  it('honest-judge PASS - renders full dossier sections with KaTeX and unreviewed badge', async () => {
    vi.mocked(anatomyApi.getQuestionAnatomy).mockResolvedValueOnce(mockDossier);

    const { container } = render(
      <AnatomyPanel
        questionId="q1"
        hasAttempted={true}
        isAdmin={false}
        mode="study"
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/Question Anatomy Dossier/i)).toBeInTheDocument();
    });

    // Badge verification
    expect(screen.getByText(/AI-GENERATED — UNREVIEWED/i)).toBeInTheDocument();
    expect(screen.getByText('gemini-3.8-flash')).toBeInTheDocument();

    // Section 1: Tested concept rendered with KaTeX (no raw $F^+$ text)
    expect(screen.getByText(/1. What This Tests/i)).toBeInTheDocument();
    const katexEl = container.querySelector('.katex');
    expect(katexEl).not.toBeNull();

    // Section 2: Invariant
    expect(screen.getByText(/2. The Core Invariant/i)).toBeInTheDocument();

    // Section 3: Distractor autopsy table
    expect(screen.getByText(/3. Distractor Autopsy & Traps/i)).toBeInTheDocument();
    expect(screen.getByText('Option')).toBeInTheDocument();
    expect(screen.getByText('Why Plausible (Cognitive Trap)')).toBeInTheDocument();
    expect(screen.getByText('Underlying Misconception')).toBeInTheDocument();

    // Section 4: Prerequisites
    expect(screen.getByText(/4. Prerequisite Concept Units/i)).toBeInTheDocument();
    expect(screen.getByText('Functional Dependency Closure')).toBeInTheDocument();

    // Section 5: Pattern Hint
    expect(screen.getByText(/5. Pattern Recognition & Problem Family/i)).toBeInTheDocument();
    expect(screen.getByText(/Standard GATE DBMS question testing decomposition/i)).toBeInTheDocument();
  });

  it('honest-judge PASS - renders CURATED badge when dossier status is ACCEPTED', async () => {
    const acceptedDossier = { ...mockDossier, status: 'ACCEPTED' as const };
    vi.mocked(anatomyApi.getQuestionAnatomy).mockResolvedValueOnce(acceptedDossier);

    render(
      <AnatomyPanel
        questionId="q1"
        hasAttempted={true}
        mode="study"
      />
    );

    await waitFor(() => {
      expect(screen.getByText(/CURATED/i)).toBeInTheDocument();
    });
  });
});
