import React, { useState, useEffect } from 'react';
import {
  AlertTriangle,
  Loader2,
  ArrowRight,
} from 'lucide-react';
import { journeyApi } from '../api/journeyApi';
import { TopicConceptCoverageResponse } from '../types/journey';
import { ConceptLessonViewer } from './ConceptLessonViewer';

interface TopicConceptsTabProps {
  topicId: string;
  isAdmin: boolean;
  selectedSlug?: string | null;
  onSelectSlug?: (slug: string | null) => void;
}

export const TopicConceptsTab: React.FC<TopicConceptsTabProps> = ({
  topicId,
  isAdmin,
  selectedSlug: externalSelectedSlug,
  onSelectSlug,
}) => {
  const [internalSelectedSlug, setInternalSelectedSlug] = useState<string | null>(null);
  const currentSlug = externalSelectedSlug !== undefined ? externalSelectedSlug : internalSelectedSlug;

  const setSelectedSlug = (slug: string | null) => {
    if (onSelectSlug) {
      onSelectSlug(slug);
    } else {
      setInternalSelectedSlug(slug);
    }
  };

  const [coverageData, setCoverageData] = useState<TopicConceptCoverageResponse | null>(null);
  const [loadingCoverage, setLoadingCoverage] = useState<boolean>(true);
  const [coverageError, setCoverageError] = useState<string | null>(null);

  const loadCoverage = async () => {
    if (!topicId) return;
    try {
      setLoadingCoverage(true);
      setCoverageError(null);
      const data = await journeyApi.getTopicConcepts(topicId);
      setCoverageData(data);
    } catch (err: any) {
      setCoverageError(err?.message || 'Failed to load concept coverage data');
    } finally {
      setLoadingCoverage(false);
    }
  };

  useEffect(() => {
    loadCoverage();
  }, [topicId]);

  if (currentSlug) {
    return (
      <ConceptLessonViewer
        slug={currentSlug}
        isAdmin={isAdmin}
        onBack={() => setSelectedSlug(null)}
        onLessonMutated={loadCoverage}
      />
    );
  }

  const summary = coverageData?.summary;
  const concepts = coverageData?.concepts || [];

  return (
    <div className="space-y-6" data-testid="concepts-tab-content">
      {/* Concept Coverage Summary Bar */}
      {summary && (
        <div
          className="grid grid-cols-1 sm:grid-cols-3 gap-4 p-4 rounded-ds bg-ds-surface border border-ds-border"
          data-testid="coverage-summary-bar"
        >
          <div className="space-y-0.5">
            <span className="text-xs font-mono text-ds-muted uppercase">Total Concepts</span>
            <div className="text-xl font-bold font-mono text-ds-primary" data-testid="stat-concepts-total">
              {summary.conceptsTotal}
            </div>
          </div>
          <div className="space-y-0.5">
            <span className="text-xs font-mono text-ds-muted uppercase">With Material</span>
            <div className="text-xl font-bold font-mono text-ds-info" data-testid="stat-concepts-material">
              {summary.conceptsWithMaterial}
            </div>
          </div>
          <div className="space-y-0.5">
            <span className="text-xs font-mono text-ds-muted uppercase">Explained</span>
            <div className="text-xl font-bold font-mono text-ds-success" data-testid="stat-concepts-explained">
              {summary.conceptsExplained}
            </div>
          </div>
        </div>
      )}

      {/* Error state */}
      {coverageError && (
        <div className="p-4 rounded-ds bg-ds-danger/10 border border-ds-danger/25 text-ds-danger text-xs flex items-center gap-2">
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <span>{coverageError}</span>
        </div>
      )}

      {/* Loading state */}
      {loadingCoverage && (
        <div className="flex flex-col items-center justify-center py-16 text-ds-muted space-y-3">
          <Loader2 className="w-8 h-8 animate-spin text-ds-accent" />
          <p className="text-xs font-mono">Loading Topic Concepts...</p>
        </div>
      )}

      {/* Concepts Grid */}
      {!loadingCoverage && concepts.length === 0 && (
        <div className="p-8 text-center text-xs text-ds-muted italic border border-ds-border rounded-ds">
          No canonical concepts registered for this topic yet.
        </div>
      )}

      {!loadingCoverage && concepts.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4" data-testid="concepts-grid">
          {concepts.map((concept) => {
            const isExplained = concept.coverage === 'EXPLAINED';
            const isMapped = concept.coverage === 'MAPPED';

            let coverageBadgeStyle = 'bg-neutral-500/10 text-neutral-400 border-neutral-500/20';
            if (isExplained) {
              coverageBadgeStyle = 'bg-ds-success/15 text-ds-success border-ds-success/30';
            } else if (isMapped) {
              coverageBadgeStyle = 'bg-ds-accent-soft text-ds-accent border-ds-accent/30';
            }

            return (
              <div
                key={concept.slug}
                onClick={() => setSelectedSlug(concept.slug)}
                className="p-5 rounded-ds bg-ds-surface border border-ds-border hover:border-ds-accent/50 hover:bg-ds-surface-hover cursor-pointer transition-all flex flex-col justify-between space-y-4 group"
                data-testid={`concept-card-${concept.slug}`}
              >
                <div className="space-y-2">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="text-sm font-bold text-ds-primary group-hover:text-ds-accent transition-colors">
                      {concept.label}
                    </h3>
                    <span
                      data-testid={`coverage-chip-${concept.coverage}`}
                      className={`px-2 py-0.5 text-[11px] font-mono font-semibold rounded-full border shrink-0 ${coverageBadgeStyle}`}
                    >
                      {concept.coverage}
                    </span>
                  </div>
                  <p className="text-xs text-ds-muted font-mono">
                    concept/{concept.slug}
                  </p>
                </div>

                <div className="flex items-center justify-between pt-3 border-t border-ds-border/60 text-xs font-mono">
                  <span className="text-ds-secondary">
                    <strong className="text-ds-primary">{concept.mappedQuestionCount}</strong> mapped ({concept.uniqueStatementCount} unique)
                  </span>
                  <div className="flex items-center gap-1.5 text-ds-muted group-hover:text-ds-accent transition-colors">
                    <span>
                      {concept.lessonStatus === 'ACCEPTED'
                        ? 'Curated Lesson'
                        : concept.lessonStatus === 'DRAFT'
                        ? 'Draft Lesson'
                        : 'View Lesson'}
                    </span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
