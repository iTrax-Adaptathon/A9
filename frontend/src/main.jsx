import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import { installTheme } from './theme.js';
import './index.css';

installTheme('light');

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
