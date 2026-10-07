import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LayoutProvider, STORAGE_KEY } from '../../context/LayoutContext';
import { DesignProvider } from '../../context/DesignContext';
import { Header } from '../../components/Header';
import { DashboardPage } from '../DashboardPage';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const renderWithProviders = (ui: React.ReactElement) => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      {ui}
    </QueryClientProvider>
  );
};

describe('Dashboard Multi-Layout Engine & Variant Views', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders Variant 1: Modern Minimalist by default', () => {
    renderWithProviders(
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

    renderWithProviders(
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

    renderWithProviders(
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

    renderWithProviders(
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

  it('renders Dashboard without throwing for each valid design preset', () => {
    const presets: Array<'tuf' | 'ink' | 'aurora' | 'sky'> = ['tuf', 'ink', 'aurora', 'sky'];

    presets.forEach((preset) => {
      localStorage.setItem('gatelms_design', preset);

      const { unmount } = renderWithProviders(
        <MemoryRouter initialEntries={['/']}>
          <DesignProvider>
            <LayoutProvider>
              <DashboardPage />
            </LayoutProvider>
          </DesignProvider>
        </MemoryRouter>
      );

      // Verify that the dashboard variant is mounted
      expect(
        screen.queryByTestId('minimalist-dashboard') ||
        screen.queryByTestId('editorial-dashboard') ||
        screen.queryByTestId('bento-dashboard')
      ).not.toBeNull();

      unmount();
    });
  });

  it('mounts design-specific sections only when their preset is active', () => {
    // 1. Sky preset -> sky-welcome-banner mounted
    localStorage.setItem('gatelms_design', 'sky');
    localStorage.setItem(STORAGE_KEY, 'bento');
    const { unmount: unmountSky } = renderWithProviders(
      <MemoryRouter initialEntries={['/']}>
        <DesignProvider>
          <LayoutProvider>
            <DashboardPage />
          </LayoutProvider>
        </DesignProvider>
      </MemoryRouter>
    );
    expect(screen.getByTestId('sky-welcome-banner')).toBeDefined();
    expect(screen.getByText(/Good to see you, Aspirant/i)).toBeDefined();
    unmountSky();

    // 2. Ink preset -> ink-quote-banner and ink-inline-column mounted, sky banner not mounted
    localStorage.setItem('gatelms_design', 'ink');
    localStorage.setItem(STORAGE_KEY, 'minimalist');
    const { unmount: unmountInk } = renderWithProviders(
      <MemoryRouter initialEntries={['/']}>
        <DesignProvider>
          <LayoutProvider>
            <DashboardPage />
          </LayoutProvider>
        </DesignProvider>
      </MemoryRouter>
    );
    expect(screen.getByTestId('ink-quote-banner')).toBeDefined();
    expect(screen.getByTestId('ink-inline-column')).toBeDefined();
    expect(screen.queryByTestId('sky-welcome-banner')).toBeNull();
    unmountInk();

    // 3. Aurora preset -> aurora-book-card mounted, sky banner not mounted
    localStorage.setItem('gatelms_design', 'aurora');
    localStorage.setItem(STORAGE_KEY, 'editorial');
    const { unmount: unmountAurora } = renderWithProviders(
      <MemoryRouter initialEntries={['/']}>
        <DesignProvider>
          <LayoutProvider>
            <DashboardPage />
          </LayoutProvider>
        </DesignProvider>
      </MemoryRouter>
    );
    expect(screen.getByTestId('aurora-book-card')).toBeDefined();
    expect(screen.getByText(/CLRS · Introduction to Algorithms/i)).toBeDefined();
    expect(screen.queryByTestId('sky-welcome-banner')).toBeNull();
    expect(screen.queryByTestId('ink-quote-banner')).toBeNull();
    unmountAurora();

    // 4. Tuf preset -> none of the special mockup cards mounted
    localStorage.setItem('gatelms_design', 'tuf');
    localStorage.setItem(STORAGE_KEY, 'minimalist');
    const { unmount: unmountTuf } = renderWithProviders(
      <MemoryRouter initialEntries={['/']}>
        <DesignProvider>
          <LayoutProvider>
            <DashboardPage />
          </LayoutProvider>
        </DesignProvider>
      </MemoryRouter>
    );
    expect(screen.queryByTestId('sky-welcome-banner')).toBeNull();
    expect(screen.queryByTestId('ink-quote-banner')).toBeNull();
    expect(screen.queryByTestId('aurora-book-card')).toBeNull();
    unmountTuf();
  });
});
