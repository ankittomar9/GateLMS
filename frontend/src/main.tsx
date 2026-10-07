import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import 'katex/dist/katex.min.css';
import { registerServiceWorker } from './pwa/registerServiceWorker';

// Register PWA service worker in production
registerServiceWorker();

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
