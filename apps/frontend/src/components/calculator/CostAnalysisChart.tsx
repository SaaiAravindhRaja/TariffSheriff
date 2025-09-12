import React from 'react'
import { ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatCurrency } from '@/lib/utils'

interface CostAnalysisChartProps {
  data: {
    baseValue: number
    tariffAmount: number
    additionalFees: number
    totalCost: number
  }
}

export function CostAnalysisChart({ data }: CostAnalysisChartProps) {
  // Generate volume-based cost analysis
  const volumeAnalysis = [1, 5, 10, 25, 50, 100, 250, 500].map(quantity => {
    const baseValue = data.baseValue * quantity
    const tariffAmount = data.tariffAmount * quantity
    const additionalFees = data.additionalFees * quantity
    const totalCost = data.totalCost * quantity
    const savingsPerUnit = quantity > 1 ? (data.totalCost - (totalCost / quantity)) : 0
    
    return {
      quantity,
      baseValue: baseValue / 1000, // Convert to thousands for readability
      tariffAmount: tariffAmount / 1000,
      additionalFees: additionalFees / 1000,
      totalCost: totalCost / 1000,
      costPerUnit: totalCost / quantity,
      savingsPerUnit: Math.max(0, savingsPerUnit)
    }
  })

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">Quantity: {label} units</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Total Cost:</span>
              <span className="font-medium">{formatCurrency(data.totalCost * 1000)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Cost per Unit:</span>
              <span className="font-medium">{formatCurrency(data.costPerUnit)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Base Value:</span>
              <span className="font-medium">{formatCurrency(data.baseValue * 1000)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tariffs:</span>
              <span className="font-medium">{formatCurrency(data.tariffAmount * 1000)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Additional Fees:</span>
              <span className="font-medium">{formatCurrency(data.additionalFees * 1000)}</span>
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
        <CardTitle>Volume Cost Analysis</CardTitle>
        <CardDescription>
          How costs scale with shipment quantity
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={volumeAnalysis} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis 
                dataKey="quantity" 
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
              />
              <YAxis 
                yAxisId="left"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => `$${value}K`}
              />
              <YAxis 
                yAxisId="right"
                orientation="right"
                axisLine={false}
                tickLine={false}
                className="text-xs text-muted-foreground"
                tickFormatter={(value) => formatCurrency(value)}
              />
              <Tooltip content={<CustomTooltip />} />
              <Legend />
              
              {/* Stacked bars for cost components */}
              <Bar 
                yAxisId="left"
                dataKey="baseValue" 
                stackId="cost"
                fill="#0ea5e9" 
                name="Base Value ($K)"
                radius={[0, 0, 0, 0]}
              />
              <Bar 
                yAxisId="left"
                dataKey="tariffAmount" 
                stackId="cost"
                fill="#f59e0b" 
                name="Tariffs ($K)"
                radius={[0, 0, 0, 0]}
              />
              <Bar 
                yAxisId="left"
                dataKey="additionalFees" 
                stackId="cost"
                fill="#ef4444" 
                name="Additional Fees ($K)"
                radius={[4, 4, 0, 0]}
              />
              
              {/* Line for cost per unit */}
              <Line
                yAxisId="right"
                type="monotone"
                dataKey="costPerUnit"
                stroke="#22c55e"
                strokeWidth={3}
                dot={{ fill: "#22c55e", strokeWidth: 2, r: 4 }}
                name="Cost per Unit"
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>

        {/* Key Insights */}
        <div className="mt-4 grid grid-cols-3 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg">
              {formatCurrency(volumeAnalysis[0].costPerUnit)}
            </div>
            <div className="text-muted-foreground">Cost per Unit (1x)</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg">
              {formatCurrency(volumeAnalysis[volumeAnalysis.length - 1].costPerUnit)}
            </div>
            <div className="text-muted-foreground">Cost per Unit (500x)</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-success-600">
              {formatCurrency(volumeAnalysis[0].costPerUnit - volumeAnalysis[volumeAnalysis.length - 1].costPerUnit)}
            </div>
            <div className="text-muted-foreground">Savings per Unit</div>
          </div>
        </div>

        {/* Volume Recommendations */}
        <div className="mt-4 p-3 bg-brand-50 dark:bg-brand-900/20 rounded-lg border border-brand-200 dark:border-brand-800">
          <h4 className="font-medium text-sm text-brand-700 dark:text-brand-300 mb-2">
            Volume Recommendations
          </h4>
          <div className="text-xs text-brand-600 dark:text-brand-400 space-y-1">
            <div>• Optimal volume: 100+ units for maximum per-unit savings</div>
            <div>• Break-even point: 25 units for significant cost reduction</div>
            <div>• Consider consolidating shipments to reduce per-unit costs</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}