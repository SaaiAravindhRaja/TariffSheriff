import React from 'react'
import { ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { formatCurrency } from '@/lib/utils'

const volumeData = [
  { 
    month: 'Jan', 
    imports: 2400, 
    exports: 1800, 
    netTrade: 600,
    avgValue: 45000,
    transactions: 1250
  },
  { 
    month: 'Feb', 
    imports: 2100, 
    exports: 1950, 
    netTrade: 150,
    avgValue: 47000,
    transactions: 1180
  },
  { 
    month: 'Mar', 
    imports: 2800, 
    exports: 2200, 
    netTrade: 600,
    avgValue: 46500,
    transactions: 1420
  },
  { 
    month: 'Apr', 
    imports: 2600, 
    exports: 2100, 
    netTrade: 500,
    avgValue: 48000,
    transactions: 1350
  },
  { 
    month: 'May', 
    imports: 3200, 
    exports: 2400, 
    netTrade: 800,
    avgValue: 49500,
    transactions: 1580
  },
  { 
    month: 'Jun', 
    imports: 2900, 
    exports: 2300, 
    netTrade: 600,
    avgValue: 48500,
    transactions: 1450
  },
  { 
    month: 'Jul', 
    imports: 3400, 
    exports: 2600, 
    netTrade: 800,
    avgValue: 50000,
    transactions: 1680
  },
  { 
    month: 'Aug', 
    imports: 3100, 
    exports: 2500, 
    netTrade: 600,
    avgValue: 49000,
    transactions: 1520
  },
  { 
    month: 'Sep', 
    imports: 3600, 
    exports: 2800, 
    netTrade: 800,
    avgValue: 51000,
    transactions: 1750
  },
  { 
    month: 'Oct', 
    imports: 3300, 
    exports: 2700, 
    netTrade: 600,
    avgValue: 50500,
    transactions: 1620
  },
  { 
    month: 'Nov', 
    imports: 3800, 
    exports: 3000, 
    netTrade: 800,
    avgValue: 52000,
    transactions: 1850
  },
  { 
    month: 'Dec', 
    imports: 4200, 
    exports: 3200, 
    netTrade: 1000,
    avgValue: 53000,
    transactions: 2050
  }
]

export function TradeVolumeChart() {
  const totalImports = volumeData.reduce((sum, item) => sum + item.imports, 0)
  const totalExports = volumeData.reduce((sum, item) => sum + item.exports, 0)
  const tradeBalance = totalImports - totalExports
  const avgTransactionValue = volumeData[volumeData.length - 1]?.avgValue || 53000

  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{`${label} 2024`}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Imports:</span>
              <span className="font-medium">${data.imports}M</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Exports:</span>
              <span className="font-medium">${data.exports}M</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Net Trade:</span>
              <span className={`font-medium ${data.netTrade > 0 ? 'text-success-600' : 'text-danger-600'}`}>
                ${data.netTrade}M
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Avg Value:</span>
              <span className="font-medium">{formatCurrency(data.avgValue)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Transactions:</span>
              <span className="font-medium">{data.transactions.toLocaleString()}</span>
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
            <CardTitle>Trade Volume Analysis</CardTitle>
            <CardDescription>
              Import/export volumes and transaction values
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Badge variant="success" className="text-xs">
              +{((tradeBalance / totalExports) * 100).toFixed(1)}% Balance
            </Badge>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={volumeData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
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
                tickFormatter={(value) => `$${value}M`}
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
              
              <Bar 
                yAxisId="left"
                dataKey="imports" 
                fill="#0ea5e9" 
                name="Imports ($M)"
                radius={[2, 2, 0, 0]}
              />
              <Bar 
                yAxisId="left"
                dataKey="exports" 
                fill="#22c55e" 
                name="Exports ($M)"
                radius={[2, 2, 0, 0]}
              />
              
              <Line
                yAxisId="right"
                type="monotone"
                dataKey="avgValue"
                stroke="#f59e0b"
                strokeWidth={3}
                dot={{ fill: "#f59e0b", strokeWidth: 2, r: 4 }}
                name="Avg Transaction Value"
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>

        {/* Trade Summary */}
        <div className="mt-4 grid grid-cols-4 gap-4 text-sm">
          <div className="text-center">
            <div className="font-medium text-lg text-brand-600">${totalImports.toLocaleString()}M</div>
            <div className="text-muted-foreground">Total Imports</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-success-600">${totalExports.toLocaleString()}M</div>
            <div className="text-muted-foreground">Total Exports</div>
          </div>
          <div className="text-center">
            <div className={`font-medium text-lg ${tradeBalance > 0 ? 'text-success-600' : 'text-danger-600'}`}>
              ${Math.abs(tradeBalance).toLocaleString()}M
            </div>
            <div className="text-muted-foreground">Trade {tradeBalance > 0 ? 'Surplus' : 'Deficit'}</div>
          </div>
          <div className="text-center">
            <div className="font-medium text-lg text-warning-600">{formatCurrency(avgTransactionValue)}</div>
            <div className="text-muted-foreground">Avg Transaction</div>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}