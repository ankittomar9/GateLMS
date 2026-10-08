export type QuestionType = 'MCQ' | 'MSQ' | 'NAT';
export type Pass1Status = 'UNSEEN' | 'SOLVED' | 'REVISE' | 'FLAGGED';
export type Pass2Status = 'NOT_ATTEMPTED' | 'SOLVED' | 'FAILED' | 'TIMEOUT';
export type CurationStatus = 'UNREVIEWED' | 'ACCEPTED' | 'REJECTED';
export type QuestionSource = 'SAMPLE' | 'SO_DERIVED' | 'AUTHORED' | 'GATE_PYP' | 'APT';
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface SubjectSummary {
  id: string;
  name: string;
  slug: string;
  code?: string;
  cluster?: string;
  weightageAvg?: number;
  avgWeightage?: number;
  priorityTier: number | string;
  primaryBookRef?: string | null;
  standardBookRef?: string | null;
  topicCount: number;
}

export interface TopicItem {
  id: string;
  subjectId: string;
  name: string;
  slug: string;
  bookChapterRef: string | null;
  masteryLevel: number;
  lastRevisedAt: string | null;
  nextRevisionDue: string | null;
  version: number;
}

export interface TopicMasteryUpdate {
  masteryLevel?: number;
  lastRevisedAt?: string;
  nextRevisionDue?: string;
  version: number;
}

export interface QuestionSummary {
  id: string;
  externalId: string;
  subjectId: string;
  subjectName: string;
  topicId: string;
  topicName: string;
  year: number;
  type: QuestionType;
  marks: number;
  statement?: string;
  statementSnippet?: string;
  gateOverflowUrl: string | null;
  isBookmarked: boolean;
  lastAttemptedAt?: string | null;
  pass1Status: Pass1Status;
  pass2Status: Pass2Status;
  source?: QuestionSource;
  curationStatus?: CurationStatus;
  curationNote?: string | null;
  difficulty?: QuestionDifficulty | null;
  version: number;
}

export interface QuestionDetail {
  id: string;
  externalId: string;
  subjectId: string;
  subjectName: string;
  topicId: string;
  topicName: string;
  year: number;
  type: QuestionType;
  marks: number;
  statement: string;
  optionsJson: string | null;
  correctOptions: string | null;
  numericMin: number | null;
  numericMax: number | null;
  explanation: string | null;
  gateOverflowUrl: string | null;
  isBookmarked: boolean;
  lastAttemptedAt?: string | null;
  pass1Status: Pass1Status;
  pass2Status: Pass2Status;
  source?: QuestionSource;
  curationStatus?: CurationStatus;
  curationNote?: string | null;
  difficulty?: QuestionDifficulty | null;
  version: number;
}

export interface QuestionFilterParams {
  q?: string;
  subjectId?: string;
  topicId?: string;
  type?: QuestionType;
  year?: number;
  pass1Status?: Pass1Status;
  bookmarked?: boolean;
  curationStatus?: CurationStatus;
  source?: QuestionSource;
  difficulty?: QuestionDifficulty;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StatsSummary {
  totalQuestions: number;
  questionsByType: Record<string, number>;
  questionsBySubject: Record<string, number>;
  questionsByPass1Status: Record<string, number>;
  overallMasteryPercentage: number;
}

export interface SubjectCurationCount {
  subjectId: string;
  name: string;
  unreviewed: number;
  accepted: number;
  rejected: number;
  total?: number;
}

export interface CurationStatsResponse {
  totalQuestions: number;
  byStatus: Record<string, number>;
  bySubject: SubjectCurationCount[];
}

export interface DistributionItem {
  subjectSlug: string;
  subjectName: string;
  type: string;
  difficulty: string;
  count: number;
}

export interface DistributionStatsResponse {
  total: number;
  distribution: DistributionItem[];
}

export type ExamMode = 'MOCK' | 'PRACTICE' | 'WEAKNESS_DRILL';
export type AttemptAnswerState = 'NOT_VISITED' | 'NOT_ANSWERED' | 'ANSWERED' | 'MARKED_FOR_REVIEW' | 'ANSWERED_AND_MARKED';
export type MistakeErrorCategory = 'CONCEPTUAL' | 'CALCULATION' | 'MSQ_TRAP' | 'MISREAD';

export interface PracticeSessionCreateRequest {
  subjectId?: string;
  topicId?: string;
  questionType?: QuestionType;
  marks?: number;
  curationStatus?: string;
  count?: number;
  year?: number;
}

export interface PracticeQuestionDto {
  id: string;
  externalId: string | null;
  type: QuestionType;
  marks: number;
  statement: string;
  optionsJson: string | null;
  topicId: string | null;
  subjectId: string | null;
  year: number | null;
}

export interface PracticeSessionResponse {
  sessionId: string;
  attemptId: string;
  questions: PracticeQuestionDto[];
  totalQuestions: number;
}

export interface PracticeAnswerSubmission {
  questionId: string;
  selectedOptions?: string;
  numericAnswer?: number;
  timeSpentSeconds?: number;
}

export interface PracticeVerdictResponse {
  correct: boolean;
  marksAwarded: number;
  correctOptions?: string | null;
  numericMin?: number | null;
  numericMax?: number | null;
  explanation?: string | null;
  topicMastery: number;
  mistakeLogPrompt: boolean;
}

export interface PracticeTopicSummary {
  topicId: string;
  name: string;
  correct: number;
  total: number;
}

export interface PracticeFinishResponse {
  correct: number;
  wrong: number;
  skipped: number;
  totalQuestions: number;
  timeSpentSeconds: number;
  byTopic: PracticeTopicSummary[];
}

export interface MistakeCreateRequest {
  questionId: string;
  attemptAnswerId?: string | null;
  errorCategory: MistakeErrorCategory;
  description?: string;
  correctiveTakeaway: string;
}

export interface MistakeReviewRequest {
  resolved?: boolean;
}

export interface MistakeDto {
  id: string;
  questionId: string;
  questionExternalId: string | null;
  questionStatement: string;
  attemptAnswerId: string | null;
  errorCategory: MistakeErrorCategory;
  description: string | null;
  correctiveTakeaway: string;
  loggedAt: string;
  reviewCount: number;
  nextReviewAt: string | null;
  resolved: boolean;
}

export interface TopicMasteryStatsDto {
  topicId: string;
  topicName: string;
  subjectId: string;
  masteryLevel: number;
  dueReviewsCount: number;
}

export interface BlueprintSection {
  subjectId: string;
  mcq: number;
  msq: number;
  nat: number;
  marks?: number;
}

export interface BlueprintQuestionPool {
  curationStatus?: string[];
  difficulty?: string[];
}

export interface MockBlueprint {
  title?: string;
  sections: BlueprintSection[];
  questionPool?: BlueprintQuestionPool;
  shuffle: boolean;
}

export interface CreateMockTemplateRequest {
  title: string;
  durationMinutes: number;
  totalMarks?: number;
  sections: BlueprintSection[];
  questionPool?: BlueprintQuestionPool;
  shuffle: boolean;
}

export interface MockTemplateSummaryDto {
  id: string;
  title: string;
  durationMinutes: number;
  totalMarks: number;
  totalQuestions: number;
  mode: string;
  createdAt: string;
  sections: BlueprintSection[];
  bestScore?: number | null;
  attemptCount: number;
  shortfallWarnings?: string[];
}

export interface MockAttemptSectionDto {
  subjectId: string;
  name: string;
  questionIds: string[];
}

export interface StartMockAttemptResponse {
  attemptId: string;
  mockId: string;
  mockTitle: string;
  durationSeconds: number;
  sections: MockAttemptSectionDto[];
  questions: PracticeQuestionDto[];
  startedAt: string;
  serverTime: string;
  shortfallWarnings: string[];
}

export interface UpsertAnswerRequest {
  selectedOptions?: string | null;
  numericAnswer?: number | null;
  state?: string | null;
  timeSpentSeconds?: number | null;
}

export interface UpsertAnswerResponse {
  saved: boolean;
  state: string;
}

export interface SubmitMockAttemptRequest {
  timeSpentSeconds?: number | null;
}

export interface SubmitMockAttemptResponse {
  attemptId: string;
  status: string;
  finishedReason: string;
  positiveScore: number;
  negativeScore: number;
  totalScore: number;
  maxMarks: number;
  correctCount: number;
  wrongCount: number;
  unattemptedCount: number;
  totalQuestions: number;
}

export interface MockQuestionReviewDto {
  questionId: string;
  externalId: string;
  type: QuestionType;
  marks: number;
  statement: string;
  optionsJson?: string | null;
  subjectId?: string | null;
  subjectName?: string | null;
  topicId?: string | null;
  yourAnswer?: string | null;
  numericAnswer?: number | null;
  correctAnswer?: string | null;
  numericMin?: number | null;
  numericMax?: number | null;
  isCorrect?: boolean | null;
  marksAwarded: number;
  state: string;
  timeSpentSeconds: number;
  explanation?: string | null;
}

export interface MockSectionScoreDto {
  subjectId: string;
  subjectName: string;
  totalQuestions: number;
  attemptedCount: number;
  correctCount: number;
  wrongCount: number;
  unattemptedCount: number;
  marksAwarded: number;
  maxMarks: number;
  accuracy: number;
  timeSpentSeconds: number;
}

export interface MockResultResponse {
  attemptId: string;
  mockId: string;
  mockTitle: string;
  startedAt: string;
  submittedAt?: string | null;
  durationSeconds: number;
  finishedReason: string;
  totalScore: number;
  positiveScore: number;
  negativeScore: number;
  maxMarks: number;
  timeSpentSeconds: number;
  questions: MockQuestionReviewDto[];
  sectionScores: MockSectionScoreDto[];
}

export interface MockAttemptHistoryDto {
  attemptId: string;
  startedAt: string;
  submittedAt?: string | null;
  durationSeconds?: number | null;
  totalScore?: number | null;
  positiveScore?: number | null;
  negativeScore?: number | null;
  status: string;
  finishedReason?: string | null;
}

export interface MockTrendDto {
  attemptId: string;
  mockId: string;
  mockTitle: string;
  date: string;
  totalScore: number;
  maxScore: number;
  percentage: number;
  percentileLike?: number | null;
}

export interface QuestionUpsertDto {
  externalId?: string;
  subjectId: string;
  topicId: string;
  year?: number;
  type: QuestionType;
  marks: number;
  statement: string;
  optionsJson?: string;
  correctOptions?: string;
  numericMin?: number;
  numericMax?: number;
  explanation?: string;
  gateOverflowUrl?: string;
  source?: string;
  curationStatus?: string;
  curationNote?: string;
  difficulty?: string;
  version?: number;
}

export interface QuestionValidationResultDto {
  valid: boolean;
  fieldErrors: Record<string, string>;
  warnings: string[];
}

export interface PypYearStatsDto {
  year: number;
  total: number;
  bySubject: Record<string, number>;
}

export interface PypCoverageDto {
  fullPaperYears: number[];
  partialYears: number[];
}

export interface PypStatsResponse {
  years: PypYearStatsDto[];
  coverage: PypCoverageDto;
}

export type PlanStrategy = 'WEIGHTED' | 'WEAKNESS_FIRST' | 'LINEAR';
export type PlanItemStatus = 'OPEN' | 'DONE' | 'SKIPPED';

export interface GeneratePlanRequest {
  title?: string;
  weeks?: number;
  strategy?: PlanStrategy;
  weeklyTargetQuestions?: number;
  weeklyTargetMinutes?: number;
  startDate?: string;
}

export interface PlanItemDto {
  id: string;
  planId: string;
  planDate: string;
  subjectId: string | null;
  subjectName: string | null;
  subjectCode: string | null;
  topicId: string | null;
  topicName: string | null;
  targetQuestions: number;
  targetMinutes: number;
  doneQuestions: number;
  doneMinutes: number;
  status: PlanItemStatus;
}

export interface StudyPlanSummaryDto {
  id: string;
  title: string;
  startDate: string;
  endDate: string;
  weeks: number;
  weeklyTargetQuestions: number;
  weeklyTargetMinutes: number;
  strategy: PlanStrategy;
  totalQuestionsTarget: number;
  totalQuestionsDone: number;
  totalMinutesTarget: number;
  totalMinutesDone: number;
  progressPercentage: number;
  itemsCount: number;
  openItemsCount: number;
  doneItemsCount: number;
  skippedItemsCount: number;
  createdAt: string;
}

export interface StudyPlanDetailDto {
  id: string;
  title: string;
  startDate: string;
  endDate: string;
  weeks: number;
  weeklyTargetQuestions: number;
  weeklyTargetMinutes: number;
  strategy: PlanStrategy;
  totalQuestionsTarget: number;
  totalQuestionsDone: number;
  totalMinutesTarget: number;
  totalMinutesDone: number;
  progressPercentage: number;
  generatedReasoning: Record<string, any> | null;
  createdAt: string;
  items: PlanItemDto[];
  itemsByDate: Record<string, PlanItemDto[]>;
}

export interface TodayPlanResponse {
  items: PlanItemDto[];
  doneQuestions: number;
  targetQuestions: number;
  minutesDone: number;
  minutesTarget: number;
}

export interface UpdatePlanItemStatusRequest {
  status: PlanItemStatus;
}

// Phase 4 SPEC 2: Engagement, Goals, Achievements & Notifications
export type NotificationType = 'REVISION_DUE' | 'STREAK_AT_RISK' | 'PLAN_DUE' | 'ACHIEVEMENT_EARNED';

export interface DailyGoalProgressDto {
  target: number;
  done: number;
  met: boolean;
}

export interface DueTodayDto {
  mistakes: number;
  topics: number;
}

export interface EngagementSummaryResponse {
  streak: number;
  protectedDate?: string | null;
  streakAtRisk: boolean;
  goal: DailyGoalProgressDto;
  dueToday: DueTodayDto;
}

export interface AchievementDto {
  code: string;
  title: string;
  description: string;
  earnedAt?: string | null;
}

export interface NotificationDto {
  id: string;
  type: NotificationType;
  title: string;
  body: string | null;
  link: string | null;
  readAt: string | null;
  createdAt: string;
}

export interface UnreadCountResponse {
  count: number;
}

export interface UserGoalDto {
  dailyTargetQuestions: number;
  streakProtection: boolean;
  updatedAt: string;
}

export interface UpdateUserGoalRequest {
  dailyTargetQuestions?: number;
  streakProtection?: boolean;
}

export interface MarkAllReadResponse {
  count: number;
}


