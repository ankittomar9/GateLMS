import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  Trophy,
  Flame,
  RotateCcw,
  CalendarRange,
  CheckCheck,
  Check,
  ArrowRight,
  Filter,
} from 'lucide-react';
import {
  useNotifications,
  useUnreadNotificationCount,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
} from '../hooks/useQueries';
import { NotificationDto, NotificationType } from '../api/types';

export const NotificationsPage: React.FC = () => {
  const navigate = useNavigate();
  const [filterUnread, setFilterUnread] = useState(false);
  const [page, setPage] = useState(0);

  const { data: pageData, isLoading } = useNotifications(filterUnread, page, 20);
  const { data: unreadCount = 0 } = useUnreadNotificationCount();
  const markReadMutation = useMarkNotificationRead();
  const markAllReadMutation = useMarkAllNotificationsRead();

  const notifications = pageData?.content || [];
  const totalPages = pageData?.totalPages || 1;

  const formatTimeAgo = (dateStr?: string) => {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      const now = new Date();
      const diffSec = Math.floor((now.getTime() - date.getTime()) / 1000);
      if (diffSec < 60) return 'just now';
      const diffMin = Math.floor(diffSec / 60);
      if (diffMin < 60) return `${diffMin}m ago`;
      const diffHour = Math.floor(diffMin / 60);
      if (diffHour < 24) return `${diffHour}h ago`;
      const diffDays = Math.floor(diffHour / 24);
      if (diffDays < 7) return `${diffDays}d ago`;
      return date.toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  const renderIcon = (type: NotificationType) => {
    switch (type) {
      case 'ACHIEVEMENT_EARNED':
        return <Trophy data-testid="icon-achievement" className="w-5 h-5 text-ds-accent shrink-0" />;
      case 'STREAK_AT_RISK':
        return <Flame data-testid="icon-streak" className="w-5 h-5 text-ds-danger shrink-0" />;
      case 'REVISION_DUE':
        return <RotateCcw data-testid="icon-revision" className="w-5 h-5 text-ds-warning shrink-0" />;
      case 'PLAN_DUE':
        return <CalendarRange data-testid="icon-plan" className="w-5 h-5 text-ds-info shrink-0" />;
      default:
        return <Bell data-testid="icon-default" className="w-5 h-5 text-ds-muted shrink-0" />;
    }
  };

  const handleItemClick = (notification: NotificationDto) => {
    if (!notification.readAt) {
      markReadMutation.mutate(notification.id);
    }
    if (notification.link) {
      navigate(notification.link);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-12" data-testid="notifications-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-ds-border">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold tracking-tight text-ds-primary">
              Notification Center
            </h1>
            {unreadCount > 0 && (
              <span
                data-testid="unread-count-badge"
                className="text-xs px-2 py-0.5 rounded-full bg-ds-accent/15 border border-ds-accent/30 text-ds-accent font-semibold font-mono"
              >
                {unreadCount} unread
              </span>
            )}
          </div>
          <p className="text-xs text-ds-muted leading-relaxed">
            Real-time updates on active study streaks, due revisions, daily study targets, and earned achievements.
          </p>
        </div>

        {unreadCount > 0 && (
          <button
            type="button"
            data-testid="mark-all-read-btn"
            onClick={() => markAllReadMutation.mutate()}
            disabled={markAllReadMutation.isPending}
            className="self-start sm:self-auto flex items-center gap-1.5 px-3 py-1.5 rounded-ds bg-ds-surface hover:bg-ds-surface-hover text-ds-secondary hover:text-ds-primary border border-ds-border text-xs font-semibold transition-all shadow-sm"
          >
            <CheckCheck className="w-4 h-4 text-ds-accent" />
            <span>Mark all as read</span>
          </button>
        )}
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2 p-1 bg-ds-surface-raised rounded-ds border border-ds-border">
          <button
            type="button"
            data-testid="filter-all-tab"
            onClick={() => {
              setFilterUnread(false);
              setPage(0);
            }}
            className={`px-3 py-1 rounded-ds text-xs font-medium transition-all ${
              !filterUnread
                ? 'bg-ds-surface text-ds-primary shadow-sm font-semibold'
                : 'text-ds-muted hover:text-ds-primary'
            }`}
          >
            All Notifications
          </button>
          <button
            type="button"
            data-testid="filter-unread-tab"
            onClick={() => {
              setFilterUnread(true);
              setPage(0);
            }}
            className={`px-3 py-1 rounded-ds text-xs font-medium transition-all ${
              filterUnread
                ? 'bg-ds-surface text-ds-primary shadow-sm font-semibold'
                : 'text-ds-muted hover:text-ds-primary'
            }`}
          >
            Unread Only {unreadCount > 0 && `(${unreadCount})`}
          </button>
        </div>

        <span className="text-[11px] text-ds-muted font-mono hidden sm:inline">
          Showing {notifications.length} of {pageData?.totalElements ?? notifications.length}
        </span>
      </div>

      {/* Notifications List */}
      <div
        data-testid="notifications-list"
        className="bg-ds-surface border border-ds-border rounded-ds divide-y divide-ds-border shadow-sm overflow-hidden"
      >
        {isLoading ? (
          <div data-testid="notifications-loading" className="p-8 text-center space-y-3">
            <div className="w-6 h-6 border-2 border-ds-accent border-t-transparent rounded-full animate-spin mx-auto" />
            <p className="text-xs text-ds-muted">Loading notifications...</p>
          </div>
        ) : notifications.length === 0 ? (
          <div
            data-testid="notifications-empty"
            className="p-12 text-center space-y-3"
          >
            <div className="w-12 h-12 rounded-full bg-ds-surface-raised border border-ds-border flex items-center justify-center mx-auto text-ds-muted">
              <Bell className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-ds-primary">
              {filterUnread ? 'No unread notifications' : 'No notifications yet'}
            </h3>
            <p className="text-xs text-ds-muted max-w-sm mx-auto">
              {filterUnread
                ? 'All caught up! Switch to "All Notifications" to review past events.'
                : 'Notifications will appear here as you solve questions, maintain your study streak, and unlock achievements.'}
            </p>
          </div>
        ) : (
          notifications.map((notification) => {
            const isUnread = !notification.readAt;
            return (
              <div
                key={notification.id}
                data-testid={`notification-item-${notification.id}`}
                onClick={() => handleItemClick(notification)}
                className={`p-4 flex items-start gap-4 hover:bg-ds-surface-hover cursor-pointer transition-colors ${
                  isUnread ? 'bg-ds-surface-raised/40' : 'opacity-85'
                }`}
              >
                {/* Type Icon Container */}
                <div className="p-2 rounded-ds bg-ds-surface-raised border border-ds-border shrink-0 mt-0.5">
                  {renderIcon(notification.type)}
                </div>

                {/* Content */}
                <div className="flex-1 min-w-0 space-y-1">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <h4
                        data-testid={`notification-title-${notification.id}`}
                        className={`text-sm truncate ${
                          isUnread ? 'font-bold text-ds-primary' : 'font-semibold text-ds-secondary'
                        }`}
                      >
                        {notification.title}
                      </h4>
                      {isUnread && (
                        <span
                          data-testid={`notification-unread-dot-${notification.id}`}
                          className="w-2 h-2 rounded-full bg-ds-accent shrink-0"
                        />
                      )}
                    </div>
                    <span
                      data-testid={`notification-time-${notification.id}`}
                      className="text-[11px] text-ds-muted font-mono shrink-0"
                    >
                      {formatTimeAgo(notification.createdAt)}
                    </span>
                  </div>

                  {notification.body && (
                    <p
                      data-testid={`notification-body-${notification.id}`}
                      className="text-xs text-ds-muted leading-relaxed"
                    >
                      {notification.body}
                    </p>
                  )}

                  {notification.link && (
                    <div className="pt-1.5 flex items-center gap-2">
                      <button
                        type="button"
                        data-testid={`notification-link-${notification.id}`}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleItemClick(notification);
                        }}
                        className="inline-flex items-center gap-1 text-xs text-ds-accent hover:text-ds-accent-hover font-semibold transition-colors"
                      >
                        <span>View details</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>

                {/* Mark as read button */}
                {isUnread && (
                  <button
                    type="button"
                    data-testid={`mark-read-btn-${notification.id}`}
                    title="Mark as read"
                    onClick={(e) => {
                      e.stopPropagation();
                      markReadMutation.mutate(notification.id);
                    }}
                    className="p-1.5 rounded-ds text-ds-muted hover:text-ds-primary hover:bg-ds-surface-raised transition-colors shrink-0"
                  >
                    <Check className="w-4 h-4" />
                  </button>
                )}
              </div>
            );
          })
        )}
      </div>

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="px-3 py-1.5 rounded-ds border border-ds-border text-xs font-semibold text-ds-secondary disabled:opacity-40 disabled:cursor-not-allowed hover:bg-ds-surface-hover transition-colors"
          >
            Previous
          </button>
          <span className="text-xs text-ds-muted font-mono">
            Page {page + 1} of {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="px-3 py-1.5 rounded-ds border border-ds-border text-xs font-semibold text-ds-secondary disabled:opacity-40 disabled:cursor-not-allowed hover:bg-ds-surface-hover transition-colors"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};
