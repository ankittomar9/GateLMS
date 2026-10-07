import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  FileClock, 
  Play, 
  Layers, 
  ArrowRight, 
  AlertCircle, 
  CheckCircle2, 
  ChevronRight, 
  BookOpen, 
  Filter,
  BarChart3,
  Flame,
  Award
} from 'lucide-react';
import { usePypStats, useGeneratePypMock } from '../hooks/useQueries';
import { PypYearStatsDto } from '../api/types';

export const PYPPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: stats, isLoading, error } = usePypStats();
  const generateMock = useGeneratePypMock();

  const [selectedYear, setSelectedYear] = useState<number | null>(null);
  const [startingYear, setStartingYear] = useState<number | null>(null);

  const years = stats?.years || [];
  const fullPaperYears = new Set(stats?.coverage.fullPaperYears || []);

  const activeYearData: PypYearStatsDto | undefined = selectedYear 
    ? years.find((y) => y.year === selectedYear)
    : years[0];

  const handleAttemptPaper = async (year: number) => {
    try {
      setStartingYear(year);
      const template = await generateMock.mutateAsync(year);
      
      // Start the mock attempt immediately using the generated template ID
      const response = await fetch(`/api/v1/mocks/${template.id}/attempts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) throw new Error('Failed to start attempt');
      const data = await response.json();
      navigate(`/mocks/attempts/${data.attemptId}`);
    } catch (err) {
      console.error(err);
      alert('Could not start mock attempt. Please check template status.');
    } finally {
      setStartingYear(null);
    }
  };

  const handleDrillYear = (year: number, subjectSlug?: string) => {
    if (subjectSlug) {
      navigate(`/practice?year=${year}&subject=${subjectSlug}`);
    } else {
      navigate(`/practice?year=${year}`);
    }
  };

  return (
    <div className="space-y-8" data-testid="pyp-page">
      {/* Top Banner */}
      <div className="bg-ds-surface border border-ds-border rounded-ds p-6 shadow-sm">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <FileClock className="w-5 h-5 text-ds-accent" />
              <h1 className="text-xl font-bold text-ds-primary tracking-tight">GATE Previous Year Papers (PYP)</h1>
            </div>
            <p className="text-ds-secondary text-sm max-w-2xl leading-relaxed">
              Official GATE CSE question papers with 100% source attribution to GateOverflow discussions. 
              Attempt official 3-hour full length papers or drill year-by-year topics in practice mode.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 font-mono text-xs text-ds-secondary bg-ds-surface-raised px-3 py-1.5 rounded-ds border border-ds-border">
              <span>Full Papers:</span>
              <strong className="text-ds-accent">{stats?.coverage.fullPaperYears.length ?? '...'} Years</strong>
            </div>
            <div className="flex items-center gap-2 font-mono text-xs text-ds-secondary bg-ds-surface-raised px-3 py-1.5 rounded-ds border border-ds-border">
              <span>Total Available:</span>
              <strong className="text-ds-primary">{years.reduce((acc, curr) => acc + curr.total, 0)} Qs</strong>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-ds-danger/10 border border-ds-danger/30 text-ds-danger p-4 rounded-ds text-sm flex items-center gap-2">
          <AlertCircle className="w-5 h-5 shrink-0" />
          <span>Failed to load previous year paper statistics. Please ensure backend services are running.</span>
        </div>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[...Array(6)].map((_, i) => (
            <div key={i} className="h-48 bg-ds-surface rounded-ds border border-ds-border animate-pulse p-6" />
          ))}
        </div>
      )}

      {/* Year Grid */}
      {!isLoading && years.length > 0 && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold text-ds-primary uppercase tracking-wider flex items-center gap-2">
              <Layers className="w-4 h-4 text-ds-accent" />
              <span>Available Exam Years (Newest First)</span>
            </h2>
            <span className="text-xs text-ds-muted">Click any year card to inspect topic breakdown</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5" data-testid="pyp-year-grid">
            {years.map((y) => {
              const isFull = fullPaperYears.has(y.year);
              const isSelected = activeYearData?.year === y.year;
              const isStarting = startingYear === y.year;
              const topSubjects = Object.entries(y.bySubject).slice(0, 3);

              return (
                <div
                  key={y.year}
                  data-testid={`pyp-year-card-${y.year}`}
                  onClick={() => setSelectedYear(y.year)}
                  className={`bg-ds-surface border rounded-ds p-5 flex flex-col justify-between cursor-pointer transition-all ${
                    isSelected 
                      ? 'border-ds-accent shadow-sm ring-1 ring-ds-accent/30' 
                      : 'border-ds-border hover:border-ds-border-strong hover:bg-ds-surface-hover'
                  }`}
                >
                  <div className="space-y-3">
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="text-lg font-bold text-ds-primary tracking-tight">GATE {y.year}</h3>
                          {isFull ? (
                            <span 
                              data-testid="full-paper-badge"
                              className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold border text-ds-success bg-ds-success/10 border-ds-success/30 flex items-center gap-1"
                            >
                              <CheckCircle2 className="w-3 h-3" />
                              Full 65-Q Paper
                            </span>
                          ) : (
                            <span 
                              data-testid="shortfall-badge"
                              className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold border text-ds-warning bg-ds-warning/10 border-ds-warning/30 flex items-center gap-1"
                            >
                              <AlertCircle className="w-3 h-3" />
                              Partial Paper
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-ds-muted mt-0.5 font-mono">
                          {y.total} Questions in Bank
                        </p>
                      </div>

                      <span className="font-mono text-xs font-semibold px-2 py-1 rounded bg-ds-surface-raised border border-ds-border text-ds-secondary">
                        {isFull ? '100 Marks' : `${y.total * 1.5 | 0} Marks`}
                      </span>
                    </div>

                    {/* Subject chips */}
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {topSubjects.map(([slug, count]) => (
                        <span 
                          key={slug}
                          className="px-2 py-0.5 text-[11px] font-mono rounded bg-ds-surface-raised border border-ds-border text-ds-secondary"
                        >
                          {slug.replace('general-', 'ga-').replace('programming-and-ds', 'pds')}: {count}
                        </span>
                      ))}
                      {Object.keys(y.bySubject).length > 3 && (
                        <span className="px-2 py-0.5 text-[11px] font-mono rounded bg-ds-surface-raised border border-ds-border text-ds-muted">
                          +{Object.keys(y.bySubject).length - 3} more
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="pt-5 mt-3 border-t border-ds-border flex items-center justify-between gap-3">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDrillYear(y.year);
                      }}
                      className="text-xs text-ds-secondary hover:text-ds-primary font-medium flex items-center gap-1 transition-colors"
                    >
                      <Filter className="w-3.5 h-3.5" />
                      <span>Drill Year</span>
                    </button>

                    <button
                      type="button"
                      data-testid={`attempt-paper-btn-${y.year}`}
                      disabled={isStarting}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleAttemptPaper(y.year);
                      }}
                      className="px-3.5 py-1.5 rounded-ds text-xs font-semibold text-ds-accent-contrast bg-ds-accent hover:opacity-90 disabled:opacity-50 flex items-center gap-1.5 transition-all shadow-sm"
                    >
                      <Play className="w-3.5 h-3.5 fill-current" />
                      <span>{isStarting ? 'Starting...' : 'Attempt Paper'}</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Year Detail & Subject Distribution Table */}
      {!isLoading && activeYearData && (
        <div className="bg-ds-surface border border-ds-border rounded-ds p-6 space-y-6 shadow-sm" data-testid="pyp-year-detail">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-ds-border pb-4">
            <div>
              <div className="flex items-center gap-2">
                <BarChart3 className="w-5 h-5 text-ds-accent" />
                <h2 className="text-base font-bold text-ds-primary">
                  GATE {activeYearData.year} Subject Distribution & Practice Drill
                </h2>
              </div>
              <p className="text-xs text-ds-muted mt-1">
                Detailed breakdown of available previous-year questions for GATE {activeYearData.year}.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => handleDrillYear(activeYearData.year)}
                className="px-3 py-1.5 rounded-ds border border-ds-border bg-ds-surface hover:bg-ds-surface-hover text-xs font-medium text-ds-primary flex items-center gap-1.5 transition-colors"
              >
                <BookOpen className="w-3.5 h-3.5 text-ds-accent" />
                <span>Drill Entire {activeYearData.year}</span>
              </button>

              <button
                type="button"
                data-testid={`attempt-paper-detail-btn-${activeYearData.year}`}
                onClick={() => handleAttemptPaper(activeYearData.year)}
                disabled={startingYear === activeYearData.year}
                className="px-3.5 py-1.5 rounded-ds bg-ds-accent text-ds-accent-contrast hover:opacity-90 text-xs font-semibold flex items-center gap-1.5 transition-all shadow-sm"
              >
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Attempt Complete Paper</span>
              </button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left border-collapse">
              <thead>
                <tr className="border-b border-ds-border text-ds-muted uppercase font-mono text-[11px]">
                  <th className="py-2.5 px-3">Subject / Domain</th>
                  <th className="py-2.5 px-3">Available Questions</th>
                  <th className="py-2.5 px-3">Weightage Share</th>
                  <th className="py-2.5 px-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-ds-border">
                {Object.entries(activeYearData.bySubject).map(([slug, count]) => {
                  const sharePct = ((count / activeYearData.total) * 100).toFixed(1);
                  const isGA = slug === 'general-aptitude';

                  return (
                    <tr key={slug} className="hover:bg-ds-surface-hover transition-colors">
                      <td className="py-3 px-3 font-medium text-ds-primary flex items-center gap-2">
                        {isGA ? (
                          <Award className="w-4 h-4 text-ds-accent" />
                        ) : (
                          <div className="w-2 h-2 rounded-full bg-ds-info" />
                        )}
                        <span>{slug.replace(/-/g, ' ').toUpperCase()}</span>
                      </td>
                      <td className="py-3 px-3 font-mono text-ds-secondary">
                        <span className="font-semibold text-ds-primary">{count}</span> questions
                      </td>
                      <td className="py-3 px-3">
                        <div className="flex items-center gap-2">
                          <div className="w-24 h-1.5 rounded-full bg-ds-surface-raised overflow-hidden">
                            <div 
                              className="h-full bg-ds-accent"
                              style={{ width: `${Math.min(100, Number(sharePct))}%` }}
                            />
                          </div>
                          <span className="font-mono text-[11px] text-ds-muted">{sharePct}%</span>
                        </div>
                      </td>
                      <td className="py-3 px-3 text-right">
                        <button
                          type="button"
                          onClick={() => handleDrillYear(activeYearData.year, slug)}
                          className="text-xs text-ds-accent hover:underline font-medium inline-flex items-center gap-1"
                        >
                          <span>Drill {activeYearData.year}</span>
                          <ArrowRight className="w-3 h-3" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default PYPPage;
