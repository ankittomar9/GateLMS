import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { AiStudioProvider, useAiStudio } from '../AiStudioContext';

const ContextConsumer: React.FC = () => {
  const {
    isOpen,
    toggleOpen,
    width,
    setWidth,
    selectedProvider,
    setSelectedProvider,
    selectedModel,
    byokKeys,
    setByokKey,
    messages,
    addMessage,
    clearMessages,
  } = useAiStudio();

  return (
    <div>
      <div data-testid="isOpen">{String(isOpen)}</div>
      <div data-testid="width">{width}</div>
      <div data-testid="provider">{selectedProvider}</div>
      <div data-testid="model">{selectedModel}</div>
      <div data-testid="byok">{JSON.stringify(byokKeys)}</div>
      <div data-testid="messageCount">{messages.length}</div>
      <button data-testid="btn-toggle" onClick={toggleOpen}>
        Toggle
      </button>
      <button data-testid="btn-set-width-min" onClick={() => setWidth(200)}>
        Set 200
      </button>
      <button data-testid="btn-set-width-max" onClick={() => setWidth(1000)}>
        Set 1000
      </button>
      <button data-testid="btn-set-gemini" onClick={() => setSelectedProvider('GEMINI', 'gemini-3.8-flash')}>
        Set Gemini
      </button>
      <button
        data-testid="btn-set-key"
        onClick={() => setByokKey('GROQ', 'gsk_session_secret_key')}
      >
        Set GROQ Key
      </button>
      <button
        data-testid="btn-add-msg"
        onClick={() => addMessage({ role: 'user', content: 'What is Paging?' })}
      >
        Add Msg
      </button>
      <button data-testid="btn-clear" onClick={clearMessages}>
        Clear
      </button>
    </div>
  );
};

describe('AiStudioContext Suite', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('1 initializes with correct defaults and clamps width to [340, 800]', () => {
    render(
      <AiStudioProvider>
        <ContextConsumer />
      </AiStudioProvider>
    );

    expect(screen.getByTestId('isOpen').textContent).toBe('false');
    expect(screen.getByTestId('width').textContent).toBe('420');
    expect(screen.getByTestId('provider').textContent).toBe('GROQ');
    expect(screen.getByTestId('model').textContent).toBe('llama-3.3-70b-versatile');

    // Clamp min width to 340
    fireEvent.click(screen.getByTestId('btn-set-width-min'));
    expect(screen.getByTestId('width').textContent).toBe('340');

    // Clamp max width to 800
    fireEvent.click(screen.getByTestId('btn-set-width-max'));
    expect(screen.getByTestId('width').textContent).toBe('800');
  });

  it('2 AM-13 setByokKey strictly writes to sessionStorage, not localStorage', () => {
    render(
      <AiStudioProvider>
        <ContextConsumer />
      </AiStudioProvider>
    );

    fireEvent.click(screen.getByTestId('btn-set-key'));

    // Check state and sessionStorage
    expect(screen.getByTestId('byok').textContent).toContain('gsk_session_secret_key');
    const sessionItem = sessionStorage.getItem('gatelms_byok_keys');
    expect(sessionItem).toBeDefined();
    expect(JSON.parse(sessionItem!)).toEqual({ GROQ: 'gsk_session_secret_key' });

    // Assert NOT leaked to localStorage
    expect(localStorage.getItem('gatelms_byok_keys')).toBeNull();
  });

  it('3 updates selectedProvider and defaults its paired model', () => {
    render(
      <AiStudioProvider>
        <ContextConsumer />
      </AiStudioProvider>
    );

    fireEvent.click(screen.getByTestId('btn-set-gemini'));
    expect(screen.getByTestId('provider').textContent).toBe('GEMINI');
    expect(screen.getByTestId('model').textContent).toBe('gemini-3.8-flash');
  });

  it('4 global Ctrl+J / Cmd+J keyboard shortcut toggles drawer open state', () => {
    render(
      <AiStudioProvider>
        <ContextConsumer />
      </AiStudioProvider>
    );

    expect(screen.getByTestId('isOpen').textContent).toBe('false');

    act(() => {
      fireEvent.keyDown(window, { key: 'j', ctrlKey: true });
    });
    expect(screen.getByTestId('isOpen').textContent).toBe('true');

    act(() => {
      fireEvent.keyDown(window, { key: 'j', metaKey: true });
    });
    expect(screen.getByTestId('isOpen').textContent).toBe('false');
  });

  it('5 message management: addMessage appends and clearMessages restores default prompt', () => {
    render(
      <AiStudioProvider>
        <ContextConsumer />
      </AiStudioProvider>
    );

    // Initial default assistant welcome message
    expect(screen.getByTestId('messageCount').textContent).toBe('1');

    fireEvent.click(screen.getByTestId('btn-add-msg'));
    expect(screen.getByTestId('messageCount').textContent).toBe('2');

    fireEvent.click(screen.getByTestId('btn-clear'));
    expect(screen.getByTestId('messageCount').textContent).toBe('1');
  });
});
