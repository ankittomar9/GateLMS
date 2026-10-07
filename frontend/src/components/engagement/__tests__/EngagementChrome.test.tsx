import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StreakChip } from '../StreakChip';
import { GoalRing } from '../GoalRing';
import { NotificationBell } from '../NotificationBell';
import * as queryHooks from '../../../hooks/useQueries';
import * as engagementApi from '../../../api/engagementApi';
import { EngagementSummaryResponse, NotificationDto } from '../../../api/types';

const mockedNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe('EngagementChrome Components', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  const renderWithProviders = (ui: React.ReactElement) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{ui}</MemoryRouter>
      </QueryClientProvider>
    );
  };

  it('1. streak number + risk state from fixture', () => {
    const summaryFixture: EngagementSummaryResponse = {
      streak: 5,
      streakAtRisk: true,
      goal: { target: 20, done: 0, met: false },
      dueToday: { mistakes: 2, topics: 1 },
    };

    vi.spyOn(queryHooks, 'useEngagementSummary').mockReturnValue({
      data: summaryFixture,
      isLoading: false,
    } as any);

    renderWithProviders(<StreakChip />);

    const chip = screen.getByTestId('streak-chip');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent('5');
    expect(screen.getByTestId('streak-at-risk')).toBeInTheDocument();
    expect(chip.title).toContain('Streak at risk');
  });

  it('2. goal ring math + met state', () => {
    const summaryFixtureMet: EngagementSummaryResponse = {
      streak: 4,
      streakAtRisk: false,
      goal: { target: 20, done: 20, met: true },
      dueToday: { mistakes: 0, topics: 0 },
    };

    vi.spyOn(queryHooks, 'useEngagementSummary').mockReturnValue({
      data: summaryFixtureMet,
      isLoading: false,
    } as any);

    const { unmount } = renderWithProviders(<GoalRing />);

    expect(screen.getByTestId('goal-ring')).toBeInTheDocument();
    expect(screen.getByTestId('goal-done-count')).toHaveTextContent('20');
    expect(screen.getByTestId('goal-target-count')).toHaveTextContent('20');
    expect(screen.getByTestId('goal-progress-pct')).toHaveTextContent('100%');
    expect(screen.getByTestId('goal-met-badge')).toBeInTheDocument();

    const ctaBtn = screen.getByTestId('goal-practice-cta');
    fireEvent.click(ctaBtn);
    expect(mockedNavigate).toHaveBeenCalledWith('/practice');

    unmount();

    // Test unmet state math
    const summaryFixtureUnmet: EngagementSummaryResponse = {
      streak: 4,
      streakAtRisk: false,
      goal: { target: 50, done: 20, met: false },
      dueToday: { mistakes: 0, topics: 0 },
    };

    vi.spyOn(queryHooks, 'useEngagementSummary').mockReturnValue({
      data: summaryFixtureUnmet,
      isLoading: false,
    } as any);

    renderWithProviders(<GoalRing />);
    expect(screen.getByTestId('goal-progress-pct')).toHaveTextContent('40%');
    expect(screen.queryByTestId('goal-met-badge')).not.toBeInTheDocument();
  });

  it('3. badge from unread-count', () => {
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 3,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useTop10Notifications').mockReturnValue({
      data: [],
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useMarkNotificationRead').mockReturnValue({
      mutate: vi.fn(),
    } as any);
    vi.spyOn(queryHooks, 'useMarkAllNotificationsRead').mockReturnValue({
      mutate: vi.fn(),
    } as any);

    const { unmount } = renderWithProviders(<NotificationBell />);

    const badge = screen.getByTestId('notification-badge');
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent('3');

    unmount();

    // 0 unread count hides badge
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 0,
      isLoading: false,
    } as any);

    renderWithProviders(<NotificationBell />);
    expect(screen.queryByTestId('notification-badge')).not.toBeInTheDocument();
  });

  it('4. dropdown renders 10 + mark-all mutation', () => {
    const mockNotifications: NotificationDto[] = Array.from({ length: 10 }).map((_, i) => ({
      id: `notif-${i + 1}`,
      type: i % 2 === 0 ? 'REVISION_DUE' : 'ACHIEVEMENT_EARNED',
      title: `Notification ${i + 1}`,
      body: `Body ${i + 1}`,
      link: '/revision',
      readAt: null,
      createdAt: '2026-10-07T08:00:00Z',
    }));

    const markAllMutate = vi.fn();

    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 10,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useTop10Notifications').mockReturnValue({
      data: mockNotifications,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useMarkNotificationRead').mockReturnValue({
      mutate: vi.fn(),
    } as any);
    vi.spyOn(queryHooks, 'useMarkAllNotificationsRead').mockReturnValue({
      mutate: markAllMutate,
      isPending: false,
    } as any);

    renderWithProviders(<NotificationBell />);

    // Click bell button to toggle dropdown
    fireEvent.click(screen.getByTestId('notification-bell-btn'));

    const panel = screen.getByTestId('notification-dropdown-panel');
    expect(panel).toBeInTheDocument();

    // Verify all 10 items rendered
    expect(screen.getByTestId('notification-item-notif-1')).toBeInTheDocument();
    expect(screen.getByTestId('notification-item-notif-10')).toBeInTheDocument();

    // Trigger mark-all mutation
    const markAllBtn = screen.getByTestId('mark-all-read-btn');
    fireEvent.click(markAllBtn);
    expect(markAllMutate).toHaveBeenCalledTimes(1);

    // Click "View all notifications" navigates to /notifications
    const viewAllBtn = screen.getByTestId('view-all-notifications-link');
    fireEvent.click(viewAllBtn);
    expect(mockedNavigate).toHaveBeenCalledWith('/notifications');
  });

  it('5. empty panel state', () => {
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 0,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useTop10Notifications').mockReturnValue({
      data: [],
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useMarkNotificationRead').mockReturnValue({
      mutate: vi.fn(),
    } as any);
    vi.spyOn(queryHooks, 'useMarkAllNotificationsRead').mockReturnValue({
      mutate: vi.fn(),
    } as any);

    renderWithProviders(<NotificationBell />);

    // Open dropdown
    fireEvent.click(screen.getByTestId('notification-bell-btn'));

    expect(screen.getByTestId('notifications-empty')).toBeInTheDocument();
    expect(screen.getByText('No notifications yet')).toBeInTheDocument();
    expect(screen.queryByTestId('mark-all-read-btn')).not.toBeInTheDocument();
  });

  it('6. broken-streak muted state', () => {
    const brokenSummary: EngagementSummaryResponse = {
      streak: 0,
      streakAtRisk: false,
      goal: { target: 20, done: 0, met: false },
      dueToday: { mistakes: 0, topics: 0 },
    };

    vi.spyOn(queryHooks, 'useEngagementSummary').mockReturnValue({
      data: brokenSummary,
      isLoading: false,
    } as any);

    renderWithProviders(<StreakChip />);

    const chip = screen.getByTestId('streak-chip');
    expect(chip).toBeInTheDocument();
    expect(chip).toHaveTextContent('0');
    expect(screen.getByTestId('streak-broken')).toBeInTheDocument();
    expect(chip.title).toContain('No active streak');
  });

  it('7. error rollback on unread count mutation', async () => {
    vi.restoreAllMocks();

    // Setup initial cache with unread count = 5
    queryClient.setQueryData(['notifications-unread-count'], 5);

    // Mock API failure on markNotificationRead
    const markReadSpy = vi
      .spyOn(engagementApi, 'markNotificationRead')
      .mockRejectedValue(new Error('Network error'));

    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    const { result } = renderHook(() => queryHooks.useMarkNotificationRead(), { wrapper });

    // Execute mutation that fails
    try {
      await act(async () => {
        await result.current.mutateAsync('notif-123');
      });
    } catch {
      // Expected error
    }

    // After failure, unread count must roll back to 5
    await waitFor(() => {
      const rolledBackCount = queryClient.getQueryData(['notifications-unread-count']);
      expect(rolledBackCount).toBe(5);
    });

    markReadSpy.mockRestore();
  });
});
