import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { InstallPrompt } from '../InstallPrompt';

describe('InstallPrompt Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. install button displays when beforeinstallprompt event fires and handles prompt click', async () => {
    render(<InstallPrompt />);

    // Initially hidden
    expect(screen.queryByTestId('pwa-install-btn')).not.toBeInTheDocument();

    const mockPrompt = vi.fn().mockResolvedValue(undefined);
    const mockChoice = Promise.resolve({ outcome: 'accepted' as const, platform: 'web' });

    const event = new Event('beforeinstallprompt') as any;
    event.prompt = mockPrompt;
    event.userChoice = mockChoice;

    act(() => {
      window.dispatchEvent(event);
    });

    const installBtn = screen.getByTestId('pwa-install-btn');
    expect(installBtn).toBeInTheDocument();
    expect(installBtn).toHaveTextContent(/Install App/i);

    // Clicking fires prompt
    fireEvent.click(installBtn);
    expect(mockPrompt).toHaveBeenCalledTimes(1);
  });

  it('2. hides completely after appinstalled event fires', () => {
    render(<InstallPrompt />);

    const event = new Event('beforeinstallprompt') as any;
    event.prompt = vi.fn().mockResolvedValue(undefined);
    event.userChoice = Promise.resolve({ outcome: 'dismissed' as const, platform: 'web' });

    act(() => {
      window.dispatchEvent(event);
    });

    expect(screen.getByTestId('pwa-install-btn')).toBeInTheDocument();

    // App was installed
    act(() => {
      window.dispatchEvent(new Event('appinstalled'));
    });

    expect(screen.queryByTestId('pwa-install-btn')).not.toBeInTheDocument();
  });

  it('3. iOS hint path displays instruction popover on iOS Safari', () => {
    // Mock iOS Safari userAgent
    const originalUserAgent = window.navigator.userAgent;
    Object.defineProperty(window.navigator, 'userAgent', {
      value: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
      configurable: true,
    });

    render(<InstallPrompt />);

    const iosBtn = screen.getByTestId('ios-install-btn');
    expect(iosBtn).toBeInTheDocument();

    // Click triggers iOS hint popover
    fireEvent.click(iosBtn);

    const hint = screen.getByTestId('ios-install-hint');
    expect(hint).toBeInTheDocument();
    expect(hint).toHaveTextContent(/Tap the Share button in Safari/i);

    // Cleanup
    Object.defineProperty(window.navigator, 'userAgent', {
      value: originalUserAgent,
      configurable: true,
    });
  });
});
