import React from 'react'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts'
/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars */
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { TrendingUp, TrendingDown } from 'lucide-react'
import { formatCurrency } from '@/lib/utils'

const marketShareData = [
  {
    company: 'Tesla',
    marketShare: 28.5,
    revenue: 1420000000,
    growth: 15.2,
    tariffImpact: 'High',
    color: '#ef4444'
  },
  {
    company: 'BYD',
    marketShare: 22.1,
    revenue: 1100000000,
    growth: 32.8,
    tariffImpact: 'Medium',
    color: '#0ea5e9'
  },
  {
    company: 'Volkswagen Group',
    marketShare: 18.3,
    revenue: 910000000,
    growth: 8.7,
    tariffImpact: 'Low',
    color: '#22c55e'
  },
  {
    company: 'Stellantis',
    marketShare: 12.7,
    revenue: 632000000,
    growth: 5.4,
    tariffImpact: 'Medium',
    color: '#f59e0b'
  },
  {
    company: 'General Motors',
    marketShare: 9.8,
    revenue: 487000000,
    growth: -2.1,
    tariffImpact: 'High',
    color: '#8b5cf6'
  },
  {
    company: 'Ford',
    marketShare: 8.6,
    revenue: 428000000,
    growth: 12.3,
    tariffImpact: 'High',
    color: '#06b6d4'
  }
]

const regionData = [
  { region: 'North America', share: 35.2, growth: 8.5 },
  { region: 'Europe', share: 28.7, growth: 12.3 },
  { region: 'Asia Pacific', share: 31.1, growth: 18.7 },
  { region: 'Others', share: 5.0, growth: 5.2 }
]

export function MarketShareChart() {
  const [viewMode, setViewMode] = React.useState<'companies' | 'regions'>('companies')
  
  const totalMarket = marketShareData.reduce((sum, item) => sum + item.revenue, 0)
  const avgGrowth = marketShareData.reduce((sum, item) => sum + item.growth, 0) / marketShareData.length

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{data.company || data.region}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Market Share:</span>
              <span className="font-medium">{(data.marketShare || data.share).toFixed(1)}%</span>
            </div>
            {data.revenue && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Revenue:</span>
                <span className="font-medium">{formatCurrency(data.revenue)}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-muted-foreground">Growth:</span>
              <span className={`font-medium ${data.growth > 0 ? 'text-success-600' : 'text-danger-600'}`}>
                {data.growth > 0 ? '+' : ''}{data.growth.toFixed(1)}%
              </span>
            </div>
            {data.tariffImpact && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Tariff Impact:</span>
                <Badge 
                  variant={
                    data.tariffImpact === 'High' ? 'destructive' : 
                    data.tariffImpact === 'Medium' ? 'warning' : 'success'
                  }
                  className="text-xs"
                >
                  {data.tariffImpact}
                </Badge>
              </div>
            )}
          </div>
        </div>
      )
    }
    return null
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Market Share Analysis</CardTitle>
            <CardDescription>
              EV market share by {viewMode === 'companies' ? 'company' : 'region'} with growth trends
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setViewMode('companies')}
              className={`px-3 py-1 text-xs rounded ${
                viewMode === 'companies' 
                  ? 'bg-brand-500 text-white' 
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              Companies
            </button>
            <button
              onClick={() => setViewMode('regions')}
              className={`px-3 py-1 text-xs rounded ${
                viewMode === 'regions' 
                  ? 'bg-brand-500 text-white' 
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              Regions
            </button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            {viewMode === 'companies' ? (
              <PieChart>
                <Pie
                  data={marketShareData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ company, marketShare }: any) => `${company}: ${marketShare.toFixed(1)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="marketShare"
                >
                  {marketShareData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
                <Legend />
              </PieChart>
            ) : (
              <BarChart data={regionData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                <XAxis 
                  dataKey="region" 
                  axisLine={false}
                  tickLine={false}
                  className="text-xs text-muted-foreground"
                />
                <YAxis 
                  axisLine={false}
                  tickLine={false}
                  className="text-xs text-muted-foreground"
                  tickFormatter={(value) => `${value}%`}
                />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="share" radius={[4, 4, 0, 0]} fill="#0ea5e9" />
              </BarChart>
            )}
          </ResponsiveContainer>
        </div>

        {/* Market Leaders */}
        <div className="mt-4 space-y-2">
          <h4 className="font-medium text-sm">
            {viewMode === 'companies' ? 'Top Performers' : 'Regional Leaders'}
          </h4>
          <div className="grid gap-2">
            {(viewMode === 'companies' ? marketShareData : regionData)
              .sort((a, b) => b.growth - a.growth)
              .slice(0, 3)
              .map((item, index) => (
                <div key={(item as any).company || (item as any).region} className="flex items-center justify-between p-2 rounded border">
                  <div className="flex items-center gap-2">
                    {viewMode === 'companies' && (
                      <div 
                        className="w-3 h-3 rounded-full" 
                        style={{ backgroundColor: (item as any).color }}
                      />
                    )}
                    <span className="text-sm font-medium">
                      {(item as any).company || (item as any).region}
                    </span>
                    <Badge variant="outline" className="text-xs">
                      #{index + 1}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2">
                    {item.growth > 0 ? (
                      <TrendingUp className="w-3 h-3 text-success-500" />
                    ) : (
                      <TrendingDown className="w-3 h-3 text-danger-500" />
                    )}
                    <span className={`text-sm font-medium ${
                      item.growth > 0 ? 'text-success-600' : 'text-danger-600'
                    }`}>
                      {item.growth > 0 ? '+' : ''}{item.growth.toFixed(1)}%
                    </span>
                  </div>
                </div>
              ))}
          </div>
        </div>

        {/* Market Summary */}
        <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg">{formatCurrency(totalMarket)}</div>
            <div className="text-muted-foreground">Total Market</div>
          </div>
          <div className="text-center">
            <div className={`font-medium text-lg ${avgGrowth > 0 ? 'text-success-600' : 'text-danger-600'}`}>
              {avgGrowth > 0 ? '+' : ''}{avgGrowth.toFixed(1)}%
            </div>
            <div className="text-muted-foreground">Avg Growth</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">{marketShareData.length}</div>
            <div className="text-muted-foreground">Major Players</div>
          </div>
        </div>

        {/* Tariff Impact Analysis */}
        {viewMode === 'companies' && (
          <div className="mt-4 p-3 bg-warning-50 dark:bg-warning-900/20 rounded-lg border border-warning-200 dark:border-warning-800">
            <h4 className="font-medium text-sm text-warning-700 dark:text-warning-300 mb-2">
              Tariff Impact Analysis
            </h4>
            <div className="text-xs text-warning-600 dark:text-warning-400 space-y-1">
              <div>• High impact companies: Tesla, GM, Ford (US-based manufacturers)</div>
              <div>• Medium impact: BYD, Stellantis (mixed supply chains)</div>
              <div>• Low impact: Volkswagen Group (EU manufacturing base)</div>
              <div>• Consider supply chain diversification to reduce tariff exposure</div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}