import React, { useState, useEffect, useMemo } from 'react';
import { 
  FileText, 
  Upload, 
  Plus, 
  Pin, 
  Search, 
  Trash2, 
  Edit3, 
  ExternalLink, 
  BookOpen, 
  X, 
  Check, 
  AlertCircle, 
  Filter,
  FileCode,
  FileImage,
  Eye,
  NotebookPen
} from 'lucide-react';
import { noteApi, NoteItem, CreateTextNoteRequest, UpdateNoteRequest } from '../api/noteApi';
import { useSubjects, useTopics } from '../hooks/useQueries';
import { MathText } from '../components/MathText';

export const NotesPage: React.FC = () => {
  const [notes, setNotes] = useState<NoteItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [selectedSubject, setSelectedSubject] = useState<string>('');
  const [selectedTopic, setSelectedTopic] = useState<string>('');
  const [selectedKind, setSelectedKind] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Modals & Panels
  const [editorOpen, setEditorOpen] = useState<boolean>(false);
  const [editingNote, setEditingNote] = useState<NoteItem | null>(null);
  const [uploadModalOpen, setUploadModalOpen] = useState<boolean>(false);
  const [viewerNote, setViewerNote] = useState<NoteItem | null>(null);
  const [conflictError, setConflictError] = useState<string | null>(null);

  // Editor State
  const [editorTitle, setEditorTitle] = useState<string>('');
  const [editorSubject, setEditorSubject] = useState<string>('');
  const [editorTopic, setEditorTopic] = useState<string>('');
  const [editorContent, setEditorContent] = useState<string>('');
  const [debouncedContent, setDebouncedContent] = useState<string>('');

  // Upload State
  const [uploadTitle, setUploadTitle] = useState<string>('');
  const [uploadSubject, setUploadSubject] = useState<string>('');
  const [uploadTopic, setUploadTopic] = useState<string>('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState<boolean>(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  // Queries
  const { data: subjects = [] } = useSubjects();
  const { data: topics = [] } = useTopics(selectedSubject || editorSubject || uploadSubject);

  // Debounce editor preview (300 ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedContent(editorContent);
    }, 300);
    return () => clearTimeout(handler);
  }, [editorContent]);

  // Fetch notes
  const fetchNotes = async () => {
    setLoading(true);
    setError(null);
    try {
      const resp = await noteApi.getNotes({
        subjectId: selectedSubject || undefined,
        topicId: selectedTopic || undefined,
        kind: selectedKind === 'ALL' ? undefined : selectedKind,
        q: searchQuery.trim() || undefined,
        pinnedFirst: true,
      });
      setNotes(resp.content);
    } catch (err: any) {
      setError(err?.message || 'Failed to load notes');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotes();
  }, [selectedSubject, selectedTopic, selectedKind, searchQuery]);

  // Pinned vs Regular Notes
  const pinnedNotes = useMemo(() => notes.filter(n => n.isPinned), [notes]);
  const unpinnedNotes = useMemo(() => notes.filter(n => !n.isPinned), [notes]);

  // Pin Toggle
  const handleTogglePin = async (note: NoteItem) => {
    try {
      const updated = await noteApi.togglePin(note.id);
      setNotes(prev => prev.map(n => (n.id === updated.id ? updated : n)));
    } catch (err: any) {
      alert(err?.message || 'Failed to toggle pin');
    }
  };

  // Delete Note
  const handleDeleteNote = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this note?')) return;
    try {
      await noteApi.deleteNote(id);
      setNotes(prev => prev.filter(n => n.id !== id));
      if (viewerNote?.id === id) setViewerNote(null);
    } catch (err: any) {
      alert(err?.message || 'Failed to delete note');
    }
  };

  // Open Editor for Create
  const handleOpenCreateEditor = () => {
    setEditingNote(null);
    setEditorTitle('');
    setEditorSubject(selectedSubject || '');
    setEditorTopic(selectedTopic || '');
    setEditorContent('');
    setConflictError(null);
    setEditorOpen(true);
  };

  // Open Editor for Edit
  const handleOpenEditEditor = (note: NoteItem) => {
    setEditingNote(note);
    setEditorTitle(note.title);
    setEditorSubject(note.subjectId || '');
    setEditorTopic(note.topicId || '');
    setEditorContent(note.contentMd || '');
    setConflictError(null);
    setEditorOpen(true);
  };

  // Save Text Note
  const handleSaveEditor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editorTitle.trim() || !editorContent.trim()) return;

    try {
      if (editingNote) {
        const updateReq: UpdateNoteRequest = {
          title: editorTitle.trim(),
          subjectId: editorSubject || undefined,
          topicId: editorTopic || undefined,
          contentMd: editorContent.trim(),
          version: editingNote.version,
        };
        const updated = await noteApi.updateNote(editingNote.id, updateReq);
        setNotes(prev => prev.map(n => (n.id === updated.id ? updated : n)));
        if (viewerNote?.id === updated.id) setViewerNote(updated);
      } else {
        const createReq: CreateTextNoteRequest = {
          title: editorTitle.trim(),
          subjectId: editorSubject || undefined,
          topicId: editorTopic || undefined,
          contentMd: editorContent.trim(),
        };
        const created = await noteApi.createTextNote(createReq);
        setNotes(prev => [created, ...prev]);
      }
      setEditorOpen(false);
    } catch (err: any) {
      if (err?.status === 409) {
        setConflictError(err.message || 'Resource was modified concurrently. Please reload latest state.');
      } else {
        alert(err?.message || 'Failed to save note');
      }
    }
  };

  // Insert formatting snippet
  const insertSnippet = (prefix: string, suffix: string = '') => {
    setEditorContent(prev => prev + prefix + suffix);
  };

  // Handle File Upload
  const handleFileUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile || !uploadTitle.trim()) return;

    setUploading(true);
    setUploadError(null);
    try {
      const formData = new FormData();
      formData.append('title', uploadTitle.trim());
      formData.append('file', selectedFile);
      if (uploadSubject) formData.append('subjectId', uploadSubject);
      if (uploadTopic) formData.append('topicId', uploadTopic);

      const created = await noteApi.createFileNote(formData);
      setNotes(prev => [created, ...prev]);
      setUploadModalOpen(false);
      setSelectedFile(null);
      setUploadTitle('');
    } catch (err: any) {
      setUploadError(err?.message || 'File upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="space-y-6 max-w-7xl mx-auto p-4 md:p-6" data-testid="notes-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-ds-border pb-5">
        <div>
          <div className="flex items-center gap-2 text-ds-accent text-xs font-mono font-semibold uppercase tracking-wider">
            <NotebookPen className="w-4 h-4" />
            <span>GATE CSE Notes Vault</span>
          </div>
          <h1 className="text-2xl md:text-3xl font-extrabold text-ds-primary tracking-tight mt-1">
            Notes & Reference Library
          </h1>
          <p className="text-xs text-ds-secondary mt-1">
            Markdown notes with live KaTeX rendering, reference textbook PDFs, and topic-tagged study materials.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            type="button"
            onClick={handleOpenCreateEditor}
            className="flex items-center gap-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-4 py-2 rounded-ds text-xs font-bold transition-all shadow-sm"
            data-testid="create-note-btn"
          >
            <Plus className="w-4 h-4" />
            <span>New Note</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setUploadTitle('');
              setSelectedFile(null);
              setUploadError(null);
              setUploadModalOpen(true);
            }}
            className="flex items-center gap-2 bg-ds-surface hover:bg-ds-surface-hover text-ds-primary border border-ds-border px-4 py-2 rounded-ds text-xs font-semibold transition-all shadow-sm"
            data-testid="upload-file-btn"
          >
            <Upload className="w-4 h-4 text-ds-accent" />
            <span>Upload File</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div 
        className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-3 shadow-sm"
        data-testid="notes-filter-bar"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Search Query */}
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-2.5 text-ds-muted" />
            <input
              type="text"
              placeholder="Search notes & content..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="w-full bg-ds-surface-input border border-ds-border rounded-ds pl-9 pr-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent font-mono"
              data-testid="notes-search-input"
            />
          </div>

          {/* Subject Select */}
          <div>
            <select
              value={selectedSubject}
              onChange={e => {
                setSelectedSubject(e.target.value);
                setSelectedTopic('');
              }}
              className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
              data-testid="subject-filter-select"
            >
              <option value="">All Subjects</option>
              {subjects.map(s => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>

          {/* Topic Select */}
          <div>
            <select
              value={selectedTopic}
              onChange={e => setSelectedTopic(e.target.value)}
              disabled={!selectedSubject}
              className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent disabled:opacity-50"
              data-testid="topic-filter-select"
            >
              <option value="">All Topics</option>
              {topics.map(t => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </div>

          {/* Kind Tabs */}
          <div className="flex items-center gap-1 bg-ds-surface-raised p-1 rounded-ds border border-ds-border text-xs font-mono">
            {['ALL', 'TEXT', 'FILE'].map(k => (
              <button
                key={k}
                type="button"
                onClick={() => setSelectedKind(k)}
                className={`flex-1 py-1 rounded-ds text-center transition-all ${
                  selectedKind === k
                    ? 'bg-ds-accent text-ds-accent-contrast font-bold'
                    : 'text-ds-muted hover:text-ds-primary'
                }`}
                data-testid={`kind-filter-${k.toLowerCase()}`}
              >
                {k === 'ALL' ? 'All' : k === 'TEXT' ? 'Markdown' : 'Files'}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Loading & Error States */}
      {loading && (
        <div className="py-16 text-center text-ds-muted font-mono text-xs animate-pulse">
          Loading notes vault...
        </div>
      )}

      {error && (
        <div className="p-4 rounded-ds bg-ds-danger/10 border border-ds-danger text-ds-danger text-xs font-mono">
          {error}
        </div>
      )}

      {/* Empty State */}
      {!loading && notes.length === 0 && (
        <div 
          className="bg-ds-surface border border-ds-border rounded-ds p-12 text-center space-y-4 max-w-md mx-auto shadow-sm"
          data-testid="notes-empty-state"
        >
          <div className="w-12 h-12 rounded-full bg-ds-accent-soft text-ds-accent mx-auto flex items-center justify-center">
            <BookOpen className="w-6 h-6" />
          </div>
          <div className="space-y-1">
            <h3 className="text-base font-bold text-ds-primary">No notes found in the vault</h3>
            <p className="text-xs text-ds-secondary">
              Write markdown study notes or upload textbook chapters and cheatsheets to organize your GATE preparation.
            </p>
          </div>
          <div className="flex items-center justify-center gap-3 pt-2">
            <button
              type="button"
              onClick={handleOpenCreateEditor}
              className="bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-4 py-2 rounded-ds text-xs font-bold transition-all shadow-sm"
            >
              Write First Note
            </button>
            <button
              type="button"
              onClick={() => setUploadModalOpen(true)}
              className="bg-ds-surface hover:bg-ds-surface-hover text-ds-primary border border-ds-border px-4 py-2 rounded-ds text-xs font-semibold transition-all shadow-sm"
            >
              Upload PDF Reference
            </button>
          </div>
        </div>
      )}

      {/* Notes Grid */}
      {!loading && notes.length > 0 && (
        <div className="space-y-8">
          {/* Pinned Notes Section */}
          {pinnedNotes.length > 0 && (
            <div className="space-y-3" data-testid="pinned-notes-section">
              <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-ds-accent font-mono">
                <Pin className="w-3.5 h-3.5 fill-current" />
                <span>Pinned Notes ({pinnedNotes.length})</span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {pinnedNotes.map(note => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    onTogglePin={handleTogglePin}
                    onDelete={handleDeleteNote}
                    onEdit={handleOpenEditEditor}
                    onView={() => setViewerNote(note)}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Unpinned Notes Section */}
          {unpinnedNotes.length > 0 && (
            <div className="space-y-3" data-testid="all-notes-section">
              <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-ds-muted font-mono">
                <span>{pinnedNotes.length > 0 ? 'Other Notes' : 'All Notes'} ({unpinnedNotes.length})</span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {unpinnedNotes.map(note => (
                  <NoteCard
                    key={note.id}
                    note={note}
                    onTogglePin={handleTogglePin}
                    onDelete={handleDeleteNote}
                    onEdit={handleOpenEditEditor}
                    onView={() => setViewerNote(note)}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Text Note Editor Modal / Drawer */}
      {editorOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div 
            className="bg-ds-surface border border-ds-border rounded-ds w-full max-w-4xl max-h-[90vh] flex flex-col shadow-xl overflow-hidden"
            data-testid="note-editor-modal"
          >
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-ds-border flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-ds-primary">
                  {editingNote ? 'Edit Study Note' : 'Create New Markdown Note'}
                </h2>
                <span className="text-[11px] text-ds-muted font-mono">
                  Full KaTeX math support ($...$ for inline, $$...$$ for display math)
                </span>
              </div>
              <button
                type="button"
                onClick={() => setEditorOpen(false)}
                className="text-ds-muted hover:text-ds-primary p-1 rounded-ds"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* 409 Conflict Banner */}
            {conflictError && (
              <div 
                className="mx-6 mt-4 p-3 rounded-ds bg-ds-warning/15 border border-ds-warning/40 text-ds-warning text-xs flex items-center justify-between"
                data-testid="conflict-banner"
              >
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <span>{conflictError}</span>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    fetchNotes();
                    setEditorOpen(false);
                  }}
                  className="underline font-bold"
                >
                  Reload Latest
                </button>
              </div>
            )}

            {/* Form & Editor Body */}
            <form onSubmit={handleSaveEditor} className="flex-1 overflow-y-auto p-6 space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-1">
                  <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Subject</label>
                  <select
                    value={editorSubject}
                    onChange={e => {
                      setEditorSubject(e.target.value);
                      setEditorTopic('');
                    }}
                    className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                  >
                    <option value="">No Subject</option>
                    {subjects.map(s => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>

                <div className="sm:col-span-1">
                  <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Topic</label>
                  <select
                    value={editorTopic}
                    onChange={e => setEditorTopic(e.target.value)}
                    disabled={!editorSubject}
                    className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent disabled:opacity-50"
                  >
                    <option value="">No Topic</option>
                    {topics.map(t => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                </div>

                <div className="sm:col-span-1">
                  <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Note Title</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Master Theorem Notes"
                    value={editorTitle}
                    onChange={e => setEditorTitle(e.target.value)}
                    className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                    data-testid="editor-title-input"
                  />
                </div>
              </div>

              {/* Toolbar */}
              <div className="flex flex-wrap items-center gap-1.5 p-2 bg-ds-surface-raised border border-ds-border rounded-ds text-xs font-mono">
                <button
                  type="button"
                  onClick={() => insertSnippet('**', '**')}
                  className="px-2 py-1 rounded bg-ds-surface hover:bg-ds-surface-hover border border-ds-border text-ds-primary"
                  title="Bold"
                >
                  B
                </button>
                <button
                  type="button"
                  onClick={() => insertSnippet('`', '`')}
                  className="px-2 py-1 rounded bg-ds-surface hover:bg-ds-surface-hover border border-ds-border text-ds-primary"
                  title="Inline Code"
                >
                  Code
                </button>
                <button
                  type="button"
                  onClick={() => insertSnippet('$', '$')}
                  className="px-2 py-1 rounded bg-ds-surface hover:bg-ds-surface-hover border border-ds-border text-ds-accent font-bold"
                  title="Inline Math: $formula$"
                >
                  $x$
                </button>
                <button
                  type="button"
                  onClick={() => insertSnippet('\n$$\n', '\n$$\n')}
                  className="px-2 py-1 rounded bg-ds-surface hover:bg-ds-surface-hover border border-ds-border text-ds-accent font-bold"
                  title="Display Math Block: $$formula$$"
                >
                  $$Block$$
                </button>
                <button
                  type="button"
                  onClick={() => insertSnippet('\n- ')}
                  className="px-2 py-1 rounded bg-ds-surface hover:bg-ds-surface-hover border border-ds-border text-ds-primary"
                  title="Bullet List"
                >
                  • List
                </button>
              </div>

              {/* Split Editor & Live Preview */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 h-72">
                <textarea
                  required
                  placeholder="Write note contents in Markdown with KaTeX math..."
                  value={editorContent}
                  onChange={e => setEditorContent(e.target.value)}
                  className="w-full h-full bg-ds-surface-input border border-ds-border rounded-ds p-3 text-xs text-ds-primary font-mono focus:outline-none focus:border-ds-accent resize-none leading-relaxed"
                  data-testid="editor-content-textarea"
                />

                <div 
                  className="w-full h-full bg-ds-surface-raised border border-ds-border rounded-ds p-3 overflow-y-auto text-xs space-y-2"
                  data-testid="editor-math-preview"
                >
                  <div className="text-[10px] font-mono text-ds-muted uppercase tracking-wider border-b border-ds-border pb-1">
                    Live KaTeX Preview
                  </div>
                  {debouncedContent ? (
                    <MathText content={debouncedContent} />
                  ) : (
                    <span className="text-ds-muted italic">Preview will appear here...</span>
                  )}
                </div>
              </div>

              {/* Form Actions */}
              <div className="flex items-center justify-end gap-3 pt-3 border-t border-ds-border">
                <button
                  type="button"
                  onClick={() => setEditorOpen(false)}
                  className="px-4 py-2 rounded-ds border border-ds-border text-xs text-ds-secondary hover:text-ds-primary hover:bg-ds-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-ds bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-bold shadow-sm"
                  data-testid="save-note-btn"
                >
                  Save Note
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* File Upload Modal */}
      {uploadModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div 
            className="bg-ds-surface border border-ds-border rounded-ds w-full max-w-lg shadow-xl overflow-hidden"
            data-testid="upload-modal"
          >
            <div className="px-6 py-4 border-b border-ds-border flex items-center justify-between">
              <h2 className="text-base font-bold text-ds-primary">Upload Reference Material</h2>
              <button
                type="button"
                onClick={() => setUploadModalOpen(false)}
                className="text-ds-muted hover:text-ds-primary p-1 rounded-ds"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {uploadError && (
              <div className="mx-6 mt-4 p-3 rounded-ds bg-ds-danger/10 border border-ds-danger text-ds-danger text-xs font-mono">
                {uploadError}
              </div>
            )}

            <form onSubmit={handleFileUpload} className="p-6 space-y-4">
              <div>
                <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Material Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Silberschatz Chapter 8 Paging"
                  value={uploadTitle}
                  onChange={e => setUploadTitle(e.target.value)}
                  className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                  data-testid="upload-title-input"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Subject</label>
                  <select
                    value={uploadSubject}
                    onChange={e => {
                      setUploadSubject(e.target.value);
                      setUploadTopic('');
                    }}
                    className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                  >
                    <option value="">No Subject</option>
                    {subjects.map(s => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Topic</label>
                  <select
                    value={uploadTopic}
                    onChange={e => setUploadTopic(e.target.value)}
                    disabled={!uploadSubject}
                    className="w-full bg-ds-surface-input border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent disabled:opacity-50"
                  >
                    <option value="">No Topic</option>
                    {topics.map(t => (
                      <option key={t.id} value={t.id}>{t.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              {/* File Dropzone */}
              <div>
                <label className="block text-[11px] font-mono text-ds-muted mb-1 uppercase">Choose File</label>
                <input
                  type="file"
                  required
                  accept=".pdf,.md,.txt,.png,.jpg,.jpeg"
                  onChange={e => setSelectedFile(e.target.files?.[0] || null)}
                  className="w-full text-xs text-ds-secondary file:mr-4 file:py-2 file:px-4 file:rounded-ds file:border-0 file:text-xs file:font-semibold file:bg-ds-accent-soft file:text-ds-accent hover:file:bg-ds-accent-soft/80 cursor-pointer"
                  data-testid="file-picker-input"
                />
                <p className="text-[10px] text-ds-muted font-mono mt-1.5">
                  Allowed whitelist: PDF, Markdown, Plain Text, PNG, JPEG (max 50 MB)
                </p>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-ds-border">
                <button
                  type="button"
                  onClick={() => setUploadModalOpen(false)}
                  className="px-4 py-2 rounded-ds border border-ds-border text-xs text-ds-secondary hover:text-ds-primary"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={uploading}
                  className="px-5 py-2 rounded-ds bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-bold shadow-sm disabled:opacity-50"
                  data-testid="submit-upload-btn"
                >
                  {uploading ? 'Uploading...' : 'Upload Reference'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Inline Viewer Modal */}
      {viewerNote && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div 
            className="bg-ds-surface border border-ds-border rounded-ds w-full max-w-4xl max-h-[90vh] flex flex-col shadow-xl overflow-hidden"
            data-testid="viewer-modal"
          >
            <div className="px-6 py-4 border-b border-ds-border flex items-center justify-between">
              <div>
                <h2 className="text-base font-bold text-ds-primary">{viewerNote.title}</h2>
                <div className="flex items-center gap-2 text-xs text-ds-muted font-mono mt-0.5">
                  <span>{viewerNote.kind === 'FILE' ? viewerNote.originalFilename : 'Markdown Note'}</span>
                  {viewerNote.subjectName && <span>· {viewerNote.subjectName}</span>}
                </div>
              </div>
              <div className="flex items-center gap-2">
                {viewerNote.kind === 'FILE' && (
                  <a
                    href={noteApi.getFileUrl(viewerNote.id)}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-ds-accent hover:underline flex items-center gap-1 mr-2"
                  >
                    <span>Open in new tab</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                )}
                {viewerNote.kind === 'TEXT' && (
                  <button
                    type="button"
                    onClick={() => {
                      setViewerNote(null);
                      handleOpenEditEditor(viewerNote);
                    }}
                    className="text-xs text-ds-accent hover:underline flex items-center gap-1 mr-2"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                    <span>Edit</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setViewerNote(null)}
                  className="text-ds-muted hover:text-ds-primary p-1 rounded-ds"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              {viewerNote.kind === 'FILE' && viewerNote.contentType === 'application/pdf' && (
                <div className="space-y-3">
                  <embed
                    data-testid="pdf-embed"
                    src={noteApi.getFileUrl(viewerNote.id)}
                    type="application/pdf"
                    className="w-full h-[32rem] rounded-ds border border-ds-border"
                  />
                  <div className="text-center text-xs text-ds-muted">
                    Can't view PDF embedded?{' '}
                    <a
                      href={noteApi.getFileUrl(viewerNote.id)}
                      target="_blank"
                      rel="noreferrer"
                      className="text-ds-accent underline"
                    >
                      Click here to download or open directly
                    </a>
                  </div>
                </div>
              )}

              {viewerNote.kind === 'FILE' && viewerNote.contentType?.startsWith('image/') && (
                <div className="flex justify-center p-4">
                  <img
                    src={noteApi.getFileUrl(viewerNote.id)}
                    alt={viewerNote.title}
                    className="max-h-[30rem] rounded-ds object-contain border border-ds-border"
                  />
                </div>
              )}

              {viewerNote.kind === 'TEXT' && (
                <div className="bg-ds-surface-raised p-6 rounded-ds border border-ds-border leading-relaxed">
                  <MathText content={viewerNote.contentMd || ''} />
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

interface NoteCardProps {
  note: NoteItem;
  onTogglePin: (note: NoteItem) => void;
  onDelete: (id: string) => void;
  onEdit: (note: NoteItem) => void;
  onView: () => void;
}

const NoteCard: React.FC<NoteCardProps> = ({ note, onTogglePin, onDelete, onEdit, onView }) => {
  return (
    <div 
      className={`bg-ds-surface border rounded-ds p-5 flex flex-col justify-between space-y-3 shadow-sm hover:border-ds-border-strong transition-all ${
        note.isPinned ? 'border-ds-accent/40 bg-ds-accent-soft/5' : 'border-ds-border'
      }`}
      data-testid="note-card"
    >
      <div className="space-y-2.5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <span 
              className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-bold ${
                note.kind === 'FILE' 
                  ? 'bg-ds-info/15 text-ds-info border border-ds-info/30' 
                  : 'bg-ds-accent-soft text-ds-accent border border-ds-accent/30'
              }`}
            >
              {note.kind === 'FILE' ? (note.contentType === 'application/pdf' ? 'PDF' : 'FILE') : 'MD'}
            </span>
            {note.subjectName && (
              <span className="text-[10px] text-ds-secondary truncate max-w-[120px] font-mono">
                {note.subjectName}
              </span>
            )}
          </div>

          <button
            type="button"
            onClick={() => onTogglePin(note)}
            className={`p-1 rounded-ds transition-colors ${
              note.isPinned 
                ? 'text-ds-accent hover:text-ds-accent-hover' 
                : 'text-ds-muted hover:text-ds-primary'
            }`}
            title={note.isPinned ? 'Unpin note' : 'Pin note'}
            data-testid="pin-note-btn"
          >
            <Pin className={`w-3.5 h-3.5 ${note.isPinned ? 'fill-current' : ''}`} />
          </button>
        </div>

        <div>
          <h3 
            className="font-bold text-sm text-ds-primary line-clamp-1 cursor-pointer hover:text-ds-accent transition-colors"
            onClick={onView}
          >
            {note.title}
          </h3>
          {note.topicName && (
            <span className="text-[11px] text-ds-muted font-mono block mt-0.5">
              Topic: {note.topicName}
            </span>
          )}
        </div>

        {/* Snippet / Preview */}
        {note.kind === 'TEXT' && note.contentMd && (
          <p className="text-xs text-ds-secondary line-clamp-3 leading-relaxed font-sans opacity-90">
            {note.contentMd.substring(0, 200)}
          </p>
        )}

        {note.kind === 'FILE' && (
          <div className="bg-ds-surface-raised p-2.5 rounded-ds border border-ds-border text-xs flex items-center justify-between">
            <span className="truncate text-ds-primary font-mono text-[11px]">
              {note.originalFilename || 'Document file'}
            </span>
            {note.fileSizeBytes && (
              <span className="text-[10px] text-ds-muted font-mono shrink-0 ml-2">
                {(note.fileSizeBytes / 1024).toFixed(0)} KB
              </span>
            )}
          </div>
        )}
      </div>

      {/* Card Actions */}
      <div className="flex items-center justify-between pt-3 border-t border-ds-border text-xs">
        <button
          type="button"
          onClick={onView}
          className="text-ds-accent hover:underline font-semibold flex items-center gap-1"
          data-testid="view-note-btn"
        >
          <Eye className="w-3.5 h-3.5" />
          <span>{note.kind === 'FILE' ? 'View Document' : 'Read Note'}</span>
        </button>

        <div className="flex items-center gap-1">
          {note.kind === 'TEXT' && (
            <button
              type="button"
              onClick={() => onEdit(note)}
              className="p-1.5 text-ds-muted hover:text-ds-primary rounded-ds"
              title="Edit Note"
              data-testid="edit-note-btn"
            >
              <Edit3 className="w-3.5 h-3.5" />
            </button>
          )}

          <button
            type="button"
            onClick={() => onDelete(note.id)}
            className="p-1.5 text-ds-muted hover:text-ds-danger rounded-ds"
            title="Delete Note"
            data-testid="delete-note-btn"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default NotesPage;
