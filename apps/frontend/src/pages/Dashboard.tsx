import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { DollarSign, Globe, Calculator, Package, MoreHorizontal, ChevronDown, TrendingUp, Newspaper } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, getCountryFlag } from '@/lib/utils'
import { RecentCalculations } from '@/components/dashboard/RecentCalculations'
import { GlobalTradeRoutes } from '@/components/dashboard/GlobalTradeRoutes'
import { DashboardStats, CalculationPeriod } from '@/types/dashboard'
import api from '@/services/api'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

interface NewsArticle {
  title: string
  url: string
  publishedAt: string
  source?: string
}

interface TariffTrendData {
  date: string
  total: number
}

function Dashboard() {
  const navigate = useNavigate()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [period, setPeriod] = useState<CalculationPeriod>('today')
  const [loading, setLoading] = useState(true)
  const [newsArticles, setNewsArticles] = useState<NewsArticle[]>([])
  const [newsLoading, setNewsLoading] = useState(true)
  const [trendsLoading, setTrendsLoading] = useState(true)

  useEffect(() => {
    fetchDashboardStats()
  }, [period])

  useEffect(() => {
    fetchLatestNews()
  }, [])

  const fetchDashboardStats = async () => {
    try {
      setLoading(true)
      const response = await api.get<DashboardStats>('/profile/dashboard-stats', {
        params: { period }
      })
      setStats(response.data)
    } catch (error) {
      console.error('Failed to fetch dashboard stats:', error)
    } finally {
      setLoading(false)
    }
  }

  const fetchLatestNews = async () => {
    try {
      setNewsLoading(true)
      const response = await api.get('/news/articles')
      // Take the first 5 articles
      setNewsArticles(response.data.slice(0, 5))
    } catch (error) {
      console.error('Failed to fetch news:', error)
    } finally {
      setNewsLoading(false)
    }
  }

  const formatRevenue = (value: number) => {
    if (value >= 1000000) {
      return `$${(value / 1000000).toFixed(2)}M`
    } else if (value >= 1000) {
      return `$${(value / 1000).toFixed(1)}K`
    }
    return `$${value.toFixed(2)}`
  }

  const getPeriodLabel = () => {
    switch (period) {
      case 'today': return 'Today'
      case 'month': return 'This Month'
      case 'year': return 'This Year'
      default: return 'Today'
    }
  }

  const statsCards = [
    {
      title: 'Total Tariff Revenue',
      value: loading ? '...' : formatRevenue(stats?.totalTariffRevenue || 0),
      icon: DollarSign,
      description: 'From all your calculations',
      color: 'text-green-600 dark:text-green-400'
    },
    {
      title: 'Active Tariff Routes',
      value: loading ? '...' : (stats?.activeTariffRoutes?.toLocaleString() || '0'),
      icon: Globe,
      description: 'Available in database',
      color: 'text-blue-600 dark:text-blue-400'
    },
    {
      title: 'Calculations',
      value: loading ? '...' : (stats?.calculationsCount?.toString() || '0'),
      icon: Calculator,
      description: getPeriodLabel(),
      color: 'text-purple-600 dark:text-purple-400',
      hasDropdown: true
    },
    {
      title: 'Most Used HS Code',
      value: loading ? '...' : (stats?.mostUsedHsCode?.hsCode || 'None'),
      icon: Package,
      description: loading ? '' : (stats?.mostUsedHsCode?.description || 'No calculations yet'),
      subtext: loading ? '' : (stats?.mostUsedHsCode ? `Used ${stats.mostUsedHsCode.count}x` : ''),
      color: 'text-orange-600 dark:text-orange-400'
    }
  ]
  return (
    <div className="flex-1 space-y-6 p-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center justify-between"
      >
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">
            Welcome back! Here's what's happening with your trade operations.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline">
            Export Report
          </Button>
          <Button variant="gradient">
            New Calculation
          </Button>
        </div>
      </motion.div>

      {/* Stats Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {statsCards.map((stat, index) => {
          const Icon = stat.icon
          
          return (
            <motion.div
              key={stat.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: index * 0.1 }}
            >
              <Card className="card-hover">
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium">
                    {stat.title}
                  </CardTitle>
                  <div className="flex items-center gap-2">
                    <Icon className={`h-4 w-4 ${stat.color}`} />
                    {stat.hasDropdown && (
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <ChevronDown className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => setPeriod('today')}>
                            Today
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => setPeriod('month')}>
                            This Month
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => setPeriod('year')}>
                            This Year
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    )}
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stat.value}</div>
                  <p className="text-xs text-muted-foreground mt-1">
                    {stat.description}
                  </p>
                  {stat.subtext && (
                    <p className="text-xs text-muted-foreground mt-1 font-medium">
                      {stat.subtext}
                    </p>
                  )}
                </CardContent>
              </Card>
            </motion.div>
          )
        })}
      </div>

      {/* Main Content Grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Tariff Trends Summary */}
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.3 }}
          className="lg:col-span-2"
        >
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle>Tariff Trends</CardTitle>
                  <CardDescription>
                    Historical charts will appear here once data is connected.
                  </CardDescription>
                </div>
                <Button variant="ghost" size="icon">
                  <MoreHorizontal className="w-4 h-4" />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex h-64 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
                Chart pending data connection.
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Latest Tariff News */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
        >
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <div className="p-2 bg-blue-100 dark:bg-blue-900/20 rounded-lg">
                    <Newspaper className="w-5 h-5 text-blue-600" />
                  </div>
                  <div>
                    <CardTitle>Latest Tariff News</CardTitle>
                    <CardDescription>
                      Recent updates on tariffs and trade
                    </CardDescription>
                  </div>
                </div>
                <Button variant="ghost" size="sm" onClick={() => navigate('/news')}>
                  View All
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-3">
              {newsLoading ? (
                <div className="flex h-64 items-center justify-center text-sm text-muted-foreground">
                  Loading news...
                </div>
              ) : newsArticles.length === 0 ? (
                <div className="flex h-64 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
                  No news articles available
                </div>
              ) : (
                newsArticles.map((article, idx) => (
                  <a
                    key={idx}
                    href={article.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block p-3 rounded-lg border hover:bg-accent/50 transition-colors group"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium line-clamp-2 group-hover:text-brand-600 transition-colors">
                          {article.title}
                        </p>
                        <div className="flex items-center gap-2 mt-1">
                          {article.source && (
                            <span className="text-xs text-muted-foreground">
                              {article.source}
                            </span>
                          )}
                          {article.publishedAt && (
                            <span className="text-xs text-muted-foreground">
                              • {new Date(article.publishedAt).toLocaleDateString()}
                            </span>
                          )}
                        </div>
                      </div>
                      <TrendingUp className="w-4 h-4 text-muted-foreground shrink-0" />
                    </div>
                  </a>
                ))
              )}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Recent Calculations */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.6 }}
      >
        <RecentCalculations />
      </motion.div>

      {/* Trade Route Visualization */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.7 }}
        className="lg:px-0"
      >
        <GlobalTradeRoutes />
      </motion.div>
    </div>
  )
}

export { Dashboard }
