import { useEffect, useState } from 'react'

type Theme = 'dark' | 'light' | 'system'

export function useTheme() {
  const [theme, setTheme] = useState<Theme>('system')
  const [resolvedTheme, setResolvedTheme] = useState<'dark' | 'light'>('light')

  useEffect(() => {
    // Get theme from localStorage or default to system
    const savedTheme = localStorage.getItem('theme') as Theme || 'system'
    setTheme(savedTheme)
    
    // Function to get system theme
    const getSystemTheme = () => {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }

    // Resolve the actual theme
    const resolveTheme = (currentTheme: Theme) => {
      if (currentTheme === 'system') {
        return getSystemTheme()
      }
      return currentTheme
    }

    const resolved = resolveTheme(savedTheme)
    setResolvedTheme(resolved)

    // Apply theme to document
    const root = window.document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(resolved)

    // Listen for system theme changes
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const handleChange = () => {
      if (theme === 'system') {
        const newResolved = getSystemTheme()
        setResolvedTheme(newResolved)
        root.classList.remove('light', 'dark')
        root.classList.add(newResolved)
      }
    }

    mediaQuery.addEventListener('change', handleChange)
    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [theme])

  const setThemeAndSave = (newTheme: Theme) => {
    setTheme(newTheme)
    localStorage.setItem('theme', newTheme)
    
    const resolved = newTheme === 'system' 
      ? (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light')
      : newTheme
    
    setResolvedTheme(resolved)
    
    const root = window.document.documentElement
    root.classList.remove('light', 'dark')
    root.classList.add(resolved)
  }

  const toggleTheme = () => {
    const newTheme = resolvedTheme === 'dark' ? 'light' : 'dark'
    setThemeAndSave(newTheme)
  }

  return {
    theme,
    resolvedTheme,
    setTheme: setThemeAndSave,
    toggleTheme,
  }
}