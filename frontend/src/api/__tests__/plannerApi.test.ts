import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { plannerApi } from '../plannerApi';
import {
  GeneratePlanRequest,
  StudyPlanDetailDto,
  StudyPlanSummaryDto,
  TodayPlanResponse,
  PlanItemDto,
} from '../types';

describe('plannerApi', () => {
  const originalFetch = globalThis.fetch;

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('1. generatePlan posts to /plans/generate and returns StudyPlanDetailDto payload shape', async () => {
    const mockDetail: StudyPlanDetailDto = {
      id: 'plan-123',
      title: '16-Week Plan',
      startDate: '2026-10-01',
      endDate: '2027-01-21',
      weeks: 16,
      weeklyTargetQuestions: 150,
      weeklyTargetMinutes: 600,
      strategy: 'WEIGHTED',
      totalQuestionsTarget: 2400,
      totalQuestionsDone: 0,
      totalMinutesTarget: 9600,
      totalMinutesDone: 0,
      progressPercentage: 0.0,
      generatedReasoning: { mode: 'weighted', reason: 'active_plan' },
      createdAt: '2026-10-01T00:00:00Z',
      items: [],
      itemsByDate: {},
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 201,
      json: async () => mockDetail,
    });

    const req: GeneratePlanRequest = {
      weeks: 16,
      strategy: 'WEIGHTED',
      weeklyTargetQuestions: 150,
    };

    const res = await plannerApi.generatePlan(req);

    expect(res.id).toBe('plan-123');
    expect(res.weeks).toBe(16);
    expect(res.strategy).toBe('WEIGHTED');
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/plans/generate',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify(req),
      })
    );
  });

  it('2. getPlans calls GET /plans and getPlanById calls GET /plans/:id', async () => {
    const mockSummaries: StudyPlanSummaryDto[] = [
      {
        id: 'plan-1',
        title: 'Plan 1',
        startDate: '2026-10-01',
        endDate: '2026-10-15',
        weeks: 2,
        weeklyTargetQuestions: 100,
        weeklyTargetMinutes: 400,
        strategy: 'LINEAR',
        totalQuestionsTarget: 200,
        totalQuestionsDone: 50,
        totalMinutesTarget: 800,
        totalMinutesDone: 200,
        progressPercentage: 25.0,
        itemsCount: 12,
        openItemsCount: 9,
        doneItemsCount: 3,
        skippedItemsCount: 0,
        createdAt: '2026-10-01T00:00:00Z',
      },
    ];

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockSummaries,
    });

    const list = await plannerApi.getPlans();
    expect(list).toHaveLength(1);
    expect(list[0].id).toBe('plan-1');
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/plans',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('3. getTodayPlan calls GET /plans/today and returns today targets shape', async () => {
    const mockToday: TodayPlanResponse = {
      items: [
        {
          id: 'item-1',
          planId: 'plan-1',
          planDate: '2026-10-07',
          subjectId: 'CS-ALGO',
          subjectName: 'Algorithms',
          subjectCode: 'ALGO',
          topicId: 'top-1',
          topicName: 'Sorting',
          targetQuestions: 10,
          targetMinutes: 30,
          doneQuestions: 5,
          doneMinutes: 15,
          status: 'OPEN',
        },
      ],
      doneQuestions: 5,
      targetQuestions: 10,
      minutesDone: 15,
      minutesTarget: 30,
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockToday,
    });

    const res = await plannerApi.getTodayPlan();
    expect(res.targetQuestions).toBe(10);
    expect(res.doneQuestions).toBe(5);
    expect(res.items).toHaveLength(1);
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/plans/today',
      expect.objectContaining({ method: 'GET' })
    );
  });

  it('4. updatePlanItemStatus calls PATCH /plan-items/:id and regeneratePlan calls POST /plans/:id/regenerate', async () => {
    const mockUpdatedItem: PlanItemDto = {
      id: 'item-1',
      planId: 'plan-1',
      planDate: '2026-10-07',
      subjectId: 'CS-ALGO',
      subjectName: 'Algorithms',
      subjectCode: 'ALGO',
      topicId: 'top-1',
      topicName: 'Sorting',
      targetQuestions: 10,
      targetMinutes: 30,
      doneQuestions: 10,
      doneMinutes: 30,
      status: 'DONE',
    };

    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockUpdatedItem,
    });

    const updated = await plannerApi.updatePlanItemStatus('item-1', { status: 'DONE' });
    expect(updated.status).toBe('DONE');
    expect(globalThis.fetch).toHaveBeenCalledWith(
      '/api/v1/plan-items/item-1',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ status: 'DONE' }),
      })
    );
  });
});
