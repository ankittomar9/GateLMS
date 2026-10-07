import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { LoginPage } from '../LoginPage';
import { AuthProvider } from '../../context/AuthContext';
import { apiClient, ApiError } from '../../api/apiClient';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
};

describe('LoginPage Component', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    // Default me check returns 401 (unauthenticated)
    vi.spyOn(apiClient, 'get').mockRejectedValue(new ApiError(401, 'Unauthorized'));
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('1 renders username, password inputs and submit button', async () => {
    const Wrapper = createWrapper();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Wrapper>
          <LoginPage />
        </Wrapper>
      </MemoryRouter>
    );

    expect(screen.getByTestId('login-username-input')).toBeDefined();
    expect(screen.getByTestId('login-password-input')).toBeDefined();
    expect(screen.getByTestId('login-submit-button')).toBeDefined();
    expect(screen.getByText(/GateLMS Sign In/i)).toBeDefined();
  });

  it('2 submits credentials and navigates on success', async () => {
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      userId: 'user-1',
      username: 'ankur',
      displayName: 'Ankur Tomar',
      role: 'ADMIN',
    });

    const Wrapper = createWrapper();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Wrapper>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<div data-testid="dashboard-target">Dashboard</div>} />
          </Routes>
        </Wrapper>
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByTestId('login-username-input'), 'ankur');
    await user.type(screen.getByTestId('login-password-input'), 'admin123');
    await user.click(screen.getByTestId('login-submit-button'));

    await waitFor(() => {
      expect(postSpy).toHaveBeenCalledWith('/auth/login', {
        username: 'ankur',
        password: 'admin123',
      });
      expect(screen.getByTestId('dashboard-target')).toBeDefined();
    });
  });

  it('3 navigates to next path if specified in search query', async () => {
    vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
      userId: 'user-2',
      username: 'student',
      displayName: 'Student User',
      role: 'STUDENT',
    });

    const Wrapper = createWrapper();
    render(
      <MemoryRouter initialEntries={['/login?next=%2Fquestions']}>
        <Wrapper>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/questions" element={<div data-testid="questions-target">Questions Page</div>} />
          </Routes>
        </Wrapper>
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByTestId('login-username-input'), 'student');
    await user.type(screen.getByTestId('login-password-input'), 'pass123');
    await user.click(screen.getByTestId('login-submit-button'));

    await waitFor(() => {
      expect(screen.getByTestId('questions-target')).toBeDefined();
    });
  });

  it('4 displays error banner on 401 invalid credentials', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValueOnce(
      new ApiError(401, 'Invalid credentials')
    );

    const Wrapper = createWrapper();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Wrapper>
          <LoginPage />
        </Wrapper>
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByTestId('login-username-input'), 'wronguser');
    await user.type(screen.getByTestId('login-password-input'), 'wrongpass');
    await user.click(screen.getByTestId('login-submit-button'));

    await waitFor(() => {
      expect(screen.getByTestId('login-error-banner')).toBeDefined();
      expect(screen.getByText('Invalid credentials')).toBeDefined();
    });
  });

  it('5 displays 429 rate limit countdown banner and disables submission', async () => {
    vi.spyOn(apiClient, 'post').mockRejectedValueOnce(
      new ApiError(429, 'Rate limited', undefined, undefined, 60)
    );

    const Wrapper = createWrapper();
    render(
      <MemoryRouter initialEntries={['/login']}>
        <Wrapper>
          <LoginPage />
        </Wrapper>
      </MemoryRouter>
    );

    const user = userEvent.setup();
    await user.type(screen.getByTestId('login-username-input'), 'badattempts');
    await user.type(screen.getByTestId('login-password-input'), 'wrong');
    await user.click(screen.getByTestId('login-submit-button'));

    await waitFor(() => {
      expect(screen.getByTestId('login-ratelimit-banner')).toBeDefined();
      expect(screen.getByText(/Rate limited. Please retry in/i)).toBeDefined();
      expect((screen.getByTestId('login-username-input') as HTMLInputElement).disabled).toBe(true);
      expect((screen.getByTestId('login-password-input') as HTMLInputElement).disabled).toBe(true);
      expect((screen.getByTestId('login-submit-button') as HTMLButtonElement).disabled).toBe(true);
    });
  });
});
