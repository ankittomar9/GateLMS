import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { apiClient, getXsrfToken, request } from '../api/apiClient';
import { AuthProvider, useAuth } from '../context/AuthContext';
import { RequireAuth } from '../components/auth/RequireAuth';

describe('Auth Flow & Interceptor Integration', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('1 apiClient automatically forwards X-XSRF-TOKEN on mutating requests', async () => {
    // Set cookie
    Object.defineProperty(document, 'cookie', {
      writable: true,
      value: 'XSRF-TOKEN=test-csrf-token-12345; other=value',
    });

    expect(getXsrfToken()).toBe('test-csrf-token-12345');

    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify({ success: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    await apiClient.post('/notes', { title: 'Test Note' });

    expect(fetchSpy).toHaveBeenCalled();
    const [calledUrl, calledConfig] = fetchSpy.mock.calls[0];
    expect(calledUrl).toBe('/api/v1/notes');
    const headers = calledConfig?.headers as Record<string, string>;
    expect(headers['X-XSRF-TOKEN']).toBe('test-csrf-token-12345');
    expect(calledConfig?.credentials).toBe('include');
  });

  it('2 401 interceptor redirects to /login?next=currentPath', async () => {
    const originalLocation = window.location;
    // Mock window.location
    delete (window as any).location;
    window.location = {
      ...originalLocation,
      pathname: '/practice',
      search: '?topic=os',
      href: '',
    } as any;

    vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      new Response(JSON.stringify({ detail: 'Unauthorized' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      })
    );

    try {
      await apiClient.get('/practice/sessions');
    } catch {
      // Expected ApiError
    }

    expect(window.location.href).toBe('/login?next=%2Fpractice%3Ftopic%3Dos');

    (window as any).location = originalLocation;
  });

  it('3 RequireAuth redirects unauthenticated user to /login?next=...', async () => {
    vi.spyOn(apiClient, 'get').mockRejectedValue(new Error('Unauthorized'));

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <MemoryRouter initialEntries={['/protected']}>
            <Routes>
              <Route path="/login" element={<div data-testid="login-view">Login View</div>} />
              <Route
                path="/protected"
                element={
                  <RequireAuth>
                    <div data-testid="protected-content">Secret Content</div>
                  </RequireAuth>
                }
              />
            </Routes>
          </MemoryRouter>
        </AuthProvider>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('login-view')).toBeDefined();
      expect(screen.queryByTestId('protected-content')).toBeNull();
    });
  });

  it('4 logout clears user state and calls auth/logout endpoint', async () => {
    vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
      userId: 'u1',
      username: 'ankur',
      displayName: 'Ankur',
      role: 'ADMIN',
    });
    const postSpy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({ status: 'LOGGED_OUT' });

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    const TestComponent = () => {
      const { user, logout } = useAuth();
      return (
        <div>
          <span data-testid="user-status">{user ? user.username : 'Anonymous'}</span>
          <button data-testid="logout-btn" onClick={() => logout()}>
            Logout
          </button>
        </div>
      );
    };

    render(
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <TestComponent />
        </AuthProvider>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user-status').textContent).toBe('ankur');
    });

    const user = userEvent.setup();
    await user.click(screen.getByTestId('logout-btn'));

    await waitFor(() => {
      expect(postSpy).toHaveBeenCalledWith('/auth/logout');
      expect(screen.getByTestId('user-status').textContent).toBe('Anonymous');
    });
  });
});
