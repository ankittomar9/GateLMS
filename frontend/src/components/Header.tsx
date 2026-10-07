import React from 'react';
import { Search, Plus, ChevronDown } from 'lucide-react';
import { DesignSwitcher } from './DesignSwitcher';
import { LayoutSwitcher } from './LayoutSwitcher';
import { useDesign } from '../context/DesignContext';
import { StreakChip } from './engagement/StreakChip';
import { NotificationBell } from './engagement/NotificationBell';

export const Header: React.FC = () => {
  const { currentDesign } = useDesign();

  return (
    <header className="h-16 border-b border-ds-border bg-ds-bg/95 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-30 transition-colors">
      {/* Search Input (Ctrl + K) */}
      <div className="relative w-64 sm:w-80 md:w-96">
        <Search className="w-4 h-4 text-ds-muted absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          placeholder="Search topics, questions, or anything..."
          className="w-full bg-ds-surface-input border border-ds-border rounded-ds pl-9 pr-12 py-1.5 text-xs text-ds-primary placeholder:text-ds-muted focus:outline-none focus:border-ds-accent/60 transition-all"
        />
        <kbd className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] bg-ds-surface-raised text-ds-muted px-1.5 py-0.5 rounded border border-ds-border-strong font-mono">
          ⌘K
        </kbd>
      </div>

      {/* Header Actions: Design Switcher, Layout Switcher, Universal Badges & Profile */}
      <div className="flex items-center gap-2.5 sm:gap-3.5">
        {/* Design Preset Switcher (Immediately left of LayoutSwitcher) */}
        <DesignSwitcher />

        {/* Client-Side Layout Switcher Engine */}
        <LayoutSwitcher />

        {/* Universal Real Streak Chip */}
        <StreakChip />

        {/* Universal Notification Bell Center */}
        <NotificationBell />

        {/* Quick Action (TUF Classic only) */}
        {currentDesign === 'tuf' && (
          <button
            type="button"
            className="flex items-center gap-1.5 bg-ds-accent hover:bg-ds-accent-hover text-ds-accent-contrast px-3.5 py-1.5 rounded-ds text-xs font-bold transition-all shadow-md shadow-ds-accent/10"
          >
            <Plus className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Quick Log</span>
          </button>
        )}

        {/* User Profile Chip (All 4 designs) */}
        <div
          className="flex items-center gap-2 pl-1 sm:pl-2 border-l border-ds-border text-xs font-medium cursor-pointer"
          data-testid="header-profile-chip"
        >
          <div className="w-7 h-7 rounded-full bg-ds-accent text-ds-accent-contrast font-bold flex items-center justify-center text-xs shadow-sm">
            A
          </div>
          <span className="text-ds-primary hidden md:inline font-semibold">Aspirant</span>
          <ChevronDown className="w-3 h-3 text-ds-muted hidden md:inline" />
        </div>
      </div>
    </header>
  );
};
