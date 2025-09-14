import React from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { TrendingUp, TrendingDown } from 'lucide-react'

const data = [
  { month: 'Jan', avgRate: 12.5, volume: 2400, disputes: 15, compliance: 94.2 },
  { month: 'Feb', avgRate: 13.2, volume: 2100, disputes: 18, compliance: 93.8 },
  { month: 'Mar', avgRate: 11.8, volume: 2800, disputes: 12, compliance: 95.1 },
  { month: 'Apr', avgRate: 14.1, volume: 2600, disputes: 22, compliance: 92.5 },
  { month: 'May', avgRate: 12.9, volume: 3200, disputes: 16, compliance: 94.7 },
  { month: 'Jun', avgRate: 13.5, volume: 2900, disputes: 19, compliance: 93.9 },
  { month: 'Jul', avgRate: 12.2, volume: 3400, disputes: 14, compliance: 95.3 },
  { month: 'Aug', avgRate: 13.8, volume: 3100, disputes: 21, compliance: 93.2 },
  { month: 'Sep', avgRate: 12.7, volume: 3600, disputes: 17, compliance: 94.8 },
  { month: 'Oct', avgRate: 14.3, volume: 3300, disputes: 25, compliance: 92.1 },
  { month: 'Nov', avgRate: 13.1, volume: 3800, disputes: 20, compliance: 94.3 },
  { month: 'Dec', avgRate: 12.4, volume: 4200, disputes: 18, compliance: 95.0 }
]

export function TariffTrendsChart() {
  const currentRate = data[data.length - 1]?.avgRate || 12.4
  const previousRate = data[data.length - 2]?.avgRate || 13.1
  const rateChange = currentRate - previousRate
  const isDecreasing = rateChange < 0

  type CustomTooltipProps = {
  active?: boolean;
  payload?: any[];
  label?: string;
};

const CustomTooltip = ({ active, payload, label }: CustomTooltipProps) => {
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
                {entry.dataKey === 'avgRate' 
                  ? `${entry.value.toFixed(2)}%`
                  : entry.dataKey === 'volume'
                  ? `${entry.value}M`
                  : entry.dataKey === 'compliance'
                  ? `${entry.value.toFixed(1)}%`
                  : entry.value
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
            <CardTitle>Tariff Rate Trends</CardTitle>
            <CardDescription>
              Average tariff rates and trade volume over time
            </CardDescription>
          </div>
          <Badge 
            variant={isDecreasing ? 'success' : 'destructive'}
            className="flex items-center gap-1"
          >
            {isDecreasing ? <TrendingDown className="w-3 h-3" /> : <TrendingUp className="w-3 h-3" />}
            {Math.abs(rateChange).toFixed(2)}%
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="rateGradient" x1="0" y1="0" x2="0" y2="1">
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
                tickFormatter={(value) => `${value}%`}
              />
              <YAxis 
                yAxisId="right"
                orientation="right"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value}M`}
              />
              <Tooltip content={<CustomTooltip />} />
              
              <Area
                yAxisId="right"
                type="monotone"
                dataKey="volume"
                stroke="#22c55e"
                strokeWidth={1}
                fill="url(#volumeGradient)"
                name="Trade Volume"
              />
              
              <Line
                yAxisId="left"
                type="monotone"
                dataKey="avgRate"
                stroke="hsl(var(--primary))"
                strokeWidth={3}
                dot={{ fill: "hsl(var(--primary))", strokeWidth: 2, r: 4 }}
                activeDot={{ r: 6, stroke: "hsl(var(--primary))", strokeWidth: 2 }}
                name="Avg Tariff Rate"
              />
              
              <Line
                yAxisId="left"
                type="monotone"
                dataKey="compliance"
                stroke="#f59e0b"
                strokeWidth={2}
                strokeDasharray="5 5"
                dot={{ fill: "#f59e0b", strokeWidth: 2, r: 3 }}
                name="Compliance Rate"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Key Insights */}
        <div className="mt-4 grid grid-cols-4 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg">{currentRate.toFixed(1)}%</div>
            <div className="text-muted-foreground">Current Rate</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">{data[data.length - 1]?.volume}M</div>
            <div className="text-muted-foreground">Monthly Volume</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">{data[data.length - 1]?.compliance.toFixed(1)}%</div>
            <div className="text-muted-foreground">Compliance</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">{data[data.length - 1]?.disputes}</div>
            <div className="text-muted-foreground">Disputes</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}