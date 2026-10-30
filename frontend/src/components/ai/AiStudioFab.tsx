import React from 'react';
import { Sparkles } from 'lucide-react';
import { useAiStudio } from '../../context/AiStudioContext';

export const AiStudioFab: React.FC<{ isLoading?: boolean }> = ({ isLoading = false }) => {
  const { isOpen, setIsOpen } = useAiStudio();

  if (isOpen) return null;

  return (
    <button
      type="button"
      data-testid="ai-studio-fab"
      onClick={() => setIsOpen(true)}
      aria-label="Open AI Studio"
      title="Open AI Studio (⌘J)"
      className="fixed bottom-6 right-6 md:bottom-6 md:right-6 max-md:bottom-4 max-md:right-4 z-30 h-12 w-12 rounded-full bg-ds-accent text-ds-accent-contrast shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all flex items-center justify-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ds-accent focus-visible:ring-offset-2 motion-reduce:transform-none cursor-pointer group"
    >
      <Sparkles className="w-5 h-5 transition-transform group-hover:rotate-12 motion-reduce:transform-none" />
      {isLoading && (
        <span
          data-testid="ai-studio-fab-pulse"
          className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-ds-accent rounded-full border-2 border-ds-surface animate-ping motion-reduce:animate-none"
        />
      )}
    </button>
  );
};

export default AiStudioFab;
