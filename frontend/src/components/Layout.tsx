import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { RightSidebar } from './RightSidebar';

export const Layout: React.FC = () => {
  return (
    <div className="flex bg-[#0d0d0d] min-h-screen text-zinc-100 selection:bg-amber-500/20 selection:text-amber-300">
      {/* 1. Left Navigation Rail (TakeUForward style) */}
      <Sidebar />

      {/* 2. Main Middle Viewport */}
      <div className="flex-1 flex flex-col min-w-0">
        <Header />

        <main className="flex-1 p-6 md:p-8 space-y-8 overflow-y-auto max-w-7xl">
          <Outlet />
        </main>
      </div>

      {/* 3. Right Productivity Rail (Circular Gauge + Widgets) */}
      <RightSidebar />
    </div>
  );
};

export default Layout;
