import { apiClient } from './apiClient';
import {
  EngagementSummaryResponse,
  AchievementDto,
  UserGoalDto,
  UpdateUserGoalRequest,
  NotificationDto,
  UnreadCountResponse,
  MarkAllReadResponse,
  PageResponse,
} from './types';

export const fetchEngagementSummary = async (): Promise<EngagementSummaryResponse> => {
  return apiClient.get<EngagementSummaryResponse>('/engagement/summary');
};

export const fetchAchievements = async (): Promise<AchievementDto[]> => {
  return apiClient.get<AchievementDto[]>('/engagement/achievements');
};

export const fetchUserGoal = async (): Promise<UserGoalDto> => {
  return apiClient.get<UserGoalDto>('/engagement/goal');
};

export const updateUserGoal = async (payload: UpdateUserGoalRequest): Promise<UserGoalDto> => {
  return apiClient.patch<UserGoalDto>('/engagement/goal', payload);
};

export const fetchNotifications = async (
  unreadOnly = false,
  page = 0,
  size = 20
): Promise<PageResponse<NotificationDto>> => {
  return apiClient.get<PageResponse<NotificationDto>>('/notifications', {
    params: { unread: unreadOnly, page, size },
  });
};

export const fetchTop10Notifications = async (): Promise<NotificationDto[]> => {
  return apiClient.get<NotificationDto[]>('/notifications/top');
};

export const fetchUnreadNotificationCount = async (): Promise<number> => {
  const response = await apiClient.get<UnreadCountResponse>('/notifications/unread-count');
  return response.count;
};

export const markNotificationRead = async (id: string): Promise<NotificationDto> => {
  return apiClient.patch<NotificationDto>(`/notifications/${id}/read`);
};

export const markAllNotificationsRead = async (): Promise<MarkAllReadResponse> => {
  return apiClient.post<MarkAllReadResponse>('/notifications/read-all');
};

export const engagementApi = {
  fetchEngagementSummary,
  fetchAchievements,
  fetchUserGoal,
  updateUserGoal,
  fetchNotifications,
  fetchTop10Notifications,
  fetchUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
};
