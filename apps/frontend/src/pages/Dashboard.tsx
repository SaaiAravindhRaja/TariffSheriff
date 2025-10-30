import React from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { DollarSign, Globe, Calculator, Zap, ArrowUpRight, ArrowDownRight, MoreHorizontal } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, getCountryFlag } from '@/lib/utils'
import { RecentCalculations } from '@/components/dashboard/RecentCalculations'

const statsData = [
  {
    title: 'Total Tariff Revenue',
    value: '$2,847,392',
    change: '+12.5%',
    trend: 'up',
    icon: DollarSign,
    description: 'This month'
  },
  {
    title: 'Active Trade Routes',
    value: '847',
    change: '+3.2%',
    trend: 'up',
    icon: Globe,
    description: 'Currently monitored'
  },
  {
    title: 'Calculations Today',
    value: '1,247',
    change: '-2.1%',
    trend: 'down',
    icon: Calculator,
    description: 'vs yesterday'
  },
  {
    title: 'Avg Processing Time',
    value: '0.8s',
    change: '-15.3%',
    trend: 'up',
    icon: Zap,
    description: 'Response time'
  }
]

const topCountries = [
  { code: 'US', name: 'United States', volume: '$1.2B', change: '+5.2%' },
  { code: 'CN', name: 'China', volume: '$987M', change: '+12.8%' },
  { code: 'DE', name: 'Germany', volume: '$654M', change: '-2.1%' },
  { code: 'JP', name: 'Japan', volume: '$432M', change: '+8.7%' },
  { code: 'GB', name: 'United Kingdom', volume: '$321M', change: '+3.4%' }
]

export function Dashboard() {
  const navigate = useNavigate();
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
        {statsData.map((stat, index) => {
          const Icon = stat.icon
          const isPositive = stat.trend === 'up'
          
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
                  <Icon className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">{stat.value}</div>
                  <div className="flex items-center space-x-1 text-xs text-muted-foreground">
                    <span className={`flex items-center ${
                      isPositive ? 'text-success-600' : 'text-danger-600'
                    }`}>
                      {isPositive ? (
                        <ArrowUpRight className="w-3 h-3 mr-1" />
                      ) : (
                        <ArrowDownRight className="w-3 h-3 mr-1" />
                      )}
                      {stat.change}
                    </span>
                    <span>{stat.description}</span>
                  </div>
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

        {/* Top Countries */}
        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.5, delay: 0.4 }}
        >
          <Card>
            <CardHeader>
              <CardTitle>Top Trading Partners</CardTitle>
              <CardDescription>
                Countries by trade volume this month
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {topCountries.map((country) => (
                <button
                  key={country.code}
                  onClick={() => navigate(`/country/${country.code.toLowerCase()}`)}
                  className="w-full flex items-center justify-between p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                >
                  <div className="flex items-center space-x-3">
                    <div className="text-2xl">{getCountryFlag(country.code)}</div>
                    <div className="text-left">
                      <p className="text-sm font-medium">{country.name}</p>
                      <p className="text-xs text-muted-foreground">{country.volume}</p>
                    </div>
                  </div>
                  <Badge 
                    variant={country.change.startsWith('+') ? 'success' : 'destructive'}
                    className="text-xs"
                  >
                    {country.change}
                  </Badge>
                </button>
              ))}
            </CardContent>
          </Card>
        </motion.div>
      </div>

      {/* Bottom Grid */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Recent Calculations */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.6 }}
        >
          <RecentCalculations />
        </motion.div>
      </div>

      {/* Trade Route Visualization */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.7 }}
      >
        <Card>
          <CardHeader>
            <CardTitle>Global Trade Routes</CardTitle>
            <CardDescription>
              Interactive visualization of active trade routes and tariff rates
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex h-64 items-center justify-center rounded-md border border-dashed text-sm text-muted-foreground">
              Map pending data connection.
            </div>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  )
}
