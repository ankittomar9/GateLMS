import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LayoutProvider, STORAGE_KEY } from '../../context/LayoutContext';
import { Header } from '../../components/Header';
import { DashboardPage } from '../DashboardPage';

describe('Dashboard Multi-Layout Engine & Variant Views', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders Variant 1: Modern Minimalist by default', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <LayoutProvider>
          <DashboardPage />
        </LayoutProvider>
      </MemoryRouter>
    );

    expect(screen.getByTestId('minimalist-dashboard')).toBeDefined();
    expect(screen.getByText(/Process Synchronization & IPC/i)).toBeDefined();
    expect(screen.getByText(/Silberschatz §5/i)).toBeDefined();
    expect(screen.getByText(/Discrete Math & Logic/i)).toBeDefined();
    expect(screen.getByText(/Systems Core \(OS \+ DBMS \+ CN\)/i)).toBeDefined();
  });

  it('renders Variant 2: Editorial & Academic when layout is editorial', () => {
    localStorage.setItem(STORAGE_KEY, 'editorial');

    render(
      <MemoryRouter initialEntries={['/']}>
        <LayoutProvider>
          <DashboardPage />
        </LayoutProvider>
      </MemoryRouter>
    );

    expect(screen.getByTestId('editorial-dashboard')).toBeDefined();
    expect(screen.getByText(/CLRS · Introduction to Algorithms/i)).toBeDefined();
    expect(screen.getByText(/Chapter 2: Getting Started/i)).toBeDefined();
    expect(screen.getByText(/Start Studying/i)).toBeDefined();
    expect(screen.getByText(/Curriculum Architecture & Subject Mastery/i)).toBeDefined();
  });

  it('renders Variant 3: Bento Grid when layout is bento and supports interactive submission', async () => {
    const user = userEvent.setup();
    localStorage.setItem(STORAGE_KEY, 'bento');

    render(
      <MemoryRouter initialEntries={['/']}>
        <LayoutProvider>
          <DashboardPage />
        </LayoutProvider>
      </MemoryRouter>
    );

    expect(screen.getByTestId('bento-dashboard')).toBeDefined();
    expect(screen.getByText(/Graph Theory & Tree Structures/i)).toBeDefined();
    expect(screen.getByText(/Kenneth H. Rosen/i)).toBeDefined();
    expect(screen.getByText(/Planar Graphs & Euler's Formula/i)).toBeDefined();

    // Verify one-click interactive answer submission in Bento Problem of the Day
    const submitBtn = screen.getByRole('button', { name: /Submit Answer/i });
    expect(submitBtn.hasAttribute('disabled')).toBe(true);

    const optionB = screen.getByText(/3.85/i);
    await user.click(optionB);

    expect(screen.getByText(/Selected Option: B/i)).toBeDefined();
    expect(submitBtn.hasAttribute('disabled')).toBe(false);

    await user.click(submitBtn);
    expect(screen.getByText(/Submitted Successfully ✓/i)).toBeDefined();
  });

  it('switches live between all 3 layouts instantaneously when toggled from Header', async () => {
    const user = userEvent.setup();

    render(
      <MemoryRouter initialEntries={['/']}>
        <LayoutProvider>
          <Header />
          <DashboardPage />
        </LayoutProvider>
      </MemoryRouter>
    );

    // Initial state: Minimalist
    expect(screen.getByTestId('minimalist-dashboard')).toBeDefined();
    expect(screen.queryByTestId('editorial-dashboard')).toBeNull();
    expect(screen.queryByTestId('bento-dashboard')).toBeNull();

    // Toggle to Editorial
    await user.click(screen.getByTestId('layout-toggle-editorial'));
    expect(screen.getByTestId('editorial-dashboard')).toBeDefined();
    expect(screen.queryByTestId('minimalist-dashboard')).toBeNull();
    expect(screen.queryByTestId('bento-dashboard')).toBeNull();

    // Toggle to Bento Grid
    await user.click(screen.getByTestId('layout-toggle-bento'));
    expect(screen.getByTestId('bento-dashboard')).toBeDefined();
    expect(screen.queryByTestId('minimalist-dashboard')).toBeNull();
    expect(screen.queryByTestId('editorial-dashboard')).toBeNull();

    // Toggle back to Minimalist
    await user.click(screen.getByTestId('layout-toggle-minimalist'));
    expect(screen.getByTestId('minimalist-dashboard')).toBeDefined();
    expect(screen.queryByTestId('editorial-dashboard')).toBeNull();
    expect(screen.queryByTestId('bento-dashboard')).toBeNull();
  });
});
