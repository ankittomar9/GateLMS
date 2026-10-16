export interface JourneyOverviewSubject {
  subjectId: string;
  name: string;
  cluster: string | null;
  priorityTier: number;
  marksWeight: number;
  standardBookRef: string | null;
  questionsVisible: number;
  questionsSolved: number;
  topicsTotal: number;
  topicsTouched: number;
  topicsCompleted: number;
  readinessPct: number;
}

export interface JourneyTopicSummary {
  topicId: string;
  name: string;
  sequence: number;
  primerStatus: string | null;
  questionsVisible: number;
  questionsSolved: number;
  masteryLevel: number;
  completed: boolean;
}

export interface TopicPrimer {
  id: string;
  topicId: string;
  topicName?: string;
  subjectName?: string;
  motivation: string;
  coreIdea: string;
  invariant: string | null;
  workedExample: string;
  traps: string | null;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  parseError: string | null;
  generatorModel: string | null;
  generatedBy?: string;
  generatedAt: string;
}

export interface TopicPrimerResponse {
  primer: TopicPrimer | null;
  conceptSlugs: string[];
  bookChapterRef: string | null;
  subjectStandardBookRef: string | null;
}

export interface JourneyContinue {
  topicId: string | null;
  subjectId: string | null;
  questionId: string | null;
}

export interface NotebookEntry {
  questionId: string;
  year: number | null;
  source: string | null;
  statementSnippet: string;
  solvedAt: string | null;
  starred: boolean;
  anatomyAvailable: boolean;
}

export interface TopicNotebookGroup {
  topicId: string;
  topicName: string;
  sequence: number;
  entries: NotebookEntry[];
}

export interface SubjectNotebookResponse {
  subjectId: string;
  subjectName: string;
  topics: TopicNotebookGroup[];
  totalEntries: number;
}

export interface SimilarQuestionsResponse {
  questions: any[];
  similarityBasis: 'CONCEPT' | 'TOPIC';
}

export interface ConceptCoverageItem {
  slug: string;
  label: string;
  mappedQuestionCount: number;
  uniqueStatementCount: number;
  lessonStatus: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED' | null;
  coverage: 'UNMAPPED' | 'MAPPED' | 'EXPLAINED';
}

export interface TopicConceptCoverageSummary {
  conceptsTotal: number;
  conceptsWithMaterial: number;
  conceptsExplained: number;
}

export interface TopicConceptCoverageResponse {
  summary: TopicConceptCoverageSummary;
  concepts: ConceptCoverageItem[];
}

export interface ConceptTagDto {
  slug: string;
  label: string;
  description: string | null;
  topicId: string;
  topicSlug: string;
  topicName: string;
}

export interface ConceptMappedQuestionItem {
  id: string;
  externalId: string;
  statement: string;
  difficulty: string | null;
  type: string;
  year: number | null;
  marks: number;
}

export interface ConceptLesson {
  id: string;
  conceptTagSlug: string;
  title: string;
  motivation: string;
  coreIdea: string;
  invariant: string | null;
  workedExample: string;
  traps: string | null;
  workedExampleExternalId: string | null;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  parseError: string | null;
  generatorModel: string | null;
  generatedAt: string;
  updatedAt: string | null;
}

export interface ConceptLessonResponse {
  lesson: ConceptLesson | null;
  concept: ConceptTagDto;
  mappedQuestions: ConceptMappedQuestionItem[];
}

export interface CurationLessonUpdateRequest {
  status?: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  title?: string;
  motivation?: string;
  coreIdea?: string;
  invariant?: string;
  workedExample?: string;
  traps?: string;
  workedExampleExternalId?: string;
}

