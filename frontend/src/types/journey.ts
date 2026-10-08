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
  motivation: string;
  coreIdea: string;
  invariant: string | null;
  workedExample: string;
  traps: string | null;
  status: 'DRAFT' | 'REVIEWED' | 'ACCEPTED' | 'REJECTED';
  parseError: string | null;
  generatorModel: string | null;
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
