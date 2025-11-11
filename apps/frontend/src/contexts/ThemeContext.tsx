import React, { createContext, useContext, useEffect, useMemo, useState } from 'react'
import safeLocalStorage from '@/lib/safeLocalStorage'

type Theme = 'light' | 'dark'

interface ThemeContextType {
  theme: Theme
  toggleTheme: () => void
  setTheme: (t: Theme) => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(() => {
    const stored = safeLocalStorage.get<Theme>('theme')
    return stored === 'light' || stored === 'dark' ? stored : 'dark'
  })

  useEffect(() => {
    const root = document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(theme)
    try {
      safeLocalStorage.set('theme', theme)
    } catch (e) {
      // ignore persistence errors (private mode, etc.)
      void e
    }
  }, [theme])

  const value = useMemo<ThemeContextType>(() => ({
    theme,
    toggleTheme: () => setThemeState(prev => prev === 'dark' ? 'light' : 'dark'),
    setTheme: (t: Theme) => setThemeState(t)
  }), [theme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
