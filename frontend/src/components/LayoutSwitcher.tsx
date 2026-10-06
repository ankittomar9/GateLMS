import React from 'react';
import { SlidersHorizontal, BookOpen, LayoutGrid } from 'lucide-react';
import { useDashboardLayout, DashboardLayoutVariant } from '../context/LayoutContext';

export const LayoutSwitcher: React.FC = () => {
  const { currentLayout, setLayout } = useDashboardLayout();

  const options: { id: DashboardLayoutVariant; label: string; icon: React.ReactNode }[] = [
    {
      id: 'minimalist',
      label: 'Minimalist',
      icon: <SlidersHorizontal className="w-3.5 h-3.5" />,
    },
    {
      id: 'editorial',
      label: 'Editorial',
      icon: <BookOpen className="w-3.5 h-3.5" />,
    },
    {
      id: 'bento',
      label: 'Bento Grid',
      icon: <LayoutGrid className="w-3.5 h-3.5" />,
    },
  ];

  return (
    <div
      className="flex items-center bg-ds-surface border border-ds-border p-1 rounded-ds gap-1"
      role="radiogroup"
      aria-label="Dashboard layout switcher"
    >
      {options.map((opt) => {
        const isActive = currentLayout === opt.id;
        return (
          <button
            key={opt.id}
            type="button"
            role="radio"
            aria-checked={isActive}
            onClick={() => setLayout(opt.id)}
            data-testid={`layout-toggle-${opt.id}`}
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-ds text-xs font-semibold transition-all duration-150 border ${
              isActive
                ? 'bg-ds-accent-soft text-ds-accent border-ds-accent/40 shadow-sm'
                : 'border-transparent text-ds-muted hover:text-ds-primary hover:bg-ds-surface-hover'
            }`}
            title={opt.label}
          >
            {opt.icon}
            <span className="hidden sm:inline">{opt.label}</span>
          </button>
        );
      })}
    </div>
  );
};
