import { apiClient, ApiError } from './apiClient';

export interface NoteItem {
  id: string;
  title: string;
  filePath: string | null;
  originalFilename: string | null;
  contentType: string | null;
  fileSizeBytes: number | null;
  contentMd: string | null;
  subjectId: string | null;
  subjectName: string | null;
  topicId: string | null;
  topicName: string | null;
  isPinned: boolean;
  kind: 'TEXT' | 'FILE';
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface NotePageResponse {
  content: NoteItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

export interface CreateTextNoteRequest {
  title: string;
  subjectId?: string;
  topicId?: string;
  contentMd?: string;
}

export interface UpdateNoteRequest {
  title?: string;
  subjectId?: string;
  topicId?: string;
  contentMd?: string;
  isPinned?: boolean;
  version: number;
}

export interface NoteQueryParams {
  subjectId?: string;
  topicId?: string;
  kind?: string;
  q?: string;
  pinnedFirst?: boolean;
  page?: number;
  size?: number;
}

export const noteApi = {
  getNotes: async (params?: NoteQueryParams): Promise<NotePageResponse> => {
    return apiClient.get<NotePageResponse>('/notes', { params });
  },

  getNote: async (id: string): Promise<NoteItem> => {
    return apiClient.get<NoteItem>(`/notes/${id}`);
  },

  createTextNote: async (req: CreateTextNoteRequest): Promise<NoteItem> => {
    return apiClient.post<NoteItem>('/notes', req);
  },

  createFileNote: async (formData: FormData): Promise<NoteItem> => {
    const response = await fetch('/api/v1/notes/files', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      let errorData: any = null;
      try {
        errorData = await response.json();
      } catch {
        // ignore json parse error
      }
      const message = errorData?.detail || errorData?.title || `Upload failed with status ${response.status}`;
      throw new ApiError(response.status, message, errorData?.fieldErrors, errorData?.detail);
    }

    return response.json();
  },

  updateNote: async (id: string, req: UpdateNoteRequest): Promise<NoteItem> => {
    return apiClient.put<NoteItem>(`/notes/${id}`, req);
  },

  deleteNote: async (id: string): Promise<void> => {
    return apiClient.delete<void>(`/notes/${id}`);
  },

  togglePin: async (id: string): Promise<NoteItem> => {
    return apiClient.patch<NoteItem>(`/notes/${id}/pin`);
  },

  getFileUrl: (id: string): string => {
    return `/api/v1/notes/${id}/file`;
  },
};
