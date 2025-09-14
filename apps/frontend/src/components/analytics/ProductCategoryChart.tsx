import React from 'react'
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend, BarChart, Bar, XAxis, YAxis, CartesianGrid } from 'recharts'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { formatCurrency } from '@/lib/utils'

const productData = [
  {
    category: 'Electric Vehicles',
    hsCode: '8703.80',
    volume: 450000000,
    tariffRate: 12.5,
    growth: 15.2,
    share: 35.2,
    color: '#0ea5e9'
  },
  {
    category: 'Batteries & Components',
    hsCode: '8507.60',
    volume: 320000000,
    tariffRate: 8.2,
    growth: 22.8,
    share: 25.1,
    color: '#22c55e'
  },
  {
    category: 'Charging Equipment',
    hsCode: '8504.40',
    volume: 180000000,
    tariffRate: 6.5,
    growth: 18.7,
    share: 14.1,
    color: '#f59e0b'
  },
  {
    category: 'Electric Motors',
    hsCode: '8501.40',
    volume: 150000000,
    tariffRate: 10.1,
    growth: 12.3,
    share: 11.7,
    color: '#8b5cf6'
  },
  {
    category: 'Control Systems',
    hsCode: '8537.10',
    volume: 120000000,
    tariffRate: 7.8,
    growth: 9.5,
    share: 9.4,
    color: '#ef4444'
  },
  {
    category: 'Other Components',
    hsCode: 'Various',
    volume: 60000000,
    tariffRate: 9.2,
    growth: 5.1,
    share: 4.7,
    color: '#06b6d4'
  }
]

export function ProductCategoryChart() {
  const [viewMode, setViewMode] = React.useState<'pie' | 'bar'>('pie')

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload
      return (
        <div className="bg-background border border-border rounded-lg shadow-lg p-3">
          <p className="font-medium text-foreground mb-2">{data.category}</p>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">HS Code:</span>
              <span className="font-medium">{data.hsCode}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Volume:</span>
              <span className="font-medium">{formatCurrency(data.volume)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Market Share:</span>
              <span className="font-medium">{data.share.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Tariff Rate:</span>
              <span className="font-medium">{data.tariffRate.toFixed(1)}%</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted-foreground">Growth:</span>
              <span className={`font-medium ${data.growth > 0 ? 'text-success-600' : 'text-danger-600'}`}>
                {data.growth > 0 ? '+' : ''}{data.growth.toFixed(1)}%
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
            <CardTitle>Product Categories</CardTitle>
            <CardDescription>
              Trade volume and tariff rates by product category
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setViewMode('pie')}
              className={`px-3 py-1 text-xs rounded ${
                viewMode === 'pie' 
                  ? 'bg-brand-500 text-white' 
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              Pie
            </button>
            <button
              onClick={() => setViewMode('bar')}
              className={`px-3 py-1 text-xs rounded ${
                viewMode === 'bar' 
                  ? 'bg-brand-500 text-white' 
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              Bar
            </button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-80">
          <ResponsiveContainer width="100%" height="100%">
            {viewMode === 'pie' ? (
              <PieChart>
                <Pie
                  data={productData}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={({ category, share }) => `${category}: ${share.toFixed(1)}%`}
                  outerRadius={80}
                  fill="#8884d8"
                  dataKey="share"
                >
                  {productData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
                <Legend />
              </PieChart>
            ) : (
              <BarChart data={productData} margin={{ top: 20, right: 30, left: 20, bottom: 60 }}>
                <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
                <XAxis 
                  dataKey="category" 
                  axisLine={false}
                  tickLine={false}
                  className="text-xs text-muted-foreground"
                  angle={-45}
                  textAnchor="end"
                  height={80}
                  interval={0}
                  tick={{ fontSize: 10 }}
                />
                <YAxis 
                  axisLine={false}
                  tickLine={false}
                  className="text-xs text-muted-foreground"
                  tickFormatter={(value) => `${value}%`}
                />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="share" radius={[4, 4, 0, 0]}>
                  {productData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Bar>
              </BarChart>
            )}
          </ResponsiveContainer>
        </div>

        {/* Category Performance */}
        <div className="mt-4 space-y-2">
          <h4 className="font-medium text-sm">Growth Leaders</h4>
          <div className="grid gap-2">
            {productData
              .sort((a, b) => b.growth - a.growth)
              .slice(0, 3)
              .map((product) => (
                <div key={product.hsCode} className="flex items-center justify-between p-2 rounded border">
                  <div className="flex items-center gap-2">
                    <div 
                      className="w-3 h-3 rounded-full" 
                      style={{ backgroundColor: product.color }}
                    />
                    <span className="text-sm font-medium">{product.category}</span>
                    <Badge variant="outline" className="text-xs">
                      {product.hsCode}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-2 text-sm">
                    <span className="text-muted-foreground">Growth:</span>
                    <span className="font-medium text-success-600">
                      +{product.growth.toFixed(1)}%
                    </span>
                  </div>
                </div>
              ))}
          </div>
        </div>
      </CardContent>
    </Card>
  )
}