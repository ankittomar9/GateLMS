import React from 'react';
import { createBrowserRouter } from 'react-router-dom';
import { Layout } from './components/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { SyllabusPage } from './pages/SyllabusPage';
import { QuestionsPage } from './pages/QuestionsPage';
import { QuestionDetailPage } from './pages/QuestionDetailPage';
import { CurationQueuePage } from './pages/CurationQueuePage';
import { PracticePage } from './pages/PracticePage';
import { PracticeRunnerPage } from './pages/PracticeRunnerPage';
import { MistakeNotebookPage } from './pages/MistakeNotebookPage';

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
    ],
  },
]);
