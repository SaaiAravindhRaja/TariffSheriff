import React from 'react'
import { motion } from 'framer-motion'
import { NavLink, useLocation } from 'react-router-dom'
import {
  LayoutDashboard,
  Calculator,
  Database,
  TrendingUp,
  Settings,
  FileText,
  Globe,
  BarChart3,
  Zap,
  ChevronLeft,
  ChevronRight
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

interface SidebarProps {
  className?: string
}

const navigationItems = [
  {
    title: 'Dashboard',
    href: '/',
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
    title: 'Trade Routes',
    href: '/routes',
    icon: Globe,
    description: 'Optimize Trade Paths'
  },
  {
    title: 'Analytics',
    href: '/analytics',
    icon: BarChart3,
    description: 'Market Insights'
  },
  {
    title: 'Simulator',
    href: '/simulator',
    icon: Zap,
    description: 'Policy Scenarios'
  },
  {
    title: 'Reports',
    href: '/reports',
    icon: FileText,
    description: 'Export & Documentation'
  },
  {
    title: 'Settings',
    href: '/settings',
    icon: Settings,
    description: 'Preferences & Config'
  }
]

export function Sidebar({ className }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = React.useState(false)
  const location = useLocation()

  return (
    <motion.aside
      initial={{ x: -300, opacity: 0 }}
      animate={{ x: 0, opacity: 1 }}
      transition={{ duration: 0.5, delay: 0.2 }}
      className={cn(
        "fixed left-0 top-16 z-40 h-[calc(100vh-4rem)] border-r bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 transition-all duration-300",
        isCollapsed ? "w-16" : "w-64",
        className
      )}
    >
      {/* Collapse Toggle */}
      <Button
        variant="ghost"
        size="icon"
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="absolute -right-3 top-6 z-50 h-6 w-6 rounded-full border bg-background shadow-md hover:shadow-lg"
      >
        {isCollapsed ? (
          <ChevronRight className="h-3 w-3" />
        ) : (
          <ChevronLeft className="h-3 w-3" />
        )}
      </Button>

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
                className={({ isActive }) =>
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
                
                {!isCollapsed && (
                  <div className="flex flex-col flex-1 min-w-0">
                    <span className="truncate">{item.title}</span>
                    <span className="text-xs text-muted-foreground truncate">
                      {item.description}
                    </span>
                  </div>
                )}

                {isActive && (
                  <motion.div
                    layoutId="activeTab"
                    className="absolute left-0 top-0 bottom-0 w-1 bg-brand-600 rounded-r-full"
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  />
                )}

                {/* Tooltip for collapsed state */}
                {isCollapsed && (
                  <div className="absolute left-full ml-2 px-2 py-1 bg-popover text-popover-foreground text-xs rounded-md shadow-md opacity-0 group-hover:opacity-100 transition-opacity duration-200 pointer-events-none whitespace-nowrap z-50">
                    {item.title}
                  </div>
                )}
              </NavLink>
            </motion.div>
          )
        })}
      </nav>

      {/* Bottom Section */}
      {!isCollapsed && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.3, delay: 0.5 }}
          className="absolute bottom-4 left-4 right-4"
        >
          <div className="rounded-lg bg-gradient-to-br from-brand-50 to-brand-100 dark:from-brand-900/20 dark:to-brand-800/20 p-4 border border-brand-200 dark:border-brand-800">
            <div className="flex items-center gap-2 mb-2">
              <TrendingUp className="w-4 h-4 text-brand-600" />
              <span className="text-sm font-medium text-brand-700 dark:text-brand-300">
                Quick Stats
              </span>
            </div>
            <div className="space-y-1 text-xs text-brand-600 dark:text-brand-400">
              <div className="flex justify-between">
                <span>Active Rules:</span>
                <span className="font-medium">1,247</span>
              </div>
              <div className="flex justify-between">
                <span>Countries:</span>
                <span className="font-medium">195</span>
              </div>
              <div className="flex justify-between">
                <span>Last Update:</span>
                <span className="font-medium">2h ago</span>
              </div>
            </div>
          </div>
        </motion.div>
      )}
    </motion.aside>
  )
}