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
    setSettings(prev => {
      const keys = path.split('.');
      const newSettings = { ...prev };
      let current: any = newSettings;
      
      for (let i = 0; i < keys.length - 1; i++) {
        current[keys[i]] = { ...current[keys[i]] };
        current = current[keys[i]];
      }
      
      current[keys[keys.length - 1]] = value;
      
      // Save to localStorage
      safeLocalStorage.set('userSettings', newSettings);
      
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