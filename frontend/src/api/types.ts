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
  code: string;
  weightageAvg: number;
  priorityTier: string;
  primaryBookRef: string;
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

