import React from 'react';
import { RouterProvider } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { router } from './router';
import { DesignProvider } from './context/DesignContext';
import { LayoutProvider } from './context/LayoutContext';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <DesignProvider>
        <LayoutProvider>
          <RouterProvider router={router} />
        </LayoutProvider>
      </DesignProvider>
    </QueryClientProvider>
  );
};

export default App;
