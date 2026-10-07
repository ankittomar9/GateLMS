import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AuthoringPage } from '../AuthoringPage';
import { ApiError } from '../../api/apiClient';

const mockNavigate = vi.fn();
let mockSearchParams = new URLSearchParams();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useSearchParams: () => [mockSearchParams, vi.fn()],
  };
});

const mockSubjectsFixture = [
  { id: 'subj_algo', name: 'Algorithms', code: 'algo' },
  { id: 'subj_os', name: 'Operating Systems', code: 'os' },
];

const mockTopicsFixture = [
  { id: 'topic_algo_asymptotic', name: 'Asymptotic Analysis', subjectId: 'subj_algo' },
  { id: 'topic_algo_dp', name: 'Dynamic Programming', subjectId: 'subj_algo' },
];

const mockQuestionFixture = {
  id: 'q-exist-1234',
  externalId: 'AUTH-ALGO-2024-001',
  subjectId: 'subj_algo',
  subjectName: 'Algorithms',
  topicId: 'topic_algo_asymptotic',
  topicName: 'Asymptotic Analysis',
  year: 2024,
  type: 'MCQ',
  marks: 2,
  statement: 'What is the recurrence relation for Merge Sort? $T(n) = 2T(n/2) + O(n)$',
  optionsJson: JSON.stringify([
    { label: 'A', text: 'O(n)' },
    { label: 'B', text: 'O(n log n)' },
    { label: 'C', text: 'O(n^2)' },
    { label: 'D', text: 'O(1)' },
  ]),
  correctOptions: 'B',
  numericMin: null,
  numericMax: null,
  explanation: 'Merge sort recurrence solves to $\\Theta(n \\log n)$.',
  gateOverflowUrl: '',
  isBookmarked: false,
  pass1Status: 'PENDING',
  pass2Status: 'PENDING',
  version: 3,
};

const mockValidateMutate = vi.fn();
const mockCreateMutate = vi.fn();
const mockUpdateMutate = vi.fn();
const mockRefetchExisting = vi.fn();

vi.mock('../../hooks/useQueries', () => ({
  useSubjects: () => ({
    data: mockSubjectsFixture,
    isLoading: false,
  }),
  useTopics: (subjectId?: string) => ({
    data: subjectId === 'subj_algo' ? mockTopicsFixture : [],
    isLoading: false,
  }),
  useQuestion: (id?: string) => ({
    data: id === 'q-exist-1234' ? mockQuestionFixture : undefined,
    isLoading: false,
    refetch: mockRefetchExisting,
  }),
  useValidateQuestion: () => ({
    mutateAsync: mockValidateMutate,
    isPending: false,
  }),
  useCreateQuestion: () => ({
    mutateAsync: mockCreateMutate,
    isPending: false,
  }),
  useUpdateQuestion: () => ({
    mutateAsync: mockUpdateMutate,
    isPending: false,
  }),
}));

describe('AuthoringPage', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    mockSearchParams = new URLSearchParams();
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  const renderComponent = () =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthoringPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('type switch swaps builders between MCQ, MSQ, and NAT', () => {
    renderComponent();

    // Default is MCQ
    expect(screen.getByText(/MCQ Options \(4 to 6 rows\)/i)).toBeInTheDocument();
    expect(screen.queryByText(/Numerical Answer Range/i)).not.toBeInTheDocument();

    // Switch to MSQ
    const msqBtn = screen.getByRole('button', { name: 'MSQ' });
    fireEvent.click(msqBtn);
    expect(screen.getByText(/MSQ Options \(4 to 6 rows\)/i)).toBeInTheDocument();
    expect(screen.getByText(/Check all correct options/i)).toBeInTheDocument();

    // Switch to NAT
    const natBtn = screen.getByRole('button', { name: 'NAT' });
    fireEvent.click(natBtn);
    expect(screen.getByText(/Numerical Answer Range/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Base Answer \(e\.g\. 12\.0\)/i)).toBeInTheDocument();
  });

  it('client V-Q blocks submit with inline errors on invalid form', async () => {
    renderComponent();

    // Submit with empty fields
    const saveBtn = screen.getByRole('button', { name: /Save Question/i });
    fireEvent.click(saveBtn);

    // Assert client inline validation messages
    expect(await screen.findByText(/Statement must not be blank/i)).toBeInTheDocument();
    expect(screen.getByText(/Subject is required/i)).toBeInTheDocument();
    expect(screen.getByText(/Topic is required/i)).toBeInTheDocument();

    // Server validate mutation should NOT be called when client validation fails
    expect(mockValidateMutate).not.toHaveBeenCalled();
    expect(mockCreateMutate).not.toHaveBeenCalled();
  });

  it('live preview renders statement with MathText and KaTeX formulas', async () => {
    renderComponent();

    const statementInput = screen.getByPlaceholderText(/Enter question statement here/i);
    fireEvent.change(statementInput, {
      target: { value: 'Compute the complexity $O(n \\log n)$' },
    });

    // Wait for debounced preview update
    await waitFor(() => {
      expect(screen.getByText(/Compute the complexity/i)).toBeInTheDocument();
    });
  });

  it('cascading topic options load when subject is selected', async () => {
    renderComponent();

    const subjectSelect = screen.getByLabelText(/Subject/i);
    // Initially topics should be empty/placeholder
    expect(screen.getByText(/-- Select Topic --/i)).toBeInTheDocument();

    // Select Algorithms subject
    fireEvent.change(subjectSelect, { target: { value: 'subj_algo' } });

    // Topics dropdown now has topics
    await waitFor(() => {
      expect(screen.getByText('Asymptotic Analysis')).toBeInTheDocument();
      expect(screen.getByText('Dynamic Programming')).toBeInTheDocument();
    });
  });

  it('save flow calls validate and then creates question via POST', async () => {
    mockValidateMutate.mockResolvedValueOnce({
      valid: true,
      fieldErrors: {},
      warnings: ['Display-math ($$...$$) absent in statement'],
    });
    mockCreateMutate.mockResolvedValueOnce({
      id: 'new-created-uuid',
      externalId: 'AUTH-ALGO-2024-999',
    });

    renderComponent();

    // Fill valid form
    const subjectSelect = screen.getByLabelText(/Subject/i);
    fireEvent.change(subjectSelect, { target: { value: 'subj_algo' } });

    const topicSelect = screen.getByLabelText(/Topic/i);
    fireEvent.change(topicSelect, { target: { value: 'topic_algo_asymptotic' } });

    const statementInput = screen.getByPlaceholderText(/Enter question statement here/i);
    fireEvent.change(statementInput, { target: { value: 'Valid question statement' } });

    // Options text
    const optInputs = screen.getAllByPlaceholderText(/Option [A-D] text/i);
    optInputs.forEach((inp, idx) => {
      fireEvent.change(inp, { target: { value: `Option text ${idx}` } });
    });

    // Click Save
    const saveBtn = screen.getByRole('button', { name: /Save Question/i });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(mockValidateMutate).toHaveBeenCalledTimes(1);
      expect(mockCreateMutate).toHaveBeenCalledTimes(1);
    });

    // Success banner appears
    expect(await screen.findByText(/Question Saved Successfully!/i)).toBeInTheDocument();
  });

  it('edit mode loads existing question and handles 409 conflict banner path', async () => {
    mockSearchParams = new URLSearchParams('edit=q-exist-1234');
    mockValidateMutate.mockResolvedValueOnce({ valid: true, fieldErrors: {}, warnings: [] });
    // Simulate 409 Version Conflict on update
    mockUpdateMutate.mockRejectedValueOnce(
      new ApiError(409, 'Question version mismatch: expected 3, but got 2')
    );

    renderComponent();

    // Form populated from fixture
    expect(await screen.findByDisplayValue(/What is the recurrence relation for Merge Sort/i)).toBeInTheDocument();
    expect(screen.getByText(/Version 3/i)).toBeInTheDocument();

    // Click Update Question
    const updateBtn = screen.getByRole('button', { name: /Update Question/i });
    fireEvent.click(updateBtn);

    // 409 conflict banner should be shown
    expect(
      await screen.findByText(/Version Conflict: Question was modified by another session/i)
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Reload Latest Server State/i })).toBeInTheDocument();

    // Click reload button
    const reloadBtn = screen.getByRole('button', { name: /Reload Latest Server State/i });
    fireEvent.click(reloadBtn);
    expect(mockRefetchExisting).toHaveBeenCalledTimes(1);
  });
});
