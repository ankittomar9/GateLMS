import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/apiClient';
import {
  SubjectSummary,
  TopicItem,
  TopicMasteryUpdate,
  QuestionSummary,
  QuestionDetail,
  QuestionFilterParams,
  PageResponse,
  StatsSummary,
  Pass1Status,
  Pass2Status,
  CurationStatus,
  CurationStatsResponse,
  DistributionStatsResponse,
  PracticeSessionCreateRequest,
  PracticeSessionResponse,
  PracticeAnswerSubmission,
  PracticeVerdictResponse,
  PracticeFinishResponse,
  MistakeCreateRequest,
  MistakeReviewRequest,
  MistakeDto,
  TopicMasteryStatsDto,
  CreateMockTemplateRequest,
  MockTemplateSummaryDto,
  StartMockAttemptResponse,
  UpsertAnswerRequest,
  UpsertAnswerResponse,
  SubmitMockAttemptRequest,
  SubmitMockAttemptResponse,
  MockResultResponse,
  MockAttemptHistoryDto,
  MockTrendDto,
  QuestionUpsertDto,
  QuestionValidationResultDto,
} from '../api/types';

export function useSubjects() {
  return useQuery({
    queryKey: ['subjects'],
    queryFn: () => apiClient.get<SubjectSummary[]>('/subjects'),
  });
}

export function useTopics(subjectId?: string, subjectSlug?: string) {
  return useQuery({
    queryKey: ['topics', subjectId, subjectSlug],
    queryFn: () =>
      apiClient.get<TopicItem[]>('/topics', {
        params: {
          ...(subjectId ? { subjectId } : {}),
          ...(subjectSlug ? { subjectSlug } : {}),
        },
      }),
    enabled: Boolean(subjectId || subjectSlug),
  });
}

export function useUpdateTopicMastery() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ topicId, update }: { topicId: string; update: TopicMasteryUpdate }) =>
      apiClient.patch<TopicItem>(`/topics/${topicId}/mastery`, update),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['topics'] });
      queryClient.invalidateQueries({ queryKey: ['stats-summary'] });
    },
  });
}

export function useQuestions(filters: QuestionFilterParams) {
  return useQuery({
    queryKey: ['questions', filters],
    queryFn: () => apiClient.get<PageResponse<QuestionSummary>>('/questions', { params: filters }),
  });
}

export function useQuestion(id?: string) {
  return useQuery({
    queryKey: ['question', id],
    queryFn: () => apiClient.get<QuestionDetail>(`/questions/${id}`),
    enabled: Boolean(id),
  });
}

export function useUpdatePass1Status() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: Pass1Status }) =>
      apiClient.patch<QuestionDetail>(`/questions/${id}/pass1`, null, { params: { status } }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['question', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['questions'] });
      queryClient.invalidateQueries({ queryKey: ['stats-summary'] });
    },
  });
}

export function useUpdatePass2Status() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: Pass2Status }) =>
      apiClient.patch<QuestionDetail>(`/questions/${id}/pass2`, null, { params: { status } }),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['question', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['questions'] });
      queryClient.invalidateQueries({ queryKey: ['stats-summary'] });
    },
  });
}

export function useToggleBookmark() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiClient.post<QuestionDetail>(`/questions/${id}/bookmark`),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['question', id] });
      queryClient.invalidateQueries({ queryKey: ['questions'] });
    },
  });
}

export function useStatsSummary() {
  return useQuery({
    queryKey: ['stats-summary'],
    queryFn: () => apiClient.get<StatsSummary>('/stats/summary'),
  });
}

export function useUpdateCurationStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status, note }: { id: string; status: CurationStatus; note?: string }) =>
      apiClient.patch<QuestionDetail>(`/questions/${id}/curation`, { status, note }),
    onMutate: async ({ id, status, note }) => {
      await queryClient.cancelQueries({ queryKey: ['questions'] });
      const previousQuestions = queryClient.getQueriesData({ queryKey: ['questions'] });

      queryClient.setQueriesData({ queryKey: ['questions'] }, (old: any) => {
        if (!old || !old.content) return old;
        return {
          ...old,
          content: old.content.map((q: any) =>
            q.id === id ? { ...q, curationStatus: status, curationNote: note ?? q.curationNote } : q
          ),
        };
      });

      return { previousQuestions };
    },
    onError: (_err, _variables, context: any) => {
      if (context?.previousQuestions) {
        context.previousQuestions.forEach(([queryKey, data]: [any, any]) => {
          queryClient.setQueryData(queryKey, data);
        });
      }
    },
    onSettled: (_data, _error, variables) => {
      queryClient.invalidateQueries({ queryKey: ['question', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['questions'] });
      queryClient.invalidateQueries({ queryKey: ['curation-stats'] });
      queryClient.invalidateQueries({ queryKey: ['distribution-stats'] });
    },
  });
}

export function useCurationStats() {
  return useQuery({
    queryKey: ['curation-stats'],
    queryFn: () => apiClient.get<CurationStatsResponse>('/stats/curation'),
  });
}

export function useDistributionStats() {
  return useQuery({
    queryKey: ['distribution-stats'],
    queryFn: () => apiClient.get<DistributionStatsResponse>('/stats/distribution'),
  });
}

export function useCreatePracticeSession() {
  return useMutation({
    mutationFn: (data: PracticeSessionCreateRequest) =>
      apiClient.post<PracticeSessionResponse>('/practice/sessions', data),
  });
}

export function useSubmitPracticeAnswer(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PracticeAnswerSubmission) =>
      apiClient.post<PracticeVerdictResponse>(`/practice/sessions/${attemptId}/answers`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mastery-stats'] });
    },
  });
}

export function useSkipPracticeQuestion(attemptId: string) {
  return useMutation({
    mutationFn: (questionId: string) =>
      apiClient.post<void>(`/practice/sessions/${attemptId}/skip`, { questionId }),
  });
}

export function useRevealPracticeQuestion(attemptId: string) {
  return useMutation({
    mutationFn: (questionId: string) =>
      apiClient.post<PracticeVerdictResponse>(`/practice/sessions/${attemptId}/reveal`, { questionId }),
  });
}

export function useFinishPracticeSession(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      apiClient.post<PracticeFinishResponse>(`/practice/sessions/${attemptId}/finish`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mastery-stats'] });
      queryClient.invalidateQueries({ queryKey: ['stats-summary'] });
    },
  });
}

export function useMistakes(params?: { resolved?: boolean; due?: boolean; page?: number; size?: number }) {
  return useQuery({
    queryKey: ['mistakes', params],
    queryFn: () =>
      apiClient.get<PageResponse<MistakeDto>>('/mistakes', { params }),
  });
}

export function useCreateMistake() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: MistakeCreateRequest) =>
      apiClient.post<MistakeDto>('/mistakes', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mistakes'] });
      queryClient.invalidateQueries({ queryKey: ['mastery-stats'] });
    },
  });
}

export function useReviewMistake() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: MistakeReviewRequest }) =>
      apiClient.patch<MistakeDto>(`/mistakes/${id}/review`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mistakes'] });
      queryClient.invalidateQueries({ queryKey: ['mastery-stats'] });
    },
  });
}

export function useTopicMasteryStats() {
  return useQuery({
    queryKey: ['mastery-stats'],
    queryFn: () => apiClient.get<TopicMasteryStatsDto[]>('/stats/mastery'),
  });
}

export function useMockTemplates() {
  return useQuery({
    queryKey: ['mock-templates'],
    queryFn: () => apiClient.get<MockTemplateSummaryDto[]>('/mocks'),
  });
}

export function useMockTemplate(id: string) {
  return useQuery({
    queryKey: ['mock-template', id],
    queryFn: () => apiClient.get<MockTemplateSummaryDto>(`/mocks/${id}`),
    enabled: Boolean(id),
  });
}

export function useCreateMockTemplate() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateMockTemplateRequest) =>
      apiClient.post<MockTemplateSummaryDto>('/mocks', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mock-templates'] });
    },
  });
}

export function useStartMockAttempt(mockId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () =>
      apiClient.post<StartMockAttemptResponse>(`/mocks/${mockId}/attempts`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mock-attempts', mockId] });
    },
  });
}

export function useUpsertAttemptAnswer(attemptId: string) {
  return useMutation({
    mutationFn: ({ questionId, data }: { questionId: string; data: UpsertAnswerRequest }) =>
      apiClient.patch<UpsertAnswerResponse>(`/attempts/${attemptId}/answers/${questionId}`, data),
  });
}

export function useSubmitMockAttempt(attemptId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data?: SubmitMockAttemptRequest) =>
      apiClient.post<SubmitMockAttemptResponse>(`/attempts/${attemptId}/submit`, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mock-result', attemptId] });
      queryClient.invalidateQueries({ queryKey: ['mock-trend'] });
    },
  });
}

export function useMockResult(attemptId: string) {
  return useQuery({
    queryKey: ['mock-result', attemptId],
    queryFn: () => apiClient.get<MockResultResponse>(`/attempts/${attemptId}/result`),
    enabled: Boolean(attemptId),
  });
}

export function useMockAttemptHistory(mockId: string) {
  return useQuery({
    queryKey: ['mock-attempts', mockId],
    queryFn: () => apiClient.get<MockAttemptHistoryDto[]>(`/mocks/${mockId}/attempts`),
    enabled: Boolean(mockId),
  });
}

export function useMockTrend() {
  return useQuery({
    queryKey: ['mock-trend'],
    queryFn: () => apiClient.get<MockTrendDto[]>('/stats/mock-trend'),
  });
}

export function useValidateQuestion() {
  return useMutation({
    mutationFn: (dto: QuestionUpsertDto) =>
      apiClient.post<QuestionValidationResultDto>('/questions/validate', dto),
  });
}

export function useCreateQuestion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (dto: QuestionUpsertDto) =>
      apiClient.post<QuestionDetail>('/questions', dto),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['questions'] });
      queryClient.invalidateQueries({ queryKey: ['stats-summary'] });
    },
  });
}

export function useUpdateQuestion() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, dto }: { id: string; dto: QuestionUpsertDto }) =>
      apiClient.put<QuestionDetail>(`/questions/${id}`, dto),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['question', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['questions'] });
      queryClient.invalidateQueries({ queryKey: ['stats-summary'] });
    },
  });
}

