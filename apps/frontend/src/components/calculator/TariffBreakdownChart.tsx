import React from 'react'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatCurrency } from '@/lib/utils'

interface TariffBreakdownChartProps {
  data: {
    baseValue: number
    tariffAmount: number
    additionalFees: number
    totalCost: number
    breakdown: Array<{
      type: string
      rate: number
      amount: number
      description: string
    }>
  }
}

const COLORS = ['#0ea5e9', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4']

export function TariffBreakdownChart({ data }: TariffBreakdownChartProps) {
  const chartData = [
    {
      name: 'Base Value',
      value: data.baseValue,
      color: '#0ea5e9'
    },
    ...data.breakdown.map((item, index) => ({
      name: item.type,
      value: item.amount,
      color: COLORS[index + 1] || COLORS[0]
    }))
  ]

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0]
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground">{data.name}</p>
          <p className="text-sm text-muted-foreground">
            Amount: <span className="font-medium text-foreground">{formatCurrency(data.value)}</span>
          </p>
          <p className="text-sm text-muted-foreground">
            Percentage: <span className="font-medium text-foreground">
              {((data.value / data.payload.totalCost) * 100).toFixed(1)}%
            </span>
          </p>
        </div>
      )
    }
    return null
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Cost Breakdown</CardTitle>
        <CardDescription>
          Visual breakdown of all tariffs and fees
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {chartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip content={<CustomTooltip />} />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>
        
        {/* Summary Stats */}
        <div className="mt-4 grid grid-cols-2 gap-4 text-sm">
          <div className="flex justify-between">
            <span className="text-muted-foreground">Total Fees:</span>
            <span className="font-medium">
              {formatCurrency(data.tariffAmount + data.additionalFees)}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted-foreground">Effective Rate:</span>
            <span className="font-medium">
              {(((data.tariffAmount + data.additionalFees) / data.baseValue) * 100).toFixed(2)}%
            </span>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}