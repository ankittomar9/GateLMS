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
