import { describe, it, expect } from 'vitest';
import { encodePcm16Wav } from '../audio-encoder';

describe('audio-encoder', () => {
  it('encodes Float32Array into valid 16kHz mono 16-bit PCM WAV blob', async () => {
    // Generate 16000 samples (1 second at 16kHz)
    const samples = new Float32Array(16000);
    for (let i = 0; i < samples.length; i++) {
      samples[i] = Math.sin((2 * Math.PI * 440 * i) / 16000); // 440Hz tone
    }

    const wavBlob = encodePcm16Wav(samples, 16000);
    expect(wavBlob).toBeDefined();
    expect(wavBlob.type).toBe('audio/wav');

    // Expected size: 44 bytes header + (16000 samples * 2 bytes/sample) = 32044 bytes
    expect(wavBlob.size).toBe(32044);

    const buffer = await wavBlob.arrayBuffer();
    const view = new DataView(buffer);

    // Verify RIFF header
    const riff = String.fromCharCode(view.getUint8(0), view.getUint8(1), view.getUint8(2), view.getUint8(3));
    expect(riff).toBe('RIFF');

    // Verify WAVE tag
    const wave = String.fromCharCode(view.getUint8(8), view.getUint8(9), view.getUint8(10), view.getUint8(11));
    expect(wave).toBe('WAVE');

    // Verify mono channel count
    expect(view.getUint16(22, true)).toBe(1);

    // Verify 16kHz sample rate
    expect(view.getUint32(24, true)).toBe(16000);

    // Verify 16 bits per sample
    expect(view.getUint16(34, true)).toBe(16);
  });
});
