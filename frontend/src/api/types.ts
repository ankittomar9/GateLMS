export type QuestionType = 'MCQ' | 'MSQ' | 'NAT';
export type Pass1Status = 'UNSEEN' | 'SOLVED' | 'REVISE' | 'FLAGGED';
export type Pass2Status = 'NOT_ATTEMPTED' | 'SOLVED' | 'FAILED' | 'TIMEOUT';

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
  statementSnippet: string;
  gateOverflowUrl: string | null;
  isBookmarked: boolean;
  pass1Status: Pass1Status;
  pass2Status: Pass2Status;
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
