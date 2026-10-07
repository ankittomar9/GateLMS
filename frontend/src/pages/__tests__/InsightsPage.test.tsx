import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { InsightsPage } from '../InsightsPage';
import { readinessApi, ReadinessResponse, HeatmapResponse, TrendsResponse, ExamCountdown } from '../../api/readinessApi';

// Mock recharts ResponsiveContainer to render children reliably in JSDOM
vi.mock('recharts', async () => {
  const original = await vi.importActual<any>('recharts');
  return {
    ...original,
    ResponsiveContainer: ({ children }: any) => <div data-testid="responsive-container">{children}</div>,
  };
});

const mockReadinessFixture: ReadinessResponse = {
  readiness: 64,
  perSubject: [
    {
      subjectId: 'subj_discrete',
      name: 'Discrete Mathematics',
      weight: 15.0,
      m: 0.8,
      a: 0.75,
      c: 0.6,
      readiness: 0.745,
    },
    {
      subjectId: 'subj_algo',
      name: 'Algorithms',
      weight: 10.0,
      m: 0.6,
      a: 0.5,
      c: 0.4,
      readiness: 0.53,
    },
    {
      subjectId: 'subj_os',
      name: 'Operating Systems',
      weight: 12.0,
      m: 0.0,
      a: 0.0,
      c: 0.0,
      readiness: 0.0,
    },
  ],
  computedAt: '2026-10-07T08:00:00Z',
};

const mockHeatmapFixture: HeatmapResponse = {
  subjects: [
    {
      subjectId: 'subj_discrete',
      name: 'Discrete Mathematics',
      topics: [
        { topicId: 'top_dm_1', name: 'Propositional Logic', mastery: 5, due: false, answered: 20, correct: 19 },
        { topicId: 'top_dm_2', name: 'Combinatorics', mastery: 4, due: true, answered: 15, correct: 12 },
        { topicId: 'top_dm_3', name: 'Graph Theory', mastery: 3, due: false, answered: 10, correct: 7 },
      ],
    },
    {
      subjectId: 'subj_algo',
      name: 'Algorithms',
      topics: [
        { topicId: 'top_al_1', name: 'Asymptotic Analysis', mastery: 2, due: false, answered: 8, correct: 5 },
        { topicId: 'top_al_2', name: 'Dynamic Programming', mastery: 1, due: true, answered: 5, correct: 2 },
        { topicId: 'top_al_3', name: 'Graph Algorithms', mastery: 0, due: false, answered: 0, correct: 0 },
      ],
    },
  ],
};

const mockTrendsFixture: TrendsResponse = {
  days: [
    { date: '2026-10-01', questionsAnswered: 0, correct: 0, minutesPracticed: 0, minutesMocked: 0 },
    { date: '2026-10-02', questionsAnswered: 10, correct: 8, minutesPracticed: 25, minutesMocked: 0 },
    { date: '2026-10-03', questionsAnswered: 0, correct: 0, minutesPracticed: 0, minutesMocked: 0 },
    { date: '2026-10-04', questionsAnswered: 15, correct: 12, minutesPracticed: 0, minutesMocked: 45 },
  ],
  rolling7: {
    accuracy: 80.0,
    volume: 25,
  },
};

const mockCountdownFixture: ExamCountdown = {
  examDate: '2028-02-12',
  daysRemaining: 493,
  weeksRemaining: 70,
  source: 'config',
};

describe('InsightsPage Component Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('1. score hero renders readiness + components from fixture', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('score-hero')).toBeDefined();
    });

    const scoreHero = screen.getByTestId('score-hero');
    expect(scoreHero).toBeDefined();

    expect(screen.getByText('64%')).toBeDefined();
    expect(screen.getAllByText('Discrete Mathematics').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Algorithms').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Operating Systems').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/M: 80%/)).toBeDefined();
    expect(screen.getByText(/A: 75%/)).toBeDefined();
    expect(screen.getByText(/C: 60%/)).toBeDefined();
  });

  it('2. heatmap cell classes map mastery 0-5', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('mastery-heatmap-card')).toBeDefined();
    });

    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells.length).toBe(6);

    // Verify mastery attributes across 0 to 5
    const masteries = cells.map(c => c.getAttribute('data-mastery'));
    expect(masteries).toContain('0');
    expect(masteries).toContain('1');
    expect(masteries).toContain('2');
    expect(masteries).toContain('3');
    expect(masteries).toContain('4');
    expect(masteries).toContain('5');

    // Mastery 5 cell has high-level styling class
    const mastery5Cell = cells.find(c => c.getAttribute('data-mastery') === '5');
    expect(mastery5Cell?.className).toContain('bg-ds-success');
  });

  it('3. due cells ringed with due indicator', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('mastery-heatmap-card')).toBeDefined();
    });

    const dueCells = screen.getAllByTestId('heatmap-cell').filter(
      c => c.getAttribute('data-due') === 'true'
    );
    expect(dueCells.length).toBe(2);
    dueCells.forEach(cell => {
      expect(cell.className).toContain('ring-2');
      expect(cell.className).toContain('ring-ds-warning');
    });
  });

  it('4. radar renders subjects', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('radar-chart-card')).toBeDefined();
    });

    expect(screen.getByText('Subject Preparedness Radar')).toBeDefined();
  });

  it('5. trend zero-fill renders with rolling 7 stats', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart-card')).toBeDefined();
    });

    expect(screen.getByText(/Rolling 7-day Accuracy:/)).toBeDefined();
    expect(screen.getByText('80%')).toBeDefined();
    expect(screen.getByText(/(25 answered)/)).toBeDefined();
  });

  it('6. countdown math from fixed date displays correctly', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('countdown-card')).toBeDefined();
    });

    expect(screen.getByText('2028-02-12')).toBeDefined();
    expect(screen.getByText(/493 days left \(70 weeks\)/)).toBeDefined();
  });

  it('7. empty-state CTA rendered when no activity exists', async () => {
    const emptyReadiness: ReadinessResponse = {
      readiness: 0,
      perSubject: [],
      computedAt: '2026-10-07T08:00:00Z',
    };

    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(emptyReadiness);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue({ subjects: [] });
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue({ days: [], rolling7: { accuracy: 0, volume: 0 } });
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('insights-empty-state')).toBeDefined();
    });

    expect(screen.getByText('No activity yet — take a practice session')).toBeDefined();
    expect(screen.getByText('Start Practicing Now')).toBeDefined();
  });

  it('8. popover quotes formula verbatim from §2.2', async () => {
    vi.spyOn(readinessApi, 'getReadiness').mockResolvedValue(mockReadinessFixture);
    vi.spyOn(readinessApi, 'getHeatmap').mockResolvedValue(mockHeatmapFixture);
    vi.spyOn(readinessApi, 'getTrends').mockResolvedValue(mockTrendsFixture);
    vi.spyOn(readinessApi, 'getCountdown').mockResolvedValue(mockCountdownFixture);

    render(
      <MemoryRouter>
        <InsightsPage />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByTestId('formula-toggle-btn')).toBeDefined();
    });

    // Toggle formula popover open
    fireEvent.click(screen.getByTestId('formula-toggle-btn'));

    expect(screen.getByTestId('formula-popover')).toBeDefined();
    expect(screen.getByText('R_s = 0.5·M_s + 0.3·A_s + 0.2·C_s')).toBeDefined();
    expect(screen.getByText('READINESS = round(100 · Σ(w_s·R_s) / Σ(w_s))')).toBeDefined();
    expect(screen.getByText('M_s = avg(mastery_level/5) over topics of s with last_revised_at IS NOT NULL')).toBeDefined();
  });
});
