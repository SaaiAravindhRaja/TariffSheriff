import React from 'react'
import { motion } from 'framer-motion'
import { 
  Shield, 
  Moon, 
  Sun, 
  User, 
  Bell,
  Search
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useNavigate } from 'react-router-dom'
import { Input } from '@/components/ui/input'
import { useTheme } from '@/hooks/useTheme'
import { cn } from '@/lib/utils'

interface HeaderProps {
  className?: string
}

export function Header({ className }: HeaderProps) {
  const { resolvedTheme, toggleTheme } = useTheme()
  const navigate = useNavigate()
  const [profile, setProfile] = React.useState<{ name: string; role: string; avatar?: string } | null>(null)

  React.useEffect(() => {
    const raw = typeof window !== 'undefined' && localStorage.getItem('app_profile')
    if (raw) {
      try {
        setProfile(JSON.parse(raw))
      } catch (e) {}
    }
    const handler = () => {
      const updated = localStorage.getItem('app_profile')
      if (updated) setProfile(JSON.parse(updated))
    }
    window.addEventListener('profile:updated', handler)
    return () => window.removeEventListener('profile:updated', handler)
  }, [])
  const [isSearchFocused, setIsSearchFocused] = React.useState(false)

  return (
    <motion.header
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

        {/* Search Bar */}
        <div className="flex-1 max-w-md mx-8">
          <div className="relative">
            <Search className={cn(
              "absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 transition-colors",
              isSearchFocused ? "text-brand-500" : "text-muted-foreground"
            )} />
            <Input
              placeholder="Search tariffs, countries, or HS codes..."
              className="pl-10 pr-4 bg-muted/50 border-0 focus:bg-background transition-all duration-200"
              onFocus={() => setIsSearchFocused(true)}
              onBlur={() => setIsSearchFocused(false)}
            />
          </div>
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-2">
          {/* Notifications */}
          <Button variant="ghost" size="icon" className="relative">
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
          <button onClick={() => navigate('/profile')} aria-haspopup="true" aria-expanded="false" className="flex items-center space-x-2 pl-2 border-l focus:outline-none">
            <div className="flex flex-col text-right">
              <span className="text-sm font-medium">{profile?.name || 'John Doe'}</span>
              <span className="text-xs text-muted-foreground">{profile?.role || 'Trade Analyst'}</span>
            </div>
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center ml-2 overflow-hidden">
              {profile?.avatar ? (
                <img src={profile.avatar} alt="avatar" className="w-full h-full object-cover" />
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