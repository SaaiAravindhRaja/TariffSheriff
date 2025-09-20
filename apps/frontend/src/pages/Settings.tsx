import React from 'react'
import { motion } from 'framer-motion'
import { Settings as SettingsIcon, Sun, Moon } from 'lucide-react'
import safeLocalStorage from '@/lib/safeLocalStorage'

export function Settings() {
  const [theme, setTheme] = React.useState<'light' | 'dark'>('light')
  const [email, setEmail] = React.useState('')
  const [saved, setSaved] = React.useState(false)

  React.useEffect(() => {
    const storedTheme = safeLocalStorage.get<'light' | 'dark'>('theme')
    if (storedTheme) setTheme(storedTheme)
    const storedEmail = safeLocalStorage.get<string>('userEmail')
    if (storedEmail) setEmail(storedEmail ?? '')
  }, [])

  function handleSave(e: React.FormEvent) {
    e.preventDefault()
    safeLocalStorage.set('theme', theme)
    safeLocalStorage.set('userEmail', email)
    setSaved(true)
    setTimeout(() => setSaved(false), 2000)
  }

  return (
    <div className="flex-1 space-y-6 p-6 max-w-xl mx-auto">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
      >
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
          <SettingsIcon className="w-8 h-8 text-brand-600" />
          Settings
        </h1>
        <p className="text-muted-foreground">
          Preferences and configuration
        </p>
      </motion.div>

      <form className="space-y-8" onSubmit={handleSave}>
        <div>
          <label className="block text-sm font-medium mb-2">Theme</label>
          <div className="flex gap-4 items-center">
            <button type="button" aria-label="Light mode" onClick={() => setTheme('light')} className={`p-2 rounded ${theme === 'light' ? 'bg-brand-100 dark:bg-brand-800' : 'bg-muted'}`}>
              <Sun className="w-5 h-5" />
            </button>
            <button type="button" aria-label="Dark mode" onClick={() => setTheme('dark')} className={`p-2 rounded ${theme === 'dark' ? 'bg-brand-100 dark:bg-brand-800' : 'bg-muted'}`}>
              <Moon className="w-5 h-5" />
            </button>
            <span className="ml-2 text-muted-foreground">Current: {theme.charAt(0).toUpperCase() + theme.slice(1)}</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium mb-2" htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            className="border rounded px-3 py-2 w-full bg-background"
            value={email}
            onChange={e => setEmail(e.target.value)}
            placeholder="your@email.com"
            autoComplete="email"
          />
        </div>

        <button type="submit" className="px-4 py-2 rounded bg-brand-600 text-white font-semibold hover:bg-brand-700 transition">
          Save Settings
        </button>
        {saved && <div className="text-green-600 mt-2">Settings saved!</div>}
      </form>
    </div>
  )
}