import React, { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { DollarSign, Globe, Calculator, Package, MoreHorizontal, ChevronDown, TrendingUp, Newspaper } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, getCountryFlag } from '@/lib/utils'
import { RecentCalculations } from '@/components/dashboard/RecentCalculations'
import { DashboardStats, CalculationPeriod } from '@/types/dashboard'
import api from '@/services/api'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
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

type TrendPeriod = 'week' | 'month' | '6months' | 'year'

function Dashboard() {
  const navigate = useNavigate()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [period, setPeriod] = useState<CalculationPeriod>('today')
  const [loading, setLoading] = useState(true)
  const [newsArticles, setNewsArticles] = useState<NewsArticle[]>([])
  const [newsLoading, setNewsLoading] = useState(true)
  const [trendsData, setTrendsData] = useState<TariffTrendData[]>([])
  const [trendsLoading, setTrendsLoading] = useState(true)
  const [trendPeriod, setTrendPeriod] = useState<TrendPeriod>('month')

  // Add print styles
  useEffect(() => {
    const style = document.createElement('style')
    style.innerHTML = `
      @media print {
        .no-print { display: none !important; }
        .print-only { display: block !important; }
        body { background: white; }
        .card-hover { box-shadow: none !important; border: 1px solid #ddd !important; }
      }
    `
    document.head.appendChild(style)
    return () => {
      document.head.removeChild(style)
    }
  }, [])

  useEffect(() => {
    fetchDashboardStats()
    fetchTariffTrends()
  }, [period, trendPeriod])

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

  const fetchTariffTrends = async () => {
    try {
      setTrendsLoading(true)
      const response = await api.get('/tariff-calculations', {
        params: { page: 0, size: 1000 } // Get enough data for yearly trends
      })
      
      // Determine number of days based on period
      let numDays = 30
      let dateFormat: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' }
      
      switch (trendPeriod) {
        case 'week':
          numDays = 7
          dateFormat = { weekday: 'short', month: 'numeric', day: 'numeric' }
          break
        case 'month':
          numDays = 30
          dateFormat = { month: 'short', day: 'numeric' }
          break
        case '6months':
          numDays = 180
          dateFormat = { month: 'short', year: 'numeric' }
          break
        case 'year':
          numDays = 365
          dateFormat = { month: 'short', year: 'numeric' }
          break
      }
      
      // Group calculations by date and sum tariffs
      const calculations = response.data.content || []
      const trendsMap = new Map<string, number>()
      
      // Initialize days
      const daysData: TariffTrendData[] = []
      const today = new Date()
      for (let i = numDays - 1; i >= 0; i--) {
        const date = new Date(today)
        date.setDate(date.getDate() - i)
        const dateStr = date.toLocaleDateString('en-US', dateFormat)
        daysData.push({ date: dateStr, total: 0 })
        trendsMap.set(dateStr, 0)
      }
      
      // Add calculation data
      calculations.forEach((calc: any) => {
        const calcDate = new Date(calc.createdAt)
        const daysDiff = Math.floor((today.getTime() - calcDate.getTime()) / (1000 * 60 * 60 * 24))
        
        // Only include if within selected period
        if (daysDiff >= 0 && daysDiff < numDays) {
          const dateStr = calcDate.toLocaleDateString('en-US', dateFormat)
          const current = trendsMap.get(dateStr) || 0
          trendsMap.set(dateStr, current + (calc.totalTariff || 0))
        }
      })
      
      // Update the daysData array with actual values
      const trends = daysData.map(day => ({
        date: day.date,
        total: trendsMap.get(day.date) || 0
      }))
      
      setTrendsData(trends)
    } catch (error) {
      console.error('Failed to fetch tariff trends:', error)
    } finally {
      setTrendsLoading(false)
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

  const getTrendPeriodLabel = () => {
    switch (trendPeriod) {
      case 'week': return 'Last 7 Days'
      case 'month': return 'Last 30 Days'
      case '6months': return 'Last 6 Months'
      case 'year': return 'Last Year'
      default: return 'Last 30 Days'
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
        <div className="flex items-center space-x-2 no-print">
          <Button variant="outline" onClick={() => window.print()}>
            Export Report
          </Button>
          <Button variant="gradient" onClick={() => navigate('/calculator')}>
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
              <Card className="card-hover h-full">
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
                <CardContent className="flex flex-col min-h-[80px]">
                  <div className="text-2xl font-bold truncate">{stat.value}</div>
                  <p className="text-xs text-muted-foreground mt-1 line-clamp-2 flex-1">
                    {stat.description}
                  </p>
                  {stat.subtext && (
                    <p className="text-xs text-muted-foreground font-medium mt-1">
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
                    Your tariff calculations over time
                  </CardDescription>
                </div>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" size="sm" className="gap-2">
                      {getTrendPeriodLabel()}
                      <ChevronDown className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => setTrendPeriod('week')}>
                      Last 7 Days
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setTrendPeriod('month')}>
                      Last 30 Days
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setTrendPeriod('6months')}>
                      Last 6 Months
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setTrendPeriod('year')}>
                      Last Year
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </CardHeader>
            <CardContent>
              {trendsLoading ? (
                <div className="flex h-64 items-center justify-center text-sm text-muted-foreground">
                  Loading trends...
                </div>
              ) : trendsData.length === 0 || trendsData.every(d => d.total === 0) ? (
                <div className="flex flex-col h-64 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
                  <TrendingUp className="w-12 h-12 mb-4 opacity-50" />
                  <p>No calculation data yet.</p>
                  <p className="mt-1">Create your first calculation to see trends!</p>
                </div>
              ) : (
                <ResponsiveContainer width="100%" height={300}>
                  <LineChart data={trendsData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                    <XAxis 
                      dataKey="date" 
                      className="text-xs"
                      tick={{ fontSize: 12 }}
                      interval={trendPeriod === 'week' ? 0 : trendPeriod === 'month' ? 4 : trendPeriod === '6months' ? 29 : 59}
                      angle={trendPeriod === 'week' ? 0 : -45}
                      textAnchor={trendPeriod === 'week' ? 'middle' : 'end'}
                      height={trendPeriod === 'week' ? 30 : 60}
                    />
                    <YAxis 
                      className="text-xs"
                      tick={{ fontSize: 12 }}
                      tickFormatter={(value) => `$${value}`}
                    />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: 'hsl(var(--background))',
                        border: '1px solid hsl(var(--border))',
                        borderRadius: '8px'
                      }}
                      formatter={(value: number) => [`$${value.toFixed(2)}`, 'Tariff Total']}
                    />
                    <Line 
                      type="monotone" 
                      dataKey="total" 
                      stroke="hsl(var(--primary))" 
                      strokeWidth={2}
                      dot={trendPeriod === 'week' ? { fill: 'hsl(var(--primary))', r: 4 } : false}
                      activeDot={{ r: 6 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </motion.div>

        {/* Latest Tariff News */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
          className="no-print"
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
    </div>
  )
}

export { Dashboard }
