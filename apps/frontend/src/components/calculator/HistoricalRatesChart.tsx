import React from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { TrendingUp, TrendingDown } from 'lucide-react'

interface HistoricalRatesChartProps {
  hsCode: string
  originCountry: string
  destinationCountry: string
}

// Mock historical data
const generateHistoricalData = () => {
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  return months.map((month, index) => ({
    month,
    tariffRate: 12.5 + (Math.sin(index * 0.5) * 2) + (Math.random() * 1 - 0.5),
    tradeVolume: 2000000 + (Math.sin(index * 0.3) * 500000) + (Math.random() * 200000),
    avgProcessingTime: 2.5 + (Math.random() * 1 - 0.5),
    complianceRate: 95 + (Math.random() * 4)
  }))
}

export function HistoricalRatesChart({ hsCode, originCountry, destinationCountry }: HistoricalRatesChartProps) {
  const data = generateHistoricalData()
  const currentRate = data[data.length - 1]?.tariffRate || 12.5
  const previousRate = data[data.length - 2]?.tariffRate || 12.5
  const rateChange = currentRate - previousRate
  const isIncreasing = rateChange > 0

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{`${label} 2024`}</p>
          {payload.map((entry: any, index: number) => (
            <div key={index} className="flex items-center space-x-2 text-sm">
              <div 
                className="w-3 h-3 rounded-full" 
                style={{ backgroundColor: entry.color }}
              />
              <span className="text-muted-foreground">{entry.name}:</span>
              <span className="font-medium text-foreground">
                {entry.dataKey === 'tariffRate' 
                  ? `${entry.value.toFixed(2)}%`
                  : entry.dataKey === 'tradeVolume'
                  ? `$${(entry.value / 1000000).toFixed(1)}M`
                  : entry.dataKey === 'avgProcessingTime'
                  ? `${entry.value.toFixed(1)} days`
                  : `${entry.value.toFixed(1)}%`
                }
              </span>
            </div>
          ))}
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
            <CardTitle>Historical Tariff Rates</CardTitle>
            <CardDescription>
              12-month trend for {hsCode || 'selected product'}
            </CardDescription>
          </div>
          <div className="flex items-center space-x-2">
            <Badge 
              variant={isIncreasing ? 'destructive' : 'success'}
              className="flex items-center gap-1"
            >
              {isIncreasing ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
              {Math.abs(rateChange).toFixed(2)}%
            </Badge>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="tariffGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="volumeGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#22c55e" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#22c55e" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="month" 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
              />
              <YAxis 
                yAxisId="left"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value.toFixed(1)}%`}
              />
              <YAxis 
                yAxisId="right"
                orientation="right"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `$${(value / 1000000).toFixed(1)}M`}
              />
              <Tooltip content={<CustomTooltip />} />
              
              <Area
                yAxisId="right"
                type="monotone"
                dataKey="tradeVolume"
                stroke="#22c55e"
                strokeWidth={1}
                fill="url(#volumeGradient)"
                name="Trade Volume"
              />
              
              <Line
                yAxisId="left"
                type="monotone"
                dataKey="tariffRate"
                stroke="hsl(var(--primary))"
                strokeWidth={3}
                dot={{ fill: "hsl(var(--primary))", strokeWidth: 2, r: 4 }}
                activeDot={{ r: 6, stroke: "hsl(var(--primary))", strokeWidth: 2 }}
                name="Tariff Rate"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Key Insights */}
        <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg">{currentRate.toFixed(1)}%</div>
            <div className="text-muted-foreground">Current Rate</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">
              ${(data[data.length - 1]?.tradeVolume / 1000000).toFixed(1)}M
            </div>
            <div className="text-muted-foreground">Monthly Volume</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">
              {data[data.length - 1]?.avgProcessingTime.toFixed(1)} days
            </div>
            <div className="text-muted-foreground">Avg Processing</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}