import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AiStudioProvider, useAiStudio } from '../../../context/AiStudioContext';
import { AiStudioDrawer } from '../AiStudioDrawer';
import { aiApi } from '../../../api/aiApi';

const TestWrapper: React.FC<{
  initialOpen?: boolean;
  activeQuestion?: any;
}> = ({ initialOpen = true, activeQuestion = null }) => {
  return (
    <AiStudioProvider>
      <ContextSetter initialOpen={initialOpen} activeQuestion={activeQuestion} />
      <AiStudioDrawer />
    </AiStudioProvider>
  );
};

const ContextSetter: React.FC<{ initialOpen: boolean; activeQuestion: any }> = ({
  initialOpen,
  activeQuestion,
}) => {
  const { setIsOpen, registerActiveQuestion } = useAiStudio();
  React.useEffect(() => {
    setIsOpen(initialOpen);
    if (activeQuestion) {
      registerActiveQuestion(activeQuestion);
    }
  }, [initialOpen, activeQuestion, setIsOpen, registerActiveQuestion]);
  return null;
};

describe('AiStudioDrawer Component', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('1 does not render when closed', () => {
    render(<TestWrapper initialOpen={false} />);
    expect(screen.queryByTestId('ai-studio-drawer')).toBeNull();
  });

  it('2 renders sidebar drawer with header, provider selector, and input box when open', () => {
    render(<TestWrapper initialOpen={true} />);
    const drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer).toBeDefined();

    expect(screen.getByText('AI Studio')).toBeDefined();
    expect(screen.getByText('Sidecar')).toBeDefined();

    const providerSelect = screen.getByRole('combobox');
    expect(providerSelect).toBeDefined();
    expect(screen.getByPlaceholderText(/Ask question, explain approach/i)).toBeDefined();
  });

  it('3 displays active question context banner with quick action chips when registered', () => {
    const mockQuestion = {
      id: 'q-test-1',
      year: 2023,
      marks: 2,
      type: 'NAT',
      subjectName: 'Operating Systems',
      topicName: 'Paging',
      statement: 'Calculate the total number of entries in a single-level page table for 32-bit address space.',
    };

    render(<TestWrapper initialOpen={true} activeQuestion={mockQuestion} />);

    expect(screen.getByText(/GATE CS 2023/i)).toBeDefined();
    expect(screen.getByText(/2 Marks/i)).toBeDefined();
    expect(screen.getByText(/Calculate the total number of entries/i)).toBeDefined();

    expect(screen.getByText(/💡 Hint Only/i)).toBeDefined();
    expect(screen.getByText(/📐 Step-by-Step/i)).toBeDefined();
    expect(screen.getByText(/⚠️ Traps/i)).toBeDefined();
  });

  it('4 allows switching provider and stores key in BYOK settings modal (AM-13 sessionStorage)', async () => {
    const user = userEvent.setup();
    render(<TestWrapper initialOpen={true} />);

    // Open settings modal
    const settingsBtn = screen.getByTitle(/Configure API Keys & Providers/i);
    await user.click(settingsBtn);

    expect(screen.getByText(/Provider & Key Configuration/i)).toBeDefined();
    const keyInput = screen.getByPlaceholderText(/Paste GROQ API key/i);
    expect(keyInput).toBeDefined();

    await user.type(keyInput, 'gsk_test_mock_secret_key');
    expect(keyInput).toHaveValue('gsk_test_mock_secret_key');

    // Verify written to sessionStorage (AM-13)
    const stored = JSON.parse(sessionStorage.getItem('gatelms_byok_keys') || '{}');
    expect(stored['GROQ']).toBe('gsk_test_mock_secret_key');
  }, 15000);

  it('5 enforces role="complementary" and aria-label="AI Studio" accessibility landmarks', () => {
    render(<TestWrapper initialOpen={true} />);
    const drawer = screen.getByRole('complementary', { name: /AI Studio/i });
    expect(drawer).toBeDefined();
    expect(drawer.getAttribute('aria-label')).toBe('AI Studio');
  });

  it('6 PED-1 hint-gating: disables Step-by-Step button during live attempts', () => {
    const liveQuestion = {
      id: 'q-live-1',
      statement: 'Find minimum number of page faults.',
      isLiveAttempt: true,
    };

    render(<TestWrapper initialOpen={true} activeQuestion={liveQuestion} />);

    const stepBtn = screen.getByRole('button', { name: /Step-by-Step/i });
    expect(stepBtn).toBeDisabled();
    expect(stepBtn.getAttribute('title')).toBe('Unlocks after you submit the attempt');

    const hintBtn = screen.getByRole('button', { name: /Hint Only/i });
    expect(hintBtn).not.toBeDisabled();
  });

  it('7 AM-16 Esc key collapses open AI Studio drawer', async () => {
    render(<TestWrapper initialOpen={true} />);
    expect(screen.getByTestId('ai-studio-drawer')).toBeDefined();

    fireEvent.keyDown(window, { key: 'Escape', code: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByTestId('ai-studio-drawer')).toBeNull();
    });
  });

  it('8 closing drawer does not abort in-flight background generation', async () => {
    let capturedSignal: AbortSignal | undefined;
    vi.spyOn(aiApi, 'chat').mockImplementation(async (_req, _key, signal) => {
      capturedSignal = signal;
      return new Promise((resolve) => setTimeout(() => resolve({ reply: 'Done', provider: 'GROQ', model: 'llama3', latencyMs: 100 }), 500));
    });

    const user = userEvent.setup();
    render(<TestWrapper initialOpen={true} />);

    const input = screen.getByPlaceholderText(/Ask question, explain approach/i);
    await user.type(input, 'Tell me about semaphores');

    const sendBtn = screen.getByRole('button', { name: /Send message/i });
    await user.click(sendBtn);

    expect(capturedSignal).toBeDefined();
    expect(capturedSignal?.aborted).toBe(false);

    // Close drawer via close button
    const closeBtn = screen.getByRole('button', { name: /Close AI Studio/i });
    await user.click(closeBtn);

    // Assert signal is still NOT aborted — generation continues in background
    expect(capturedSignal?.aborted).toBe(false);
  });
});
