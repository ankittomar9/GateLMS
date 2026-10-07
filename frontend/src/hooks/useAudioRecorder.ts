import { useState, useRef, useCallback, useEffect } from 'react';
import { convertBlobTo16kHzWav } from '../lib/audio-encoder';
import { aiApi } from '../api/aiApi';

export interface UseAudioRecorderOptions {
  apiKey?: string;
  provider?: string;
  promptContext?: string;
  onTranscript: (text: string) => void;
  onError?: (error: string) => void;
}

export interface UseAudioRecorderReturn {
  isRecording: boolean;
  isTranscribing: boolean;
  durationSec: number;
  volumeLevel: number; // 0.0 to 1.0 for live waveform
  errorMessage: string | null;
  startRecording: () => Promise<boolean>;
  stopRecording: () => Promise<void>;
  cancelRecording: () => void;
}

export function useAudioRecorder({
  apiKey,
  provider,
  promptContext,
  onTranscript,
  onError,
}: UseAudioRecorderOptions): UseAudioRecorderReturn {
  const [isRecording, setIsRecording] = useState(false);
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [durationSec, setDurationSec] = useState(0);
  const [volumeLevel, setVolumeLevel] = useState(0);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const mediaStreamRef = useRef<MediaStream | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<Blob[]>([]);
  const timerIntervalRef = useRef<number | null>(null);
  const audioContextRef = useRef<AudioContext | null>(null);
  const analyserRef = useRef<AnalyserNode | null>(null);
  const animFrameRef = useRef<number | null>(null);
  const startTimeRef = useRef<number>(0);
  const isCancelledRef = useRef<boolean>(false);

  const cleanupAudioNodes = useCallback(() => {
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current);
      animFrameRef.current = null;
    }
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
    if (audioContextRef.current) {
      try {
        void audioContextRef.current.close();
      } catch {}
      audioContextRef.current = null;
    }
    if (mediaStreamRef.current) {
      mediaStreamRef.current.getTracks().forEach((track) => track.stop());
      mediaStreamRef.current = null;
    }
    mediaRecorderRef.current = null;
    setVolumeLevel(0);
  }, []);

  useEffect(() => {
    return () => {
      cleanupAudioNodes();
    };
  }, [cleanupAudioNodes]);

  const startRecording = useCallback(async (): Promise<boolean> => {
    setErrorMessage(null);
    isCancelledRef.current = false;
    audioChunksRef.current = [];

    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error('Microphone recording is not supported in this browser.');
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          channelCount: 1,
        },
      });

      mediaStreamRef.current = stream;

      // Setup audio analyzer for volume visualization
      try {
        const AudioCtxClass = window.AudioContext || (window as any).webkitAudioContext;
        if (AudioCtxClass) {
          const ctx = new AudioCtxClass();
          audioContextRef.current = ctx;
          const source = ctx.createMediaStreamSource(stream);
          const analyser = ctx.createAnalyser();
          analyser.fftSize = 64;
          source.connect(analyser);
          analyserRef.current = analyser;

          const dataArray = new Uint8Array(analyser.frequencyBinCount);
          const updateVolume = () => {
            if (!analyserRef.current) return;
            analyserRef.current.getByteFrequencyData(dataArray);
            let sum = 0;
            for (let i = 0; i < dataArray.length; i++) {
              sum += dataArray[i];
            }
            const avg = sum / dataArray.length;
            setVolumeLevel(Math.min(1, avg / 128));
            animFrameRef.current = requestAnimationFrame(updateVolume);
          };
          animFrameRef.current = requestAnimationFrame(updateVolume);
        }
      } catch (err) {
        console.warn('Audio analyzer init notice:', err);
      }

      const recorder = new MediaRecorder(stream, { mimeType: 'audio/webm' });
      mediaRecorderRef.current = recorder;

      recorder.ondataavailable = (e) => {
        if (!isCancelledRef.current && e.data.size > 0) {
          audioChunksRef.current.push(e.data);
        }
      };

      recorder.start(100);
      startTimeRef.current = Date.now();
      setIsRecording(true);
      setDurationSec(0);

      timerIntervalRef.current = window.setInterval(() => {
        const elapsed = Math.floor((Date.now() - startTimeRef.current) / 1000);
        setDurationSec(elapsed);
        if (elapsed >= 60) {
          stopRecording();
          onError?.('Maximum recording duration (60s) reached.');
        }
      }, 500);

      return true;
    } catch (err: any) {
      const msg = err.name === 'NotAllowedError' ? 'Microphone permission denied.' : (err.message || 'Failed to access microphone.');
      setErrorMessage(msg);
      onError?.(msg);
      cleanupAudioNodes();
      setIsRecording(false);
      return false;
    }
  }, [onError, cleanupAudioNodes]);

  const stopRecording = useCallback(async (): Promise<void> => {
    if (!mediaRecorderRef.current || !isRecording) return;

    const elapsedMs = Date.now() - startTimeRef.current;
    setIsRecording(false);

    if (elapsedMs < 300) {
      // Guard accidental micro-tap
      cleanupAudioNodes();
      return;
    }

    return new Promise((resolve) => {
      const recorder = mediaRecorderRef.current;
      if (!recorder) {
        cleanupAudioNodes();
        resolve();
        return;
      }

      recorder.onstop = async () => {
        cleanupAudioNodes();
        if (isCancelledRef.current || audioChunksRef.current.length === 0) {
          resolve();
          return;
        }

        const rawBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
        audioChunksRef.current = [];

        setIsTranscribing(true);
        setErrorMessage(null);

        try {
          // Convert WebM to 16kHz PCM WAV for neural accuracy
          let uploadBlob: Blob = rawBlob;
          try {
            uploadBlob = await convertBlobTo16kHzWav(rawBlob);
          } catch (convErr) {
            console.warn('16kHz WAV conversion fallback to WebM:', convErr);
          }

          const response = await aiApi.transcribeAudio(uploadBlob, {
            apiKey,
            provider,
            promptContext,
          });

          if (response.status === 'TOO_SHORT') {
            setErrorMessage(response.message || 'Recording was too short.');
            onError?.(response.message || 'Recording was too short.');
          } else if (response.text && response.text.trim()) {
            onTranscript(response.text.trim());
          } else {
            setErrorMessage('No speech detected.');
          }
        } catch (err: any) {
          const msg = err.message || 'Speech transcription failed.';
          setErrorMessage(msg);
          onError?.(msg);
        } finally {
          setIsTranscribing(false);
          resolve();
        }
      };

      try {
        recorder.stop();
      } catch {
        cleanupAudioNodes();
        resolve();
      }
    });
  }, [isRecording, apiKey, provider, promptContext, onTranscript, onError, cleanupAudioNodes]);

  const cancelRecording = useCallback(() => {
    isCancelledRef.current = true;
    setIsRecording(false);
    setIsTranscribing(false);
    cleanupAudioNodes();
  }, [cleanupAudioNodes]);

  return {
    isRecording,
    isTranscribing,
    durationSec,
    volumeLevel,
    errorMessage,
    startRecording,
    stopRecording,
    cancelRecording,
  };
}
