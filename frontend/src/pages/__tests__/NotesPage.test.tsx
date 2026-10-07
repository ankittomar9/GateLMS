import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NotesPage } from '../NotesPage';
import { noteApi, NoteItem, NotePageResponse } from '../../api/noteApi';
import { ApiError } from '../../api/apiClient';

// Mock MathText to render content with testable marker
vi.mock('../../components/MathText', () => ({
  MathText: ({ content }: { content: string }) => (
    <div data-testid="mock-mathtext">{content}</div>
  ),
}));

// Mock useSubjects and useTopics hooks
vi.mock('../../hooks/useQueries', () => ({
  useSubjects: () => ({
    data: [
      { id: 'subj_os', name: 'Operating Systems', slug: 'os', cluster: 'systems', avgWeightage: 10.0, priorityTier: 1 },
      { id: 'subj_algo', name: 'Algorithms', slug: 'algo', cluster: 'core', avgWeightage: 10.0, priorityTier: 1 },
    ],
  }),
  useTopics: () => ({
    data: [
      { id: 'top_paging', name: 'Virtual Memory & Paging', slug: 'paging' },
      { id: 'top_dp', name: 'Dynamic Programming', slug: 'dp' },
    ],
  }),
}));

const mockNotesList: NoteItem[] = [
  {
    id: 'note-pinned-1',
    title: 'Operating Systems Key Formulas',
    filePath: null,
    originalFilename: null,
    contentType: null,
    fileSizeBytes: null,
    contentMd: 'Important OS equations: $$EAT = h(t + m) + (1-h)(t + 2m)$$',
    subjectId: 'subj_os',
    subjectName: 'Operating Systems',
    topicId: 'top_paging',
    topicName: 'Virtual Memory & Paging',
    isPinned: true,
    kind: 'TEXT',
    version: 0,
    createdAt: '2026-10-07T08:00:00Z',
    updatedAt: '2026-10-07T08:00:00Z',
  },
  {
    id: 'note-unpinned-2',
    title: 'Silberschatz Chapter 8 Memory PDF',
    filePath: 'note-unpinned-2/uuid.pdf',
    originalFilename: 'silberschatz_ch8.pdf',
    contentType: 'application/pdf',
    fileSizeBytes: 1048576,
    contentMd: null,
    subjectId: 'subj_os',
    subjectName: 'Operating Systems',
    topicId: 'top_paging',
    topicName: 'Virtual Memory & Paging',
    isPinned: false,
    kind: 'FILE',
    version: 0,
    createdAt: '2026-10-07T08:30:00Z',
    updatedAt: '2026-10-07T08:30:00Z',
  },
];

describe('NotesPage Component Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('1. list + filters - renders notes and filter controls', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: mockNotesList,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    });

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('notes-filter-bar')).toBeDefined();
    });

    expect(screen.getByTestId('subject-filter-select')).toBeDefined();
    expect(screen.getByTestId('topic-filter-select')).toBeDefined();
    expect(screen.getByTestId('notes-search-input')).toBeDefined();
    expect(screen.getByText('Operating Systems Key Formulas')).toBeDefined();
    expect(screen.getByText('Silberschatz Chapter 8 Memory PDF')).toBeDefined();
  });

  it('2. pinned first - pinned notes render in pinned section', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: mockNotesList,
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    });

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('pinned-notes-section')).toBeDefined();
    });

    const pinnedSection = screen.getByTestId('pinned-notes-section');
    expect(pinnedSection.textContent).toContain('Operating Systems Key Formulas');

    const allSection = screen.getByTestId('all-notes-section');
    expect(allSection.textContent).toContain('Silberschatz Chapter 8 Memory PDF');
  });

  it('3. editor live preview renders KaTeX from content', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      isFirst: true,
      isLast: true,
    });

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('create-note-btn')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('create-note-btn'));
    expect(screen.getByTestId('note-editor-modal')).toBeDefined();

    const textarea = screen.getByTestId('editor-content-textarea');
    fireEvent.change(textarea, { target: { value: '$$EAT = h(t + m)$$ equation' } });

    // Wait for 300 ms debounce
    await waitFor(() => {
      const preview = screen.getByTestId('editor-math-preview');
      expect(preview.textContent).toContain('$$EAT = h(t + m)$$ equation');
    }, { timeout: 1000 });
  });

  it('4. upload flow posts multipart FormData to file endpoint', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      isFirst: true,
      isLast: true,
    });

    const createFileSpy = vi.spyOn(noteApi, 'createFileNote').mockResolvedValue(mockNotesList[1]);

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('upload-file-btn')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('upload-file-btn'));
    expect(screen.getByTestId('upload-modal')).toBeDefined();

    const titleInput = screen.getByTestId('upload-title-input');
    fireEvent.change(titleInput, { target: { value: 'Silberschatz Memory PDF' } });

    const fileInput = screen.getByTestId('file-picker-input');
    const fakeFile = new File(['%PDF-1.4'], 'test.pdf', { type: 'application/pdf' });
    Object.defineProperty(fileInput, 'files', {
      value: [fakeFile],
      configurable: true,
    });
    fireEvent.change(fileInput);

    const form = screen.getByTestId('upload-modal').querySelector('form')!;
    fireEvent.submit(form);

    await waitFor(() => {
      expect(createFileSpy).toHaveBeenCalledTimes(1);
    });
  });

  it('5. PDF card renders embed with API src when viewed', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: [mockNotesList[1]],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    });

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('view-note-btn')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('view-note-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('viewer-modal')).toBeDefined();
    });

    const embed = screen.getByTestId('pdf-embed');
    expect(embed).toBeDefined();
    expect(embed.getAttribute('src')).toBe('/api/v1/notes/note-unpinned-2/file');
    expect(embed.getAttribute('type')).toBe('application/pdf');
  });

  it('6. delete confirm triggers deletion API', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: [mockNotesList[0]],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    });

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const deleteSpy = vi.spyOn(noteApi, 'deleteNote').mockResolvedValue();

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('delete-note-btn')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('delete-note-btn'));

    await waitFor(() => {
      expect(deleteSpy).toHaveBeenCalledWith('note-pinned-1');
    });
  });

  it('7. 409 conflict banner renders when edit encounters version conflict', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: [mockNotesList[0]],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
      isFirst: true,
      isLast: true,
    });

    vi.spyOn(noteApi, 'updateNote').mockRejectedValue(
      new ApiError(409, 'Resource was modified concurrently. Please reload latest state.')
    );

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('edit-note-btn')).toBeDefined();
    });

    fireEvent.click(screen.getByTestId('edit-note-btn'));
    expect(screen.getByTestId('note-editor-modal')).toBeDefined();

    fireEvent.click(screen.getByTestId('save-note-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('conflict-banner')).toBeDefined();
    });

    expect(screen.getByText(/Resource was modified concurrently/)).toBeDefined();
  });

  it('8. empty state renders clean CTA when vault has no notes', async () => {
    vi.spyOn(noteApi, 'getNotes').mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      isFirst: true,
      isLast: true,
    });

    render(
      <MemoryRouter>
        <NotesPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('notes-empty-state')).toBeDefined();
    });

    expect(screen.getByText('No notes found in the vault')).toBeDefined();
    expect(screen.getByText('Write First Note')).toBeDefined();
    expect(screen.getByText('Upload PDF Reference')).toBeDefined();
  });
});
