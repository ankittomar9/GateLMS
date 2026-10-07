import React from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { RightSidebar } from './RightSidebar';
import { OfflineBanner } from './pwa/OfflineBanner';
import { UpdateToast } from './pwa/UpdateToast';
import { AiStudioDrawer } from './ai/AiStudioDrawer';

export const Layout: React.FC = () => {
  return (
    <div className="flex bg-ds-bg min-h-screen text-ds-primary font-ds-sans selection:bg-ds-accent-soft selection:text-ds-accent transition-colors duration-150">
      {/* 1. Left Navigation Rail */}
      <Sidebar />

      {/* 2. Main Middle Viewport */}
      <div className="flex-1 flex flex-col min-w-0">
        <OfflineBanner />
        <Header />

        <main className="flex-1 p-6 md:p-8 space-y-8 overflow-y-auto max-w-7xl">
          <Outlet />
        </main>
        <UpdateToast />
      </div>

      {/* 3. Right Productivity Rail */}
      <RightSidebar />

      {/* 4. In-Portal AI Studio Sidecar (Collapsible, Stretchable, Resizable) */}
      <AiStudioDrawer />
    </div>
  );
};

export default Layout;
