import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useAudioRecorder } from '../useAudioRecorder';

describe('useAudioRecorder Hook', () => {
  const originalMediaDevices = navigator.mediaDevices;

  beforeEach(() => {
    vi.restoreAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    Object.defineProperty(navigator, 'mediaDevices', {
      value: originalMediaDevices,
      writable: true,
      configurable: true,
    });
  });

  it('1 initializes with idle state', () => {
    const onTranscript = vi.fn();
    const { result } = renderHook(() =>
      useAudioRecorder({ onTranscript })
    );

    expect(result.current.isRecording).toBe(false);
    expect(result.current.isTranscribing).toBe(false);
    expect(result.current.durationSec).toBe(0);
    expect(result.current.volumeLevel).toBe(0);
    expect(result.current.errorMessage).toBeNull();
  });

  it('2 reports error when getUserMedia is not supported', async () => {
    Object.defineProperty(navigator, 'mediaDevices', {
      value: undefined,
      writable: true,
      configurable: true,
    });

    const onError = vi.fn();
    const onTranscript = vi.fn();
    const { result } = renderHook(() =>
      useAudioRecorder({ onTranscript, onError })
    );

    let started = false;
    await act(async () => {
      started = await result.current.startRecording();
    });

    expect(started).toBe(false);
    expect(result.current.errorMessage).toContain('Microphone recording is not supported');
    expect(onError).toHaveBeenCalledWith(expect.stringContaining('not supported'));
  });

  it('3 handles NotAllowedError when microphone permission is denied', async () => {
    const notAllowedError = new Error('Permission denied');
    notAllowedError.name = 'NotAllowedError';

    Object.defineProperty(navigator, 'mediaDevices', {
      value: {
        getUserMedia: vi.fn().mockRejectedValue(notAllowedError),
      },
      writable: true,
      configurable: true,
    });

    const onError = vi.fn();
    const onTranscript = vi.fn();
    const { result } = renderHook(() =>
      useAudioRecorder({ onTranscript, onError })
    );

    let started = false;
    await act(async () => {
      started = await result.current.startRecording();
    });

    expect(started).toBe(false);
    expect(result.current.errorMessage).toBe('Microphone permission denied.');
    expect(onError).toHaveBeenCalledWith('Microphone permission denied.');
  });

  it('4 cancelRecording resets state and marks cancelled', () => {
    const onTranscript = vi.fn();
    const { result } = renderHook(() =>
      useAudioRecorder({ onTranscript })
    );

    act(() => {
      result.current.cancelRecording();
    });

    expect(result.current.isRecording).toBe(false);
    expect(result.current.isTranscribing).toBe(false);
    expect(result.current.volumeLevel).toBe(0);
  });
});
