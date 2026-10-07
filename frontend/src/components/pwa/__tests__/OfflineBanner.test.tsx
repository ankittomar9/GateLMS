import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { OfflineBanner } from '../OfflineBanner';

describe('OfflineBanner Component', () => {
  let originalOnLine: boolean;

  beforeEach(() => {
    originalOnLine = navigator.onLine;
  });

  afterEach(() => {
    Object.defineProperty(navigator, 'onLine', {
      value: originalOnLine,
      configurable: true,
    });
  });

  it('1. banner appears on offline event and displays status text', () => {
    Object.defineProperty(navigator, 'onLine', {
      value: true,
      configurable: true,
    });

    render(<OfflineBanner />);

    // Initially online, nothing rendered
    expect(screen.queryByTestId('offline-banner')).not.toBeInTheDocument();

    // Trigger window offline event
    act(() => {
      window.dispatchEvent(new Event('offline'));
    });

    const banner = screen.getByTestId('offline-banner');
    expect(banner).toBeInTheDocument();
    expect(banner).toHaveTextContent(/Offline — showing cached content/i);
  });

  it('2. auto-dismisses when reconnecting via online event', () => {
    Object.defineProperty(navigator, 'onLine', {
      value: false,
      configurable: true,
    });

    render(<OfflineBanner />);

    // Renders because offline initially
    expect(screen.getByTestId('offline-banner')).toBeInTheDocument();

    // Trigger window online event
    act(() => {
      window.dispatchEvent(new Event('online'));
    });

    // Auto-dismisses
    expect(screen.queryByTestId('offline-banner')).not.toBeInTheDocument();
  });

  it('3. cached-response detection shows banner when X-From-Cache event fires', () => {
    Object.defineProperty(navigator, 'onLine', {
      value: true,
      configurable: true,
    });

    render(<OfflineBanner />);
    expect(screen.queryByTestId('offline-banner')).not.toBeInTheDocument();

    // Trigger custom cached response event dispatched by apiClient on X-From-Cache header
    act(() => {
      window.dispatchEvent(new CustomEvent('gatelms:cached-response'));
    });

    expect(screen.getByTestId('offline-banner')).toBeInTheDocument();
    expect(screen.getByText(/Offline — showing cached content/i)).toBeInTheDocument();
  });

  it('4. accessibility role="status" and dismiss button functionality', () => {
    Object.defineProperty(navigator, 'onLine', {
      value: false,
      configurable: true,
    });

    render(<OfflineBanner />);

    const banner = screen.getByTestId('offline-banner');
    expect(banner).toHaveAttribute('role', 'status');

    const dismissBtn = screen.getByTestId('offline-dismiss-btn');
    fireEvent.click(dismissBtn);

    expect(screen.queryByTestId('offline-banner')).not.toBeInTheDocument();
  });
});
