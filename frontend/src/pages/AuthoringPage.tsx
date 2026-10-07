import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import { 
  PenLine, 
  Sparkles, 
  CheckCircle2, 
  AlertCircle, 
  AlertTriangle, 
  Plus, 
  Trash2, 
  HelpCircle, 
  RotateCcw, 
  ExternalLink,
  Save,
  Check,
  Eye,
  EyeOff
} from 'lucide-react';
import { 
  useSubjects, 
  useTopics, 
  useQuestion, 
  useValidateQuestion, 
  useCreateQuestion, 
  useUpdateQuestion 
} from '../hooks/useQueries';
import { QuestionType, QuestionUpsertDto } from '../api/types';
import { MathText } from '../components/MathText';
import { ApiError } from '../api/apiClient';

interface OptionRow {
  label: string;
  text: string;
}

const DEFAULT_OPTIONS: OptionRow[] = [
  { label: 'A', text: '' },
  { label: 'B', text: '' },
  { label: 'C', text: '' },
  { label: 'D', text: '' },
];

const OPTION_LETTERS = ['A', 'B', 'C', 'D', 'E', 'F'];

export const AuthoringPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const editId = searchParams.get('edit');

  // Queries
  const { data: subjects = [] } = useSubjects();
  const { data: existingQuestion, isLoading: loadingExisting, refetch: refetchExisting } = useQuestion(editId || undefined);

  // Mutations
  const validateMutation = useValidateQuestion();
  const createMutation = useCreateQuestion();
  const updateMutation = useUpdateQuestion();

  // Form State
  const [type, setType] = useState<QuestionType>('MCQ');
  const [marks, setMarks] = useState<number>(1);
  const [year, setYear] = useState<number>(new Date().getFullYear());
  const [subjectId, setSubjectId] = useState<string>('');
  const [topicId, setTopicId] = useState<string>('');
  const [externalId, setExternalId] = useState<string>('');
  const [statement, setStatement] = useState<string>('');
  const [debouncedStatement, setDebouncedStatement] = useState<string>('');
  const [options, setOptions] = useState<OptionRow[]>(DEFAULT_OPTIONS);
  const [correctOptions, setCorrectOptions] = useState<string[]>(['A']);
  const [numericMin, setNumericMin] = useState<string>('');
  const [numericMax, setNumericMax] = useState<string>('');
  const [explanation, setExplanation] = useState<string>('');
  const [gateOverflowUrl, setGateOverflowUrl] = useState<string>('');
  const [difficulty, setDifficulty] = useState<string>('MEDIUM');
  const [version, setVersion] = useState<number | undefined>(undefined);
  const [updatedAt, setUpdatedAt] = useState<string | undefined>(undefined);

  // Symmetric Tolerance NAT Helper State
  const [natAnswer, setNatAnswer] = useState<string>('');
  const [natTolAmount, setNatTolAmount] = useState<string>('0.5');

  // UI state
  const [showExplanationPreview, setShowExplanationPreview] = useState<boolean>(true);
  const [clientErrors, setClientErrors] = useState<Record<string, string>>({});
  const [serverErrors, setServerErrors] = useState<Record<string, string>>({});
  const [serverWarnings, setServerWarnings] = useState<string[]>([]);
  const [conflictError, setConflictError] = useState<boolean>(false);
  const [successSaved, setSuccessSaved] = useState<{ id: string; externalId?: string } | null>(null);

  // Cascading topics
  const { data: topics = [] } = useTopics(subjectId);

  const statementTextareaRef = useRef<HTMLTextAreaElement>(null);

  // Populate edit mode
  useEffect(() => {
    if (existingQuestion) {
      setType(existingQuestion.type);
      setMarks(existingQuestion.marks);
      setYear(existingQuestion.year);
      setSubjectId(existingQuestion.subjectId);
      setTopicId(existingQuestion.topicId);
      setExternalId(existingQuestion.externalId || '');
      setStatement(existingQuestion.statement);
      setDebouncedStatement(existingQuestion.statement);
      setExplanation(existingQuestion.explanation || '');
      setGateOverflowUrl(existingQuestion.gateOverflowUrl || '');
      setDifficulty(existingQuestion.difficulty || 'MEDIUM');
      setVersion(existingQuestion.version);

      if (existingQuestion.type === 'NAT') {
        setNumericMin(existingQuestion.numericMin != null ? String(existingQuestion.numericMin) : '');
        setNumericMax(existingQuestion.numericMax != null ? String(existingQuestion.numericMax) : '');
      } else {
        if (existingQuestion.optionsJson) {
          try {
            const parsed = JSON.parse(existingQuestion.optionsJson);
            if (Array.isArray(parsed) && parsed.length > 0) {
              setOptions(parsed.map((item: any, idx: number) => ({
                label: item.label || OPTION_LETTERS[idx] || 'A',
                text: item.text || '',
              })));
            }
          } catch {
            // retain default
          }
        }
        if (existingQuestion.correctOptions) {
          const split = existingQuestion.correctOptions.split(',').map((s) => s.trim()).filter(Boolean);
          setCorrectOptions(split);
        }
      }
    }
  }, [existingQuestion]);

  // Debounce statement preview (300 ms)
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedStatement(statement);
    }, 300);
    return () => clearTimeout(handler);
  }, [statement]);

  // Auto-suggest externalId
  const handleAutoSuggestExternalId = () => {
    const selectedSubj = subjects.find((s) => s.id === subjectId);
    const code = selectedSubj ? selectedSubj.code.toUpperCase() : 'GEN';
    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
    const seq = Math.floor(1000 + Math.random() * 9000);
    setExternalId(`AUTH-${code}-${dateStr}-${seq}`);
  };

  // Toolbar action helper
  const insertText = (before: string, after: string = '') => {
    const textarea = statementTextareaRef.current;
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selected = statement.substring(start, end);
    const replacement = `${before}${selected}${after}`;
    const nextVal = statement.substring(0, start) + replacement + statement.substring(end);
    setStatement(nextVal);
    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + before.length, end + before.length);
    }, 0);
  };

  // Option row operations
  const handleOptionTextChange = (index: number, text: string) => {
    setOptions((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], text };
      return next;
    });
  };

  const handleAddOption = () => {
    if (options.length >= 6) return;
    const nextLetter = OPTION_LETTERS[options.length];
    setOptions((prev) => [...prev, { label: nextLetter, text: '' }]);
  };

  const handleRemoveOption = (index: number) => {
    if (options.length <= 4) return;
    const removedLabel = options[index].label;
    const remaining = options.filter((_, i) => i !== index).map((opt, i) => ({
      ...opt,
      label: OPTION_LETTERS[i],
    }));
    setOptions(remaining);
    setCorrectOptions((prev) => prev.filter((l) => l !== removedLabel));
  };

  const handleToggleMsqOption = (label: string) => {
    setCorrectOptions((prev) =>
      prev.includes(label) ? prev.filter((l) => l !== label) : [...prev, label]
    );
  };

  // Symmetric NAT helper
  const handleApplySymmetricTolerance = () => {
    const ans = parseFloat(natAnswer);
    const tol = parseFloat(natTolAmount);
    if (!isNaN(ans) && !isNaN(tol)) {
      setNumericMin((ans - tol).toString());
      setNumericMax((ans + tol).toString());
    }
  };

  // Client-side V-Q mirror validation
  const validateClient = (): boolean => {
    const errors: Record<string, string> = {};

    if (!statement.trim()) {
      errors.statement = 'Statement must not be blank';
    }
    if (!subjectId) {
      errors.subjectId = 'Subject is required';
    }
    if (!topicId) {
      errors.topicId = 'Topic is required';
    }
    if (marks !== 1 && marks !== 2) {
      errors.marks = 'Marks must be 1 or 2';
    }

    if (type === 'MCQ') {
      if (options.length < 4) {
        errors.options = 'MCQ requires at least 4 options';
      }
      if (correctOptions.length !== 1) {
        errors.correctOptions = 'MCQ must have exactly 1 correct option selected';
      }
      if (options.some((o) => !o.text.trim())) {
        errors.options = 'All option rows must have text';
      }
    } else if (type === 'MSQ') {
      if (options.length < 4) {
        errors.options = 'MSQ requires at least 4 options';
      }
      if (correctOptions.length < 2) {
        errors.correctOptions = 'MSQ requires at least 2 correct options (V-Q parity)';
      }
      if (options.some((o) => !o.text.trim())) {
        errors.options = 'All option rows must have text';
      }
    } else if (type === 'NAT') {
      const min = parseFloat(numericMin);
      const max = parseFloat(numericMax);
      if (isNaN(min) || isNaN(max)) {
        errors.numericMin = 'NAT questions require both numericMin and numericMax';
      } else if (min > max) {
        errors.numericMin = 'numericMin must be less than or equal to numericMax';
      }
    }

    setClientErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Prepare DTO
  const buildUpsertDto = (): QuestionUpsertDto => {
    return {
      externalId: externalId.trim() || undefined,
      subjectId,
      topicId,
      year,
      type,
      marks,
      statement: statement.trim(),
      optionsJson: type === 'NAT' ? undefined : JSON.stringify(options),
      correctOptions: type === 'NAT' ? undefined : correctOptions.join(','),
      numericMin: type === 'NAT' && numericMin !== '' ? parseFloat(numericMin) : undefined,
      numericMax: type === 'NAT' && numericMax !== '' ? parseFloat(numericMax) : undefined,
      explanation: explanation.trim() || undefined,
      gateOverflowUrl: gateOverflowUrl.trim() || undefined,
      difficulty,
      source: 'AUTHORED',
      curationStatus: 'UNREVIEWED',
      version,
    };
  };

  // Dry-run server validation
  const handleValidateDryRun = async () => {
    setServerErrors({});
    setServerWarnings([]);
    const isValidClient = validateClient();
    const dto = buildUpsertDto();

    try {
      const result = await validateMutation.mutateAsync(dto);
      if (result.valid) {
        setServerWarnings(result.warnings || []);
      } else {
        setServerErrors(result.fieldErrors || {});
        setServerWarnings(result.warnings || []);
      }
    } catch (err: any) {
      if (err instanceof ApiError && err.fieldErrors) {
        setServerErrors(err.fieldErrors);
      }
    }
  };

  // Save handler
  const handleSaveQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    setConflictError(false);
    setServerErrors({});
    setServerWarnings([]);

    if (!validateClient()) {
      return;
    }

    const dto = buildUpsertDto();

    try {
      // 1. Dry run validate first
      const validation = await validateMutation.mutateAsync(dto);
      if (!validation.valid) {
        setServerErrors(validation.fieldErrors || {});
        setServerWarnings(validation.warnings || []);
        return;
      }
      setServerWarnings(validation.warnings || []);

      // 2. Persist
      if (editId) {
        const updated = await updateMutation.mutateAsync({ id: editId, dto });
        setSuccessSaved({ id: updated.id, externalId: updated.externalId });
      } else {
        const created = await createMutation.mutateAsync(dto);
        setSuccessSaved({ id: created.id, externalId: created.externalId });
      }
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 409) {
          setConflictError(true);
        } else if (err.fieldErrors) {
          setServerErrors(err.fieldErrors);
        }
      }
    }
  };

  const handleResetForm = () => {
    setSuccessSaved(null);
    setExternalId('');
    setStatement('');
    setDebouncedStatement('');
    setOptions(DEFAULT_OPTIONS);
    setCorrectOptions(['A']);
    setNumericMin('');
    setNumericMax('');
    setExplanation('');
    setGateOverflowUrl('');
    setClientErrors({});
    setServerErrors({});
    setServerWarnings([]);
    setConflictError(false);
    if (editId) {
      navigate('/author');
    }
  };

  const selectedSubject = subjects.find((s) => s.id === subjectId);
  const selectedTopic = topics.find((t) => t.id === topicId);

  // Compute NAT tolerance span live
  const toleranceSpan = useMemo(() => {
    if (type !== 'NAT') return null;
    const min = parseFloat(numericMin);
    const max = parseFloat(numericMax);
    if (isNaN(min) || isNaN(max)) return null;
    return {
      span: max - min,
      center: (min + max) / 2,
      delta: (max - min) / 2,
    };
  }, [type, numericMin, numericMax]);

  return (
    <div className="max-w-7xl mx-auto px-4 py-8 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-ds-border pb-6">
        <div>
          <div className="flex items-center gap-2 text-ds-accent text-sm font-semibold tracking-wide uppercase mb-1">
            <PenLine className="w-4 h-4" />
            <span>Authoring Console</span>
          </div>
          <h1 className="text-2xl font-bold text-ds-primary tracking-tight">
            {editId ? 'Edit Question' : 'Author New Question'}
          </h1>
          <p className="text-xs text-ds-muted mt-1">
            Type-aware authoring with live KaTeX preview, V-Q validation, and server dry-run verification.
          </p>
        </div>

        {editId && existingQuestion && (
          <div className="bg-ds-surface border border-ds-border-strong rounded-ds px-3 py-2 text-xs flex items-center gap-3">
            <span className="font-mono text-ds-accent">Version {existingQuestion.version}</span>
            <span className="text-ds-muted">|</span>
            <span className="text-ds-muted">ID: {existingQuestion.id.slice(0, 8)}...</span>
          </div>
        )}
      </div>

      {/* Version Conflict Alert (409) */}
      {conflictError && (
        <div className="bg-ds-surface border-2 border-ds-danger rounded-ds p-4 space-y-3">
          <div className="flex items-center gap-2 text-ds-danger font-semibold text-sm">
            <AlertTriangle className="w-5 h-5 shrink-0" />
            <span>Version Conflict: Question was modified by another session</span>
          </div>
          <p className="text-xs text-ds-secondary">
            Your current changes conflict with the version in the database. Choose whether to reload the latest server state or keep your edits to retry.
          </p>
          <div className="flex items-center gap-3 pt-2">
            <button
              type="button"
              onClick={async () => {
                await refetchExisting();
                setConflictError(false);
              }}
              className="px-3 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium hover:bg-ds-accent/90 transition-colors"
            >
              Reload Latest Server State
            </button>
            <button
              type="button"
              onClick={() => setConflictError(false)}
              className="px-3 py-1.5 rounded-ds bg-ds-surface-hover text-ds-primary border border-ds-border text-xs font-medium hover:bg-ds-surface transition-colors"
            >
              Keep My Edits
            </button>
          </div>
        </div>
      )}

      {/* Success Panel */}
      {successSaved && (
        <div className="bg-ds-surface border border-ds-border-strong rounded-ds p-5 space-y-3 shadow-sm">
          <div className="flex items-center gap-2 text-ds-accent font-semibold text-sm">
            <CheckCircle2 className="w-5 h-5 shrink-0" />
            <span>Question Saved Successfully!</span>
          </div>
          <p className="text-xs text-ds-secondary">
            Question ID: <span className="font-mono text-ds-primary">{successSaved.id}</span>
            {successSaved.externalId && ` (${successSaved.externalId})`}
          </p>
          <div className="flex flex-wrap items-center gap-3 pt-2">
            <Link
              to={`/questions/${successSaved.id}`}
              className="px-3 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-medium flex items-center gap-1.5 hover:bg-ds-accent/90 transition-colors"
            >
              <ExternalLink className="w-3.5 h-3.5" />
              <span>View in Question Bank</span>
            </Link>
            <button
              type="button"
              onClick={handleResetForm}
              className="px-3 py-1.5 rounded-ds bg-ds-surface-hover text-ds-primary border border-ds-border text-xs font-medium flex items-center gap-1.5 hover:bg-ds-surface transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Author Another Question</span>
            </button>
            <Link
              to="/curation"
              className="px-3 py-1.5 rounded-ds bg-ds-surface-hover text-ds-secondary border border-ds-border text-xs font-medium hover:text-ds-primary transition-colors"
            >
              Go to Curation Queue
            </Link>
          </div>
        </div>
      )}

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
        {/* LEFT COLUMN: Editor Form */}
        <form onSubmit={handleSaveQuestion} className="space-y-6">
          {/* Segmented Type Control */}
          <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-2">
            <label className="text-xs font-semibold text-ds-muted uppercase tracking-wider block">
              Question Type
            </label>
            <div className="grid grid-cols-3 gap-2">
              {(['MCQ', 'MSQ', 'NAT'] as QuestionType[]).map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => {
                    setType(t);
                    if (t === 'MCQ' && correctOptions.length > 1) {
                      setCorrectOptions([correctOptions[0] || 'A']);
                    }
                  }}
                  className={`py-2 px-3 text-xs font-semibold rounded-ds border transition-all text-center ${
                    type === t
                      ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent shadow-sm'
                      : 'bg-ds-surface-hover text-ds-secondary border-ds-border hover:text-ds-primary hover:bg-ds-surface'
                  }`}
                >
                  {t}
                </button>
              ))}
            </div>
          </div>

          {/* Metadata Grid: Marks, Year, Difficulty */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Marks */}
            <div className="bg-ds-surface border border-ds-border rounded-ds p-3 space-y-1.5">
              <label className="text-xs font-semibold text-ds-muted uppercase tracking-wider block">
                Marks
              </label>
              <div className="flex gap-2">
                {[1, 2].map((m) => (
                  <button
                    key={m}
                    type="button"
                    onClick={() => setMarks(m)}
                    className={`flex-1 py-1.5 text-xs font-medium rounded-ds border transition-colors ${
                      marks === m
                        ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent font-semibold'
                        : 'bg-ds-surface-hover text-ds-secondary border-ds-border hover:text-ds-primary'
                    }`}
                  >
                    {m} Mark{m > 1 ? 's' : ''}
                  </button>
                ))}
              </div>
              {clientErrors.marks && (
                <span className="text-[11px] text-ds-danger block">{clientErrors.marks}</span>
              )}
            </div>

            {/* Year */}
            <div className="bg-ds-surface border border-ds-border rounded-ds p-3 space-y-1.5">
              <label className="text-xs font-semibold text-ds-muted uppercase tracking-wider block">
                Exam Year
              </label>
              <input
                type="number"
                value={year}
                onChange={(e) => setYear(parseInt(e.target.value) || 2024)}
                className="w-full bg-ds-surface-hover border border-ds-border rounded-ds px-3 py-1.5 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                min="1990"
                max="2035"
              />
            </div>

            {/* Difficulty */}
            <div className="bg-ds-surface border border-ds-border rounded-ds p-3 space-y-1.5">
              <label htmlFor="difficulty-select" className="text-xs font-semibold text-ds-muted uppercase tracking-wider block">
                Difficulty
              </label>
              <select
                id="difficulty-select"
                aria-label="Difficulty select"
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
                className="w-full bg-ds-surface-hover border border-ds-border rounded-ds px-3 py-1.5 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
              >
                <option value="EASY">EASY</option>
                <option value="MEDIUM">MEDIUM</option>
                <option value="HARD">HARD</option>
              </select>
            </div>
          </div>

          {/* Taxonomy: Subject -> Cascading Topic */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Subject */}
            <div className="space-y-1.5">
              <label htmlFor="subject-select" className="text-xs font-semibold text-ds-primary block">
                Subject <span className="text-ds-danger">*</span>
              </label>
              <select
                id="subject-select"
                aria-label="Subject select"
                value={subjectId}
                onChange={(e) => {
                  setSubjectId(e.target.value);
                  setTopicId('');
                }}
                className={`w-full bg-ds-surface border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent ${
                  clientErrors.subjectId ? 'border-ds-danger' : 'border-ds-border'
                }`}
              >
                <option value="">-- Select Subject --</option>
                {subjects.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
              {clientErrors.subjectId && (
                <span className="text-[11px] text-ds-danger block">{clientErrors.subjectId}</span>
              )}
            </div>

            {/* Topic */}
            <div className="space-y-1.5">
              <label htmlFor="topic-select" className="text-xs font-semibold text-ds-primary block">
                Topic <span className="text-ds-danger">*</span>
              </label>
              <select
                id="topic-select"
                aria-label="Topic select"
                value={topicId}
                onChange={(e) => setTopicId(e.target.value)}
                disabled={!subjectId}
                className={`w-full bg-ds-surface border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent disabled:opacity-50 ${
                  clientErrors.topicId ? 'border-ds-danger' : 'border-ds-border'
                }`}
              >
                <option value="">-- Select Topic --</option>
                {topics.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
              {clientErrors.topicId && (
                <span className="text-[11px] text-ds-danger block">{clientErrors.topicId}</span>
              )}
            </div>
          </div>

          {/* External ID Auto-suggest */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-ds-primary">
                External ID (Unique Tag)
              </label>
              <button
                type="button"
                onClick={handleAutoSuggestExternalId}
                className="text-[11px] font-medium text-ds-accent hover:underline flex items-center gap-1"
              >
                <Sparkles className="w-3 h-3" />
                <span>Auto-suggest ID</span>
              </button>
            </div>
            <input
              type="text"
              placeholder="e.g. AUTH-ALGO-20261007-4291"
              value={externalId}
              onChange={(e) => setExternalId(e.target.value)}
              className="w-full bg-ds-surface border border-ds-border rounded-ds px-3 py-2 text-xs font-mono text-ds-primary focus:outline-none focus:border-ds-accent"
            />
          </div>

          {/* Statement Field & Toolbar */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-ds-primary">
                Question Statement (LaTeX supported) <span className="text-ds-danger">*</span>
              </label>
              <div className="flex items-center gap-1 bg-ds-surface border border-ds-border rounded-ds p-0.5">
                <button
                  type="button"
                  onClick={() => insertText('$', '$')}
                  className="px-2 py-0.5 text-[10px] font-mono text-ds-secondary hover:text-ds-primary hover:bg-ds-surface-hover rounded"
                  title="Insert inline LaTeX math ($...$)"
                >
                  $ math $
                </button>
                <button
                  type="button"
                  onClick={() => insertText('$$\n', '\n$$')}
                  className="px-2 py-0.5 text-[10px] font-mono text-ds-secondary hover:text-ds-primary hover:bg-ds-surface-hover rounded"
                  title="Insert display LaTeX math ($$...$$)"
                >
                  $$ block $$
                </button>
                <button
                  type="button"
                  onClick={() => insertText('**', '**')}
                  className="px-2 py-0.5 text-[10px] font-semibold text-ds-secondary hover:text-ds-primary hover:bg-ds-surface-hover rounded"
                  title="Insert Bold text (**...**)"
                >
                  B
                </button>
                <button
                  type="button"
                  onClick={() => insertText('`', '`')}
                  className="px-2 py-0.5 text-[10px] font-mono text-ds-secondary hover:text-ds-primary hover:bg-ds-surface-hover rounded"
                  title="Insert code span (`...`)"
                >
                  &lt;/&gt;
                </button>
              </div>
            </div>
            <textarea
              ref={statementTextareaRef}
              rows={5}
              placeholder="Enter question statement here. You can use $x^2$ for inline math or $$\sum_{i=1}^n i$$ for display math."
              value={statement}
              onChange={(e) => setStatement(e.target.value)}
              className={`w-full bg-ds-surface border rounded-ds p-3 text-xs text-ds-primary font-mono focus:outline-none focus:border-ds-accent leading-relaxed ${
                clientErrors.statement || serverErrors.statement ? 'border-ds-danger' : 'border-ds-border'
              }`}
            />
            {(clientErrors.statement || serverErrors.statement) && (
              <span className="text-[11px] text-ds-danger block">
                {clientErrors.statement || serverErrors.statement}
              </span>
            )}
          </div>

          {/* Dynamic Builder: MCQ / MSQ / NAT */}
          {type === 'MCQ' && (
            <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-xs font-semibold text-ds-primary uppercase tracking-wider">
                    MCQ Options (4 to 6 rows)
                  </h3>
                  <p className="text-[11px] text-ds-muted">
                    Select exactly one radio button as the correct key.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleAddOption}
                  disabled={options.length >= 6}
                  className="px-2 py-1 text-xs font-medium text-ds-accent hover:bg-ds-surface-hover rounded-ds border border-ds-border disabled:opacity-40 flex items-center gap-1"
                >
                  <Plus className="w-3 h-3" />
                  <span>Add Option</span>
                </button>
              </div>

              <div className="space-y-2">
                {options.map((opt, idx) => (
                  <div key={opt.label} className="flex items-center gap-2">
                    {/* Radio Button */}
                    <input
                      type="radio"
                      name="mcq-correct"
                      checked={correctOptions[0] === opt.label}
                      onChange={() => setCorrectOptions([opt.label])}
                      className="text-ds-accent focus:ring-0 cursor-pointer"
                    />
                    {/* Label Badge */}
                    <span className="w-6 h-6 rounded-full bg-ds-surface-raised border border-ds-border-strong text-ds-primary text-xs font-bold flex items-center justify-center shrink-0">
                      {opt.label}
                    </span>
                    {/* Input */}
                    <input
                      type="text"
                      placeholder={`Option ${opt.label} text (LaTeX supported)`}
                      value={opt.text}
                      onChange={(e) => handleOptionTextChange(idx, e.target.value)}
                      className="flex-1 bg-ds-surface-hover border border-ds-border rounded-ds px-3 py-1.5 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                    />
                    {/* Remove button */}
                    <button
                      type="button"
                      onClick={() => handleRemoveOption(idx)}
                      disabled={options.length <= 4}
                      className="p-1.5 text-ds-muted hover:text-ds-danger disabled:opacity-30 transition-colors"
                      title="Remove option"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>

              {(clientErrors.options || clientErrors.correctOptions || serverErrors.correctOptions) && (
                <span className="text-[11px] text-ds-danger block">
                  {clientErrors.options || clientErrors.correctOptions || serverErrors.correctOptions}
                </span>
              )}
            </div>
          )}

          {type === 'MSQ' && (
            <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-xs font-semibold text-ds-primary uppercase tracking-wider">
                    MSQ Options (4 to 6 rows)
                  </h3>
                  <p className="text-[11px] text-ds-muted">
                    Check all correct options (at least 2 required for MSQ parity).
                  </p>
                </div>
                <button
                  type="button"
                  onClick={handleAddOption}
                  disabled={options.length >= 6}
                  className="px-2 py-1 text-xs font-medium text-ds-accent hover:bg-ds-surface-hover rounded-ds border border-ds-border disabled:opacity-40 flex items-center gap-1"
                >
                  <Plus className="w-3 h-3" />
                  <span>Add Option</span>
                </button>
              </div>

              <div className="space-y-2">
                {options.map((opt, idx) => (
                  <div key={opt.label} className="flex items-center gap-2">
                    {/* Checkbox */}
                    <input
                      type="checkbox"
                      checked={correctOptions.includes(opt.label)}
                      onChange={() => handleToggleMsqOption(opt.label)}
                      className="rounded text-ds-accent focus:ring-0 cursor-pointer"
                    />
                    {/* Label Badge */}
                    <span className="w-6 h-6 rounded-full bg-ds-surface-raised border border-ds-border-strong text-ds-primary text-xs font-bold flex items-center justify-center shrink-0">
                      {opt.label}
                    </span>
                    {/* Input */}
                    <input
                      type="text"
                      placeholder={`Option ${opt.label} text (LaTeX supported)`}
                      value={opt.text}
                      onChange={(e) => handleOptionTextChange(idx, e.target.value)}
                      className="flex-1 bg-ds-surface-hover border border-ds-border rounded-ds px-3 py-1.5 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
                    />
                    {/* Remove button */}
                    <button
                      type="button"
                      onClick={() => handleRemoveOption(idx)}
                      disabled={options.length <= 4}
                      className="p-1.5 text-ds-muted hover:text-ds-danger disabled:opacity-30 transition-colors"
                      title="Remove option"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>

              {(clientErrors.options || clientErrors.correctOptions || serverErrors.correctOptions) && (
                <span className="text-[11px] text-ds-danger block">
                  {clientErrors.options || clientErrors.correctOptions || serverErrors.correctOptions}
                </span>
              )}
            </div>
          )}

          {type === 'NAT' && (
            <div className="bg-ds-surface border border-ds-border rounded-ds p-4 space-y-4">
              <div>
                <h3 className="text-xs font-semibold text-ds-primary uppercase tracking-wider">
                  Numerical Answer Range [numericMin, numericMax]
                </h3>
                <p className="text-[11px] text-ds-muted">
                  Specify the accepted range for evaluation. Both boundary values are inclusive.
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-ds-secondary">
                    Minimum Bound (numericMin) <span className="text-ds-danger">*</span>
                  </label>
                  <input
                    type="number"
                    step="any"
                    placeholder="e.g. 10.0"
                    value={numericMin}
                    onChange={(e) => setNumericMin(e.target.value)}
                    className="w-full bg-ds-surface-hover border border-ds-border rounded-ds px-3 py-2 text-xs font-mono text-ds-primary focus:outline-none focus:border-ds-accent"
                  />
                </div>
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-ds-secondary">
                    Maximum Bound (numericMax) <span className="text-ds-danger">*</span>
                  </label>
                  <input
                    type="number"
                    step="any"
                    placeholder="e.g. 10.5"
                    value={numericMax}
                    onChange={(e) => setNumericMax(e.target.value)}
                    className="w-full bg-ds-surface-hover border border-ds-border rounded-ds px-3 py-2 text-xs font-mono text-ds-primary focus:outline-none focus:border-ds-accent"
                  />
                </div>
              </div>

              {/* Tolerance Span Live Chip */}
              {toleranceSpan && (
                <div className="bg-ds-surface-raised border border-ds-border-strong rounded-ds px-3 py-2 text-xs flex flex-wrap items-center gap-3">
                  <span className="font-semibold text-ds-primary">Range Span:</span>
                  <span className="font-mono text-ds-accent">{toleranceSpan.span.toFixed(2)}</span>
                  <span className="text-ds-muted">|</span>
                  <span className="text-ds-muted">Center: {toleranceSpan.center.toFixed(2)}</span>
                  <span className="text-ds-muted">|</span>
                  <span className="text-ds-accent font-mono">Tolerance: ±{toleranceSpan.delta.toFixed(2)}</span>
                </div>
              )}

              {/* Symmetric Around Answer Helper */}
              <div className="border-t border-ds-border pt-3 space-y-2">
                <div className="text-[11px] font-semibold text-ds-muted uppercase">
                  Symmetric Around Answer Helper
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <input
                    type="number"
                    step="any"
                    placeholder="Base Answer (e.g. 12.0)"
                    value={natAnswer}
                    onChange={(e) => setNatAnswer(e.target.value)}
                    className="flex-1 min-w-[120px] bg-ds-surface-hover border border-ds-border rounded-ds px-2.5 py-1.5 text-xs text-ds-primary"
                  />
                  <input
                    type="number"
                    step="any"
                    placeholder="± Tolerance (e.g. 0.5)"
                    value={natTolAmount}
                    onChange={(e) => setNatTolAmount(e.target.value)}
                    className="w-28 bg-ds-surface-hover border border-ds-border rounded-ds px-2.5 py-1.5 text-xs text-ds-primary"
                  />
                  <button
                    type="button"
                    onClick={handleApplySymmetricTolerance}
                    className="px-3 py-1.5 rounded-ds bg-ds-surface-hover text-ds-primary border border-ds-border hover:bg-ds-surface text-xs font-medium transition-colors"
                  >
                    Apply Bounds
                  </button>
                </div>
              </div>

              {(clientErrors.numericMin || serverErrors.numericMin) && (
                <span className="text-[11px] text-ds-danger block">
                  {clientErrors.numericMin || serverErrors.numericMin}
                </span>
              )}
            </div>
          )}

          {/* Explanation Field */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold text-ds-primary">
                Detailed Explanation / Solution
              </label>
              <button
                type="button"
                onClick={() => setShowExplanationPreview(!showExplanationPreview)}
                className="text-[11px] text-ds-muted hover:text-ds-primary flex items-center gap-1"
              >
                {showExplanationPreview ? <Eye className="w-3 h-3" /> : <EyeOff className="w-3 h-3" />}
                <span>{showExplanationPreview ? 'Hide in Preview' : 'Show in Preview'}</span>
              </button>
            </div>
            <textarea
              rows={3}
              placeholder="Provide solution steps, formulas used, or proof outline for post-submission review."
              value={explanation}
              onChange={(e) => setExplanation(e.target.value)}
              className="w-full bg-ds-surface border border-ds-border rounded-ds p-3 text-xs text-ds-primary font-mono focus:outline-none focus:border-ds-accent"
            />
          </div>

          {/* GateOverflow URL */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-ds-primary">
              GATE Overflow Discussion URL (Optional)
            </label>
            <input
              type="url"
              placeholder="https://gateoverflow.in/..."
              value={gateOverflowUrl}
              onChange={(e) => setGateOverflowUrl(e.target.value)}
              className="w-full bg-ds-surface border border-ds-border rounded-ds px-3 py-2 text-xs text-ds-primary focus:outline-none focus:border-ds-accent"
            />
          </div>

          {/* Server Warnings Display */}
          {serverWarnings.length > 0 && (
            <div className="bg-ds-surface border border-ds-warning/50 rounded-ds p-3 space-y-1.5">
              <div className="flex items-center gap-1.5 text-ds-warning text-xs font-semibold">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>Validation Advisory / Warnings:</span>
              </div>
              <ul className="list-disc pl-5 text-xs text-ds-secondary space-y-0.5">
                {serverWarnings.map((w, idx) => (
                  <li key={idx}>{w}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Form Actions: Dry-Run Validate & Save */}
          <div className="flex items-center gap-3 pt-4 border-t border-ds-border">
            <button
              type="button"
              onClick={handleValidateDryRun}
              disabled={validateMutation.isPending}
              className="px-4 py-2 rounded-ds bg-ds-surface-hover text-ds-primary border border-ds-border text-xs font-semibold hover:bg-ds-surface disabled:opacity-50 transition-colors flex items-center gap-1.5"
            >
              <Check className="w-3.5 h-3.5" />
              <span>Validate (Dry Run)</span>
            </button>

            <button
              type="submit"
              disabled={createMutation.isPending || updateMutation.isPending}
              className="px-5 py-2 rounded-ds bg-ds-accent text-ds-accent-contrast text-xs font-semibold hover:bg-ds-accent/90 disabled:opacity-50 transition-colors flex items-center gap-1.5 shadow-sm"
            >
              <Save className="w-3.5 h-3.5" />
              <span>{editId ? 'Update Question' : 'Save Question'}</span>
            </button>

            {editId && (
              <button
                type="button"
                onClick={() => navigate('/author')}
                className="px-3 py-2 rounded-ds bg-transparent text-ds-muted hover:text-ds-primary text-xs transition-colors"
              >
                Cancel Edit
              </button>
            )}
          </div>
        </form>

        {/* RIGHT COLUMN: Live Preview Card */}
        <div className="sticky top-6 space-y-4">
          <div className="bg-ds-surface border border-ds-border rounded-ds p-5 space-y-4 shadow-sm">
            {/* Preview Card Header */}
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-ds-border pb-3">
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 text-[11px] font-bold rounded-ds bg-ds-accent text-ds-accent-contrast">
                  {type}
                </span>
                <span className="px-2 py-0.5 text-[11px] font-semibold rounded-ds bg-ds-surface-raised border border-ds-border text-ds-secondary">
                  {marks} Mark{marks > 1 ? 's' : ''}
                </span>
                <span className="text-[11px] font-mono text-ds-muted">
                  GATE {year}
                </span>
              </div>

              <div className="text-[11px] text-ds-muted">
                {selectedSubject?.name || 'Subject'} &rarr; {selectedTopic?.name || 'Topic'}
              </div>
            </div>

            {/* External ID tag if any */}
            {externalId && (
              <div className="text-[10px] font-mono text-ds-accent">
                {externalId}
              </div>
            )}

            {/* Statement Preview with MathText */}
            <div className="text-sm text-ds-primary leading-relaxed py-1">
              <MathText content={debouncedStatement || 'Question statement will preview here...'} />
            </div>

            {/* Options Preview for MCQ / MSQ */}
            {type !== 'NAT' && (
              <div className="space-y-2 pt-2 border-t border-ds-border">
                {options.map((opt) => {
                  const isCorrectKey = correctOptions.includes(opt.label);
                  return (
                    <div
                      key={opt.label}
                      className={`p-3 rounded-ds border flex items-start gap-3 transition-colors ${
                        isCorrectKey
                          ? 'bg-ds-surface-raised border-ds-accent/60 shadow-xs'
                          : 'bg-ds-surface border-ds-border'
                      }`}
                    >
                      <span className={`w-5 h-5 rounded-full text-xs font-bold flex items-center justify-center shrink-0 ${
                        isCorrectKey
                          ? 'bg-ds-accent text-ds-accent-contrast'
                          : 'bg-ds-surface-hover text-ds-secondary border border-ds-border'
                      }`}>
                        {opt.label}
                      </span>
                      <div className="text-xs text-ds-primary flex-1 pt-0.5">
                        <MathText content={opt.text || `Option ${opt.label} text`} />
                      </div>
                      {isCorrectKey && (
                        <span className="text-[10px] font-semibold text-ds-accent bg-ds-accent/10 px-1.5 py-0.5 rounded border border-ds-accent/30 shrink-0">
                          Answer Key
                        </span>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* NAT Answer Range Preview */}
            {type === 'NAT' && (
              <div className="pt-2 border-t border-ds-border space-y-2">
                <span className="text-xs font-semibold text-ds-muted uppercase">
                  Accepted Answer Range
                </span>
                <div className="p-3 rounded-ds bg-ds-surface-raised border border-ds-border-strong text-xs font-mono text-ds-primary flex items-center justify-between">
                  <span>[{numericMin || 'min'}, {numericMax || 'max'}]</span>
                  {toleranceSpan && (
                    <span className="text-ds-accent text-[11px]">
                      Span: {toleranceSpan.span.toFixed(2)}
                    </span>
                  )}
                </div>
              </div>
            )}

            {/* Explanation Preview */}
            {showExplanationPreview && explanation.trim() && (
              <div className="pt-3 border-t border-ds-border space-y-1.5">
                <span className="text-xs font-semibold text-ds-accent flex items-center gap-1.5">
                  <HelpCircle className="w-3.5 h-3.5" />
                  <span>Solution Explanation Preview</span>
                </span>
                <div className="p-3 rounded-ds bg-ds-surface-hover border border-ds-border text-xs text-ds-secondary leading-relaxed">
                  <MathText content={explanation} />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthoringPage;
