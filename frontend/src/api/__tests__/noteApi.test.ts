import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiError } from '../apiClient';
import { noteApi, NoteItem, NotePageResponse } from '../noteApi';

describe('Note API Layer', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('1. getNotes query shapes propagates subject, topic, kind, q, and pagination params', async () => {
    const mockResponse: NotePageResponse = {
      content: [
        {
          id: 'note-1',
          title: 'Virtual Memory & Paging',
          filePath: null,
          originalFilename: null,
          contentType: null,
          fileSizeBytes: null,
          contentMd: 'Paging details and formulas',
          subjectId: 'subj_os',
          subjectName: 'Operating Systems',
          topicId: 'top_paging',
          topicName: 'Paging',
          isPinned: true,
          kind: 'TEXT',
          version: 0,
          createdAt: '2026-10-07T08:00:00Z',
          updatedAt: '2026-10-07T08:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockResponse,
    });

    const result = await noteApi.getNotes({
      subjectId: 'subj_os',
      topicId: 'top_paging',
      kind: 'TEXT',
      q: 'Paging',
      pinnedFirst: true,
      page: 0,
      size: 20,
    });

    expect(result.content).toHaveLength(1);
    expect(result.content[0].title).toBe('Virtual Memory & Paging');
    expect(result.content[0].isPinned).toBe(true);

    const calledUrl = (globalThis.fetch as any).mock.calls[0][0];
    expect(calledUrl).toContain('/api/v1/notes?');
    expect(calledUrl).toContain('subjectId=subj_os');
    expect(calledUrl).toContain('topicId=top_paging');
    expect(calledUrl).toContain('kind=TEXT');
    expect(calledUrl).toContain('q=Paging');
  });

  it('2. createFileNote posts multipart FormData to files endpoint', async () => {
    const mockFileNote: NoteItem = {
      id: 'note-file-1',
      title: 'Operating Systems Textbook Chapter 8',
      filePath: 'note-file-1/uuid.pdf',
      originalFilename: 'silberschatz_ch8.pdf',
      contentType: 'application/pdf',
      fileSizeBytes: 2048500,
      contentMd: null,
      subjectId: 'subj_os',
      subjectName: 'Operating Systems',
      topicId: null,
      topicName: null,
      isPinned: false,
      kind: 'FILE',
      version: 0,
      createdAt: '2026-10-07T08:00:00Z',
      updatedAt: '2026-10-07T08:00:00Z',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => mockFileNote,
    });

    const formData = new FormData();
    formData.append('title', 'Operating Systems Textbook Chapter 8');
    formData.append('file', new Blob(['%PDF-1.4']), 'silberschatz_ch8.pdf');

    const result = await noteApi.createFileNote(formData);
    expect(result.id).toBe('note-file-1');
    expect(result.kind).toBe('FILE');
    expect(result.originalFilename).toBe('silberschatz_ch8.pdf');
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/notes/files',
      expect.objectContaining({
        method: 'POST',
        body: formData,
      })
    );
  });

  it('3. updateNote throws ApiError with status 409 on version conflict', async () => {
    const problemDetail = {
      type: 'https://gatelms.dev/errors/version-conflict',
      title: 'Conflict',
      status: 409,
      detail: 'Note was modified concurrently. Expected version 0, got 2',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 409,
      json: async () => problemDetail,
    });

    await expect(
      noteApi.updateNote('note-1', { title: 'Updated Title', version: 2 })
    ).rejects.toThrow(ApiError);

    try {
      await noteApi.updateNote('note-1', { title: 'Updated Title', version: 2 });
    } catch (err: any) {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(409);
      expect(err.message).toContain('Note was modified concurrently');
    }
  });

  it('4. togglePin fires PATCH to pin endpoint and returns updated note', async () => {
    const mockToggledNote: Partial<NoteItem> = {
      id: 'note-1',
      isPinned: true,
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockToggledNote,
    });

    const result = await noteApi.togglePin('note-1');
    expect(result.isPinned).toBe(true);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/notes/note-1/pin',
      expect.objectContaining({ method: 'PATCH' })
    );
  });
});
