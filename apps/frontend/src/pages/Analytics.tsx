import React from 'react'
import { motion } from 'framer-motion'
import { 
  BarChart3, 
  TrendingUp, 
  Globe, 
  Calendar,
  Filter,
  Download,
  RefreshCw,
  Target,
  DollarSign,
  Percent,
  Clock,
  Users
} from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatCurrency, formatPercentage } from '@/lib/utils'

// Import analytics components
import { TariffTrendsChart } from '@/components/analytics/TariffTrendsChart'
import { CountryAnalysisChart } from '@/components/analytics/CountryAnalysisChart'
import { ProductCategoryChart } from '@/components/analytics/ProductCategoryChart'
import { TradeVolumeChart } from '@/components/analytics/TradeVolumeChart'
import { SeasonalityChart } from '@/components/analytics/SeasonalityChart'
import { ComplianceMetricsChart } from '@/components/analytics/ComplianceMetricsChart'
import { CostEfficiencyChart } from '@/components/analytics/CostEfficiencyChart'
import { MarketShareChart } from '@/components/analytics/MarketShareChart'
import { RiskAnalysisChart } from '@/components/analytics/RiskAnalysisChart'
import { PredictiveAnalyticsChart } from '@/components/analytics/PredictiveAnalyticsChart'
import { RegionalHeatmap } from '@/components/analytics/RegionalHeatmap'
import { SupplyChainAnalytics } from '@/components/analytics/SupplyChainAnalytics'

const analyticsStats = [
  {
    title: 'Total Trade Value',
    value: '$847.2M',
    change: '+12.5%',
    trend: 'up',
    icon: DollarSign,
    description: 'This quarter'
  },
  {
    title: 'Avg Tariff Rate',
    value: '13.2%',
    change: '+0.8%',
    trend: 'up',
    icon: Percent,
    description: 'Across all products'
  },
  {
    title: 'Processing Time',
    value: '2.3 days',
    change: '-15.2%',
    trend: 'down',
    icon: Clock,
    description: 'Average clearance'
  },
  {
    title: 'Active Traders',
    value: '1,247',
    change: '+8.7%',
    trend: 'up',
    icon: Users,
    description: 'This month'
  }
]

export function Analytics() {
  const [selectedTimeRange, setSelectedTimeRange] = React.useState('3M')
  const [selectedRegion, setSelectedRegion] = React.useState('all')

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
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-3">
            <BarChart3 className="w-8 h-8 text-brand-600" />
            Analytics Dashboard
          </h1>
          <p className="text-muted-foreground">
            Comprehensive market insights and trade analytics
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" size="sm">
            <Calendar className="w-4 h-4 mr-2" />
            Last 3 Months
          </Button>
          <Button variant="outline" size="sm">
            <Filter className="w-4 h-4 mr-2" />
            Filters
          </Button>
          <Button variant="outline" size="sm">
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </Button>
          <Button variant="gradient" size="sm">
            <Download className="w-4 h-4 mr-2" />
            Export Report
          </Button>
        </div>
      </motion.div>

      {/* Key Metrics */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {analyticsStats.map((stat, index) => {
          const Icon = stat.icon
          const isPositive = stat.trend === 'up' && !stat.title.includes('Time')
          
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
                    <Badge 
                      variant={isPositive ? 'success' : 'destructive'}
                      className="text-xs"
                    >
                      {stat.change}
                    </Badge>
                    <span>{stat.description}</span>
                  </div>
                </CardContent>
              </Card>
            </motion.div>
          )
        })}
      </div>

      {/* Analytics Tabs */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.3 }}
      >
        <Tabs defaultValue="overview" className="space-y-6">
          <TabsList className="grid w-full grid-cols-6">
            <TabsTrigger value="overview">Overview</TabsTrigger>
            <TabsTrigger value="trends">Trends</TabsTrigger>
            <TabsTrigger value="countries">Countries</TabsTrigger>
            <TabsTrigger value="products">Products</TabsTrigger>
            <TabsTrigger value="compliance">Compliance</TabsTrigger>
            <TabsTrigger value="predictions">Predictions</TabsTrigger>
          </TabsList>

          {/* Overview Tab */}
          <TabsContent value="overview" className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <TariffTrendsChart />
              <TradeVolumeChart />
            </div>
            <div className="grid gap-6 lg:grid-cols-3">
              <CountryAnalysisChart />
              <ProductCategoryChart />
              <CostEfficiencyChart />
            </div>
            <RegionalHeatmap />
          </TabsContent>

          {/* Trends Tab */}
          <TabsContent value="trends" className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <SeasonalityChart />
              <MarketShareChart />
            </div>
            <div className="grid gap-6 lg:grid-cols-2">
              <TariffTrendsChart />
              <TradeVolumeChart />
            </div>
          </TabsContent>

          {/* Countries Tab */}
          <TabsContent value="countries" className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <CountryAnalysisChart />
              <RegionalHeatmap />
            </div>
            <div className="grid gap-6 lg:grid-cols-3">
              <MarketShareChart />
              <CostEfficiencyChart />
              <RiskAnalysisChart />
            </div>
          </TabsContent>

          {/* Products Tab */}
          <TabsContent value="products" className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <ProductCategoryChart />
              <SupplyChainAnalytics />
            </div>
            <div className="grid gap-6 lg:grid-cols-2">
              <SeasonalityChart />
              <CostEfficiencyChart />
            </div>
          </TabsContent>

          {/* Compliance Tab */}
          <TabsContent value="compliance" className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <ComplianceMetricsChart />
              <RiskAnalysisChart />
            </div>
            <div className="grid gap-6 lg:grid-cols-2">
              <CountryAnalysisChart />
              <CostEfficiencyChart />
            </div>
          </TabsContent>

          {/* Predictions Tab */}
          <TabsContent value="predictions" className="space-y-6">
            <div className="grid gap-6 lg:grid-cols-2">
              <PredictiveAnalyticsChart />
              <RiskAnalysisChart />
            </div>
            <div className="grid gap-6 lg:grid-cols-2">
              <TariffTrendsChart />
              <MarketShareChart />
            </div>
          </TabsContent>
        </Tabs>
      </motion.div>
    </div>
  )
}