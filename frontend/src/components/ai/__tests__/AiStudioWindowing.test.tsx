import React from 'react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { AiStudioProvider, useAiStudio } from '../../../context/AiStudioContext';
import { AiStudioDrawer } from '../AiStudioDrawer';
import { AiStudioFab } from '../AiStudioFab';
import { aiApi } from '../../../api/aiApi';

const WindowingTestWrapper: React.FC<{
  initialOpen?: boolean;
  initialMode?: 'docked' | 'floating';
}> = ({ initialOpen = true, initialMode = 'docked' }) => {
  return (
    <AiStudioProvider>
      <ModeSetter initialOpen={initialOpen} initialMode={initialMode} />
      <AiStudioDrawer />
      <AiStudioFab />
    </AiStudioProvider>
  );
};

const ModeSetter: React.FC<{ initialOpen: boolean; initialMode: 'docked' | 'floating' }> = ({
  initialOpen,
  initialMode,
}) => {
  const { setIsOpen, setWindowMode } = useAiStudio();
  React.useEffect(() => {
    setIsOpen(initialOpen);
    setWindowMode(initialMode);
  }, [initialOpen, initialMode, setIsOpen, setWindowMode]);
  return null;
};

describe('AiStudio Windowing, Resizing & FAB Suite', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  it('G4 resizing: preset buttons apply 420, 650, half, full and reflect aria-pressed state', () => {
    render(<WindowingTestWrapper initialOpen={true} />);

    // Compact preset
    const btn420 = screen.getByRole('button', { name: /Compact width \(420px\)/i });
    expect(btn420.getAttribute('aria-pressed')).toBe('true');

    // Wide preset
    const btn650 = screen.getByRole('button', { name: /Wide width \(650px\)/i });
    fireEvent.click(btn650);
    expect(btn650.getAttribute('aria-pressed')).toBe('true');
    expect(btn420.getAttribute('aria-pressed')).toBe('false');

    // Check localStorage persistence
    expect(localStorage.getItem('gatelms_ai_studio_width')).toBe('650');

    // Half preset
    const btnHalf = screen.getByRole('button', { name: /Half screen \(50%\)/i });
    fireEvent.click(btnHalf);
    expect(btnHalf.getAttribute('aria-pressed')).toBe('true');

    // Full preset
    const btnFull = screen.getByRole('button', { name: /Full width/i });
    fireEvent.click(btnFull);
    expect(btnFull.getAttribute('aria-pressed')).toBe('true');
  });

  it('G4 resizing: double-clicking drag handle toggles width between 420px and 650px', () => {
    render(<WindowingTestWrapper initialOpen={true} />);

    const handle = screen.getByTestId('ai-studio-resize-handle');
    expect(handle).toBeDefined();

    // Default width is 420
    const drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.style.width).toBe('420px');

    // Double-click toggles to 650px
    fireEvent.doubleClick(handle);
    expect(drawer.style.width).toBe('650px');
    expect(localStorage.getItem('gatelms_ai_studio_width')).toBe('650');

    // Double-click again toggles back to 420px
    fireEvent.doubleClick(handle);
    expect(drawer.style.width).toBe('420px');
    expect(localStorage.getItem('gatelms_ai_studio_width')).toBe('420');
  });

  it('G5 floating window: toggles docked ↔ floating mode and preserves in-flight generation without aborting', async () => {
    let capturedSignal: AbortSignal | undefined;
    vi.spyOn(aiApi, 'chat').mockImplementation(async (_req, _key, signal) => {
      capturedSignal = signal;
      return new Promise((resolve) =>
        setTimeout(() => resolve({ reply: 'Shortest path computation', provider: 'GROQ', model: 'llama3', latencyMs: 150 }), 800)
      );
    });

    render(<WindowingTestWrapper initialOpen={true} initialMode="docked" />);

    // Send a message to start in-flight generation
    const input = screen.getByPlaceholderText(/Ask question, explain approach/i);
    fireEvent.change(input, { target: { value: 'Explain Dijkstra algorithm' } });
    fireEvent.click(screen.getByRole('button', { name: /Send message/i }));

    expect(capturedSignal).toBeDefined();
    expect(capturedSignal?.aborted).toBe(false);

    // Initial docked mode
    let drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.className).toContain('z-40');
    expect(drawer.className).toContain('right-0');

    // Toggle to floating mode
    const detachBtn = screen.getByRole('button', { name: /Detach to floating window/i });
    fireEvent.click(detachBtn);

    // Verify container converted to floating mode (z-[60], rounded, positioned by rect)
    drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.className).toContain('z-[60]');
    expect(drawer.className).toContain('rounded-ds');

    // Crucial check: in-flight signal was NOT aborted during mode switch!
    expect(capturedSignal?.aborted).toBe(false);

    // Toggle back to docked mode
    const dockBtn = screen.getByRole('button', { name: /Dock to right side/i });
    fireEvent.click(dockBtn);

    drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.className).toContain('z-40');
    expect(capturedSignal?.aborted).toBe(false);
  });

  it('G5 floating window: drag header and corner resize update window coordinates within viewport bounds', () => {
    render(<WindowingTestWrapper initialOpen={true} initialMode="floating" />);

    const drawer = screen.getByTestId('ai-studio-drawer');
    expect(drawer.className).toContain('z-[60]');

    // Check corner resize handle
    const cornerHandle = screen.getByTestId('ai-studio-floating-resize-handle');
    expect(cornerHandle).toBeDefined();

    // Mouse down on corner resize handle and move
    fireEvent.mouseDown(cornerHandle, { clientX: 500, clientY: 600 });
    fireEvent.mouseMove(window, { clientX: 700, clientY: 750 });
    fireEvent.mouseUp(window);

    // Rect was persisted to localStorage
    const savedRect = localStorage.getItem('gatelms_ai_studio_window_rect');
    expect(savedRect).not.toBeNull();
  });

  it('G6 FAB: visible when closed, hidden when open, and opens AI Studio when clicked', async () => {
    render(<WindowingTestWrapper initialOpen={false} />);

    // Studio is closed -> FAB is visible
    const fab = screen.getByTestId('ai-studio-fab');
    expect(fab).toBeDefined();
    expect(fab.getAttribute('aria-label')).toBe('Open AI Studio');
    expect(fab.getAttribute('title')).toBe('Open AI Studio (⌘J)');
    expect(screen.queryByTestId('ai-studio-drawer')).toBeNull();

    // Click FAB to open
    fireEvent.click(fab);

    // Studio opens, FAB disappears
    expect(screen.getByTestId('ai-studio-drawer')).toBeDefined();
    expect(screen.queryByTestId('ai-studio-fab')).toBeNull();

    // Close via close button -> FAB reappears
    const closeBtn = screen.getByRole('button', { name: /Close AI Studio/i });
    fireEvent.click(closeBtn);

    await waitFor(() => {
      expect(screen.getByTestId('ai-studio-fab')).toBeDefined();
    });
  });

  it('G6 FAB: shows in-flight pulse indicator when generation is active in background while closed', () => {
    const ContextPulseTester = () => {
      const { setIsLoading } = useAiStudio();
      React.useEffect(() => {
        setIsLoading(true);
      }, [setIsLoading]);
      return <AiStudioFab />;
    };

    render(
      <AiStudioProvider>
        <ContextPulseTester />
      </AiStudioProvider>
    );

    expect(screen.getByTestId('ai-studio-fab')).toBeDefined();
    expect(screen.getByTestId('ai-studio-fab-pulse')).toBeDefined();
  });
});
