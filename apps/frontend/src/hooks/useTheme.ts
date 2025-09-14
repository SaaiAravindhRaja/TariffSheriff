import { useEffect, useState } from 'react'

type Theme = 'dark' | 'light' | 'system'

export function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => {
    if (typeof window !== 'undefined') {
      return (localStorage.getItem('theme') as Theme) || 'system'
    }
    return 'system'
  })

  const [resolvedTheme, setResolvedTheme] = useState<'dark' | 'light'>(() => {
    if (typeof window !== 'undefined') {
      const stored = (localStorage.getItem('theme') as Theme) || 'system'
      if (stored === 'system') {
        return window.matchMedia('(prefers-color-scheme: dark)').matches
          ? 'dark'
          : 'light'
      }
      return stored === 'dark' ? 'dark' : 'light'
    }
    return 'light'
  })

  useEffect(() => {
    const root = window.document.documentElement
    root.classList.remove('light', 'dark')
    const apply = (t: Theme) => {
      if (t === 'system') {
        const systemTheme = window.matchMedia('(prefers-color-scheme: dark)').matches
          ? 'dark'
          : 'light'
        root.classList.add(systemTheme)
        setResolvedTheme(systemTheme)
      } else {
        root.classList.add(t)
        setResolvedTheme(t === 'dark' ? 'dark' : 'light')
      }
    }

    apply(theme)

    // listen to system changes when theme === 'system'
    const mql = window.matchMedia('(prefers-color-scheme: dark)')
    const listener = (e: MediaQueryListEvent) => {
      if (theme === 'system') {
        const systemTheme = e.matches ? 'dark' : 'light'
        root.classList.remove('light', 'dark')
        root.classList.add(systemTheme)
        setResolvedTheme(systemTheme)
      }
    }

    try {
      mql.addEventListener('change', listener)
    } catch (err) {
      // Safari fallback
      // @ts-ignore
      mql.addListener && mql.addListener(listener)
    }

    return () => {
      try {
        mql.removeEventListener('change', listener)
      } catch (err) {
        // @ts-ignore
        mql.removeListener && mql.removeListener(listener)
      }
    }
  }, [theme])

  const setThemeValue = (newTheme: Theme) => {
    localStorage.setItem('theme', newTheme)
    setTheme(newTheme)
  }

  const toggleTheme = () => {
    if (typeof window === 'undefined') return
    if (theme === 'system') {
      const systemIsDark = window.matchMedia('(prefers-color-scheme: dark)').matches
      // set opposite of current system so user sees a change
      setThemeValue(systemIsDark ? 'light' : 'dark')
    } else {
      setThemeValue(theme === 'dark' ? 'light' : 'dark')
    }
  }

  return {
    theme,
    resolvedTheme,
    setTheme: setThemeValue,
    toggleTheme,
  }
}