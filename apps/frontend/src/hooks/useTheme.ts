import { useState, useEffect } from 'react';
import safeLocalStorage from '@/lib/safeLocalStorage';

type Theme = 'light' | 'dark' | 'system';

export const useTheme = () => {
  const [theme, setTheme] = useState<Theme>('system');
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light');

  // Get system preference
  const getSystemTheme = (): 'light' | 'dark' => {
    if (typeof window !== 'undefined') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    return 'light';
  };

  // Apply theme to document
  const applyTheme = (newTheme: 'light' | 'dark') => {
    const root = document.documentElement;
    root.classList.remove('light', 'dark');
    root.classList.add(newTheme);
    setResolvedTheme(newTheme);
  };

  // Initialize theme
  useEffect(() => {
    const savedTheme = safeLocalStorage.get<Theme>('theme') || 'system';
    setTheme(savedTheme);

    const resolveTheme = (themeValue: Theme): 'light' | 'dark' => {
      if (themeValue === 'system') {
        return getSystemTheme();
      }
      return themeValue;
    };

    applyTheme(resolveTheme(savedTheme));

    // Listen for system theme changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handleChange = () => {
      if (savedTheme === 'system') {
        applyTheme(getSystemTheme());
      }
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, []);

  // Update theme
  const updateTheme = (newTheme: Theme) => {
    setTheme(newTheme);
    safeLocalStorage.set('theme', newTheme);
    
    const resolvedNewTheme = newTheme === 'system' ? getSystemTheme() : newTheme;
    applyTheme(resolvedNewTheme);
  };

  const toggleTheme = () => {
    const newTheme = resolvedTheme === 'light' ? 'dark' : 'light';
    updateTheme(newTheme);
  };

  return {
    theme,
    resolvedTheme,
    setTheme: updateTheme,
    toggleTheme,
  };
};