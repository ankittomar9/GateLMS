import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AiStudioProvider, useAiStudio } from '../../../context/AiStudioContext';
import { AiStudioDrawer } from '../AiStudioDrawer';

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

  it('4 allows switching provider and stores key in BYOK settings modal', async () => {
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

    // Verify written to localStorage
    const stored = JSON.parse(localStorage.getItem('gatelms_byok_keys') || '{}');
    expect(stored['GROQ']).toBe('gsk_test_mock_secret_key');
  });
});
