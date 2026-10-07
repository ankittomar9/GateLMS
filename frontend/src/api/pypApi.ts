import { apiClient } from './apiClient';
import { PypStatsResponse, MockTemplateSummaryDto } from './types';

export const pypApi = {
  getPypStats: async (): Promise<PypStatsResponse> => {
    return apiClient.get<PypStatsResponse>('/stats/pyp');
  },

  generatePypMock: async (year: number): Promise<MockTemplateSummaryDto> => {
    return apiClient.post<MockTemplateSummaryDto>(`/mocks/pyp/${year}`);
  },
};
