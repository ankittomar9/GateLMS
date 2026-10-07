import React, { useState } from 'react';
import { Search, Plus, ChevronDown, LogOut, Shield, Sparkles } from 'lucide-react';
import { DesignSwitcher } from './DesignSwitcher';
import { LayoutSwitcher } from './LayoutSwitcher';
import { useDesign } from '../context/DesignContext';
import { StreakChip } from './engagement/StreakChip';
import { NotificationBell } from './engagement/NotificationBell';
import { InstallPrompt } from './pwa/InstallPrompt';
import { useAuth } from '../context/AuthContext';
import { useAiStudio } from '../context/AiStudioContext';
import { useNavigate } from 'react-router-dom';

export const Header: React.FC = () => {
  const { currentDesign } = useDesign();
  const { user, logout } = useAuth();
  const { isOpen, toggleOpen } = useAiStudio();
  const navigate = useNavigate();
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const displayName = user?.displayName || user?.username || 'Aspirant';
  const initial = displayName.charAt(0).toUpperCase();
  const role = user?.role || 'STUDENT';

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch {
      navigate('/login');
    }
  };

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

        {/* PWA Install Prompt Button */}
        <InstallPrompt />

        {/* AI Studio Sidecar Toggle Button */}
        <button
          type="button"
          onClick={toggleOpen}
          data-testid="header-ai-studio-button"
          title="Toggle AI Studio Sidecar (⌘J)"
          className={`flex items-center gap-1.5 px-2.5 py-1.5 rounded-ds text-xs font-bold transition-all border ${
            isOpen
              ? 'bg-ds-accent text-ds-accent-contrast border-ds-accent shadow-sm'
              : 'bg-ds-surface border-ds-border text-ds-primary hover:border-ds-accent/60 hover:bg-ds-surface-raised'
          }`}
        >
          <Sparkles className="w-3.5 h-3.5" />
          <span className="hidden md:inline">AI Studio</span>
          <kbd className="hidden lg:inline text-[9px] font-mono px-1 py-0.2 rounded bg-ds-surface-raised border border-ds-border text-ds-muted">
            ⌘J
          </kbd>
        </button>

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
        <div className="relative">
          <div
            className="flex items-center gap-2 pl-1 sm:pl-2 border-l border-ds-border text-xs font-medium cursor-pointer select-none"
            data-testid="header-profile-chip"
            onClick={() => setDropdownOpen((prev) => !prev)}
          >
            <div className="w-7 h-7 rounded-full bg-ds-accent text-ds-accent-contrast font-bold flex items-center justify-center text-xs shadow-sm">
              {initial}
            </div>
            <div className="hidden md:flex flex-col items-start leading-none gap-0.5">
              <span className="text-ds-primary font-semibold" data-testid="header-display-name">
                {displayName}
              </span>
              <span
                className="text-[9px] uppercase px-1 py-0.2 rounded font-bold bg-ds-surface-raised border border-ds-border text-ds-muted"
                data-testid="header-role-badge"
              >
                {role}
              </span>
            </div>
            <ChevronDown className={`w-3 h-3 text-ds-muted hidden md:inline transition-transform duration-200 ${dropdownOpen ? 'rotate-180' : ''}`} />
          </div>

          {dropdownOpen && (
            <div
              className="absolute right-0 mt-2 w-48 bg-ds-surface border border-ds-border rounded-lg shadow-xl py-1.5 z-50 animate-fadeIn"
              data-testid="header-profile-dropdown"
            >
              <div className="px-3 py-2 border-b border-ds-border">
                <p className="text-xs font-bold text-ds-primary truncate">{displayName}</p>
                <p className="text-[10px] text-ds-muted truncate">@{user?.username || 'user'}</p>
                <div className="mt-1 inline-flex items-center gap-1 text-[9px] font-semibold text-ds-accent bg-ds-accent/10 px-1.5 py-0.5 rounded">
                  <Shield className="w-2.5 h-2.5" />
                  <span>{role}</span>
                </div>
              </div>
              <button
                type="button"
                onClick={handleLogout}
                data-testid="header-logout-button"
                className="w-full text-left px-3 py-2 text-xs text-red-500 hover:bg-red-500/10 flex items-center gap-2 transition-colors font-medium"
              >
                <LogOut className="w-3.5 h-3.5" />
                <span>Sign Out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
