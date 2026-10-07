import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { NotificationsPage } from '../NotificationsPage';
import * as queryHooks from '../../hooks/useQueries';
import { NotificationDto, PageResponse } from '../../api/types';

const mockedNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockedNavigate,
  };
});

describe('NotificationsPage', () => {
  let queryClient: QueryClient;
  const mutateMarkRead = vi.fn();
  const mutateMarkAllRead = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    vi.spyOn(queryHooks, 'useMarkNotificationRead').mockReturnValue({
      mutate: mutateMarkRead,
      isPending: false,
    } as any);

    vi.spyOn(queryHooks, 'useMarkAllNotificationsRead').mockReturnValue({
      mutate: mutateMarkAllRead,
      isPending: false,
    } as any);
  });

  const renderComponent = () => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );
  };

  const sampleNotifications: NotificationDto[] = [
    {
      id: 'notif-1',
      type: 'ACHIEVEMENT_EARNED',
      title: 'First Question Solved!',
      body: 'You answered your very first question.',
      link: '/insights',
      readAt: null,
      createdAt: '2026-10-07T08:00:00Z',
    },
    {
      id: 'notif-2',
      type: 'STREAK_AT_RISK',
      title: 'Keep Your Streak Alive',
      body: 'Answer a question before midnight.',
      link: '/practice',
      readAt: null,
      createdAt: '2026-10-07T20:00:00Z',
    },
    {
      id: 'notif-3',
      type: 'REVISION_DUE',
      title: 'Revision Due: 5 Mistakes',
      body: 'Time to review your mistake notebook.',
      link: '/revision',
      readAt: '2026-10-07T09:00:00Z',
      createdAt: '2026-10-07T08:00:00Z',
    },
    {
      id: 'notif-4',
      type: 'PLAN_DUE',
      title: 'Daily Study Plan Ready',
      body: '2 items pending on your calendar.',
      link: '/planner',
      readAt: null,
      createdAt: '2026-10-07T08:30:00Z',
    },
  ];

  it('1. list + unread filter toggle', () => {
    const pagedResponse: PageResponse<NotificationDto> = {
      content: sampleNotifications,
      totalElements: 4,
      totalPages: 1,
      size: 20,
      page: 0,
    };

    const useNotificationsSpy = vi
      .spyOn(queryHooks, 'useNotifications')
      .mockImplementation((unreadOnly) => {
        const filtered = unreadOnly
          ? sampleNotifications.filter((n) => !n.readAt)
          : sampleNotifications;
        return {
          data: { ...pagedResponse, content: filtered, totalElements: filtered.length },
          isLoading: false,
        } as any;
      });

    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 3,
      isLoading: false,
    } as any);

    renderComponent();

    expect(screen.getByTestId('notifications-page')).toBeInTheDocument();
    expect(screen.getByText('Notification Center')).toBeInTheDocument();
    expect(screen.getByTestId('unread-count-badge')).toHaveTextContent('3 unread');

    // All 4 notifications shown
    expect(screen.getByTestId('notification-item-notif-1')).toBeInTheDocument();
    expect(screen.getByTestId('notification-item-notif-3')).toBeInTheDocument();

    // Toggle unread filter tab
    const unreadTab = screen.getByTestId('filter-unread-tab');
    fireEvent.click(unreadTab);

    // Verify useNotifications called with unreadOnly=true
    expect(useNotificationsSpy).toHaveBeenCalledWith(true, 0, 20);
  });

  it('2. click navigation link and mark read', () => {
    const pagedResponse: PageResponse<NotificationDto> = {
      content: [sampleNotifications[3]], // PLAN_DUE
      totalElements: 1,
      totalPages: 1,
      size: 20,
      page: 0,
    };

    vi.spyOn(queryHooks, 'useNotifications').mockReturnValue({
      data: pagedResponse,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 1,
      isLoading: false,
    } as any);

    renderComponent();

    const notifItem = screen.getByTestId('notification-item-notif-4');
    fireEvent.click(notifItem);

    // Should mutate mark-read and navigate to link
    expect(mutateMarkRead).toHaveBeenCalledWith('notif-4');
    expect(mockedNavigate).toHaveBeenCalledWith('/planner');
  });

  it('3. type icons render according to notification type', () => {
    const pagedResponse: PageResponse<NotificationDto> = {
      content: sampleNotifications,
      totalElements: 4,
      totalPages: 1,
      size: 20,
      page: 0,
    };

    vi.spyOn(queryHooks, 'useNotifications').mockReturnValue({
      data: pagedResponse,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 3,
      isLoading: false,
    } as any);

    renderComponent();

    expect(screen.getByTestId('icon-achievement')).toBeInTheDocument();
    expect(screen.getByTestId('icon-streak')).toBeInTheDocument();
    expect(screen.getByTestId('icon-revision')).toBeInTheDocument();
    expect(screen.getByTestId('icon-plan')).toBeInTheDocument();
  });

  it('4. read-all button fires mutation', () => {
    const pagedResponse: PageResponse<NotificationDto> = {
      content: sampleNotifications,
      totalElements: 4,
      totalPages: 1,
      size: 20,
      page: 0,
    };

    vi.spyOn(queryHooks, 'useNotifications').mockReturnValue({
      data: pagedResponse,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 3,
      isLoading: false,
    } as any);

    renderComponent();

    const markAllBtn = screen.getByTestId('mark-all-read-btn');
    expect(markAllBtn).toBeInTheDocument();

    fireEvent.click(markAllBtn);
    expect(mutateMarkAllRead).toHaveBeenCalledTimes(1);
  });

  it('5. empty state handles all notifications and unread filter correctly', () => {
    const emptyResponse: PageResponse<NotificationDto> = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 20,
      page: 0,
    };

    vi.spyOn(queryHooks, 'useNotifications').mockReturnValue({
      data: emptyResponse,
      isLoading: false,
    } as any);
    vi.spyOn(queryHooks, 'useUnreadNotificationCount').mockReturnValue({
      data: 0,
      isLoading: false,
    } as any);

    const { rerender } = renderComponent();

    expect(screen.getByTestId('notifications-empty')).toBeInTheDocument();
    expect(screen.getByText('No notifications yet')).toBeInTheDocument();
    expect(screen.queryByTestId('mark-all-read-btn')).not.toBeInTheDocument();

    // Toggle unread filter
    fireEvent.click(screen.getByTestId('filter-unread-tab'));
    expect(screen.getByText('No unread notifications')).toBeInTheDocument();
  });
});
