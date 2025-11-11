import React from 'react'
import { motion } from 'framer-motion'
import { 
  Shield, 
  User, 
  Bell,
  Search,
  LogOut,
  Settings,
  ChevronDown
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useNavigate } from 'react-router-dom'
import { CountrySearch } from '@/components/search/CountrySearch'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/lib/utils'
import api from '@/services/api'

interface HeaderProps {
  className?: string
}

type UserProfile = {
  id?: number
  name: string
  email: string
  aboutMe?: string
}

export function Header({ className }: HeaderProps) {
  const { user: auth0User, logout } = useAuth()
  const navigate = useNavigate()
  const [showUserMenu, setShowUserMenu] = React.useState(false)
  const [showNotifications, setShowNotifications] = React.useState(false)
  const [userProfile, setUserProfile] = React.useState<UserProfile | null>(null)
  const dropdownRef = React.useRef<HTMLDivElement>(null)
  const notificationsRef = React.useRef<HTMLDivElement>(null)

  const handleLogout = () => {
    logout()
    setShowUserMenu(false)
  }

  // Fetch user profile from backend
  React.useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get('/profile')
        setUserProfile(response.data)
      } catch (error) {
        console.error('Failed to fetch profile for header:', error)
        // Fallback to Auth0 user data
        if (auth0User) {
          setUserProfile({
            name: auth0User.name || auth0User.email || 'User',
            email: auth0User.email || '',
          })
        }
      }
    }
    fetchProfile()
  }, [auth0User])

  // Listen for profile updates
  React.useEffect(() => {
    const handler = async () => {
      try {
        const response = await api.get('/profile')
        setUserProfile(response.data)
      } catch (error) {
        console.error('Failed to refresh profile:', error)
      }
    }
    window.addEventListener('profile:updated', handler)
    return () => window.removeEventListener('profile:updated', handler)
  }, [])

  // Close user menu when clicking outside
  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowUserMenu(false)
      }
      if (notificationsRef.current && !notificationsRef.current.contains(event.target as Node)) {
        setShowNotifications(false)
      }
    }

    if (showUserMenu || showNotifications) {
      document.addEventListener('mousedown', handleClickOutside)
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [showUserMenu, showNotifications])

  const displayName = userProfile?.name || auth0User?.name || auth0User?.email || 'User'
  const displayEmail = userProfile?.email || auth0User?.email || ''


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
            className="flex items-center rounded-md bg-transparent focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500"
          >
            {/* App icon intentionally removed to avoid dark mode contrast issues */}
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
          <div className="relative" ref={notificationsRef}>
            <Button 
              variant="ghost" 
              size="icon" 
              className="relative" 
              aria-label="Notifications"
              onClick={() => setShowNotifications(!showNotifications)}
            >
              <Bell className="w-5 h-5" />
              <span className="absolute -top-1 -right-1 w-3 h-3 bg-danger-500 rounded-full text-xs flex items-center justify-center">
                <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
              </span>
            </Button>

            {/* Notifications Dropdown */}
            {showNotifications && (
              <div className="absolute right-0 mt-2 w-80 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-200 dark:border-gray-700 z-50">
                <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
                  <h3 className="text-sm font-semibold text-gray-900 dark:text-white">Platform Updates</h3>
                </div>
                
                <div className="p-8 text-center">
                  <Bell className="w-12 h-12 mx-auto mb-3 text-gray-300 dark:text-gray-600" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    No updates about the platform yet
                  </p>
                  <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                    We'll notify you when there's something new
                  </p>
                </div>
              </div>
            )}
          </div>

          {/* User Menu */}
          <div className="relative" ref={dropdownRef}>
            <button 
              onClick={() => setShowUserMenu(!showUserMenu)}
              aria-haspopup="true" 
              aria-expanded={showUserMenu}
              aria-label={`Open profile menu for ${displayName}`} 
              className="flex items-center space-x-2 pl-2 border-l focus:outline-none hover:bg-gray-50 dark:hover:bg-gray-800 rounded-md px-2 py-1 transition-colors"
            >
              <div className="flex flex-col text-right">
                <span className="text-sm font-medium">{displayName}</span>
                <span className="text-xs text-muted-foreground">Member</span>
              </div>
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center ml-2 overflow-hidden">
                <User className="w-4 h-4 text-white" />
              </div>
              <ChevronDown className={`w-4 h-4 transition-transform ${showUserMenu ? 'rotate-180' : ''}`} />
            </button>

            {/* User Dropdown Menu */}
            {showUserMenu && (
              <div className="absolute right-0 mt-2 w-56 bg-white dark:bg-gray-800 rounded-md shadow-lg border border-gray-200 dark:border-gray-700 py-1 z-50">
                <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700">
                  <p className="text-sm font-medium text-gray-900 dark:text-white">{displayName}</p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">{displayEmail}</p>
                </div>
                
                <button
                  onClick={() => {
                    navigate('/profile')
                    setShowUserMenu(false)
                  }}
                  className="flex items-center w-full px-4 py-2 text-sm text-gray-700 dark:text-gray-200 hover:bg-gray-100 dark:hover:bg-gray-700"
                >
                  <User className="w-4 h-4 mr-3" />
                  Profile Settings
                </button>
                
                <div className="border-t border-gray-200 dark:border-gray-700 mt-1">
                  <button
                    onClick={handleLogout}
                    className="flex items-center w-full px-4 py-2 text-sm text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-900/10"
                  >
                    <LogOut className="w-4 h-4 mr-3" />
                    Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </motion.header>
  )
}
