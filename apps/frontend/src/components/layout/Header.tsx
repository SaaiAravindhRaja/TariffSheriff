import React from 'react'
import { motion } from 'framer-motion'
import { 
  Shield, 
  Moon, 
  Sun, 
  User, 
  Bell,
  Search,
  LogOut,
  Settings,
  UserCircle,
  ChevronDown
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useNavigate } from 'react-router-dom'
import { CountrySearch } from '@/components/search/CountrySearch'
import { useTheme } from '@/hooks/useTheme'
import { useAuth } from '@/contexts/AuthContext'
import { cn } from '@/lib/utils'

interface HeaderProps {
  className?: string
}

export function Header({ className }: HeaderProps) {
  const { resolvedTheme, toggleTheme } = useTheme()
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [showUserMenu, setShowUserMenu] = React.useState(false)

  const handleLogout = async () => {
    try {
      await logout()
      navigate('/login')
    } catch (error) {
      console.error('Logout failed:', error)
    }
  }

  const handleProfileClick = () => {
    setShowUserMenu(false)
    navigate('/profile')
  }

  const handleSettingsClick = () => {
    setShowUserMenu(false)
    navigate('/settings')
  }

  // Close user menu when clicking outside
  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as Element
      if (!target.closest('[data-user-menu]')) {
        setShowUserMenu(false)
      }
    }

    if (showUserMenu) {
      document.addEventListener('mousedown', handleClickOutside)
      return () => document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [showUserMenu])


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

        {/* Country Search - Only show when authenticated */}
        {isAuthenticated && (
          <div className="flex-1 max-w-md mx-8">
            <CountrySearch 
              placeholder="Search countries for trade data..."
              className="w-full"
            />
          </div>
        )}

        {/* Actions */}
        <div className="flex items-center space-x-2">
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

          {isAuthenticated ? (
            <>
              {/* Notifications - Only for authenticated users */}
              <Button variant="ghost" size="icon" className="relative" aria-label="Notifications">
                <Bell className="w-5 h-5" />
                <span className="absolute -top-1 -right-1 w-3 h-3 bg-danger-500 rounded-full text-xs flex items-center justify-center">
                  <span className="w-1.5 h-1.5 bg-white rounded-full"></span>
                </span>
              </Button>

              {/* User Menu */}
              <div className="relative" data-user-menu>
                <button 
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  aria-haspopup="true" 
                  aria-expanded={showUserMenu}
                  aria-label={`Open profile menu for ${user?.name ?? 'user'}`}
                  className="flex items-center space-x-2 pl-2 border-l focus:outline-none hover:bg-accent rounded-md p-2 transition-colors"
                >
                  <div className="flex flex-col text-right">
                    <span className="text-sm font-medium">{user?.name || 'User'}</span>
                    <span className="text-xs text-muted-foreground capitalize">
                      {user?.role?.toLowerCase() || 'User'}
                    </span>
                  </div>
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-brand-400 to-brand-600 flex items-center justify-center ml-2 overflow-hidden">
                    <User className="w-4 h-4 text-white" />
                  </div>
                  <ChevronDown className={cn(
                    "w-4 h-4 text-muted-foreground transition-transform",
                    showUserMenu && "rotate-180"
                  )} />
                </button>

                {/* User Dropdown Menu */}
                {showUserMenu && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="absolute right-0 mt-2 w-56 bg-popover border border-border rounded-md shadow-lg z-50"
                  >
                    <div className="p-2">
                      <div className="px-3 py-2 border-b border-border">
                        <p className="text-sm font-medium">{user?.name}</p>
                        <p className="text-xs text-muted-foreground">{user?.email}</p>
                      </div>
                      
                      <div className="py-1">
                        <button
                          onClick={handleProfileClick}
                          className="flex items-center w-full px-3 py-2 text-sm text-foreground hover:bg-accent rounded-md transition-colors"
                        >
                          <UserCircle className="w-4 h-4 mr-2" />
                          Profile
                        </button>
                        
                        <button
                          onClick={handleSettingsClick}
                          className="flex items-center w-full px-3 py-2 text-sm text-foreground hover:bg-accent rounded-md transition-colors"
                        >
                          <Settings className="w-4 h-4 mr-2" />
                          Settings
                        </button>
                      </div>
                      
                      <div className="border-t border-border pt-1">
                        <button
                          onClick={handleLogout}
                          className="flex items-center w-full px-3 py-2 text-sm text-destructive hover:bg-destructive/10 rounded-md transition-colors"
                        >
                          <LogOut className="w-4 h-4 mr-2" />
                          Sign Out
                        </button>
                      </div>
                    </div>
                  </motion.div>
                )}
              </div>
            </>
          ) : (
            /* Login/Register buttons for unauthenticated users */
            <div className="flex items-center space-x-2">
              <Button
                variant="ghost"
                onClick={() => navigate('/login')}
                className="text-sm"
              >
                Sign In
              </Button>
              <Button
                variant="gradient"
                onClick={() => navigate('/register')}
                className="text-sm"
              >
                Sign Up
              </Button>
            </div>
          )}
        </div>
      </div>
    </motion.header>
  )
}