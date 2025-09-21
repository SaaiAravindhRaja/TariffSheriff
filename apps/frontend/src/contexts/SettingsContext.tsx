import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import safeLocalStorage from '@/lib/safeLocalStorage';

interface UserSettings {
  email: string;
  firstName: string;
  lastName: string;
  company: string;
  role: string;
  timezone: string;
  language: string;
  currency: string;
  dateFormat: string;
  notifications: {
    email: boolean;
    push: boolean;
    tariffUpdates: boolean;
    tradeAlerts: boolean;
    weeklyReports: boolean;
  };
  privacy: {
    analytics: boolean;
    dataSharing: boolean;
    marketingEmails: boolean;
  };
  display: {
    animations: boolean;
    compactMode: boolean;
    showFlags: boolean;
    defaultView: string;
  };
}

const defaultSettings: UserSettings = {
  email: '',
  firstName: '',
  lastName: '',
  company: '',
  role: 'Trade Analyst',
  timezone: 'UTC',
  language: 'en-US',
  currency: 'USD',
  dateFormat: 'MM/DD/YYYY',
  notifications: {
    email: true,
    push: true,
    tariffUpdates: true,
    tradeAlerts: true,
    weeklyReports: false,
  },
  privacy: {
    analytics: true,
    dataSharing: false,
    marketingEmails: false,
  },
  display: {
    animations: true,
    compactMode: false,
    showFlags: true,
    defaultView: 'dashboard',
  },
};

interface SettingsContextType {
  settings: UserSettings;
  updateSettings: (path: string, value: any) => void;
  resetSettings: () => void;
}

const SettingsContext = createContext<SettingsContextType | undefined>(undefined);

export const useSettings = () => {
  const context = useContext(SettingsContext);
  if (context === undefined) {
    throw new Error('useSettings must be used within a SettingsProvider');
  }
  return context;
};

interface SettingsProviderProps {
  children: ReactNode;
}

export const SettingsProvider = ({ children }: SettingsProviderProps) => {
  const [settings, setSettings] = useState<UserSettings>(defaultSettings);

  useEffect(() => {
    const savedSettings = safeLocalStorage.get<UserSettings>('userSettings');
    if (savedSettings) {
      setSettings({ ...defaultSettings, ...savedSettings });
    }
  }, []);

  const updateSettings = (path: string, value: any) => {
    // Defensive: prevent prototype pollution by rejecting dangerous path segments
    const denylist = new Set(['__proto__', 'prototype', 'constructor']);
    if (!path || typeof path !== 'string') {
      console.warn('updateSettings called with invalid path:', path);
      return;
    }

    const keys = path.split('.');
    if (keys.some(k => denylist.has(k))) {
      console.warn('Ignored updateSettings path containing unsafe segment:', path);
      return;
    }

    setSettings(prev => {
      const newSettings = { ...prev } as any;
      let current: any = newSettings;

      for (let i = 0; i < keys.length - 1; i++) {
        const key = keys[i];
        const next = current[key];
        // Only copy plain objects; if something unexpected is encountered, replace with an object
        current[key] = (next && typeof next === 'object' && !Array.isArray(next)) ? { ...next } : {};
        current = current[key];
      }

      const lastKey = keys[keys.length - 1];
      current[lastKey] = value;

      // Save to localStorage
      try {
        safeLocalStorage.set('userSettings', newSettings);
      } catch (err) {
        console.error('Failed to persist settings:', err);
      }

      return newSettings;
    });
  };

  const resetSettings = () => {
    setSettings(defaultSettings);
    safeLocalStorage.remove('userSettings');
  };

  return (
    <SettingsContext.Provider value={{ settings, updateSettings, resetSettings }}>
      {children}
    </SettingsContext.Provider>
  );
};