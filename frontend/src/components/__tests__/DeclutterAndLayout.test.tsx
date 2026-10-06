import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { LayoutProvider, STORAGE_KEY } from '../../context/LayoutContext';
import { Header } from '../Header';
import { Sidebar } from '../Sidebar';
import { RightSidebar } from '../RightSidebar';

describe('UI Declutter and Navigation Verification', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('Header Declutter & Layout Switcher', () => {
    it('verifies points badge and countdown pill are purged from Header', () => {
      render(
        <LayoutProvider>
          <Header />
        </LayoutProvider>
      );

      // Spec §2.1 removals
      expect(screen.queryByText(/pts/i)).toBeNull();
      expect(screen.queryByText(/420/)).toBeNull();
      expect(screen.queryByText(/GATE 2028: ~485 Days/i)).toBeNull();
      expect(screen.queryByText(/485 Days/i)).toBeNull();

      // Verified retained items
      expect(screen.getByText('14')).toBeDefined();
      expect(screen.getByText(/days streak/i)).toBeDefined();
      expect(screen.getByPlaceholderText(/Search topics/i)).toBeDefined();
    });

    it('renders the layout switcher with 3 options and updates layout on click', async () => {
      const user = userEvent.setup();

      render(
        <LayoutProvider>
          <Header />
        </LayoutProvider>
      );

      const minimalistBtn = screen.getByTestId('layout-toggle-minimalist');
      const editorialBtn = screen.getByTestId('layout-toggle-editorial');
      const bentoBtn = screen.getByTestId('layout-toggle-bento');

      expect(minimalistBtn).toBeDefined();
      expect(editorialBtn).toBeDefined();
      expect(bentoBtn).toBeDefined();

      expect(minimalistBtn.getAttribute('aria-checked')).toBe('true');
      expect(editorialBtn.getAttribute('aria-checked')).toBe('false');

      // Click Editorial
      await user.click(editorialBtn);
      expect(editorialBtn.getAttribute('aria-checked')).toBe('true');
      expect(minimalistBtn.getAttribute('aria-checked')).toBe('false');
      expect(localStorage.getItem(STORAGE_KEY)).toBe('editorial');

      // Click Bento
      await user.click(bentoBtn);
      expect(bentoBtn.getAttribute('aria-checked')).toBe('true');
      expect(localStorage.getItem(STORAGE_KEY)).toBe('bento');
    });
  });

  describe('Sidebar Navigation Declutter', () => {
    it('verifies redundant Planly Study Schedule nav link is purged', () => {
      render(
        <MemoryRouter initialEntries={['/']}>
          <Sidebar />
        </MemoryRouter>
      );

      // Spec §2.2 removal
      expect(screen.queryByText(/Planly Study Schedule/i)).toBeNull();

      // Verified retained navigation
      expect(screen.getByText('Dashboard')).toBeDefined();
      expect(screen.getByText('Syllabus Taxonomy')).toBeDefined();
      expect(screen.getByText('Question Bank')).toBeDefined();
      expect(screen.getByText('Mistake Notebook')).toBeDefined();
    });
  });

  describe('RightSidebar Declutter & Theming', () => {
    it('verifies promo card and gimmicky point badges are purged from RightSidebar', () => {
      render(
        <LayoutProvider>
          <RightSidebar />
        </LayoutProvider>
      );

      // Spec §2.3 removals
      expect(screen.queryByText(/Too many tabs open/i)).toBeNull();
      expect(screen.queryByText(/Open Focus Mode/i)).toBeNull();
      expect(screen.queryByText(/One-stop platform/i)).toBeNull();
      expect(screen.queryByText(/\+20/)).toBeNull();

      // Verified retained components
      expect(screen.getByText(/GATE Syllabus Progress/i)).toBeDefined();
      expect(screen.getByText(/16.5/)).toBeDefined();
      expect(screen.getByText(/Problem of the Day/i)).toBeDefined();
      expect(screen.getByText(/Due for Revision/i)).toBeDefined();
    });
  });
});
