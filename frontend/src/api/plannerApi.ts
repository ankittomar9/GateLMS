import { apiClient } from './apiClient';
import {
  GeneratePlanRequest,
  StudyPlanDetailDto,
  StudyPlanSummaryDto,
  TodayPlanResponse,
  PlanItemDto,
  UpdatePlanItemStatusRequest,
} from './types';

export const plannerApi = {
  generatePlan: async (req: GeneratePlanRequest): Promise<StudyPlanDetailDto> => {
    return apiClient.post<StudyPlanDetailDto>('/plans/generate', req);
  },

  getPlans: async (): Promise<StudyPlanSummaryDto[]> => {
    return apiClient.get<StudyPlanSummaryDto[]>('/plans');
  },

  getPlanById: async (id: string): Promise<StudyPlanDetailDto> => {
    return apiClient.get<StudyPlanDetailDto>(`/plans/${id}`);
  },

  getTodayPlan: async (): Promise<TodayPlanResponse> => {
    return apiClient.get<TodayPlanResponse>('/plans/today');
  },

  regeneratePlan: async (id: string): Promise<StudyPlanDetailDto> => {
    return apiClient.post<StudyPlanDetailDto>(`/plans/${id}/regenerate`);
  },

  updatePlanItemStatus: async (
    itemId: string,
    req: UpdatePlanItemStatusRequest
  ): Promise<PlanItemDto> => {
    return apiClient.patch<PlanItemDto>(`/plan-items/${itemId}`, req);
  },
};
