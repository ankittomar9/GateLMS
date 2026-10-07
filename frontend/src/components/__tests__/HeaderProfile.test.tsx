import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Header } from '../Header';
import { AuthProvider } from '../../context/AuthContext';
import { DesignProvider } from '../../context/DesignContext';
import { LayoutProvider } from '../../context/LayoutContext';
import { apiClient } from '../../api/apiClient';

const renderHeader = (mockUser: { userId: string; username: string; displayName: string; role: 'ADMIN' | 'STUDENT' } = { userId: 'u1', username: 'ankur', displayName: 'Ankur Tomar', role: 'ADMIN' }) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });

  queryClient.setQueryData(['auth', 'me'], mockUser);

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <DesignProvider>
          <LayoutProvider>
            <MemoryRouter initialEntries={['/']}>
              <Routes>
                <Route path="/" element={<Header />} />
                <Route path="/login" element={<div data-testid="login-page">Login Page</div>} />
              </Routes>
            </MemoryRouter>
          </LayoutProvider>
        </DesignProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
};

describe('Header Profile Chip & Role Badge', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('1 renders user displayName and initial avatar letter', () => {
    renderHeader({
      userId: '1',
      username: 'ankur',
      displayName: 'Ankur Tomar',
      role: 'ADMIN',
    });

    expect(screen.getByTestId('header-display-name').textContent).toBe('Ankur Tomar');
    expect(screen.getByText('A')).toBeDefined();
  });

  it('2 renders role badge correctly for ADMIN and STUDENT', () => {
    const { unmount } = renderHeader({
      userId: '1',
      username: 'ankur',
      displayName: 'Ankur Tomar',
      role: 'ADMIN',
    });

    expect(screen.getByTestId('header-role-badge').textContent).toBe('ADMIN');
    unmount();

    renderHeader({
      userId: '2',
      username: 'student1',
      displayName: 'Jane Doe',
      role: 'STUDENT',
    });

    expect(screen.getByTestId('header-role-badge').textContent).toBe('STUDENT');
  });

  it('3 clicking profile chip opens dropdown and clicking logout invokes logout handler', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ status: 'LOGGED_OUT' });

    renderHeader();

    expect(screen.queryByTestId('header-profile-dropdown')).toBeNull();

    const user = userEvent.setup();
    await user.click(screen.getByTestId('header-profile-chip'));

    expect(screen.getByTestId('header-profile-dropdown')).toBeDefined();
    expect(screen.getByTestId('header-logout-button')).toBeDefined();

    await user.click(screen.getByTestId('header-logout-button'));

    await waitFor(() => {
      expect(postSpy).toHaveBeenCalledWith('/auth/logout');
      expect(screen.getByTestId('login-page')).toBeDefined();
    });
  });
});
