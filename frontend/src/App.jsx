import { AppProvider } from './context/AppContext.jsx';
import Dashboard from './pages/Dashboard.jsx';

export default function App() {
  return (
    <AppProvider>
      <Dashboard />
    </AppProvider>
  );
}
