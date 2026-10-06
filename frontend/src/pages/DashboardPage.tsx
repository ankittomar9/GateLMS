import React from 'react';
import { useDashboardLayout } from '../context/LayoutContext';
import { MinimalistDashboard } from '../components/dashboard/MinimalistDashboard';
import { EditorialDashboard } from '../components/dashboard/EditorialDashboard';
import { BentoDashboard } from '../components/dashboard/BentoDashboard';

export const DashboardPage: React.FC = () => {
  const { currentLayout } = useDashboardLayout();

  switch (currentLayout) {
    case 'editorial':
      return <EditorialDashboard />;
    case 'bento':
      return <BentoDashboard />;
    case 'minimalist':
    default:
      return <MinimalistDashboard />;
  }
};

export default DashboardPage;
