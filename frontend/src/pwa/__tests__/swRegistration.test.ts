import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { registerServiceWorker } from '../registerServiceWorker';

describe('Service Worker Registration Engine', () => {
  let mockRegister: ReturnType<typeof vi.fn>;
  let originalServiceWorker: any;

  beforeEach(() => {
    vi.clearAllMocks();
    originalServiceWorker = (navigator as any).serviceWorker;

    mockRegister = vi.fn().mockResolvedValue({
      addEventListener: vi.fn(),
      installing: null,
    });

    Object.defineProperty(navigator, 'serviceWorker', {
      value: {
        register: mockRegister,
        addEventListener: vi.fn(),
      },
      configurable: true,
      writable: true,
    });
  });

  afterEach(() => {
    Object.defineProperty(navigator, 'serviceWorker', {
      value: originalServiceWorker,
      configurable: true,
      writable: true,
    });
  });

  it('1. registers service worker in PROD environment', async () => {
    const result = await registerServiceWorker({ isProd: true });

    expect(mockRegister).toHaveBeenCalledWith('/sw.js');
    expect(result).not.toBeNull();
  });

  it('2. never registers service worker in DEV environment to prevent cache poisoning', async () => {
    const result = await registerServiceWorker({ isProd: false });

    expect(mockRegister).not.toHaveBeenCalled();
    expect(result).toBeNull();
  });

  it('3. listens for controllerchange and dispatches gatelms:sw-update event', async () => {
    let controllerChangeHandler: (() => void) | null = null;

    Object.defineProperty(navigator, 'serviceWorker', {
      value: {
        register: mockRegister,
        addEventListener: vi.fn((event: string, handler: () => void) => {
          if (event === 'controllerchange') {
            controllerChangeHandler = handler;
          }
        }),
      },
      configurable: true,
      writable: true,
    });

    const updateListener = vi.fn();
    window.addEventListener('gatelms:sw-update', updateListener);

    await registerServiceWorker({ isProd: true });

    expect(controllerChangeHandler).not.toBeNull();

    // Trigger controllerchange event
    controllerChangeHandler!();

    expect(updateListener).toHaveBeenCalled();
    window.removeEventListener('gatelms:sw-update', updateListener);
  });
});
