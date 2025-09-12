import React from 'react'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { formatCurrency, getCountryFlag } from '@/lib/utils'
import { ArrowDown, ArrowUp } from 'lucide-react'

interface ComparisonChartProps {
  baseResult: {
    baseValue: number
    tariffRate: number
    totalCost: number
  }
  alternatives: Array<{
    country: string
    tariffRate: number
    savings: number
  }>
}

export function ComparisonChart({ baseResult, alternatives }: ComparisonChartProps) {
  const chartData = [
    {
      country: 'Current Route',
      tariffRate: baseResult.tariffRate * 100,
      totalCost: baseResult.totalCost,
      savings: 0,
      isBase: true
    },
    ...alternatives.map(alt => ({
      country: alt.country,
      tariffRate: alt.tariffRate * 100,
      totalCost: baseResult.baseValue * (1 + alt.tariffRate) + (baseResult.totalCost - baseResult.baseValue - baseResult.baseValue * baseResult.tariffRate),
      savings: alt.savings,
      isBase: false
    }))
  ]

  const getBarColor = (savings: number, isBase: boolean) => {
    if (isBase) return '#64748b' // Current route in gray
    if (savings > 0) return '#22c55e' // Savings in green
    return '#ef4444' // More expensive in red
  }

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <div className="flex items-center gap-2 mb-2">
            <span className="text-lg">{getCountryFlag(data.country === 'Current Route' ? 'US' : data.country.substring(0, 2).toUpperCase())}</span>
            <p className="font-medium text-foreground">{data.country}</p>
          </div>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tariff Rate:</span>
              <span className="font-medium">{data.tariffRate.toFixed(2)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Total Cost:</span>
              <span className="font-medium">{formatCurrency(data.totalCost)}</span>
            </div>
            {data.savings !== 0 && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">
                  {data.savings > 0 ? 'Savings:' : 'Extra Cost:'}
                </span>
                <span className={`font-medium ${data.savings > 0 ? 'text-success-600' : 'text-danger-600'}`}>
                  {formatCurrency(Math.abs(data.savings))}
                </span>
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
        <CardTitle>Route Comparison</CardTitle>
        <CardDescription>
          Compare tariff rates and costs across different trade routes
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="country" 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                angle={-45}
                textAnchor="end"
                height={60}
              />
              <YAxis 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `${value.toFixed(1)}%`}
              />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="tariffRate" radius={[4, 4, 0, 0]}>
                {chartData.map((entry, index) => (
                  <Cell 
                    key={`cell-${index}`} 
                    fill={getBarColor(entry.savings, entry.isBase)} 
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Savings Summary */}
        <div className="mt-4 space-y-2">
          <h4 className="font-medium text-sm">Potential Savings</h4>
          <div className="grid gap-2">
            {alternatives.map((alt, index) => (
              <div key={index} className="flex items-center justify-between p-2 rounded border">
                <div className="flex items-center gap-2">
                  <span className="text-lg">{getCountryFlag(alt.country.substring(0, 2).toUpperCase())}</span>
                  <span className="text-sm font-medium">{alt.country}</span>
                  <Badge variant="outline" className="text-xs">
                    {(alt.tariffRate * 100).toFixed(1)}%
                  </Badge>
                </div>
                <div className="flex items-center gap-1">
                  {alt.savings > 0 ? (
                    <>
                      <ArrowDown className="w-3 h-3 text-success-500" />
                      <span className="text-sm font-medium text-success-600">
                        Save {formatCurrency(alt.savings)}
                      </span>
                    </>
                  ) : (
                    <>
                      <ArrowUp className="w-3 h-3 text-danger-500" />
                      <span className="text-sm font-medium text-danger-600">
                        +{formatCurrency(Math.abs(alt.savings))}
                      </span>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}