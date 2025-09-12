import React from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

const seasonalData = [
  { month: 'Jan', current: 85, previous: 78, forecast: 88, pattern: 'Low Season' },
  { month: 'Feb', current: 82, previous: 75, forecast: 85, pattern: 'Low Season' },
  { month: 'Mar', current: 95, previous: 88, forecast: 98, pattern: 'Spring Rise' },
  { month: 'Apr', current: 110, previous: 105, forecast: 115, pattern: 'Spring Rise' },
  { month: 'May', current: 125, previous: 118, forecast: 130, pattern: 'Peak Season' },
  { month: 'Jun', current: 135, previous: 128, forecast: 140, pattern: 'Peak Season' },
  { month: 'Jul', current: 140, previous: 135, forecast: 145, pattern: 'Peak Season' },
  { month: 'Aug', current: 138, previous: 132, forecast: 142, pattern: 'Peak Season' },
  { month: 'Sep', current: 120, previous: 115, forecast: 125, pattern: 'Decline' },
  { month: 'Oct', current: 105, previous: 98, forecast: 108, pattern: 'Decline' },
  { month: 'Nov', current: 95, previous: 90, forecast: 98, pattern: 'Low Season' },
  { month: 'Dec', current: 88, previous: 85, forecast: 92, pattern: 'Low Season' }
]

export function SeasonalityChart() {
  const peakMonth = seasonalData.reduce((max, item) => item.current > max.current ? item : max)
  const lowMonth = seasonalData.reduce((min, item) => item.current < min.current ? item : min)
  const seasonalVariation = ((peakMonth.current - lowMonth.current) / lowMonth.current * 100).toFixed(1)

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{`${label} - ${data.pattern}`}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Current Year:</span>
              <span className="font-medium">{data.current}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Previous Year:</span>
              <span className="font-medium">{data.previous}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Forecast:</span>
              <span className="font-medium text-brand-600">{data.forecast}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">YoY Change:</span>
              <span className={`font-medium ${data.current > data.previous ? 'text-success-600' : 'text-danger-600'}`}>
                {data.current > data.previous ? '+' : ''}{((data.current - data.previous) / data.previous * 100).toFixed(1)}%
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
            <CardTitle>Seasonal Trade Patterns</CardTitle>
            <CardDescription>
              Monthly trade volume patterns and forecasts
            </CardDescription>
          </div>
          <Badge variant="info" className="text-xs">
            {seasonalVariation}% Variation
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={seasonalData} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="currentGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="forecastGradient" x1="0" y1="0" x2="0" y2="1">
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
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              
              <Area
                type="monotone"
                dataKey="current"
                stroke="hsl(var(--primary))"
                strokeWidth={2}
                fill="url(#currentGradient)"
                name="Current Year"
              />
              
              <Line
                type="monotone"
                dataKey="previous"
                stroke="#94a3b8"
                strokeWidth={2}
                strokeDasharray="5 5"
                dot={{ fill: "#94a3b8", strokeWidth: 2, r: 3 }}
                name="Previous Year"
              />
              
              <Line
                type="monotone"
                dataKey="forecast"
                stroke="#22c55e"
                strokeWidth={2}
                strokeDasharray="8 4"
                dot={{ fill: "#22c55e", strokeWidth: 2, r: 3 }}
                name="Forecast"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Seasonal Insights */}
        <div className="mt-4 grid grid-cols-2 gap-4">
          <div className="space-y-2">
            <h4 className="font-medium text-sm">Peak Season</h4>
            <div className="p-3 bg-success-50 dark:bg-success-900/20 rounded-lg border border-success-200 dark:border-success-800">
              <div className="text-sm">
                <div className="font-medium text-success-700 dark:text-success-300">
                  {peakMonth.month} - {peakMonth.current}%
                </div>
                <div className="text-success-600 dark:text-success-400 text-xs">
                  Best month for trade volume
                </div>
              </div>
            </div>
          </div>
          
          <div className="space-y-2">
            <h4 className="font-medium text-sm">Low Season</h4>
            <div className="p-3 bg-warning-50 dark:bg-warning-900/20 rounded-lg border border-warning-200 dark:border-warning-800">
              <div className="text-sm">
                <div className="font-medium text-warning-700 dark:text-warning-300">
                  {lowMonth.month} - {lowMonth.current}%
                </div>
                <div className="text-warning-600 dark:text-warning-400 text-xs">
                  Lowest trade activity
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Pattern Analysis */}
        <div className="mt-4 p-3 bg-brand-50 dark:bg-brand-900/20 rounded-lg border border-brand-200 dark:border-brand-800">
          <h4 className="font-medium text-sm text-brand-700 dark:text-brand-300 mb-2">
            Seasonal Patterns
          </h4>
          <div className="text-xs text-brand-600 dark:text-brand-400 space-y-1">
            <div>• Peak season: May-August (125-140% of baseline)</div>
            <div>• Spring rise: March-April (+15-25% growth)</div>
            <div>• Low season: November-February (80-95% of baseline)</div>
            <div>• Forecast shows continued growth trend (+5-8% YoY)</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}