import React from 'react';
import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AiStudioProvider, useAiStudio } from '../../../context/AiStudioContext';
import { AiStudioDrawer } from '../AiStudioDrawer';

const TestWrapper: React.FC<{
  initialOpen?: boolean;
  selectedProvider?: string;
}> = ({ initialOpen = true, selectedProvider }) => {
  return (
    <AiStudioProvider>
      <ContextInit initialOpen={initialOpen} selectedProvider={selectedProvider} />
      <AiStudioDrawer />
    </AiStudioProvider>
  );
};

const ContextInit: React.FC<{ initialOpen: boolean; selectedProvider?: string }> = ({
  initialOpen,
  selectedProvider,
}) => {
  const { setIsOpen, setSelectedProvider } = useAiStudio();
  React.useEffect(() => {
    setIsOpen(initialOpen);
    if (selectedProvider) {
      setSelectedProvider(selectedProvider);
    }
  }, [initialOpen, selectedProvider, setIsOpen, setSelectedProvider]);
  return null;
};

describe('AiStudioMobileAndSettings Suite', () => {
  const originalInnerWidth = window.innerWidth;

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: originalInnerWidth,
    });
  });

  it('1 AM-16 mobile viewport (<768px): renders full-width sheet and omits drag handle', () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 600,
    });

    render(<TestWrapper initialOpen={true} />);

    const drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.style.width).toBe('100%');
    expect(screen.queryByTitle('Drag to resize AI Studio')).toBeNull();
  });

  it('2 desktop viewport (>=768px): renders drag resize handle', () => {
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 1200,
    });

    render(<TestWrapper initialOpen={true} />);

    const drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.style.width).toBe('420px');
    expect(screen.getByTitle('Drag to resize AI Studio')).toBeDefined();
  });

  it('3 AM-12 renders privacy disclosure notice inside settings modal', async () => {
    const user = userEvent.setup();
    render(<TestWrapper initialOpen={true} />);

    const settingsBtn = screen.getByTitle(/Configure API Keys & Providers/i);
    await user.click(settingsBtn);

    expect(screen.getByText(/Privacy Disclosure/i)).toBeDefined();
    expect(screen.getByText(/Cloud providers receive your question text and prompts/i)).toBeDefined();
    expect(screen.getByText(/zero egress/i)).toBeDefined();
  });

  it('4 disables API key input when OLLAMA provider is active', async () => {
    const user = userEvent.setup();
    render(<TestWrapper initialOpen={true} selectedProvider="OLLAMA" />);

    const settingsBtn = screen.getByTitle(/Configure API Keys & Providers/i);
    await user.click(settingsBtn);

    const keyInput = screen.getByPlaceholderText(/No key needed for Local Ollama/i);
    expect(keyInput).toBeDisabled();
  });

  it('5 send button is disabled when prompt input is empty', () => {
    render(<TestWrapper initialOpen={true} />);

    const sendBtn = screen.getByRole('button', { name: /Send message/i });
    expect(sendBtn).toBeDisabled();
  });
});
