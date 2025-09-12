import React from 'react'
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart
} from 'recharts'
import { formatCurrency, formatPercentage } from '@/lib/utils'

const data = [
  { month: 'Jan', tariffRate: 12.5, tradeVolume: 2400000, avgCost: 1200 },
  { month: 'Feb', tariffRate: 13.2, tradeVolume: 2100000, avgCost: 1350 },
  { month: 'Mar', tariffRate: 11.8, tradeVolume: 2800000, avgCost: 1180 },
  { month: 'Apr', tariffRate: 14.1, tradeVolume: 2600000, avgCost: 1420 },
  { month: 'May', tariffRate: 12.9, tradeVolume: 3200000, avgCost: 1290 },
  { month: 'Jun', tariffRate: 13.5, tradeVolume: 2900000, avgCost: 1380 },
  { month: 'Jul', tariffRate: 12.2, tradeVolume: 3400000, avgCost: 1220 },
  { month: 'Aug', tariffRate: 13.8, tradeVolume: 3100000, avgCost: 1400 },
  { month: 'Sep', tariffRate: 12.7, tradeVolume: 3600000, avgCost: 1270 },
  { month: 'Oct', tariffRate: 14.3, tradeVolume: 3300000, avgCost: 1450 },
  { month: 'Nov', tariffRate: 13.1, tradeVolume: 3800000, avgCost: 1310 },
  { month: 'Dec', tariffRate: 12.4, tradeVolume: 4200000, avgCost: 1240 }
]

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
                ? formatPercentage(entry.value / 100)
                : entry.dataKey === 'tradeVolume'
                ? formatCurrency(entry.value)
                : formatCurrency(entry.value)
              }
            </span>
          </div>
        ))}
      </div>
    )
  }
  return null
}

export function TariffChart() {
  return (
    <div className="h-80 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 10, right: 30, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="tariffGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="hsl(var(--primary))" stopOpacity={0.3}/>
              <stop offset="95%" stopColor="hsl(var(--primary))" stopOpacity={0}/>
            </linearGradient>
            <linearGradient id="volumeGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#22c55e" stopOpacity={0.3}/>
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
            tickFormatter={(value) => `$${(value / 1000000).toFixed(1)}M`}
          />
          <Tooltip content={<CustomTooltip />} />
          
          <Area
            yAxisId="right"
            type="monotone"
            dataKey="tradeVolume"
            stroke="#22c55e"
            strokeWidth={2}
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
  )
}