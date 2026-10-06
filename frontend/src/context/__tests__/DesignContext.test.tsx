import React from 'react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  DesignProvider,
  useDesign,
  DESIGN_STORAGE_KEY,
  DEFAULT_DESIGN,
  VALID_DESIGNS,
  DESIGN_META,
  DesignPreset,
} from '../DesignContext';
import { LayoutProvider, useDashboardLayout, STORAGE_KEY as LAYOUT_STORAGE_KEY } from '../LayoutContext';

const TestConsumer: React.FC = () => {
  const { currentDesign, setDesign } = useDesign();
  const { currentLayout } = useDashboardLayout();

  return (
    <div>
      <span data-testid="current-design">{currentDesign}</span>
      <span data-testid="current-layout">{currentLayout}</span>
      <button onClick={() => setDesign('ink')}>Set Ink</button>
      <button onClick={() => setDesign('aurora')}>Set Aurora</button>
      <button onClick={() => setDesign('sky')}>Set Sky</button>
      <button onClick={() => setDesign('tuf')}>Set Classic</button>
      <button onClick={() => setDesign('banana' as DesignPreset)}>Set Corrupt</button>
    </div>
  );
};

describe('DesignContext', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-design');
    delete document.documentElement.dataset.design;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('defaults to tuf when localStorage is empty and sets dataset.design', () => {
    render(
      <DesignProvider>
        <LayoutProvider>
          <TestConsumer />
        </LayoutProvider>
      </DesignProvider>
    );

    expect(screen.getByTestId('current-design').textContent).toBe(DEFAULT_DESIGN);
    expect(screen.getByTestId('current-design').textContent).toBe('tuf');
    expect(document.documentElement.dataset.design).toBe('tuf');
  });

  it('initializes from localStorage if valid design is stored', () => {
    localStorage.setItem(DESIGN_STORAGE_KEY, 'aurora');

    render(
      <DesignProvider>
        <LayoutProvider>
          <TestConsumer />
        </LayoutProvider>
      </DesignProvider>
    );

    expect(screen.getByTestId('current-design').textContent).toBe('aurora');
    expect(document.documentElement.dataset.design).toBe('aurora');
  });

  it('honest-judge: corrupt value "banana" falls back to default tuf', () => {
    localStorage.setItem(DESIGN_STORAGE_KEY, 'banana');

    render(
      <DesignProvider>
        <LayoutProvider>
          <TestConsumer />
        </LayoutProvider>
      </DesignProvider>
    );

    expect(screen.getByTestId('current-design').textContent).toBe('tuf');
    expect(document.documentElement.dataset.design).toBe('tuf');
  });

  it('setDesign persists to localStorage and updates document.documentElement.dataset.design', async () => {
    const user = userEvent.setup();

    render(
      <DesignProvider>
        <LayoutProvider>
          <TestConsumer />
        </LayoutProvider>
      </DesignProvider>
    );

    expect(screen.getByTestId('current-design').textContent).toBe('tuf');

    await user.click(screen.getByText('Set Ink'));
    expect(screen.getByTestId('current-design').textContent).toBe('ink');
    expect(localStorage.getItem(DESIGN_STORAGE_KEY)).toBe('ink');
    expect(document.documentElement.dataset.design).toBe('ink');

    await user.click(screen.getByText('Set Sky'));
    expect(screen.getByTestId('current-design').textContent).toBe('sky');
    expect(localStorage.getItem(DESIGN_STORAGE_KEY)).toBe('sky');
    expect(document.documentElement.dataset.design).toBe('sky');
  });

  it('setDesign applies the paired layout (aurora -> editorial, sky -> bento, ink -> minimalist)', async () => {
    const user = userEvent.setup();

    render(
      <DesignProvider>
        <LayoutProvider>
          <TestConsumer />
        </LayoutProvider>
      </DesignProvider>
    );

    // Initial layout default
    expect(screen.getByTestId('current-layout').textContent).toBe('minimalist');

    // Switch to Aurora -> should pair with editorial
    await user.click(screen.getByText('Set Aurora'));
    expect(screen.getByTestId('current-design').textContent).toBe('aurora');
    expect(screen.getByTestId('current-layout').textContent).toBe('editorial');
    expect(localStorage.getItem(LAYOUT_STORAGE_KEY)).toBe('editorial');

    // Switch to Sky -> should pair with bento
    await user.click(screen.getByText('Set Sky'));
    expect(screen.getByTestId('current-design').textContent).toBe('sky');
    expect(screen.getByTestId('current-layout').textContent).toBe('bento');
    expect(localStorage.getItem(LAYOUT_STORAGE_KEY)).toBe('bento');

    // Switch to Ink -> should pair with minimalist
    await user.click(screen.getByText('Set Ink'));
    expect(screen.getByTestId('current-design').textContent).toBe('ink');
    expect(screen.getByTestId('current-layout').textContent).toBe('minimalist');
    expect(localStorage.getItem(LAYOUT_STORAGE_KEY)).toBe('minimalist');
  });

  it('localStorage failure (mocked throw) does not crash the provider', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('SecurityError: localStorage blocked in private sandbox');
    });

    render(
      <DesignProvider>
        <LayoutProvider>
          <TestConsumer />
        </LayoutProvider>
      </DesignProvider>
    );

    expect(screen.getByTestId('current-design').textContent).toBe(DEFAULT_DESIGN);
  });

  it('provides safe fallback when useDesign is consumed outside provider', () => {
    render(<TestConsumer />);
    expect(screen.getByTestId('current-design').textContent).toBe('tuf');
  });
});
