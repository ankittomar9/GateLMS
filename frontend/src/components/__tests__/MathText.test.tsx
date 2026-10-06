import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MathText } from '../MathText';

describe('MathText Component', () => {
  it('honest-judge PASS - renders plain text with no math segments', () => {
    const { container } = render(<MathText content="What is the page table base address?" />);
    expect(container.textContent).toContain('What is the page table base address?');
    expect(container.querySelector('.katex')).toBeNull();
  });

  it('honest-judge PASS - renders inline math $x^2+1$ with KaTeX markup', () => {
    const { container } = render(<MathText content="The polynomial is $x^2+1$." />);
    expect(container.textContent).toContain('The polynomial is');
    const katexEl = container.querySelector('.katex');
    expect(katexEl).not.toBeNull();
  });

  it('honest-judge PASS - renders display math $$\\sum_{i=1}^n i$$ in block mode', () => {
    const { container } = render(<MathText content="Sum: $$\\sum_{i=1}^n i$$" />);
    const katexEl = container.querySelector('.katex');
    expect(katexEl).not.toBeNull();
    const blockEl = container.querySelector('.block');
    expect(blockEl).not.toBeNull();
  });

  it('honest-judge PASS - renders mixed plain text and multiple inline math formulas', () => {
    const { container } = render(
      <MathText content="Let $G = (V, E)$ be a graph with $|V| = n$ vertices and $|E| = m$ edges." />
    );
    const katexElements = container.querySelectorAll('.katex');
    expect(katexElements.length).toBeGreaterThanOrEqual(3);
  });

  it('honest-judge FAIL - deliberate-wrong malformed $\\frac{$ renders fallback chip without crashing and body text does not contain raw frac', () => {
    const { container } = render(<MathText content="Compute value: $\\frac{$ in O(1) time." />);
    const fallbackChip = screen.getByTestId('math-fallback');
    expect(fallbackChip).toBeInTheDocument();
    expect(fallbackChip.textContent).toBe('[Math Error]');
    // Assert body text does not contain raw unrendered LaTeX keyword "frac"
    expect(container.textContent).not.toContain('frac');
  });
});
