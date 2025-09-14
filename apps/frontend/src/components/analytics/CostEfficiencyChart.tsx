/* eslint-disable @typescript-eslint/no-explicit-any, @typescript-eslint/no-unused-vars */
import { ScatterChart, Scatter, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { formatCurrency } from '@/lib/utils'

const efficiencyData = [
  { 
    country: 'Japan', 
    code: 'JP',
    processingCost: 1200, 
    tariffRate: 6.8, 
    volume: 432,
    efficiency: 95.2,
    size: 15
  },
  { 
    country: 'Germany', 
    code: 'DE',
    processingCost: 1450, 
    tariffRate: 8.2, 
    volume: 654,
    efficiency: 92.8,
    size: 20
  },
  { 
    country: 'South Korea', 
    code: 'KR',
    processingCost: 1100, 
    tariffRate: 9.5, 
    volume: 298,
    efficiency: 94.1,
    size: 12
  },
  { 
    country: 'United States', 
    code: 'US',
    processingCost: 1800, 
    tariffRate: 12.5, 
    volume: 1200,
    efficiency: 88.5,
    size: 35
  },
  { 
    country: 'China', 
    code: 'CN',
    processingCost: 950, 
    tariffRate: 15.8, 
    volume: 987,
    efficiency: 85.2,
    size: 30
  },
  { 
    country: 'United Kingdom', 
    code: 'GB',
    processingCost: 1650, 
    tariffRate: 10.1, 
    volume: 321,
    efficiency: 90.7,
    size: 14
  },
  { 
    country: 'France', 
    code: 'FR',
    processingCost: 1550, 
    tariffRate: 9.8, 
    volume: 287,
    efficiency: 91.3,
    size: 13
  },
  { 
    country: 'Canada', 
    code: 'CA',
    processingCost: 1350, 
    tariffRate: 7.5, 
    volume: 245,
    efficiency: 93.6,
    size: 11
  }
]

export function CostEfficiencyChart() {
  const getEfficiencyColor = (efficiency: number) => {
    if (efficiency >= 93) return '#22c55e' // High efficiency - green
    if (efficiency >= 90) return '#f59e0b' // Medium efficiency - yellow
    return '#ef4444' // Low efficiency - red
  }

  const bestEfficiency = efficiencyData.reduce((max, item) => item.efficiency > max.efficiency ? item : max)
  const lowestCost = efficiencyData.reduce((min, item) => item.processingCost < min.processingCost ? item : min)

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{data.country}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Processing Cost:</span>
              <span className="font-medium">{formatCurrency(data.processingCost)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tariff Rate:</span>
              <span className="font-medium">{data.tariffRate.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Trade Volume:</span>
              <span className="font-medium">${data.volume}M</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Efficiency Score:</span>
              <span className={`font-medium ${
                data.efficiency >= 93 ? 'text-success-600' : 
                data.efficiency >= 90 ? 'text-warning-600' : 'text-danger-600'
              }`}>
                {data.efficiency.toFixed(1)}%
              </span>
            </div>
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
            <CardTitle>Cost Efficiency Analysis</CardTitle>
            <CardDescription>
              Processing costs vs tariff rates by country (bubble size = trade volume)
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Badge variant="success" className="text-xs">
              Best: {bestEfficiency.country}
            </Badge>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <ScatterChart margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                type="number" 
                dataKey="processingCost"
                name="Processing Cost"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => formatCurrency(value)}
              />
              <YAxis 
                type="number" 
                dataKey="tariffRate"
                name="Tariff Rate"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Scatter data={efficiencyData} fill="#8884d8">
                {efficiencyData.map((entry, index) => (
                  <Cell 
                    key={`cell-${index}`} 
                    fill={getEfficiencyColor(entry.efficiency)} 
                  />
                ))}
              </Scatter>
            </ScatterChart>
          </ResponsiveContainer>
        </div>

        {/* Efficiency Quadrants */}
        <div className="mt-4 grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <h4 className="font-medium text-sm">High Efficiency Countries</h4>
            <div className="space-y-1">
              {efficiencyData
                .filter(country => country.efficiency >= 93)
                .sort((a, b) => b.efficiency - a.efficiency)
                .map((country) => (
                  <div key={country.code} className="flex items-center justify-between p-2 rounded border">
                    <span className="text-sm font-medium">{country.country}</span>
                    <Badge variant="success" className="text-xs">
                      {country.efficiency.toFixed(1)}%
                    </Badge>
                  </div>
                ))}
            </div>
          </div>
          
          <div className="space-y-2">
            <h4 className="font-medium text-sm">Cost Leaders</h4>
            <div className="space-y-1">
              {efficiencyData
                .sort((a, b) => a.processingCost - b.processingCost)
                .slice(0, 3)
                .map((country) => (
                  <div key={country.code} className="flex items-center justify-between p-2 rounded border">
                    <span className="text-sm font-medium">{country.country}</span>
                    <Badge variant="info" className="text-xs">
                      {formatCurrency(country.processingCost)}
                    </Badge>
                  </div>
                ))}
            </div>
          </div>
        </div>

        {/* Efficiency Insights */}
        <div className="mt-4 p-3 bg-brand-50 dark:bg-brand-900/20 rounded-lg border border-brand-200 dark:border-brand-800">
          <h4 className="font-medium text-sm text-brand-700 dark:text-brand-300 mb-2">
            Efficiency Insights
          </h4>
          <div className="text-xs text-brand-600 dark:text-brand-400 space-y-1">
            <div>• {bestEfficiency.country} leads in efficiency at {bestEfficiency.efficiency.toFixed(1)}%</div>
            <div>• {lowestCost.country} offers lowest processing costs at {formatCurrency(lowestCost.processingCost)}</div>
            <div>• Countries with lower tariff rates tend to have higher efficiency scores</div>
            <div>• Consider routing through high-efficiency, low-cost corridors</div>
          </div>
        </div>

        {/* Legend */}
        <div className="mt-4 flex items-center justify-center space-x-6 text-xs">
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-success-500"></div>
            <span>High Efficiency (93%+)</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-warning-500"></div>
            <span>Medium Efficiency (90-93%)</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-3 h-3 rounded-full bg-danger-500"></div>
            <span>Low Efficiency (&lt;90%)</span>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}