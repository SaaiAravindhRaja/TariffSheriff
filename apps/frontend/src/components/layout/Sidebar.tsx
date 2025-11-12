import React from 'react'
import { motion } from 'framer-motion'
import { NavLink, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Calculator,
  Database,
  Settings,
  FileText,
  Globe,
  BarChart3,
  Zap,
  Sparkles,
  Bot,
  Newspaper
} from 'lucide-react'
import { cn } from '@/lib/utils'

interface SidebarProps {
  className?: string
}

const navigationItems = [
  {
    title: 'Dashboard',
    href: '/dashboard',
    icon: LayoutDashboard,
    description: 'Overview & Analytics'
  },
  {
    title: 'Tariff Calculator',
    href: '/calculator',
    icon: Calculator,
    description: 'Calculate Import Costs'
  },
  {
    title: 'Tariff Database',
    href: '/database',
    icon: Database,
    description: 'Browse Tariff Rules'
  },
  {
    title: 'Saved Tariffs',
    href: '/saved-tariffs',
    icon: FileText,
    description: 'Your saved calculations'
  },
  {
    title: 'AI Assistant',
    href: '/ai-assistant',
    icon: Bot,
    description: 'Ask Trade Questions'
  },
  {
    title: 'News',
    href: '/news',
    icon: Newspaper,
    description: 'Tariff News & Updates'
  }
]

export function Sidebar({ className }: SidebarProps) {
  const location = useLocation()

  return (
    <motion.aside
      initial={{ x: -300, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.5, delay: 0.2 }}
      className={cn(
        "fixed left-0 top-16 z-40 h-[calc(100vh-4rem)] border-r bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 transition-all duration-300 w-64",
        className
      )}
    >
      {/* Navigation */}
      <nav className="flex flex-col gap-2 p-4">
        {navigationItems.map((item, index) => {
          const isActive = location.pathname === item.href
          const Icon = item.icon

          return (
            <motion.div
              key={item.href}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.3, delay: index * 0.1 }}
            >
              <NavLink
                to={item.href}
                className={({ isActive }: { isActive: boolean }) =>
                  cn(
                    "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-all duration-200 hover:bg-accent hover:text-accent-foreground group relative",
                    isActive
                      ? "bg-brand-50 text-brand-700 dark:bg-brand-900/20 dark:text-brand-300"
                      : "text-muted-foreground hover:text-foreground"
                  )
                }
              >
                <div className={cn(
                  "flex items-center justify-center w-5 h-5 transition-colors",
                  isActive && "text-brand-600 dark:text-brand-400"
                )}>
                  <Icon className="w-5 h-5" />
                </div>
                
                <div className="flex flex-col flex-1 min-w-0">
                  <span className="truncate">{item.title}</span>
                  <span className="text-xs text-muted-foreground truncate">
                    {item.description}
                  </span>
                </div>

                {isActive && (
                  <motion.div
                    layoutId="activeTab"
                    className="absolute left-0 top-0 bottom-0 w-1 bg-brand-600 rounded-r-full"
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  />
                )}
              </NavLink>
            </motion.div>
          )
        })}
      </nav>
    </motion.aside>
  )
}
