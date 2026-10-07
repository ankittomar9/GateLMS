import React from 'react';
import { useRouteError, useNavigate } from 'react-router-dom';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

export const RouteErrorBoundary: React.FC = () => {
  const error = useRouteError() as any;
  const navigate = useNavigate();

  const errorMessage =
    error?.statusText ||
    error?.message ||
    (typeof error === 'string' ? error : 'An unexpected rendering error occurred.');

  return (
    <div className="min-h-[50vh] flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-ds-surface border border-ds-danger/30 rounded-ds p-6 shadow-md text-center space-y-4">
        <div className="w-12 h-12 rounded-full bg-ds-danger/10 text-ds-danger flex items-center justify-center mx-auto">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-ds-primary tracking-tight">Something went wrong</h2>
          <p className="text-xs text-ds-secondary mt-1">
            The page encountered an error while rendering.
          </p>
        </div>

        <div className="bg-ds-surface-raised border border-ds-border rounded-ds p-3 text-left">
          <p className="text-xs font-mono text-ds-danger break-words">
            {errorMessage}
          </p>
        </div>

        <div className="flex items-center justify-center gap-3 pt-2">
          <button
            onClick={() => window.location.reload()}
            className="flex items-center gap-1.5 px-4 py-2 bg-ds-surface border border-ds-border hover:bg-ds-surface-hover text-ds-primary text-xs font-medium rounded-ds transition-all"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reload Page</span>
          </button>
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-1.5 px-4 py-2 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast text-xs font-medium rounded-ds transition-all shadow-sm"
          >
            <Home className="w-3.5 h-3.5" />
            <span>Dashboard</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default RouteErrorBoundary;
