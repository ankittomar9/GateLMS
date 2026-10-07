import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../api/apiClient';
import { Lock, User, AlertCircle, Clock } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [retryCountdown, setRetryCountdown] = useState<number>(0);

  const nextPath = searchParams.get('next') || '/';

  // If already authenticated, redirect
  useEffect(() => {
    if (user) {
      navigate(nextPath, { replace: true });
    }
  }, [user, navigate, nextPath]);

  // Countdown timer for 429 rate limit
  useEffect(() => {
    if (retryCountdown <= 0) return;
    const interval = setInterval(() => {
      setRetryCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [retryCountdown]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (retryCountdown > 0 || isSubmitting) return;

    setErrorMsg(null);
    setIsSubmitting(true);

    try {
      await login({ username, password });
      navigate(nextPath, { replace: true });
    } catch (err: any) {
      if (err instanceof ApiError) {
        if (err.status === 429) {
          const waitTime = err.retryAfter && err.retryAfter > 0 ? err.retryAfter : 300;
          setRetryCountdown(waitTime);
          setErrorMsg(`Too many failed attempts. Account temporarily locked.`);
        } else if (err.status === 401) {
          setErrorMsg('Invalid credentials');
        } else {
          setErrorMsg(err.message || 'Login failed. Please try again.');
        }
      } else {
        setErrorMsg('Network error occurred. Please check your connection.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-ds-bg flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-ds-surface border border-ds-border rounded-xl p-8 shadow-xl">
        <div className="text-center mb-8">
          <div className="w-12 h-12 bg-ds-accent/10 border border-ds-accent/20 rounded-xl flex items-center justify-center mx-auto mb-3 text-ds-accent">
            <Lock className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-bold text-ds-primary">GateLMS Sign In</h1>
          <p className="text-xs text-ds-muted mt-1">Enter your credentials to access your preparation portal</p>
        </div>

        {errorMsg && (
          <div
            className="mb-6 p-3.5 bg-red-500/10 border border-red-500/30 rounded-lg flex items-start gap-2.5 text-xs text-red-500 animate-fadeIn"
            data-testid="login-error-banner"
          >
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span className="font-medium">{errorMsg}</span>
          </div>
        )}

        {retryCountdown > 0 && (
          <div
            className="mb-6 p-3.5 bg-amber-500/10 border border-amber-500/30 rounded-lg flex items-center gap-2.5 text-xs text-amber-500 font-semibold"
            data-testid="login-ratelimit-banner"
          >
            <Clock className="w-4 h-4 shrink-0 animate-pulse" />
            <span>Rate limited. Please retry in {retryCountdown}s</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-ds-muted mb-1.5" htmlFor="username">
              Username
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-ds-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
                disabled={retryCountdown > 0 || isSubmitting}
                data-testid="login-username-input"
                className="w-full bg-ds-surface-input border border-ds-border rounded-lg pl-9 pr-3 py-2 text-xs text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent transition-all disabled:opacity-50"
                placeholder="ankur"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-ds-muted mb-1.5" htmlFor="password">
              Password
            </label>
            <div className="relative">
              <Lock className="w-4 h-4 text-ds-muted absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                disabled={retryCountdown > 0 || isSubmitting}
                data-testid="login-password-input"
                className="w-full bg-ds-surface-input border border-ds-border rounded-lg pl-9 pr-3 py-2 text-xs text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent transition-all disabled:opacity-50"
                placeholder="••••••••"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={retryCountdown > 0 || isSubmitting}
            data-testid="login-submit-button"
            className="w-full mt-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast font-semibold py-2.5 px-4 rounded-lg text-xs transition-all shadow-md shadow-ds-accent/15 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {isSubmitting ? (
              <>
                <div className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin" />
                <span>Signing in...</span>
              </>
            ) : retryCountdown > 0 ? (
              `Locked (${retryCountdown}s)`
            ) : (
              'Sign In'
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
