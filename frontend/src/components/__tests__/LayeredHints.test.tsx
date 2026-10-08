import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LayeredHints } from '../LayeredHints';
import { QuestionAnatomyDto } from '../../api/anatomyApi';

const mockDossier: QuestionAnatomyDto = {
  id: 'dossier-uuid-1',
  questionId: 'question-uuid-1',
  testedConcept: 'Lossless join decomposition under $BCNF$.',
  invariantRule: 'Determinant must be a superkey in every non-trivial functional dependency.',
  trapAnalysis: [
    {
      option: 'A',
      verdict: 'INCORRECT',
      why_plausible: 'Confuses 3NF with BCNF',
      misconception: 'Assumes FD preservation is required for BCNF',
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
  similarPatternHint: 'Standard decomposition invariant pattern.',
  status: 'DRAFT',
  parseError: null,
  generatorModel: 'gemini-3.8-flash',
  generatedAt: '2026-10-08T12:00:00Z',
  updatedAt: null,
};

describe('LayeredHints Component', () => {
  it('honest-judge PASS - hard-hidden in test mode', () => {
    const { container } = render(
      <LayeredHints
        anatomy={mockDossier}
        mode="test"
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('honest-judge PASS - hard-hidden in mock mode', () => {
    const { container } = render(
      <LayeredHints
        anatomy={mockDossier}
        mode="mock"
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('honest-judge PASS - hard-hidden when anatomy is null', () => {
    const { container } = render(
      <LayeredHints
        anatomy={null}
        mode="study"
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('honest-judge PASS - progressive 3-stage reveal workflow in study mode', async () => {
    const user = userEvent.setup();
    const handlePrereqClick = vi.fn();

    render(
      <LayeredHints
        anatomy={mockDossier}
        mode="study"
        onPrerequisiteClick={handlePrereqClick}
      />
    );

    // Initial state: Level 0
    expect(screen.getByText(/Progressive Hints \(Study Mode\)/i)).toBeInTheDocument();
    expect(screen.getByText('0 of 3 revealed')).toBeInTheDocument();
    expect(screen.queryByText(/Lossless join decomposition/i)).not.toBeInTheDocument();

    // Reveal Hint 1
    const hint1Btn = screen.getByRole('button', { name: /Reveal Hint 1/i });
    await user.click(hint1Btn);

    expect(screen.getByText('1 of 3 revealed')).toBeInTheDocument();
    expect(screen.getByText(/Lossless join decomposition/i)).toBeInTheDocument();
    expect(screen.queryByText(/Determinant must be a superkey/i)).not.toBeInTheDocument();

    // Reveal Hint 2
    const hint2Btn = screen.getByRole('button', { name: /Reveal Hint 2/i });
    await user.click(hint2Btn);

    expect(screen.getByText('2 of 3 revealed')).toBeInTheDocument();
    expect(screen.getByText(/Determinant must be a superkey/i)).toBeInTheDocument();
    expect(screen.queryByText('Functional Dependency Closure')).not.toBeInTheDocument();

    // Reveal Hint 3
    const hint3Btn = screen.getByRole('button', { name: /Reveal Hint 3/i });
    await user.click(hint3Btn);

    expect(screen.getByText('3 of 3 revealed')).toBeInTheDocument();
    expect(screen.getByText('Functional Dependency Closure')).toBeInTheDocument();

    // Click prerequisite unit link
    const viewUnitBtn = screen.getByRole('button', { name: /View Concept Unit/i });
    await user.click(viewUnitBtn);
    expect(handlePrereqClick).toHaveBeenCalledWith('functional-dependency-closure');
  });

  it('honest-judge PASS - never leaks option verdicts or letters in hints', () => {
    render(
      <LayeredHints
        anatomy={mockDossier}
        mode="study"
      />
    );

    const bodyText = document.body.textContent || '';
    expect(bodyText).not.toContain('INCORRECT');
    expect(bodyText).not.toContain('CORRECT');
    expect(bodyText).not.toContain('Option A');
  });
});
