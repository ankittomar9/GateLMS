import React from 'react';
import { SlidersHorizontal, BookOpen, LayoutGrid } from 'lucide-react';
import { useDashboardLayout, DashboardLayoutVariant } from '../context/LayoutContext';

export const LayoutSwitcher: React.FC = () => {
  const { currentLayout, setLayout } = useDashboardLayout();

  const options: { id: DashboardLayoutVariant; label: string; icon: React.ReactNode; activeClass: string }[] = [
    {
      id: 'minimalist',
      label: 'Minimalist',
      icon: <SlidersHorizontal className="w-3.5 h-3.5" />,
      activeClass: 'bg-blue-500/20 text-blue-400 border-blue-500/40 shadow-sm shadow-blue-500/10'
    },
    {
      id: 'editorial',
      label: 'Editorial',
      icon: <BookOpen className="w-3.5 h-3.5" />,
      activeClass: 'bg-indigo-500/20 text-indigo-400 border-indigo-500/40 shadow-sm shadow-indigo-500/10'
    },
    {
      id: 'bento',
      label: 'Bento Grid',
      icon: <LayoutGrid className="w-3.5 h-3.5" />,
      activeClass: 'bg-amber-500/20 text-amber-400 border-amber-500/40 shadow-sm shadow-amber-500/10'
    }
  ];

  return (
    <div
      className="flex items-center bg-[#141414] border border-[#262626] p-1 rounded-xl gap-1"
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
            className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-all duration-150 border ${
              isActive
                ? opt.activeClass
                : 'border-transparent text-zinc-400 hover:text-zinc-200 hover:bg-[#1f1f1f]'
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
