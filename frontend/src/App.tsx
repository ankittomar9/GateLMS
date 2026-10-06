import React, { useState } from 'react';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { HeroPlanner } from './components/HeroPlanner';
import { SubjectCards } from './components/SubjectCards';
import { RightSidebar } from './components/RightSidebar';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState('dashboard');

  return (
    <div className="flex bg-[#0d0d0d] min-h-screen text-zinc-100 selection:bg-amber-500/20 selection:text-amber-300">
      {/* 1. Left Navigation Rail (TakeUForward style) */}
      <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} />

      {/* 2. Main Middle Viewport */}
      <div className="flex-1 flex flex-col min-w-0">
        <Header />

        <main className="flex-1 p-6 md:p-8 space-y-8 overflow-y-auto max-w-7xl">
          {/* Hero Planner Banner */}
          <HeroPlanner />

          {/* Core High-Yield Subjects & GATE Must-Haves */}
          <SubjectCards />
        </main>
      </div>

      {/* 3. Right Productivity Rail (Circular Progress + Widgets) */}
      <RightSidebar />
    </div>
  );
};

export default App;
