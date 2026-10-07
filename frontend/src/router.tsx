import React from 'react';
import { createBrowserRouter } from 'react-router-dom';
import { Layout } from './components/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { SyllabusPage } from './pages/SyllabusPage';
import { QuestionsPage } from './pages/QuestionsPage';
import { QuestionDetailPage } from './pages/QuestionDetailPage';
import { CurationQueuePage } from './pages/CurationQueuePage';

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
    ],
  },
]);
