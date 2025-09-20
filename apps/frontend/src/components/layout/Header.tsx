import React from 'react'
import { motion } from 'framer-motion'
import { 
  Shield, 
  Moon, 
  Sun, 
  User, 
  Bell
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useNavigate } from 'react-router-dom'
import { CountrySearch } from '@/components/search/CountrySearch'
import { useTheme } from '@/hooks/useTheme'
import { cn } from '@/lib/utils'
import safeLocalStorage from '@/lib/safeLocalStorage'

interface HeaderProps {
  className?: string
}

export function Header({ className }: HeaderProps) {
  const { resolvedTheme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const [profile, setProfile] = React.useState<{ name: string; role: string; avatar?: string } | null>(null)

  React.useEffect(() => {
    const raw = safeLocalStorage.get<string>('app_profile')
    if (raw) {
      try {
        setProfile(typeof raw === 'string' ? JSON.parse(raw) : raw)
      } catch (e) {}
    }
    const handler = () => {
      const updated = safeLocalStorage.get<string>('app_profile')
      if (updated) {
        try { setProfile(typeof updated === 'string' ? JSON.parse(updated) : updated) } catch {}
      }
    }
    try {
      if (typeof window !== 'undefined') window.addEventListener('profile:updated', handler)
    } catch {}
    return () => {
      try { if (typeof window !== 'undefined') window.removeEventListener('profile:updated', handler) } catch {}
    }
  }, [])


  return (
    <motion.header
      role="banner"
      initial={{ y: -100, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ duration: 0.5 }}
      className={cn(
        "sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60",
        className
      )}
    >
      <div className="container flex h-16 items-center justify-between px-4">
        {/* Logo and Brand */}
        <motion.div 
          className="flex items-center space-x-3"
          whileHover={{ scale: 1.05 }}
          transition={{ type: "spring", stiffness: 400, damping: 10 }}
        >
          <a
            onClick={(e) => { e.preventDefault(); navigate('/') }}
            href="/"
            aria-label="Go to TariffSheriff home"
            title="TariffSheriff home"
            className="flex items-center justify-center w-16 h-16 rounded-md bg-transparent overflow-hidden focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500"
          >
            <img
              src="https://github.com/user-attachments/assets/f63f8f5b-6540-4f21-9f6f-508ea4254337"
              alt="TariffSheriff"
              className="w-12 h-12 sm:w-14 sm:h-14 object-contain"
            />
          </a>
          <div className="flex flex-col">
            <h1 className="text-xl font-bold bg-gradient-to-r from-brand-600 to-brand-800 bg-clip-text text-transparent">
              TariffSheriff
            </h1>
            <p className="text-xs text-muted-foreground -mt-1">
              Trade Intelligence Platform
            </p>
          </div>
        </motion.div>

        {/* Country Search */}
        <div className="flex-1 max-w-md mx-8">
          <CountrySearch 
            placeholder="Search countries for trade data..."
            className="w-full"
          />
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-2">
          {/* Notifications */}
          <Button variant="ghost" size="icon" className="relative" aria-label="Notifications">
            <Bell className="w-5 h-5" />
            <span className="absolute -top-1 -right-1 w-3 h-3 bg-danger-500 rounded-full text-xs flex items-center justify-center">
              <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
            </span>
          </Button>

          {/* Theme Toggle */}
          <Button
            variant="ghost"
            size="icon"
            onClick={() => toggleTheme()}
            aria-label={`Toggle theme, currently ${resolvedTheme}`}
            title={`Toggle theme (currently ${resolvedTheme})`}
            className="transition-transform hover:scale-110"
          >
            {resolvedTheme === 'dark' ? (
              <Sun className="w-5 h-5" />
            ) : (
              <Moon className="w-5 h-5" />
            )}
          </Button>

          {/* User Menu */}
          <button onClick={() => navigate('/profile')} aria-haspopup="true" aria-expanded="false" aria-label={`Open profile for ${profile?.name ?? 'user'}`} className="flex items-center space-x-2 pl-2 border-l focus:outline-none">
            <div className="flex flex-col text-right">
              <span className="text-sm font-medium">{profile?.name || 'John Doe'}</span>
              <span className="text-xs text-muted-foreground">{profile?.role || 'Trade Analyst'}</span>
            </div>
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center ml-2 overflow-hidden">
              {profile?.avatar ? (
                <img src={profile.avatar} alt={profile?.name ? `${profile.name} avatar` : 'User avatar'} className="w-full h-full object-cover" />
              ) : (
                <User className="w-4 h-4 text-white" />
              )}
            </div>
          </button>
        </div>
      </div>
    </motion.header>
  )
}