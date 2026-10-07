import React, { useState, useRef, useEffect } from 'react';
import { Bell, Trophy, Flame, RotateCcw, CalendarRange, CheckCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import {
  useUnreadNotificationCount,
  useTop10Notifications,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
} from '../../hooks/useQueries';
import { NotificationDto, NotificationType } from '../../api/types';

export const NotificationBell: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const { data: unreadCount = 0 } = useUnreadNotificationCount();
  const { data: notifications = [] } = useTop10Notifications();
  const markReadMutation = useMarkNotificationRead();
  const markAllReadMutation = useMarkAllNotificationsRead();

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  const handleItemClick = (notification: NotificationDto) => {
    if (!notification.readAt) {
      markReadMutation.mutate(notification.id);
    }
    if (notification.link) {
      setIsOpen(false);
      navigate(notification.link);
    }
  };

  const renderIcon = (type: NotificationType) => {
    switch (type) {
      case 'ACHIEVEMENT_EARNED':
        return <Trophy className="w-4 h-4 text-ds-accent shrink-0" />;
      case 'STREAK_AT_RISK':
        return <Flame className="w-4 h-4 text-ds-danger shrink-0" />;
      case 'REVISION_DUE':
        return <RotateCcw className="w-4 h-4 text-ds-warning shrink-0" />;
      case 'PLAN_DUE':
        return <CalendarRange className="w-4 h-4 text-ds-info shrink-0" />;
      default:
        return <Bell className="w-4 h-4 text-ds-muted shrink-0" />;
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        aria-label="Notifications"
        data-testid="notification-bell-btn"
        onClick={() => setIsOpen(!isOpen)}
        className="p-1.5 rounded-ds bg-ds-surface hover:bg-ds-surface-hover text-ds-secondary hover:text-ds-primary border border-ds-border transition-colors relative"
      >
        <Bell className="w-4 h-4" />
        {unreadCount > 0 && (
          <span
            data-testid="notification-badge"
            className="absolute -top-1.5 -right-1.5 min-w-[18px] h-[18px] px-1 bg-ds-accent text-ds-accent-contrast text-[10px] font-bold rounded-full flex items-center justify-center shadow-sm"
          >
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          data-testid="notification-dropdown-panel"
          className="absolute right-0 mt-2 w-80 sm:w-96 bg-ds-surface border border-ds-border rounded-ds shadow-xl z-50 overflow-hidden"
        >
          {/* Header */}
          <div className="p-3 border-b border-ds-border flex items-center justify-between bg-ds-surface-raised">
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-ds-primary">Notifications</span>
              {unreadCount > 0 && (
                <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-ds-accent/15 text-ds-accent font-semibold font-mono">
                  {unreadCount} unread
                </span>
              )}
            </div>
            {unreadCount > 0 && (
              <button
                type="button"
                data-testid="mark-all-read-btn"
                onClick={() => markAllReadMutation.mutate()}
                disabled={markAllReadMutation.isPending}
                className="flex items-center gap-1 text-[11px] text-ds-muted hover:text-ds-primary transition-colors font-medium"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                <span>Mark all read</span>
              </button>
            )}
          </div>

          {/* List */}
          <div className="max-h-80 overflow-y-auto divide-y divide-ds-border">
            {notifications.length === 0 ? (
              <div
                data-testid="notifications-empty"
                className="py-8 text-center text-xs text-ds-muted"
              >
                No notifications yet
              </div>
            ) : (
              notifications.map((n) => {
                const isUnread = !n.readAt;
                return (
                  <div
                    key={n.id}
                    data-testid={`notification-item-${n.id}`}
                    onClick={() => handleItemClick(n)}
                    className={`p-3 flex items-start gap-2.5 hover:bg-ds-surface-hover cursor-pointer transition-colors ${
                      isUnread ? 'bg-ds-surface-raised/40' : 'opacity-85'
                    }`}
                  >
                    <div className="mt-0.5">{renderIcon(n.type)}</div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1 mb-0.5">
                        <h4
                          className={`text-xs truncate ${
                            isUnread ? 'font-bold text-ds-primary' : 'font-medium text-ds-secondary'
                          }`}
                        >
                          {n.title}
                        </h4>
                        {isUnread && (
                          <span className="w-1.5 h-1.5 rounded-full bg-ds-accent shrink-0" />
                        )}
                      </div>
                      {n.body && (
                        <p className="text-[11px] text-ds-muted line-clamp-2 leading-snug">
                          {n.body}
                        </p>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Footer */}
          <div className="p-2.5 border-t border-ds-border bg-ds-surface-raised text-center">
            <button
              type="button"
              data-testid="view-all-notifications-link"
              onClick={() => {
                setIsOpen(false);
                navigate('/notifications');
              }}
              className="text-xs text-ds-accent hover:text-ds-accent-hover font-semibold transition-colors"
            >
              View all notifications
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
