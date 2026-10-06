import React, { useState, useRef, useEffect } from 'react';
import { Palette, Check, ChevronDown } from 'lucide-react';
import { useDesign, VALID_DESIGNS, DESIGN_META, DesignPreset } from '../context/DesignContext';
import { useDashboardLayout } from '../context/LayoutContext';

export const DesignSwitcher: React.FC = () => {
  const { currentDesign, setDesign } = useDesign();
  const { setLayout } = useDashboardLayout();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  // Close popover on outside click or Esc key
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
        triggerRef.current?.focus();
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const handleSelect = (preset: DesignPreset) => {
    setDesign(preset);
    setLayout(DESIGN_META[preset].pairedLayout);
    setIsOpen(false);
    triggerRef.current?.focus();
  };

  const currentMeta = DESIGN_META[currentDesign] || DESIGN_META.tuf;

  return (
    <div className="relative inline-block text-left" ref={containerRef}>
      {/* Trigger Button */}
      <button
        ref={triggerRef}
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        aria-haspopup="true"
        aria-expanded={isOpen}
        aria-label="Select design preset"
        data-testid="design-switcher-trigger"
        className="flex items-center gap-2 bg-ds-surface hover:bg-ds-surface-hover text-ds-primary border border-ds-border px-3 py-1.5 rounded-ds text-xs font-medium transition-all shadow-sm"
      >
        <Palette className="w-3.5 h-3.5 text-ds-accent" />
        <span>{currentMeta.label}</span>
        <ChevronDown className={`w-3 h-3 text-ds-muted transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
      </button>

      {/* Popover Dropdown */}
      {isOpen && (
        <div
          role="radiogroup"
          aria-label="Design presets"
          className="absolute left-0 mt-1.5 w-48 rounded-ds bg-ds-surface border border-ds-border-strong shadow-xl py-1.5 z-50 focus:outline-none"
        >
          {VALID_DESIGNS.map((preset) => {
            const meta = DESIGN_META[preset];
            const isActive = currentDesign === preset;

            return (
              <button
                key={preset}
                type="button"
                role="radio"
                aria-checked={isActive}
                data-testid={`design-toggle-${preset}`}
                onClick={() => handleSelect(preset)}
                className={`w-full flex items-center justify-between px-3 py-2 text-xs transition-colors ${
                  isActive
                    ? 'bg-ds-surface-raised text-ds-primary font-semibold'
                    : 'text-ds-secondary hover:bg-ds-surface-hover hover:text-ds-primary'
                }`}
              >
                <div className="flex items-center gap-2.5">
                  {/* 3 Swatch Dots */}
                  <div className="flex items-center gap-1">
                    {meta.swatch.map((color, idx) => (
                      <span
                        key={idx}
                        style={{ backgroundColor: color }}
                        className="w-2.5 h-2.5 rounded-full inline-block border border-ds-border"
                      />
                    ))}
                  </div>
                  <span>{meta.label}</span>
                </div>

                {isActive && <Check className="w-3.5 h-3.5 text-ds-accent" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
