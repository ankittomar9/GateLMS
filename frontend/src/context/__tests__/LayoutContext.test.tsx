import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { 
  LayoutProvider, 
  useDashboardLayout, 
  STORAGE_KEY, 
  DEFAULT_LAYOUT,
  DashboardLayoutVariant 
} from '../LayoutContext';

const TestConsumer: React.FC = () => {
  const { 
    currentLayout, 
    setLayout,
    rightSidebarCollapsed,
    toggleRightSidebar,
    leftSidebarCollapsed,
    toggleLeftSidebar,
  } = useDashboardLayout();
  return (
    <div>
      <span data-testid="current-layout">{currentLayout}</span>
      <span data-testid="right-collapsed">{String(rightSidebarCollapsed)}</span>
      <span data-testid="left-collapsed">{String(leftSidebarCollapsed)}</span>
      <button onClick={() => setLayout('editorial')}>Set Editorial</button>
      <button onClick={() => setLayout('bento')}>Set Bento</button>
      <button onClick={() => setLayout('minimalist')}>Set Minimalist</button>
      <button onClick={toggleRightSidebar}>Toggle Right Sidebar</button>
      <button onClick={toggleLeftSidebar}>Toggle Left Sidebar</button>
    </div>
  );
};

describe('LayoutContext', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('defaults to minimalist layout when localStorage is empty', () => {
    render(
      <LayoutProvider>
        <TestConsumer />
      </LayoutProvider>
    );

    expect(screen.getByTestId('current-layout').textContent).toBe(DEFAULT_LAYOUT);
    expect(screen.getByTestId('current-layout').textContent).toBe('minimalist');
  });

  it('initializes from localStorage if valid layout is stored', () => {
    localStorage.setItem(STORAGE_KEY, 'editorial');

    render(
      <LayoutProvider>
        <TestConsumer />
      </LayoutProvider>
    );

    expect(screen.getByTestId('current-layout').textContent).toBe('editorial');
  });

  it('updates layout state and writes to localStorage when setLayout is invoked', async () => {
    const user = userEvent.setup();

    render(
      <LayoutProvider>
        <TestConsumer />
      </LayoutProvider>
    );

    expect(screen.getByTestId('current-layout').textContent).toBe('minimalist');

    await user.click(screen.getByText('Set Editorial'));
    expect(screen.getByTestId('current-layout').textContent).toBe('editorial');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('editorial');

    await user.click(screen.getByText('Set Bento'));
    expect(screen.getByTestId('current-layout').textContent).toBe('bento');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('bento');

    await user.click(screen.getByText('Set Minimalist'));
    expect(screen.getByTestId('current-layout').textContent).toBe('minimalist');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('minimalist');
  });

  it('safely handles unknown localStorage values by falling back to default', () => {
    localStorage.setItem(STORAGE_KEY, 'invalid_unknown_variant');

    render(
      <LayoutProvider>
        <TestConsumer />
      </LayoutProvider>
    );

    expect(screen.getByTestId('current-layout').textContent).toBe('minimalist');
  });

  it('toggles right and left sidebar collapsed states and persists to localStorage', async () => {
    const user = userEvent.setup();

    render(
      <LayoutProvider>
        <TestConsumer />
      </LayoutProvider>
    );

    expect(screen.getByTestId('right-collapsed').textContent).toBe('false');
    expect(screen.getByTestId('left-collapsed').textContent).toBe('false');

    // Toggle right
    await user.click(screen.getByText('Toggle Right Sidebar'));
    expect(screen.getByTestId('right-collapsed').textContent).toBe('true');
    expect(localStorage.getItem('gatelms_right_sidebar_collapsed')).toBe('true');

    // Toggle left
    await user.click(screen.getByText('Toggle Left Sidebar'));
    expect(screen.getByTestId('left-collapsed').textContent).toBe('true');
    expect(localStorage.getItem('gatelms_left_sidebar_collapsed')).toBe('true');

    // Toggle right back
    await user.click(screen.getByText('Toggle Right Sidebar'));
    expect(screen.getByTestId('right-collapsed').textContent).toBe('false');
    expect(localStorage.getItem('gatelms_right_sidebar_collapsed')).toBe('false');
  });
});
