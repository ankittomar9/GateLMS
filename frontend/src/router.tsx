import React from 'react';
import { createBrowserRouter } from 'react-router-dom';
import { Layout } from './components/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { SyllabusPage } from './pages/SyllabusPage';
import { QuestionsPage } from './pages/QuestionsPage';
import { QuestionDetailPage } from './pages/QuestionDetailPage';
import { CurationQueuePage } from './pages/CurationQueuePage';
import { AuthoringPage } from './pages/AuthoringPage';
import { PracticePage } from './pages/PracticePage';
import { PracticeRunnerPage } from './pages/PracticeRunnerPage';
import { MistakeNotebookPage } from './pages/MistakeNotebookPage';
import { MockTestsPage } from './pages/MockTestsPage';
import { TestRunnerPage } from './pages/TestRunnerPage';
import { ResultPage } from './pages/ResultPage';
import { InsightsPage } from './pages/InsightsPage';
import { NotesPage } from './pages/NotesPage';
import { RevisionPage } from './pages/RevisionPage';
import { PYPPage } from './pages/PYPPage';
import { PlannerPage } from './pages/PlannerPage';
import { NotificationsPage } from './pages/NotificationsPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: 'planner',
        element: <PlannerPage />,
      },
      {
        path: 'pyp',
        element: <PYPPage />,
      },
      {
        path: 'insights',
        element: <InsightsPage />,
      },
      {
        path: 'notifications',
        element: <NotificationsPage />,
      },
      {
        path: 'revision',
        element: <RevisionPage />,
      },
      {
        path: 'notes',
        element: <NotesPage />,
      },
      {
        path: 'syllabus',
        element: <SyllabusPage />,
      },
      {
        path: 'questions',
        element: <QuestionsPage />,
      },
      {
        path: 'questions/:id',
        element: <QuestionDetailPage />,
      },
      {
        path: 'curation',
        element: <CurationQueuePage />,
      },
      {
        path: 'author',
        element: <AuthoringPage />,
      },
      {
        path: 'practice',
        element: <PracticePage />,
      },
      {
        path: 'practice/sessions/:attemptId',
        element: <PracticeRunnerPage />,
      },
      {
        path: 'mistakes',
        element: <MistakeNotebookPage />,
      },
      {
        path: 'mocks',
        element: <MockTestsPage />,
      },
      {
        path: 'mocks/attempts/:attemptId',
        element: <TestRunnerPage />,
      },
      {
        path: 'mocks/attempts/:attemptId/result',
        element: <ResultPage />,
      },
    ],
  },
]);
