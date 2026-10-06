import React from 'react';
import { HeroPlanner } from '../components/HeroPlanner';
import { SubjectCards } from '../components/SubjectCards';

export const DashboardPage: React.FC = () => {
  return (
    <div className="space-y-8">
      <HeroPlanner />
      <SubjectCards />
    </div>
  );
};

export default DashboardPage;
