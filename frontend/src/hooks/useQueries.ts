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
