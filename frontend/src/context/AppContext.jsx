import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from '../services/api.js';
import { todayISO } from '../utils/time.js';
import { installTheme } from '../theme.js';

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [users, setUsers] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [availabilities, setAvailabilities] = useState([]);
  const [selectedDate, setSelectedDate] = useState(todayISO());
  const [backendStatus, setBackendStatus] = useState('checking');
  const [theme, setTheme] = useState('light');

  useEffect(() => {
    installTheme(theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  }, []);

  const refresh = useCallback(async (date = selectedDate) => {
    try {
      const [u, a, av] = await Promise.all([
        api.getUsers(),
        api.getAppointments(date),
        api.getAvailability(date),
      ]);
      setUsers(u);
      setAppointments(a);
      setAvailabilities(av);
      setBackendStatus('connected');
    } catch {
      setBackendStatus('disconnected');
    }
  }, [selectedDate]);

  useEffect(() => {
    refresh(selectedDate);
  }, [selectedDate, refresh]);

  const value = {
    users,
    appointments,
    availabilities,
    selectedDate,
    setSelectedDate,
    backendStatus,
    refresh,
    theme,
    toggleTheme,
  };

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useApp() {
  return useContext(AppContext);
}
